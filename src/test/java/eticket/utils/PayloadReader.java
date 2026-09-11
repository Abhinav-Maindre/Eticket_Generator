package eticket.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * PayloadReader is a simple utility class to read files from our resources folder.
 * In API automation, we keep our request payloads (JSON bodies) in separate files
 * rather than hardcoding long JSON strings in our Java code. This makes the code
 * clean and easy to maintain.
 */
public class PayloadReader {

    /**
     * Reads a file from a specified file path and returns its contents as a String.
     * @param filePath - Path to the file (e.g., "src/test/resources/testdata/EticketRequest.json")
     * @return String - The content of the file
     */
    public static String getJsonPayload(String filePath) {
        try {
            // Files.readAllBytes reads all bytes from the file, and we convert those bytes into a String.
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            System.err.println("ERROR: Could not read JSON payload file at: " + filePath);
            e.printStackTrace();
            return "";
        }
    }
}
