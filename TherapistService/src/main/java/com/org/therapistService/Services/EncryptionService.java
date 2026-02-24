package com.org.therapistService.Services;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {

	private static final String ALGORITHM = "AES/GCM/NoPadding";

	private static final int IV_LENGTH = 12;

	private static final int TAG_LENGTH = 128;

	private final SecretKey secretKey;

	public EncryptionService(@Value("${app.encryption.secret}")String base64Key) {

		byte[] decodedKey = Base64.getDecoder().decode(base64Key);

		this.secretKey = new SecretKeySpec(decodedKey, "AES");
	}

	public String encrypt(String plainText) {

		if (plainText == null)
			return null;

		try {

			byte[] iv = new byte[IV_LENGTH];

			SecureRandom random = new SecureRandom();

			random.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(ALGORITHM);

			GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);

			cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

			byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

			byte[] combined =
					ByteBuffer.allocate(iv.length + encrypted.length)
					.put(iv)
					.put(encrypted)
					.array();

			return Base64.getEncoder().encodeToString(combined);

		}

		catch (Exception e) {
			throw new RuntimeException("Encryption failed", e);
		}
	}

	public String decrypt(String encryptedText) {

		if (encryptedText == null)
			return null;

		try {

			byte[] decoded = Base64.getDecoder().decode(encryptedText);

			ByteBuffer buffer = ByteBuffer.wrap(decoded);

			byte[] iv = new byte[IV_LENGTH];

			buffer.get(iv);

			byte[] encrypted = new byte[buffer.remaining()];

			buffer.get(encrypted);

			Cipher cipher = Cipher.getInstance(ALGORITHM);

			GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);

			cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

			byte[] decrypted = cipher.doFinal(encrypted);

			return new String(decrypted, StandardCharsets.UTF_8);

		}

		catch (Exception e) {
			throw new RuntimeException("Decryption failed", e);
		}
	}
}
