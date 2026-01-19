package Amps_project

import com.crankuptheamps.client.Client
import play.api.libs.json.Json

object EnrichmentPublisher {

  private var client: Client = _

  def connect(): Unit = {
    client = new Client("EnrichmentPublisher")
    client.connect("tcp://192.168.20.122:9007/amps/json")
    client.logon()
    println("EnrichmentPublisher connected to AMPS")
  }

  def publishEnrichedTrade(trade: Trade): Unit = {
    try {
      val json = Json.toJson(trade).toString()
      client.publish("trades.enriched", json)
      println(s"Published to trades.enriched: ${trade.trade_id}")
    } catch {
      case e: Exception =>
        println(s"Publishing failed: ${e.getMessage}")
    }
  }

  def disconnect(): Unit = {
    if (client != null) {
      client.close()
      println("EnrichmentPublisher disconnected")
    }
  }
}