package com.backend.streetmed_backend.util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Generates self-signed certificates for TLS using Bouncy Castle
 * Runs automatically in local/dev profiles if keystore doesn't exist
 */
@Component
@Profile({"local", "dev", "local-tls"})
public class KeystoreGenerator implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(KeystoreGenerator.class);

    @Value("${tls.keystore.path:src/main/resources/keystore/streetmed-keystore.p12}")
    private String keystorePath;

    @Value("${tls.keystore.password:streetmed123}")
    private String keystorePassword;

    @Value("${tls.key.alias:streetmed}")
    private String keyAlias;

    @Value("${tls.key.size:2048}")
    private int keySize;

    @Value("${tls.validity.days:365}")
    private int validityDays;

    @Value("${tls.common.name:localhost}")
    private String commonName;

    @Value("${tls.organization:StreetMed@Pitt}")
    private String organization;

    @Value("${tls.organization.unit:Development}")
    private String organizationUnit;

    @Value("${tls.locality:Pittsburgh}")
    private String locality;

    @Value("${tls.state:PA}")
    private String state;

    @Value("${tls.country:US}")
    private String country;

    static {
        // Add Bouncy Castle as security provider
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public void run(String... args) throws Exception {
        Path keystore = Paths.get(keystorePath);

        // Check if keystore already exists
        if (Files.exists(keystore)) {
            logger.info("=================================================");
            logger.info("TLS Keystore already exists at: {}", keystorePath);
            logger.info("Password: {}", keystorePassword);
            logger.info("Alias: {}", keyAlias);
            logger.info("To regenerate, delete the existing keystore file");
            logger.info("=================================================");
            return;
        }

        logger.info("=================================================");
        logger.info("Generating self-signed certificate for TLS...");
        logger.info("=================================================");

        generateSelfSignedCertificate();
    }

    /**
     * Generate a self-signed certificate and store it in a PKCS12 keystore
     */
    public void generateSelfSignedCertificate() throws Exception {
        // Create keystore directory if it doesn't exist
        Path keystoreDir = Paths.get(keystorePath).getParent();
        if (keystoreDir != null && !Files.exists(keystoreDir)) {
            Files.createDirectories(keystoreDir);
            logger.info("Created keystore directory: {}", keystoreDir);
        }

        // Generate RSA key pair
        logger.info("Generating RSA key pair ({} bits)...", keySize);
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGen.initialize(keySize, new SecureRandom());
        KeyPair keyPair = keyPairGen.generateKeyPair();

        // Build certificate
        X509Certificate certificate = generateCertificate(keyPair);

        // Create PKCS12 keystore
        logger.info("Creating PKCS12 keystore...");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);

        // Add private key and certificate to keystore
        X509Certificate[] chain = new X509Certificate[] { certificate };
        keyStore.setKeyEntry(keyAlias, keyPair.getPrivate(),
                keystorePassword.toCharArray(), chain);

        // Save keystore to file
        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            keyStore.store(fos, keystorePassword.toCharArray());
        }

        logger.info("=================================================");
        logger.info("✓ Self-signed certificate generated successfully!");
        logger.info("=================================================");
        logger.info("Keystore Details:");
        logger.info("  Location: {}", keystorePath);
        logger.info("  Password: {}", keystorePassword);
        logger.info("  Alias: {}", keyAlias);
        logger.info("  Type: PKCS12");
        logger.info("Certificate Details:");
        logger.info("  CN: {}", commonName);
        logger.info("  Validity: {} days", validityDays);
        logger.info("  Key Algorithm: RSA");
        logger.info("  Key Size: {} bits", keySize);
        logger.info("  Signature Algorithm: SHA256withRSA");
        logger.info("=================================================");
        logger.info("Add to application.properties:");
        logger.info("  server.ssl.enabled=true");
        logger.info("  server.ssl.key-store=classpath:keystore/streetmed-keystore.p12");
        logger.info("  server.ssl.key-store-type=PKCS12");
        logger.info("  server.ssl.key-store-password={}", keystorePassword);
        logger.info("  server.ssl.key-alias={}", keyAlias);
        logger.info("=================================================");
    }

    /**
     * Generate X.509 certificate using Bouncy Castle
     */
    private X509Certificate generateCertificate(KeyPair keyPair) throws Exception {
        logger.info("Generating X.509 certificate...");

        // Certificate validity period
        Date notBefore = new Date(System.currentTimeMillis());
        Date notAfter = new Date(System.currentTimeMillis() + (validityDays * 86400000L));

        // Build X.500 Name (Distinguished Name)
        String distinguishedName = String.format(
                "CN=%s, OU=%s, O=%s, L=%s, ST=%s, C=%s",
                commonName, organizationUnit, organization, locality, state, country
        );
        X500Name issuer = new X500Name(distinguishedName);

        // Serial number
        BigInteger serialNumber = new BigInteger(64, new SecureRandom());

        // Create certificate builder
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serialNumber,
                notBefore,
                notAfter,
                issuer,
                keyPair.getPublic()
        );

        // Add extensions
        addCertificateExtensions(certBuilder);

        // Sign the certificate
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);

        // Convert to Java X509Certificate
        X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);

        logger.info("Certificate generated for: {}", distinguishedName);
        logger.info("Serial Number: {}", serialNumber.toString(16));
        logger.info("Valid from: {} to: {}", notBefore, notAfter);

        return certificate;
    }

    /**
     * Add certificate extensions for localhost and common names
     */
    private void addCertificateExtensions(X509v3CertificateBuilder certBuilder) throws Exception {
        // Key Usage
        KeyUsage keyUsage = new KeyUsage(
                KeyUsage.digitalSignature |
                        KeyUsage.keyEncipherment |
                        KeyUsage.dataEncipherment
        );
        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                keyUsage
        );

        // Extended Key Usage
        KeyPurposeId[] usages = new KeyPurposeId[] {
                KeyPurposeId.id_kp_serverAuth,
                KeyPurposeId.id_kp_clientAuth
        };
        certBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(usages)
        );

        // Subject Alternative Names (SAN)
        GeneralName[] subjectAltNames = new GeneralName[] {
                new GeneralName(GeneralName.dNSName, "localhost"),
                new GeneralName(GeneralName.dNSName, "127.0.0.1"),
                new GeneralName(GeneralName.dNSName, commonName),
                new GeneralName(GeneralName.iPAddress, "127.0.0.1"),
                new GeneralName(GeneralName.iPAddress, "::1") // IPv6 localhost
        };

        GeneralNames sans = new GeneralNames(subjectAltNames);
        certBuilder.addExtension(
                Extension.subjectAlternativeName,
                false,
                sans
        );

        // Basic Constraints (CA:FALSE)
        certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(false)
        );

        logger.info("Added certificate extensions: KeyUsage, ExtendedKeyUsage, SubjectAltNames");
    }
}