package rms;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import play.Logger;
import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;

import core.entities.Device;
import core.entities.PushNotification;
import core.services.EventNotificationBuilder;
import core.services.PushNotificationService;
import core.utils.Enums.EventType;
import core.utils.Enums.MessageType;
import core.utils.Enums.NotificationType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jpa-config.xml", "classpath:application-context.xml" })
public class PushNotificationTest {

	@Autowired
	@Qualifier("GcmPushNotification")
	public PushNotificationService	pushNotificationService1;

	@Autowired
	@Qualifier("ApnsPushNotification")
	public PushNotificationService		pushNotificationService2;

	@Autowired
	private EventNotificationBuilder	eventNotificationBuilder;

	// @Autowired
	// @Qualifier("ApnsPushNotification")
	// public PushNotificationService pushNotificationService1;

	@Test
	public void defaultTestCase() {

		PushNotification pushNotification = new PushNotification();

		List<String> tokens = new ArrayList<>();
		// String token = "b4266a6dbedf2a840ae60402b0542742c5190c0429d42889c748b103a5fd21ea";
		String token = "fNgTZC7UqjM:APA91bH1IJ-C8-5ywss9wRGPk5z0surCP_LunFxkK4oL22TjRiXzLb5_iRmusOacbh5I2Cz_O8QyBsdevbX0Rm3P0lPnMh36pdG_ddUJQFQm_LVv3Y8zSYepCIe_vnYap69mBdAqXvhM";
		// tokens.add("6dc4b13236296b5a37fe9af3d3e6ff5932ce377cd93fdfaae1c8f52ff0f83e7f");
		List<Device> devices = new ArrayList<>();
		Device device = new Device();
		device.setDeviceToken(token);
		device.setClientId("108");
		device.setBundleId("");
		devices.add(device);

		pushNotification.setDevices(devices);
		pushNotification.setFrom(20);

		String message = getMobileNotification();

		pushNotification.setMessage(message);
		pushNotification.setNotificationType(NotificationType.Chat);

		pushNotificationService1.send(pushNotification);

		// This is must else local test doesnt work
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Logger.underlying().info("hello");
	}

	public String getMobileNotification() {

		ObjectNode pushNotification = Json.newObject();
		ObjectNode data = Json.newObject();
		data.put("type", MessageType.Notification.getId());
		data.put("subtype", 1);
		data.put("to", 2222);
		data.put("from", 2226);
		data.put("name", "Shankar");

		ObjectNode aps = Json.newObject();
		String msg = null;

		NotificationType notificationType = NotificationType.Chat;
		msg = "Shankar" + ": " + "Latest10";
		aps.put("alert", msg);
		pushNotification.put("aps", aps);

		pushNotification.put("data", data);
		Logger.underlying().info(pushNotification.toString());
		return pushNotification.toString();
	}

	// @Test
	public void testEventNotifications() {
		EventType eventType1 = EventType.ADSync_AddGroup;
		String data1 = "{\"eventType\":15,\"groupId\":8,\"groupName\":\"Grp1\"}";
		String msg1 = eventNotificationBuilder.buildMessage(eventType1, data1, 3);
		System.out.println("Message for EventType.ADSync_AddGroup = " + msg1);

		EventType eventType2 = EventType.ADSync_UpdateGroup_AddMember;
		String data2 = "{\"eventType\":16,\"groupId\":8,\"groupName\":\"Grp1\", \"affectedMemberId\":26}";
		String msg2 = eventNotificationBuilder.buildMessage(eventType2, data2, 3);
		System.out.println("Message for EventType.ADSync_UpdateGroup_AddMember = " + msg2);

		EventType eventType3 = EventType.ADSync_UpdateGroup_RemoveMember;
		String data3 = "{\"eventType\":17,\"groupId\":8,\"groupName\":\"Grp1\", \"affectedMemberId\":26}";
		String msg3 = eventNotificationBuilder.buildMessage(eventType3, data3, 3);
		System.out.println("Message for EventType.ADSync_UpdateGroup_RemoveMember = " + msg3);

		EventType eventType4 = EventType.ADSync_CloseGroup;
		String data4 = "{\"eventType\":18,\"groupId\":8,\"groupName\":\"Grp1\"}";
		String msg4 = eventNotificationBuilder.buildMessage(eventType4, data1, 4);
		System.out.println("Message for EventType.ADSync_CloseGroup = " + msg4);

		EventType eventType5 = EventType.ADSync_UpdateGroup_ChangeGroupName;
		String data5 = "{\"oldName\":\"Grp1\",\"newName\":\"Grp10\",\"eventType\":19,\"groupId\":8,\"groupName\":\"Grp10\"}";
		String msg5 = eventNotificationBuilder.buildMessage(eventType5, data5, 3);
		System.out.println("Message for EventType.ADSync_UpdateGroup_ChangeGroupName = " + msg5);
	}
}
