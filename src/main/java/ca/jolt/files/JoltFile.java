package ca.jolt.files;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import lombok.Getter;

/**
 * Represents an uploaded file stored in memory.
 * Provides metadata such as field name, original filename, content type, and
 * file size,
 * along with direct access to the file data as a byte array or input stream.
 */
@Getter
public class JoltFile {
    private final String fileName;
    private final String contentType;
    private final long size;
    private final byte[] data;

    public JoltFile(String fileName, String contentType, long size, byte[] data) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.data = data;
    }

    /**
     * Creates a new input stream to read the file data.
     */
    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }
}
