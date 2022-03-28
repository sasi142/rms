package core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import akka.actor.ActorRef;
import core.akka.utils.ActorCleanupUtil;
/*import com.ims.owb.core.exceptions.BadRequestException;
import com.ims.owb.core.utils.Constants;
import com.ims.owb.core.utils.PropertyUtil;
import com.ims.owb.core.utils.Enums.BulkUploadFileTypes;
 */
import core.akka.utils.AkkaUtil;
import core.entities.Attachment;
import core.entities.BulkMemoDump;
import core.entities.ChatMessage;
import core.entities.Contact;
import core.entities.Device;
import core.entities.DeviceLocation;
import core.entities.Memo;
import core.entities.Notification;
import core.entities.User;
import core.entities.UserContext;
import core.entities.UserPhoto;
import core.exceptions.InternalServerErrorException;
import core.exceptions.UnAuthorizedException;
import core.services.CacheService;
import core.services.UserConnectionService;
import core.utils.Enums.ChatType;
import core.utils.Enums.ClientType;
import core.utils.Enums.ConnectionType;
import core.utils.Enums.DeviceType;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.MessageType;
import io.jsonwebtoken.lang.Strings;
import messages.UserConnection;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Http.Cookie;
import utils.RmsApplicationContext;

@Component
public class CommonUtil implements InitializingBean {
	private static Logger logger = LoggerFactory.getLogger(CommonUtil.class);
	@Autowired
	private AkkaUtil akkaUtil;

	@Autowired
	private Environment env;

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	@Autowired
	private ActorCleanupUtil actorCleanupUtil;

	@Autowired
	UserConnectionService userConnectionService;

	private Set<String> mobileIconClientIds;
	private Set<String> mobileClients;
	private Set<String> groupChtSupportedClients;
	private Set<String> one2oneChtSupportedClients;
	private Set<String> videoChatSupportedClients;
	private Integer currentIOSVersion;
	private Map<Integer, String> orgChatAppNameMap = new ConcurrentHashMap<Integer, String>();
	private Boolean alwaysSendPushNotifications = false;

	@Override
	public void afterPropertiesSet() throws Exception {
		String mobileIconClientStr = env.getProperty(Constants.MOBILE_ICON_CLIENT_IDS);
		String ids[] = mobileIconClientStr.split(",");
		mobileIconClientIds = new HashSet<>(Arrays.asList(ids));
		logger.info(
				"showMobileIconClientIds property " + Constants.MOBILE_ICON_CLIENT_IDS + " = " + mobileIconClientIds);

		String mobileClientsStr = env.getProperty(Constants.CHAT_APP_CLIENT_IDS);
		String mobileIds[] = mobileClientsStr.split(",");
		mobileClients = new HashSet<>(Arrays.asList(mobileIds));
		logger.info("mobileClients: " + mobileClients);

		String groupChatClientStr = env.getProperty(Constants.GROUP_CHAT_SUPPORTED_CLIENTS);
		String groupChatClients[] = groupChatClientStr.split(",");
		groupChtSupportedClients = new HashSet<>(Arrays.asList(groupChatClients));
		logger.info("groupChtSupportedClients: " + groupChtSupportedClients);

		String one2oneChatClientsStr = env.getProperty(Constants.ONE_2_ONE_CHAT_SUPPORTED_CLIENTS);
		String one2oneChatClients[] = one2oneChatClientsStr.split(",");
		one2oneChtSupportedClients = new HashSet<>(Arrays.asList(one2oneChatClients));
		logger.info("one2oneChtSupportedClients: " + one2oneChtSupportedClients);

		String videoChatClientsStr = env.getProperty(Constants.VIDEO_CHAT_SUPPORTED_CLIENTS);
		String videoChatClients[] = videoChatClientsStr.split(",");
		videoChatSupportedClients = new HashSet<>(Arrays.asList(videoChatClients));
		logger.info("videoChatSupportedClients: " + videoChatSupportedClients);

		String currentIOSVersionStr = env.getProperty(Constants.VERSION_IOS);
		currentIOSVersion = CommonUtil.convertVersionToInteger(currentIOSVersionStr);

		alwaysSendPushNotifications = Boolean.valueOf(env.getProperty(Constants.ALWAYS_SEND_PUSH_NOTIFICATION));
	}

	public Boolean isMobileChatClient(String clientId) {
		logger.debug("input MobileChatClient " + clientId);
		if (mobileClients.contains(clientId)) {
			return true;
		}
		return false;
	}

	public static String getDateTimeWithTimeZone(Long timeMillis, String timeZone) {
		logger.debug("getDateTimeWithTimeZone for  (timeMillis, timeZone) = " + timeMillis + "," + timeZone);
		if (timeZone == null) {
			timeZone = DateTimeZone.UTC.toString();
		}
		TimeZone tz = TimeZone.getTimeZone(timeZone);
		String result = null;
		if (timeMillis != null) {
			DateTime date = new DateTime(timeMillis, DateTimeZone.forID(tz.getID()));
			result = date.toString();
		}
		logger.debug("getDateTimeWithTimeZone reulted as " + result);
		return result;
	}

