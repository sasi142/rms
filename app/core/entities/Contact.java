package core.entities;

import java.util.List;
import java.util.Map;

import play.libs.Json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import core.utils.Enums.ContactType;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Contact {
	private String						name;
	private Integer						Id;
	private String						userStatus;
	private Presence					presence;
	private UserPhoto					photoURL;
	private String						mobileNumber;
	private String						landlineNumber;
	private Integer						contactType		= ContactType.Normal.getId();
	private Boolean						unreadMsg		= false;
	private ChatSummary					chatSummary;
	private String						userType;
	private Byte						chatType;
	private Byte						userCategory;

	private Byte						memberStatus;
	private Byte						memberRole;
	private String						leftDate;

	private List<Map<String, String>>	userPreferences	= null;
	private DeviceLocation				deviceLocation;
	private Boolean						isJourneyMapAccessible;
	private Boolean						isUserAttendanceAccessible;
	private Integer						orgId;
	private String						designation;
	private String						department;
	private String					email;

	public Contact() {

	}

	public Contact(String name, Integer id, String email, Byte memberStatus, String photoUrl, String designation, String departmentName, Integer contactType, Byte memberRole) {
		super();
		this.name = name;
		Id = id;
		if(email != null && !email.contentEquals("null")) {
			this.email = email;
		}		
		this.memberStatus = memberStatus;
		if (photoUrl != null) {
			this.photoURL = Json.fromJson(Json.parse(photoUrl), UserPhoto.class);
		}
		this.designation = designation;
		this.department = departmentName;
		this.contactType = contactType;
		this.memberRole = memberRole;
	}

	public Contact(Integer id, Byte chatType) {
		super();
		Id = id;
		this.chatType = chatType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@JsonProperty("id")
	public Integer getId() {
		return Id;
	}

	@JsonProperty("id")
	public void setId(Integer id) {
		Id = id;
	}

	public String getUserStatus() {
		return userStatus;
	}

	public void setUserStatus(String userStatus) {
		this.userStatus = userStatus;
	}

	public Presence getPresence() {
		return presence;
	}

	public void setPresence(Presence presence) {
		this.presence = presence;
	}

	public UserPhoto getPhotoURL() {
		return photoURL;
	}

	public void setPhotoURL(UserPhoto photoURL) {
		this.photoURL = photoURL;
	}

	public Integer getContactType() {
		return contactType;
	}

	public void setContactType(Integer contactType) {
		this.contactType = contactType;
	}

	public Boolean getUnreadMsg() {
		return unreadMsg;
	}

	public void setUnreadMsg(Boolean unreadMsg) {
		this.unreadMsg = unreadMsg;
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

	public ChatSummary getChatSummary() {
		return chatSummary;
	}

	public void setChatSummary(ChatSummary chatSummary) {
		this.chatSummary = chatSummary;
	}

	public String getUserType() {
		return userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
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

	public String getLeftDate() {
		return leftDate;
	}

	public void setLeftDate(String leftDate) {
		this.leftDate = leftDate;
	}

	public List<Map<String, String>> getUserPreferences() {
		return userPreferences;
	}

	public void setUserPreferences(List<Map<String, String>> preferences) {
		this.userPreferences = preferences;
	}

	public DeviceLocation getDeviceLocation() {
		return deviceLocation;
	}

	public void setDeviceLocation(DeviceLocation deviceLocation) {
		this.deviceLocation = deviceLocation;
	}

	public Boolean getIsJourneyMapAccessible() {
		return isJourneyMapAccessible;
	}

	public void setIsJourneyMapAccessible(Boolean isJouernyMapAccessible) {
		this.isJourneyMapAccessible = isJouernyMapAccessible;
	}

	public Integer getOrgId() {
		return orgId;
	}

	public void setOrgId(Integer orgId) {
		this.orgId = orgId;
	}

	public Boolean getIsUserAttendanceAccessible() {
		return isUserAttendanceAccessible;
	}

	public void setIsUserAttendanceAccessible(Boolean isUserAttendanceAccessible) {
		this.isUserAttendanceAccessible = isUserAttendanceAccessible;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Id == null) ? 0 : Id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Contact other = (Contact) obj;
		if (Id == null) {
			if (other.Id != null)
				return false;
		} else if (!Id.equals(other.Id))
			return false;
		return true;
	}

	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public Byte getUserCategory() {
		return userCategory;
	}

	public void setUserCategory(Byte userCategory) {
		this.userCategory = userCategory;
	}

	public Byte getMemberRole() {
		return memberRole;
	}

	public void setMemberRole(Byte memberRole) {
		this.memberRole = memberRole;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	
}