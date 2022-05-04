package apixio.infraconfig.core;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SshKeyUtilsTest {

    public final static String TEST_PUBLIC_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQDcsjWDyOKa9spv0YiXMT8CgM4g1g764XBzrni1zV8AOjfXxhIFpXBvOMmWHCH8aPfI+HqarT+qvLTtRPnTVFLoGKBFQqVKKmSRuYG/Yjjuu2DHV6n9qqQmhXkSbXOz8ZgpGGQahqBdoSoCOyYTbd7wyKx5smONLbFCr1wt5f0kow==";
    public final static String TEST_PRIVATE_KEY = "-----BEGIN OPENSSH PRIVATE KEY-----\nb3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAlwAAAAdzc2gtcn\nNhAAAAAwEAAQAAAIEA3LI1g8jimvbKb9GIlzE/AoDOINYO+uFwc654tc1fADo318YSBaVw\nbzjJlhwh/Gj3yPh6mq0/qry07UT501RS6BigRUKlSipkkbmBv2I47rtgx1ep/aqkJoV5Em\n1zs/GYKRhkGoagXaEqAjsmE23e8MisebJjjS2xQq9cLeX9JKMAAAIQoQO576EDue8AAAAH\nc3NoLXJzYQAAAIEA3LI1g8jimvbKb9GIlzE/AoDOINYO+uFwc654tc1fADo318YSBaVwbz\njJlhwh/Gj3yPh6mq0/qry07UT501RS6BigRUKlSipkkbmBv2I47rtgx1ep/aqkJoV5Em1z\ns/GYKRhkGoagXaEqAjsmE23e8MisebJjjS2xQq9cLeX9JKMAAAADAQABAAAAgDMZN/bJXl\n5O4dQ+CYgoKNSlihRkO5eu9uBx9xTw9hwRXrig7a9M/NljppkQ7nWIwEZR8eE6V9FqW+HL\n5KQflAom1y5f3YM1KPjJZiNp0cwRYhnnrloLGKgMJsWjaGnlOzDuTEZFYiztPF+NO5iQ8G\nL5wURo1H/p9X+3bRopsQWBAAAAQD0BYbeFWhoNdjwPaFQZI69aOrrlCSyXeT4n2DPBxqkk\nLsCsFjBKno5rLo1kA4XMTBYrkMhmc4AdBeF3HH769L4AAABBAPwOivkHHhZ/EfLNcNywzy\nHzTEeszvtdeFB0HQ+PkD6BdsGk+qhsmWjvwsAsIIsj3yKdL4Uq+8h/LMrsmnuGOyEAAABB\nAOAmEghc5a5DRNKiI84XFwJLcnZBpj3bkHeCySDInieus4dH55ebSA8s6BhuOhJ1oSaUIQ\nKFp9wUD4iCTz5pS0MAAAAXa21jZ292ZXJuQEFQWDIyOTkubG9jYWwBAgME\n-----END OPENSSH PRIVATE KEY-----";

    @Before
    public void doBeforeEachTest() {
        Logger.getGlobal().setLevel(Level.INFO);
    }

    @Test
    public void create() {
        try {
            SshKeyUtils keyUtils = new SshKeyUtils(TEST_PUBLIC_KEY, TEST_PRIVATE_KEY);
            assertTrue(keyUtils.validKeyPair());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            fail("Error while running test");
        }
    }

    @Test
    public void isValidPublicKey() {
        try {
            SshKeyUtils keyUtils = new SshKeyUtils(TEST_PUBLIC_KEY);
            assertTrue(keyUtils.isValidPublicKey());
            keyUtils.sshPublicKey = "ssh-rsa AAAAABBBCCCCCi==";
            assertFalse(keyUtils.isValidPublicKey());
            keyUtils.sshPublicKey = "thisisnotavalidkeyy";
            assertFalse(keyUtils.isValidPublicKey());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            fail("Error while running test");
        }
    }

    @Test
    public void generateSshKeyPair() {
        try {
            SshKeyUtils keyUtils = new SshKeyUtils();
            keyUtils.generateSshKeyPair("ecdsa-sha2-nistp384", 384);
            assertNotNull(keyUtils.privateKey);
            assertNotNull(keyUtils.sshPublicKey);
            keyUtils = new SshKeyUtils(keyUtils.sshPublicKey, keyUtils.privateKey);
            assertNotNull(keyUtils.privateKey);
            assertNotNull(keyUtils.sshPublicKey);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            fail("Unable to generate new ssh key");
        }
    }

    @Test
    public void validKeyPair() {
        try {
            SshKeyUtils keyUtils = new SshKeyUtils();
            keyUtils.generateSshKeyPair("ssh-rsa", 2048);
            assertTrue(keyUtils.validKeyPair());
            keyUtils.privateKey = TEST_PRIVATE_KEY;
            assertFalse(keyUtils.validKeyPair());
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            fail("Unable to generate new ssh key");
        }
    }
}