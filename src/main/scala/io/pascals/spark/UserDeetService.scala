package io.pascals.spark

import com.typesafe.config.{Config, ConfigFactory}

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

}
