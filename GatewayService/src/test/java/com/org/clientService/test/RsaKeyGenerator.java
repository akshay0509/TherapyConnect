package com.org.clientService.test;

import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class RsaKeyGenerator {

	public static void main(String[] args) throws Exception {

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        // Private key (PKCS8)
        try (FileOutputStream fos = new FileOutputStream("gateway-private.pem")) {
            fos.write("-----BEGIN PRIVATE KEY-----\n".getBytes());
            fos.write(Base64.getMimeEncoder(64, "\n".getBytes())
                    .encode(keyPair.getPrivate().getEncoded()));
            fos.write("\n-----END PRIVATE KEY-----\n".getBytes());
        }

        // Public key (X509)
        try (FileOutputStream fos = new FileOutputStream("gateway-public.pem")) {
            fos.write("-----BEGIN PUBLIC KEY-----\n".getBytes());
            fos.write(Base64.getMimeEncoder(64, "\n".getBytes())
                    .encode(keyPair.getPublic().getEncoded()));
            fos.write("\n-----END PUBLIC KEY-----\n".getBytes());
        }

        System.out.println("RSA key pair generated.");
    }
}
