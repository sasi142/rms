/**
 * 
 */
package core.entities;

import java.util.Set;

/**
 * @author Chandramohan.Murkute
 */
public class GroupContact extends Contact {
	private Byte			groupStatus;
	private Byte			groupType;
	private Integer			groupCreatedById;
	private Set<Contact>	groupMembers;
	private Integer			totalMembersCount;

	public Byte getGroupStatus() {
		return groupStatus;
	}

	public void setGroupStatus(Byte groupStatus) {
		this.groupStatus = groupStatus;
	}

	public Integer getGroupCreatedById() {
		return groupCreatedById;
	}

	public void setGroupCreatedById(Integer groupCreatedById) {
		this.groupCreatedById = groupCreatedById;
	}

	public Set<Contact> getGroupMembers() {
		return groupMembers;
	}

	public void setGroupMembers(Set<Contact> groupMembers) {
		this.groupMembers = groupMembers;
	}

	public Integer getTotalMembersCount() {
		return totalMembersCount;
	}

	public void setTotalMembersCount(Integer totalMembersCount) {
		this.totalMembersCount = totalMembersCount;
	}

	public Byte getGroupType() {
		return groupType;
	}

	public void setGroupType(Byte groupType) {
		this.groupType = groupType;
	}
}
