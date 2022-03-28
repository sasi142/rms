package core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.daos.*;
import core.entities.Event;
import core.entities.User;
import core.entities.VideokycAgentQueue;
import core.entities.projections.VideoKyc;
import core.utils.*;
import core.utils.Enums.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import play.libs.Json;

import java.util.*;
import java.util.function.Supplier;

@Service
@Transactional(rollbackFor = { Exception.class })
public class VideokycServiceImpl implements VideokycService, InitializingBean {

	private static final String AVAILABLE_CUSTOM_STATUS = "Available";

	final static Logger logger = LoggerFactory.getLogger(VideokycServiceImpl.class);

	@Autowired
	private VideokycAgentQueueDao videokycAgentQueueDao;

	@Autowired
	private VideoKycDao videoKycDao;

	@Autowired
	private VideoKycCustomerQueueDao videokycCustomerQueueDao;

	@Autowired
	private UserConnectionService userConnectionService;

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	@Autowired
	private CacheUserDao cacheUserDao;

	private Byte priority = 1;

	private Short beathingTime = 0;

	private Integer averageCallDuration = 120;

	private Byte retryLimit = 2;

	@Autowired
	private Environment env;

	@Autowired
	@Qualifier("RedisCache")
	private CacheConnectionInfoDao cacheConnectionInfoDao;

	@Autowired
	private RetryUtil retryUtil;

	@Value("${agent.status.update.retry.max:3}")
	private int retryMax;

	@Value("${agent.status.update.retry.delay:100}")
	private int retryDelay;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private OrgUtil orgUtil;

	@Autowired
	private CacheVideoKycDao cacheVideoKycDao;

	/**
	 *  Agent
	 *	if agent assigned, dont do anything
	 *  if agent not assigned, then see if available (using 'Available' status and connection info).
	 *     if available change the status	
	 * 
	 *  Customer
	 *  if status waiting then remove from queue. Change status to Open
	 *  if not waiting then don't do anything
	 *  	
	 */
	@Override
	public void onUserConnection(User user) {
		logger.info("on connect - user role: "+user.getRoles());
		if (RoleName.VideoKYCAgent.getName().equalsIgnoreCase(user.getRoles())) {
			logger.info("agent is connected. change his status to available: "+user.getId());
		}
		else if (UserCategory.Guest.getId().equals(user.getUserCategory().intValue())) {
			logger.info("guest id: "+user.getId());
			JsonNode kycInfo = cacheService.getVideoKycInfo(user.getId());	
			if (kycInfo != null) {
				boolean valid = false;
				JsonNode activeNode = kycInfo.findPath("active");
				if (activeNode != null) {
					boolean active = activeNode.asBoolean();
					logger.info("kyc active: "+active);
					if (active) {
						valid = true;
					}
				}

				if (valid) {
					Integer kycId = kycInfo.get("id").asInt();
					logger.info("Non deleted kyc {}. video kyc customer", kycId);
					int status = kycInfo.findPath("status").asInt();
					logger.info("video kyc status: "+status);
					Integer groupId = kycInfo.findPath("groupId").asInt();
					logger.info("group Id for kyc customer :"+groupId);
					if (VideoKYCStatus.Open.getId().intValue() == status || VideoKYCStatus.Waiting.getId().intValue() == status) {
						logger.info("try call wait for group Id :"+groupId+", userId: "+user.getId());
						videokycAgentQueueDao.flush();
						String ipriority = kycInfo.findPath("priority").asText();
						priority =  Byte.valueOf(ipriority);
						//Procedure name & input parameter changed.
						videokycCustomerQueueDao.add(kycId, kycInfo.get("guestGroupId").asLong(), groupId, user.getId(), priority);
						logger.info("tried call wait");
					}
					else if (VideoKYCStatus.AgentAssigned.getId().intValue() == status) {
						logger.info("status is agent assigned. So send message on socket");
						ObjectNode node = Json.newObject();
						node.put("type", MessageType.VideoKyc.getId());		
						Integer agentId = kycInfo.findPath("agentId").asInt();
						node.put("agentId", agentId);
						JsonNode agentJson = cacheService.getUserJson(agentId);	
						String name = agentJson.findPath("firstName").asText();	
						if (name != null) {
							node.put("agentName", name);
						}

						node.put("customerName", user.getName());
						node.put("groupId", groupId);
						node.put("kycId", kycId);

						Integer orgId = kycInfo.findPath("organizationId").asInt();
						JsonNode organization = cacheService.getOrgJson(orgId);
						String enableGuestLocation = orgUtil.getPreference(organization, "EnableGuestLocation");
						String enableAadhaarXML = orgUtil.getPreference(organization, "EnableAadhaarXML");

						node.put("organizationId", orgId);
						node.put("enableGuestLocation", enableGuestLocation);
						node.put("enableAadhaarXML", enableAadhaarXML);
						node.put("status", VideokycAgentStatus.Available.getName());
						userConnectionService.sendMessageToActor(user.getId(), node, null);
					}
					else {
						logger.info("non open no agent assigned kyc. so expire it");
						ObjectNode node = Json.newObject();
						node.put("type", MessageType.Event.getId());
						node.put("subtype", EventType.SessionExpired.getId());
						node.put("reason", SessionExpiryReason.OpenKycNotAvailable.getName());
						userConnectionService.sendMessageToActor(user.getId(), node, null);
					}
				}
				else {
					logger.info("deleted kyc. send session expired event to customer");
					ObjectNode node = Json.newObject();
					node.put("type", MessageType.Event.getId());
					node.put("subtype", EventType.SessionExpired.getId());
					node.put("reason", SessionExpiryReason.ActiveKycNotAvailable.getName());
					userConnectionService.sendSessionExpiredMessageToActor(user.getId(), node);
				}
			}	
			else {
				logger.info("its not a video kyc customer");
			}
		}		
	}

