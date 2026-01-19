//package org.example
//
//import AMPS.{DatabaseConfig, Trade, ValidationService}
//import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.matchers.should.Matchers
//
//class SimpleValidationTest extends AnyFlatSpec with Matchers {
//
//  private val sampleRules: Map[String, DatabaseConfig.ValidationRule] = Map(
//    "AAPL" -> DatabaseConfig.ValidationRule(
//      symbol = "AAPL",
//      minQuantity = BigDecimal(1),
//      maxQuantity = BigDecimal(10000),
//      minPrice = BigDecimal(100),
//      maxPrice = BigDecimal(500),
//      allowedSides = List("BUY", "SELL"),
//      allowedCurrencies = List("USD"),
//      maxCommissionRate = BigDecimal(0.5),
//      taxRate = BigDecimal(0.1)
//    )
//  )
//
//  private def createValidTrade(): Trade = {
//    Trade(
//      trade_id = 1,
//      order_id = "ORD001",
//      execution_id = "EX001",
//      symbol = "AAPL",
//      side = "BUY",
//      quantity = BigDecimal(100),
//      price = BigDecimal(150),
//      trade_time = "2024-01-15",
//      venue = "NYSE",
//      currency = "USD",
//      account_id = "ACC001",
//      broker_id = "BRK001",
//      commission = BigDecimal(10),
//      tax = BigDecimal(15),
//      gross_amount = BigDecimal(15000),
//      net_amount = BigDecimal(14975),
//      received_time = "2024-01-15T10:30:00",
//      status = "NEW"
//    )
//  }
//
//  "ValidationService.validateTrade" should "PASS validation for a completely valid trade" in {
//    val validTrade = createValidTrade()
//
//    val result = ValidationService.validateTrade(validTrade, sampleRules)
//    result.isValid shouldBe true
//    result.errors shouldBe empty
//    println(" PASS TEST: Trade passed validation successfully")
//    println(s"  Trade ID: ${validTrade.trade_id}")
//    println(s"  Symbol: ${validTrade.symbol}")
//    println(s"  Status: ${if(result.isValid) "VALID" else "INVALID"}")
//  }
//
//  it should "FAIL validation when quantity exceeds maximum limit" in {
//    val invalidTrade = createValidTrade().copy(
//      quantity = BigDecimal(15000),
//      gross_amount = BigDecimal(2250000),
//      net_amount = BigDecimal(2250000 - 10 - 15)
//    )
//
//    val result = ValidationService.validateTrade(invalidTrade, sampleRules)
//
//    result.isValid shouldBe false
//    result.errors should contain ("Quantity 15000 exceeds maximum 10000 for AAPL")
//
//    println("\n FAIL TEST: Trade failed validation as expected")
//    println(s"  Trade ID: ${invalidTrade.trade_id}")
//    println(s"  Symbol: ${invalidTrade.symbol}")
//    println(s"  Quantity: ${invalidTrade.quantity} (Max allowed: 10000)")
//    println(s"  Errors found: ${result.errors.size}")
//    result.errors.foreach(err => println(s"    - $err"))
//  }
//}