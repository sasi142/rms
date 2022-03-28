package core.services;

import core.entities.MeetingEvent;
import core.utils.Enums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import core.daos.ElasticSearchDao;
import core.daos.UserEventDao;
import core.entities.MeetingEvent;
import core.daos.MeetingEventDao;
import core.entities.UserEvent;
import core.entities.UserInteractionEvent;
import core.utils.Constants;
import core.utils.Enums.UserInteractionType;
import core.utils.Enums.MeetingEventType;
import core.utils.Enums.UserEventType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EventTrackingServiceImpl implements EventTrackingService, InitializingBean {

	final Logger logger = LoggerFactory.getLogger(EventTrackingServiceImpl.class);

	@Autowired
	private UserEventDao userEventDao;

	@Autowired
	private MeetingEventDao meetingEventDao;

	@Autowired
	private ElasticSearchDao elasticSearchDao;	

	@Autowired
	private Environment			env;

	private String userInteractionEventIndexName;

	private Set<Integer> meetingEventSet;	

	private Set<Integer> userEventSet;	

	@Transactional(propagation = Propagation.REQUIRED)
	@Override	    
	public void sendMeetingEvent(Integer meetingId, Integer userId, Integer groupId, Byte eventType, Byte eventSource, String data) {	
		logger.debug("send meeting event "+eventType);
		UserInteractionEvent event = new UserInteractionEvent();
		if (meetingId != null) {
			event.setMeetingId(meetingId);
		}
		if (userId != null) {
			event.setUserId(userId);
		}
		if (eventType != null) {
			event.setEventType(eventType);
		}
		if (eventSource != null) {
			event.setEventSource(eventSource);
		}
		
		if (groupId != null) {
			event.setGroupId(groupId);
		}

		event.setCreatedDate(System.currentTimeMillis());	
		event.setType(UserInteractionType.Meeting.getId());
		if (data != null) {
			event.setData(data);
		}
		
		MeetingEventType meetingEvent = MeetingEventType.getEnum(eventType);
		try {
			if(!meetingEventSet.isEmpty() && meetingEventSet.contains(meetingEvent.getId())){
				logger.info("store meeting event in Mysql database");
				MeetingEvent updatedEvent = getMeetingEvent(meetingId, userId, eventType, eventSource, data);
				meetingEventDao.create(updatedEvent);
				logger.info("event stored successfully");
			}

			ObjectMapper mapper = new ObjectMapper();	
			String json = mapper.writeValueAsString(event);
			elasticSearchDao.add(userInteractionEventIndexName, json);
		} catch (Exception ex) {
			logger.error("failed to save to API Request. ", ex);
		}	
		logger.debug("sent meeting event "+eventType);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void sendUserEvent(Integer userId, Integer groupId, Byte eventType, Byte eventSource, String data) {	
		logger.debug("send user event "+eventType);
		UserInteractionEvent event = getUserInteractionEvent(userId, groupId, eventType, eventSource, data);

		UserEventType userEvent = UserEventType.getEnum(eventType);
		try {
			if(!userEventSet.isEmpty() && userEventSet.contains(userEvent.getId())){
				logger.info("store user event in Mysql database");
				UserEvent updatedEvent = getEvent(userId, groupId, eventType, eventSource, data);
				userEventDao.create(updatedEvent);
				logger.info("event stored successfully");
			}

			ObjectMapper mapper = new ObjectMapper();	
			String json = mapper.writeValueAsString(event);
			elasticSearchDao.add(userInteractionEventIndexName, json);
		} catch (Exception ex) {
			logger.error("failed to save to API Request. ", ex);
		}	
		logger.debug("sent user event "+eventType);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void sendUserConnectionEvent(Integer userId, Integer groupId, Byte eventType, Byte eventSource,
			String data) {
		logger.debug("send connection event "+eventType);	
		sendUserEvent(userId, groupId, eventType, eventSource, data);
		UserEvent event = getEvent(userId, groupId, eventType, eventSource, data);
		userEventDao.create(event);
	}

	private UserEvent getEvent(Integer userId, Integer groupId, Byte eventType, Byte eventSource, String data) {
		UserEvent event = new UserEvent();		
		if (userId != null) {
			event.setUserId(userId);
		}
		if (groupId != null) {
			event.setGroupId(groupId);
		}
		if (eventType != null) {
			event.setEventType(eventType);
		}
		if (eventSource != null) {
			event.setEventSource(eventSource);
		}
		event.setCreatedDate(System.currentTimeMillis());	
		event.setActive(true);
		if (data != null) {
			event.setData(data);
		}
		
		logger.debug("user event: "+event.toString());
		
		return event;
	}

	private MeetingEvent getMeetingEvent(Integer meetingId, Integer userId, Byte eventType, Byte eventSource, String data) {
		MeetingEvent event = new MeetingEvent();
		if (userId != null) {
			event.setUserId(userId);
		}
		if (meetingId != null) {
			event.setMeetingId(meetingId);
		}
		if (eventType != null) {
			event.setEventType(eventType);
		}
		if (eventSource != null) {
			event.setEventSource(eventSource);
		}
		event.setCreatedDate(System.currentTimeMillis());
		event.setActive(true);
		if (data != null) {
			event.setData(data);
		}
		return event;
	}
	
	private UserInteractionEvent getUserInteractionEvent(Integer userId, Integer groupId, Byte eventType, Byte eventSource, String data) {
		UserInteractionEvent event = new UserInteractionEvent();		
		if (userId != null) {
			event.setUserId(userId);
		}
		if (groupId != null) {
			event.setGroupId(groupId);
		}
		if (eventType != null) {
			event.setEventType(eventType);
		}
		if (eventSource != null) {
			event.setEventSource(eventSource);
		}
		event.setCreatedDate(System.currentTimeMillis());			
		if (data != null) {
			event.setData(data);
		}
		event.setType(UserInteractionType.User.getId());
		
		logger.debug("event interaction event: "+event.toString());
		
		return event;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		userInteractionEventIndexName = env.getProperty(Constants.ES_USER_INTERACTION_EVENT_INDEX_NAME);
		logger.info("userInteractionEventIndexName: "+userInteractionEventIndexName);

		String storeUserEvenListToMysqlDb = env.getProperty(Constants.STORE_USER_EVENT_LIST_TO_MYSQLDB);
		logger.info("storeUserEvenListToMysqlDb: "+storeUserEvenListToMysqlDb);
		Set<String> userSet =  Stream.of(storeUserEvenListToMysqlDb.trim().split("\\s*,\\s*") ).collect( Collectors.toSet());
		userEventSet = userSet.stream().map(Integer::parseInt).collect(Collectors.toSet());


		String storeMeetingEvenListToMysqlDb = env.getProperty(Constants.STORE_MEETING_EVENT_LIST_TO_MYSQLDB);
		logger.info("storeMeetingEvenListToMysqlDb: "+storeMeetingEvenListToMysqlDb);
		Set<String> meetingSet =  Stream.of(storeMeetingEvenListToMysqlDb.trim().split("\\s*,\\s*") ).collect( Collectors.toSet());
		meetingEventSet = meetingSet.stream().map(Integer::parseInt).collect(Collectors.toSet());
	}
}