	/**
	 * AGENT
	 * - if kyc assigned then don't do anything
	 * - if kyc not assigned, mark him not available
	 * 
	 * Customer
	 * - if kyc customer and is waiting then remove from queue and change kyc status
	 *   if not waiting don't do anything
	 */

	@Override
	public void onUserDisconnect(User user) {
		logger.info("on disconnect - user role: "+user.getRoles());
		if (RoleName.VideoKYCAgent.getName().equalsIgnoreCase(user.getRoles())) {
			logger.info("agent disconnected: "+user.getId());
//			// If agent is available after disconnecting socket means this socket is from video window.
//			// No need to change his status as he is  still connected.
//			Byte onlineStatus = isConnected(user.getId());
//			if (VideokycAgentStatus.NotAvailable.getId().equals(onlineStatus.intValue())) {
//				logger.info("agent is disconnected and not available. run call wait");
//				changeAgentStatus(user.getId(), onlineStatus);
//				videokycAgentQueueDao.flush();
//			}
//			else {
//				logger.info("agent is disconnected but available. he might have disconnected from video screen");
//			}
		}
		else if (UserCategory.Guest.getId().equals(user.getUserCategory().intValue())) {
			logger.info("guest disconnected: "+user.getId());
			JsonNode kycInfo = cacheService.getVideoKycInfo(user.getId()); 
			logger.info("kycInfo: "+kycInfo);
			if (kycInfo != null) { 
				videokycCustomerQueueDao.deleteCustomerById(user.getId());		
				logger.info("deleted customer from queue");
			}
		}
		else {
			logger.info("non kyc guest");
		}
	}



