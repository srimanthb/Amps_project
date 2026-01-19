package AMPS

import java.sql.{Connection, DriverManager, ResultSet}
import scala.collection.mutable.ListBuffer

object DatabaseConfig {
  private val url = "jdbc:sqlserver://localhost:1433;databaseName=Amps;encrypt=true;trustServerCertificate=true"
  private val username = "sa"
  private val password = "Srimanth@9"

  case class ValidationRule(
                             symbol: String,
                             minQuantity: BigDecimal,
                             maxQuantity: BigDecimal,
                             minPrice: BigDecimal,
                             maxPrice: BigDecimal,
                             allowedSides: List[String],
                             allowedCurrencies: List[String],
                             maxCommissionRate: BigDecimal,
                             taxRate: BigDecimal
                           )

  private def getConnection: Connection = {
    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
    DriverManager.getConnection(url, username, password)
  }

  // Method to load validation rules from database
  def loadValidationRules(): Map[String, ValidationRule] = {
    var connection: Connection = null
    var result = Map.empty[String, ValidationRule]

    try {
      connection = getConnection
      val statement = connection.createStatement()
      val query = "SELECT symbol, min_quantity, max_quantity, min_price, max_price, allowed_sides, allowed_currencies, max_commission_rate, tax_rate FROM validation_static_rules"
      val resultSet = statement.executeQuery(query)

      while (resultSet.next()) {
        val rule = ValidationRule(
          symbol = resultSet.getString("symbol"),
          minQuantity = BigDecimal(resultSet.getBigDecimal("min_quantity")),
          maxQuantity = BigDecimal(resultSet.getBigDecimal("max_quantity")),
          minPrice = BigDecimal(resultSet.getBigDecimal("min_price")),
          maxPrice = BigDecimal(resultSet.getBigDecimal("max_price")),
          allowedSides = resultSet.getString("allowed_sides").split(",").map(_.trim).toList,
          allowedCurrencies = resultSet.getString("allowed_currencies").split(",").map(_.trim).toList,
          maxCommissionRate = BigDecimal(resultSet.getBigDecimal("max_commission_rate")),
          taxRate = BigDecimal(resultSet.getBigDecimal("tax_rate"))
        )
        result += (rule.symbol -> rule)
      }

    } catch {
      case e: Exception =>
        println(s"Error loading validation rules: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      if (connection != null) connection.close()
    }

    result
  }
}