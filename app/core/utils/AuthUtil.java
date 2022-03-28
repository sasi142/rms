package core.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.entities.ApiRequest;
import core.entities.User;
import core.entities.UserContext;
import core.exceptions.InternalServerErrorException;
import core.exceptions.UnAuthorizedException;
import core.services.CacheService;
import core.services.CommonService;
import core.utils.Enums.ClientType;
import core.utils.Enums.ErrorCode;
import play.mvc.Http;
import play.mvc.Http.Cookie;
import play.mvc.Http.Cookies;
import play.mvc.Http.Headers;
import play.mvc.Http.Request;
import utils.RmsApplicationContext;

@Component
public class AuthUtil implements InitializingBean {
	private static Logger logger = LoggerFactory.getLogger(AuthUtil.class);

	@Autowired
	private HttpConnectionManager httpConnectionManager;

	@Autowired
	private Environment env;

	@Autowired
	private CommonService commonService;

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	private Map<String, String> apiKeySecretMap = new HashMap<>();

	private static String desktopChatAppClientId = "107";

	public AuthUtil() {

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		apiKeySecretMap = getApiKeySecretList();
		desktopChatAppClientId = (env.getProperty(Constants.DESKTOP_CHAT_APP_CLIENT_ID)).trim();
		logger.info("desktopChatAppClientId : " + desktopChatAppClientId);
	}

	public UserContext checkApiAuth(Request request, String requestId) throws Exception {
		logger.info("checking ApiAuth for request : " + requestId);
		String apiKey = getApiKey(request);
		String clientId = getClientId(request);
		Long timestamp = getTimeStamp(request);
		String signature = getSignature(request);
		String apiTimePair = apiKey + ":" + timestamp;
		validateAPIRequest(apiTimePair, apiKey, signature);

		UserContext context = new UserContext();
		context.setClientId(clientId);
		context.setApiKey(apiKey);
		ThreadContext.setUsercontext(context);

		logger.info("checked ApiAuth for request : " + requestId);
		return context;
	}

	public UserContext checkUserAuth(Http.RequestHeader request, String requestId) throws Exception {
		String url = PropertyUtil.getProperty(Constants.IMS_VALIDATE_TOKEN_URL);
		logger.info("validate token url to create context : -" + url);

		String clientId = getClientId(request);
		String guid = getGuid(request);
		Integer versionId = getVersionId(request);
		logger.debug("received version Id " + versionId + " from clientId " + clientId);
		String tokenStr = getTokenStr(request, clientId);

		logger.debug("request Id " + requestId);

		String jsonResponse = executeRESTApi(url, tokenStr, clientId, requestId, request);
		logger.info("validate token response received");
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(jsonResponse);
		final JsonNode jsonUserId = json.findPath("userId");
		Integer userId = Integer.valueOf(jsonUserId.toString());
		logger.info("userId: " + userId);
		JsonNode token = json.findPath("token");
		String sessionId = token.findPath("sessionId").asText();
		logger.debug("sessionId: " + sessionId);

		// Check cross site request forgery check for Web client
		Boolean enableXsrfCheck = Boolean.valueOf((PropertyUtil.getProperty(Constants.ENABLE_XSRF_CHECK)).trim());
		if (enableXsrfCheck) {
			String httpMethod = request.method(); // bypass it for all GET requests
			logger.info("method: " + httpMethod);
			if ((ClientType.Web.getClientId().equalsIgnoreCase(clientId)
					|| ClientType.OpenChat.getClientId().equalsIgnoreCase(clientId))
					&& !"get".equalsIgnoreCase(httpMethod)) {
				CommonUtil.checkCSRFToken(request);
			}
		}

		String groupShortUrl = request.header(Constants.GROUP_SHORT_URI).isPresent() ? request.header(Constants.GROUP_SHORT_URI).get() : null;

		User user = cacheService.getUser(userId, false);
		UserContext context = new UserContext();
		context.setClientId(clientId);
		context.setVersionId(versionId);
		context.setToken(tokenStr);
		context.setRequestId(requestId);
		context.setUser(user);
		context.setWaguid(guid);

		if(groupShortUrl !=null) {
			context.setGroupShorturl(groupShortUrl);
		}
		context.setSessionId(sessionId);

		logger.info(
				"created user context for user [" + user.getId() + "|" + user.getName() + "|" + user.getEmail() + "]");

		CommonUtil.addUserInfoLoggingContext(context);
		return context;
	}

	private Integer getVersionId(Http.RequestHeader request) {
		Integer versionId = null;
		String versionIdStr = request.header(Constants.X_VERSION_ID).isPresent()
				? request.header(Constants.X_VERSION_ID).get()
						: null;
				versionId = CommonUtil.convertVersionToInteger(versionIdStr);
				return versionId;
	}

