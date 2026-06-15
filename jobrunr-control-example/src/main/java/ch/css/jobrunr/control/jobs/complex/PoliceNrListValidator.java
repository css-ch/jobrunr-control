package ch.css.jobrunr.control.jobs.complex;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PoliceNrListValidator implements ConstraintValidator<PoliceNrListValidation, String> {
    private static final int POLICE_NR_LENGTH = 11;
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // optional field
        }
        int i = 0;
        int n = value.length();
        while (i < n) {
            // Skip delimiters: comma, semicolon, whitespace incl. newlines
            while (i < n && isDelimiter(value.charAt(i))) {
                i++;
            }
            if (i >= n) {
                break;
            }
            int tokenStart = i;
            int tokenLen = 0;
            // Read one token
            while (i < n && !isDelimiter(value.charAt(i))) {
                char c = value.charAt(i);
                if (!isAllowedPoliceNrChar(c)) {
                    return fail(context, "Ungültiges Zeichen in PoliceNr-Liste an Position " + i + ": '" + c + "' (erwarte A-Z, 0-9 oder '-')");
                }
                tokenLen++;
                if (tokenLen > POLICE_NR_LENGTH) {
                    return fail(context, "PoliceNr zu lang an Postion " + tokenStart + " (erwarte 11 Zeichen)");
                }
                i++;
            }
            if (tokenLen != POLICE_NR_LENGTH) {
                return fail(context, "PoliceNr muss exakt 11 Zeichen lang sein (Fehler bei Position " + tokenStart + ")");
            }
        }
        return true;
    }
    private static boolean isDelimiter(char c) {
        return c == ',' || c == ';' || Character.isWhitespace(c);
    }
    private static boolean isAllowedPoliceNrChar(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-';
    }
    private static boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
