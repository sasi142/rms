package core.kms.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import core.kms.KMSService;

@Service("AzureKmsServiceImpl")
public class AzureKmsServiceImpl implements KMSService  {
	private static final Logger			logger	= LoggerFactory.getLogger(AzureKmsServiceImpl.class);

	

	@Override
	public String encrypt(String data, Integer orgId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String decrypt(String data, Integer orgId) {
		// TODO Auto-generated method stub
		return null;
	} 
}
