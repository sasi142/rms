package core.schedule;


import java.net.InetAddress;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import core.daos.ElasticSearchDao;
import core.entities.VideokycAgentQueue;
import core.redis.RedisConnection;
import core.services.MemoService;
import core.services.VideokycService;
import core.utils.CommonUtil;
import core.utils.Constants;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import play.libs.Json;

@Component
@EnableScheduling
public class MemoProcessScheduledJobs implements InitializingBean {
	private static final Logger	logger	= LoggerFactory.getLogger(RmsScheduledJobs.class);

	@Autowired
	private RedisConnection redisConnection;

	@Autowired
	private Environment env;

	private String environment;

	@Autowired
	private MemoService memoService;

	private Long memoProcessJobLockAtLeastTime = 3000L;
	private Long memoProcessJobLockAtMostTime = 30000L;

	private LockProvider lockProvider;
	
	private String jobName;

	@Scheduled(cron = "${bulk.memo.process.cron.job.interval}")
	public void bulkMemoProcess() {
		logger.info("starting memo process job");
		Optional<SimpleLock> lockObj = getLock(jobName);		
		if (lockObj.isEmpty()) {
			logger.info("memo process job lock not acquired. so not running on this machine");			
			return;
		}	
		try {
		String date = CommonUtil.getDateTimeWithTimeZone(System.currentTimeMillis(), null);
		logger.info("memoProcess Job started time is: "+date);
		memoService.processBulkMemo();	
		
		String endDate = CommonUtil.getDateTimeWithTimeZone(System.currentTimeMillis(), null);
		logger.info("memoProcess Job end time is: "+endDate);

		}
		catch(Exception e){
			logger.error("Error in Memo process job", e);
		}
		finally {
	//	if (lockObj.isPresent()) {
			lockObj.get().unlock();	
		}
		
	}

	private Optional<SimpleLock> getLock(String name) {
		Instant lockAtLeast = Instant.now().plusMillis(memoProcessJobLockAtLeastTime);
		Instant lockAtMost = Instant.now().plusMillis(memoProcessJobLockAtLeastTime);
		LockConfiguration config = new LockConfiguration(name, lockAtMost, lockAtLeast);
		Optional<SimpleLock> lockObj = lockProvider.lock(config);
		return lockObj;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		environment = env.getProperty(Constants.ENVIRONMENT);
		logger.info("environment: "+environment);	

		memoProcessJobLockAtLeastTime = Long.valueOf(env.getProperty(Constants.MEMO_PROCESS_JOB_LOCK_ATLEAST_TIME));
		logger.info("memoProcessJobLockAtLeastTime: "+memoProcessJobLockAtLeastTime);	

		memoProcessJobLockAtLeastTime = Long.valueOf(env.getProperty(Constants.MEMO_PROCESS_JOB_LOCK_ATMOST_TIME));
		logger.info("memoProcessJobLockAtLeastTime: "+memoProcessJobLockAtLeastTime);	

		Integer database = Integer.valueOf(env.getProperty(Constants.REDIS_IMS_DB_INDEX));
		jobName = Constants.MEMO_PROCESS_JOB_NAME + database;
		logger.info("jobName:  "+jobName);

		lockProvider = redisConnection.getLockProvider(environment);	
	}
}
