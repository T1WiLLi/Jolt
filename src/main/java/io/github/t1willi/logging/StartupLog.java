package io.github.t1willi.logging;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Displays a stylized startup banner for Jolt applications.
 * Shows essential system information and a distinctive ASCII art logo.
 */
public final class StartupLog {
    private static final String VERSION = "2.6.9";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private StartupLog() {
        // Prevent instantiation
    }

    /**
     * Prints the Jolt startup banner and essential environment information.
     */
    public static void printStartup() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        String startTime = FORMATTER.format(Instant.ofEpochMilli(runtimeMXBean.getStartTime()));
        String javaVersion = System.getProperty("java.version");
        String javaVM = System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.version") + ")";
        String os = System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")";

        System.out.println("\n" + getAsciiArt());
        System.out.println("   :: Jolt ::  (v" + VERSION + ")\n");
        System.out.println("Startup Information:");
        System.out.println("--------------------");
        System.out.println("Start Time: " + startTime);
        System.out.println("Java Version: " + javaVersion);
        System.out.println("Java VM: " + javaVM);
        System.out.println("Operating System: " + os);
        System.out.println();
    }

    private static String getAsciiArt() {
        return """
                     ██╗ ██████╗ ██╗  ████████╗
                     ██║██╔═══██╗██║  ╚══██╔══╝
                     ██║██║   ██║██║     ██║
                ██   ██║██║   ██║██║     ██║
                ╚█████╔╝╚██████╔╝███████╗██║
                 ╚════╝  ╚═════╝ ╚══════╝╚═╝
                """;
    }
}