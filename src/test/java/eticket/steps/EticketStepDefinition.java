package eticket.steps;

import eticket.api.AuthHelper;
import eticket.utils.PayloadReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import com.jayway.jsonpath.JsonPath;
import org.testng.Assert;
import java.util.Base64;

/**
 * Step Definitions class for the E-Ticket Generator.
 * This class binds the Gherkin feature steps (e.g., Given, When, Then) to actual Java execution code.
 * 
 * For manual testers:
 * - Each method is annotated with a Gherkin annotation (@Given, @When, @Then).
 * - The text inside the parentheses matches the step defined in the Feature file.
 * - Variables can be passed dynamically from feature files using curly braces, e.g., {string} or {int}.
 */
public class EticketStepDefinition {

    private String authToken;
    private Response apiResponse;
    private String base64TicketString;
    private static java.util.Properties errorProps;

    static {
        errorProps = new java.util.Properties();
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream("src/test/resources/errormessages.properties");
            errorProps.load(fis);
        } catch (java.io.IOException e) {
            System.err.println("WARNING: Could not load errormessages.properties!");
        }
    }

    @Given("The user is authenticated with Client Credentials")
    public void userIsAuthenticated() {
        System.out.println("\n=== STEP 1: AUTHENTICATION ===");
        
        // 1. Call our AuthHelper to perform the OAuth2 Client Credentials flow.
        // It fetches the token from Cognito/Okta and formats it as "Bearer <token>".
        this.authToken = AuthHelper.getAuthToken();
        
        // 2. Validate that we actually got a token. If it's null or empty, the test should fail immediately.
        Assert.assertNotNull(authToken, "Authentication failed! Token is null.");
        Assert.assertTrue(authToken.contains("Bearer"), "Token should contain 'Bearer' prefix.");
        
        System.out.println("[STEP 1 SUCCESS] Authentication verified. Bearer token is ready.");
    }

    @When("The user sends a POST request to generate an e-ticket with the payload {string}")
    public void userSendsPostRequestToGenerateEticket(String payloadPath) {
        System.out.println("\n=== STEP 2: SENDING API REQUEST ===");
        
        // 1. Read the JSON request body (payload) from our testdata folder.
        String requestBody = PayloadReader.getJsonPayload(payloadPath);
        System.out.println("[INFO] Request Payload Loaded:\n" + requestBody);

        // 2. Retrieve endpoint configurations from our config.properties via AuthHelper.
        String baseUri = AuthHelper.getProp("baseUri");
        String endpoint = AuthHelper.getProp("eticketEndpoint");

        System.out.println("[INFO] Target URL: " + baseUri + endpoint);

        // 3. Construct the RestAssured request.
        // - "given()" sets up the request specifications (Headers, Auth, Body).
        // - "when()" triggers the HTTP method action (POST, GET, etc.) against the endpoint.
        RequestSpecification request = RestAssured.given()
                .baseUri(baseUri)
                .header("Content-Type", "application/json")
                .header("Authorization", this.authToken) // Pass our Bearer token!
                .body(requestBody);

        // 4. Send the POST request and capture the response object.
        this.apiResponse = request.post(endpoint);

        System.out.println("[STEP 2 SUCCESS] POST Request sent. Response received.");
    }

    @Then("The response status code should be {int}")
    public void responseStatusCodeShouldBe(int expectedStatusCode) {
        System.out.println("\n=== STEP 3: VERIFYING STATUS CODE ===");
        
        // Extract the actual status code from our API response.
        int actualStatusCode = this.apiResponse.getStatusCode();
        System.out.println("[INFO] Expected Status Code: " + expectedStatusCode);
        System.out.println("[INFO] Actual Status Code: " + actualStatusCode);

        // Assert that they match. If they don't, the test fails here.
        Assert.assertEquals(actualStatusCode, expectedStatusCode, "API response status code mismatch!");
        System.out.println("[STEP 3 SUCCESS] Status code verified matches: " + expectedStatusCode);
    }

    @Then("The response should contain the e-ticket base64 payload under key {string}")
    public void responseShouldContainBase64Payload(String jsonKey) {
        System.out.println("\n=== STEP 4: PARSING RESPONSE FOR BASE64 ===");
        
        // Print the full response body for debugging and learning.
        String responseBody = this.apiResponse.asPrettyString();
        System.out.println("[INFO] Response Body:\n" + responseBody);

        // Extract the value under the specified key (e.g., $.ticketBase64) using JsonPath.
        // If the key is nested, JsonPath is perfect for navigating to it.
        try {
            this.base64TicketString = JsonPath.read(responseBody, "$." + jsonKey);
        } catch (Exception e) {
            Assert.fail("Failed to find key '" + jsonKey + "' in the response body! Error: " + e.getMessage());
        }

        // Validate that the extracted string is not null or empty.
        Assert.assertNotNull(this.base64TicketString, "Base64 ticket string is missing from response!");
        Assert.assertFalse(this.base64TicketString.isEmpty(), "Base64 ticket string is empty!");

        System.out.println("[STEP 4 SUCCESS] Successfully extracted Base64 ticket value: " + this.base64TicketString);
    }

    @Then("The e-ticket payload is successfully decoded using Base64")
    public void eticketPayloadIsSuccessfullyDecoded() {
        System.out.println("\n=== STEP 5: BASE64 DECODING ===");

        try {
            // 1. Get the standard Base64 decoder from Java's built-in java.util package.
            Base64.Decoder decoder = Base64.getDecoder();

            // 2. Decode the Base64-encoded string into its raw byte form.
            byte[] decodedBytes = decoder.decode(this.base64TicketString);

            // 3. Save the decoded bytes as a unique timestamped PDF file in the target directory.
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            java.io.File pdfFile = new java.io.File("target/generated-ticket_" + timestamp + ".pdf");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(pdfFile)) {
                fos.write(decodedBytes);
            }
            System.out.println("[INFO] Successfully saved decoded e-ticket PDF to: " + pdfFile.getAbsolutePath());

            // 4. Assert that the decoded PDF file was created and is not empty.
            Assert.assertTrue(pdfFile.exists(), "PDF file was not created!");
            Assert.assertTrue(pdfFile.length() > 0, "Generated PDF file is empty!");
            System.out.println("[STEP 5 SUCCESS] Decoded PDF is valid and saved.");

        } catch (Exception e) {
            Assert.fail("Failed to decode or write PDF payload! Error: " + e.getMessage());
        }
    }

    @Given("The user is not authenticated")
    public void userIsNotAuthenticated() {
        System.out.println("\n=== STEP 1: BYPASSING AUTHENTICATION ===");
        
        // Use an invalid authorization header value to simulate unauthenticated access.
        this.authToken = "Bearer invalid_token_123456789_xyz";
        
        System.out.println("[STEP 1 SUCCESS] Mocked an invalid token for security validation testing.");
    }

    @Then("The response should contain error details under key {string}")
    public void responseShouldContainErrorDetails(String jsonKey) {
        System.out.println("\n=== VERIFYING ERROR DETAILS ===");
        
        // Print the full response body for visibility.
        String responseBody = this.apiResponse.asPrettyString();
        System.out.println("[INFO] Response Body:\n" + responseBody);
        
        // Extract the error value under the specified key (e.g. $.errors).
        Object errors = null;
        try {
            errors = JsonPath.read(responseBody, "$." + jsonKey);
        } catch (Exception e) {
            Assert.fail("Failed to find error key '" + jsonKey + "' in the response body! Error: " + e.getMessage());
        }
        
        // Assert that errors exist and are not empty.
        Assert.assertNotNull(errors, "Expected error details but found null under key: " + jsonKey);
        if (errors instanceof java.util.List) {
            Assert.assertFalse(((java.util.List<?>) errors).isEmpty(), "Expected validation error details but the error list is empty!");
        }
        System.out.println("[SUCCESS] Successfully verified error details under key: " + jsonKey);
    }

    @Then("The response should contain error details under key {string} for error code {string}")
    public void responseShouldContainErrorDetailsForCode(String jsonKey, String errorCode) {
        System.out.println("\n=== VERIFYING CENTRALIZED ERROR MAPPING ===");
        
        // Print response body for visibility
        String responseBody = this.apiResponse.asPrettyString();
        System.out.println("[INFO] Response Body:\n" + responseBody);
        
        // Get expected message from our centralized properties mapping
        String expectedMessage = errorProps.getProperty(errorCode);
        Assert.assertNotNull(expectedMessage, "No custom error message mapped for error code: " + errorCode + " in errormessages.properties!");
        System.out.println("[INFO] Mapped Expected Message: '" + expectedMessage + "'");
        
        // Extract error details list using JsonPath
        java.util.List<java.util.Map<String, Object>> errors = null;
        try {
            errors = JsonPath.read(responseBody, "$." + jsonKey);
        } catch (Exception e) {
            Assert.fail("Failed to find error key '" + jsonKey + "' in the response body! Error: " + e.getMessage());
        }
        
        Assert.assertNotNull(errors, "Expected error details but found null under key: " + jsonKey);
        Assert.assertFalse(errors.isEmpty(), "Expected error list to contain elements!");
        
        // Find if any returned error matches both the code and the expected mapped message
        boolean found = false;
        for (java.util.Map<String, Object> error : errors) {
            String actualCode = String.valueOf(error.get("code"));
            String actualMessage = (String) error.get("message");
            
            if (errorCode.equals(actualCode)) {
                Assert.assertEquals(actualMessage, expectedMessage, "Error message mismatch for code: " + errorCode);
                found = true;
                break;
            }
        }
        
        Assert.assertTrue(found, "Expected error code '" + errorCode + "' was not found in the response error list!");
        System.out.println("[SUCCESS] Successfully verified mapped error code " + errorCode + " matches expected message.");
    }
}
