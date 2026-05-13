package com.manocorbas.ciphermq.protocols.certificate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.PublicKey;
import java.util.Base64;

import com.manocorbas.ciphermq.client.ClientCredentials;
import com.manocorbas.ciphermq.common.Message;

/**
 * Representes the client's certificate
 * - who is he? (clientId)
 * - his public key
 * - CA's signature over clientId + publicKey
 * 
 * Implements Serializable so it can easily navigate as the content field in an {@code com.manocorbas.ciphermq.common.Message} object (Base64 JSON)
 * 
 * @see Message 
 * @see ClientCredentials
 */
public record ClientCertificate(
        String clientId,
        PublicKey clientPublicKey,
        byte[] signature
) implements Serializable {

    /**
     * Serializes to String (Base64) to fit in the Message's content field.
     */
    public String serialize() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
        }
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }

    /**
     * Deserializes back from the Message's content field.
     */
    public static ClientCertificate deserialize(String encoded) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (ClientCertificate) ois.readObject();
        }
    }
}
