package apixio.infraconfig.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.apache.sshd.common.util.io.resource.SshStringKeyResource;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.postgresql.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.joining;

/**
 * handles all the logic of ssh key generation and validation.
 */
public class SshKeyUtils {

    private final static ImmutableMap<String, Set<Integer>> VALID_KEY_ALGS = ImmutableMap.of(
            "ssh-rsa", ImmutableSet.of(2048, 4096, 8192),
            "ecdsa-sha2-nistp384", ImmutableSet.of(384),
            "ecdsa-sha2-nistp521", ImmutableSet.of(521)
    );
    private final ImmutableMap<String, String> VERIFY_ALGS = ImmutableMap.of("RSA", "SHA256withRSA", "EC", "SHA256withECDSA");
    private final String SIGN_DATA = "Signing test data";
    public String sshPublicKey;
    public String privateKey;

    /**
     * @throws InvalidKeyException if a private key is passed with no corresponding public key.
     *                             This is done as EC keys are unable to recover the public key in mina-sshd
     */
    public SshKeyUtils() throws InvalidKeyException {
        this(null, null);
    }

    /**
     * @param sshPublicKey public key in SSH format
     * @throws InvalidKeyException if a private key is passed with no corresponding public key.
     *                             This is done as EC keys are unable to recover the public key in mina-sshd
     */
    public SshKeyUtils(String sshPublicKey) throws InvalidKeyException {
        this(sshPublicKey, null);
    }

    /**
     * @param sshPublicKey public key in SSH format
     * @param privateKey   private key in PEM format
     * @throws InvalidKeyException if a private key is passed with no corresponding public key.
     *                             This is done as EC keys are unable to recover the public key in mina-sshd
     */
    public SshKeyUtils(String sshPublicKey, String privateKey) throws InvalidKeyException {
        this.privateKey = privateKey;
        if (Objects.isNull(sshPublicKey) && Objects.nonNull(privateKey)) {
            this.sshPublicKey = null;
            throw new InvalidKeyException("Must pass public and private key or only public key");
        } else {
            this.sshPublicKey = sshPublicKey;
        }
    }

    /**
     * @return PrivateKey object
     * @throws InvalidKeyException if there are any problems when converting. This could be due to the key not being
     *                             in PKCS8 format or an unsupported algorithm of the private key
     */
    private PrivateKey tryGeneratePrivateKeyFromString() throws InvalidKeyException {

        SshStringKeyResource location = new SshStringKeyResource(privateKey);
        PrivateKey pKey;
        try (InputStream inputStream = location.openInputStream()) {
            pKey = SecurityUtils.loadKeyPairIdentities(null, location, inputStream, null).iterator().next().getPrivate();
        } catch (GeneralSecurityException | IOException | NullPointerException e) {
            e.printStackTrace();
            throw new InvalidKeyException("Unable to load private key");
        }
        return pKey;
    }

    /**
     * @return PublicKey object
     * @throws InvalidKeyException if there are any issues converting the ssh public key string into a
     *                             PublicKey object (bad format, invalid algorithm, etc)
     */
    private PublicKey tryGeneratePublicKeyFromSshString() throws InvalidKeyException {
        String[] keyInfo = sshPublicKey.split(" ");
        if (keyInfo.length < 2 || keyInfo.length > 3) {
            throw new InvalidKeyException(String.format("Invalid public key format %s", sshPublicKey));
        }

        PublicKeyEntry publicKeyObj = new PublicKeyEntry(keyInfo[0], Base64.decode(keyInfo[1]));
        try {
            return publicKeyObj.resolvePublicKey(null, new HashMap<>(), null);
        } catch (IOException | GeneralSecurityException e) {
            throw new InvalidKeyException("Unable to parse ssh private key");
        }
    }

    /**
     * @return True if the ssh public key string can be converted to a public key object. false if there are any problems
     */
    public Boolean isValidPublicKey() {
        try {
            if (Objects.isNull(sshPublicKey)) {
                throw new InvalidKeyException("Key is not initalized. Unable to validate public key");
            }
            tryGeneratePublicKeyFromSshString();
        } catch (InvalidKeyException e) {
            return false;
        }
        return true;
    }

