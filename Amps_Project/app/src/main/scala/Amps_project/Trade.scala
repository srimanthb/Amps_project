package Amps_project

import play.api.libs.json._

case class Trade(
                  trade_id: Int,
                  symbol: String,
                  side: String,
                  quantity: Double,
                  price: Double,
                  trade_time: String,
                  account_id: String,

                  // Enrichment fields (YOU add these)
                  venue: String = "",
                  currency: String = "",
                  broker_id: String = "",           // ← YOU add this

                  // Validation fields
                  is_valid: Boolean = true,
                  validation_errors: String = "",

                  // Figuration fields
                  gross_amount: Double = 0.0,
                  commission: Double = 0.0,
                  tax: Double = 0.0,
                  net_amount: Double = 0.0,

                  // Settlement field
                  received_time: String = "",       // ← Harish adds this

                  status: String = ""
                )
object Trade {
  implicit val format: OFormat[Trade] = Json.format[Trade]
}