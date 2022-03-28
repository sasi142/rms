package core.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import core.daos.CacheInstanceInfoDao;
import core.redis.RedisPubSubSubscriber;
import core.utils.Constants;
import core.utils.JsonUtil;
import utils.RmsApplicationContext;
import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

@Service
public class PubSubServiceImpl implements PubSubService, InitializingBean, DisposableBean {
	final static Logger logger = LoggerFactory.getLogger(PubSubServiceImpl.class);

	@Autowired
	private RedisPubSubSubscriber redisPubSubSubscriber;

	
	@Autowired
	private JsonUtil jsonUtil;
	private Integer numPubSubChannels;

	@Autowired
	private Environment env;

	private List<String> channels = new ArrayList<>();

	private ExecutorService service;
	
	@Autowired
	private CacheInstanceInfoDao cacheInstanceInfoDao;

	@Override
	public void subscribe() {		
		String channelPrefix = RmsApplicationContext.getInstance().getChannel(); 
		for (int indx = 1; indx <= numPubSubChannels; indx++) {
			String channel = channelPrefix+"_"+indx;
			logger.info("channel name: "+channel);
			channels.add(channel);	
		}		
		for (final String channel: channels) {
			service.execute(()->{
				logger.info("subscribe to channel: "+channel);
				redisPubSubSubscriber.subscribe(channel); 	
			});
		}
		ObjectNode json = Json.newObject();
		json.put("time", String.valueOf(System.currentTimeMillis()));
		json.put("instanceIp", RmsApplicationContext.getInstance().getIp());
		// Store channel current time in Redis. This is used to delete the channel info in case its closed	
		cacheInstanceInfoDao.saveInstanceInfo(RmsApplicationContext.getInstance().getInstanceId(), jsonUtil.write(json));
	}

	public List<String> getChannelList() {
		return channels;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		numPubSubChannels = Integer.valueOf(env.getProperty(Constants.PUBSUB_CHANNELS_MAX_COUNT));
		logger.info("numPubSubChannels: " + numPubSubChannels);		
		service = Executors.newFixedThreadPool(numPubSubChannels);
	}

	@Override
	public void destroy() throws Exception {
		service.shutdown();		
	}
}
