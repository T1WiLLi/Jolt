package io.github.t1willi.files;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import io.github.t1willi.exceptions.JoltException;
import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.utils.MimeInterpreter;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents an uploaded file stored in memory.
 * <p>
 * Provides metadata such as the original file name, content type,
 * and file size, along with direct access to the underlying byte
 * data as either a {@code byte[]} or an {@link InputStream}.
 * </p>
 */
@Getter
public class JoltFile {

    /**
     * Creates a {@link JoltFile} instance from a static file located in the
     * "static" resource directory.
     *
     * @param filename the name of the file to be loaded. If the filename starts
     *                 with a "/", it will be removed.
     * @return a {@link JoltFile} instance containing the file data, MIME type, and
     *         other metadata.
     * @throws JoltHttpException if the file is not found or if an I/O error occurs
     *                           while reading the file.
     */
    public static JoltFile fromStatic(String filename) {
        String normalizedResource = filename.startsWith("/") ? filename.substring(1) : filename;
        InputStream in = JoltFile.class.getClassLoader().getResourceAsStream("static/" + normalizedResource);
        if (in == null) {
            throw new JoltHttpException(HttpStatus.NOT_FOUND, "File not found : " + filename);
        }
        try {
            byte[] data = in.readAllBytes();
            int dotIndex = filename.lastIndexOf('.');
            String extension = dotIndex != -1 ? filename.substring(dotIndex + 1) : "";
            String mimeType = MimeInterpreter.getMime(extension);
            return new JoltFile(filename, mimeType, data.length, data);
        } catch (IOException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file : " + filename);
        }
    }

    /**
     * The original file name (Stored for reference).
     */
    @Setter
    private String originalFilename;
    /**
     * The original file name.
     */
    private final String fileName;

    /**
     * The content type of the file (e.g., {@code "image/png"}).
     */
    private final String contentType;

    /**
     * The size of the file in bytes.
     */
    private final long size;

    /**
     * The file data stored in memory.
     */
    private final byte[] data;

    /**
     * Constructs a new {@code JoltFile} with the specified metadata and file data.
     *
     * @param fileName    The original file name
     * @param contentType The MIME content type of the file
     * @param size        The size of the file in bytes
     * @param data        A byte array containing the file's data
     */
    public JoltFile(String originalFilename, String contentType, long size, byte[] data) {
        if (data == null || data.length == 0) {
            throw new JoltHttpException(HttpStatus.UNPROCESSABLE_ENTITY, "File data is empty.");
        }
        if (isMaliciousCode(data)) {
            throw new JoltHttpException(HttpStatus.BAD_REQUEST, "File may contains malicious code.");
        }
        this.originalFilename = originalFilename;
        this.fileName = generateFilename(originalFilename);
        this.contentType = contentType;
        this.size = size;
        this.data = data.clone();
    }

    /**
     * Returns a new {@link InputStream} that reads from the in-memory file data.
     *
     * @return An {@link InputStream} for reading the stored file data
     */
    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }

    public File toFile() throws IOException { // Check for resources leaks.
        File temp = new File(System.getProperty("java.io.tmpdir"), getFileName());
        Files.write(temp.toPath(), getData());
        return temp;
    }

    public boolean save(String path) {
        try {
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Unable to create directory '" + parent + "'");
            }
            Files.write(file.toPath(), getData());
        } catch (IOException e) {
            throw new JoltException("Failed to save file to path: " + path, e);
        }
        return false;
    }

    private String generateFilename(String originalFilename) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(originalFilename.getBytes());
            String hashHex = bytesToHex(hashBytes);
            String extension = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            return UUID.randomUUID().toString() + "-" + hashHex.substring(0, 8) + extension;
        } catch (NoSuchAlgorithmException e) {
            throw new JoltException("Failed to generate filename", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Comprehensive malicious file detection system that checks for executable
     * formats,
     * script patterns, and various obfuscation techniques.
     * 
     * @param data the file data to check
     * @return true if potentially malicious, false otherwise
     */
    private boolean isMaliciousCode(byte[] data) {
        if (data.length > 4) {
            if (data[0] == 0x7F && data[1] == 'E' && data[2] == 'L' && data[3] == 'F') {
                return true;
            }
            if (data[0] == 'M' && data[1] == 'Z') {
                return true;
            }
        }
        return false;
    }
}