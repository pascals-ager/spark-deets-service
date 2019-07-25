package io.pascals.spark

package object models {

  sealed trait EventType {
    def event: Option[String]
  }

  case object BROCHURE_CLICK extends EventType { val event = Some("brochure_click") }
  case object ENTER_VIEW extends EventType { val event = Some("ENTER_VIEW") }
  case object EXIT_VIEW extends EventType { val event = Some("EXIT_VIEW") }
  case object PAGE_TURN extends EventType { val event = Some("PAGE_TURN") }
  case object UNDEFINED extends EventType { val event = Some("UNDEFINED") }

  /* Common trait to model page access */
  sealed trait PageAccess extends EventType{
    def page: Option[Long]

    def page_view_mode: Option[String]
  }

  /* Data model definitions. These could be generated from Schema files */
  sealed case class BrochureClick(brochure_click_id: Option[Long], brochure_click_uuid: Option[String], brochure_id: Option[Long], date_time: Option[String], event: Option[String], ip: Option[String], location_device_lat: Option[Double], location_device_lng: Option[Double], location_intended_lat: Option[Double], location_intended_lng: Option[Double], no_profile: Option[Boolean], page: Option[Long], page_type: Option[String], restricted_ip: Option[Boolean], traffic_source_type: Option[String], traffic_source_value: Option[String], treatment: Option[String], user_agent: Option[String], user_ident: Option[String], user_zip: Option[String], visit_id: Option[Long], visit_origin_type: Option[String]) extends EventType

  sealed case class PageTurn(brochure_click_uuid: Option[String], date_time: Option[String], override val event: Option[String], ip_address: Option[String], override val page: Option[Long], override val page_view_mode: Option[String], user_agent: Option[String]) extends PageAccess

  sealed case class PageEnter(brochure_click_uuid: Option[String], date_time: Option[String], override val event: Option[String], ip_address: Option[String], override val page: Option[Long], override val page_view_mode: Option[String], screen_size: Option[String], user_agent: Option[String]) extends PageAccess

  sealed case class PageExit(brochure_click_uuid: Option[String], date_time: Option[String], override val event: Option[String], ip_address: Option[String], override val page: Option[Long], override val page_view_mode: Option[String],  user_agent: Option[String]) extends PageAccess

  /* Intermediate definitions to do typed transformations and actions */
  /* An Event can be a Page Turn, Enter or Exit, all of which extend trait PageAccess */
  sealed case class PageAccessEvent(user_ident: Option[String],  page_view_mode: Option[String], override val event: Option[String]) extends EventType

  sealed case class PageAccessEventCount(user_ident: Option[String], event_count: Option[Int], override val event: Option[String]) extends EventType

  sealed case class UserEventTotal(user_ident: Option[String], total_events: Option[BigInt], override val event: Option[String]) extends EventType

  sealed case class PageTurnTotal(user_ident: Option[String], total_views: Option[BigInt])
  sealed case class PageEnterTotal(user_ident: Option[String], total_enters: Option[BigInt])
  sealed case class PageExitTotal(user_ident: Option[String], total_exits: Option[BigInt])
  sealed case class UndefinedTotal(user_ident: Option[String], total_undefined: Option[BigInt])


  /* Data model of the final result Dataset*/
  sealed case class UserDeetsDescription(user_ident: Option[String], total_views: Option[BigInt], total_enters: Option[BigInt], total_exits: Option[BigInt], total_undefined: Option[String])

}
