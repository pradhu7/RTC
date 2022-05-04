package apixio.infraconfig.api;

import apixio.infraconfig.InfraConfigTestHelper;
import apixio.infraconfig.core.InvalidSftpUserException;
import apixio.infraconfig.core.PasswordHashGeneratorHelper;
import apixio.infraconfig.core.SshKeyUtilsTest;
import apixio.infraconfig.testhelpers.LocalstackTestHelper;
import com.apixio.SysServices;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.ResourceHelpers;
import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SftpUserModelTest extends LocalstackTestHelper {

    private SftpUserModel sftpUserModel;
    private String TEST_PUBLIC_KEY = SshKeyUtilsTest.TEST_PUBLIC_KEY;
    private String TEST_PRIVATE_KEY = SshKeyUtilsTest.TEST_PRIVATE_KEY;
    private String APIXIO_ORG_ID = "UO_57faf530-e6e3-4677-afc5-bc87ff00ace9";

    @Before
    public void doBeforeEachTest() {
        Logger.getGlobal().setLevel(Level.INFO);
    }

    @Before
    public void initTest() {
        sftpUserModel = new SftpUserModel();
    }

    @Test
    public void setPrivateKey() {
        try {
            String generateInfo = "generate ssh-rsa 2048";
            sftpUserModel.setPrivateKey(generateInfo);
            assertNotEquals(sftpUserModel.privateKeyRaw, generateInfo);
            assertTrue(sftpUserModel.privateKeyRaw.contains("BEGIN"));
            assertTrue(sftpUserModel.publicKey.startsWith("ssh-rsa"));
            assertNull(sftpUserModel.privateKey);
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setPrivateKey("generate invalid-type 1234");
            });
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setPrivateKey("generate ssh-rsa 1024");
            });
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setPrivateKey("generate ssh-rsa");
            });
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setPrivateKey("thisisnotvalid");
            });
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.publicKey = null;
                sftpUserModel.setPrivateKey(TEST_PRIVATE_KEY);
            });
        } catch (InvalidSftpUserException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void setUsername() {
        try {
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setUsername("");
            });
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setUsername("George");
            });
            sftpUserModel.setUsername("george");
            assertEquals(sftpUserModel.username, "george");
        } catch (InvalidSftpUserException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void setPassword() {
        try {
            sftpUserModel.setPassword(null);
            assertNotNull(sftpUserModel.passwordRaw);
            sftpUserModel.setPassword("");
            assertFalse(sftpUserModel.passwordRaw.isEmpty());
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setPassword("password");
            });
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setPassword("P@ssw0rd123");
            });
            sftpUserModel.setPassword("Thi5isa$ecureP@sswordKindaSorta");
            assertEquals(sftpUserModel.passwordRaw, "Thi5isa$ecureP@sswordKindaSorta");
            assertTrue(sftpUserModel.password.startsWith(PasswordHashGeneratorHelper.hashPrefix));
            String passwordHash = sftpUserModel.password;
            sftpUserModel.setPassword(passwordHash);
            assertTrue(sftpUserModel.password.startsWith(PasswordHashGeneratorHelper.hashPrefix));
            assertNull(sftpUserModel.passwordRaw);

        } catch (InvalidSftpUserException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void setPublicKey() {
        try {
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setPublicKey("notavalidkey");
            });

            sftpUserModel.setPublicKey(TEST_PUBLIC_KEY);
            assertEquals(sftpUserModel.publicKey, TEST_PUBLIC_KEY);
        } catch (InvalidSftpUserException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void setAcceptedIpNetwork() {
        try {
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setAcceptedIpNetwork(Arrays.asList("300.10.9.500/32"));
            });
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setAcceptedIpNetwork(Arrays.asList("review.apixio.com"));
            });
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setAcceptedIpNetwork(Arrays.asList("garbage"));
            });
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setAcceptedIpNetwork(Arrays.asList());
            });
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setAcceptedIpNetwork(Arrays.asList(""));
            });
            assertThrows(InvalidSftpUserException.class, () -> {
                sftpUserModel.setAcceptedIpNetwork((Set<String>) null);
            });

            Set<String> testNetworks = Sets.newLinkedHashSet("10.10.10.10/32", "192.168.10.0/24");
            sftpUserModel.setAcceptedIpNetwork(testNetworks);
            assertEquals(sftpUserModel.acceptedIpNetwork, testNetworks);
            assertEquals(sftpUserModel.removedCidrs, Sets.newLinkedHashSet());

            testNetworks = Sets.newLinkedHashSet("-10.10.10.10/32", "10.10.10.10/32", "192.168.10.0/24");
            sftpUserModel.setAcceptedIpNetwork(testNetworks);
            assertEquals(sftpUserModel.acceptedIpNetwork, Sets.newLinkedHashSet("192.168.10.0/24"));
            assertEquals(sftpUserModel.removedCidrs, Sets.newLinkedHashSet("10.10.10.10/32"));

            testNetworks = Sets.newLinkedHashSet("-10.10.10.10", "10.10.10.10/32", "192.168.10.0/24");
            sftpUserModel.setAcceptedIpNetwork(testNetworks);
            assertEquals(sftpUserModel.acceptedIpNetwork, Sets.newLinkedHashSet("192.168.10.0/24"));
            assertEquals(sftpUserModel.removedCidrs, Sets.newLinkedHashSet("10.10.10.10/32"));

            testNetworks = Sets.newLinkedHashSet("10.10.10.10/32", "192.168.10.0/24");
            sftpUserModel.setAcceptedIpNetwork(testNetworks);
            assertEquals(sftpUserModel.acceptedIpNetwork, testNetworks);
            sftpUserModel.removeAcceptedIpNetwork(Sets.newLinkedHashSet("10.10.10.10"));
            assertEquals(sftpUserModel.acceptedIpNetwork, Sets.newLinkedHashSet("192.168.10.0/24"));
            assertEquals(sftpUserModel.removedCidrs, Sets.newLinkedHashSet("10.10.10.10/32"));

            testNetworks = Sets.newLinkedHashSet("10.10.10.10/32", "192.168.10.0/24");
            sftpUserModel.setAcceptedIpNetwork(testNetworks);
            assertEquals(sftpUserModel.acceptedIpNetwork, testNetworks);
            sftpUserModel.removeAcceptedIpNetwork(Sets.newLinkedHashSet("10.10.10.10/32"));
            assertEquals(sftpUserModel.acceptedIpNetwork, Sets.newLinkedHashSet("192.168.10.0/24"));
            assertEquals(sftpUserModel.removedCidrs, Sets.newLinkedHashSet("10.10.10.10/32"));

            testNetworks = Sets.newLinkedHashSet("-10.10.10.10", "10.10.10.10/32", "192.168.10.0/24");
            sftpUserModel.setAcceptedIpNetwork(testNetworks);
            Set<String> removeCidrs = sftpUserModel.removedCidrs;
            assertEquals(sftpUserModel.acceptedIpNetwork, Sets.newLinkedHashSet("192.168.10.0/24"));
            testNetworks = Sets.newLinkedHashSet("10.10.10.10/32", "192.168.10.0/24");
            sftpUserModel.setAcceptedIpNetwork(testNetworks);
            assertEquals(sftpUserModel.acceptedIpNetwork, testNetworks);
            sftpUserModel.removeAcceptedIpNetwork(removeCidrs);
            assertEquals(sftpUserModel.acceptedIpNetwork, Sets.newLinkedHashSet("192.168.10.0/24"));
            assertEquals(sftpUserModel.removedCidrs, Sets.newLinkedHashSet("10.10.10.10/32"));


        } catch (InvalidSftpUserException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void validateGroup() throws Exception {
        SysServices sysServices = InfraConfigTestHelper.getSysServices();

        assertThrows(InvalidSftpUserException.class, () -> {
            sftpUserModel.setGroup(null);
        });

        sftpUserModel.setGroup(APIXIO_ORG_ID);
        assertTrue(sftpUserModel.validateGroup(sysServices));

        // basically the same as the apixio org but last 3 chars removed
        sftpUserModel.setGroup("UO_57faf530-e6e3-4677-afc5-bc87ff00abc0");
        assertThrows(InvalidSftpUserException.class, () -> {
            sftpUserModel.setGroup(null);
        });
    }

    @Test
    public void setHomeDirectoryDetails() throws InvalidSftpUserException {
        assertThrows(InvalidSftpUserException.class, () -> {
            sftpUserModel.setHomeDirectoryDetails("badjson");
        });
        assertThrows(InvalidSftpUserException.class, () -> {
            sftpUserModel.setHomeDirectoryDetails("{\"badjson\": true}");
        });
        assertThrows(InvalidSftpUserException.class, () -> {
            sftpUserModel.setHomeDirectoryDetails("[{\"badjson\": true}]");
        });
        String testEntry = "[{\"Entry\":\"/\",\"Target\":\"/bucket/path\"}]";
        sftpUserModel.setHomeDirectoryDetails(testEntry);
        assertEquals(sftpUserModel.homeDirectoryDetails, testEntry);

        List<SftpHomeDirectoryDetails> homeDirectoryDetails = new ArrayList<>();
        homeDirectoryDetails.add(
                SftpHomeDirectoryDetails.SftpHomeDirectoryDetailsBuilder.builder()
                        .withEntry("/")
                        .withTarget("/bucket/path")
                        .build()
        );

        sftpUserModel.setHomeDirectoryDetails(homeDirectoryDetails);
        assertEquals(sftpUserModel.homeDirectoryDetails, testEntry);
    }

    @Test
    public void setHomeDirectoryType() throws InvalidSftpUserException {
        assertThrows(InvalidSftpUserException.class, () -> {
            sftpUserModel.setHomeDirectoryType("BAD");
        });
        sftpUserModel.setHomeDirectoryType(null);
        assertEquals(sftpUserModel.homeDirectoryType, "LOGICAL");
        sftpUserModel.setHomeDirectoryType("LOGICAL");
        assertEquals(sftpUserModel.homeDirectoryType, "LOGICAL");
    }

    @Test
    public void testMapper() throws InvalidSftpUserException, IOException {
        String resourcePath = ResourceHelpers.resourceFilePath("fixtures/user-with-comma-network.json");
        File resourceFile = new File(resourcePath);
        ObjectMapper objectMapper = new ObjectMapper();
        SftpUserModel deserializedSftpUser = objectMapper.readValue(resourceFile, SftpUserModel.class);
        String resourceContents = String.join("\n", Files.readAllLines(resourceFile.toPath()));
        // make sure active is set so later assumption is accurate
        assertFalse(resourceContents.contains("\"Active\""));
        // make sure AccceptedIpNetwork is set to assumptions are accurate
        assertTrue(resourceContents.contains("\"AcceptedIpNetwork\""));
        assertEquals(deserializedSftpUser.acceptedIpNetwork.size(), 2);
        assertEquals(deserializedSftpUser.awsRole, "arn:aws:iam::088921318242:role/stg/sftp-stg-transferfamily");
        assertTrue(deserializedSftpUser.initialized);
        // "Active" is not defined in the test json file so ensure it defaults to true on serialisation if missing
        assertTrue(deserializedSftpUser.getActive());
    }

    @Test
    public void setActive() {
        assertTrue(sftpUserModel.getActive());
        sftpUserModel.setActive(false);
        assertFalse(sftpUserModel.getActive());
    }

    @Test
    public void removeAcceptedIpNetwork() throws InvalidSftpUserException {
        Set<String> testNetworks = Sets.newLinkedHashSet("10.10.10.10/32", "192.168.10.0/24");
        sftpUserModel.setAcceptedIpNetwork(testNetworks);
        sftpUserModel.removeAcceptedIpNetwork(Sets.newLinkedHashSet("10.10.10.10"));
        assertEquals(sftpUserModel.acceptedIpNetwork, Sets.newLinkedHashSet("192.168.10.0/24"));
        assertEquals(sftpUserModel.removedCidrs, Sets.newLinkedHashSet("10.10.10.10/32"));
        // verify impotency
        sftpUserModel.removeAcceptedIpNetwork(Sets.newLinkedHashSet("10.10.10.10/32"));
        assertEquals(sftpUserModel.acceptedIpNetwork, Sets.newLinkedHashSet("192.168.10.0/24"));
        assertEquals(sftpUserModel.removedCidrs, Sets.newLinkedHashSet("10.10.10.10/32"));

        // verify unable to remove last network
        assertThrows(InvalidSftpUserException.class, () -> {
            sftpUserModel.removeAcceptedIpNetwork(Sets.newLinkedHashSet("192.168.10.0/24"));
        });

        testNetworks = Sets.newLinkedHashSet("10.10.10.10/32", "192.168.10.0/24", "10.10.1.1");
        sftpUserModel.setAcceptedIpNetwork(testNetworks);
        sftpUserModel.removeAcceptedIpNetwork(Sets.newLinkedHashSet("10.10.10.10", "192.168.10.0/24"));
        assertEquals(sftpUserModel.acceptedIpNetwork, Sets.newLinkedHashSet("10.10.1.1/32"));
        assertEquals(sftpUserModel.removedCidrs, Sets.newLinkedHashSet("10.10.10.10/32", "192.168.10.0/24"));
    }

    @Test
    public void addAcceptedIpNetwork() throws InvalidSftpUserException {
        Set<String> testNetworks = Sets.newLinkedHashSet("10.10.10.10/32", "192.168.10.0/24");
        sftpUserModel.setAcceptedIpNetwork(testNetworks);
        sftpUserModel.addAcceptedIpNetwork(Sets.newLinkedHashSet("10.10.1.1", "10.10.1.1/32"));
        assertEquals(sftpUserModel.acceptedIpNetwork, Sets.newLinkedHashSet("10.10.10.10/32", "192.168.10.0/24", "10.10.1.1/32"));
        assertTrue(sftpUserModel.removedCidrs.isEmpty());
        sftpUserModel.addAcceptedIpNetwork(Sets.newLinkedHashSet("-10.10.1.1"));
        assertEquals(sftpUserModel.acceptedIpNetwork, testNetworks);
        assertEquals(sftpUserModel.removedCidrs, Sets.newLinkedHashSet("10.10.1.1/32"));
    }

    @Test
    public void validateNetworks() throws InvalidSftpUserException {
        sftpUserModel.acceptedIpNetwork = Sets.newLinkedHashSet();
        assertThrows(InvalidSftpUserException.class, () -> {
            sftpUserModel.validateAcceptedIpcidrs();
        });
    }
}