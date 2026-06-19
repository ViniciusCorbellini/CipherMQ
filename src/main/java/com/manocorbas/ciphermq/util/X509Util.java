package com.manocorbas.ciphermq.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class X509Util {

    /**
     * Loads a certificate from a .crt file (PEM or DER)
     * 
     * @param path to the .crt file
     * 
     * @return the X509 certificate
     */
    public static X509Certificate loadCertificate(Path path) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        try (InputStream is = Files.newInputStream(path)) {
            return (X509Certificate) factory.generateCertificate(is);
        }
    }

    /**
     * Verifies if a certificate has been signed by a CA
     * 
     * @param certificate   certificate to be validated
     * @param caCertificate CA's certificate
     * 
     * @throws Exception if the certificate is invalid
     */
    public static void verifyCertificate(X509Certificate certificate, X509Certificate caCertificate) throws Exception {
        certificate.verify(caCertificate.getPublicKey());
        certificate.checkValidity();
    }

    /**
     * Serializes a X.509 certificate to Base64
     * 
     * @param certificate certificate to be serialized
     * @return Base64 encoded certificate
     * @throws Exception
     */
    public static String serialize(X509Certificate certificate) throws Exception {
        return Base64.getEncoder().encodeToString(certificate.getEncoded());
    }

    /**
     * Deserializes a Base64 encoded ceecrtificate back to X.509
     * 
     * @param encoded encoded certificate
     * @return X.509 certificate
     * @throws Exception
     */
    public static X509Certificate deserialize(String encoded) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes));
    }

}
