package com.manocorbas.ciphermq.server.ca;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class BrokerCertificateSetup {
    /**
     * Gera um cert autoassinado pela AC para o broker.
     * Usado APENAS para desenvolvimento e testes 
     * 
     * será substituido pelo cert real assinado pelo professor.
     */
    public static X509Certificate selfSign(PublicKey brokerPub, PrivateKey acPriv) throws Exception {
        X500Name issuer = new X500Name("CN=RedesComputadoresII");
        X500Name subject = new X500Name("CN=CipherMQ Broker");

        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, brokerPub);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .build(acPriv); // assina com a chave privada da AC

        return new JcaX509CertificateConverter()
                .getCertificate(builder.build(signer));
    }
}
