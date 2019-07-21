package io.pascals.spark

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.functions._

object UserDeetService extends App {
  val appConfig: Config = ConfigFactory.parseResources("defaults.conf")
  appConfig.checkValid(ConfigFactory.defaultReference(), "source-conf")

  val hdfsURI = appConfig.getString("source-conf.node_master")
  val brochuresClickSrc = hdfsURI.concat(appConfig.getString("source-conf.brochure_click"))
  val pageTurnsSrc = hdfsURI.concat(appConfig.getString("source-conf.page_turn"))
  val pageEntersSrc = hdfsURI.concat(appConfig.getString("source-conf.page_enter"))
  val pageExitsSrc = hdfsURI.concat(appConfig.getString("source-conf.page_exit"))

  trait PageView {
    def page: Option[Long]
    def page_view_mode: Option[String]
    def user_agent: Option[String]
  }

  case class BrochureClick(brochure_click_id: Option[Long], brochure_click_uuid: Option[String], brochure_id: Option[Long], date_time: Option[String], event: Option[String], ip: Option[String], location_device_lat: Option[Double], location_device_lng: Option[Double], location_intended_lat: Option[Double], location_intended_lng: Option[Double], no_profile: Option[Boolean], page: Option[Long], page_type: Option[String], restricted_ip: Option[Boolean], traffic_source_type: Option[String], traffic_source_value: Option[String], treatment: Option[String], user_agent: Option[String], user_ident: Option[String], user_zip: Option[String], visit_id: Option[Long], visit_origin_type: Option[String])
  case class PageTurn(brochure_click_uuid: Option[String], date_time: Option[String], event: Option[String], ip_address: Option[String], override val page: Option[Long], override val page_view_mode: Option[String], override val user_agent: Option[String]) extends PageView
  case class PageEnter(brochure_click_uuid: Option[String], date_time: Option[String], event: Option[String], ip_address: Option[String], override val page: Option[Long], override val page_view_mode: Option[String], screen_size: Option[String], override val user_agent: Option[String]) extends PageView
  case class PageExit(brochure_click_uuid: Option[String], date_time: Option[String], event: Option[String], ip_address: Option[String], override val page: Option[Long], override val page_view_mode: Option[String], override val user_agent: Option[String]) extends PageView

  case class ClickEvent(user_ident: Option[String], page_view_mode: Option[String])
  case class UserEventCount(user_ident: Option[String], event_count: Option[Int])
  case class UserEventTotal(user_ident: Option[String], total_events: Option[BigInt])

  implicit val spark: SparkSession = SparkSession
    .builder()
    .appName("userDeetProcessor")
    .getOrCreate()

  import spark.implicits._

  val brochureClick: Dataset[BrochureClick] = spark.read.json(brochuresClickSrc).as[BrochureClick]
  val pageTurn: Dataset[PageTurn] = spark.read.json(pageTurnsSrc).as[PageTurn]
  val pageEnter: Dataset[PageEnter] = spark.read.json(pageEntersSrc).as[PageEnter]
  val pageExit: Dataset[PageExit] = spark.read.json(pageExitsSrc).as[PageExit]

  val userDatasets: Seq[Dataset[_ >: PageTurn with PageEnter with PageExit <: PageView]] = Seq(pageTurn, pageEnter, pageExit)

  def userEventAggregator(datasets: => Seq[Dataset[_ >: PageExit with PageEnter with PageTurn <: PageView]]): Seq[Dataset[UserEventTotal]] = datasets.map {
    ds => {
      brochureClick.joinWith(ds, brochureClick.col("brochure_click_uuid") === ds.col("brochure_click_uuid"), "left").map{
        case (click: BrochureClick, turn: PageView) => ClickEvent(click.user_ident, turn.page_view_mode)
        case (click: BrochureClick, null) => ClickEvent(click.user_ident, Some("None"))
      }.map {
        case ClickEvent(user_ident, Some("DOUBLE_PAGE_MODE")) => UserEventCount(user_ident, Some(2))
        case ClickEvent(user_ident, Some("SINGLE_PAGE_MODE")) => UserEventCount(user_ident, Some(1))
        case ClickEvent(user_ident, Some("None")) => UserEventCount(user_ident, Some(0))
      }.groupBy("user_ident").agg(sum("event_count") as "total_events").as[UserEventTotal]
    }
  }


  userEventAggregator(userDatasets).foreach(ds => println(ds.count()))

}
