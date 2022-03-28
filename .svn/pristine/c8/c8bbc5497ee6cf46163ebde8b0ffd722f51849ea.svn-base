package core.services;

import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import core.entities.Device;
import core.entities.PushNotification;
import core.utils.Constants;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;

import play.libs.Json;

@Service("WebBrowserPushNotification")
public class WebBrowserPushNotificationServiceImpl implements PushNotificationService, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(WebBrowserPushNotificationServiceImpl.class);

	private static String vapidPublicKey;
	private static String vapidPrivateKey;

	@Autowired
	private Environment env;

	public WebBrowserPushNotificationServiceImpl() throws Exception {
	}

	@SuppressWarnings("unused")
	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			Boolean enableProxy = Boolean.valueOf(env.getProperty(Constants.ENABLE_HTTP_PROXY));

			vapidPublicKey = env.getProperty(Constants.WEB_PUSH_PUBLIC_KEY);
			vapidPrivateKey = env.getProperty(Constants.WEB_PUSH_PRIVATE_KEY);

			if (enableProxy) {
				String httpProxyHost = env.getProperty(Constants.HTTP_PROXY_HOST);
				String httpProxyPort = env.getProperty(Constants.HTTP_PROXY_PORT);
				String httpProxyUsername = env.getProperty(Constants.HTTP_PROXY_USERNAME);
				String httpProxyPassword = env.getProperty(Constants.HTTP_PROXY_PASSWORD);

				// TODO: Need to change javapns library
				// ProxyManager.setProxy(httpProxyHost, httpProxyPort);
				// ProxyManager.setProxyBasicAuthorization(httpProxyUsername,
				// httpProxyPassword);
			}
		} catch (Exception e) {
			logger.error("Exception occurred while setting up proxy in WebBrowserPushNotificationService", e);
			throw e;
		}
	}

	@Override
	public void send(PushNotification pushNotification) {
		logger.info("Sending web browser pushNotification : " + pushNotification);
		logger.info("Web Browser pns message : " + pushNotification.getMessage().getBytes());

		for (Device device : pushNotification.getDevices()) {
			try {
				logger.info("In web push notification");
				Security.addProvider(new BouncyCastleProvider());
				Map<String, String> subscriptionData = createSubscriptionMap(device.getDeviceToken());
				logger.info("web browser device token : " + device.getDeviceToken());
				if (subscriptionData != null && !subscriptionData.isEmpty()) {
					String endpoint = subscriptionData.get(Constants.SUBSCRIPTION_ENDPOINT);
					String userPublicKey = subscriptionData.get(Constants.SUBSCRIPTION_PUBLIC_ENCRYPTION_KEY);
					String userAuth = subscriptionData.get(Constants.SUBSCRIPTION_AUTH_SECRET);

					// Construct notification
					Notification notification = null;
					try {
						notification = new Notification(endpoint, userPublicKey, userAuth,
								pushNotification.getMessage().getBytes());
					} catch (Exception e) {
						logger.info(
								"Exception occurred while creating web browser push notification using third party");
						e.printStackTrace();
					}

					// Construct push service
					PushService pushService = new PushService();
					pushService.setPublicKey(Utils.loadPublicKey(vapidPublicKey));
					pushService.setPrivateKey(Utils.loadPrivateKey(vapidPrivateKey));
					logger.info("before response");
					// Send notification!
					HttpResponse httpResponse = null;
					try {
						httpResponse = pushService.send(notification);

						if (httpResponse.getStatusLine().getStatusCode() != 200) {
							logger.info("Failed sending web browser push notification for device : "
									+ device.getDeviceToken() + " with resonse code : "
									+ httpResponse.getStatusLine().getStatusCode());
						} else {
							logger.info("Successfully sent web browser push notification to device "
									+ device.getDeviceToken());
						}
					} catch (Exception e) {
						logger.info("Exception occurred while sending web browser push notification : "
								+ device.getDeviceToken());
						e.printStackTrace();
					}
					logger.info("after response");
				} else {
					logger.info(
							"Received invalid subscription data by web browser device : " + device.getDeviceToken());
				}
			} catch (Exception ex) {
				logger.error("webPush - failed to send messages to device: " + device.getDeviceToken(), ex);
			}
		}
		logger.info("End of Sending web browser pushNotification");
	}

	private Map<String, String> createSubscriptionMap(String jsonStr) {
		Map<String, String> data = new HashMap<String, String>();
		try {
			JsonNode node = Json.parse(jsonStr);
			String endpoint = node.findPath(Constants.SUBSCRIPTION_ENDPOINT).asText();
			String key = node.findPath(Constants.SUBSCRIPTION_PUBLIC_ENCRYPTION_KEY).asText();
			String auth = node.findPath(Constants.SUBSCRIPTION_AUTH_SECRET).asText();
			if (!endpoint.isEmpty() && !key.isEmpty() && !auth.isEmpty()) {
				data.put(Constants.SUBSCRIPTION_ENDPOINT, endpoint);
				data.put(Constants.SUBSCRIPTION_PUBLIC_ENCRYPTION_KEY, key);
				data.put(Constants.SUBSCRIPTION_AUTH_SECRET, auth);
			} else {
				logger.info("Received invalid subscription data by web browser device");
			}
		} catch (Exception ex) {
			logger.info("Exception occured while parsing subscription object received by we browser device : " + ex);
		}
		return data;
	}
}
