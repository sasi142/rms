package core.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class User {
	private Integer Id;
	private String name;
	private String timezone;
	private String thumbnailUrl;
	private Integer organizationId;
	private Byte userCategory;
	private UserPhoto photoURL;
	private String designation;
	private String department;
	private String subDepartment;
	private String office;
	private String mobile;
	private String email;
	private Boolean active;
	private String thumbnail;
	private String profile;
	private String roles;
	private String videoKycId;
	private  String userName;
	
	public Integer getId() {
		return Id;
	}

	public void setId(Integer id) {
		Id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public Integer getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(Integer organizationId) {
		this.organizationId = organizationId;
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

	public String getSubDepartment() {
		return subDepartment;
	}

	public void setSubDepartment(String subDepartment) {
		this.subDepartment = subDepartment;
	}

	public String getOffice() {
		return office;
	}

	public void setOffice(String office) {
		this.office = office;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Byte getUserCategory() {
		return userCategory;
	}

	public void setUserCategory(Byte userCategory) {
		this.userCategory = userCategory;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public UserPhoto getPhotoURL() {
		return photoURL;
	}

	public void setPhotoURL(UserPhoto photoURL) {
		this.photoURL = photoURL;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
		//TODO::Remove this as well as thumbnailUrl field from User Object ,when android update goes to Prod
		this.thumbnailUrl = thumbnail;
	}

	public String getProfile() {
		return profile;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	public String getRoles() {
		return roles;
	}

	public void setRoles(String roles) {
		this.roles = roles;
	}

	public String getVideoKycId() {
		return videoKycId;
	}

	public void setVideoKycId(String videoKycId) {
		this.videoKycId = videoKycId;
	}	

	public final String getUserName() {
		return userName;
	}

	public final void setUserName(String userName) {
		this.userName = userName;
	}

	@Override
	public String toString() {
		return "User [Id=" + Id + ", name=" + name + ", timezone=" + timezone + ", thumbnailUrl=" + thumbnailUrl
				+ ", organizationId=" + organizationId + ", userCategory=" + userCategory + ", photoURL=" + photoURL
				+ ", designation=" + designation + ", department=" + department + ", subDepartment=" + subDepartment
				+ ", office=" + office + ", mobile=" + mobile + ", email=" + email + ", active=" + active + "]";
	}
}
