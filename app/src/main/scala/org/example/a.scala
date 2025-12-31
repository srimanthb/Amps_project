package org.example

import com.crankuptheamps.client._

object a {
  def main(args: Array[String]): Unit = {


    val client = new Client("HII")
    client.connect("tcp://192.168.20.118:9007/amps")
    println("Helloo Srimanth")
    client.logon()

    println("Connected!!")
    val handler = new MessageHandler() {
      override def invoke(message: Message): Unit = {
        println("Helloo Srimanth")
        println(message.getData)

      }
    }

    println("Done")
    val cmd = new Command("sub").setTopic("data")
    client.executeAsync(cmd, handler)

    client.wait(7000)
    client.close()
  }
}