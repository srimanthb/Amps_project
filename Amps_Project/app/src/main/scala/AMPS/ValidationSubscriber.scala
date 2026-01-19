package AMPS

import com.crankuptheamps.client._

object ValidationSubscriber {

  def main(args: Array[String]): Unit = {
    println("Validation Service starting...")

    val validationRules = DatabaseConfig.loadValidationRules()
    println(s"Rules loaded for: ${validationRules.keys.mkString(", ")}")

    val ampsServer = "tcp://192.168.20.184:9007/amps/json"
    val subscribeTopic = "trades.raw"
    val publishTopic = "validated"

    val client = new Client("ValidationService")

    try {
      println(s"Connecting to AMPS: $ampsServer")
      client.connect(ampsServer)
      client.logon()
      println("Connected to AMPS")

      println(s"Subscribing to: $subscribeTopic")

      val command = new Command("subscribe")
      command.setTopic(subscribeTopic)

      val messageStream = client.execute(command)

      println("Successfully subscribed")
      println("Waiting for messages from Vinesh...")
      println("Will run for 2 minutes then terminate")

      val iterator = messageStream.iterator()
      val startTime = System.currentTimeMillis()
      val timeout = 2 * 60 * 1000 // 2 minutes in milliseconds

      while (iterator.hasNext && (System.currentTimeMillis() - startTime) < timeout) {
        val message = iterator.next()
        processMessage(message, client, validationRules, publishTopic)
      }

      println(s"\nTimeout reached (${timeout/1000} seconds). Stopping...")

    } catch {
      case e: Exception =>
        println(s"Error: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      client.close()
      println("Disconnected from AMPS")
      println("Validation Service stopped")
    }
  }

  private def processMessage(
                              message: Message,
                              client: Client,
                              validationRules: Map[String, DatabaseConfig.ValidationRule],
                              publishTopic: String
                            ): Unit = {
    try {
      println("\n" + "=" * 60)
      println("RAW DATA RECEIVED FROM VINESH:")
      println("=" * 60)

      val jsonData = extractDataFromMessage(message)
      println(jsonData)
      println("=" * 60)

      println("\nPARSING JSON DATA...")
      import play.api.libs.json._
      val json = Json.parse(jsonData)

      println("JSON Fields found:")
      json.as[JsObject].fields.foreach { case (key, value) =>
        println(s"  $key: $value (type: ${value.getClass.getSimpleName})")
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
        println(s"General error: ${e.getMessage}")
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
