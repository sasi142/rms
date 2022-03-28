package core.daos.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import redis.clients.jedis.Jedis;
import utils.RmsApplicationContext;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import core.daos.DeviceDao;
import core.entities.Device;
import core.exceptions.ApplicationException;
import core.exceptions.InternalServerErrorException;
import core.exceptions.UnAuthorizedException;
import core.redis.RedisConnection;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;
import core.utils.Enums.DeviceType;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.NotificationType;
import core.utils.HttpConnectionManager;
import core.utils.PropertyUtil;

@Repository
public class DeviceDaoImpl implements DeviceDao, InitializingBean {
	
	final static Logger logger = LoggerFactory.getLogger(DeviceDaoImpl.class);
	
	private static String			imsGetDevicesUrl;
	@Autowired
	private HttpConnectionManager	httpConnectionManager;

	@Autowired
	private Environment				env;

	@Autowired
	public RedisConnection			redisConnection;

	private static String			bucketName;

	private Long					timeToMarkDeviceInactive;

	private Set<String>				chatAppClientIds;
	private Set<String>				workAppsAppClientIds;
	private Set<String>				memoAppClientIds;
	private String					deleteDeviceByTokenUrl;
	private Set<String>				webPushNotificationClientIds;

	public DeviceDaoImpl() {

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		imsGetDevicesUrl = env.getProperty(Constants.IMS_GETDEVICES_URL);
		logger.info("ims get devices url: " + imsGetDevicesUrl);

		deleteDeviceByTokenUrl = env.getProperty(Constants.IMS_DELETE_DEVICE_TOKEN_URL);
		logger.info("delete device token url: " + deleteDeviceByTokenUrl);
		bucketName = env.getProperty(Constants.REDIS_IMS_DEVICES_STORE);
		logger.info("ims device bucket name: " + bucketName);

		timeToMarkDeviceInactive = Long.valueOf(env.getProperty(Constants.TIME_TO_MARK_DEVICE_INACTIVE));
		logger.info("timeToMarkDeviceInactive: " + timeToMarkDeviceInactive);

		String chatClientIds = env.getProperty(Constants.CHAT_APP_CLIENT_IDS);
		String chatApps[] = chatClientIds.split(",");
		chatAppClientIds = new HashSet<>(Arrays.asList(chatApps));
		logger.info("chatAppClientIds: " + chatAppClientIds);

		String workAppsClientIds = env.getProperty(Constants.WORKAPPS_APP_CLIENT_IDS);
		String workappsApps[] = workAppsClientIds.split(",");
		workAppsAppClientIds = new HashSet<>(Arrays.asList(workappsApps));
		logger.info("workAppsClientIds: " + workAppsClientIds);

		String memoClientIds = env.getProperty(Constants.MEMO_APP_CLIENT_IDS);
		String memoApps[] = chatClientIds.split(",");
		memoAppClientIds = new HashSet<>(Arrays.asList(memoApps));
		logger.info("memoAppClientIds: " + memoAppClientIds);

		String webPushClientIds = env.getProperty(Constants.WEB_PUSH_NOTIFICATION_CLIENT_IDS);
		String webApps[] = webPushClientIds.split(",");
		webPushNotificationClientIds = new HashSet<>(Arrays.asList(webApps));
		logger.info("webPushNotificationClientIds: " + webPushNotificationClientIds);
	}

	@Override
	public List<Device> getDevices(List<Integer> recipients, NotificationType notificationType) {
		logger.debug("get Device List of recipients " + (recipients == null ? null : recipients.toString())
				+ " and notificationType = " + (notificationType == null ? null : notificationType.name()));
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		List<Device> list = new ArrayList<>();
		try {
			if (recipients != null && !recipients.isEmpty()) {
				for (Integer userId : recipients) {
					String jsonStr = jedis.hget(bucketName, String.valueOf(userId));
					logger.debug("Received Devieces jsonStr: " + jsonStr + " for user " + userId);
					if (jsonStr != null && !"".equalsIgnoreCase(jsonStr)) {
						ObjectMapper mapper = new ObjectMapper();
						ArrayNode node = (ArrayNode) mapper.readTree(jsonStr);
						if (node.isArray() && node.size() > 0) {
							for (int indx = 0; indx < node.size(); indx++) {
								JsonNode json = node.get(indx);
								String token = json.findPath("token").asText();
								String clientId = json.findPath("clientId").asText();
								if (ignoreDevice(clientId, notificationType)) {
									logger.debug("device is ignored due to clientId " + clientId
											+ " and notificationType " + (notificationType == null ? null : notificationType.name()));
									continue;
								}

								Long date = json.findPath("updatedDate").asLong();
								if ((System.currentTimeMillis() - date) > timeToMarkDeviceInactive) {
									logger.debug("device ignored due to input date " + date
											+ " creates date diff greater than " + timeToMarkDeviceInactive);
									continue; // ignore this device if last update is more than 15 days
								}

								Device device = new Device();
								
								device.setLastUpdatedDate(date);
								String type = json.findPath("type").asText();
								DeviceType deviceType = DeviceType.getDeviceType(type);
								if (deviceType.compareTo(DeviceType.NotSupported) != 0) {
									device.setDeviceType(DeviceType.valueOf(type));
								}								
								String bundleId = "";
								if (!json.findPath("bundleId").isMissingNode()) {
									bundleId = json.findPath("bundleId").asText();
								}
								String organizationId = null;
								if (!json.findPath("organizationId").isMissingNode()) {
									organizationId = json.findPath("organizationId").asText();
								}
								if (!json.findPath("deviceName").isMissingNode()) {
									String deviceName = json.findPath("deviceName").asText();
									device.setDeviceName(deviceName);
								}
								if (token != null || !"".equals(token) || !"null".equalsIgnoreCase(token)) {								
									logger.debug("token: " + token);
									if (DeviceType.WebBrowser.equals(deviceType)) {
										device.setDeviceToken(CommonUtil.unEscapeHtmlForWEB(token));
									} else {
										device.setDeviceToken(token);
									}
								}
								
								String voipToken = json.findPath("voipToken").asText();
								
								if (token != null || !"".equals(voipToken) || !"null".equalsIgnoreCase(voipToken)) {								
									logger.debug("voipToken: " + voipToken);								
										device.setVoipToken(voipToken);									
								}
								String vendor = "";
								if (!json.findPath("vendor").isMissingNode()) {
									vendor = json.findPath("vendor").asText();
								}
								device.setVendor(vendor);
								device.setUserId(userId);
								device.setClientId(clientId);
								device.setBundleId(bundleId);
								if (organizationId != null && !"null".equalsIgnoreCase(organizationId)) {
									device.setOrganizationId(Integer.parseInt(organizationId));
								}
								list.add(device);
								logger.debug("Added device with " + device.getBundleId() + " to list");
							}
						}
					}
				}
			}
			logger.info("Returning device list of size " + (list == null ? null : list.size()));
			return list;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, "failed to read devices from redis", ex);
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
	}

