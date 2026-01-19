package AMPS

import scala.collection.mutable.ListBuffer

object ValidationService {

  case class ValidationResult(
                               isValid: Boolean,
                               errors: List[String] = List.empty,
                               warnings: List[String] = List.empty
                             )

  def validateTrade(trade: Trade, validationRules: Map[String, DatabaseConfig.ValidationRule]): ValidationResult = {
    val errors = ListBuffer[String]()
    val warnings = ListBuffer[String]()

    validationRules.get(trade.symbol.toUpperCase) match {
      case Some(rule) =>
        if (trade.quantity < rule.minQuantity) {
          errors += s"Quantity ${trade.quantity} is below minimum ${rule.minQuantity} for ${trade.symbol}"
        }
        if (trade.quantity > rule.maxQuantity) {
          errors += s"Quantity ${trade.quantity} exceeds maximum ${rule.maxQuantity} for ${trade.symbol}"
        }

        if (trade.price < rule.minPrice) {
          errors += s"Price ${trade.price} is below minimum ${rule.minPrice} for ${trade.symbol}"
        }

        if (trade.price > rule.maxPrice) {
          errors += s"Price ${trade.price} exceeds maximum ${rule.maxPrice} for ${trade.symbol}"
        }

        val upperSide = trade.side.toUpperCase
        if (!rule.allowedSides.contains(upperSide)) {
          errors += s"Side '$upperSide' not allowed. Allowed: ${rule.allowedSides.mkString(", ")}"
        }

        val upperCurrency = trade.currency.toUpperCase
        if (!rule.allowedCurrencies.contains(upperCurrency)) {
          errors += s"Currency '$upperCurrency' not allowed. Allowed: ${rule.allowedCurrencies.mkString(", ")}"
        }

        val commissionRate = if (trade.gross_amount != BigDecimal(0)) {
          (trade.commission / trade.gross_amount) * BigDecimal(100)
        } else {
          BigDecimal(0)
        }

        if (commissionRate > rule.maxCommissionRate) {
          errors += s"Commission rate ${commissionRate.setScale(2, BigDecimal.RoundingMode.HALF_UP)}% exceeds maximum ${rule.maxCommissionRate}%"
        }

        val expectedTax = if (trade.gross_amount != BigDecimal(0)) {
          (trade.gross_amount * rule.taxRate) / BigDecimal(100)
        } else {
          BigDecimal(0)
        }

        val taxDifference = (trade.tax - expectedTax).abs
        val taxTolerance = expectedTax * BigDecimal("0.10")

        if (taxDifference > taxTolerance) {
          warnings += s"Tax amount ${trade.tax} differs from expected ${expectedTax.setScale(2, BigDecimal.RoundingMode.HALF_UP)} (${rule.taxRate}% of gross)"
        }

        val calculatedNet = trade.gross_amount - trade.commission - trade.tax
        val netDifference = (trade.net_amount - calculatedNet).abs

        if (netDifference > BigDecimal("0.01")) {
          errors += s"Net amount calculation error: Expected ${calculatedNet.setScale(2, BigDecimal.RoundingMode.HALF_UP)}, got ${trade.net_amount}"
        }

        if (trade.quantity <= BigDecimal(0)) errors += "Quantity must be positive"
        if (trade.price <= BigDecimal(0)) errors += "Price must be positive"
        if (trade.commission < BigDecimal(0)) errors += "Commission cannot be negative"
        if (trade.tax < BigDecimal(0)) errors += "Tax cannot be negative"
        if (trade.gross_amount <= BigDecimal(0)) errors += "Gross amount must be positive"

        try {
          val tradeTime = java.time.LocalDate.parse(trade.trade_time)
          val receivedTime = java.time.LocalDate.parse(trade.received_time)

          if (receivedTime.isBefore(tradeTime)) {
            warnings += "Received time is before trade time"
          }
        } catch {
          case e: Exception => warnings += s"Timestamp parsing issue: ${e.getMessage}"
        }

      case None =>
        errors += s"Symbol '${trade.symbol}' not found in validation rules. Supported: ${validationRules.keys.mkString(", ")}"
    }

    val validVenues = List("NYSE", "NASDAQ", "LSE", "TSE","NSE","BSE")
    if (!validVenues.contains(trade.venue.toUpperCase)) {
      warnings += s"Unusual venue: ${trade.venue}"
    }

    if (!List("BUY", "SELL").contains(trade.side.toUpperCase)) {
      errors += s"Invalid side: ${trade.side}. Must be BUY or SELL"
    }

    ValidationResult(
      isValid = errors.isEmpty,
      errors = errors.toList,
      warnings = warnings.toList
    )
  }

  def addValidationStatus(trade: Trade, validationResult: ValidationResult): Trade = {
    val newStatus = if (validationResult.isValid) "VALIDATED" else "VALIDATION_FAILED"
    trade.copy(status = newStatus)
  }

  def printValidationResult(trade: Trade, result: ValidationResult): Unit = {
    println(s"\n=== Validation Results for Trade ${trade.trade_id} ===")
    println(s"Symbol: ${trade.symbol}, Status: ${if (result.isValid) "VALID" else "INVALID"}")

    if (result.errors.nonEmpty) {
      println("Errors:")
      result.errors.foreach(error => println(s"  - $error"))
    }

    if (result.warnings.nonEmpty) {
      println("Warnings:")
      result.warnings.foreach(warning => println(s"  - $warning"))
    }

    if (result.isValid) {
      println("Trade passed all validations")
    } else {
      println("Trade failed validation")
    }
  }
}

