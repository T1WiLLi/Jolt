package ca.jolt.files;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.http.HttpStatus;
import ca.jolt.routing.MimeInterpreter;
import lombok.Getter;

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
    public JoltFile(String fileName, String contentType, long size, byte[] data) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.data = data;
    }

    /**
     * Returns a new {@link InputStream} that reads from the in-memory file data.
     *
     * @return An {@link InputStream} for reading the stored file data
     */
    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }
}