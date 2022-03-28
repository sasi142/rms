package core.entities;

import core.utils.Enums.DeviceType;

public class Device {
	private String		deviceToken;
	private DeviceType	deviceType;
	private Integer		userId;
	private Long		lastUpdatedDate;
	private String		clientId;
	private String		bundleId;
	private Integer		organizationId;
	private String		deviceName;
	private String		vendor;
	private String      voipToken;

	public Device() {

	}

	public String getDeviceToken() {
		return deviceToken;
	}

	public void setDeviceToken(String deviceToken) {
		this.deviceToken = deviceToken;
	}

	public DeviceType getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(DeviceType deviceType) {
		this.deviceType = deviceType;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Long getLastUpdatedDate() {
		return lastUpdatedDate;
	}

	public void setLastUpdatedDate(Long lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientId() {
		return clientId;
	}

	public String getBundleId() {
		return bundleId;
	}

	public void setBundleId(String bundleId) {
		this.bundleId = bundleId;
	}

	public Integer getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(Integer organizationId) {
		this.organizationId = organizationId;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}	

	public String getVoipToken() {
		return voipToken;
	}

	public void setVoipToken(String voipToken) {
		this.voipToken = voipToken;
	}

	@Override
	public String toString() {
		return deviceToken;
	}
}
