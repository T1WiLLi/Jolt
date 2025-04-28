package io.github.t1willi.utils;

/**
 * Enum representing various MIME types and their associated file extensions.
 * Provides a method to retrieve the MIME type based on a file extension.
 */
public enum MimeInterpreter {
    /**
     * AAC audio file.
     */
    ACC("audio/aac", ".aac"),

    /**
     * AbiWord document.
     */
    ABW("application/x-abiword", ".abw"),

    /**
     * Animated PNG image.
     */
    APNG("image/apng", ".apng"),

    /**
     * Archive document (multiple files embedded).
     */
    ARC("application/x-freearc", ".arc"),

    /**
     * AVIF image.
     */
    AVIF("image/avif", ".avif"),

    /**
     * AVI: Audio Video Interleave.
     */
    AVI("video/x-msvideo", ".avi"),

    /**
     * Amazon Kindle eBook format.
     */
    AZW("application/vnd.amazon.ebook", ".azw"),

    /**
     * Any kind of binary data.
     */
    BIN("application/octet-stream", ".bin"),

    /**
     * Windows OS/2 Bitmap Graphics.
     */
    BMP("image/bmp", ".bmp"),

    /**
     * BZip archive.
     */
    BZ("application/x-bzip", ".bz"),

    /**
     * BZip2 archive.
     */
    BZ2("application/x-bzip2", ".bz2"),

    /**
     * CD audio track.
     */
    CDA("application/x-cdf", ".cda"),

    /**
     * C-Shell script.
     */
    CSH("application/x-csh", ".csh"),

    /**
     * Cascading Style Sheets (CSS).
     */
    CSS("text/css", ".css"),

    /**
     * Comma-separated values (CSV).
     */
    CSV("text/csv", ".csv"),

    /**
     * Microsoft Word.
     */
    DOC("application/msword", ".doc"),

