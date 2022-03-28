package core.kms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import core.utils.Enums.SupportedKmsDataEncryptionKey;


@Component
public class KMSProviderServiceFactory {
	private static final Logger			logger = LoggerFactory.getLogger(KMSProviderServiceFactory.class);
	@Autowired	
	@Qualifier("AwsKmsServiceImpl")
	private KMSService awsKmsServiceImpl;

	@Autowired
	@Qualifier("AzureKmsServiceImpl")
	private KMSService azureKmsServiceImpl;



	public KMSService getKmsServiceProvider(String kmsServiceProvider) {
		logger.info("kmsServiceProvider: "+kmsServiceProvider);
		if ("AwsKmsDataEncryptionKey".equals(kmsServiceProvider)) {
			return awsKmsServiceImpl;
		}
		else {
			return azureKmsServiceImpl;
		}
	}
}
