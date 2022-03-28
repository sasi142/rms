package core.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EncryptionUtil {
	private static final Logger logger = LoggerFactory.getLogger(EncryptionUtil.class);
	private static String encryptionKey;
	private static String cipherAlgorithm = "AES/GCM/NoPadding";
	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 16;

	public static void setEncryptionKey(String encKey) {
		encryptionKey = encKey;
	}
	
	public static String getEncryptionKey() {
		return encryptionKey;
	}

	public String getRandomPublicUrl() {
		String publicUrl = RandomStringUtils.random(8, true, true);
		return publicUrl;
	}

	public static String decrypt(String encryptedStr) throws GeneralSecurityException {
		byte[] encrypted = Base64.decodeBase64(encryptedStr);

		byte[] raw = encryptionKey.getBytes(Charset.forName("US-ASCII"));
		if (raw.length != 16) {
			throw new IllegalArgumentException("Invalid key size.");
		}
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");

		Cipher cipher = Cipher.getInstance(cipherAlgorithm);
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, new byte[GCM_IV_LENGTH]);
		cipher.init(Cipher.DECRYPT_MODE, skeySpec, gcmParameterSpec);
		byte[] original = cipher.doFinal(encrypted);
		return new String(original, Charset.forName("US-ASCII"));
	}

	public static String decryptPropertyValue(String originalValue) {
		if (originalValue.contains("{<ENC>}") && originalValue.contains("{</ENC>}")) {
			String encryptedTaggedValue = originalValue.substring(originalValue.indexOf("{<ENC>}"),
					originalValue.indexOf("{</ENC>}") + 8);
			String encryptedValue = originalValue.substring(originalValue.indexOf("{<ENC>}") + 7,
					originalValue.indexOf("{</ENC>}"));

			try {
				String decryptedValue = decrypt(encryptedValue);
				if (originalValue.contains("://")) {
					decryptedValue = URLEncoder.encode(decryptedValue, "UTF8");
				}
				originalValue = originalValue.replace(encryptedTaggedValue, decryptedValue);
			} catch (GeneralSecurityException e) {
				logger.error("failed to decrypt property returning original value as in properties file.", e);
			} catch (UnsupportedEncodingException e) {
				logger.error("failed to encode property returning original value as in properties file.", e);
			}
		}
		return originalValue;
	}

}