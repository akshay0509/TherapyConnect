package com.org.gatewayService.Config;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;


@Configuration
public class JwtKeyConfig {

	@Value("${jwt.private-key}")
    private Resource privateKey;

    @Value("${jwt.public-key}")
    private Resource publicKey;

    @Bean
    public RSAPrivateKey rsaPrivateKey() throws Exception {
        return loadPrivateKey(privateKey);
    }

    @Bean
    public RSAPublicKey rsaPublicKey() throws Exception {
        return loadPublicKey(publicKey);
    }

    private RSAPrivateKey loadPrivateKey(Resource resource) throws Exception {
        String key = new String(resource.getInputStream().readAllBytes())
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private RSAPublicKey loadPublicKey(Resource resource) throws Exception {
        String key = new String(resource.getInputStream().readAllBytes())
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
