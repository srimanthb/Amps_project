package AMPS

import com.crankuptheamps.client._
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import play.api.libs.json._

object ValidationSubscriber {

  private var isRunning = true

  def main(args: Array[String]): Unit = {
    println("Validation Service starting...")

    val validationRules = DatabaseConfig.loadValidationRules()
    println(s"Rules loaded for: ${validationRules.keys.mkString(", ")}")

    val ampsServer = "tcp://192.168.20.60:9007/amps/json"
    val subscribeTopic = "trades.enriched"
    val publishTopic = "trades.validated"

    val client = new Client("ValidationService")

    try {
      println(s"Connecting to AMPS: $ampsServer")
      client.connect(ampsServer)
      client.logon()
      println("Connected to AMPS")

      val scheduler = Executors.newScheduledThreadPool(1)

      val task = new Runnable {
        def run(): Unit = {
          if (isRunning) {
            checkForNewTrades(client, validationRules, publishTopic, subscribeTopic)
          }
        }
      }

      scheduler.scheduleAtFixedRate(task, 0, Long.MaxValue, TimeUnit.SECONDS)

      println(s"Scheduler started - checking $subscribeTopic every 5 seconds for NEW trades")

      Thread.sleep(2 * 60 * 1000)

      isRunning = false
    }
  }

  private def checkForNewTrades(
                                 client: Client,
                                 validationRules: Map[String, DatabaseConfig.ValidationRule],
                                 publishTopic: String,
                                 subscribeTopic: String
                               ): Unit = {
    try {
      val messageHandler = new MessageHandler {
        override def invoke(message: Message): Unit = {
          processTradeMessage(message, client, validationRules, publishTopic)
        }
      }

      val command = new Command("subscribe")
        .setTopic(subscribeTopic)
        .setFilter("/status = 'ENRICHED'")
        .setCommandId(s"check_new_trades_${System.currentTimeMillis()}")
        .setTimeout(5000)

      client.executeAsync(command, messageHandler)

      Thread.sleep(1000)

    } catch {
      case _: com.crankuptheamps.client.exception.InvalidTopicException =>
        println(s"[${new java.util.Date()}] Topic '$subscribeTopic' not found. Waiting for it to be created...")
      case e: Exception if e.getMessage.contains("timeout") =>
        println(s"[${new java.util.Date()}] No ENRICHED trades available")
      case e: Exception =>
        println(s"[${new java.util.Date()}] Error checking for trades: ${e.getMessage}")
    }
  }

  private def processTradeMessage(
                                   message: Message,
                                   client: Client,
                                   validationRules: Map[String, DatabaseConfig.ValidationRule],
                                   publishTopic: String
                                 ): Unit = {
    try {
      println("\n" + "=" * 60)
      println(s"[${new java.util.Date()}] NEW TRADE RECEIVED")
      println("=" * 60)

      val jsonData = extractDataFromMessage(message)
      println("RAW DATA:")
      println(jsonData)
      println("=" * 60)

      println("\nPARSING JSON DATA...")
      val json = Json.parse(jsonData)

      val status = (json \ "status").asOpt[String].getOrElse("")
      if (status.toUpperCase != "ENRICHED") {
        println(s"SKIPPING: Trade status is '$status', not ENRICHED")
        return
      }

      println("JSON Fields found:")
      json.as[JsObject].fields.foreach { case (key, value) =>
        println(s"  $key: $value")
      }

      println("\nATTEMPTING TO PARSE AS TRADE...")

      try {
        val trade = TradeJsonParser.parseTrade(jsonData)
        println("SUCCESS: Trade parsed correctly")
        println(s"Trade ID: ${trade.trade_id}")
        println(s"Symbol: ${trade.symbol}")
        println(s"Quantity: ${trade.quantity}")
        println(s"Price: ${trade.price}")
        println(s"Side: ${trade.side}")
        println(s"Status: ${trade.status}")

        if (trade.status.toUpperCase != "ENRICHED") {
          println(s"ERROR: Trade status changed to '${trade.status}', skipping validation.")
          return
        }

        println("\nVALIDATING TRADE...")
        val validationResult = ValidationService.validateTrade(trade, validationRules)
        ValidationService.printValidationResult(trade, validationResult)

        if (validationResult.isValid) {
          val validatedTrade = ValidationService.addValidationStatus(trade, validationResult)
          val validatedJson = TradeJsonParser.toJson(validatedTrade)

          ValidationPublisher.publish(validatedJson, publishTopic)
        }

      } catch {
        case e: Exception =>
          println(s"ERROR PARSING TRADE: ${e.getMessage}")
      }

      println("\n" + "=" * 60)

    } catch {
      case e: Exception =>
        println(s"Error processing trade message: ${e.getMessage}")
    }
  }

  private def extractDataFromMessage(message: Message): String = {
    try {
      val data = message.getData
      if (data != null && data.nonEmpty) return data
    } catch {
      case e: Exception =>
        println(s"getData error: ${e.getMessage}")
    }

    "{}"
  }
}