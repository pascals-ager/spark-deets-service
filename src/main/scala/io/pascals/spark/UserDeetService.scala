package io.pascals.spark

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.{Logger, LoggerFactory}

object UserDeetService extends App {

  val logger: Logger = LoggerFactory.getLogger(getClass)
  /* Define config file and check for existence of a config property */
  val appConfig: Config = ConfigFactory.parseResources("defaults.conf")
  appConfig.checkValid(ConfigFactory.defaultReference(), "source-conf")

  logger.info("Initialized source and destination file configurations")
  /* Initialize required configs */
  val hdfsURI = appConfig.getString("source-conf.node_master")
  val brochuresClickSrc = hdfsURI.concat(appConfig.getString("source-conf.brochure_click"))
  val pageTurnsSrc = hdfsURI.concat(appConfig.getString("source-conf.page_turn"))
  val pageEntersSrc = hdfsURI.concat(appConfig.getString("source-conf.page_enter"))
  val pageExitsSrc = hdfsURI.concat(appConfig.getString("source-conf.page_exit"))
  val userDeetsDest = hdfsURI.concat(appConfig.getString("source-conf.user_details"))

  /* Common trait to model page view */
  trait PageView {
    def page: Option[Long]

    def page_view_mode: Option[String]

    def user_agent: Option[String]
  }

  /* Data model definitions. These could be generated from Schema files */
  case class BrochureClick(brochure_click_id: Option[Long], brochure_click_uuid: Option[String], brochure_id: Option[Long], date_time: Option[String], event: Option[String], ip: Option[String], location_device_lat: Option[Double], location_device_lng: Option[Double], location_intended_lat: Option[Double], location_intended_lng: Option[Double], no_profile: Option[Boolean], page: Option[Long], page_type: Option[String], restricted_ip: Option[Boolean], traffic_source_type: Option[String], traffic_source_value: Option[String], treatment: Option[String], user_agent: Option[String], user_ident: Option[String], user_zip: Option[String], visit_id: Option[Long], visit_origin_type: Option[String])

  case class PageTurn(brochure_click_uuid: Option[String], date_time: Option[String], event: Option[String], ip_address: Option[String], override val page: Option[Long], override val page_view_mode: Option[String], override val user_agent: Option[String]) extends PageView

  case class PageEnter(brochure_click_uuid: Option[String], date_time: Option[String], event: Option[String], ip_address: Option[String], override val page: Option[Long], override val page_view_mode: Option[String], screen_size: Option[String], override val user_agent: Option[String]) extends PageView

  case class PageExit(brochure_click_uuid: Option[String], date_time: Option[String], event: Option[String], ip_address: Option[String], override val page: Option[Long], override val page_view_mode: Option[String], override val user_agent: Option[String]) extends PageView

  /* Intermediate definitions to do typed transformations and actions */
  /* An Event can be a Page Turn, Enter or Exit, all of which extend trait PageView */
  case class ClickEvent(user_ident: Option[String], page_view_mode: Option[String])

  case class UserEventCount(user_ident: Option[String], event_count: Option[Int])

  case class UserEventTotal(user_ident: Option[String], total_events: Option[BigInt])

  /* Data model of the final result Dataset*/
  case class UserDeetsDescription(user_ident: Option[String], total_views: Option[BigInt], total_enters: Option[BigInt], total_exits: Option[BigInt])

  implicit val spark: SparkSession = SparkSession
    .builder()
    .appName("userDeetProcessor")
    .getOrCreate()

  import spark.implicits._

  logger.info("Reading source files into typed datasets")
  /* Read source files into typed Datasets */
  val brochureClick: Dataset[BrochureClick] = spark.read.json(brochuresClickSrc).as[BrochureClick]
  val pageTurn: Dataset[PageTurn] = spark.read.json(pageTurnsSrc).as[PageTurn]
  val pageEnter: Dataset[PageEnter] = spark.read.json(pageEntersSrc).as[PageEnter]
  val pageExit: Dataset[PageExit] = spark.read.json(pageExitsSrc).as[PageExit]

  /* Dataset which is bound to the trait PageView. */
  val userDatasets: Seq[Dataset[_ >: PageTurn with PageEnter with PageExit <: PageView]] = Seq(pageTurn, pageEnter, pageExit)

  /**
  * @param  userDataset upper bound to trait PageView
  * */

  def userEventAggregator(userDataset: => Seq[Dataset[_ >: PageExit with PageEnter with PageTurn <: PageView]]): Seq[Dataset[UserEventTotal]] = userDataset.map {
    ds => {
      brochureClick.joinWith(ds, brochureClick.col("brochure_click_uuid") === ds.col("brochure_click_uuid"), "left").map {
        case (click: BrochureClick, turn: PageView) => ClickEvent(click.user_ident, turn.page_view_mode)
        case (click: BrochureClick, null) => ClickEvent(click.user_ident, Some("None"))
      }.map {
        case ClickEvent(user_ident, Some("DOUBLE_PAGE_MODE")) => UserEventCount(user_ident, Some(2))
        case ClickEvent(user_ident, Some("SINGLE_PAGE_MODE")) => UserEventCount(user_ident, Some(1))
        case ClickEvent(user_ident, Some("None")) => UserEventCount(user_ident, Some(0))
      }.groupBy("user_ident").agg(sum("event_count") as "total_events").as[UserEventTotal]
    }
  }

  logger.info("Transforming the events datasets into generic UserEventTotal dataset")
  /* Get intermediate counts for all three events -> turns, enters, exits */
  val transformedDS = userEventAggregator(userDatasets)

  /* Not ideal, but statically extracting the transformedDS in order */
  val pageTurnTransformedDS = transformedDS.head
  val pageEnterTransformedDS = transformedDS.tail.head
  val pageExitTransformedDS = transformedDS.tail.tail.head

  logger.info("Joining the UserEventTotal datasets on user_ident and mapping counts to appropriate variables in UserDeetsDescription")
  /* Join the three event Datasets while mapping the individual counts into the appropriate variable of UserDeetsDescription model*/
  val tempDS: Dataset[UserDeetsDescription] = pageTurnTransformedDS.joinWith(pageEnterTransformedDS, pageTurnTransformedDS.col("user_ident") === pageEnterTransformedDS.col("user_ident"), "inner").map {
    case (view: UserEventTotal, turns: UserEventTotal) => UserDeetsDescription(view.user_ident, view.total_events, turns.total_events, Some(0))
  }

  val userDeetsDescriptionDS: Dataset[UserDeetsDescription] = tempDS.joinWith(pageExitTransformedDS, tempDS.col("user_ident") === pageExitTransformedDS.col("user_ident"), "inner").map {
    case (deets: UserDeetsDescription, exits: UserEventTotal) => UserDeetsDescription(deets.user_ident, deets.total_views, deets.total_enters, exits.total_events)
  }

  /* Write the dataset to hdfs for easier review */
  userDeetsDescriptionDS.coalesce(1).write.mode(SaveMode.Overwrite).json(userDeetsDest)
}
