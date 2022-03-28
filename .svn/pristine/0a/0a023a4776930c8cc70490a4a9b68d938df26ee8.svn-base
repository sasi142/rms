package core.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;


import com.google.android.gcm.server.Endpoint;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import core.entities.ClientCertificate;
import core.entities.Device;
import core.entities.PushNotification;
import core.utils.Constants;
import core.utils.Enums.ClientType;
import core.utils.Enums.DeviceType;
import core.utils.GCMProxySender;

@Service("GcmPushNotification")
@DependsOn("CacheClientCertificateDao")
public class GcmPushNotificationServiceImpl implements PushNotificationService, InitializingBean, DisposableBean {
	final static Logger logger = LoggerFactory.getLogger(GcmPushNotificationServiceImpl.class);
	
	private static ExecutorService				threadPool;

	@Autowired
	private Environment							env;

	@Autowired
	private DeviceService 							deviceService;
	
	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService							cacheService;
	
	public GcmPushNotificationServiceImpl() throws Exception {

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// thread size
		Integer size = Integer.valueOf(env.getProperty(Constants.GCM_THREAD_POOL_SIZE));
		threadPool = Executors.newFixedThreadPool(size);
		logger.info("GCM thread pool size : " + size);
	}

	@Override
	public void send(PushNotification pushNotification) {
		Message message = new Message.Builder().addData("message", pushNotification.getMessage()).build();

		asyncSend(message, pushNotification.getDevices());
	}

	private void asyncSend(final Message message, final List<Device> devices) {
		threadPool.execute(new Runnable() {
			public void run() {
				Result result = null;
				List<Device> devicesToBeDeleted = new ArrayList<>();

				for(Device device: devices){
					try {
						String bundleKey = createKey(device.getOrganizationId(),device.getClientId(), device.getBundleId());
						logger.info("Getting certificate for bundleKey: " + bundleKey);
						ClientCertificate cert = cacheService.getClientCertificate(bundleKey);
						Sender sender = getGCMSender(cert);
						result = sender.send(message, device.getDeviceToken(), 5); // 5 sec. is delay
						logger.info("Result: " + result.getErrorCodeName());
						validateResult(devicesToBeDeleted, device.getDeviceToken(), result);
					} catch (IOException e) {
						logger.error("GCM - failed to send message to device: " + device.getDeviceToken(), e);
					}
				}

				if (!devicesToBeDeleted.isEmpty()) {
					deviceService.deleteDeviceTokens(devicesToBeDeleted);
				}
			}

			private void validateResult(List<Device> devicesToBeDeleted,
					String regId, Result result) {
				String messageId = result.getMessageId();
				if (messageId != null) {
					logger.info("Succesfully sent message to device: " + regId + "; messageId = " + messageId);
					logger.info("Succesfully sent message to device: " + regId + "; messageId = " + messageId);
				} else {
					Device device = new Device();
					String error = result.getErrorCodeName();
					if (error.equalsIgnoreCase(Constants.ERROR_NOT_REGISTERED)) {
						logger.info("Unregistered device: " + regId);
						logger.info("Unregistered device: " + regId);
						device.setDeviceToken(regId);
						device.setDeviceType(DeviceType.Android);
						devicesToBeDeleted.add(device);
					} else if (error.equalsIgnoreCase(Constants.ERROR_INVALID_REGISTRATION)) {
						logger.info("Invalid registration token: " + regId);
						logger.info("Invalid registration token: " + regId);
						device.setDeviceToken(regId);
						device.setDeviceType(DeviceType.Android);
						devicesToBeDeleted.add(device);
					} else if (error.equalsIgnoreCase(Constants.ERROR_INVALID_APNS_CREDENTIAL)) {
						logger.info("Invalid Apns credential: " + regId);
						logger.info("Invalid Apns credential: " + regId);
						device.setDeviceToken(regId);
						device.setDeviceType(DeviceType.iOS);
						devicesToBeDeleted.add(device);
					} else {
						logger.info("Error sending message to " + regId + ": " + error);
						logger.error("Error sending message to " + regId + ": " + error);
					}
				}
			}
		});
	}

	@Override
	public void destroy() throws Exception {
		if (threadPool.isShutdown()) {
			threadPool.shutdown();
		}
	}

	private String createKey(Integer orgId, String clientId, String bundleId){
		String bundleKey = null;
		List<Integer> orgIds = cacheService.getBrandChatOrgs(clientId);
		if(orgIds.contains(orgId)){
			bundleKey = ((orgId != null)? (orgId + "_"):"") + clientId + "_" + ((bundleId != null && !"".equals(bundleId.trim())) ? bundleId:"");
		}else{
			bundleKey = clientId + "_" + ((bundleId != null && !"".equals(bundleId.trim())) ? bundleId:"");
		}
		return bundleKey;
	}
	
	private Sender getGCMSender(ClientCertificate cert){
		logger.info("inside getGCMSender");
		Sender sender = null;
		logger.info("Client Certificate : " + cert);
		logger.info("---------App key------: " + cert.getAppKey());
		Boolean enableProxy = Boolean.valueOf(env.getProperty(Constants.ENABLE_HTTP_PROXY));
		logger.info("enable Proxy value : " + enableProxy);
		if (enableProxy) {
			String httpProxyHost = env.getProperty(Constants.HTTP_PROXY_HOST);
			String httpProxyPort = env.getProperty(Constants.HTTP_PROXY_PORT);
			String httpProxyUsername = env.getProperty(Constants.HTTP_PROXY_USERNAME);
			String httpProxyPassword = env.getProperty(Constants.HTTP_PROXY_PASSWORD);
			logger.info("enable Proxy value : " + enableProxy);
			if (cert.getAppKey() != null && !"".equals(cert.getAppKey().trim())) {
				sender = new GCMProxySender(cert.getAppKey(), httpProxyHost, httpProxyPort, httpProxyUsername, httpProxyPassword);
				logger.info("httpProxyHost: "+httpProxyHost+" httpProxyPort: "+httpProxyPort+" httpProxyUsername: "+httpProxyUsername);
			}
		} else {
			if (cert.getAppKey() != null && !"".equals(cert.getAppKey().trim())) {
				if (ClientType.Android.getClientId().equals(cert.getClientId())) {
					logger.info("choosing GCM Endpoint : " + Endpoint.GCM.getUrl());
					sender = new Sender(cert.getAppKey(), Endpoint.GCM);
				} else {
					logger.info("choosing FCM Endpoint : " + Endpoint.FCM.getUrl());
					sender = new Sender(cert.getAppKey(), Endpoint.FCM);
				}
			}
		}
		logger.info("completing call getGCMSender() : returning sender = " + sender);
		return sender;
	}	
}
