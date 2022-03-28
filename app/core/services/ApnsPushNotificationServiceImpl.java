package core.services;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import core.entities.ClientCertificate;
import core.entities.Device;
import core.entities.PushNotification;
import core.utils.Constants;
import core.utils.Enums.DeviceType;
import core.utils.Enums.Env;
import core.utils.Enums.NotificationType;
import core.utils.PropertyUtil;
import javapns.Push;
import javapns.notification.PushNotificationBigPayload;
import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotification;
import javapns.notification.ResponsePacket;

@Service("ApnsPushNotification")
public class ApnsPushNotificationServiceImpl implements PushNotificationService, InitializingBean, DisposableBean {
	final static Logger logger = LoggerFactory.getLogger(ApnsPushNotificationServiceImpl.class);

	private static Boolean production = false;

	@Autowired
	private Environment env;

	@Autowired
	private DeviceService deviceService;

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	private String voIpNotificationBaseUri;

	private String apnsExpiration;

	private String apnsPushType;
	private String apnsPriority;
	private String apnsPushTypeForVoip;
	private String apnsPriorityForVoip;
	private static Long connectionRequestTimeout;



	public ApnsPushNotificationServiceImpl() {

	}

	@SuppressWarnings("unused")
	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			Boolean enableProxy = Boolean.valueOf(env.getProperty(Constants.ENABLE_HTTP_PROXY));
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

			voIpNotificationBaseUri = env.getProperty(Constants.VOIP_NOTIFICATION_BASE_URL);

			apnsExpiration = env.getProperty(Constants.APNS_EXPIRATION_VALUE, "0");
			apnsPushType    = env.getProperty(Constants.APNS_PUSH_TYPE_VALUE, "voip");
			apnsPriority = env.getProperty(Constants.APNS_PRIORITY_VALUE, "10");
			apnsPushTypeForVoip    = env.getProperty(Constants.APNS_PUSH_TYPE_VALUE_VOIP, "voip");
			apnsPriorityForVoip = env.getProperty(Constants.APNS_PRIORITY_VALUE_VOIP, "10");
			connectionRequestTimeout = Long.valueOf(env.getProperty(Constants.HTTP_CONNECTION_REQUEST_TIMEOUT));

