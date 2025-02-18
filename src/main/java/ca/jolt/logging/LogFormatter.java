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
 * </p>
 * 
 * <pre>
 * Jolt [LOG_LEVEL] - [YYYY-MM-DD HH:mm:ss] : Log message
 * [Stack trace if an exception is present]
 * </pre>
 * 
 * <p>
 * Example output:
 * </p>
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
 * </p>
 * 
 * @author William Beaudin
 * @see LogConfigurator
 * @since 1.0
 */
public class LogFormatter extends Formatter {
    private static final ThreadLocal<DateTimeFormatter> formatter = ThreadLocal
            .withInitial(() -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    /**
     * This function can be overriden to change the format of the log output.
     * {@inheritDoc}
     */
    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(128);

        builder.append("Jolt [")
                .append(record.getLevel())
                .append("] - [")
                .append(formatter.get().format(LocalDateTime.now()))
                .append("] : ")
                .append(formatMessage(record))
                .append(System.lineSeparator());

        return formatStackTrace(builder, record).toString();
    }

    protected final StringBuilder formatStackTrace(StringBuilder sb, LogRecord record) {
        if (record.getThrown() != null) {
            try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
                record.getThrown().printStackTrace(pw);
                sb.append(sw.toString());
            } catch (Exception ex) {
                sb.append(record.getThrown()).append(System.lineSeparator());
            }
        }
        return sb;
    }
}
