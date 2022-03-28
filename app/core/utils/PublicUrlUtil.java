package core.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PublicUrlUtil {
	private static final Logger logger = LoggerFactory.getLogger(PublicUrlUtil.class);	

	public static String encrypt(String value) throws GeneralSecurityException {
		String encryptionKey = EncryptionUtil.getEncryptionKey();
		byte[] raw = encryptionKey.getBytes(Charset.forName("US-ASCII"));
		if (raw.length != 16) {
			throw new IllegalArgumentException("Invalid key size.");
		}

		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16]));
		byte[] encryptedBytes = cipher.doFinal(value.getBytes(Charset.forName("US-ASCII")));
		String encryptedStr = Base64.encodeBase64URLSafeString(encryptedBytes);
		return encryptedStr;
	}

	public static String decrypt(String encryptedStr) throws GeneralSecurityException {
		String encryptionKey = EncryptionUtil.getEncryptionKey();
		byte[] encrypted = Base64.decodeBase64(encryptedStr);

		byte[] raw = encryptionKey.getBytes(Charset.forName("US-ASCII"));
		if (raw.length != 16) {
			throw new IllegalArgumentException("Invalid key size.");
		}
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16]));
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
				String decryptedValue = PublicUrlUtil.decrypt(encryptedValue);
				if (originalValue.contains("://")) {// if the encrypted value used in URL
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
