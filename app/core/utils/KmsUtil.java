package core.utils;

import java.util.ArrayList;
import java.util.List;

import core.kms.KMSProviderServiceFactory;
import core.kms.KMSService;
import core.services.CacheService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.utils.Enums.SupportedKmsDataEncryptionKey;



@Component
public class KmsUtil {

	private static final Logger	logger	= LoggerFactory.getLogger(KmsUtil.class);

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	@Autowired
	private KMSProviderServiceFactory kMSProviderServiceFactory;


	public String decryptName(String name, Integer orgId) {
		String kmsProviderName = getKmsProviderName(orgId);		
		if(kmsProviderName != null) {
			KMSService kmsProviderService = kMSProviderServiceFactory.getKmsServiceProvider(kmsProviderName.toString());
			logger.info("kmsProviderService name is : "+kmsProviderService);		
			name = decryptKycValue(name, kmsProviderService, orgId);
		}
		return name;
	}
	public Boolean checkEncryptionRequired(Integer orgId){
		JsonNode orgJson = cacheService.getOrgJson(orgId);
		JsonNode prefs = orgJson.findPath("settings");		
		if (prefs != null && prefs.isArray()) {
			for (JsonNode pref : prefs) {
				if ("EnableVideoKycCustomerDataEncrption".equalsIgnoreCase(pref.findPath("preference").asText())) {
					return Boolean.valueOf(pref.findPath("value").asText());					
				}
			}
		}			
		return false;
	}


	private String decrypt(String value,  KMSService authProviderService, Integer orgId) {
		logger.info("decrypt Video kyc data value started ");		
		String encValue = authProviderService.decrypt(value, orgId);
		logger.info("decrypt Video kyc data value done ");
		return encValue;
	}



	private String decryptKycValue(String value,  KMSService authProviderService, Integer orgId) {
		if(value.contains("< enc>")) {
			Integer targetPosition = value.lastIndexOf(" ");
			logger.info("Value is encrypted");
			String kycString = value.substring(0, targetPosition-1);
			logger.info("decryption of Value is started.");
			value = decrypt(kycString, authProviderService, orgId);
		}
		return value;
	}

	private String getKmsProviderName(Integer orgId) {
		String kmsProviderName  = null;
		logger.info("decryption required check started for organization: "+orgId);
		ArrayNode node =cacheService.getAppSettingsJson(orgId);
		if (node.isArray() && node.size() > 0) {
			for (int indx = 0; indx < node.size(); indx++) {
				JsonNode json = node.get(indx);				
				if (SupportedKmsDataEncryptionKey.AwsKmsDataEncryptionKey.getName().equalsIgnoreCase(json.get("settingName").asText())) {
					logger.info("aws encryption setting");
					kmsProviderName = SupportedKmsDataEncryptionKey.AwsKmsDataEncryptionKey.getName();
				} else if (SupportedKmsDataEncryptionKey.AzureKmsDataEncryptionKey.getName().equalsIgnoreCase(json.get("settingName").asText())) {
					logger.info("azure encryption setting");
					kmsProviderName = SupportedKmsDataEncryptionKey.AzureKmsDataEncryptionKey.getName();
				} 
			}
		}
		return kmsProviderName;		
	}

}