	public static Long getDateTimeMillsWithTimeZone(Long timeMillis, String timeZone) {
		logger.debug("getDateTimeMillsWithTimeZone for  (timeMillis, timeZone) = " + timeMillis + "," + timeZone);
		if (timeZone == null) {
			timeZone = DateTimeZone.UTC.toString();
		}
		TimeZone tz = TimeZone.getTimeZone(timeZone);
		Long result = null;
		if (timeMillis != null) {
			DateTime date = new DateTime(timeMillis, DateTimeZone.forID(tz.getID()));
			result = date.getMillis();
		}
		logger.debug("getDateTimeMillsWithTimeZone reulted as " + result);
		return result;
	}

	public JsonNode getPushNotification(Notification noti) {
		logger.debug("In getPushNotification, input notification is " + noti);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode notification = Json.newObject();
		ObjectNode aps = Json.newObject();
		aps.put("content-available", "1");
		aps.put("alert", noti.getPlainText());
		aps.put("sound", "default");
		aps.put("category", "reply");
		Properties props = noti.getProperties();
		if (props != null) {
			String s1 = props.getProperty("Mute");
			Boolean mute = Boolean.valueOf(s1);
			if (!mute) {
				aps.put("content-available", "0");
			}
		}
		notification.set("aps", aps);
		JsonNode data;
		try {
			data = mapper.readTree(noti.getDataJsonString());
			notification.set("data", data);
			notification.put("type", MessageType.Notification.getId());
			notification.put("subtype", noti.getType().getId());
			logger.debug("created Notification as " + notification.toString());
		} catch (IOException e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to transform input notification to JsonNode", e);
		}
		return notification;

	}

	public Integer getDateDifferenceInDays(Long longDate, String timeZone) {
		logger.debug("getDateDifferenceInDays for  (longDate, timeZone) = " + longDate + "," + timeZone);
		Long systemDate = System.currentTimeMillis();
		Integer result = Integer.valueOf(0);
		try {
			if (longDate != null) {
				TimeZone tz = TimeZone.getTimeZone(timeZone);
				DateTime dateTimeSystem = new DateTime(systemDate, DateTimeZone.forID(tz.getID()));
				DateTime dateTimeDB = new DateTime(longDate, DateTimeZone.forID(tz.getID()));
				dateTimeSystem = dateTimeSystem.withTimeAtStartOfDay();
				dateTimeDB = dateTimeDB.withTimeAtStartOfDay();
				result = Days.daysBetween(dateTimeSystem, dateTimeDB).getDays();
			}
		} catch (Exception e) {
			logger.warn("getDateTimeWithTimeZone reulted in error ", e);
		}
		logger.debug("getDateTimeWithTimeZone reulted as " + result);
		return result;
	}

	public void cleanUpConnectionsOnShutdown() {
		actorCleanupUtil.cleanUpConnectionsOnShutdown();
	}

	public void cleanUpConnectionInfo() {
		logger.info("cleaning up old ws redis connections");
		actorCleanupUtil.cleanUpNonActiveConnections();
		logger.info("cleaned up old ws redis connections");
	}

