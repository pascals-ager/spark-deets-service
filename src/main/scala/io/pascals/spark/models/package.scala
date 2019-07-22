package io.pascals.spark

package object models {


  /* Common trait to model page access */
  trait PageAccess {
    def page: Option[Long]

    def page_view_mode: Option[String]

    def user_agent: Option[String]
  }

  /* Data model definitions. These could be generated from Schema files */
  case class BrochureClick(brochure_click_id: Option[Long], brochure_click_uuid: Option[String], brochure_id: Option[Long], date_time: Option[String], event: Option[String], ip: Option[String], location_device_lat: Option[Double], location_device_lng: Option[Double], location_intended_lat: Option[Double], location_intended_lng: Option[Double], no_profile: Option[Boolean], page: Option[Long], page_type: Option[String], restricted_ip: Option[Boolean], traffic_source_type: Option[String], traffic_source_value: Option[String], treatment: Option[String], user_agent: Option[String], user_ident: Option[String], user_zip: Option[String], visit_id: Option[Long], visit_origin_type: Option[String])

  case class PageTurn(brochure_click_uuid: Option[String], date_time: Option[String], event: Option[String], ip_address: Option[String], override val page: Option[Long], override val page_view_mode: Option[String], override val user_agent: Option[String]) extends PageAccess

  case class PageEnter(brochure_click_uuid: Option[String], date_time: Option[String], event: Option[String], ip_address: Option[String], override val page: Option[Long], override val page_view_mode: Option[String], screen_size: Option[String], override val user_agent: Option[String]) extends PageAccess

  case class PageExit(brochure_click_uuid: Option[String], date_time: Option[String], event: Option[String], ip_address: Option[String], override val page: Option[Long], override val page_view_mode: Option[String], override val user_agent: Option[String]) extends PageAccess

  /* Intermediate definitions to do typed transformations and actions */
  /* An Event can be a Page Turn, Enter or Exit, all of which extend trait PageView */
  case class ClickEvent(user_ident: Option[String], page_view_mode: Option[String])

  case class UserEventCount(user_ident: Option[String], event_count: Option[Int])

  case class UserEventTotal(user_ident: Option[String], total_events: Option[BigInt])

  /* Data model of the final result Dataset*/
  case class UserDeetsDescription(user_ident: Option[String], total_views: Option[BigInt], total_enters: Option[BigInt], total_exits: Option[BigInt])

}
