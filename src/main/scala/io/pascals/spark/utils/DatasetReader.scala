package io.pascals.spark.utils


import java.io.FileNotFoundException

import io.pascals.spark.models._
import io.pascals.spark.utils.ConfigLoad._
import org.apache.spark.sql.{Dataset, SparkSession}
import org.slf4j.{Logger, LoggerFactory}


object DatasetReader {

  val logger: Logger = LoggerFactory.getLogger(getClass)
  def readPageAccessJson(implicit spark: SparkSession): Seq[Dataset[_ >: PageTurn with PageEnter with PageExit <: PageAccess]] = {
    try {
      import spark.implicits._
      val pageTurn: Dataset[PageTurn] = spark.read.json(pageTurnsSrc).as[PageTurn]
      val pageEnter: Dataset[PageEnter] = spark.read.json(pageEntersSrc).as[PageEnter]
      val pageExit: Dataset[PageExit] = spark.read.json(pageExitsSrc).as[PageExit]
      Seq(pageTurn, pageEnter, pageExit)
    }
    catch {
      case exception: FileNotFoundException => {
        logger.error(s"File not found $exception")
        /* Fail early when file not found*/
        throw new FileNotFoundException
      }
    }
  }

  def readBrochureClickJson(implicit spark: SparkSession): Dataset[BrochureClick] = {
    try {
    import spark.implicits._
    val brochureClick: Dataset[BrochureClick] = spark.read.json(brochuresClickSrc).as[BrochureClick]
    brochureClick
  }
    catch {
      case exception: FileNotFoundException => {
        logger.error(s"File not found $exception")
        /* Fail early when file not found*/
        throw new FileNotFoundException
      }
    }
  }

}
