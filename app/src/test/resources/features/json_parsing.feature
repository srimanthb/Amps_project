Feature: JSON Trade Parsing
  As a trading system
  I want to parse trade data from JSON format
  So that I can process trades from various sources

  Scenario: Parse valid JSON trade
    Given I have a JSON trade string:
      """
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
    When I parse the JSON trade
    Then the trade should be parsed successfully
    And the parsed trade should have symbol "GOOGL"
    And the parsed trade should have quantity 50.0

  Scenario: Parse trade with invalid JSON format
    Given I have a JSON trade string:
      """
      {
        "trade_id": 2002,
        "order_id": "ORD2002",
        "symbol": "AAPL",
        "side": "BUY",
        "quantity": "invalid",
        "price": 150,
        "trade_time": "2024-01-16",
        "venue": "NYSE",
        "currency": "USD"
      }
      """
    When I parse the JSON trade
    Then the trade should not be parsed successfully