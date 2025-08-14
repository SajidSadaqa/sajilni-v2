package com.sajilni.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Service
public class CertificateService {

    private final ResourceLoader resourceLoader;
    @Getter
    private PrivateKey privateKey;
    @Getter
    private PublicKey publicKey;

    @Value("${app.jwt.private-key-path:classpath:certs/private_key.pem}")
    private String privateKeyPath;

    @Value("${app.jwt.public-key-path:classpath:certs/public_key.pem}")
    private String publicKeyPath;

    @Value("${app.jwt.generate-keys-if-missing:true}")
    private boolean generateKeysIfMissing;

    public CertificateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initializeKeys() {
        try {
            if (keysExist()) {
                loadExistingKeys();
            } else if (generateKeysIfMissing) {
                generateNewKeys();
            } else {
                throw new IllegalStateException("JWT keys not found and generation is disabled");
            }

            log.info("JWT keys initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize JWT keys", e);
            throw new IllegalStateException("JWT key initialization failed", e);
        }
    }

    private boolean keysExist() {
        try {
            Resource privateKeyResource = resourceLoader.getResource(privateKeyPath);
            Resource publicKeyResource = resourceLoader.getResource(publicKeyPath);
            return privateKeyResource.exists() && publicKeyResource.exists();
        } catch (Exception e) {
            return false;
        }
    }

    private void loadExistingKeys() throws Exception {
        log.info("Loading existing JWT keys from {} and {}", privateKeyPath, publicKeyPath);

        // Load private key
        Resource privateKeyResource = resourceLoader.getResource(privateKeyPath);
        String privateKeyContent = StreamUtils.copyToString(
                privateKeyResource.getInputStream(), StandardCharsets.UTF_8);
        this.privateKey = parsePrivateKey(privateKeyContent);

        // Load public key
        Resource publicKeyResource = resourceLoader.getResource(publicKeyPath);
        String publicKeyContent = StreamUtils.copyToString(
                publicKeyResource.getInputStream(), StandardCharsets.UTF_8);
        this.publicKey = parsePublicKey(publicKeyContent);

        log.info("Successfully loaded existing JWT keys");
    }

    private void generateNewKeys() throws Exception {
        log.info("Generating new RSA key pair for JWT");

        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(2048);
        KeyPair keyPair = keyGenerator.generateKeyPair();

        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();

        // Log the keys in PEM format for saving (in production, save to secure location)
        log.info("Generated new keys. Save the following to your certificate files:");
        log.info("Private key PEM:\n{}", formatPrivateKeyToPem(privateKey));
        log.info("Public key PEM:\n{}", formatPublicKeyToPem(publicKey));

        log.warn("SECURITY WARNING: In production, save these keys securely and disable key generation");
    }

    private PrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        String privateKeyContent = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private PublicKey parsePublicKey(String publicKeyPem) throws Exception {
        String publicKeyContent = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(publicKeyContent);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private String formatPrivateKeyToPem(PrivateKey privateKey) {
        String encoded = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" +
                formatBase64String(encoded) +
                "\n-----END PRIVATE KEY-----";
    }

    private String formatPublicKeyToPem(PublicKey publicKey) {
        String encoded = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" +
                formatBase64String(encoded) +
                "\n-----END PUBLIC KEY-----";
    }

    private String formatBase64String(String base64) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < base64.length(); i += 64) {
            formatted.append(base64, i, Math.min(i + 64, base64.length()));
            if (i + 64 < base64.length()) {
                formatted.append("\n");
            }
        }
        return formatted.toString();
    }

}
