package core.daos.impl;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import core.daos.CacheVideoMeetingRoomDao;
import core.redis.RedisConnection;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;
import core.utils.Enums.VideoCallStatus;
import redis.clients.jedis.Jedis;

@Repository
public class CacheVideoMeetingRoomDaoImpl implements CacheVideoMeetingRoomDao, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(CacheVideoMeetingRoomDaoImpl.class);
	
	@Autowired
	public RedisConnection redisConnection;

	@Autowired
	public Environment env;

	public String videoMeetingRoomBucketName;

	@Override
	public void afterPropertiesSet() throws Exception {
		videoMeetingRoomBucketName = env.getProperty(Constants.REDIS_RMS_VIDEO_MEETING_ROOM_STORE);
		logger.info("VideoMeetingRoomBucketName: " + videoMeetingRoomBucketName);
	}

	@Override
	public VideoCallStatus createOrJoin(String meetingId, Integer userId) {
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
		VideoCallStatus status = null;
		try {
			Set<String> coworkers = jedis.smembers(getRoom(meetingId));
			if (coworkers == null || coworkers.isEmpty()) {
				status = VideoCallStatus.Created;
			}
			else {
				status = VideoCallStatus.Joined;
			}
			
			jedis.sadd(getRoom(meetingId), String.valueOf(userId));
			return status;
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Rms);
		}		
	}

	@Override
	public void remove(String meetingId, Integer userId) {
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
		try {
			jedis.srem(getRoom(meetingId), String.valueOf(userId));
			Set<String> coworkers = jedis.smembers(getRoom(meetingId));
			if (coworkers == null || coworkers.isEmpty()) {
				jedis.del(getRoom(meetingId));
			}
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Rms);
		}		
	}

	@Override
	public Set<String> getUsers(String meetingId) {
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
		try {
			Set<String> members = jedis.smembers(getRoom(meetingId));
			return members;
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Rms);
		}
	}
	
	private String getRoom(String meetingId) {
		String room = videoMeetingRoomBucketName+"-"+meetingId;
		return room;
	}
}