	private Boolean ignoreDevice(String clientId, NotificationType type) {
		logger.debug("ignoreDevice input clientId " + clientId + " and notificationType " + (type == null ? null : type.name()));
		if (type == NotificationType.WorkApps && workAppsAppClientIds.contains(clientId)) {
			return false;
		} else if (type == NotificationType.Chat && (chatAppClientIds.contains(clientId) || webPushNotificationClientIds.contains(clientId))) {
			return false;
		} else if (type == NotificationType.GroupChat && (chatAppClientIds.contains(clientId) || webPushNotificationClientIds.contains(clientId))) {
			return false;
		} else if (type == NotificationType.DeleteMessage && chatAppClientIds.contains(clientId)) {
			return false;
		} else if (type == NotificationType.Memo && memoAppClientIds.contains(clientId)) {
			return false;
		} else if (type == NotificationType.ChatGuestAdded && chatAppClientIds.contains(clientId)) {
			return false;
		} else if (type == NotificationType.VideoCall && chatAppClientIds.contains(clientId)) {
			return false;
		} else if ((type == NotificationType.VideoKycAgentAssigned || type == NotificationType.AddGroup) && chatAppClientIds.contains(clientId)) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public String deleteDeviceTokens(List<Device> devices) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		logger.info("Delete device tokens url:" + deleteDeviceByTokenUrl + " for device list of size "
				+ (devices == null ? null : devices.size()));
		CloseableHttpClient client = httpConnectionManager.getHttpClient();
		HttpPost httpPost = new HttpPost(deleteDeviceByTokenUrl);
		try {
			StringEntity requestEntity = new StringEntity(mapper.writeValueAsString(devices), ContentType.APPLICATION_JSON);
			httpPost.setEntity(requestEntity);
			String clientId = RmsApplicationContext.getInstance().getClientId();
			httpPost.addHeader(Constants.CLIENT_ID, clientId);
			String requestId = UUID.randomUUID().toString();
			httpPost.addHeader(Constants.X_REQUEST_ID, requestId);
			Long timestamp = System.currentTimeMillis();
			httpPost.addHeader(Constants.TIMESTAMP_STR, String.valueOf(timestamp));

			String imsApiKey = PropertyUtil.getProperty(Constants.IMS_API_KEY);
			httpPost.addHeader(Constants.API_KEY, imsApiKey);

			String apiTimePair = imsApiKey + ":" + timestamp;
			logger.debug("apiTimePair: " + apiTimePair);

			Mac imsSHA256_HMAC = getImsShaHmacInstance();
			String signature = Base64.encodeBase64String(imsSHA256_HMAC.doFinal(apiTimePair.getBytes()));
			httpPost.addHeader(Constants.API_SIGNATURE, signature);

			CloseableHttpResponse response = client.execute(httpPost);
			logger.info("remove device tokens ResponseCode:" + response.getStatusLine().getStatusCode());

			if (response.getStatusLine().getStatusCode() == 401) {
				throw new UnAuthorizedException(ErrorCode.InvalidToken, ErrorCode.InvalidToken.getName());
			} else if (response.getStatusLine().getStatusCode() >= 300) { // 200, 201, 202 are success responses
				throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, "Ims call failed");
			} else {
				logger.info("remove device tokens success");
			}

			StringBuffer result = new StringBuffer();
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			if (rd != null) {
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				rd.close(); // close the stream
			}
			if (response != null) {
				response.close();
			}
			logger.info("Delete device tokens url:" + deleteDeviceByTokenUrl + " Returned result as " + result);
			return result.toString();
		} catch (ApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			httpPost.releaseConnection();
		}
	}

	private Mac getImsShaHmacInstance() {
		try {
			String secret = (PropertyUtil.getProperty(Constants.IMS_API_SECRET)).trim();
			// logger.info("ims secret " + secret);
			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
			sha256_HMAC.init(secretKey);
			return sha256_HMAC;
		} catch (Exception ex) {
			logger.error("failed to get hmac instance for ims ", ex);
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, "failed to get hmac instance for ims", ex);
		}
	}
}
