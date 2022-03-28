package core.entities;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import core.utils.Enums.ChatType;
import play.libs.Json;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatContact {
	private Integer Id;
	private Integer orgId;
	private String email;
	private String userType;
	private String name;
	private UserPhoto photoURL;
	private String designation;
	private String mobileNumber;
	private String landlineNumber;
	private Integer reportingManagerId;
	private String department;
	private String subDepartment;
	private Byte chatType;
	private Byte memberStatus;
	private Byte memberRole;
	private Byte groupStatus;
	private Byte groupType;
	private Integer groupCreatedById;
	private Set<Contact> groupMembers;
	private Boolean unreadMsg = false;
	private ChatSummary chatSummary;
	private List<Map<String, String>> userPreferences = null;
	private List<Map<String, String>> groupPreferences = null;
	private Boolean isJourneyMapAccessible;
	private Boolean isUserAttendanceAccessible;
	private Boolean active;
	@JsonIgnore
	private String						userPreferencesStr;
	@JsonIgnore
	private String						groupPreferencesStr;
	@JsonIgnore
	private String						photoURLStr;
	@JsonIgnore
	private String						groupMembersStr;

	private Integer						contactType;
	private Long						createdDate;
	private Long						updatedDate;
	private Presence					presence;
	private DeviceLocation				deviceLocation;
	private List<Device>				devices;
	private Byte						userCategory;
	private String						userStatus;

	private String						groupCreatorName;
	private String						parentGroupName;
	private Long						parentGroupId;
 	private Byte                        useCase;
 	private String                      shortUri;
 	private Byte                        useCaseStatus;
 	private Integer                     videoKycId;
 	private String                      videoKycFlow;
 	private String                      productName;
	private Integer meetingId;
	private Integer recordingId;
	private String                      trackingId;

	public ChatContact() {

	}

	public ChatContact(Integer id, Integer orgId, String email, String userType, String name, String photoURLStr,
			String designation, String mobileNumber, String landlineNumber, String department, String subDepartment,
			Byte chatType, Byte memberStatus, Byte groupStatus, Integer groupCreatedById, String groupMembersStr,
			Short unreadCount, String message, Integer lastMsgSenderId, String lastMsgSenderName,
			Long lastMessageDateTime, String userPreferencesStr, Boolean active, Integer reportingManagerId,
			String attachmentData, Integer contactType, Long lastMessageId, Long updatedDate, Long createdDate,
			Long ParentMsgId, String ParentMsg, Byte groupType, Byte memberRole, Byte userCategory, String userStatus) {
		super();
		Id = id;
		this.orgId = orgId;
		this.email = email;
		this.userType = userType;
		this.name = name;
		this.designation = designation;
		this.mobileNumber = mobileNumber;
		this.landlineNumber = landlineNumber;
		this.department = department;
		this.subDepartment = subDepartment;
		this.chatType = chatType;
		this.memberStatus = memberStatus;
		this.groupStatus = groupStatus;
		this.groupType = groupType;
		this.groupCreatedById = groupCreatedById;
		if (chatType == ChatType.One2One.getId()) {
			this.userPreferencesStr = userPreferencesStr;
		} else if (chatType == ChatType.GroupChat.getId()) {
			this.groupPreferencesStr = userPreferencesStr;
		}
		this.photoURLStr = photoURLStr;
		this.groupMembersStr = groupMembersStr;
		this.chatSummary = new ChatSummary(unreadCount, message, lastMsgSenderId, lastMsgSenderName,
				lastMessageDateTime, attachmentData, lastMessageId, ParentMsgId, ParentMsg);
		if (unreadCount != null && unreadCount.intValue() > 0) {
			this.unreadMsg = true;
		}
		this.active = active;
		this.reportingManagerId = reportingManagerId;
		this.contactType = contactType;
		this.createdDate = createdDate;
		this.updatedDate = updatedDate;
		this.memberRole = memberRole;
		this.userCategory = userCategory;
		this.userStatus = userStatus;

	}
	
	
	
	//getChatContactDetail uses this
	public ChatContact(Integer id, Integer orgId, String email, String userType, String name, String photoURLStr, String designation, String mobileNumber,
			String landlineNumber, String department, String subDepartment, Byte chatType, Byte memberStatus, Byte groupStatus, Integer groupCreatedById,
			String groupMembersStr, Short unreadCount, String message, Integer lastMsgSenderId, String lastMsgSenderName, Long lastMessageDateTime,
			String userPreferencesStr, Boolean active, Integer reportingManagerId, String attachmentData, Integer contactType, Long lastMessageId, 
			Long updatedDate, Long createdDate, Long ParentMsgId, String ParentMsg, Byte groupType, Byte memberRole, Byte userCategory, String userStatus, 
			String groupCreatorName, String parentGroupName, Long parentGroupId, Byte useCase, String shortUri, Byte useCaseStatus, Integer videoKycId,
			String videoKycFlow, String productName, String trackingId) {
		super();
		Id = id;
		this.orgId = orgId;
		this.email = email;
		this.userType = userType;
		this.name = name;
		this.designation = designation;
		this.mobileNumber = mobileNumber;
		this.landlineNumber = landlineNumber;
		this.department = department;
		this.subDepartment = subDepartment;
		this.chatType = chatType;
		this.memberStatus = memberStatus;
		this.groupStatus = groupStatus;
		this.groupType = groupType;
		this.groupCreatedById = groupCreatedById;
		if (chatType == ChatType.One2One.getId()) {
			this.userPreferencesStr = userPreferencesStr;
		} else if (chatType == ChatType.GroupChat.getId()) {
			this.groupPreferencesStr = userPreferencesStr;
		}
		this.photoURLStr = photoURLStr;
		this.groupMembersStr = groupMembersStr;
		this.chatSummary = new ChatSummary(unreadCount, message, lastMsgSenderId, lastMsgSenderName,
				lastMessageDateTime, attachmentData, lastMessageId, ParentMsgId, ParentMsg);
		if (unreadCount != null && unreadCount.intValue() > 0) {
			this.unreadMsg = true;
		}
		this.active = active;
		this.reportingManagerId = reportingManagerId;
		this.contactType = contactType;
		this.createdDate = createdDate;
		this.updatedDate = updatedDate;
		this.memberRole = memberRole;
		this.userCategory = userCategory;
		this.userStatus = userStatus;
		this.groupCreatorName = groupCreatorName;
		this.parentGroupName = parentGroupName;
		this.parentGroupId = parentGroupId;		
        this.useCase = useCase;
        this.shortUri = shortUri;
        this.useCaseStatus = useCaseStatus;
        this.videoKycId = videoKycId;
        this.videoKycFlow = videoKycFlow;
        this.productName=productName;
        this.trackingId=trackingId;
	}	
	
	// GetContactsForSyncV2
	public ChatContact(Integer id, String userType, String name, Byte chatType, Integer contactType, Long createdDate,
			Boolean active, Byte groupStatus, Byte memberStatus, Long lastMessageId, String message,
			Integer lastMsgSenderId, Long lastMessageDateTime, Long readDate, String designation, String department,
			String photo, String mobileNumber, String landlineNumber, Integer groupCreatedById, Byte groupType,
			Byte memberRole) {
		Id = id;
		this.userType = userType;
		this.name = name;
		this.chatType = chatType;
		this.createdDate = createdDate;
		this.active = active;
		this.contactType = contactType;
		this.chatSummary = new ChatSummary(message, lastMsgSenderId, lastMessageDateTime, lastMessageId, readDate);
		this.memberStatus = memberStatus;
		this.memberRole = memberRole;
		this.groupStatus = groupStatus;
		this.groupType = groupType;
		this.groupCreatedById = groupCreatedById;
		this.designation = designation;
		this.department = department;
		this.unreadMsg = null;
		this.mobileNumber = mobileNumber;
		this.landlineNumber = landlineNumber;
		if (photo != null && !photo.isEmpty()) {
			this.setPhotoURL(Json.fromJson(Json.parse(photo), UserPhoto.class));
		}
		this.memberRole = memberRole;
	}

	public Integer getId() {
		return Id;
	}

	public void setId(Integer id) {
		Id = id;
	}

	public Integer getOrgId() {
		return orgId;
	}

	public void setOrgId(Integer orgId) {
		this.orgId = orgId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUserType() {
		return userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UserPhoto getPhotoURL() {
		return photoURL;
	}

	public void setPhotoURL(UserPhoto photoURL) {
		this.photoURL = photoURL;
	}

	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public String getLandlineNumber() {
		return landlineNumber;
	}

	public void setLandlineNumber(String landlineNumber) {
		this.landlineNumber = landlineNumber;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getSubDepartment() {
		return subDepartment;
	}

	public void setSubDepartment(String subDepartment) {
		this.subDepartment = subDepartment;
	}

	public Byte getChatType() {
		return chatType;
	}

	public void setChatType(Byte chatType) {
		this.chatType = chatType;
	}

	public Byte getMemberStatus() {
		return memberStatus;
	}

	public void setMemberStatus(Byte memberStatus) {
		this.memberStatus = memberStatus;
	}

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

	public Boolean getUnreadMsg() {
		return unreadMsg;
	}

	public void setUnreadMsg(Boolean unreadMsg) {
		this.unreadMsg = unreadMsg;
	}

	public ChatSummary getChatSummary() {
		return chatSummary;
	}

	public void setChatSummary(ChatSummary chatSummary) {
		this.chatSummary = chatSummary;
	}

	public List<Map<String, String>> getUserPreferences() {
		return userPreferences;
	}

	public void setUserPreferences(List<Map<String, String>> userPreferences) {
		this.userPreferences = userPreferences;
	}

	public Boolean getIsJourneyMapAccessible() {
		return isJourneyMapAccessible;
	}

	public void setIsJourneyMapAccessible(Boolean isJourneyMapAccessible) {
		this.isJourneyMapAccessible = isJourneyMapAccessible;
	}

	public Boolean getIsUserAttendanceAccessible() {
		return isUserAttendanceAccessible;
	}

	public void setIsUserAttendanceAccessible(Boolean isUserAttendanceAccessible) {
		this.isUserAttendanceAccessible = isUserAttendanceAccessible;
	}

	public String getUserPreferencesStr() {
		return userPreferencesStr;
	}

	public void setUserPreferencesStr(String userPreferencesStr) {
		this.userPreferencesStr = userPreferencesStr;
	}

	public String getPhotoURLStr() {
		return photoURLStr;
	}

	public void setPhotoURLStr(String photoURLStr) {
		this.photoURLStr = photoURLStr;
	}

	public String getGroupMembersStr() {
		return groupMembersStr;
	}

	public void setGroupMembersStr(String groupMembersStr) {
		this.groupMembersStr = groupMembersStr;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Integer getReportingManagerId() {
		return reportingManagerId;
	}

	public void setReportingManagerId(Integer reportingManagerId) {
		this.reportingManagerId = reportingManagerId;
	}

	public Integer getContactType() {
		return contactType;
	}

	public void setContactType(Integer contactType) {
		this.contactType = contactType;
	}

	public Long getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(Long updatedDate) {
		this.updatedDate = updatedDate;
	}

	public Long getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}

	public Presence getPresence() {
		return presence;
	}

	public void setPresence(Presence presence) {
		this.presence = presence;
	}

	public DeviceLocation getDeviceLocation() {
		return deviceLocation;
	}

	public void setDeviceLocation(DeviceLocation deviceLocation) {
		this.deviceLocation = deviceLocation;
	}

	public Byte getGroupType() {
		return groupType;
	}

	public void setGroupType(Byte groupType) {
		this.groupType = groupType;
	}

	public Byte getMemberRole() {
		return memberRole;
	}

	public void setMemberRole(Byte memberRole) {
		this.memberRole = memberRole;
	}

	public List<Device> getDevices() {
		return devices;
	}

	public void setDevices(List<Device> devices) {
		this.devices = devices;
	}

	public Byte getUserCategory() {
		return userCategory;
	}

	public void setUserCategory(Byte userCategory) {
		this.userCategory = userCategory;
	}

	public String getUserStatus() {
		return userStatus;
	}

	public void setUserStatus(String userStatus) {
		this.userStatus = userStatus;
	}

	public List<Map<String, String>> getGroupPreferences() {
		return groupPreferences;
	}

	public void setGroupPreferences(List<Map<String, String>> groupPreferences) {
		this.groupPreferences = groupPreferences;
	}

	public String getGroupPreferencesStr() {
		return groupPreferencesStr;
	}

	public void setGroupPreferencesStr(String groupPreferencesStr) {
		this.groupPreferencesStr = groupPreferencesStr;
	}

	public String getGroupCreatorName() {
		return groupCreatorName;
	}

	public void setGroupCreatorName(String groupCreatorName) {
		this.groupCreatorName = groupCreatorName;
	}

	public String getParentGroupName() {
		return parentGroupName;
	}

	public void setParentGroupName(String parentGroupName) {
		this.parentGroupName = parentGroupName;
	}

	public Long getParentGroupId() {
		return parentGroupId;
	}

	public void setParentGroupId(Long parentGroupId) {
		this.parentGroupId = parentGroupId;
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

	public Integer getVideoKycId() {
		return videoKycId;
	}

	public void setVideoKycId(Integer videoKycId) {
		this.videoKycId = videoKycId;
	}

	public String getVideoKycFlow() {
		return videoKycFlow;
	}

	public void setVideoKycFlow(String videoKycFlow) {
		this.videoKycFlow = videoKycFlow;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public void setMeetingId(Integer meetingId) {
		this.meetingId = meetingId;
	}

	public Integer getMeetingId() {
		return meetingId;
	}

	public void setRecordingId(Integer recordingId) {
		this.recordingId = recordingId;
	}

	public Integer getRecordingId() {
		return recordingId;
	}

	public String getTrackingId() {
		return trackingId;
	}

	public void setTrackingId(String trackingId) {
		this.trackingId = trackingId;
	}
	
	
}
