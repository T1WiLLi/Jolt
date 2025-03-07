package ca.jolt.files;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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