	/**
	 * Check if he already exists in queue. if yes then update the record. if not then add one.
	 */	
	@Override
	public void addAgentInQueue(List<Integer> members, Long groupId) {
		logger.info("Add agent in queue for group : "+groupId);
		for(Integer userId : members) {
			String[] role = cacheService.getUserRole(userId);
			if(Arrays.asList(role).contains(RoleName.VideoKYCAgent.getName())) {	
				logger.info("check if he already exists in queue");
				Boolean exists = false;
				VideokycAgentQueue agent = videokycAgentQueueDao.getByGroupAndAgentId(userId, groupId);
				if (agent != null) {					
					exists = true;
				}
				else {
					logger.info("Add agent in queue: "+userId);
					agent = new VideokycAgentQueue();
					agent.setAgentId(userId);
					agent.setGroupId(Long.valueOf(groupId));					
					exists = false;
				}
				logger.info("set customer to null");
				//Set agent status to not available initially, then call changeAgentStatus procedure(existing agent assignment handled in this)	
				agent.setAgentStatus(VideokycAgentStatus.NotAvailable.getId().byteValue());
				agent.setUpdatedDate(System.currentTimeMillis());
				agent.setAvailableDate(System.currentTimeMillis());
				agent.setActive(true);
				agent.setGuestUserId(null);
				if (exists) {
					videokycAgentQueueDao.update(agent);
				}
				else {
					videokycAgentQueueDao.create(agent);
				}
				videokycAgentQueueDao.flush();
				updateAgentStatusInAgentQueue(userId);
				logger.info("Added agent in queue done ");									
			}			
		}
	}


	/**
	 * Ideally if KYC is assigned to agent, he should not be allowed to remove from queue.
	 * This check should be done in IMS. Here as well, same check is added
	 */
	@Override
	public void removeAgentFromQueue(List<Integer> members, Long groupId) {
		for(Integer userId : members) {
			String[] role = cacheService.getUserRole(userId);
			if(Arrays.asList(role).contains(RoleName.VideoKYCAgent.getName())) {
				logger.info("remove agent from queue");
				VideokycAgentQueue agent = videokycAgentQueueDao.getByGroupAndAgentId(userId, groupId);					
				if(agent != null && agent.getAgentId() != null) {					
					videokycAgentQueueDao.deActivateById(agent.getId());
				}				
			}			
		}
	}		

	private Byte getOnlineStatus(Integer userId) {
		String customStatus = cacheService.getUserCustomStatus(userId);
		return AVAILABLE_CUSTOM_STATUS.equalsIgnoreCase(customStatus)?
				VideokycAgentStatus.Available.getId().byteValue():
				VideokycAgentStatus.NotAvailable.getId().byteValue();
	}

	private Integer socketConnectionCount(Integer userId) {
		ArrayNode arrayNode = cacheConnectionInfoDao.getAll(userId);
		return arrayNode.size();
	}

	private Byte isConnected(Integer userId) {		
		ArrayNode arrayNode = cacheConnectionInfoDao.getAll(userId);		
		logger.info("arrayNode: "+arrayNode.size());
		if (arrayNode.size() > 0) {
			return VideokycAgentStatus.Available.getId().byteValue();
		}
		return VideokycAgentStatus.NotAvailable.getId().byteValue();
	}


	@Override
	public ObjectNode handleCallWaitChange(List<VideokycAgentQueue> callWaitData) {

		Integer waitTimeUpperLimit = Integer.valueOf(PropertyUtil.getProperty(Constants.CALL_WAIT_TIME_UPPER_LIMIT));

		List<VideokycAgentQueue> busy = new ArrayList<>();
		List<VideokycAgentQueue> notAvailable = new ArrayList<>();
		List<VideokycAgentQueue> assigned = new ArrayList<>();

		//split it in 3 categories
		for (VideokycAgentQueue callWait : callWaitData) {
			if (callWait.getAgentId() != null) {
				//agent assigned
				assigned.add(callWait);
			} else if (callWait.getCallWaitDuration() != null){
				if (callWait.getCallWaitDuration() >= waitTimeUpperLimit) {
					//crossed limit
					notAvailable.add(callWait);
				}
				else{
					//within limit
					busy.add(callWait);
				}
			} else {
				logger.info("invalid data received in call wait");
			}
		}

		logger.info("Process Not available for {}", notAvailable);
		notAvailable.forEach(this::processNotAvailable);
		logger.info("Process busy for {}", busy);
		busy.forEach(this::processBusy);
		logger.info("Process Assigned for {}", assigned);
		assigned.forEach(this::processAssigned);

		ObjectNode data = Json.newObject();
		data.put("agentsAssigned", assigned.size());
		data.put("customersWaiting", busy.size());
		return data;
	}

