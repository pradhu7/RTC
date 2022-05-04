package apixio.infraconfig.client;

import apixio.infraconfig.api.SftpUserModel;
import apixio.infraconfig.core.InvalidSftpUserException;
import apixio.infraconfig.testhelpers.VaultTestHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ianferguson.vault.VaultException;
import io.ianferguson.vault.json.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.json.stream.JsonParsingException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// work-around for now so writes happen before they are read
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VaultClientTest extends VaultTestHelper {

    @Before
    public void doBeforeEachTest() throws Exception {
        initAwsMocks();
        Logger.getGlobal().setLevel(Level.INFO);
        getVaultClient();
    }

    private String writeRandomUser() throws Exception {
        String sftpUserId = UUID.randomUUID().toString();
        vaultClient.authenticate();
        SftpUserModel data = SftpUserModel.SftpUserModelBuilder.builder()
                .withUsername(sftpUserId)
                .withSftpServerId("test")
                .withAcceptedIpNetwork(Arrays.asList("10.10.10.10/8", "192.168.0.0/24", "192.168.10.10", "192.168.10.11/32"))
                .withGroup("INTERNAL")
                .withHomeDirectoryDetails("[{\"Entry\":\"/\",\"Target\":\"/bucket/path\"}]")
                .build();
        ObjectMapper oMapper = new ObjectMapper();
        Map<String, Object> map = oMapper.convertValue(data, Map.class);
        vaultClient.writeObject(String.format("secret/sftp/unit-test/%s", sftpUserId), map);
        return sftpUserId;
    }

    @Test
    public void authenticate() {
        try {
            vaultClient.authenticate();
            Long currentTime = (System.currentTimeMillis() / 1000) - 60;
            assert vaultClient.vaultClient.debug().health().getServerTimeUTC() >= currentTime;
        } catch (VaultException e) {
            e.printStackTrace();
            fail("Failed to authenticate to vault");
        }
    }

    @Test
    public void writeObject() {
        try {
            vaultClient.authenticate();
            SftpUserModel data = SftpUserModel.SftpUserModelBuilder.builder()
                    .withUsername("test-user")
                    .withSftpServerId("test")
                    .withAcceptedIpNetwork(Arrays.asList("10.10.10.10/8", "192.168.0.0/24", "192.168.10.10", "192.168.10.11/32"))
                    .withGroup("INTERNAL")
                    .withHomeDirectoryDetails("[{\"Entry\":\"/\",\"Target\":\"/bucket/path\"}]")
                    .build();
            ObjectMapper oMapper = new ObjectMapper();
            Map<String, Object> map = oMapper.convertValue(data, Map.class);
            Exception exception = assertThrows(VaultException.class, () -> {
                vaultClient.writeObject("secret/doesnotesit", map);
            });
            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("permission denied"));

            vaultClient.writeObject("secret/sftp/unit-test/test-user", map);
        } catch (VaultException | InvalidSftpUserException e) {
            e.printStackTrace();
            fail("Failed to write to vault");
        }
    }

    @Test
    public void getObject() throws Exception {
        try {
            vaultClient.authenticate();
            String sftpUserId = writeRandomUser();
            JsonObject jsonObject = vaultClient.getObject(String.format("secret/sftp/unit-test/%s", sftpUserId));
            ObjectMapper oMapper = new ObjectMapper();
            SftpUserModel sftpUser = oMapper.readValue(jsonObject.toString(), SftpUserModel.class);
            Assert.assertEquals(sftpUserId, sftpUser.username);
        } catch (VaultException e) {
            e.printStackTrace();
            fail("Failed to write to vault");
        } catch (JsonParsingException | JsonProcessingException e) {
            e.printStackTrace();
            fail("Unable to serialize data from vault");
        }
    }

    @Test
    public void listObjects() throws Exception {
        try {
            vaultClient.authenticate();
            writeRandomUser();
            JsonObject jsonObject = vaultClient.listObjects("secret/sftp/unit-test");
            assert (jsonObject.get("keys").asArray().size() > 0);
        } catch (VaultException e) {
            e.printStackTrace();
            fail("Failed to write to vault");
        }
    }
}