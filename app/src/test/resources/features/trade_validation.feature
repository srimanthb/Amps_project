Feature: Trade Validation
  As a trading system operator
  I want to validate trades against business rules
  So that I can ensure only valid trades are processed

  Background:
    Given validation rules are loaded for symbols "AAPL,GOOGL"

  Scenario: Valid trade passes all validations
    Given I have a trade with the following details:
      | trade_id | order_id | execution_id | symbol | side | quantity | price | trade_time | venue | currency | account_id | broker_id | commission | tax  | gross_amount | net_amount | received_time | status   |
      | 1001     | ORD001   | EXEC001      | AAPL   | BUY  | 100      | 150   | 2024-01-15 | NYSE  | USD      | ACC001     | BROK001   | 10         | 22.5 | 15000       | 14967.5    | 2024-01-15    | ENRICHED |
    When I validate the trade
    Then the validation should pass
    And the trade status should be updated to "VALIDATED"

  Scenario: Trade with invalid symbol fails validation
    Given I have a trade with the following details:
      | trade_id | order_id | execution_id | symbol | side | quantity | price | trade_time | venue  | currency | account_id | broker_id | commission | tax | gross_amount | net_amount | received_time | status   |
      | 1002     | ORD002   | EXEC002      | MSFT   | SELL | 50       | 300   | 2024-01-15 | NASDAQ | USD      | ACC002     | BROK002   | 15         | 45  | 15000       | 14940      | 2024-01-15    | ENRICHED |
    When I validate the trade
    Then the validation should fail
    And the validation should fail with error "Symbol 'MSFT' not found in validation rules. Supported: AAPL, GOOGL"