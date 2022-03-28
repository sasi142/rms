package core.services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import core.daos.ElasticSearchDao;
import core.entities.ApiRequest;
import core.utils.Constants;


@Service
public class CommonServiceImpl implements CommonService, InitializingBean  {	
	private static final Logger	logger	= LoggerFactory.getLogger(CommonServiceImpl.class);
	
	@Autowired
	private Environment			env;
	
	private String apiRestIndexName;
	
	@Autowired
	private ElasticSearchDao elasticSearchDao;

	public CommonServiceImpl() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	//@Async	
	public void createApiRequest(ApiRequest apiRequest) {
		try {
			apiRequest.setComponentName("rms");
			ObjectMapper mapper = new ObjectMapper();	
			String json = mapper.writeValueAsString(apiRequest);
			elasticSearchDao.add(apiRestIndexName, json);
		} catch (Exception ex) {
			logger.error("failed to save to API Request. ", ex);
		}		
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		apiRestIndexName = env.getProperty(Constants.ES_API_REQUEST_INDEX_NAME);
		logger.info("apiRestIndexName: "+apiRestIndexName);
	}
}