    @Override
    public void removeCustomersFromQueue(List<Integer> userIds) {
        videokycCustomerQueueDao.deleteCustomerByIds(userIds);
    }

	@Override
	public Optional<VideoKyc> getVideoKycStatusByGroupId(Long groupId) {
		return videoKycDao.getVideoKycStatusByGroupId(groupId);
	}

	private void processAssigned(VideokycAgentQueue callWait) {
		logger.info("Assigning kyc {} to agent {} of guest user {} guest group {}",
				callWait.getVideoKycId(), callWait.getAgentId(), callWait.getGuestUserId(), callWait.getGuestGroupId());

		ObjectNode kyc = cacheVideoKycDao.get(callWait.getVideoKycId());

		ObjectNode node = Json.newObject();
		node.put("type", MessageType.VideoKyc.getId());
		node.put("agentId", callWait.getAgentId());
		node.put("customerId", callWait.getGuestUserId());
		node.put("kycId", callWait.getVideoKycId());
		JsonNode agentJson = cacheService.getUserJson(callWait.getAgentId());
		String name = agentJson.findPath("firstName").asText();
		if (name != null) {
			node.put("agentName", name);
		}
		node.put("status", VideokycAgentStatus.Available.getName());

		JsonNode customer = cacheService.getUserJson(callWait.getGuestUserId());
		node.put("customerName", customer.findPath("firstName").asText());
		node.put("groupId", callWait.getGuestGroupId());
		Integer orgId = kyc.findPath("organizationId").asInt();
		JsonNode organization = cacheService.getOrgJson(orgId);
		String enableGuestLocation = orgUtil.getPreference(organization, "EnableGuestLocation");
		String enableAadhaarXML = orgUtil.getPreference(organization, "EnableAadhaarXML");
		node.put("organizationId", orgId);
		node.put("enableGuestLocation", enableGuestLocation);
		node.put("enableAadhaarXML", enableAadhaarXML);


		Event event = new Event();
		event.setType(EventType.AgentAssignedToVideoKyc.getId());
		Map<String, String> props = new HashMap<>();
		props.put("agentId", String.valueOf(callWait.getAgentId()));
		props.put("guestGroupId", String.valueOf(callWait.getGuestGroupId()));
		props.put("kycId", String.valueOf(callWait.getVideoKycId()));	
		event.setData(props);

		logger.info("Mark kyc {}, agent {}, guest group {} assigned in cache", callWait.getVideoKycId(), callWait.getAgentId(), callWait.getGuestGroupId());
		cacheService.markKycAgentAssigned(callWait.getVideoKycId(), callWait.getAgentId(), callWait.getGuestGroupId());
		logger.info("Send message to actor {}, event {}", callWait.getGuestUserId(), node.toString());
		userConnectionService.sendMessageToActor(callWait.getGuestUserId(), node, null);
		logger.info("Send agent assigned event {}", event.toString());
		//userConnectionService.agentAssignedEvent(event);
		EventService eventService = (EventService) applicationContext.getBean(Constants.EVENT_SERVICE_SPRING_BEAN);
		eventService.agentAssignedToVideoKyc(event);
	}

	private void processBusy(VideokycAgentQueue callWait) {
		ObjectNode node = Json.newObject();
		node.put("type", MessageType.VideoKyc.getId());
		node.put("status", VideokycAgentStatus.Busy.getName());
		node.put("callWaitTime", callWait.getCallWaitDuration());
		logger.info("send message on customer socket: "+node.toString());
		userConnectionService.sendMessageToActor(callWait.getGuestUserId(), node, null);
	}

