package io.pascals.spark.test

import org.scalatest.{FunSpec, FunSuite, Matchers}
import io.pascals.spark.models._
import org.apache.spark.sql.Dataset

class UserDeetServiceTest extends FunSpec with Matchers with SparkSessionTestWrapper{

  import spark.implicits._

  it ("Let us see ") {
    val brochuresClickSrc = getClass.getResource("/brochure_clicks.json").getPath
    val pageTurnsSrc = getClass.getResource("/page_turns.json").getPath
    val pageEntersSrc = getClass.getResource("/enters.json").getPath
    val pageExitsSrc = getClass.getResource("/exits.json").getPath
    /* Read source files into typed Datasets */
    val brochureClick: Dataset[BrochureClick] = spark.read.json(brochuresClickSrc).as[BrochureClick]
    val pageTurn: Dataset[PageTurn] = spark.read.json(pageTurnsSrc).as[PageTurn]
    val pageEnter: Dataset[PageEnter] = spark.read.json(pageEntersSrc).as[PageEnter]
    val pageExit: Dataset[PageExit] = spark.read.json(pageExitsSrc).as[PageExit]

   brochureClick.count() should equal(30)
   println(pageTurn.count())
    println(pageEnter.count())
    println(pageExit.count())
  }


}
