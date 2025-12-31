package org.example

import java.io.PrintWriter
import java.net.{ServerSocket, Socket}
import scala.util.{Try, Using}
import java.sql.DriverManager
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object SimplePublisher {

  private val dbConfig = Map(
    "url" -> "jdbc:sqlserver://192.168.20.122:1433;databaseName=Amps;encrypt=false;",
    "user" -> "sa",
    "password" -> "Srimanth@9",
    "driver" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver"
  )

  private var subscribers: Set[Socket] = Set()
  private val PORT = 9007

  def main(args: Array[String]): Unit = {
    println("=== Starting Simple Publisher ===")
    println(s"Listening on port $PORT")
    println("Your friend should connect to: 192.168.20.122:9007")

    Future {
      acceptSubscribers()
    }

    startDataStream()
  }

  private def acceptSubscribers(): Unit = {
    Using.resource(new ServerSocket(PORT)) { serverSocket =>
      while (true) {
        val socket = serverSocket.accept()
        subscribers += socket
        println(s"New subscriber connected: ${socket.getInetAddress}")

        // Send welcome message
        val writer = new PrintWriter(socket.getOutputStream, true)
        writer.println("CONNECTED: Welcome to Data Stream!")
        writer.println("SERVER: Connected to Amps database")
      }
    }
  }


  private def startDataStream(): Unit = {
    println("Press Enter to send data to subscribers...")

    // Create a simple table structure
    val sampleData = List(
      ("1", "John", "IT", "50000"),
      ("2", "Alice", "HR", "45000"),
      ("3", "Bob", "Sales", "48000")
    )

    while (true) {
      scala.io.StdIn.readLine()  // Wait for Enter key

      // Option 1: Use sample data
      sendDataToSubscribers(sampleData)

      // Option 2: Uncomment to fetch from database
      // fetchDataFromDBAndSend()
    }
  }

  private def sendDataToSubscribers(data: List[(String, String, String, String)]): Unit = {
    println(s"\nSending data to ${subscribers.size} subscriber(s)...")

    val timestamp = new java.util.Date().toString
    val message =
      s"""DATA_START
         |Timestamp: $timestamp
         |Total Records: ${data.size}
         |=== Table Data ===
         |${formatAsTable(data)}
         |DATA_END
         |""".stripMargin

    // Send to all subscribers
    subscribers.foreach { socket =>
      try {
        val writer = new PrintWriter(socket.getOutputStream, true)
        writer.println(message)
        println(s"Data sent to ${socket.getInetAddress}")
      } catch {
        case e: Exception =>
          println(s"Failed to send to ${socket.getInetAddress}: ${e.getMessage}")
          subscribers -= socket  // Remove disconnected subscriber
      }
    }
  }

  def fetchDataFromDBAndSend(): Unit = {
    println("Fetching data from database...")

    Try {
      Class.forName(dbConfig("driver"))
      val conn = DriverManager.getConnection(
        dbConfig("url"),
        dbConfig("user"),
        dbConfig("password")
      )

      val stmt = conn.createStatement()
      // CHANGE THIS QUERY TO YOUR TABLE
      val query = "SELECT TOP 3 * FROM YourTableName"  // EDIT TABLE NAME
      val rs = stmt.executeQuery(query)

      var data = List[(String, String, String, String)]()
      while (rs.next()) {
        // CHANGE THESE COLUMN NAMES
        val row = (
          rs.getString("id"),
          rs.getString("name"),
          rs.getString("department"),
          rs.getString("salary")
        )
        data = row :: data
      }

      rs.close()
      stmt.close()
      conn.close()

      sendDataToSubscribers(data.reverse)

    }.recover {
      case e: Exception =>
        println(s"Database error: ${e.getMessage}")
        println("Using sample data instead...")
        val sampleData = List(
          ("1", "John", "IT", "50000"),
          ("2", "Alice", "HR", "45000"),
          ("3", "Bob", "Sales", "48000")
        )
        sendDataToSubscribers(sampleData)
    }
  }

  private def formatAsTable(data: List[(String, String, String, String)]): String = {
    val header = "ID\tName\tDepartment\tSalary"
    val rows = data.map { case (id, name, dept, salary) =>
      s"$id\t$name\t$dept\t$$$salary"
    }.mkString("\n")

    header + "\n" + "-"*40 + "\n" + rows
  }
}