	public void setUserDetails(Contact contact, JsonNode user) {
		logger.info("set user details in contact from jsonUser " + user.toString());
		contact.setName(user.findPath("firstName").asText());
		contact.setEmail(user.findPath("email").asText());
		logger.info("email: {} ",user.findPath("email").asText());
		JsonNode photoUrlNode = user.findPath("photoURL");
		if (photoUrlNode != null && !photoUrlNode.isMissingNode()) {
			ObjectMapper mapper = new ObjectMapper();
			UserPhoto photo = null;
			try {
				photo = mapper.readValue(photoUrlNode.asText(), UserPhoto.class);
				contact.setPhotoURL(photo);
			} catch (Exception e) {
				logger.error("failed to load photo object of user " + contact.getName(), e);
			}
		}
		logger.debug("set photoUrl details in contact from jsonUser as " + contact.getPhotoURL());

		// TODO: not sure why we setting customStatus in userStatus, removing this for
		// now.
		// if (customStatusNode != null && !customStatusNode.isMissingNode()) {
		// contact.setUserStatus(customStatusNode.asText());
		// }

		JsonNode prefs = user.findPath("userPreferences");
		if (prefs != null) {
			List<Map<String, String>> preferences = new ArrayList<Map<String, String>>();
			for (int count = 0; count < prefs.size(); count++) {
				JsonNode pref = prefs.get(count);
				String name = pref.findPath("name").asText();
				if (name != null && name.equalsIgnoreCase("CustomStatus")) {
					// contact.setUserStatus(pref.findPath("value").asText());
					Map<String, String> mapCustStatus = new HashMap<String, String>();
					mapCustStatus.put("name", "CustomStatus");
					mapCustStatus.put("value", pref.findPath("value").asText());
					preferences.add(mapCustStatus);
				}
				if (name != null && name.equalsIgnoreCase("ShareDeviceLocation")) {
					Map<String, String> mapTrackDev = new HashMap<String, String>();
					mapTrackDev.put("name", "ShareDeviceLocation");
					mapTrackDev.put("value", pref.findPath("value").asText());
					preferences.add(mapTrackDev);
				}
				if (name != null && name.equalsIgnoreCase("ShareJourneyMap")) {
					Map<String, String> mapShareJourney = new HashMap<String, String>();
					mapShareJourney.put("name", "ShareJourneyMap");
					mapShareJourney.put("value", pref.findPath("value").asText());
					preferences.add(mapShareJourney);
				}
				if (name != null && name.equalsIgnoreCase("ShareAttendanceRegister")) {
					Map<String, String> mapShareAttendance = new HashMap<String, String>();
					mapShareAttendance.put("name", "ShareAttendanceRegister");
					mapShareAttendance.put("value", pref.findPath("value").asText());
					preferences.add(mapShareAttendance);
				}
			}
			if (!preferences.isEmpty()) {
				contact.setUserPreferences(preferences);
			}
		}
		logger.debug("set userPreferences in contact" + contact.getUserPreferences());

		JsonNode mobileNumber = user.findPath("mobileNumber");
		if (mobileNumber != null && !mobileNumber.isMissingNode() && mobileNumber.asText() != null) {
			String mobileNumberValue = mobileNumber.asText().replace("#", "");
			contact.setMobileNumber(mobileNumberValue);
		} else {
			JsonNode chatOnlyMobileNumber = user.findPath("chatOnlyMobileNumber");
			if (chatOnlyMobileNumber != null && !chatOnlyMobileNumber.isMissingNode()
					&& chatOnlyMobileNumber.asText() != null) {
				String chatOnlyMobileNumberValue = chatOnlyMobileNumber.asText().replace("#", "");
				contact.setMobileNumber(chatOnlyMobileNumberValue);
			}
		}
		logger.debug("set mobileNumber in contact" + contact.getMobileNumber());

		JsonNode landlineNumber = user.findPath("landLineNumber");
		if (landlineNumber != null && !landlineNumber.isMissingNode() && landlineNumber.asText() != null) {
			String landlineNumberValue = landlineNumber.asText().replace("#", "");
			contact.setLandlineNumber(landlineNumberValue);
		}
		logger.debug("set landlineNumber in contact" + contact.getLandlineNumber());

		JsonNode userType = user.findPath("userType");
		if (userType != null && !userType.isMissingNode()) {
			contact.setUserType(userType.asText());
		}

		// set device location
		setUserLocation(contact, user);

		// set designation and departmentName need to show in contact list while
		// forwarding message
		JsonNode designation = user.findPath("designation");
		if (designation != null && !designation.isMissingNode() && designation.asText() != null) {
			contact.setDesignation(designation.asText());
		}
		logger.debug("set designation in contact" + contact.getDesignation());
		JsonNode department = user.findPath("departmentName");
		if (department != null && !department.isMissingNode() && department.asText() != null) {
			contact.setDepartment(department.asText());
		}
		logger.debug("set department in contact" + contact.getDepartment());
	}

	public void setCustomStatusPreference(Contact contact, JsonNode user) {
		JsonNode prefs = user.findPath("userPreferences");
		if (prefs != null) {
			List<Map<String, String>> preferences = new ArrayList<Map<String, String>>();
			for (int count = 0; count < prefs.size(); count++) {
				JsonNode pref = prefs.get(count);
				String name = pref.findPath("name").asText();
				if (name != null && name.equalsIgnoreCase("CustomStatus")) {

					Map<String, String> mapCustStatus = new HashMap<String, String>();
					mapCustStatus.put("name", "CustomStatus");
					mapCustStatus.put("value", pref.findPath("value").asText());
					preferences.add(mapCustStatus);
					break;
				}
			}
			if (!preferences.isEmpty()) {
				contact.setUserPreferences(preferences);
			}
		}
	}

	public void setUserInfo(Contact contact, JsonNode user) {
		logger.debug("set user info in contact from jsonUser " + user.toString());

		JsonNode prefs = user.findPath("userPreferences");
		if (prefs != null) {
			for (int count = 0; count < prefs.size(); count++) {
				JsonNode pref = prefs.get(count);
				String name = pref.findPath("name").asText();
				if (name != null && name.equalsIgnoreCase("CustomStatus")) {
					contact.setUserStatus(pref.findPath("value").asText());
				}
				if (name != null && name.equalsIgnoreCase("ShareDeviceLocation")) {
					if ("true".equalsIgnoreCase(pref.findPath("value").asText())) {
						setUserLocation(contact, user);
					}
				}
			}
		}
	}

	public void removeConnectionInfo(String ip) {
		logger.info("cleaning up the previous connection info of Ip: " + ip);
		CacheService cacheService = (CacheService) RmsApplicationContext.getInstance().getSpringContext()
				.getBean(Constants.CACHE_SERVICE_SPRING_BEAN);
		Map<String, ArrayNode> map = cacheService.getAll();
		if (map != null && !map.isEmpty()) {
			for (Entry<String, ArrayNode> entry : map.entrySet()) {
				String id = entry.getKey();
				ArrayNode arrayNode = entry.getValue();
				if (arrayNode != null && arrayNode.size() > 0) {
					for (JsonNode node : arrayNode) {
						String conIp = node.findPath("ip").asText();
						if (ip.equalsIgnoreCase(conIp)) {
							String uuid = node.findPath("uuid").asText();
							ActorRef ref = akkaUtil.getActor(node);
							if (ref == null) {
								logger.info("delete conns info- userId:" + id + ",uuid: " + uuid);
								cacheService.remove(Integer.valueOf(id), uuid);
							} else {
								logger.info("valid actor. so not removing the connection");
							}
						}
					}
				}
			}
		}
	}