    /**
     * Microsoft Word (OpenXML).
     */
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),

    /**
     * MS Embedded OpenType fonts.
     */
    EOT("application/vnd.ms-fontobject", ".eot"),

    /**
     * Electronic publication (EPUB).
     */
    EPUB("application/epub+zip", ".epub"),

    /**
     * GZip Compressed Archive.
     */
    GZ("application/gzip", ".gz"),

    /**
     * Graphics Interchange Format (GIF).
     */
    GIF("image/gif", ".gif"),

    /**
     * HyperText Markup Language (HTML).
     */
    HTML("text/html", ".html"),

    /**
     * Icon format.
     */
    ICO("image/vnd.microsoft.icon", ".ico"),

    /**
     * iCalendar format.
     */
    ICS("text/calendar", ".ics"),

    /**
     * Java Archive (JAR).
     */
    JAR("application/java-archive", ".jar"),

    /**
     * JPEG images.
     */
    JPEG("image/jpeg", ".jpeg"),

    /**
     * JPEG images.
     */
    JPG("image/jpeg", ".jpg"),

    /**
     * JavaScript.
     */
    JS("text/javascript", ".js"),

    /**
     * JSON format.
     */
    JSON("application/json", ".json"),

    /**
     * JSON-LD format.
     */
    JSONLD("application/ld+json", ".jsonld"),

    /**
     * Musical Instrument Digital Interface (MIDI).
     */
    MID("audio/midi", ".mid"),

    /**
     * Musical Instrument Digital Interface (MIDI).
     */
    MIDI("audio/midi", ".midi"),

    /**
     * JavaScript module.
     */
    MJS("text/javascript", ".mjs"),

    /**
     * MP3 audio.
     */
    MP3("audio/mpeg", ".mp3"),

    /**
     * MP4 video.
     */
    MP4("video/mp4", ".mp4"),

    /**
     * MPEG Video.
     */
    MPEG("video/mpeg", ".mpeg"),

    /**
     * Apple Installer Package.
     */
    MPKG("application/vnd.apple.installer+xml", ".mpkg"),

    /**
     * OpenDocument presentation document.
     */
    ODP("application/vnd.oasis.opendocument.presentation", ".odp"),

    /**
     * OpenDocument spreadsheet document.
     */
    ODS("application/vnd.oasis.opendocument.spreadsheet", ".ods"),

    /**
     * OpenDocument text document.
     */
    ODT("application/vnd.oasis.opendocument.text", ".odt"),

    /**
     * OGG audio.
     */
    OGA("audio/ogg", ".oga"),

    /**
     * OGG video.
     */
    OGV("video/ogg", ".ogv"),

    /**
     * OGG.
     */
    OGX("application/ogg", ".ogx"),

    /**
     * OPUS audio.
     */
    OPUS("audio/ogg", ".opus"),

    /**
     * OpenType font.
     */
    OTF("font/otf", ".otf"),

    /**
     * Portable Network Graphics.
     */
    PNG("image/png", ".png"),

    /**
     * Adobe Portable Document Format (PDF).
     */
    PDF("application/pdf", ".pdf"),

    /**
     * Hypertext Preprocessor (Personal Home Page).
     */
    PHP("application/x-httpd-php", ".php"),

    /**
     * Microsoft PowerPoint.
     */
    PPT("application/vnd.ms-powerpoint", ".ppt"),

    /**
     * Microsoft PowerPoint (OpenXML).
     */
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"),

    /**
     * RAR archive.
     */
    RAR("application/vnd.rar", ".rar"),

    /**
     * Rich Text Format (RTF).
     */
    RTF("application/rtf", ".rtf"),

    /**
     * Bourne shell script.
     */
    SH("application/x-sh", ".sh"),

    /**
     * Scalable Vector Graphics (SVG).
     */
    SVG("image/svg+xml", ".svg"),

    /**
     * Tape Archive (TAR).
     */
    TAR("application/x-tar", ".tar"),

    /**
     * Tagged Image File Format (TIFF).
     */
    TIFF("image/tiff", ".tiff"),

    /**
     * Tagged Image File Format (TIFF).
     */
    TIF("image/tiff", ".tif"),

    /**
     * MPEG transport stream.
     */
    TS("video/mp2t", ".ts"),

    /**
     * TrueType Font.
     */
    TTF("font/ttf", ".ttf"),

    /**
     * Text, (generally ASCII or ISO 8859-n).
     */
    TXT("text/plain", ".txt"),

    /**
     * Microsoft Visio.
     */
    VSD("application/vnd.visio", ".vsd"),

    /**
     * Waveform Audio Format.
     */
    WAV("audio/wav", ".wav"),

    /**
     * WEBM audio.
     */
    WEBA("audio/webm", ".weba"),

    /**
     * WEBM video.
     */
    WEBM("video/webm", ".webm"),

    /**
     * WEBP image.
     */
    WEBP("image/webp", ".webp"),

    /**
     * Web Open Font Format (WOFF).
     */
    WOFF("font/woff", ".woff"),

    /**
     * Web Open Font Format (WOFF).
     */
    WOFF2("font/woff2", ".woff2"),

    /**
     * XHTML.
     */
    XHTML("application/xhtml+xml", ".xhtml"),

    /**
     * Microsoft Excel.
     */
    XLS("application/vnd.ms-excel", ".xls"),

    /**
     * Microsoft Excel (OpenXML).
     */
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),

    /**
     * XML.
     */
    XML("application/xml", ".xml"),

    /**
     * XUL.
     */
    XUL("application/vnd.mozilla.xul+xml", ".xul"),

    /**
     * ZIP archive.
     */
    ZIP("application/zip", ".zip"),

    /**
     * 3GPP audio/video container.
     */
    _3GP("video/3gpp", ".3gp"),

    /**
     * 3GPP2 audio/video container.
     */
    _3G2("video/3gpp2", ".3g2"),

    /**
     * 7-zip archive.
     */
    _7Z("application/x-7z-compressed", ".7z");

    /**
     * Retrieves the MIME type based on the file extension.
     *
     * @param extension the file extension
     * @return the corresponding MIME type, or "text/plain" if not found
     */
    public static String getMime(String extension) {
        for (MimeInterpreter mime : values()) {
            if (mime.getExtension().equals(extension)) {
                return mime.getMimeType();
            }
        }
        return "text/plain";
    }

    private final String mimeType;
    private final String extension;

    /**
     * Constructor for MimeInterpreter enum.
     *
     * @param mimeType  the MIME type
     * @param extension the file extension
     */
    private MimeInterpreter(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    /**
     * Gets the MIME type.
     *
     * @return the MIME type
     */
    private String getMimeType() {
        return mimeType;
    }

    /**
     * Gets the file extension.
     *
     * @return the file extension
     */
    private String getExtension() {
        return extension;
    }
}
