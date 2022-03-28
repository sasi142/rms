package core.services;

import core.entities.PushNotification;

public interface PushNotificationService {
	public void send(PushNotification pushNotification);
}