	public Integer getConnectionType(String Id) {
		if (mobileIconClientIds.contains(Id)) {
			return ConnectionType.Mobile.getId();
		} else {
			return ConnectionType.Web.getId();
		}
	}

	@SuppressWarnings("rawtypes")
	public static String convertListToString(List ids) {
		String str = StringUtils.join(ids, ',');
		logger.info("userStr = " + str);
		if (str != null && str.endsWith(",")) {
			str = str.substring(0, str.lastIndexOf(','));
		}
		return str;
	}

	@SuppressWarnings("deprecation")
	public static String escapeHtml(String input) {
		String output = null;
		if (input != null && !input.isEmpty()) {
			output = org.apache.commons.lang3.StringEscapeUtils.escapeHtml3(input);

		} else {
			output = input;
		}
		return output;
	}

	/**
	 * Escapes the input only if the client Id of current request is WEB
	 * 
	 * @param input
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static String escapeHtmlForWEB(String input) {
		String output = input;

		String clientId = ClientType.Web.getClientId();
		if (ThreadContext.getUserContext() != null && ThreadContext.getUserContext().getClientId() != null) {
			clientId = ThreadContext.getUserContext().getClientId();
		}

		logger.info("clientId: " + clientId);

		if ((ClientType.Web.getClientId().equalsIgnoreCase(clientId)
				|| ClientType.OpenChat.getClientId().equalsIgnoreCase(clientId)) && input != null && !input.isEmpty()) {
			logger.info("excaping text of size: " + input.length());
			output = StringEscapeUtils.escapeHtml4(input);
			logger.info("excaping text: " + output);
		}
		return output;
	}

	@SuppressWarnings("deprecation")
	public static String unEscapeHtmlForWEB(String input) {
		String output = input;

		String clientId = ClientType.Web.getClientId();
		if (ThreadContext.getUserContext() != null && ThreadContext.getUserContext().getClientId() != null) {
			clientId = ThreadContext.getUserContext().getClientId();
		}

		if ((ClientType.Web.getClientId().equalsIgnoreCase(clientId)
				|| ClientType.OpenChat.getClientId().equalsIgnoreCase(clientId)) && input != null && !input.isEmpty()) {
			output = StringEscapeUtils.unescapeHtml4(input);
		}
		return output;
	}

	@SuppressWarnings("deprecation")
	public static String unEscapeHtmlForMobile(String input) {
		String output = input;

		String memoApps[] = PropertyUtil.getProperty(Constants.MEMO_APP_CLIENT_IDS).split(",");
		Set<String> memoAppClientIds = new HashSet<>(Arrays.asList(memoApps));

		String clientId = ClientType.Web.getClientId();
		if (ThreadContext.getUserContext() != null && ThreadContext.getUserContext().getClientId() != null) {
			clientId = ThreadContext.getUserContext().getClientId();
		}

		if (memoAppClientIds.contains(clientId) && input != null && !input.isEmpty()) {

			output = StringEscapeUtils.unescapeHtml4(input);
		}
		return output;
	}

	public static String formatDateWithTimeZone(Long timeMillis, String format, String timeZone) {
		DateTime date = null;
		if (timeZone == null) {
			timeZone = DateTimeZone.UTC.toString();
		}
		TimeZone tz = TimeZone.getTimeZone(timeZone);
		if (timeMillis != null) {
			// date = new DateTime(timeMillis, DateTimeZone.UTC);
			date = new DateTime(timeMillis, DateTimeZone.forID(tz.getID()));
		}
		DateTimeFormatter dateFormatter = DateTimeFormat.forPattern(format);
		String result = dateFormatter.print(date);
		return result;
	}

	public Set<String> getGroupChtSupportedClients() {
		logger.debug("groupChtSupportedClients: " + groupChtSupportedClients.toString());
		return new HashSet<>(groupChtSupportedClients);
	}

	public Set<String> getOne2oneChtSupportedClients() {
		logger.debug("one2oneChtSupportedClients: " + one2oneChtSupportedClients.toString());
		return new HashSet<>(one2oneChtSupportedClients);
	}

	public Set<String> getSupportedClientIdList() {
		logger.debug("videoChatSupportedClients: " + videoChatSupportedClients.toString());
		return new HashSet<>(videoChatSupportedClients);
	}

	public void setUserLocation(Contact contact, JsonNode user) {
		JsonNode deviceLocation = user.findPath("deviceLocation");
		if (deviceLocation != null && !deviceLocation.isMissingNode() && deviceLocation.size() > 0) {
			try {
				DeviceLocation lastLocation = Json.fromJson(deviceLocation, DeviceLocation.class);
				String timeZone = user.findPath("timezone").asText();
				lastLocation.setDayDiff(getDateDifferenceInDays(lastLocation.getCreatedDate(), timeZone));
				lastLocation.setDisplayDate(getDateTimeWithTimeZone(lastLocation.getCreatedDate(), timeZone));
				contact.setDeviceLocation(lastLocation);
				logger.debug("set lastLocation in contact" + contact.getDeviceLocation());
			} catch (Exception e) {
				logger.error("Failed to read the DeviceLocation of user " + contact.getId(), e);
			}
		}
	}

	public Boolean isUserNotRegistered(Integer userId) {
		logger.debug("Checking if user " + userId + " is not registered");
		boolean notRegistered = false;
		JsonNode userJson = cacheService.getUserJson(userId);
		if (userJson.findPath("lastLogin").isMissingNode()) {
			JsonNode userPrefs = userJson.findPath("userPreferences");
			if (userPrefs != null && userPrefs.isArray()) {
				for (JsonNode pref : userPrefs) {
					if ("CustomStatus".equalsIgnoreCase(pref.findPath("name").asText())) {
						String status = pref.findPath("value").asText();
						String notRegisteredStatus = PropertyUtil.getProperty(Constants.NOT_REGISTERED_CUSTOM_STATUS,
								"Not Registered");
						if (notRegisteredStatus.equalsIgnoreCase(status)) {
							notRegistered = true;
							break;
						}
					}
				}
			}
		}
		return notRegistered;
	}

	public String getUserCustomStatus(Integer userId) {
		logger.debug("get user custom status with userId: " + userId);
		String status = null;
		JsonNode userJson = cacheService.getUserJson(userId);
		JsonNode userPrefs = userJson.findPath("userPreferences");
		if (userPrefs != null && userPrefs.isArray()) {
			for (JsonNode pref : userPrefs) {
				if ("CustomStatus".equalsIgnoreCase(pref.findPath("name").asText())) {
					status = pref.findPath("value").asText();
					break;
				}
			}
		}
		return status;
	}

	@SuppressWarnings("deprecation")
	public String formatSystemMessage(Integer fromId, String fromName, Integer currentUserId, Integer currentUserOrgId,
			String toName) {
		String textMsg = null;
		String appName = null;
		Map<String, String> args = new HashMap<String, String>();
		if (fromId.equals(currentUserId)) {
			textMsg = PropertyUtil.getProperty(Constants.CHAT_MESSAGE_FOR_REGISTERED_USER);
			appName = getChatAppName(currentUserOrgId);
			args.put("Name", toName);
			args.put("AppName", appName);
		} else {
			textMsg = PropertyUtil.getProperty(Constants.CHAT_MESSAGE_FOR_NOT_REGISTERED_USER);
			args.put("Name", fromName);
		}

		String text = StrSubstitutor.replace(textMsg, args, "{", "}");
		return text;
	}

	public String getChatAppName(Integer orgId) {
		String chatAppName = PropertyUtil.getProperty(Constants.DEFAULT_APP_NAME);
		if (orgChatAppNameMap.containsKey(orgId)) {
			chatAppName = orgChatAppNameMap.get(orgId);
		} else {
			JsonNode orgJson = cacheService.getOrgJson(orgId);
			JsonNode prefs = orgJson.findPath("settings");
			if (prefs != null && prefs.isArray()) {
				for (JsonNode pref : prefs) {
					if ("ChatAppName".equalsIgnoreCase(pref.findPath("preference").asText())) {
						chatAppName = pref.findPath("value").asText();
						orgChatAppNameMap.put(orgId, chatAppName);
						break;
					}
				}
			}
		}
		return chatAppName;
	}

	@SuppressWarnings("deprecation")
	public static void checkCSRFToken(Http.RequestHeader request) {
		Optional<String> csrfHeader = request.header(Constants.X_COOKIE_CSRF_NAME);
		String xsrfToken = null;
		if (csrfHeader != null && csrfHeader.isPresent()) {
			xsrfToken = csrfHeader.get();
		} else {
			Map<String, String[]> params = request.queryString();
			String xsrfTokenStr[] = params.get(Constants.X_COOKIE_CSRF_NAME);
			if (xsrfTokenStr != null && xsrfTokenStr.length > 0) {
				xsrfToken = xsrfTokenStr[0];
			}
		}

		String xXsrfToken = "";

		// if not found, read it in cookie
		Cookie clientIdCookie = request.cookies().get(Constants.COOKIE_CSRF_NAME);
		xXsrfToken = clientIdCookie.value();
		if (xXsrfToken == null) {
			xXsrfToken = request.getQueryString(Constants.COOKIE_CSRF_NAME);
		}
		if (xsrfToken != null && xXsrfToken != null && !"".equals(xsrfToken) && !"".equals(xXsrfToken)
				&& xsrfToken.equals(xXsrfToken)) {
			logger.debug("csrf token is valid");
		} else {
			throw new UnAuthorizedException(ErrorCode.Invalid_XSRF_Token, "missing info");
		}
	}

	public static List<Device> getDevicesByType(List<Device> devices, DeviceType type) {
		List<Device> deviceList = new ArrayList<>();
		for (Device device : devices) {
			if (device.getDeviceType().equals(type)) {
				deviceList.add(device);
			}
		}
		return deviceList;
	}

	// FIX : for iOS old versions, before 3.1.0, set the contactType as 2 if it is 5
	// or 6, as it crashes on getting of 5/6
	public Integer overRideContactType(UserContext userContext, Integer contactType) {
		if (contactType == null){
			return null;
		}
		logger.debug("overRideContactType : input :: userContext.getVersionId() = " + userContext.getVersionId()
		+ " , contactType = " + contactType);
		if (ClientType.iOSChatApp.getClientId().equalsIgnoreCase(userContext.getClientId())
				&& (userContext.getVersionId() == null || userContext.getVersionId() < currentIOSVersion)
				&& (contactType == 5 || contactType == 6)) {
			contactType = 2;
		}
		logger.debug("overRideContactType : returning contactType = " + contactType);
		return contactType;
	}

	public static Integer convertVersionToInteger(String versionIdStr) {
		Integer versionId = null;
		String versionIdStrNoDot = null;
		try {
			if (versionIdStr != null && !versionIdStr.isEmpty()) {
				versionIdStrNoDot = versionIdStr.replace(".", "");
				versionId = Integer.parseInt(versionIdStrNoDot);
			}
		} catch (Exception e) {
			logger.error("failed parsing version string " + versionIdStr + " to integer version ");
		}
		return versionId;
	}

	// Converts seconds to "HH:mm:ss hrs" format
	public static String getDurationInHoursMinutesAndSeconds(Long durationSeconds) {
		Date d = new Date(durationSeconds * 1000L);
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss"); // HH for 0-23
		df.setTimeZone(TimeZone.getTimeZone("GMT")); // This is to ensure time start counting from 0
		String time = df.format(d);
		return time + " hrs";
	}

	public static List<String> getSupportedClientIds(ChatType chatType) {
		String[] supportedClients = null;
		if (ChatType.GroupChat.equals(chatType)) {
			supportedClients = PropertyUtil.getProperty(Constants.GROUP_CHAT_SUPPORTED_CLIENTS).split(",");
		} else if (ChatType.One2OneVideoChat.equals(chatType)) {
			supportedClients = PropertyUtil.getProperty(Constants.VIDEO_CHAT_SUPPORTED_CLIENTS).split(",");
		} else {
			supportedClients = PropertyUtil.getProperty(Constants.ONE_2_ONE_CHAT_SUPPORTED_CLIENTS).split(",");
		}
		List<String> clientIds = Arrays.asList(supportedClients);
		logger.debug("Supported clientIds = " + (clientIds == null ? null : clientIds.toString()));
		return clientIds;
	}

	public Boolean isAlwaysSendPushNofication(ChatMessage message, Set<String> msgReceiverClients) {
		Boolean sendPushNotification = false;
		logger.info("In isAlwaysSendPushNofication with alwaysSendPushNotifications_property = "
				+ alwaysSendPushNotifications + ", msg.subType=" + message.getSubtype() + " and msgReceiverClients = "
				+ msgReceiverClients.toString());
		// 27-09-2018 Now sending push notification for video invite also
		// if (!(message.getSubtype().byteValue() ==
		// ChatType.One2OneVideoChat.getId().byteValue())) {
		if (msgReceiverClients != null && !msgReceiverClients.isEmpty()) {
			if (alwaysSendPushNotifications && !(msgReceiverClients.contains(ClientType.AndroidChatApp.getClientId())
					|| msgReceiverClients.contains(ClientType.iOSChatApp.getClientId()))) {
				sendPushNotification = true;
			}
		} else {
			sendPushNotification = true;
		}
		// }
		return sendPushNotification;
	}

	public Boolean isAlwaysSendPushNofication(Set<String> msgReceiverClients) {
		Boolean sendPushNotification = false;
		logger.debug("In isAlwaysSendPushNofication with alwaysSendPushNotifications_property = "
				+ alwaysSendPushNotifications + " and msgReceiverClients = " + msgReceiverClients.toString());
		if (msgReceiverClients != null && !msgReceiverClients.isEmpty()) {
			if (alwaysSendPushNotifications && !(msgReceiverClients.contains(ClientType.AndroidChatApp.getClientId())
					|| msgReceiverClients.contains(ClientType.iOSChatApp.getClientId()))) {
				sendPushNotification = true;
			}
		} else {
			sendPushNotification = true;
		}
		return sendPushNotification;
	}

	public static String[] getImageFileExtension() {
		String[] fileExtensions = PropertyUtil.getProperty(Constants.IMAGE_FILE_EXTENSIONS).split(",");
		return fileExtensions;
	}

	public static String addDateToChatMessage(Long messageDate, String message, String timeZone) {
		String pattern = "dd MMM, hh:mma";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		messageDate = getDateTimeMillsWithTimeZone(messageDate, timeZone);
		String date = simpleDateFormat.format(new Date(messageDate));
		date = date.replace("AM", "am").replace("PM", "pm");
		message = message + " (" + date + ")";
		return message;
	}

	/***
	 * Screenshots are taken by the receiver on behalf of the caller (this was done
	 * to improve quality). The screenshot messages therefore is sent by the
	 * receiver and the user details need to be switch for it to look like its
	 * coming from the caller.
	 * 
	 * @param connection
	 * @param message
	 */
	public void switchUserForScreenshotMessage(UserConnection connection, ChatMessage message) {
		if (message.getSubtype().byteValue() == ChatType.One2One.getId().byteValue()) {
			Integer from = message.getFrom();
			message.setFrom(message.getTo());
			message.setTo(from);
			User toUser = cacheService.getUser(message.getFrom(), false);
			message.setName(toUser.getName());
		}

		// For group chats the sender will send the value of the RU
		if (message.getSubtype().byteValue() == ChatType.GroupChat.getId().byteValue()) {
			Integer from = null;
			JsonNode attachmentNode = message.getData().findPath("createdById");
			if (attachmentNode != null) {
				from = attachmentNode.asInt();
			}
			message.setFrom(from);
			User toUser = cacheService.getUser(message.getFrom(), false);
			message.setName(toUser.getName());
		}
	}

