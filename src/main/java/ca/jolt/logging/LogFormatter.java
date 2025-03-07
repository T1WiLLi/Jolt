package ca.jolt.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Custom log formatter for the Jolt framework that provides a consistent, clean
 * logging format.
 * 
 * <p>
 * Log entries are formatted as follows:
 * 
 * <pre>
 * Jolt [LOG_LEVEL] - [YYYY-MM-DD HH:mm:ss] : Log message
 * [Stack trace if an exception is present]
 * </pre>
 * 
 * <p>
 * Example output:
 * 
 * <pre>
 * Jolt [INFO] - [2024-02-18 14:30:22] : Application started successfully
 * Jolt [ERROR] - [2024-02-18 14:30:23] : Failed to process request
 * java.lang.IllegalArgumentException: Invalid parameter
 *     at com.example.Class.method(Class.java:25)
 *     ...
 * </pre>
 * 
 * <p>
 * This formatter is thread-safe and handles exceptions appropriately by
 * including
 * their stack traces in the log output.
 * 
 * @author William Beaudin
 * @see LogConfigurator
 * @since 1.0
 */
public final class LogFormatter extends Formatter {
    private static final ThreadLocal<DateTimeFormatter> formatter = ThreadLocal
            .withInitial(() -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    /**
     * {@inheritDoc}
     */
    @Override
    public String format(LogRecord logRecord) {
        StringBuilder builder = new StringBuilder(128);
        DateTimeFormatter dtf = formatter.get();
        try {
            builder.append("Jolt [")
                    .append(logRecord.getLevel())
                    .append("] - [")
                    .append(dtf.format(LocalDateTime.now()))
                    .append("] : ")
                    .append(formatMessage(logRecord))
                    .append(System.lineSeparator());

            return formatStackTrace(builder, logRecord).toString();
        } finally {
            formatter.remove();
        }
    }

    private StringBuilder formatStackTrace(StringBuilder sb, LogRecord logRecord) {
        if (logRecord.getThrown() != null) {
            try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
                logRecord.getThrown().printStackTrace(pw);
                sb.append(sw.toString());
            } catch (Exception ex) {
                sb.append(logRecord.getThrown()).append(System.lineSeparator());
            }
        }
        return sb;
    }
}
