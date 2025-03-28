package io.github.t1willi.utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.github.t1willi.server.config.ConfigurationManager;
import io.github.t1willi.server.config.ServerConfig;
import jakarta.servlet.http.HttpServletResponse;

public class DirectoryListingHtmlTemplateBuilder {

    /**
     * Tries to serve a directory listing as an HTML page if enabled, even if the
     * directory is empty.
     *
     * @param path the path to the directory to serve
     * @param res  the HttpServletResponse object to write to
     * @return true if the directory listing was served, false otherwise
     * @throws IOException if an I/O error occurs
     */
    public static boolean tryServeDirectoryListing(String path, HttpServletResponse res) {
        ServerConfig serverConfig = ConfigurationManager.getInstance().getServerConfig();
        if (!serverConfig.isDirectoryListingEnabled()) {
            return false;
        }

        String listingPath = serverConfig.getDirectoryListingPath();
        if (!path.equals(listingPath)) {
            return false;
        }

        URL staticUrl = DirectoryListingHtmlTemplateBuilder.class.getClassLoader().getResource("static");
        File staticDir = null;
        if (staticUrl != null) {
            try {
                staticDir = new File(staticUrl.toURI());
            } catch (URISyntaxException e) {
                // Log the error if needed, but proceed with an empty listing
            }
        }

        DirectoryListingHtmlTemplateBuilder builder = new DirectoryListingHtmlTemplateBuilder()
                .setTitle("Directory Listing 📂");

        // Always build the listing, even if staticDir is null or not a directory
        if (staticDir != null && staticDir.isDirectory()) {
            buildDirectoryEntries(staticDir, builder);
        }
        // If no entries were added, the "empty" message will appear in the build step

        String html = builder.build();
        res.setContentType("text/html;charset=UTF-8");
        try {
            res.getWriter().write(html);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Recursively scans the given directory and adds its entries to the provided
     * builder.
     *
     * @param directory The directory to scan.
     * @param builder   The builder to which file and directory entries are added.
     */
    private static void buildDirectoryEntries(File directory, DirectoryListingHtmlTemplateBuilder builder) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    DirectoryListingHtmlTemplateBuilder subBuilder = new DirectoryListingHtmlTemplateBuilder();
                    buildDirectoryEntries(file, subBuilder);
                    builder.addDirectory(file.getName(), subBuilder);
                } else {
                    builder.addFile(file.getName());
                }
            }
        }
    }

    private static class DirectoryEntry {
        private final String name;
        private final boolean isDirectory;
        private final List<DirectoryEntry> children = new ArrayList<>();

        DirectoryEntry(String name, boolean isDirectory) {
            this.name = name;
            this.isDirectory = isDirectory;
        }
    }

    private String title = "Directory Listing";
    private final List<DirectoryEntry> entries = new ArrayList<>();

    public DirectoryListingHtmlTemplateBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public DirectoryListingHtmlTemplateBuilder addFile(String fileName) {
        entries.add(new DirectoryEntry(fileName, false));
        return this;
    }

    public DirectoryListingHtmlTemplateBuilder addDirectory(String directoryName,
            DirectoryListingHtmlTemplateBuilder subBuilder) {
        DirectoryEntry dir = new DirectoryEntry(directoryName, true);
        dir.children.addAll(subBuilder.entries);
        entries.add(dir);
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <title>").append(title).append("</title>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("  <h1>").append(title).append("</h1>\n");
        sb.append(buildEntries(entries));
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }

    private String buildEntries(List<DirectoryEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <ul>\n");
        if (entries.isEmpty()) {
            sb.append("    <li>📭 Empty directory listing</li>\n");
        } else {
            for (DirectoryEntry entry : entries) {
                String icon = entry.isDirectory ? "📁" : "📄";
                sb.append("    <li>").append(icon).append(" ").append(entry.name);
                if (entry.isDirectory && !entry.children.isEmpty()) {
                    sb.append("\n").append(buildEntries(entry.children));
                    sb.append("    ");
                }
                sb.append("</li>\n");
            }
        }
        sb.append("  </ul>\n");
        return sb.toString();
    }
}