package io.pascals.spark.test

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import io.pascals.spark.models._
import io.pascals.spark.utils._
import org.apache.spark.sql.Dataset

class UserDeetServiceTest extends FunSuite with BeforeAndAfterAll with Matchers with SparkSessionTestWrapper{

  override def beforeAll: Unit = {

  }
  val brochuresClickSrc: String = getClass.getResource("/brochure_clicks.json").getPath
  val pageTurnsSrc: String = getClass.getResource("/page_turns.json").getPath
  val pageEntersSrc: String = getClass.getResource("/enters.json").getPath
  val pageExitsSrc: String = getClass.getResource("/exits.json").getPath
  val spark = sparkBuilder.getOrCreate()

  test ("ConfigsTest") {
    val appConfig: Config = ConfigFactory.parseResources("test.conf")
    appConfig.checkValid(ConfigFactory.defaultReference(), "source-conf")

    /* Initialize required configs */
    val hdfsURI = appConfig.getString("source-conf.node_master")
    val brochuresClickSrc = hdfsURI.concat(appConfig.getString("source-conf.brochure_click"))
    val pageTurnsSrc = hdfsURI.concat(appConfig.getString("source-conf.page_turn"))
    val pageEntersSrc = hdfsURI.concat(appConfig.getString("source-conf.page_enter"))
    val pageExitsSrc = hdfsURI.concat(appConfig.getString("source-conf.page_exit"))
    val userDeetsDest = hdfsURI.concat(appConfig.getString("source-conf.user_details"))

    hdfsURI should equal("hdfs://node-master:8020")
    brochuresClickSrc should equal("hdfs://node-master:8020/tmp/brochure_clicks.json")
    pageTurnsSrc should equal("hdfs://node-master:8020/tmp/page_turns.json")
    pageEntersSrc should equal("hdfs://node-master:8020/tmp/enters.json")
    pageExitsSrc should equal("hdfs://node-master:8020/tmp/exits.json")
    userDeetsDest should equal("hdfs://node-master:8020/tmp/user_details")
  }

  test ("Basic read counts") {
    import spark.implicits._

    /* Read source files into typed Datasets */
    val brochureClick: Dataset[BrochureClick] = spark.read.json(brochuresClickSrc).as[BrochureClick]
    val pageTurn: Dataset[PageTurn] = spark.read.json(pageTurnsSrc).as[PageTurn]
    val pageEnter: Dataset[PageEnter] = spark.read.json(pageEntersSrc).as[PageEnter]
    val pageExit: Dataset[PageExit] = spark.read.json(pageExitsSrc).as[PageExit]

    brochureClick.count() should equal(30)
    pageTurn.count() should equal(317)
    pageEnter.count() should equal(30)
    pageExit.count() should equal(23)
    brochureClick.select("user_ident").distinct().count() should equal(23)

  }

  test( "Events aggregate test" ) {

    import spark.implicits._

    /* Read source files into typed Datasets */
    val brochureClick: Dataset[BrochureClick] = spark.read.json(brochuresClickSrc).as[BrochureClick]
    val pageTurn: Dataset[PageTurn] = spark.read.json(pageTurnsSrc).as[PageTurn]
    val pageEnter: Dataset[PageEnter] = spark.read.json(pageEntersSrc).as[PageEnter]
    val pageExit: Dataset[PageExit] = spark.read.json(pageExitsSrc).as[PageExit]
    val userDatasets: Seq[Dataset[_ >: PageTurn with PageEnter with PageExit <: PageAccess]] = Seq(pageTurn, pageEnter, pageExit)

    val transformedDS: Seq[Dataset[UserEventTotal]] = userEventAggregator(brochureClick, userDatasets)(spark)
    transformedDS.head.count() should equal(23)
    transformedDS.head.columns should equal(Array("user_ident", "total_events"))
    transformedDS.tail.head.count() should equal(23)
    transformedDS.tail.head.columns should equal(Array("user_ident", "total_events"))
    transformedDS.tail.tail.head.count() should equal(23)
    transformedDS.tail.tail.head.columns should equal(Array("user_ident", "total_events"))

  }

  test( "User Details test" ) {

    import spark.implicits._

    /* Read source files into typed Datasets */
    val brochureClick: Dataset[BrochureClick] = spark.read.json(brochuresClickSrc).as[BrochureClick]
    val pageTurn: Dataset[PageTurn] = spark.read.json(pageTurnsSrc).as[PageTurn]
    val pageEnter: Dataset[PageEnter] = spark.read.json(pageEntersSrc).as[PageEnter]
    val pageExit: Dataset[PageExit] = spark.read.json(pageExitsSrc).as[PageExit]
    val userDatasets: Seq[Dataset[_ >: PageTurn with PageEnter with PageExit <: PageAccess]] = Seq(pageTurn, pageEnter, pageExit)

    val transformedDS: Seq[Dataset[UserEventTotal]] = userEventAggregator(brochureClick, userDatasets)(spark)
    val userDeetsDescriptionDS: Dataset[UserDeetsDescription] = userDetailsMergeFromEvents(transformedDS)(spark)
    userDeetsDescriptionDS.count() should equal(23)
    userDeetsDescriptionDS.columns should equal(Array("user_ident", "total_views", "total_enters", "total_exits"))

  }

   override def afterAll {
     spark.close()
  }





}