	private void processNotAvailable(VideokycAgentQueue callWait) {
		ObjectNode node = Json.newObject();
		node.put("type", MessageType.VideoKyc.getId());
		node.put("status", VideokycAgentStatus.NotAvailable.getName());
		node.put("callWaitTime", callWait.getCallWaitDuration());
		userConnectionService.sendMessageToActor(callWait.getGuestUserId(), node, null);
	}

//	private void handleAssignAgentToCustomer(List<Integer> videoKycIds, List<Long> groupIds, List<Event> events, List<ObjectNode> nodes, List<Integer> userIds) {
//		logger.info("refresh video kyc cache started");
//		try {
//		    videokycAgentQueueDao.refreshVideoKycCache(videoKycIds, groupIds);
//		    logger.info("refresh video kyc cache done");
//		} catch(Exception e) {
//			logger.info("refresh video kyc cache ims call failed. "+e);
//			//TODO: What can be done here to rollback the operation
//		}
//		logger.info("refresh video kyc cache done successfully, events.size(): "+events.size());
//		for(int index=0; index < events.size(); index++) {
//			logger.info("send message to customer started............");
//			ObjectNode node = nodes.get(index);
//			logger.info("..........node ...."+node);
//			Integer customerId = nodes.get(index).get("customerId").asInt();
//			logger.info("send agent assigned event to customer: " +customerId);
//			Integer agentId = node.get("agentId").asInt();
//			logger.info("agentId : " +agentId);
//			userConnectionService.sendMessageToActor(customerId, node, null);
//			logger.info("send update group, agent added event to both customer & agent, customerId: "+customerId);
//			Event event = events.get(index);
//			logger.info("---------event-------------- :"+event);
//			userConnectionService.agentAssignedEvent(event);
//		}
//	}
//
//	@Override
//	public void cleanupCustomerQueue() {
//
//	}

	@Override
	public void deleteCustomerById(Integer customerId) {
		logger.info("mark customer Inactive in db for customerId: "+customerId);
		try {	
			videokycCustomerQueueDao.deleteCustomerById(customerId);				
		}
		catch(Exception ex) {
			logger.info("failed to clean up ",ex);	
		}
	}

	@Override
	public Integer getGroupCallWaitTime(Integer groupId, Integer priority){
		logger.info("get call wait time from db for GroupId: "+groupId);		
		Integer callWaitData = videokycAgentQueueDao.GetVideoKycGroupCallWait(groupId, priority.byteValue(), beathingTime, averageCallDuration);
		logger.info("returned call wait time from db successfully: "+callWaitData);
		return callWaitData;
	}

	@Override
	public void updateAgentStatusInAgentQueue(@NonNull Integer userId) {
		logger.info("Update agent {} status in queue", userId);
		Byte onlineStatus = getOnlineStatus(userId);
		String message = String.format("Update agent %d status %d in queue", userId, onlineStatus);
		logger.info(message);
		Supplier<Void> call = () -> {
			videokycAgentQueueDao.updateAgentQueueStatus(userId, onlineStatus);
			return null;
		};
		retryUtil.retry(message, retryMax, retryDelay, call);
	}

	/**
	 * @param userId
	 */
	@Override
	public void changeUserStatusInQueue(Integer userId) {
		String[] role = cacheService.getUserRole(userId);
		if (Arrays.asList(role).contains(RoleName.VideoKYCAgent.getName())) {
			updateAgentStatusInAgentQueue(userId);
		}
	}

	@Override
	public List<VideokycAgentQueue> getCallWaitTime(Short breathingTime, Integer avgCallDuration, Boolean syncStatus) {
		logger.info("breathingTime: "+breathingTime+", avgCallDuration: "+avgCallDuration);
		List<VideokycAgentQueue> callWait = videokycAgentQueueDao.getCallWaitTime(breathingTime, avgCallDuration, syncStatus);
		logger.info("callWaitQueue: "+callWait);		
		return callWait;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		averageCallDuration = Integer.valueOf(env.getProperty(Constants.VIDEOKYC_AVERAGE_CALL_DURATION));
		logger.info("averageCallDuration: " + averageCallDuration);

		beathingTime = Short.valueOf(env.getProperty(Constants.VIDEOKYC_AGENT_BREATHING_TIME));
		logger.info("beathingTime: " + beathingTime);

		retryLimit = Byte.valueOf(env.getProperty(Constants.CUSTOMER_QUEUE_FAILURE_RETRY_LIMIT));
	}
}
