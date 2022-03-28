package core.kms.impl;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.services.CacheService;
import org.springframework.core.env.Environment;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import org.springframework.core.env.Environment;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.EncryptRequest;
import core.exceptions.InternalServerErrorException;

import com.amazonaws.util.Base64;
import core.kms.KMSService;
import core.utils.Constants;
import core.utils.Enums.ErrorCode;
import redis.clients.jedis.Jedis;
import play.libs.Json;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.utils.Enums.SupportedKmsDataEncryptionKey;


@Service("AwsKmsServiceImpl")
public class AwsKmsServiceImpl implements KMSService{

	final static Logger logger = LoggerFactory.getLogger(AwsKmsServiceImpl.class);

	@Autowired
	private CacheService cacheService;

	@Autowired
	private Environment env;
	private AWSKMS awsKmsClient; //do
	private String awsKmskeyId;


	@Override
	public String encrypt(String data, Integer orgId) {
		// if per client, same
		getAwsKeyConnection(orgId);	
		try {
			ByteBuffer plaintext = ByteBuffer.wrap(data.getBytes());		
			EncryptRequest req = new EncryptRequest().withKeyId(awsKmskeyId).withPlaintext(plaintext);
			ByteBuffer ciphertext = awsKmsClient.encrypt(req).getCiphertextBlob();	
			byte[] base64EncodedValue = Base64.encode(ciphertext.array());		
			data = new String(base64EncodedValue, Charset.forName("UTF-8"));
			logger.info("encryption done");
			//return value;
		} catch (Exception e) {
			logger.error("Aws connection failed", e);
			throw new InternalServerErrorException(ErrorCode.Failed_To_Encrypt_Value, "Failed to encrypt value");
		}
		return data;
	}

	@Override
	public String decrypt(String data, Integer orgId) {
		logger.info("Decrypt value Satrted:");
		getAwsKeyConnection(orgId);
		try {
			byte cipherBytes[] = Base64.decode(data);
			ByteBuffer cipherBuffer = ByteBuffer.wrap(cipherBytes);
			DecryptRequest req1 = new DecryptRequest().withKeyId(awsKmskeyId).withCiphertextBlob(cipherBuffer);
			DecryptResult resp = awsKmsClient.decrypt(req1);
			data =  new String(resp.getPlaintext().array(), Charset.forName("UTF-8"));
			logger.info("Decrypt value Done: ");
			//data = decryptedValue;
		} catch (Exception e) {
			logger.error("Aws connection failed", e);
			throw new InternalServerErrorException(ErrorCode.Failed_To_Decrypt_Value, "Failed to decrypt value");
		}
		return data;

	}



	private void getAwsKeyConnection(Integer orgId) {
		logger.debug("get Aws connection");
		String awsKeySettings = null;	
		JsonNode arrayNode = cacheService.getAppSettingsJson(orgId);
		for(JsonNode settings : arrayNode ) {
			String settingName = settings.findPath("settingName").asText();
			if(settingName.equalsIgnoreCase(SupportedKmsDataEncryptionKey.AwsKmsDataEncryptionKey.getName())) {
				awsKeySettings = settings.findPath("value").asText();
			}
		}
		try {
			if (awsKeySettings != null) {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode awsKeySettingsObj = mapper.readTree(awsKeySettings);
				String apiKey =awsKeySettingsObj.get("apiKey").asText();
				logger.info("apiKey recived ");
				//String apiKey = awsKeySettings.getString("apiKey");
				String apiSecrete = awsKeySettingsObj.get("apiSecret").asText();
				String userIamRolestr = awsKeySettingsObj.get("userIamRole").asText();
				Boolean userIamRole = false;
				if(userIamRolestr!= null) {
					userIamRole = Boolean.valueOf(userIamRolestr);
				}				 
				logger.info("apiSecrete recived");
				AWSCredentialsProvider credentialsProvider = null;
				if (userIamRole) {
					logger.info("user instance role");
					credentialsProvider = InstanceProfileCredentialsProvider.getInstance();
				} else {
					AWSCredentials credentials = new BasicAWSCredentials(apiKey, apiSecrete);
					credentialsProvider = new AWSStaticCredentialsProvider(credentials);
				}			

				awsKmsClient = AWSKMSClientBuilder.standard()
						.withCredentials(credentialsProvider)
						.withRegion(Regions.AP_SOUTH_1)
						.build();
				awsKmskeyId =awsKeySettingsObj.get("awsKmsKeyId").asText();
				logger.info("awsKmskeyId recived.");
			}
		} catch (Exception e) {
			logger.error("Failed to format json", e);
			throw new InternalServerErrorException(ErrorCode.Failed_To_Parse_Json_Obj, "Failed to parse json");
		}
		logger.debug("return aws connection");
	}



}
