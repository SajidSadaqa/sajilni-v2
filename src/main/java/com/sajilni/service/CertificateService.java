package com.sajilni.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.logging.Logger;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;

@Service
public class CertificateService {

    private static final Logger logger = Logger.getLogger(CertificateService.class.getName());

    @Value("${app.security.private-key-path}")
    private String privateKeyPath;

    @Value("${app.security.public-key-path}")
    private String publicKeyPath;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void initializeKeys() {
        try {
            Path privatePath = resolvePath(privateKeyPath);
            Path publicPath = resolvePath(publicKeyPath);

            if (Files.exists(privatePath) && Files.exists(publicPath)) {
                logger.info("Found existing key pair. Loading from file system.");
                try {
                    loadExistingKeys(privatePath, publicPath);
                } catch (Exception e) {
                    logger.warning("Failed to load existing keys: " + e.getMessage() + ". Regenerating...");
                    // Delete corrupted files and generate new ones
                    deleteExistingKeys(privatePath, publicPath);
                    generateNewKeyPair(privatePath, publicPath);
                }
            } else {
                logger.info("Key pair not found. Generating new key pair.");
                generateNewKeyPair(privatePath, publicPath);
            }
            logger.info("JWT keys initialized successfully.");
        } catch (Exception e) {
            logger.severe("FATAL: JWT key initialization failed: " + e.getMessage());
            throw new IllegalStateException("JWT key initialization failed", e);
        }
    }

    private void loadExistingKeys(Path privatePath, Path publicPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Load private key
        byte[] privateKeyBytes = Files.readAllBytes(privatePath);
        privateKeyBytes = decodePemIfNeeded(privateKeyBytes, "PRIVATE KEY");
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);

        // Load public key
        byte[] publicKeyBytes = Files.readAllBytes(publicPath);
        publicKeyBytes = decodePemIfNeeded(publicKeyBytes, "PUBLIC KEY");
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.privateKey = keyFactory.generatePrivate(privateSpec);
        this.publicKey = keyFactory.generatePublic(publicSpec);
    }

    private void generateNewKeyPair(Path privatePath, Path publicPath) throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();

        // Ensure parent directories exist
        Files.createDirectories(privatePath.getParent());
        Files.createDirectories(publicPath.getParent());

        // Save keys in PEM format for better compatibility
        String privateKeyPem = toPem(privateKey.getEncoded(), "PRIVATE KEY");
        String publicKeyPem = toPem(publicKey.getEncoded(), "PUBLIC KEY");

        Files.write(privatePath, privateKeyPem.getBytes(StandardCharsets.UTF_8));
        Files.write(publicPath, publicKeyPem.getBytes(StandardCharsets.UTF_8));

        logger.info("New key pair generated and saved to:");
        logger.info("Private key: " + privatePath.toAbsolutePath());
        logger.info("Public key: " + publicPath.toAbsolutePath());
    }

    private void deleteExistingKeys(Path privatePath, Path publicPath) {
        try {
            if (Files.exists(privatePath)) {
                Files.delete(privatePath);
                logger.info("Deleted corrupted private key file");
            }
            if (Files.exists(publicPath)) {
                Files.delete(publicPath);
                logger.info("Deleted corrupted public key file");
            }
        } catch (IOException e) {
            logger.warning("Failed to delete existing key files: " + e.getMessage());
        }
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    private Path resolvePath(String pathStr) throws IOException {
        final String prefix = "classpath:";
        if (pathStr != null && pathStr.startsWith(prefix)) {
            String cp = pathStr.substring(prefix.length()).replaceFirst("^/*", "");
            ClassPathResource resource = new ClassPathResource(cp);
            if (resource.exists()) {
                return resource.getFile().toPath();
            }
            return Paths.get(cp);
        }
        return Paths.get(pathStr);
    }

    /**
     * Decode a PEM-formatted key if necessary. If the byte array starts with a
     * "-----BEGIN" line, it is treated as PEM and the base64 content between
     * the BEGIN/END markers is extracted and decoded. Otherwise, the original
     * byte array is returned unchanged.
     *
     * @param keyBytes the raw bytes read from a key file
     * @param type     the PEM type (e.g. "PRIVATE KEY" or "PUBLIC KEY")
     * @return DER-encoded key bytes suitable for KeySpec constructors
     */
    private byte[] decodePemIfNeeded(byte[] keyBytes, String type) {
        String content = new String(keyBytes, StandardCharsets.UTF_8).trim();
        if (content.startsWith("-----BEGIN")) {
            // Extract the base64 content between BEGIN and END markers
            String beginMarker = "-----BEGIN " + type + "-----";
            String endMarker = "-----END " + type + "-----";

            int beginIndex = content.indexOf(beginMarker);
            int endIndex = content.indexOf(endMarker);

            if (beginIndex != -1 && endIndex != -1) {
                String base64Content = content.substring(beginIndex + beginMarker.length(), endIndex)
                        .replaceAll("\\s+", ""); // Remove all whitespace
                return Base64.getDecoder().decode(base64Content);
            }
        }
        return keyBytes; // Return as-is if not PEM format
    }

    /**
     * Convert DER-encoded key bytes to PEM format
     */
    private String toPem(byte[] keyBytes, String type) {
        String base64 = Base64.getEncoder().encodeToString(keyBytes);
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN ").append(type).append("-----\n");

        // Split base64 into 64-character lines
        for (int i = 0; i < base64.length(); i += 64) {
            int end = Math.min(i + 64, base64.length());
            pem.append(base64, i, end).append("\n");
        }

        pem.append("-----END ").append(type).append("-----\n");
        return pem.toString();
    }
}