	private String cookie(Http.RequestHeader request, String name) {
		if (request.cookies() == null) {
			return null;
		}
		else{
			Http.Cookie cookie = request.cookie(name);
			if (cookie == null) {
				return null;
			}
			else {
				return cookie.value();
			}
		}
	}

	@SuppressWarnings("deprecation")
	private String getTokenStr(Http.RequestHeader request, String clientId) {
		String tokenStr = request.getQueryString(Constants.X_OPENCHAT_OWB_TOKEN);
		logger.info("Token found in query param {} with value {}", Constants.X_OPENCHAT_OWB_TOKEN, tokenStr);
		if (tokenStr == null || tokenStr.length() == 0) {
			String userId = getHeaderInformattion(request, Constants.REGISTER_USER_ID);
			String tokenName = getTokenName(clientId, userId);
			tokenStr = cookie(request, tokenName);
			logger.info("Token found in cookie for userId {}, tokenName {}, token {}", userId, tokenName, tokenStr);
		}
		if (tokenStr == null || tokenStr.trim().length() == 0){
			throw new UnAuthorizedException(ErrorCode.InvalidToken, ErrorCode.InvalidToken.getName());
		}
		return tokenStr;
	}

	private String getTokenName(String clientId, String userId) {
		if (clientId.equalsIgnoreCase(ClientType.DesktopChatApp.getClientId())) {
			return Constants.X_DESKTOP_OWB_TOKEN;
		} else if (clientId.equalsIgnoreCase(ClientType.OpenChat.getClientId())) {
			return Constants.X_OPENCHAT_OWB_TOKEN + "-" + userId;
		} else {
			return Constants.X_OWB_TOKEN;
		}
	}


	public String executeRESTApi(String url, String encodedToken, String clientId, String requestId,
			Http.RequestHeader request) throws Exception {
		CloseableHttpClient client = httpConnectionManager.getHttpClient();
		url = url + "?token=" + encodedToken + "&client_id=" + clientId;
		logger.debug("excuting REST Api  with url " + url);
		HttpGet httpget = new HttpGet(url);
		try {
			logger.debug("validate token using Ims URL: -" + url);
			httpget.addHeader(Constants.CLIENT_ID, RmsApplicationContext.getInstance().getClientId());
			httpget.addHeader(Constants.X_REQUEST_ID, requestId);

			//nuull check
			if(request != null) {
				httpget.addHeader(Constants.X_FORWARDED_FOR, getClientIP(request));
			}

			Long timestamp = System.currentTimeMillis();
			httpget.addHeader(Constants.TIMESTAMP_STR, String.valueOf(timestamp));

			String imsApiKey = PropertyUtil.getProperty(Constants.IMS_API_KEY);
			httpget.addHeader(Constants.API_KEY, imsApiKey);

			String apiTimePair = imsApiKey + ":" + timestamp;

			logger.info("created apiTimePair as  " + apiTimePair);

			Mac imsSHA256_HMAC = getImsShaHmacInstance();
			String signature = Base64.encodeBase64String(imsSHA256_HMAC.doFinal(apiTimePair.getBytes()));
			httpget.addHeader(Constants.API_SIGNATURE, signature);

			CloseableHttpResponse response = client.execute(httpget);
			logger.info("validate token ResponseCode:" + response.getStatusLine().getStatusCode());

			if (response.getStatusLine().getStatusCode() == 401) {
				throw new UnAuthorizedException(ErrorCode.InvalidToken, ErrorCode.InvalidToken.getName());
			} else if (response.getStatusLine().getStatusCode() >= 300) { // 200, 201, 202 are success responses
				throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, "Ims call failed");
			} else {
				logger.info("token is valid");
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
			logger.debug("excuted REST Api with url " + url);
			return result.toString();
		} finally {
			httpget.releaseConnection();
		}
	}

	private String getHeaderInformattion(Http.RequestHeader request, String headerInformation) {
		String header = request.getQueryString(headerInformation);
		if (header == null || "".equals(header.trim())) {
			header = request.header(headerInformation).isPresent() ? request.header(headerInformation).get() : null;
		}

		if (header == null || "".equals(header.trim())) {
			logger.warn("Missing " + headerInformation + " in request");
		}
		return header;
	}

	public String getClientId(Http.RequestHeader request) {
		String clientId = request.getQueryString(Constants.CLIENT_ID);
		if (clientId == null || "".equals(clientId.trim())) {
			clientId = request.header(Constants.CLIENT_ID).isPresent() ? request.header(Constants.CLIENT_ID).get()
					: null;
		}

		if (clientId == null || "".equals(clientId.trim())) {
			if (request.cookie(Constants.CLIENT_ID) != null) {
				clientId = request.cookie(Constants.CLIENT_ID).value();
			}
		}

		if (clientId == null || "".equals(clientId.trim())) {
			logger.error("Missing client id in request");
			throw new UnAuthorizedException(ErrorCode.Missing_Client_Id, "missing client id");
		}
		return clientId;
	}

