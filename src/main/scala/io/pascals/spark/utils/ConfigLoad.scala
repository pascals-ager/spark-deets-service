package io.pascals.spark.utils

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

object ConfigLoad {
  val logger: Logger = LoggerFactory.getLogger(getClass)
  /* Define config file and check for existence of a config property */
  val appConfig: Config = ConfigFactory.parseResources("defaults.conf")
  appConfig.checkValid(ConfigFactory.defaultReference(), "source-conf")

  logger.info("Initialized source and destination file configurations")
  /* Initialize required configs */
  val hdfsURI: String = appConfig.getString("source-conf.node_master")
  val brochuresClickSrc: String = hdfsURI.concat(appConfig.getString("source-conf.brochure_click"))
  val pageTurnsSrc: String = hdfsURI.concat(appConfig.getString("source-conf.page_turn"))
  val pageEntersSrc: String = hdfsURI.concat(appConfig.getString("source-conf.page_enter"))
  val pageExitsSrc: String = hdfsURI.concat(appConfig.getString("source-conf.page_exit"))
  val userDeetsDest: String = hdfsURI.concat(appConfig.getString("source-conf.user_details"))
}
