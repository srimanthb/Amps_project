package AMPS

import play.api.libs.json._

object TradeJsonParser {
  implicit val tradeFormat: Format[Trade] = Json.format[Trade]

  def parseTrade(jsonString: String): Trade = {
    val json = Json.parse(jsonString)
    json.as[Trade]
  }

  def toJson(trade: Trade): String = {
    Json.stringify(Json.toJson(trade))
  }
}



