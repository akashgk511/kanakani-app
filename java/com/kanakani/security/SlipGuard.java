package com.kanakani.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Finds details in a draft that could point back to the writer: an email address, a phone number,
 * a link or something that looks like an employee ID. The server checks again on submit,
 * because the browser check can be skipped.
 */
public final class SlipGuard {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+");
    private static final Pattern PHONE = Pattern.compile("(?:\\+?\\d[\\s-]?){10,}");
    private static final Pattern LINK = Pattern.compile("https?://|www\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMPLOYEE_ID = Pattern.compile("\\b[A-Za-z]{2,4}-?\\d{4,}\\b");

    private SlipGuard() { }

    /** Returns plain-language findings, empty when the text looks safe. */
    public static List<String> check(String text) {
        List<String> found = new ArrayList<>();
        if (text == null) {
            return found;
        }
        if (EMAIL.matcher(text).find()) {
            found.add("an email address");
        }
        if (PHONE.matcher(text).find()) {
            found.add("a phone number");
        }
        if (LINK.matcher(text).find()) {
            found.add("a link");
        }
        if (EMPLOYEE_ID.matcher(text).find()) {
            found.add("what looks like an employee ID");
        }
        return found;
    }
}
