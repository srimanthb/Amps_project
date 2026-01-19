package Amps_project

import com.crankuptheamps.client._

object SimpleSubscriber {
  @throws[Exception]
  def main(args: Array[String]): Unit = {

    val client = new Client("SimpleSub")
    println("Connecting to AMPS...")
    client.connect("tcp://192.168.20.184:9007/amps")
    client.logon()
    println("Connected!")

    // Message handler
    val handler = new MessageHandler() {
      override def invoke(msg: Message): Unit = {
        println("\nRECEIVED MESSAGE:")
        println("Topic: " + msg.getTopic)
        println("Command: " + msg.getCommand)
        println("Data: " + msg.getData)
        println("---\n")
      }
    }

    println("Subscribing to 'data' topic...")
    println("Now run your Scala publisher in IntelliJ!")
    println("Waiting for messages...\n")

    // Use the working executeAsync method
    val cmd = new Command("subscribe").setTopic("data")
    client.executeAsync(cmd, handler)

    // Keep program alive
    Thread.sleep(100000)
    println("Finished.")
    client.close()
  }
}