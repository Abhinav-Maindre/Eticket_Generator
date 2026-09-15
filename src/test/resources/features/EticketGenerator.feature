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

  Scenario: Verify missing BookingReference in D11 returns 400 Bad Request
    Given The user is authenticated with Client Credentials
    When The user sends a POST request to generate an e-ticket with the payload "src/test/resources/testdata/EticketRequest_D11_MissingBookingRef.json"
    Then The response status code should be 400
    And The response should contain error details under key "errors" for error code "10431"

  # --- FORMAT-SPECIFIC DYNAMIC MATRIX COVERAGE ---

  Scenario Outline: Validate format-specific rules for valid payloads
    Given The user is authenticated with Client Credentials
    When The user generates a payload for format "<FormatId>" with BookingReference "<BookingReference>" and "<Legs>" legs
    Then The response status code should be 200
    And The response should contain the e-ticket base64 payload under key "tickets[0]"
    And The e-ticket payload is successfully decoded using Base64

    Examples:
      | FormatId | BookingReference | Legs    |
      | D01      | Absent           | Present |
      | D01      | Present          | Present |
      | D02      | Absent           | Absent  |
      | D02      | Present          | Present |
      | D04      | Present          | Present |
      | D04      | Absent           | Absent  |
      | D10      | Present          | 1       |
      | D11      | Present          | 2+      |
      | D12      | Absent           | Absent  |
      | D12      | Present          | Present |

  Scenario Outline: Validate format-specific rules for invalid payloads
    Given The user is authenticated with Client Credentials
    When The user generates a payload for format "<FormatId>" with BookingReference "<BookingReference>" and "<Legs>" legs
    Then The response status code should be 400
    And The response should contain error details under key "errors"

    Examples:
      | FormatId | BookingReference | Legs    |
      | D01      | Present          | Absent  |
      | D01      | Absent           | Absent  |
      | D10      | Absent           | 1       |
      | D10      | Empty            | 1       |
      | D10      | Present          | 0       |
      | D10      | Present          | 2       |
      | D11      | Absent           | 2+      |
      | D11      | Empty            | 2+      |
      | D11      | Present          | 0       |
      | D11      | Present          | 1       |
      | D12      | Present          | Absent  |

  @TestCase_113568 @US111878
  Scenario Outline: Test Case 113568: US111878 - Validate the error response when Mandatory fields are missing values
    Given The user is authenticated with Client Credentials
    When The user generates a payload with root field "<FieldPath>" missing
    Then The response status code should be 400
    And The response should contain error details under key "errors" for error code "<ErrorCode>"

    Examples:
      | FieldPath               | ErrorCode |
      | $.TocReference          | 10404     |
      | $.MethodOfPayment       | 0003      |
      | $.RecipientEmailAddress | 10405     |
      | $.TotalPrice            | 10406     |
      | $.TicketDetails         | 10407     |

  @TestCase_113569 @US111878
  Scenario Outline: Test Case 113569: US111878 - Validate the error response when Mandatory fields has invalid values in request body
    Given The user is authenticated with Client Credentials
    When The user generates a payload with field "<FieldPath>" set to "<InvalidValue>"
    Then The response status code should be 400
    And The response should contain error details under key "errors" for error code "<ErrorCode>"

    Examples:
      | FieldPath               | InvalidValue   | ErrorCode |
      | $.TocReference          | EMPTY_STRING   | 10404     |
      | $.TocReference          | @#%            | 10404     |
      | $.TocReference          | BLANK_SPACE    | 10404     |
      | $.MethodOfPayment       | EMPTY_STRING   | 0003      |
      | $.MethodOfPayment       | INVALID_MOP    | 0003      |
      | $.MethodOfPayment       | @#%            | 0003      |
      | $.MethodOfPayment       | BLANK_SPACE    | 0003      |
      | $.MethodOfPayment       | 12345          | 0003      |
      | $.RecipientEmailAddress | EMPTY_STRING   | 10405     |
      | $.TotalPrice            | -10            | 10406     |
      | $.TicketDetails         | EMPTY_ARRAY    | 10407     |

  @TestCase_113569 @US111878
  Scenario Outline: Test Case 113569: US111878 - Validate the error response when numeric fields have non-numeric invalid values causing parsing failure
    Given The user is authenticated with Client Credentials
    When The user generates a payload with field "<FieldPath>" set to "<InvalidValue>"
    Then The response status code should be 400

    Examples:
      | FieldPath               | InvalidValue   |
      | $.TotalPrice            | EMPTY_STRING   |
      | $.TotalPrice            | BLANK_SPACE    |
      | $.TotalPrice            | string_val     |
