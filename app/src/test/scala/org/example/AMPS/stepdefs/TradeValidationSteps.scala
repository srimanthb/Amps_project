package org.example.AMPS.stepdefs

import AMPS._
import io.cucumber.scala.{EN, ScalaDsl}
import org.scalatest.matchers.should.Matchers
import scala.jdk.CollectionConverters._

class TradeValidationSteps extends ScalaDsl with EN with Matchers {

  private var trade: Trade = _
  private var validationRules: Map[String, DatabaseConfig.ValidationRule] = _
  private var validationResult: ValidationService.ValidationResult = _
  private var parsedTrade: Either[String, Trade] = _

  private val sampleRules = Map(
    "AAPL" -> DatabaseConfig.ValidationRule(
      symbol = "AAPL",
      minQuantity = BigDecimal(10),
      maxQuantity = BigDecimal(10000),
      minPrice = BigDecimal(100),
      maxPrice = BigDecimal(500),
      allowedSides = List("BUY", "SELL"),
      allowedCurrencies = List("USD"),
      maxCommissionRate = BigDecimal(2),
      taxRate = BigDecimal(15)
    ),
    "GOOGL" -> DatabaseConfig.ValidationRule(
      symbol = "GOOGL",
      minQuantity = BigDecimal(5),
      maxQuantity = BigDecimal(5000),
      minPrice = BigDecimal(1200),
      maxPrice = BigDecimal(2000),
      allowedSides = List("BUY", "SELL"),
      allowedCurrencies = List("USD", "EUR"),
      maxCommissionRate = BigDecimal(1.5),
      taxRate = BigDecimal(18)
    )
  )

  Given("validation rules are loaded for symbols {string}") { (symbols: String) =>
    val symbolList = symbols.split(",").map(_.trim.toUpperCase).toSet
    validationRules = sampleRules.filter { case (symbol, _) =>
      symbolList.contains(symbol)
    }
  }


  Given("I have a trade with the following details:") {
    (dataTable: io.cucumber.datatable.DataTable) =>
      val tradeData = dataTable.asMaps().get(0).asScala

      trade = Trade(
        trade_id = tradeData("trade_id").toInt,
        order_id = tradeData("order_id"),
        execution_id = tradeData("execution_id"),
        symbol = tradeData("symbol"),
        side = tradeData("side"),
        quantity = BigDecimal(tradeData("quantity")),
        price = BigDecimal(tradeData("price")),
        trade_time = tradeData("trade_time"),
        venue = tradeData("venue"),
        currency = tradeData("currency"),
        account_id = tradeData("account_id"),
        broker_id = tradeData("broker_id"),
        commission = BigDecimal(tradeData("commission")),
        tax = BigDecimal(tradeData("tax")),
        gross_amount = BigDecimal(tradeData("gross_amount")),
        net_amount = BigDecimal(tradeData("net_amount")),
        received_time = tradeData("received_time"),
        status = tradeData("status")
      )
  }

  When("I validate the trade") { () =>
    validationResult = ValidationService.validateTrade(trade, validationRules)
  }

  Then("the validation should pass") { () =>
    validationResult.isValid shouldBe  true
    validationResult.errors shouldBe empty
  }

  Then("the validation should fail") { () =>
    validationResult.isValid shouldBe false
  }

  Then("the validation should fail with error {string}") { (expectedError: String) =>
    validationResult.isValid shouldBe false
    validationResult.errors should contain (expectedError)
  }

  Then("I should see warning {string}") { (expectedWarning: String) =>
    validationResult.warnings should contain (expectedWarning)
  }

  Then("the trade status should be updated to {string}") { (expectedStatus: String) =>
    val updatedTrade = ValidationService.addValidationStatus(trade, validationResult)
    updatedTrade.status shouldBe expectedStatus
  }

  // ===== JSON PARSING STEPS =====

  Given("I have a JSON trade string:") { (jsonString: String) =>
    try {
      parsedTrade = Right(TradeJsonParser.parseTrade(jsonString))
    } catch {
      case e: Exception =>
        parsedTrade = Left(e.getMessage)
    }
  }

  When("I parse the JSON trade") { () =>
    // Already parsed in Given step
  }

  Then("the trade should be parsed successfully") { () =>
    parsedTrade.isRight shouldBe true
  }

  Then("the trade should not be parsed successfully") { () =>
    parsedTrade.isLeft shouldBe true
  }

  Then("the parsed trade should have symbol {string}") { (expectedSymbol: String) =>
    parsedTrade match {
      case Right(t) => t.symbol shouldBe expectedSymbol
      case Left(err) => fail(s"Failed to parse trade: $err")
    }
  }

  Then("the parsed trade should have quantity {double}") { (expectedQuantity: Double) =>
    parsedTrade match {
      case Right(t) => t.quantity shouldBe BigDecimal(expectedQuantity)
      case Left(err) => fail(s"Failed to parse trade: $err")
    }
  }
}