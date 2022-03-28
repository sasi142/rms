package core.utils;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class EncryptationAwarePropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

	@Override
	protected String convertPropertyValue(String originalValue) {
		String encKey = System.getenv("ENC_KEY");
		EncryptionUtil.setEncryptionKey(encKey);
		originalValue = EncryptionUtil.decryptPropertyValue(originalValue);
		return originalValue;
	}
}