	private String getGuid(Http.RequestHeader request) {
		String guid = request.getQueryString(Constants.GUID);
		if (guid == null || "".equals(guid.trim())) {
			guid = request.header(Constants.GUID).isPresent() ? request.header(Constants.GUID).get()
					: null;
		}

		if (guid == null || "".equals(guid.trim())) {
			if (request.cookie(Constants.GUID) != null) {
				guid = request.cookie(Constants.GUID).value();
			}
		}
		return guid;
	}

	private String getApiKey(Http.RequestHeader request) {
		String apiKey = request.header(Constants.API_KEY).isPresent() ? request.header(Constants.API_KEY).get() : null;
		logger.info("api Key" + apiKey);
		if (apiKey == null || "".equals(apiKey.trim())) {
			logger.error("Missing apiKey");
			throw new UnAuthorizedException(ErrorCode.Missing_Api_Key, "missing api key");
		} else if (apiKeySecretMap.get(apiKey) == null) {
			logger.error("apiKey not found in Rms");
			throw new UnAuthorizedException(ErrorCode.InvalidApiKey, "invalid api key");
		}
		return apiKey;
	}

	private Long getTimeStamp(Http.RequestHeader request) {
		String timestampStr = request.header(Constants.TIMESTAMP_STR).isPresent()
				? request.header(Constants.TIMESTAMP_STR).get()
						: null;
				Long timestamp = Long.valueOf(timestampStr);
				Long timeout = Long.valueOf((PropertyUtil.getProperty(Constants.API_REQUEST_TIMEOUT)).trim());
				logger.info("timestamp " + timestamp);
				if (timestampStr == null || "".equalsIgnoreCase(timestampStr)) {
					logger.error("missing timestamp");
					throw new UnAuthorizedException(ErrorCode.Missing_TimeStamp, "missing time stamp");
				} else if ((System.currentTimeMillis() - timestamp) > timeout) {
					logger.error("api request is expired");
					throw new UnAuthorizedException(ErrorCode.Invalid_TimeStamp, "invalid time stamp");
				}
				return timestamp;
	}

	private String getSignature(Http.RequestHeader request) {
		String signature = request.header(Constants.API_SIGNATURE).isPresent()
				? request.header(Constants.API_SIGNATURE).get()
						: null;
				logger.info("api signature" + signature);
				if (signature == null || "".equals(signature.trim())) {
					logger.error("Missing signature");
					throw new UnAuthorizedException(ErrorCode.Missing_Signature, "missing api signature");
				}
				return signature;
	}

	private void validateAPIRequest(String apiTimePair, String apiKey, String signature) {
		logger.info("validating API Request with params (api:timeStamp, apiKey, signature)" + apiTimePair + "," + apiKey + "," + signature);
		try {
			Mac sha256_HMAC = getShaHmacInstance(apiKey);
			String hash = Base64.encodeBase64String(sha256_HMAC.doFinal(apiTimePair.getBytes()));
			logger.info("api:timeStamp: " + apiTimePair);
			logger.info("signature: " + signature);
			logger.info("hash: " + hash);
			if (!hash.equals(signature)) {
				throw new UnAuthorizedException(ErrorCode.Unauthorized_Api_Access, "Unauthorized_Api_Access");
			}
		} catch (Exception ex) {
			throw new UnAuthorizedException(ErrorCode.Unauthorized_Api_Access, "Unauthorized_Api_Access");
		}
		logger.info("validated API Request with params (api:timeStamp, apiKey, signature)" + apiTimePair + "," + apiKey + "," + signature);
	}

