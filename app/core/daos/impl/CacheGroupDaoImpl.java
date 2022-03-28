/**
 * 
 */
package core.daos.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import core.daos.CacheGroupDao;
import core.entities.ChatContact;
import core.entities.Group;
import core.entities.GroupMember;
import core.entities.UserContext;
import core.redis.RedisConnection;
import core.utils.CacheUtil;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;
import core.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Chandramohan.Murkute
 */
@Repository
public class CacheGroupDaoImpl implements CacheGroupDao, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(CacheGroupDaoImpl.class);
	@Autowired
	public RedisConnection	redisConnection;

	@Autowired
	public Environment		env;

	public String			groupBucketName;

	@Autowired
	private JsonUtil jsonUtil;

	@Autowired
	private CacheUtil cacheUtil;


	@Override
	public void afterPropertiesSet() throws Exception {
		groupBucketName = env.getProperty(Constants.REDIS_IMS_GROUP_STORE);
		logger.info("groupBucketName => REDIS_IMS_GROUP_STORE : " + groupBucketName);
	}


	public List<Integer> getGroupMembers(Integer groupId) {
		logger.debug("get Group Members for groupId " + groupId);
		List<Integer> groupMemberIds = new ArrayList<>();
		Group group = getGroup(groupId);
		if (group.getMembers() != null && !group.getMembers().isEmpty()) {
			List<GroupMember> members = group.getMembers();
			for (GroupMember groupMember : members) {
				if (groupMember.getMemberStatus().byteValue() == 1) {
					groupMemberIds.add(groupMember.getId());
				}
			}
		}
		logger.debug("returning all Group Members");
		return groupMemberIds;
	}
	public Set<Integer> getGroupMembersSet(Integer groupId) {
		logger.debug("get Group Members Set for groupId " + groupId);
		Set<Integer> groupMemberIds = new HashSet<>();
		Group group = getGroup(groupId);
		if (group.getMembers() != null && !group.getMembers().isEmpty()) {
			List<GroupMember> members = group.getMembers();
			for (GroupMember groupMember : members) {
				if (groupMember.getMemberStatus().byteValue() == 1) {
					groupMemberIds.add(groupMember.getId());
				}
			}
		}
		logger.debug("returning all Group Members Set");
		return groupMemberIds;
	}

	public List<Integer> getGroupMembers(Group group) {
		logger.debug("get Group Members for group " + group.getName());
		List<Integer> groupMemberIds = new ArrayList<>();
		if (group.getMembers() != null && !group.getMembers().isEmpty()) {
			List<GroupMember> members = group.getMembers();
			for (GroupMember groupMember : members) {
				if (groupMember.getMemberStatus().byteValue() == 1) {
					groupMemberIds.add(groupMember.getId());
				}
			}
		}
		logger.debug("returning all Group Members");
		return groupMemberIds;
	}

	private String getKey(Integer groupId) {
		return groupBucketName + ":" + groupId;
	}

	public Group getGroup(Integer groupId) {
		String groupStr = null;
		Group group = null;
		logger.debug("get Group with groupId " + groupId);
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		try {
			groupStr = jedis.hget(groupBucketName, String.valueOf(groupId)); // get json from redis
			logger.debug("returning Group {} with groupId {} ", groupStr,  groupId);
			if (groupStr != null && !"".equalsIgnoreCase(groupStr)) {
				ObjectMapper mapper = new ObjectMapper();
				group = mapper.readValue(groupStr, Group.class);
			}
		} catch (IOException e) {
			logger.error("Error parsing Group JSON String : " + groupStr, e);
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
		logger.debug("returning Group with groupId " + groupId);
		return group;
	}


	public GroupMember getGroupMember(Integer groupId, Integer memberId) {
		logger.debug("get Group Member for groupId " + groupId + " and memberId " + memberId);
		Group group = getGroup(groupId);
		List<GroupMember> members = group.getMembers();
		if (members != null && !members.isEmpty()) {
			for (GroupMember groupMember : members) {
				if (groupMember.getId().equals(memberId)) {
					logger.debug("Returning Group Member for groupId " + groupId + " and memberId " + memberId);
					return groupMember;
				}
			}
		}
		logger.debug("No Group Member for groupId " + groupId + " and memberId " + memberId);
		return null;
	}
	
	
	public GroupMember getGroupMember(Group group, Integer memberId) {
		logger.debug("get Group Member for groupId " + group.getId() + " and memberId " + memberId);
		List<GroupMember> members = group.getMembers();
		if (members != null && !members.isEmpty()) {
			for (GroupMember groupMember : members) {
				if (groupMember.getId().equals(memberId)) {
					logger.debug("Returning Group Member for groupId " + group.getId() + " and memberId " + memberId);
					return groupMember;
				}
			}
		}
		logger.debug("No Group Member for groupId " + group.getId() + " and memberId " + memberId);
		return null;
	}


	public Boolean isInGroup(Integer groupId, Integer memberId) {
		GroupMember member = getGroupMember(groupId, memberId);
		if (member == null) {
			return false;
		} else {
			return true;
		}
	}
	
	public Boolean isMemberInGroup(Group group, Integer memberId) {
		GroupMember member = getGroupMember(group, memberId);
		if (member == null) {
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	public void updateGroupMember(Integer groupId, GroupMember groupMember) {
		Group group = getGroup(groupId);
		logger.info("group members size, {}",group.getMembers().size());
		group.getMembers().add(groupMember);	
		logger.info("group members size, {}",group.getMembers().size());
		put(groupId, group);
	}

	@Override
	public void put(Integer groupId, Group group) {
		String value = jsonUtil.write(group);
		cacheUtil.put(groupBucketName, groupId, value);
	}
}
