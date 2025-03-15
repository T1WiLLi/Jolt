package ca.jolt.utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ca.jolt.server.config.ConfigurationManager;
import ca.jolt.server.config.ServerConfig;
import jakarta.servlet.http.HttpServletResponse;

public class DirectoryListingHtmlTemplateBuilder {

    /**
     * A function to try and serve a directory listing as an HTML page.
     * 
     * @param path the path to the directory to serve
     * @param res  the HttpServletResponse object to write to
     * @return true if the directory listing was served, false otherwise
     * @throws IOException if an I/O error occurs
     */
    public static boolean tryServeDirectoryListing(String path, HttpServletResponse res) throws IOException {
        ServerConfig serverConfig = ConfigurationManager.getInstance().getServerConfig();
        if (!serverConfig.isDirectoryListingEnabled()) {
            return false;
        }

        String listingPath = serverConfig.getDirectoryListingPath();
        if (!path.equals(listingPath)) {
            return false;
        }

        URL staticUrl = DirectoryListingHtmlTemplateBuilder.class.getClassLoader().getResource("static");
        if (staticUrl == null) {
            return false;
        }

        File staticDir;
        try {
            staticDir = new File(staticUrl.toURI());
        } catch (URISyntaxException e) {
            return false;
        }

        if (!staticDir.isDirectory()) {
            return false;
        }

        DirectoryListingHtmlTemplateBuilder builder = new DirectoryListingHtmlTemplateBuilder()
                .setTitle("Directory Listing 📂");
        buildDirectoryEntries(staticDir, builder);

        String html = builder.build();
        res.setContentType("text/html;charset=UTF-8");
        res.getWriter().write(html);
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

    /**
     * Sets the title for the directory listing page.
     *
     * @param title The page title.
     * @return This builder instance.
     */
    public DirectoryListingHtmlTemplateBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Adds a file entry to the listing.
     *
     * @param fileName The name of the file.
     * @return This builder instance.
     */
    public DirectoryListingHtmlTemplateBuilder addFile(String fileName) {
        entries.add(new DirectoryEntry(fileName, false));
        return this;
    }

    /**
     * Adds a directory entry to the listing with its nested contents.
     *
     * @param directoryName The name of the directory.
     * @param subBuilder    A builder containing the nested entries of the
     *                      directory.
     * @return This builder instance.
     */
    public DirectoryListingHtmlTemplateBuilder addDirectory(String directoryName,
            DirectoryListingHtmlTemplateBuilder subBuilder) {
        DirectoryEntry dir = new DirectoryEntry(directoryName, true);
        dir.children.addAll(subBuilder.entries);
        entries.add(dir);
        return this;
    }

    /**
     * Builds the final HTML string for the directory listing.
     *
     * @return A string containing the complete HTML.
     */
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

    /**
     * Recursively builds the HTML for a list of directory entries.
     *
     * @param entries The list of entries to process.
     * @return A string representing nested HTML unordered lists.
     */
    private String buildEntries(List<DirectoryEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <ul>\n");
        for (DirectoryEntry entry : entries) {
            String icon = entry.isDirectory ? "📁" : "📄";
            sb.append("    <li>").append(icon).append(" ").append(entry.name);
            if (entry.isDirectory && !entry.children.isEmpty()) {
                sb.append("\n").append(buildEntries(entry.children));
                sb.append("    ");
            }
            sb.append("</li>\n");
        }
        sb.append("  </ul>\n");
        return sb.toString();
    }
}