	private Mac getShaHmacInstance(String apiKey) {
		try {
			String secret = apiKeySecretMap.get(apiKey);
			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
			sha256_HMAC.init(secretKey);
			return sha256_HMAC;
		} catch (Exception ex) {
			logger.error("failed to get hmac instance for ims ", ex);
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"error happened during request signing");
		}
	}

	private Mac getImsShaHmacInstance() {
		try {
			String secret = (PropertyUtil.getProperty(Constants.IMS_API_SECRET)).trim();
			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
			sha256_HMAC.init(secretKey);
			return sha256_HMAC;
		} catch (Exception ex) {
			logger.error("failed to get hmac instance for ims ", ex);
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"failed to get hmac instance for ims", ex);
		}
	}

	private Map<String, String> getApiKeySecretList() {
		Map<String, String> apiAuthURLList = new HashMap<String, String>();
		String keySecretList = (env.getProperty(Constants.APIS_KEY_SECRET_PAIR)).trim();
		//	String keySecretList = PropertyUtil.getProperty(Constants.APIS_KEY_SECRET_PAIR);
		String keySecretPairList[] = keySecretList.split(",");
		if (keySecretPairList != null && keySecretPairList.length > 0) {
			for (String keyPair : keySecretPairList) {
				String keyPairList[] = keyPair.split(":");
				apiAuthURLList.put(keyPairList[0], keyPairList[1]);
			}
		}
		return apiAuthURLList;
	}

	//@Async
	public void printRequest(Http.RequestHeader req, Integer diff, Integer userId, Integer status, String clientId,
			String requestId) {
		ApiRequest apiRequest = new ApiRequest(req.path(), userId, req.method(), System.currentTimeMillis(), diff,
				status.shortValue(), clientId, requestId, getClientIP(req), getServerIP());
		commonService.createApiRequest(apiRequest);
		logger.info("Created ApiRequest for user " + userId + " and req " + apiRequest.toString());
		Headers headers = req.getHeaders();	
		String headerStr = null;
		for (Entry<String, List<String>> entry: headers.toMap().entrySet()) {
			if (headerStr == null) {
				headerStr = entry.getKey()+":"+entry.getValue()+"#";
			}
			else {
				headerStr = headerStr+entry.getKey()+":"+entry.getValue()+"|";
			}
		}
		logger.info("headerStr: "+headerStr);

		/*
		Cookies cookies = req.cookies();
		String cookieStr = null;
		if (cookies != null) {
			Iterator<Cookie> cookieItr =  cookies.iterator();
			while (cookieItr.hasNext()) {
				Cookie cookie = cookieItr.next();
				if (cookieStr == null) {
					cookieStr="name:"+cookie.name()+",value:"+cookie.value()+",expiry:"+cookie.maxAge()+",path:"+cookie.path()+"#";
				}
				else {
					cookieStr=cookieStr+"name:"+cookie.name()+",value:"+cookie.value()+",expiry:"+cookie.maxAge()+"#";
				}
			}
		}

		logger.info("cookieStr: "+cookieStr);*/

		/*Map<String, String[]> queries = req.queryString();
		String queryStr = null;
		Set<Map.Entry<String, String[]>> set = queries.entrySet();
		for (Entry<String, String[]> entry: set) {
			if (queryStr == null) {
				queryStr = entry.getKey()+":"+entry.getValue()+",";
			}
			else {
				queryStr = queryStr+entry.getKey()+":"+entry.getValue()+",";
			}
		}
		logger.info("queryStr: "+queryStr); */
	}


	public String getClientIP(Http.RequestHeader request) {
		String ipAddress = request.header("X-FORWARDED-FOR").isPresent() ? request.header("X-FORWARDED-FOR").get()
				: null;
		logger.info("ClientIP" + ipAddress);
		if (ipAddress == null) {
			ipAddress = request.remoteAddress();
		}
		return ipAddress;
	}

	private String getServerIP() {
		String returnValue = null;
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			returnValue = localhost.getHostAddress();

		} catch (Exception ex) {
			logger.info("Could not read local server IP address" + ex.getMessage());
		}
		return returnValue;
	}

	public UserContext mockUserAuth(Http.RequestHeader request, String requestId) {

		String url = PropertyUtil.getProperty(Constants.IMS_VALIDATE_TOKEN_URL);
		logger.info("validate token url to create context : -" + url);

		String clientId = getClientId(request);
		String guid = getGuid(request);
		Integer versionId = getVersionId(request);
		logger.debug("received version Id " + versionId + " from clientId " + clientId);
	//	String tokenStr = getTokenStr(request, clientId);

		logger.debug("request Id " + requestId);

	//	Integer userId = 1;

		String groupShortUrl = request.header(Constants.GROUP_SHORT_URI).isPresent() ? request.header(Constants.GROUP_SHORT_URI).get() : null;

	//	User user = cacheService.getUser(userId, false);
		User user = new User();
		UserContext context = new UserContext();
		context.setClientId(clientId);
		context.setVersionId(versionId);
	//	context.setToken(tokenStr);
		user.setId(1);
		context.setRequestId(requestId);
		context.setUser(user);
		context.setWaguid(guid);

		if(groupShortUrl !=null) {
			context.setGroupShorturl(groupShortUrl);
		}
		String sessionId = "";
		context.setSessionId(sessionId);

		logger.info(
				"created user context for user [" + user.getId() + "|" + user.getName() + "|" + user.getEmail() + "]");

		CommonUtil.addUserInfoLoggingContext(context);
		return context;

	}
}
