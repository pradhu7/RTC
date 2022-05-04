package apixio.infraconfig.core;

import software.amazon.awssdk.http.ContentStreamProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

// based off FileContentStreamProvider to allow for strings as content bodies
public class StringContentStreamProvider implements ContentStreamProvider {
    private final String content;
    private InputStream currentStream;

    public StringContentStreamProvider(String content) {
        this.content = content;
    }

    @Override
    public InputStream newStream() {
        closeCurrentStream();
        currentStream = invokeSafely(() -> new ByteArrayInputStream(content.getBytes()));
        return currentStream;
    }

    private void closeCurrentStream() {
        if (currentStream != null) {
            invokeSafely(currentStream::close);
            currentStream = null;
        }
    }
}
