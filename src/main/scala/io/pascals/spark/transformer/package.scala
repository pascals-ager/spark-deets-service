package io.pascals.spark

import io.pascals.spark.models._
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.functions.sum

import scala.util.{Failure, Success, Try}

package object transformer {


  /**
    * @param  brochureClick BrochureClick Dataset
    * @param  userDataset Seq of Dataset upper bound to trait PageAccess
    * @param  spark implicit spark session
    * @return Seq of UserEventTotal Dataset
    * */

  def userEventAggregator(brochureClick: Dataset[BrochureClick], userDataset: => Seq[Dataset[_ >: PageExit with PageEnter with PageTurn <: PageAccess]])(implicit spark: SparkSession): Try[Seq[Dataset[UserEventTotal]]] = {
    import spark.implicits._
    Try(
      userDataset.map(
        ds => {
          brochureClick.joinWith(ds, brochureClick.col("brochure_click_uuid") === ds.col("brochure_click_uuid"), "left").map {
            case (click: BrochureClick, turn: PageAccess) => ClickEvent(click.user_ident, turn.page_view_mode)
            case (click: BrochureClick, _) => ClickEvent(click.user_ident, Some("None"))
          }.map {
            case ClickEvent(user_ident, Some("DOUBLE_PAGE_MODE")) => UserEventCount(user_ident, Some(2))
            case ClickEvent(user_ident, Some("SINGLE_PAGE_MODE")) => UserEventCount(user_ident, Some(1))
            case ClickEvent(user_ident, Some("None")) => UserEventCount(user_ident, Some(0))
          }.groupBy("user_ident").agg(sum("event_count") as "total_events").as[UserEventTotal]
        }
      )
    )
  }

  /**
    * @param  transformedDS Seq of UserEventTotal Dataset i.e PageTurn, PageEnter and PageExit in that exact order
    * @param  spark implicit spark session
    * @return UserDeetsDescription Dataset
    * */
  def userDetailsMergeFromEvents(transformedDS: Seq[Dataset[UserEventTotal]])(implicit spark: SparkSession): Try[Dataset[UserDeetsDescription]] = {
    /* Not ideal, but statically extracting the transformedDS in order */
    import spark.implicits._
    transformedDS match {
      case pageTurnTransformedDS :: pageEnterTransformedDS :: pageExitTransformedDS :: Nil =>
        val tempJoinDS: Try[Dataset[UserDeetsDescription]] =  Try(
          /* Join the three event Datasets while mapping the individual counts into the appropriate variable of UserDeetsDescription model*/
          pageTurnTransformedDS.joinWith(pageEnterTransformedDS, pageTurnTransformedDS.col("user_ident") === pageEnterTransformedDS.col("user_ident"), "inner").map {
            case (view: UserEventTotal, turns: UserEventTotal) => UserDeetsDescription(view.user_ident, view.total_events, turns.total_events, Some(0))
          })
        tempJoinDS match {
          case Success(tempDS: Dataset[UserDeetsDescription]) =>
            Try(tempDS.joinWith (pageExitTransformedDS, tempDS.col ("user_ident") === pageExitTransformedDS.col ("user_ident"), "inner").map {
              case (deets: UserDeetsDescription, exits: UserEventTotal) => UserDeetsDescription (deets.user_ident, deets.total_views, deets.total_enters, exits.total_events)
            })
          case Failure(ex: Throwable) => Failure(ex)
        }

      case _ => Failure(new Throwable("The Join implementation expects three Datasets in order PageTurn, PageEnter, PageExit"))
    }


  }


}

