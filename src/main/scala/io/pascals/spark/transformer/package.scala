package io.pascals.spark

import io.pascals.spark.models._
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

package object transformer {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  /**
    * @param  brochureClick BrochureClick Dataset
    * @param  userDataset Seq of Dataset upper bound to trait PageAccess
    * @param  spark implicit spark session
    * @return Seq of UserEventTotal Dataset
    * */

  def userEventCounter(brochureClick: Dataset[BrochureClick], userDataset: => Seq[Dataset[_ >: PageExit with PageEnter with PageTurn <: PageAccess]])(implicit spark: SparkSession): Try[Seq[Dataset[PageAccessEventCount]]] = {
    import spark.implicits._
    Try(
      userDataset.map(
        ds => {
          brochureClick.joinWith(ds, brochureClick.col("brochure_click_uuid") === ds.col("brochure_click_uuid"), "left").map {
            case (click: BrochureClick, turn: PageAccess) => {
              logger.info(s"matched to a PageAccess event of type ${turn.event}")
              PageAccessEvent(click.user_ident, turn.page_view_mode, turn.event)
            }
            case (click: BrochureClick, _) => {
              logger.info(s"matched to another undefined event")
              PageAccessEvent(click.user_ident, Some("None"), UNDEFINED.event)
            }
          }.map {
            case PageAccessEvent(user_ident, Some("DOUBLE_PAGE_MODE"), event) => PageAccessEventCount(user_ident, Some(2), event)
            case PageAccessEvent(user_ident, Some("SINGLE_PAGE_MODE"), event) => PageAccessEventCount(user_ident, Some(1), event)
            case PageAccessEvent(user_ident, Some(_), event) => PageAccessEventCount(user_ident, Some(0), event)
            case PageAccessEvent(user_ident, None, event) => PageAccessEventCount(user_ident, Some(0), event)
          }.as[PageAccessEventCount]
        }
      )
    )
  }

  /**
    * @param  pageAccessCounts Seq of PageAccessEventCount Dataset
    * @param  spark implicit spark session
    * @return Seq of UserDeetsDescription Dataset
    * */

  def userEventAggregator(pageAccessCounts: Seq[Dataset[PageAccessEventCount]])(implicit spark: SparkSession): Try[Seq[Dataset[UserDeetsDescription]]] = {
    import spark.implicits._
    Try(
    pageAccessCounts.map{
      pa =>
        pa.filter(!($"event" === typedLit(UNDEFINED.event))).head match {
        case PageAccessEventCount(_, _ , PAGE_TURN.event) => pa.groupBy("user_ident").agg(sum("event_count") as "total_views")
            .withColumn("total_enters", typedLit(Some(0)))
           .withColumn("total_exits", typedLit(Some(0)))
          .withColumn("total_undefined", typedLit(Some(0)))
          .as[UserDeetsDescription]
        case PageAccessEventCount(_, _ , ENTER_VIEW.event) => pa.groupBy("user_ident").agg(sum("event_count") as "total_enters")
          .withColumn("total_views", typedLit(Some(0)))
          .withColumn("total_exits", typedLit(Some(0)))
          .withColumn("total_undefined", typedLit(Some(0)))
          .as[UserDeetsDescription]
        case PageAccessEventCount(_, _ , EXIT_VIEW.event) => pa.groupBy("user_ident").agg(sum("event_count") as "total_exits")
          .withColumn("total_views", typedLit(Some(0)))
          .withColumn("total_enters", typedLit(Some(0)))
          .withColumn("total_undefined", typedLit(Some(0)))
          .as[UserDeetsDescription]
        case _ => spark.emptyDataset[UserDeetsDescription]
      }
    }
    )
  }

  /**
    * @param  userEventTotals Seq of UserEventTotal Dataset i.e PageTurn, PageEnter and PageExit in that exact order
    * @return UserDeetsDescription Dataset
    * */
  def userEventsMerge(userEventTotals: Seq[Dataset[UserDeetsDescription]])(implicit spark: SparkSession): Try[Dataset[UserDeetsDescription]] = {
    import spark.implicits._
    Try(
      userEventTotals.reduce{
        (ue1: Dataset[UserDeetsDescription], ue2: Dataset[UserDeetsDescription]) =>
            ue1.unionByName(ue2).groupBy("user_ident").agg(
              max("total_views").as("total_views"),
              max("total_enters").as("total_enters"),
              max("total_exits").as("total_exits"),
              max("total_undefined").as("total_undefined")
            ).as[UserDeetsDescription]
      }
    )
  }


}

