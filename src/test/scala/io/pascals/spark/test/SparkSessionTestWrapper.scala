package io.pascals.spark.test

import org.apache.spark.sql.SparkSession

trait SparkSessionTestWrapper {

  lazy val spark: SparkSession = {
    SparkSession
      .builder()
      .master("local")
      .appName("user deets spark test")
      .getOrCreate()
  }

}
