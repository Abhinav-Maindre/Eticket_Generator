# Feature file written in Gherkin syntax.
# Gherkin is a business-readable, domain-specific language that lets you describe your product's behavior
# without needing to detail how that behavior is implemented. It uses Keywords: Given, When, Then, And.

Feature: E-Ticket Generator API Testing
  As an automation tester
  I want to verify that the E-Ticket Generator API generates tickets correctly
  And returns a Base64 encoded payload that can be parsed and decoded successfully.

  Scenario Outline: Generate an E-ticket and decode the Base64 response for format <Format>
    Given The user is authenticated with Client Credentials
    When The user sends a POST request to generate an e-ticket with the payload "src/test/resources/testdata/EticketRequest_<Format>.json"
    Then The response status code should be 200
    And The response should contain the e-ticket base64 payload under key "tickets[0]"
    And The e-ticket payload is successfully decoded using Base64

    Examples:
      | Format |
      | D01    |
      | D02    |
      | D04    |
      | D10    |
      | D11    |
      | D12    |

  Scenario: Verify unauthorized request returns 401 status code
    Given The user is not authenticated
    When The user sends a POST request to generate an e-ticket with the payload "src/test/resources/testdata/EticketRequest_D01.json"
    Then The response status code should be 401

  Scenario: Verify missing BookingReference in D10 returns 400 Bad Request
    Given The user is authenticated with Client Credentials
    When The user sends a POST request to generate an e-ticket with the payload "src/test/resources/testdata/EticketRequest_D10_MissingBookingRef.json"
    Then The response status code should be 400
    And The response should contain error details under key "errors" for error code "10431"

  Scenario: Verify invalid FormatId D99 returns 400 Bad Request
    Given The user is authenticated with Client Credentials
    When The user sends a POST request to generate an e-ticket with the payload "src/test/resources/testdata/EticketRequest_InvalidFormat.json"
    Then The response status code should be 400
    And The response should contain error details under key "errors" for error code "10422"