			logger.info("apns queue created successfully ");
		} catch (Exception e) {
			logger.error("failed to create apns message queue", e);
			throw e;
		}
	}

	public void printResponse(List<PushedNotification> notifications) {
		ArrayList<Device> deviceTokensToBeDeleted = new ArrayList<>();
		for (PushedNotification notification : notifications) {
			if (notification.isSuccessful()) {
				logger.info("Push notification sent successfully to: " + notification.getDevice().getToken());
			} else {
				String invalidToken = notification.getDevice().getToken();
				logger.warn("Failed sending Push notification for token " + invalidToken);
				if (notification.getResponse() != null) {
					int statusCode = notification.getResponse().getStatus();
					if (statusCode == Constants.INVALID_TOKEN || statusCode == Constants.INVALID_TOKEN_SIZE) {
						Device device = new Device();
						device.setDeviceToken(invalidToken);
						device.setDeviceType(DeviceType.iOS);
						deviceTokensToBeDeleted.add(device);
					}
				}
				Exception theProblem = notification.getException();
				theProblem.printStackTrace();
				ResponsePacket theErrorResponse = notification.getResponse();
				if (theErrorResponse != null) {
					logger.error(theErrorResponse.getMessage());
				}
			}
		}
		if (!deviceTokensToBeDeleted.isEmpty()) {
			deviceService.deleteDeviceTokens(deviceTokensToBeDeleted);
			logger.info("Deleted invalid tokens after Failing to send Push notification");
		}
	}

	@Override
	public void send(PushNotification pushNotification) {
		logger.info("notification tye is: "+NotificationType.VideoCall.name());
		Boolean enableVideoCallPushNotification = Boolean.valueOf(PropertyUtil.getProperty(Constants.ENABLE_VIDEO_CALL_PUSH_NOTIFICATION, "false"));
		if( NotificationType.VideoCall.equals(pushNotification.getNotificationType()) && !enableVideoCallPushNotification) {
			logger.info("enableVideoCallPushNotification flag is {}, hence no need to send pushNotification ");
		} else {
			sendNotifictaion(pushNotification);	
		}
	}


	private void sendNotifictaion(PushNotification pushNotification) {
		logger.info("Send video call pushNotification started. ");
		for (Device device : pushNotification.getDevices()) {
			try {
				String url = voIpNotificationBaseUri;
				String bundleKey = createKey(device.getOrganizationId(), device.getClientId(), device.getBundleId());
				logger.info("Getting certificate for bundleKey: " + bundleKey);
				ClientCertificate cert = cacheService.getClientCertificate(bundleKey);
				String type = cert.getCertificateType();
				logger.info("Certificate Type : " + type);
				if (type != null && Env.prod.name().equalsIgnoreCase(type)) {
					production = true;
				}			
				HttpRequest request = null;
				String password= cert.getCertificatePassword();
				String certificatePath = cert.getCertificatePath();				
				logger.debug("certificate path: "+certificatePath);

				if(NotificationType.VideoCall.equals(pushNotification.getNotificationType())) {
					logger.info("device.getVoipToken(): "+device.getVoipToken());	
					url = url + device.getVoipToken();
					logger.info("url: "+url);					
					password = cert.getVoIpCertificatePassword();

					logger.info("voip certificate password: "+password);				
					logger.debug("voip certificate path: "+cert.getVoIpCertificatePath());			
					String bundleId = StringUtils.appendIfMissing(device.getBundleId(), ".voip"); 
					certificatePath = cert.getVoIpCertificatePath();					
					request = createVoipRequest(pushNotification, url, bundleId);
				} else {
					logger.info("device token: "+device.getDeviceToken());	
					url = url + device.getDeviceToken();
					logger.info("url: "+url);
					request = createHttpRequest(pushNotification, url, device.getBundleId());
				}				
				HttpClient httpClient = getHttpClient(password, certificatePath);
				HttpResponse<String> response = httpClient.send(request,
						HttpResponse.BodyHandlers.ofString());

				if (response.statusCode() == 200) {
					logger.info("Push notification sent successfully to: " + device.getVoipToken());
				} else if(response.statusCode() == 400){	
					ArrayList<Device> deviceTokensToBeDeleted = new ArrayList<>();
					logger.warn("Failed sending Push notification with exception: " + response.body());		
					logger.warn("Failed sending Push notification for token " + device.getDeviceToken());	
					if(response.body().equalsIgnoreCase(Constants.BAD_DEVICE_TOKEN)) {
						Device deleteDevice = new Device();
						deleteDevice.setDeviceToken(device.getDeviceToken());
						deleteDevice.setDeviceType(DeviceType.iOS);
						deviceTokensToBeDeleted.add(deleteDevice);							
					}
					if (!deviceTokensToBeDeleted.isEmpty()) {
						deviceService.deleteDeviceTokens(deviceTokensToBeDeleted);
						logger.info("Deleted invalid tokens after Failing to send Push notification");
					}						
				}
			} catch (Exception ex) {
				logger.error(" failed to send messages to device: " + device.getDeviceToken(), ex);
			}			
		}
	}

	private HttpRequest createVoipRequest(PushNotification pushNotification, String url, String bundleId) {
		logger.info("UUID.randomUUID().toString() "+UUID.randomUUID().toString());
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(Duration.ofMinutes(2))
				.header("Content-Type", "application/json")			        
				.POST(HttpRequest.BodyPublishers.ofString(pushNotification.getMessage()))
				.header(Constants.APNS_EXPIRATION, apnsExpiration)
				.header(Constants.APNS_PUSH_TYPE, apnsPushTypeForVoip)
				.header(Constants.APNS_ID, UUID.randomUUID().toString())
				.header(Constants.APNS_PRIORITY, apnsPriorityForVoip)
				.header(Constants.APNS_TOPIC, bundleId)				    
				.build();
		return request;
	}


	private HttpRequest createHttpRequest(PushNotification pushNotification, String url, String bundleId) {
		logger.info("UUID.randomUUID().toString() "+UUID.randomUUID().toString());
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(Duration.ofMinutes(2))
				.header("Content-Type", "application/json")			        
				.POST(HttpRequest.BodyPublishers.ofString(pushNotification.getMessage()))
				.header(Constants.APNS_EXPIRATION, apnsExpiration)
				.header(Constants.APNS_PUSH_TYPE, apnsPushType)
				.header(Constants.APNS_ID, UUID.randomUUID().toString())
				.header(Constants.APNS_PRIORITY, apnsPriority)
				.header(Constants.APNS_TOPIC, bundleId)				    
				.build();
		return request;
	}



	private String createKey(Integer orgId, String clientId, String bundleId) {
		String bundleKey = null;
		List<Integer> orgIds = cacheService.getBrandChatOrgs(clientId);
		if (orgIds.contains(orgId)) {
			bundleKey = ((orgId != null) ? (orgId + "_") : "") + clientId + "_"
					+ ((bundleId != null && !"".equals(bundleId.trim())) ? bundleId : "");
		} else {
			bundleKey = clientId + "_" + ((bundleId != null && !"".equals(bundleId.trim())) ? bundleId : "");
		}
		return bundleKey;
	}


	private static HttpClient getHttpClient(String password, String voipCertificate) {
		logger.info("Get HTTP client started. ");
		HttpClient HttpClient = null;

		try {

			FileInputStream certificate = new FileInputStream(voipCertificate);
			logger.info("File input Straem read successfully.");

			KeyStore ks = KeyStore.getInstance("PKCS12");
			ks.load(certificate, password.toCharArray());		

			logger.info("load certificate done.");
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, password.toCharArray());
			KeyManager[] keyManagers = kmf.getKeyManagers();
			SSLContext sslContext = SSLContext.getInstance("TLS");

			final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init((KeyStore) null);
			sslContext.init(keyManagers, tmf.getTrustManagers(), null);	    


			HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(connectionRequestTimeout/1000))
					.priority(1)
					.version(Version.HTTP_2)              
					.sslContext(sslContext)
					.build();
			logger.info("get http client done");
		} catch(Exception ex) {
			logger.error("Exception occured while sending http client: "+ex);
		}
		return HttpClient;	    

	}

	@Override
	public void destroy() throws Exception {

	}
}
