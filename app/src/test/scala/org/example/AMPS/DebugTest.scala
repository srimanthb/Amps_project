package org.example.AMPS

import AMPS.{DatabaseConfig, Trade, TradeJsonParser, ValidationService}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DebugTest extends AnyFlatSpec with Matchers {

  "TradeJsonParser" should "parse the JSON correctly" in {
    val jsonString = """
      {
        "trade_id": 2001,
        "order_id": "ORD2001",
        "execution_id": "EXEC2001",
        "symbol": "GOOGL",
        "side": "BUY",
        "quantity": 50,
        "price": 1500,
        "trade_time": "2024-01-16",
        "venue": "NASDAQ",
        "currency": "USD",
        "account_id": "ACC2001",
        "broker_id": "BROK2001",
        "commission": 75,
        "tax": 135,
        "gross_amount": 75000,
        "net_amount": 74790,
        "received_time": "2024-01-16",
        "status": "ENRICHED"
      }
    """

    println("=== Testing JSON Parsing ===")
    println(s"JSON string: $jsonString")

    try {
      val trade = TradeJsonParser.parseTrade(jsonString)
      println("SUCCESS: Trade parsed")
      println(s"Trade: $trade")
      println(s"Symbol: ${trade.symbol}")
      println(s"Quantity: ${trade.quantity} (type: ${trade.quantity.getClass})")
      println(s"Expected quantity: 50")
      println(s"Quantity matches: ${trade.quantity == BigDecimal(50)}")
    } catch {
      case e: Exception =>
        println(s"ERROR parsing JSON: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  "ValidationService" should "debug validation logic" in {
    val aaplRule = DatabaseConfig.ValidationRule(
      symbol = "AAPL",
      minQuantity = BigDecimal(10),
      maxQuantity = BigDecimal(10000),
      minPrice = BigDecimal(100),
      maxPrice = BigDecimal(500),
      allowedSides = List("BUY", "SELL"),
      allowedCurrencies = List("USD"),
      maxCommissionRate = BigDecimal(2),
      taxRate = BigDecimal(15)
    )

    val validationRules = Map("AAPL" -> aaplRule)

    // Test Case 1: Valid trade
    val validTrade = Trade(
      trade_id = 1001,
      order_id = "ORD001",
      execution_id = "EXEC001",
      symbol = "AAPL",
      side = "BUY",
      quantity = BigDecimal(100),
      price = BigDecimal(150),
      trade_time = "2024-01-15",
      venue = "NYSE",
      currency = "USD",
      account_id = "ACC001",
      broker_id = "BROK001",
      commission = BigDecimal(10),
      tax = BigDecimal(22.5),
      gross_amount = BigDecimal(15000),
      net_amount = BigDecimal(14967.5),
      received_time = "2024-01-15",
      status = "ENRICHED"
    )

    println("\n=== Test 1: Valid Trade ===")
    val result1 = ValidationService.validateTrade(validTrade, validationRules)
    println(s"Valid: ${result1.isValid}")
    println(s"Errors: ${result1.errors}")
    println(s"Warnings: ${result1.warnings}")

    // Test Case 2: Invalid symbol
    val invalidSymbolTrade = Trade(
      trade_id = 1002,
      order_id = "ORD002",
      execution_id = "EXEC002",
      symbol = "MSFT",
      side = "SELL",
      quantity = BigDecimal(50),
      price = BigDecimal(300),
      trade_time = "2024-01-15",
      venue = "NASDAQ",
      currency = "USD",
      account_id = "ACC002",
      broker_id = "BROK002",
      commission = BigDecimal(15),
      tax = BigDecimal(45),
      gross_amount = BigDecimal(15000),
      net_amount = BigDecimal(14940),
      received_time = "2024-01-15",
      status = "ENRICHED"
    )

    println("\n=== Test 2: Invalid Symbol ===")
    val result2 = ValidationService.validateTrade(invalidSymbolTrade, validationRules)
    println(s"Valid: ${result2.isValid}")
    println(s"Errors: ${result2.errors}")
    println(s"Expected error contains: 'Symbol 'MSFT' not found'")
    println(s"Actual error message: ${result2.errors.headOption.getOrElse("No error")}")
  }
}