    /**
     * @param keyPair the private key object to convert to PEM
     * @return the private key in PEM format
     * @throws IOException if there is an error writing to the StringWriter
     */
    private String getPrivateKeyPem(KeyPair keyPair) throws GeneralSecurityException, IOException {

        OpenSSHKeyPairResourceWriter resourceWriter = new OpenSSHKeyPairResourceWriter();
        ByteArrayOutputStream stringWriter = new ByteArrayOutputStream();
        resourceWriter.writePrivateKey(keyPair, null, null, stringWriter);
        return stringWriter.toString();
    }

    /**
     * @param publicKey a java public key object to be converted into an ssh public key entry
     * @return An encoded ssh pubic likey (IE: ssh-rsa AAAAE2VjZHNhLXNoYTItbmlzdHA1Mj....azBfQ==)
     */
    private String getPublicKeyToSsh(PublicKey publicKey) {
        return PublicKeyEntry.toString(publicKey);
    }

    /**
     * @param keyType a valid ssh key type. IE ssh-rsa, ecdsa-sha2-nistp384
     * @param keySize the size of the key in bytes. must be valid for the specified key type
     * @throws GeneralSecurityException thrown if there is an error when generating the keypair or encoding to the ssh format
     * @throws IOException              thrown if there is an error writing encoding the private key to PEM
     */
    public void generateSshKeyPair(String keyType, Integer keySize) throws GeneralSecurityException, IOException {
        checkAlgorithm(keyType, keySize);
        KeyPair keyPair = KeyUtils.generateKeyPair(keyType, keySize);
        this.privateKey = getPrivateKeyPem(keyPair);

        this.sshPublicKey = getPublicKeyToSsh(keyPair.getPublic());
    }

    public Boolean validKeyPair() {
        try {
            PublicKey publicKey = tryGeneratePublicKeyFromSshString();
            PrivateKey privateKey = tryGeneratePrivateKeyFromString();
            String signatureAlg = VERIFY_ALGS.get(privateKey.getAlgorithm());
            Signature signer = Signature.getInstance(signatureAlg);
            signer.initSign(privateKey);
            signer.update(SIGN_DATA.getBytes());
            byte[] signedData = signer.sign();
            Signature verifier = Signature.getInstance(signatureAlg);
            verifier.initVerify(publicKey);
            verifier.update(SIGN_DATA.getBytes());
            return verifier.verify(signedData);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @return a list of key algoritms that are available to use
     */
    public static ImmutableMap<String, Set<Integer>> validSshKeyAlgorithms() {
        return VALID_KEY_ALGS;
    }

    /**
     * @param keyType a valid ssh key type. IE ssh-rsa, ecdsa-sha2-nistp384
     * @param keySize the size of the key in bytes. must be valid for the specified key type
     * @return true if key type and key size are valid options
     */
    public static Boolean isValidAlgorithm(String keyType, Integer keySize) {
        try {
            checkAlgorithm(keyType, keySize);
        } catch (InvalidKeyException ignored) {
            return false;
        }
        return true;
    }

    /**
     * @param keyType a valid ssh key type. IE ssh-rsa, ecdsa-sha2-nistp384
     * @param keySize the size of the key in bytes. must be valid for the specified key type
     * @throws InvalidKeyException thrown if the key type or key size are not valid options
     */
    public static void checkAlgorithm(String keyType, Integer keySize) throws InvalidKeyException {
        Set<Integer> validKeySizes = VALID_KEY_ALGS.get(keyType);
        if (Objects.isNull(validKeySizes)) {
            throw new InvalidKeyException(String.format(
                    "%s is not an allowed key type. Allowed types: %s",
                    keyType,
                    String.join(", ", VALID_KEY_ALGS.keySet())
            ));
        }
        if (!validKeySizes.contains(keySize)) {
            throw new InvalidKeyException(String.format(
                    "%d is not an allowed key size for %s. Allowed key sizes: %s",
                    keySize,
                    keyType,
                    validKeySizes.stream().map(String::valueOf).collect(joining(", "))
            ));
        }
    }
}