	public static List<Attachment> getAttachments(String attachmentDetails) {
		List<Attachment> attachments = new ArrayList<Attachment>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			Attachment[] atts = mapper.readValue(attachmentDetails, Attachment[].class);
			for (Attachment att : atts) {
				attachments.add(att);
			}
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return attachments;
	}

	public static String getMessagePublicUrl(Memo memo) {
		// create Memo url for SEO
		String seoPublicUrl = memo.getPublicURL();
		String channelCategory = memo.getChannelCategory().trim().replaceAll("\\s+", "-");
		String memoTitle = memo.getSubject().trim().replaceAll("\\s+", "-");

		logger.info(
				"memoTitle: " + memoTitle + "channelCategory:" + channelCategory + "messagePublicUrl:" + seoPublicUrl);

		if (Strings.hasText(seoPublicUrl) && Strings.hasText(channelCategory) && Strings.hasText(memoTitle)) {
			String seoUrlBaseString = PropertyUtil.getProperty(Constants.SEO_URL_BASE_STRING);
			try {
				channelCategory = URLEncoder.encode(channelCategory, PropertyUtil.getProperty(Constants.CHARSET));
				memoTitle = URLEncoder.encode(memoTitle, PropertyUtil.getProperty(Constants.CHARSET));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			seoPublicUrl = seoUrlBaseString + "/" + channelCategory + "/" + memoTitle + "-" + seoPublicUrl;
			logger.info("seoPublicUrl for Message: " + seoPublicUrl);

		}
		return seoPublicUrl;
	}

	public static String getReadableCreationDate(Long creationDate) {
		Date date = new Date(creationDate);
		SimpleDateFormat dateformat = new SimpleDateFormat("dd MMM, hh:mm aa");
		String msgdateTime = "<b>" + dateformat.format(date) + "</b>";
		return msgdateTime;
	}

	@SuppressWarnings("resource")
	public static List<ObjectNode> readCustomMemoXLFile(File file, Integer bulkMemoId) {
		logger.info("read xlsx file started");
				
		List<ObjectNode> memoList =new ArrayList<ObjectNode>();
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(file);
			XSSFWorkbook wb = new XSSFWorkbook(inputStream);
			XSSFSheet sheet = wb.getSheetAt(0);
			logger.info("sheet last roww Number is: " + sheet.getLastRowNum());		
			for (int index = 1; index <= sheet.getLastRowNum(); index++) {				
				XSSFRow row = sheet.getRow(index);
				logger.info("row: " + row);	
				if(row != null) {
				ObjectNode node = Json.newObject();	
				ObjectMapper mapper = new ObjectMapper();
				ArrayNode recipients =mapper.createArrayNode();
			//	List<String> recipients = new ArrayList<String>();				
				recipients.add(getCellValue(row.getCell(0)));				
				logger.info("recipients: " + recipients);		
				node.put("recipients", recipients.toString());
				node.put("subject", getCellValue(row.getCell(1)));
				node.put("message", getCellValue(row.getCell(2)));
				String snippet = getSnippet(getCellValue(row.getCell(2)));
				node.put("snippet", snippet);		
			    node.put("isPublic", ReturnIntegerValue(row.getCell(3)));
				node.put("id", bulkMemoId);	
				logger.info("node: " + node);		
				memoList.add(node);
				} else {
					logger.info("username value is blank. ");
					break;
				}
			}			
			logger.info("memo List: " + memoList);

		} catch (Exception e) {			
			logger.info("error is: " + e);
		}
		logger.debug(
				"file reading done. valid memoList size: " + memoList.size());
		return memoList;
	}
	
