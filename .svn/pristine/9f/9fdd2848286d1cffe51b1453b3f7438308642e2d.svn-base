/**
 * 
 */
package core.entities;

import java.util.List;
import java.util.Map;

import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.annotation.JsonProperty;

import views.html.helper.input;

/**
 * @author Chandramohan.Murkute
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Group {
	@JsonProperty("groupId")
	private Integer					id;
	@JsonProperty("groupName")
	private String					name;
	private Integer					createdById;
	private Byte					groupStatus;
	private Byte					groupType;
	private Byte					deletedMemberStatus;
	@JsonProperty("totalMembersCount")
	private Integer					membersCount;
	private List<GroupMember>		members;
	private String					oldName;
	private String					newName;
	private Integer					affectedMemberId;
	private Integer					eventType;
	private Long					createdDate;
	private Integer                 actionTakerId;
	private Integer					guestUserId;
	private Integer					callDuration;
	private Integer					parentGroupId;
	private Integer					creatorOrganizationId;	
	private Boolean 				autoRecordingEnabled;
	private Integer 				videoCallMessageType;
	private Integer 				meetingId;
	private Byte					useCase;
	private String 					shortUri;
	private Byte 					useCaseStatus;
	private String                 excludedRecipients;
	private Integer					dataEncryptionKeyId;	
	private List<GroupPreference> preferences;
	
	@JsonProperty("groupChatHistory")
	private List<MergedGroupChat>	mergedChatList	= null;

	//"groupStatus", "membersCount", "members", "createdById", "id", "name"
	public Group() {
	}

	public Group(Integer groupId, List<MergedGroupChat> mergedChatList) {
		this.id = groupId;
		this.mergedChatList = mergedChatList;
	}

	/**
	 * Used by ChatHistory
	 */
	public Group(Group inputGroup) {
		this.id = inputGroup.id;
		this.name = inputGroup.name;
		this.createdById = inputGroup.createdById;
		this.oldName = inputGroup.oldName;
		this.newName = inputGroup.newName;
		this.affectedMemberId = inputGroup.affectedMemberId;
		this.actionTakerId = inputGroup.getActionTakerId();
		this.callDuration = inputGroup.getCallDuration();
		this.setAutoRecordingEnabled(inputGroup.getAutoRecordingEnabled());
		this.setVideoCallMessageType(inputGroup.getVideoCallMessageType());
		this.setMeetingId(inputGroup.getMeetingId());
		this.setExcludedRecipients(inputGroup.getExcludedRecipients());
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getCreatedById() {
		return createdById;
	}

	public void setCreatedById(Integer createdById) {
		this.createdById = createdById;
	}

	public Byte getGroupStatus() {
		return groupStatus;
	}

	public void setGroupStatus(Byte groupStatus) {
		this.groupStatus = groupStatus;
	}

	public Integer getMembersCount() {
		return membersCount;
	}

	public void setMembersCount(Integer membersCount) {
		this.membersCount = membersCount;
	}

	public List<GroupMember> getMembers() {
		return members;
	}

	public void setMembers(List<GroupMember> members) {
		this.members = members;
	}

	public List<MergedGroupChat> getMergedChatList() {
		return mergedChatList;
	}

	public void setMergedChatList(List<MergedGroupChat> mergedChatList) {
		this.mergedChatList = mergedChatList;
	}

	public String getOldName() {
		return oldName;
	}

	public void setOldName(String oldName) {
		this.oldName = oldName;
	}

	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	public Integer getAffectedMemberId() {
		return affectedMemberId;
	}

	public void setAffectedMemberId(Integer affectedMemberId) {
		this.affectedMemberId = affectedMemberId;
	}

	public Integer getEventType() {
		return eventType;
	}

	public void setEventType(Integer eventType) {
		this.eventType = eventType;
	}

	public Byte getDeletedMemberStatus() {
		return deletedMemberStatus;
	}

	public void setDeletedMemberStatus(Byte deletedMemberStatus) {
		this.deletedMemberStatus = deletedMemberStatus;
	}

	public Long getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}

	public Byte getGroupType() {
		return groupType;
	}

	public void setGroupType(Byte groupType) {
		this.groupType = groupType;
	}

	public Integer getActionTakerId() {
		return actionTakerId;
	}

	public void setActionTakerId(Integer actionTakerId) {
		this.actionTakerId = actionTakerId;
	}
	
	public Integer getGuestUserId() {
		return guestUserId;
	}

	public Integer getCallDuration() {
		return callDuration;
	}

	public void setCallDuration(Integer callDuration) {
		this.callDuration = callDuration;
	}

	public void setGuestUserId(Integer guestUserId) {
		this.guestUserId = guestUserId;
	}

	public Integer getParentGroupId() {
		return parentGroupId;
	}

	public void setParentGroupId(Integer parentGroupId) {
		this.parentGroupId = parentGroupId;
	}

	public Integer getCreatorOrganizationId() {
		return creatorOrganizationId;
	}

	public void setCreatorOrganizationId(Integer creatorOrganizationId) {
		this.creatorOrganizationId = creatorOrganizationId;
	}

	public Boolean getAutoRecordingEnabled() {
		return autoRecordingEnabled;
	}

	public void setAutoRecordingEnabled(Boolean autoRecordingEnabled) {
		this.autoRecordingEnabled = autoRecordingEnabled;
	}

	public Integer getVideoCallMessageType() {
		return videoCallMessageType;
	}

	public void setVideoCallMessageType(Integer videoCallMessageType) {
		this.videoCallMessageType = videoCallMessageType;
	}

	public Integer getMeetingId() {
		return meetingId;
	}

	public void setMeetingId(Integer meetingId) {
		this.meetingId = meetingId;
	}
	public Byte getUseCase() {
		return useCase;
	}

	public void setUseCase(Byte useCase) {
		this.useCase = useCase;
	}

	public String getShortUri() {
		return shortUri;
	}

	public void setShortUri(String shortUri) {
		this.shortUri = shortUri;
	}

	public Byte getUseCaseStatus() {
		return useCaseStatus;
	}

	public void setUseCaseStatus(Byte useCaseStatus) {
		this.useCaseStatus = useCaseStatus;
	}

	public String getExcludedRecipients() {
		return excludedRecipients;
	}

	public void setExcludedRecipients(String excludedRecipients) {
		this.excludedRecipients = excludedRecipients;
	}


	public Integer getDataEncryptionKeyId() {
		return dataEncryptionKeyId;
	}

	public void setDataEncryptionKeyId(Integer dataEncryptionKeyId) {
		this.dataEncryptionKeyId = dataEncryptionKeyId;
	}

	public List<GroupPreference> getPreferences() {
		return preferences;
	}

	public void setPreferences(List<GroupPreference> preferences) {
		this.preferences = preferences;
	}
	
	
}

