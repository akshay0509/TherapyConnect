package com.org.therapistService.Utility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.org.therapistService.Services.EncryptionService;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Component
@Converter(autoApply = false)
public class SessionNotesEncryptor implements AttributeConverter<String, String>{

	private final EncryptionService encryptionService;

	@Autowired
	public SessionNotesEncryptor(EncryptionService encryptionService) {
		this.encryptionService = encryptionService;
	}

	@Override
	public String convertToDatabaseColumn(String attribute) {
		if (attribute == null)
			return null;

		return encryptionService.encrypt(attribute);
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		if (dbData == null)
			return null;

		return encryptionService.decrypt(dbData);
	}
}
