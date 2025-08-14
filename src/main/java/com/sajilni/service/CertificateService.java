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

@Service
public class CertificateService {

    private static final Logger logger = Logger.getLogger(CertificateService.class.getName());

    // These values will be like "certs/private.key"
    @Value("${app.security.private-key-path}")
    private String privateKeyPath;

    @Value("${app.security.public-key-path}")
    private String publicKeyPath;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void initializeKeys() {
        try {
            Path privatePath = Paths.get(privateKeyPath);
            Path publicPath = Paths.get(publicKeyPath);

            if (Files.exists(privatePath) && Files.exists(publicPath)) {
                logger.info("Found existing key pair. Loading from file system.");
                loadExistingKeys(privatePath, publicPath);
            } else {
                logger.info("Key pair not found. Generating new key pair.");
                generateNewKeyPair(privatePath, publicPath);
            }
            logger.info("JWT keys initialized successfully.");
        } catch (Exception e) {
            logger.severe("FATAL: JWT key initialization failed: " + e.getMessage());
            // Wrapping in a runtime exception will cause the application context to fail to start,
            // which is the correct behavior if security keys can't be loaded.
            throw new IllegalStateException("JWT key initialization failed", e);
        }
    }

    private void loadExistingKeys(Path privatePath, Path publicPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] privateKeyBytes = Files.readAllBytes(privatePath);
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);

        byte[] publicKeyBytes = Files.readAllBytes(publicPath);
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

        // Save the raw key bytes directly. This is more robust than saving PEM strings.
        Files.write(privatePath, privateKey.getEncoded());
        Files.write(publicPath, publicKey.getEncoded());

        logger.info("New key pair generated and saved to:");
        logger.info("Private key: " + privatePath.toAbsolutePath());
        logger.info("Public key: " + publicPath.toAbsolutePath());
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
