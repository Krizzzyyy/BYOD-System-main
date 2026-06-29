package com.example.registration;

public class RegistrationValidator {

    private static final String ID_PATTERN = "^\\d{4}-\\d{5}-SR-\\d{1}$";
    private static final String NAME_PATTERN = "^[a-zA-ZÑñ\\s.'-]+$";
    private static final String CONTACT_PATTERN = "^\\d{11}$";

    // Year: 1-9
    // Section: 1 or 2 digits, for example 4-1 or 4-10
    private static final String YEAR_SEC_PATTERN = "^[1-9]-\\d{1,2}$";

    public static String validate(String userType, String sid, String fn, String ln, String contact, String yearSec, String scheduledDateStr) {
        userType = clean(userType);
        sid      = clean(sid);
        fn       = clean(fn);
        ln       = clean(ln);
        yearSec  = clean(yearSec);
        contact  = clean(contact).replaceAll("[\\s-]", "");

        if (userType.isEmpty())
            return "Please select a User Type.";
        if (fn.isEmpty() || ln.isEmpty() || contact.isEmpty())
            return "Please complete all required fields.";
        if (!fn.matches(NAME_PATTERN) || !ln.matches(NAME_PATTERN))
            return "Names must contain letters only. Spaces, period, hyphen, apostrophe, and Ñ/ñ are allowed.";
        if (!contact.matches(CONTACT_PATTERN))
            return "Contact number must be exactly 11 digits.";

        boolean isStudent = userType.equalsIgnoreCase("Student");

        if (isStudent) {
            if (sid.isEmpty() || yearSec.isEmpty())
                return "Please complete all required student information fields.";
            if (!sid.matches(ID_PATTERN))
                return "Invalid Student ID! Format must be: 2024-00123-SR-0";
            if (!yearSec.matches(YEAR_SEC_PATTERN))
                return "Year & Section must follow the format: X-X or X-XX. Example: 4-1 or 4-10";
        }

        if (scheduledDateStr == null || scheduledDateStr.isBlank())
            return "Please select a Scheduled Entry Date.";

        java.time.LocalDate scheduled;
        try {
            scheduled = java.time.LocalDate.parse(scheduledDateStr);
        } catch (Exception e) {
            return "Invalid Scheduled Entry Date.";
        }

        if (!scheduled.isAfter(java.time.LocalDate.now()))
            return "Scheduled entry date must be at least 1 day from today.";

        return "VALID";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
