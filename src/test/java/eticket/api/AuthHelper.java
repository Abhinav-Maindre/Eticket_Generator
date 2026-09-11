package eticket.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import com.jayway.jsonpath.JsonPath;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * AuthHelper is a utility class that handles acquiring an OAuth2 access token.
 * Since most enterprise APIs are secured, we must first authenticate using our
 * Client ID and Client Secret to get a "Bearer Token" before hitting the main endpoint.
 */
public class AuthHelper {

    private static Properties prop;

    // Static block to load the config.properties once when this class is accessed.
    static {
        prop = new Properties();
        try {
            // Load the configuration file so we can access values like authUri, clientId, etc.
            FileInputStream fileInputStream = new FileInputStream("src/test/resources/config.properties");
            prop.load(fileInputStream);
        } catch (IOException e) {
            System.err.println("ERROR: Could not load config.properties file! Please check the path.");
            e.printStackTrace();
        }
    }

    /**
     * Retrieves an OAuth2 Access Token using Client Credentials flow and returns the full Auth Header.
     * @return String - e.g. "Bearer eyJhbGciOi..."
     */
    public static String getAuthToken() {
        System.out.println("[INFO] Authenticating with server via Client Credentials flow...");

        // 1. Build the RestAssured request. We specify content type and form parameters.
        RequestSpecification requestSpecification = RestAssured.given()
                .baseUri(prop.getProperty("authUri"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .formParam("grant_type", prop.getProperty("grantType"))
                .formParam("client_id", prop.getProperty("clientId"))
                .formParam("client_secret", prop.getProperty("clientSecret"));

        // 2. Perform the POST call to retrieve the response.
        Response response = requestSpecification.post();
        
        // Log the response output so you can visually verify it in console logs.
        System.out.println("[DEBUG] Auth Response Status: " + response.getStatusCode());
        System.out.println("[DEBUG] Auth Response Body: " + response.asPrettyString());

        // 3. Extract the 'access_token' and 'token_type' from the JSON body using JsonPath.
        // JsonPath allows us to search JSON structures easily using syntax like "$.access_token".
        String tokenType = JsonPath.read(response.asString(), "$.token_type");
        String accessToken = JsonPath.read(response.asString(), "$.access_token");

        // Format the return value as "Bearer <token>" which is standard for HTTP Authorization headers.
        String fullAuthHeader = tokenType + " " + accessToken;
        System.out.println("[INFO] Successfully authenticated. Token generated.");
        
        return fullAuthHeader;
    }

    /**
     * Helper method to get any configuration property value by its key.
     */
    public static String getProp(String key) {
        return prop.getProperty(key);
    }
}