	public static ObjectNode readUserMemoXLFile(File file, BulkMemoDump memoDump) {
		logger.info("read xlsx file started");
		ObjectNode node = Json.newObject();	
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode recipients =mapper.createArrayNode();
		//	List<String> recipients = new ArrayList<String>();			
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(file);
			XSSFWorkbook wb = new XSSFWorkbook(inputStream);
			XSSFSheet sheet = wb.getSheetAt(0);				
			 for (Row row : sheet) {
				 XSSFCell cell =  (XSSFCell) row.getCell(0);
				 if(getCellValue(cell) != null) {
					 recipients.add(getCellValue(cell));
				 } else {
					 logger.info("username value is blank. ");
					break;
				 }
				 
			 }	
			 //Sender should be bydefault added in recipient list
			recipients.add(memoDump.getCreatedById());
			node.put("recipients", recipients.toString());
			node.put("subject", memoDump.getSubject());
			node.put("message", memoDump.getMessage());			
			node.put("snippet", memoDump.getSnippet());				
			node.put("attachmentIds", memoDump.getAttachments());
            Integer isPublic = (memoDump.getIsPublic()) ? 1 :0;
			node.put("isPublic", isPublic);
			node.put("id", memoDump.getId());	
			logger.info("node: " + node);	
			
		} catch (Exception e) {			
			logger.info("error is: " + e);
		}
		logger.debug("file reading done. valid usernameList: "+recipients.size());
		return node;
	}
	
	
	public static String getSnippet(String memoText) {
		String snippet = "";
		String snippetDelimiter = "...";
		Elements pTags = Jsoup.parse(memoText).select("p");
		for (Element pTag : pTags) {
			if (pTag != null && pTag.hasText()) {
				snippet = snippet + pTag.text();
				if (snippet.length() > 150) {
					snippet = snippet.substring(0, 150);
					break;
				}
			}
		}
		snippet = snippet + snippetDelimiter;
		logger.info("returning snippet");
		return snippet;
	}

	public static String getCellValue(XSSFCell cell) {			
		
		if (cell != null && cell.getCellTypeEnum() != CellType.BLANK ) {
			DataFormatter dataFormatter = new DataFormatter();
	        String value = dataFormatter.formatCellValue(cell);
	        logger.info("cell Value: " + value);	
	        value.trim();
	        return value;
		}
		 logger.info("cell Value: " + null);	
		return null;
	}

	private static Integer ReturnIntegerValue(XSSFCell cell) {
		Integer isPublic= 0;
		String sharingEnable = getCellValue(cell);
		logger.info("sharingEnable: "+sharingEnable);
		if (sharingEnable != null ) {
			if(sharingEnable.equalsIgnoreCase("On")) {
				isPublic = 1;
			}
		
		}
		return isPublic;
	}

	public static void setUserInfoLoggingContext(UserContext userContext) {
		logger.info("set UserInfo LoggingContext");
		if (userContext != null) {
			Integer userId = userContext.getUser().getId();
			String sessionId = userContext.getSessionId();
			logger.info("Logging userId:" + userId + " with sessionId:" + sessionId
					+ " to be added to each log statement for this user");
			if (userId != null && sessionId != null) {
				MDC.put("userId", userId.toString());
				MDC.put("UUID", sessionId);
			}
			logger.info("Logging context set for user with id:" + userId);
			userContext.setSessionId(sessionId);
			logger.debug("sessionId:" + sessionId + " set in userContext");
		} else {
			logger.error("******UserContext is null,Logs will miss UserInfo******");
		}
		logger.info("set UserInfo LoggingContext ends");
	}

	public static void addUserInfoLoggingContext(UserContext userContext) {
		logger.info("get UserInfo for logging context");
		if (userContext != null) {
			Map<String, String> userLoggingInfo = MDC.getCopyOfContextMap();
			logger.info("***Context:****" + userLoggingInfo);
			Integer currentUserId = userContext.getUser().getId();
			try {
				if (userLoggingInfo == null || !userLoggingInfo.containsKey(currentUserId)) {
					logger.debug("userInfo for logging context is absent...fetching info from UserCOntext");
					String sessionId = userContext.getSessionId();
					logger.debug("sessionId from UserCOntext" + sessionId);
					if (sessionId == null) {
						logger.debug(
								"sessionId is null in UserContext....making call to validateUserToken to get SessionId");
						CommonUtil commonUtil = new CommonUtil();
						String jsonResponse = commonUtil.validateToken(userContext);
						if (jsonResponse != null && !jsonResponse.isBlank()) {
							logger.debug("response from validateUserToken:" + jsonResponse);
							ObjectMapper mapper = new ObjectMapper();
							JsonNode json = mapper.readTree(jsonResponse);
							final JsonNode jsonUserId = json.findPath("userId");
							Integer userId = Integer.valueOf(jsonUserId.toString());
							logger.debug("userId: " + userId);
							JsonNode token = json.findPath("token");
							sessionId = token.findPath("sessionId").asText();
							logger.debug("sessionId: " + sessionId);
							userContext.setSessionId(sessionId);
							logger.debug("sessionId added to userContext");
						} else {
							logger.error("ValidateToken response cannot be null or empty");
							throw new InternalServerErrorException(ErrorCode.Invalid_Data,
									"ValidateToken response cannot be null or empty");
						}
					}
					setUserInfoLoggingContext(userContext);
				}
			} catch (Exception e) {
				logger.error(
						"******error while getting UserInfo for setting in LoggingContext,Logs will miss UserInfo******",
						e);
			}
			logger.info("add UserInfo LoggingContext ends");
		} else {
			logger.error("******UserContext is null,Logs will miss UserInfo******");
		}
	}

	private String validateToken(UserContext userContext) {
		return userConnectionService.validateUserToken(userContext);
	}

	public void cleanUpConnectionsOnStartup(String prevInstanceId) {
		actorCleanupUtil.cleanUpConnectionsOnStartup(prevInstanceId);
	}
}