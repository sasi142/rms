package core.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import play.libs.Json;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.daos.CacheConnectionInfoDao;
import core.entities.Group;
import core.entities.GroupMember;
import core.entities.Presence;
import core.exceptions.InternalServerErrorException;
import core.utils.Constants;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.MessageType;
import core.utils.Enums.PresenceStatus;
import core.utils.Enums.GroupType;
import core.utils.Enums.ClientType;

@Service
public class PresenceServiceImpl implements PresenceService, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(PresenceServiceImpl.class);
	@Autowired
	@Qualifier("CacheImpl")
	private CacheConnectionInfoDao cacheConnectionInfoDao;

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	@Autowired
	@Lazy
	private UserConnectionService userConnectionService;

	@Autowired
	private Environment env;

	private Boolean checkActorPresence;

	private Set<String> mobileIconClientIds;


	private Long mobileConnectionIdletime = 21600000L; // 6 hrs

	@Override
	public void afterPropertiesSet() throws Exception {
		checkActorPresence = Boolean.valueOf(env.getProperty(Constants.ACTOR_CHECK_PRESENCE));
		logger.info("checkActorPresence: " + checkActorPresence);

		String showDesktopIconClientStr = env.getProperty(Constants.MOBILE_ICON_CLIENT_IDS);
		String ids[] = showDesktopIconClientStr.split(",");
		mobileIconClientIds = new HashSet<>(Arrays.asList(ids));
		logger.info(
				"showMobileIconClientIds property " + Constants.MOBILE_ICON_CLIENT_IDS + " = " + mobileIconClientIds);

		mobileConnectionIdletime = Long.valueOf(env.getProperty(Constants.MOBILE_CONNECTION_IDLE_TIME));
		logger.info("mobileConnectionIdletime: " + mobileConnectionIdletime);
	}

	@Override
	@Async("presenceTaskExecutor")
	public void sendPresenceInfoToContact(Integer Id) {	
		JsonNode openWindowObject= cacheService.getUserObjectWithOpenChatWindow(Id);
		logger.info("send PresenceInfo To All Contacts sizing " );
		if (openWindowObject != null  && !openWindowObject.isMissingNode()) {				
			JsonNode contactIds = openWindowObject.get("users");		
			logger.debug("Getting array of users who has to send presence "+ contactIds );
			ObjectNode node = getJsonPresenceNode();
			node.put("id", Id);
			Presence presence = getPresence(Id, true);
			node.put("presence", Json.toJson(presence));
			if(contactIds != null && !contactIds.isMissingNode()) {
				for (JsonNode connectionNode : contactIds) {
					Integer contactId = connectionNode.asInt();
					userConnectionService.sendMessageToActor(contactId, node, null);
					logger.debug("sent PresenceInfo To Contact " + contactId);

				}
			}	
			JsonNode groupIds = openWindowObject.get("guestGroups");	
			logger.debug("Getting array of guestGroups "+ groupIds );		
			if(groupIds != null  && !groupIds.isMissingNode()) {
				for (JsonNode groupIdNode: groupIds) {
					Integer guestGroupId = groupIdNode.asInt();
					logger.debug("send presence of registered user to guest user: "+guestGroupId);							
					Group group = cacheService.getGroupDetails(guestGroupId);
					if(group.getGroupType().equals(GroupType.GuestDirectGroupChat.getId().byteValue())) {	
						//For GuestDirectGroup Chat send presence of creator only
						userConnectionService.sendMessageToActor(group.getGuestUserId(), node, null);
						logger.debug("sent PresenceInfo To Contact " + group.getGuestUserId());
					} else {
						sendPresencetoGuest(group, Id);
						logger.debug("sent PresenceInfo To Contact " + group.getGuestUserId());
					}
				}
			}
		}

	}

	//using guest group: 1. check presence of all members, if any one connected to web not send presence(on socket connection). 
	//2. check presence of all members if no one connected to web or android then send presence(on socket disconnect)
	public void sendPresencetoGuest(Group group, Integer Id) {
		List<GroupMember> groupMembers = group.getMembers();					
		logger.debug("Getting array of users who is in contact with guest "+ groupMembers );
		Set<Integer> webIds = new HashSet<>();
		Set<Integer> mobileIds = new HashSet<>();
		for (GroupMember member : groupMembers ) {	
			if(!member.getId().equals(group.getGuestUserId())) { // if not a guest
				ArrayNode arrayNode = cacheConnectionInfoDao.getAll(member.getId());	
				for (JsonNode connectionNode : arrayNode) {
					String cid = connectionNode.findPath("cid").asText();
					if(cid.equalsIgnoreCase(ClientType.Web.getClientId())) {
						webIds.add(member.getId());
					} 
					else if (cid.equalsIgnoreCase(ClientType.AndroidChatApp.getClientId()) || cid.equalsIgnoreCase(ClientType.iOSChatApp.getClientId())) {
						mobileIds.add(member.getId());
					} 
				}
			}												
		}	

		Integer presenceValue = -1; // placeholder
		if (!webIds.isEmpty()) {
			if (webIds.contains(Id) && webIds.size() == 1) {
				presenceValue = PresenceStatus.AvailableWeb.ordinal();
			}
		}						
		else if (!mobileIds.isEmpty()) {
			if (mobileIds.contains(Id) && mobileIds.size() == 1) {
				presenceValue = PresenceStatus.AvailableMobile.ordinal();
			}
		}	
		else {
			presenceValue = PresenceStatus.Unavailable.ordinal();
		}

		if (presenceValue != -1) {
			logger.debug("sending presence to contact:" +  group.getGuestUserId());
			ObjectNode groupPresenceNode = getJsonPresenceNode();	
			Presence groupPresence = new Presence();
			groupPresence.setShow(presenceValue);
			//	No need to send id for guest
			//	groupPresenceNode.put("id", guestGroupId);
			groupPresenceNode.put("presence", Json.toJson(groupPresence));
			userConnectionService.sendMessageToActor(group.getGuestUserId(), groupPresenceNode, null);
			logger.debug("sent  presence to contact:" +  group.getGuestUserId());
		}   

	}

	public Presence getGroupPresence(Integer groupId) {
		Group group = cacheService.getGroupDetails(groupId);
		List<GroupMember> groupMembers = group.getMembers();					
		logger.debug("Getting array of users who is in contact with guest "+ groupMembers );
		Set<Integer> webIds = new HashSet<>();
		Set<Integer> mobileIds = new HashSet<>();
		List<Integer> userConnClientIds = new ArrayList<>();
		for (GroupMember member : groupMembers ) {	
			if(!member.getId().equals(group.getGuestUserId())) { // if not a guest
				ArrayNode arrayNode = cacheConnectionInfoDao.getAll(member.getId());	
				for (JsonNode connectionNode : arrayNode) {
					String cid = connectionNode.findPath("cid").asText();
					if(cid.equalsIgnoreCase(ClientType.Web.getClientId())) {
						webIds.add(member.getId());
						userConnClientIds.add(Integer.valueOf(cid));
					} 
					else if (cid.equalsIgnoreCase(ClientType.AndroidChatApp.getClientId()) || cid.equalsIgnoreCase(ClientType.iOSChatApp.getClientId())) {
						mobileIds.add(member.getId());
						userConnClientIds.add(Integer.valueOf(cid));
					} 
				}
			}												
		}
		Integer presenceValue;
		if (!webIds.isEmpty()) {			
			presenceValue = PresenceStatus.AvailableWeb.ordinal();
			//userConnClientIds.add(Integer.valueOf(ClientType.Web.getClientId()));
		}						
		else if (!mobileIds.isEmpty()) {		
			presenceValue = PresenceStatus.AvailableMobile.ordinal();	
			//userConnClientIds.add(Integer.valueOf(ClientType.AndroidChatApp.getClientId()));
		}	
		else {
			presenceValue = PresenceStatus.Unavailable.ordinal();
		}
		Presence groupPresence = new Presence();
		groupPresence.setShow(presenceValue);
		groupPresence.setClientIds(userConnClientIds);
		return groupPresence;
	}

	@Override
	public Presence getPresence(Integer Id, Boolean checkClosed) {
		logger.debug("get presence for " + Id + ",checkClosed:" + checkClosed);
		if (!checkClosed) {
			Presence presence = new Presence();
			presence.setShow(PresenceStatus.Unavailable.ordinal());
			return presence;
		}

		ArrayNode arrayNode = cacheConnectionInfoDao.getAll(Id);
		List<Integer> userConnClientIds = new ArrayList<>();
		if (arrayNode != null && arrayNode.size() > 0) {
			for (JsonNode node : arrayNode) {
				Integer clientId = node.findPath("cid").asInt();
				userConnClientIds.add(clientId);
			}
		}
		Presence presence = new Presence();
		if (userConnClientIds.size() == 0) {
			presence.setShow(PresenceStatus.Unavailable.ordinal());
		} else {
			boolean found = false;
			/*	for (String clientId : mobileIconClientIds) {
				if (userConnClientIds.contains(Integer.valueOf(clientId))) {
					found = true;
					break;
				}
			}
			if (found) {
				presence.setShow(PresenceStatus.AvailableMobile.ordinal());
			} else {
				presence.setShow(PresenceStatus.AvailableWeb.ordinal());
			}*/
			Integer webClientId = Integer.parseInt(ClientType.Web.getClientId());

			if (userConnClientIds.contains(webClientId)) {
				found = true;					
			}

			if (found) {

				presence.setShow(PresenceStatus.AvailableWeb.ordinal());
			} else {				
				presence.setShow(PresenceStatus.AvailableMobile.ordinal());
			}
		}

		presence.setClientIds(userConnClientIds);
		logger.debug("Got presence info " + presence.toString());
		return presence;
	}

	private ObjectNode getJsonPresenceNode() {
		ObjectNode node = Json.newObject();
		node.put("type", MessageType.Presence.getId());
		return node;

	}
}
