package core.entities;

import java.util.List;

import core.utils.Enums.NotificationType;

public class PushNotification {
	private Integer from;
	private String message;		
	private List<Device> devices;
	private NotificationType notificationType;
	public PushNotification() {

	}		
	public PushNotification(String message, List<Device> devices, NotificationType notificationType) {
		super();
		this.message = message;
		this.devices = devices;
		this.notificationType = notificationType;
	}
	public Integer getFrom() {
		return from;
	}
	public void setFrom(Integer from) {
		this.from = from;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public List<Device> getDevices() {
		return devices;
	}
	public void setDevices(List<Device> devices) {
		this.devices = devices;
	}
	public NotificationType getNotificationType() {
		return notificationType;
	}
	public void setNotificationType(NotificationType notificationType) {
		this.notificationType = notificationType;
	}	
}
