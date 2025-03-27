package io.github.t1willi.template;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Provides common formatting functions for use in templates.
 * <p>
 * This class is designed to be registered with the Freemarker configuration
 * as a global function/method.
 *
 * @since 1.0
 */
public class TemplateFormatters {

    /**
     * Formats a date using the specified pattern.
     *
     * @param date    The date to format
     * @param pattern The date pattern
     * @return The formatted date string
     */
    public String date(LocalDate date, String pattern) {
        if (date == null)
            return "";
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Formats a datetime using the specified pattern.
     *
     * @param dateTime The datetime to format
     * @param pattern  The datetime pattern
     * @return The formatted datetime string
     */
    public String dateTime(LocalDateTime dateTime, String pattern) {
        if (dateTime == null)
            return "";
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Formats a number using the specified pattern.
     *
     * @param number  The number to format
     * @param pattern The number pattern
     * @return The formatted number string
     */
    public String number(Number number, String pattern) {
        if (number == null)
            return "";
        return new DecimalFormat(pattern).format(number);
    }

    /**
     * Formats a currency value using the specified locale.
     *
     * @param value The value to format
     * @return The formatted currency string
     */
    public String currency(Number value) {
        if (value == null)
            return "";
        return new DecimalFormat("$#,##0.00").format(value);
    }

    /**
     * Formats a percentage value.
     *
     * @param value The value to format (0-1)
     * @return The formatted percentage string
     */
    public String percent(Number value) {
        if (value == null)
            return "";
        return new DecimalFormat("#0.0%").format(value);
    }

    /**
     * Truncates a string to the specified length and adds ellipsis if truncated.
     *
     * @param text      The text to truncate
     * @param maxLength The maximum length
     * @return The truncated text
     */
    public String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        if (text.length() <= maxLength)
            return text;
        return text.substring(0, maxLength) + "...";
    }
}