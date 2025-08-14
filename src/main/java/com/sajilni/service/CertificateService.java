package com.sajilni.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.logging.Logger;

@Service
public class CertificateService {

    private static final Logger logger = Logger.getLogger(CertificateService.class.getName());

    // --- CHANGE 1: Inject ResourceLoader ---
    // This is the key to handling classpath resources correctly.
    private final ResourceLoader resourceLoader;

    @Value("${app.security.private-key-path}")
    private String privateKeyPath; // e.g., "classpath:certs/private.key" or "file:./certs/private.key"

    @Value("${app.security.public-key-path}")
    private String publicKeyPath; // e.g., "classpath:certs/public.key" or "file:./certs/public.key"

    private PrivateKey privateKey;
    private PublicKey publicKey;

    // --- CHANGE 2: Use constructor injection for ResourceLoader ---
    public CertificateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initializeKeys() {
        try {
            // --- CHANGE 3: Use Resource objects to check for existence ---
            Resource privateKeyResource = resourceLoader.getResource(privateKeyPath);
            Resource publicKeyResource = resourceLoader.getResource(publicKeyPath);

            if (!privateKeyResource.exists() || !publicKeyResource.exists()) {
                logger.info("Key files not found, generating new key pair...");
                generateNewKeyPair();
                return;
            }

            loadExistingKeys();
            logger.info("JWT keys loaded successfully");
        } catch (Exception e) {
            logger.warning("Failed to load existing keys: " + e.getMessage());
            try {
                logger.info("Generating new key pair...");
                generateNewKeyPair();
                logger.info("JWT keys generated successfully");
            } catch (Exception ex) {
                logger.severe("JWT key initialization failed: " + ex.getMessage());
                throw new IllegalStateException("JWT key initialization failed", ex);
            }
        }
    }

    private void loadExistingKeys() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // --- CHANGE 4: Read from Resource's InputStream ---
        Resource privateKeyResource = resourceLoader.getResource(privateKeyPath);
        String privateKeyContent;
        try (InputStream inputStream = privateKeyResource.getInputStream()) {
            privateKeyContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        this.privateKey = parsePrivateKey(privateKeyContent);

        Resource publicKeyResource = resourceLoader.getResource(publicKeyPath);
        String publicKeyContent;
        try (InputStream inputStream = publicKeyResource.getInputStream()) {
            publicKeyContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        this.publicKey = parsePublicKey(publicKeyContent);
    }

    private void generateNewKeyPair() throws NoSuchAlgorithmException, IOException {
        logger.info("Generating new RSA key pair...");

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();

        // --- CHANGE 5: Save keys using Resource's OutputStream ---
        // This makes saving compatible with both file system and classpath locations (if writable).
        saveKeyToFile(privateKey.getEncoded(), privateKeyPath, "PRIVATE KEY");
        saveKeyToFile(publicKey.getEncoded(), publicKeyPath, "PUBLIC KEY");

        logger.info("New key pair generated and saved to:");
        logger.info("Private key: " + resourceLoader.getResource(privateKeyPath).getURI());
        logger.info("Public key: " + resourceLoader.getResource(publicKeyPath).getURI());
    }

    private void saveKeyToFile(byte[] keyBytes, String resourcePath, String keyType) throws IOException {
        Resource resource = resourceLoader.getResource(resourcePath);

        // Ensure the resource is writable. Classpath resources inside a JAR are not.
        if (!(resource instanceof WritableResource)) {
            throw new IOException("Cannot write to resource: " + resourcePath + ". It is not a writable resource (e.g., a file system location).");
        }

        WritableResource writableResource = (WritableResource) resource;

        String keyContent = "-----BEGIN " + keyType + "-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyBytes) + "\n" +
                "-----END " + keyType + "-----";

        try (OutputStream outputStream = writableResource.getOutputStream()) {
            outputStream.write(keyContent.getBytes(StandardCharsets.UTF_8));
        }
    }

    // No changes needed for the methods below this line
    private PrivateKey parsePrivateKey(String keyContent) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String cleanedKey = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(cleanedKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    private PublicKey parsePublicKey(String keyContent) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String cleanedKey = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(cleanedKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
