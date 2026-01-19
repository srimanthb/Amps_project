package Amps_project

import com.crankuptheamps.client.{Client, Command, Message, MessageHandler}
import play.api.libs.json.Json

object EnrichmentSubscriber {

  def main(args: Array[String]): Unit = {
    println("==================================================")
    println("ENRICHMENT SERVICE - Srimanth")
    println("==================================================")

    println("Connecting Publisher...")
    EnrichmentPublisher.connect()

    println("Connecting Subscriber...")
    val subscriber = new Client("EnrichmentSubscriber")
    subscriber.connect("tcp://192.168.20.169:9007/amps/json")
    subscriber.logon()
    println("Subscriber connected!")

    val handler = new MessageHandler() {
      override def invoke(msg: Message): Unit = {
        println("\n" + "-" * 50)
        println("RECEIVED RAW TRADE:")
        println(s"Topic: ${msg.getTopic}")

        val data = msg.getData

        try {
          val trade = Json.parse(data).as[Trade]
          println(s"Trade ID   : ${trade.trade_id}")
          println(s"Symbol     : ${trade.symbol}")
          println(s"Side       : ${trade.side}")
          println(s"Quantity   : ${trade.quantity}")
          println(s"Price      : ${trade.price}")
          println(s"Account    : ${trade.account_id}")
          println(s"Status     : ${trade.status}")
        } catch {
          case _: Exception => println(s"Raw data: ${data.take(100)}...")
        }

        println("-" * 30)

        println("Doing enrichment...")
        val enrichedTrade = enrichTrade(data)

        println("Calling Publisher to send to next stage...")
        EnrichmentPublisher.publishEnrichedTrade(enrichedTrade)

        println("-" * 50)
      }
    }

    println("\nSubscribing to 'trades.raw' topic...")
    println("Will auto-call Publisher for each trade")
    println("Waiting for messages...")

    val cmd = new Command("subscribe").setTopic("trades.raw")
    subscriber.executeAsync(cmd, handler)

    Thread.sleep(300000)

    println("\nStopping...")
    subscriber.close()
    EnrichmentPublisher.disconnect()
    println("Service stopped.")
  }

  private def enrichTrade(rawJson: String): Trade = {
    try {
      // Parse the raw JSON to get a Trade object
      val rawTrade = Json.parse(rawJson).as[Trade]

      println(s"\nProcessing trade ID: ${rawTrade.trade_id}")
      println(s"Symbol: ${rawTrade.symbol}")

      // Get venue and currency based on symbol
      val (venue, currency) = getVenueAndCurrency(rawTrade.symbol)

      // Generate broker_id based on venue
      val brokerId = getBrokerId(venue)

      println(s"Adding: venue=$venue, currency=$currency, broker_id=$brokerId")

      // Return the ENRICHED Trade object (not a String!)
      rawTrade.copy(
        venue = venue,
        currency = currency,
        broker_id = brokerId,
        status = "ENRICHED"
      )

    } catch {
      case e: Exception =>
        println(s"Enrichment failed: ${e.getMessage}")
        // If parsing fails, try to return the original trade
        try {
          Json.parse(rawJson).as[Trade]
        } catch {
          case _: Exception =>
            // Create a default trade if everything fails
            Trade(
              trade_id = 0,
              symbol = "ERROR",
              side = "",
              quantity = 0.0,
              price = 0.0,
              trade_time = "",
              account_id = "",
              status = "ENRICHMENT_FAILED"
            )
        }
    }
  }

  private def getVenueAndCurrency(symbol: String): (String, String) = {
    symbol match {
      case "USDINR" => ("NSE", "USD")
      case "TCS"    => ("NSE", "INR")
      case "RELIANCE" => ("BSE", "INR")
      case "AAPL"   => ("NASDAQ", "USD")
      case _        => ("UNKNOWN", "USD")
    }
  }

  private def getBrokerId(venue: String): String = {
    venue match {
      case "NSE" => "NSE_BROKER_001"
      case "BSE" => "BSE_BROKER_001"
      case "NASDAQ" => "NASDAQ_BROKER_001"
      case _ => "DEFAULT_BROKER"
    }
  }
}