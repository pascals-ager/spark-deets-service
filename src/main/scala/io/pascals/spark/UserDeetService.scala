package io.pascals.spark

import com.typesafe.config.{Config, ConfigFactory}
import io.pascals.spark.models._
import io.pascals.spark.utils._
import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}
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

  /* Dataset which is bound to the trait PageAccess. */
  val userDatasets: Seq[Dataset[_ >: PageTurn with PageEnter with PageExit <: PageAccess]] = Seq(pageTurn, pageEnter, pageExit)

  logger.info("Transforming the events datasets into generic UserEventTotal dataset")
  /* Get intermediate counts for all three events -> turns, enters, exits */
  val transformedDS: Seq[Dataset[UserEventTotal]] = userEventAggregator(brochureClick, userDatasets)

  logger.info("Joining the UserEventTotal datasets on user_ident and mapping counts to appropriate variables in UserDeetsDescription")
  val userDeetsDescriptionDS: Dataset[UserDeetsDescription] = userDetailsMergeFromEvents(transformedDS)

  /* Write the dataset to hdfs for easier review */
  userDeetsDescriptionDS.coalesce(1).write.mode(SaveMode.Overwrite).json(userDeetsDest)
}
