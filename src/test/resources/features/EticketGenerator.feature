# Feature file written in Gherkin syntax.
# Gherkin is a business-readable, domain-specific language that lets you describe your product's behavior
# without needing to detail how that behavior is implemented. It uses Keywords: Given, When, Then, And.

Feature: E-Ticket Generator API Testing
  As an automation tester
  I want to verify that the E-Ticket Generator API generates tickets correctly
  And returns a Base64 encoded payload that can be parsed and decoded successfully.

  Scenario: Generate an E-ticket and decode the Base64 response
    Given The user is authenticated with Client Credentials
    When The user sends a POST request to generate an e-ticket with the payload "src/test/resources/testdata/EticketRequest.json"
    Then The response status code should be 200
    And The response should contain the e-ticket base64 payload under key "tickets[0]"
    And The e-ticket payload is successfully decoded using Base64
