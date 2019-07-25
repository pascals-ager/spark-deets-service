package io.pascals.spark

import io.pascals.spark.models._
import io.pascals.spark.transformer._
import io.pascals.spark.utils.ConfigLoad._
import io.pascals.spark.utils.DatasetReader
import org.apache.spark.SparkException
import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.control.NonFatal

object UserDeetService extends App {

  val logger: Logger = LoggerFactory.getLogger(getClass)
  implicit val spark: SparkSession = SparkSession
    .builder()
    .appName("userDeetProcessor")
    .getOrCreate()

  try {

    logger.info("Reading source files into typed datasets")
    /* Read source files into typed Datasets */
    val brochureClick: Dataset[BrochureClick] = DatasetReader.readBrochureClickJson
    /* Dataset which is bound to the trait PageAccess. */
    val userDatasets: Seq[Dataset[_ >: PageTurn with PageEnter with PageExit <: PageAccess]] = DatasetReader.readPageAccessJson

    logger.info("Transforming the events datasets into generic UserEventTotal dataset")
    /* Get intermediate counts for all three events -> turns, enters, exits */

      for {
        seqPageAccess <- userEventCounter(brochureClick, userDatasets)
        seqUserDetails <- userEventAggregator(seqPageAccess)
        userDetails <- userEventsMerge(seqUserDetails)
      } yield userDetails.coalesce(1).write.mode(SaveMode.Overwrite).json(userDeetsDest)

  }
  catch {
    case exception: SparkException => {
      logger.error(s"Spark Exception occurred. See trace for details $exception")
      throw new SparkException("Spark Exception occurred", exception)
    }
    case NonFatal(exc) => {
      logger.error(s"Unknown exception occured. See the trace for details $exc")
    }
  }
}
