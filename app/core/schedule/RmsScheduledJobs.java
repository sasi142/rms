package core.schedule;


import com.fasterxml.jackson.databind.node.ObjectNode;
import core.daos.ElasticSearchDao;
import core.entities.VideokycAgentQueue;
import core.redis.RedisConnection;
import core.services.VideokycService;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums.VideoKYCStatus;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import play.libs.Json;

import java.net.InetAddress;
import java.time.Instant;
import java.util.*;

@Component
@EnableScheduling
public class RmsScheduledJobs implements InitializingBean {
	private static final Logger	logger	= LoggerFactory.getLogger(RmsScheduledJobs.class);

	@Autowired
	private RedisConnection redisConnection;

	@Autowired
	private Environment env;

	private String environment;

	@Autowired
	private VideokycService videokycService;

	private Long videoKycCallWaitLockAtLeastTime = 3000L;
	private Long videoKycCallWaitLockAtMostTime = 30000L;

	private Short beathingTime = 0;

	private Integer averageCallDuration = 120;

	private LockProvider lockProvider;
	
	private String jobName;
	
    private Set<String> queueStatues = Collections.emptySet();

	@Autowired
	private ElasticSearchDao elasticSearchDao;	

	private String esJobIndexName;
	
	private Boolean syncStatus = false;

	@Scheduled(cron = "${videokyc.call_wait.cron.job.interval}")
	public void updateCallWaitTime() {
		logger.info("starting call wait job");
		Optional<SimpleLock> lockObj = getLock(jobName);		
		Long start = System.currentTimeMillis();
		if (lockObj.isEmpty()) {
			logger.info("call wait job lock not acquired. so not running on this machine");
			addJobDetailsToEs("no lock", null, start);
			return;
		}
		try {
			logger.info("running call wait job");
			String date = CommonUtil.getDateTimeWithTimeZone(System.currentTimeMillis(), null);
			logger.info("updateCallWaitTime Job: " + date);
			List<VideokycAgentQueue> customers = videokycService.getCallWaitTime(beathingTime, averageCallDuration, syncStatus);
			logger.info("num customers : " + customers.size());
			ObjectNode data = videokycService.handleCallWaitChange(customers);
			addJobDetailsToEs("success", data, start);
		}
		catch(Exception e){
			logger.error("Error in updateCallWaitTime", e);
		}
		finally {
			lockObj.get().unlock();
		}
	}

	private Optional<SimpleLock> getLock(String name) {
		Instant lockAtLeast = Instant.now().plusMillis(videoKycCallWaitLockAtLeastTime);
		Instant lockAtMost = Instant.now().plusMillis(videoKycCallWaitLockAtMostTime);
		LockConfiguration config = new LockConfiguration(name, lockAtMost, lockAtLeast);
		return lockProvider.lock(config);
	}

	private void addJobDetailsToEs(String status, ObjectNode data, Long startTime) {
		try {			
			ObjectNode json = Json.newObject();			
			InetAddress host = InetAddress.getLocalHost();		
			json.put("machineIp", host.getHostAddress());
			json.put("jobStatus", status);
			json.put("jobName", "CallWaitJob");
			json.put("startDate", startTime);
			Long end = System.currentTimeMillis();
			json.put("endDate", end);
			json.put("duration", (end-startTime));			
			if (data != null) {
				Iterator<String> keys = data.fieldNames();
				while(keys.hasNext()) {
					String key = keys.next();
					if (data.get(key) != null) {
						json.put(key, data.get(key).asText());
					}
				}
			}
			elasticSearchDao.add(esJobIndexName, json.toString());
		} catch (Exception e) {
			logger.info("failed to send data to ES");
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		environment = env.getProperty(Constants.ENVIRONMENT);
		logger.info("environment: "+environment);	

		videoKycCallWaitLockAtLeastTime = Long.valueOf(env.getProperty(Constants.VIDEOKYC_CALL_WAIT_LOCK_ATLEAST_TIME));
		logger.info("videoKycCallWaitLockAtLeastTime: "+videoKycCallWaitLockAtLeastTime);	

		videoKycCallWaitLockAtMostTime = Long.valueOf(env.getProperty(Constants.VIDEOKYC_CALL_WAIT_LOCK_ATMOST_TIME));
		logger.info("videoKycCallWaitLockAtMostTime: "+videoKycCallWaitLockAtMostTime);	

		averageCallDuration = Integer.valueOf(env.getProperty(Constants.VIDEOKYC_AVERAGE_CALL_DURATION));
		logger.info("averageCallDuration: " + averageCallDuration);

		beathingTime = Short.valueOf(env.getProperty(Constants.VIDEOKYC_AGENT_BREATHING_TIME));
		logger.info("beathingTime: " + beathingTime);
		
		Integer database = Integer.valueOf(env.getProperty(Constants.REDIS_IMS_DB_INDEX));
		jobName = Constants.VIDEOKYC_CALL_WAIT_JOB_NAME + database;
		logger.info("jobName:  "+jobName);
		
		 String queueStatuses = env.getProperty(Constants.VIDEOKYC_MESSAGE_QUEUE_STATUES);
	        logger.info("queue statues: " + queueStatuses);

	        if (!StringUtils.isEmpty(queueStatuses)) {
	            String[] splitStatuses = queueStatuses.split(",");
	            queueStatues = new HashSet<>();
	            for (String status : splitStatuses) {
	                queueStatues.add(status.toLowerCase());
	            }
	        }
		
	        if(queueStatues.contains(VideoKYCStatus.AgentAssigned.getName().toLowerCase())) {
	        	syncStatus = true;
	        }


		esJobIndexName = env.getProperty(Constants.ES_JOB_INDEX_NAME);
		logger.info("esJobIndexName: "+esJobIndexName);

		lockProvider = redisConnection.getLockProvider(environment);	
	}
}
