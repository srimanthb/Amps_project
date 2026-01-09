package AMPS

import com.crankuptheamps.client._

object ValidationPublisher {
  private val ampsServer = "tcp://192.168.20.60:9007/amps/json"
  private var client: Client = _

  private def init(): Unit = {
    try {
      client = new Client("ValidationPublisher")
      println(s"Publisher connecting to AMPS: $ampsServer")
      client.connect(ampsServer)
      client.logon()
      println("Publisher connected successfully")
    } catch {
      case e: Exception =>
        println(s"Publisher connection error: ${e.getMessage}")
        throw e
    }
  }

  def publish(jsonData: String, topic: String): Unit = {
    if (client == null) {
      init()
    }

    try {
      println(s"Publishing to topic: $topic")
      client.publish(topic, jsonData)
      println("Published successfully")
    } catch {
      case e: Exception =>
        println(s"Publishing error: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  def close(): Unit = {
    if (client != null) {
      client.close()
      println("Publisher disconnected")
    }
  }
}