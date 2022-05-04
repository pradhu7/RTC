package com.apixio.model.external;

/**
 * Supported MIME types for clinical documents.
 */
public enum AxmMimeType {
    TXT("text/plain"),
    RTF("text/rtf"),
    HTML("text/html"),
    PDF("application/pdf"),
    WORD("application/msword"),
    WORD1("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    CCR("application/x-ccr"),
    CCD("application/x-ccd"),
    XPS("application/vnd.ms-xpsdocument"),
    XPS1("application/oxps"),
    JPG("image/jpeg"),
    TIFF("image/tiff"),
    PNG("image/png"),
    BMP("image/bmp"),
    FHIR("application/fhir+json");

    private final String mime;

    AxmMimeType(String mime) {
        this.mime = mime;
    }

    @Override
    public String toString() {
        return mime;
    }
}
