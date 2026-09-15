package eticket.utils;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.DocumentContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PayloadBuilder is a reusable test-data utility class designed to dynamically construct 
 * e-ticket API request payloads. It parses the base EticketRequest.json template and 
 * manipulates the JSON structure to generate scenarios with precise field presence (absent, 
 * empty, null, or populated values) of FormatId, BookingReference, and Itinerary Legs.
 */
public class PayloadBuilder {

    private static final String TEMPLATE_PATH = "src/test/resources/testdata/EticketRequest.json";

    /**
     * Dynamically builds a JSON request payload based on format specific requirements and scenario inputs.
     * Throws IllegalArgumentException if any business validation rules are violated.
     *
     * @param formatId - The target ticket format ID (e.g. "D01", "D02", "D10", etc.)
     * @param bookingRefType - "Absent", "Null", "Empty", "Present" / "Populated", or a literal string.
     * @param legsCountStr - "Absent", or a count representation like "0", "1", "2", "2+", "Any".
     * @return String - The serialized JSON request payload.
     */
    @SuppressWarnings("unchecked")
    public static String buildPayload(String formatId, String bookingRefType, String legsCountStr) {
        // Validate format-specific rules at the test framework level first
        validateRules(formatId, bookingRefType, legsCountStr);

        // Read the standard JSON payload template
        String templateJson = PayloadReader.getJsonPayload(TEMPLATE_PATH);
        DocumentContext context = JsonPath.parse(templateJson);

        // 1. Set the FormatId in HeaderDetails
        context.set("$.TicketDetails[0].HeaderDetails.FormatId", formatId);

        // 2. Configure BookingReference at the root level
        if ("Absent".equalsIgnoreCase(bookingRefType)) {
            context.delete("$.BookingReference");
        } else if ("Null".equalsIgnoreCase(bookingRefType)) {
            context.set("$.BookingReference", null);
        } else if ("Empty".equalsIgnoreCase(bookingRefType)) {
            context.set("$.BookingReference", "");
        } else if ("Present".equalsIgnoreCase(bookingRefType) || "Populated".equalsIgnoreCase(bookingRefType)) {
            context.set("$.BookingReference", "BOOK1234");
        } else {
            // Set to the provided literal string value
            context.set("$.BookingReference", bookingRefType);
        }

        // Extract a single leg from the template to use as a blueprint for generating legs list
        Map<String, Object> legBlueprint = null;
        try {
            legBlueprint = context.read("$.TicketDetails[0].Itinerary.Legs[0]");
        } catch (Exception e) {
            // Robust fallback if template structure differs
            legBlueprint = new HashMap<>();
            legBlueprint.put("origin", "1111");
            legBlueprint.put("depart", "2020-09-02T09:09:09");
            legBlueprint.put("destination", "2222");
            legBlueprint.put("arrival", "2020-09-02T09:09:09");
            legBlueprint.put("tocName", "Toc");
            legBlueprint.put("places", new ArrayList<>());
        }

        // 3. Configure Itinerary and Legs list
        if ("Absent".equalsIgnoreCase(legsCountStr)) {
            // Omit the Legs list strictly from the serialized JSON
            context.delete("$.TicketDetails[0].Itinerary.Legs");
        } else {
            int legsCount = 0;
            if ("Present".equalsIgnoreCase(legsCountStr) || "Any".equalsIgnoreCase(legsCountStr)) {
                legsCount = 1;
            } else if ("2+".equalsIgnoreCase(legsCountStr)) {
                legsCount = 2;
            } else {
                try {
                    legsCount = Integer.parseInt(legsCountStr);
                } catch (NumberFormatException e) {
                    legsCount = 0;
                }
            }

            List<Map<String, Object>> generatedLegs = new ArrayList<>();
            for (int i = 0; i < legsCount; i++) {
                Map<String, Object> newLeg = new HashMap<>(legBlueprint);
                if (i > 0) {
                    // Differentiate subsequent legs slightly to simulate a real multi-leg journey
                    newLeg.put("origin", String.valueOf(1111 + i * 1111));
                    newLeg.put("destination", String.valueOf(2222 + i * 1111));
                }
                generatedLegs.add(newLeg);
            }
            context.set("$.TicketDetails[0].Itinerary.Legs", generatedLegs);
        }

        return context.jsonString();
    }

    /**
     * Enforces the business requirements and constraints for each formatId.
     */
    public static void validateRules(String formatId, String bookingRefType, String legsCountStr) {
        // Parse legsCount
        int legsCount = -1; // -1 represents Absent
        if (!"Absent".equalsIgnoreCase(legsCountStr)) {
            if ("Present".equalsIgnoreCase(legsCountStr) || "Any".equalsIgnoreCase(legsCountStr)) {
                legsCount = 1;
            } else if ("2+".equalsIgnoreCase(legsCountStr)) {
                legsCount = 2;
            } else {
                try {
                    legsCount = Integer.parseInt(legsCountStr);
                } catch (NumberFormatException e) {
                    legsCount = 0;
                }
            }
        }

        boolean isBookingRefPresent = !"Absent".equalsIgnoreCase(bookingRefType) 
                                   && !"Null".equalsIgnoreCase(bookingRefType) 
                                   && !"Empty".equalsIgnoreCase(bookingRefType);

        if ("D01".equalsIgnoreCase(formatId) || "D12".equalsIgnoreCase(formatId)) {
            // If BookingReference is present, Itinerary and Legs must also be present
            if (isBookingRefPresent && (legsCount == -1 || legsCount == 0)) {
                throw new IllegalArgumentException("Dependency Rule Violation: For format " + formatId + ", if BookingReference is present, Itinerary/Legs must be present.");
            }
        } else if ("D02".equalsIgnoreCase(formatId) || "D04".equalsIgnoreCase(formatId)) {
            // D02/D04 do not require BookingReference or Legs (both are optional).
            // No strict exclusion check is needed since the API natively supports both states.
        } else if ("D10".equalsIgnoreCase(formatId)) {
            // BookingReference is mandatory and Legs must contain exactly 1 leg
            if (!isBookingRefPresent) {
                throw new IllegalArgumentException("Format Rule Violation: For format D10, BookingReference is mandatory.");
            }
            if (legsCount != 1) {
                throw new IllegalArgumentException("Format Rule Violation: For format D10, Itinerary.Legs must contain exactly one leg.");
            }
        } else if ("D11".equalsIgnoreCase(formatId)) {
            // BookingReference is mandatory and Legs must contain 2 or more legs
            if (!isBookingRefPresent) {
                throw new IllegalArgumentException("Format Rule Violation: For format D11, BookingReference is mandatory.");
            }
            if (legsCount < 2) {
                throw new IllegalArgumentException("Format Rule Violation: For format D11, Itinerary.Legs must contain 2 or more legs.");
            }
        }
    }
}