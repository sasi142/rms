package core.daos;

import java.util.List;
import java.util.Set;

import core.entities.Group;
import core.entities.GroupMember;

public interface CacheGroupDao {
	public List<Integer> getGroupMembers(Integer groupId);	
	public List<Integer> getGroupMembers(Group group);
	public Set<Integer> getGroupMembersSet(Integer groupId);
	public Group getGroup(Integer id);
	public GroupMember getGroupMember(Integer groupId, Integer currentUserId);
	public GroupMember getGroupMember(Group group, Integer memberId);
	public Boolean isInGroup(Integer contactId, Integer id);
	public Boolean isMemberInGroup(Group group, Integer memberId);
	void put(Integer groupId, Group group);
	void updateGroupMember(Integer groupId, GroupMember groupMember);
}
