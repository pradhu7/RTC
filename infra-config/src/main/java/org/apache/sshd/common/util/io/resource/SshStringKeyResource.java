package org.apache.sshd.common.util.io.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class to allow private keys to be loaded via a string input
 */
public class SshStringKeyResource extends AbstractIoResource<String> {

    public SshStringKeyResource(String key) {
        super(String.class, key);
    }

    public String getStringKey() {
        return getResourceValue();
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(getStringKey().getBytes());
    }
}
