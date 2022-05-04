package com.apixio.model.external;

import java.net.URI;
import java.util.Objects;

/**
 * Created by alarocca on 1/3/18.
 */
public class AxmDocumentPart {
    private URI uri;
    private String content;
    private String mimeType;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(uri, content, mimeType);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;

        final AxmDocumentPart that = (AxmDocumentPart) obj;
        return Objects.equals(this.uri, that.uri)
                && Objects.equals(this.content, that.content)
                && Objects.equals(this.mimeType, that.mimeType);
    }
}

