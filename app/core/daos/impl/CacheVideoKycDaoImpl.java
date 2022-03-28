package core.daos.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.daos.CacheVideoKycDao;
import core.utils.CacheUtil;
import core.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import play.libs.Json;

@Repository
public class CacheVideoKycDaoImpl implements CacheVideoKycDao, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(CacheVideoKycDaoImpl.class);
	@Autowired
	public Environment env;

	public String PROP_VIDEO_KYC_BUCKET;

	@Autowired
	private CacheUtil cacheUtil;

	@Override
	public void afterPropertiesSet() throws Exception {
		PROP_VIDEO_KYC_BUCKET = env.getProperty(Constants.REDIS_IMS_VIDEOKYC_STORE);
		logger.info("videoKycBucket: " + PROP_VIDEO_KYC_BUCKET);
	}

	@Override
	public ObjectNode get(Integer kycId) {
		String kyc = cacheUtil.get(PROP_VIDEO_KYC_BUCKET, kycId);
		if (kyc == null || kyc.trim().length() == 0) return null;
		return (ObjectNode) Json.parse(kyc);
	}

	@Override
	public void put(Integer kycId, ObjectNode kyc) {
		cacheUtil.put(PROP_VIDEO_KYC_BUCKET, kycId, kyc);
	}
}
