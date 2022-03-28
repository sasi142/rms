package core.services;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import core.entities.ChatMessage;
import core.entities.Notification;
import core.utils.Enums.NotificationType;
import core.utils.Enums.PushNotificationVisibility;

public interface NotificationService {
	public void sendNotifications(Notification notification);
	public void sendMobilePushNotification(List<Integer> recipients, JsonNode pushJson, NotificationType type);
	public void sendMobileNotification(int subtype, ChatMessage message, List<Integer> recipients, PushNotificationVisibility pushNotificationVisibility);
	public void sendMobileNotification(int subtype, Integer to, Integer from, String name, String text, String date,
			Long utcDate, Long mid, List<Integer> recipients, String parentMsg, Byte chatMessageType, PushNotificationVisibility pushNotificationVisibility);
	void sendVideoCallingPushNotification(int subtype, ChatMessage message, List<Integer> recipients,
			PushNotificationVisibility pushNotificationVisibility, JsonNode videoCallData);
}
