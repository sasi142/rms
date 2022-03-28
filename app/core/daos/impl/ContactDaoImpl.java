package core.daos.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.*;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.CallableStatementCreatorFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import core.daos.ContactDao;
import core.entities.ChatContact;
import core.entities.Contact;
import core.entities.GroupPreference;
import core.entities.UserContext;
import core.entities.UserPhoto;
import core.exceptions.ApplicationException;
import core.exceptions.InternalServerErrorException;
import core.exceptions.ResourceNotFoundException;
import core.utils.Constants;
import core.utils.Enums.ChatType;
import core.utils.Enums.ClientType;
import core.utils.Enums.ErrorCode;
import core.utils.HttpConnectionManager;
import core.utils.ThreadContext;
import play.libs.Json;

@Repository
public class ContactDaoImpl implements InitializingBean, ContactDao {

	final static Logger logger = LoggerFactory.getLogger(ContactDaoImpl.class);

	@Autowired
	private HttpConnectionManager	httpConnectionManager;

	@Autowired
	private Environment				env;

	@PersistenceContext
	protected EntityManager			entityManager;
	
	@PersistenceContext(unitName = "readEntityManagerFactory")
	protected EntityManager			readEntityManager;

	private String					contactUrl;
	private String					contactChatUrl;
	private String					contactIdChatUrl;

	@Override
	public void afterPropertiesSet() throws Exception {
		contactUrl = env.getProperty(Constants.GET_CONTACTS_URL);
		logger.info("Get contact url is " + contactUrl);

		contactChatUrl = env.getProperty(Constants.GET_CHAT_CONTACTS_URL);
		logger.info("Get chat contact url is " + contactChatUrl);

		contactIdChatUrl = env.getProperty(Constants.GET_CHAT_CONTACT_By_ID_URL);
		logger.info("Get chat contact url is " + contactIdChatUrl);
	}

	@Override
	public List<Contact> getChatContacts(UserContext userContext, Integer offset, Integer limit) {
		List<Contact> contacts = new ArrayList<>();
		try {
			Integer contactId = null;
			logger.info("Getting chat contacts from Ims");
			String contactStr = getChatContactsFromIms(userContext, offset, limit, "", null, null);
			logger.debug("Received chat contacts from Ims as " + contactStr);
			if (contactStr != null && !"".equalsIgnoreCase(contactStr)) {
				ObjectMapper mapper = new ObjectMapper();
				ArrayNode node = (ArrayNode) mapper.readTree(contactStr);
				if (node.isArray() && node.size() > 0) {
					for (int indx = 0; indx < node.size(); indx++) {
						JsonNode json = node.get(indx);
						Contact contact = new Contact();
						contact.setName(json.findPath("firstName").asText());
						contactId = json.findPath("id").asInt();
						contact.setId(contactId);
						contact.setContactType(json.findPath("contactType").asInt());
						JsonNode chatTypeNode = json.findPath("chatType");
						if (chatTypeNode != null && !chatTypeNode.isMissingNode()) {
							contact.setChatType(Byte.valueOf(chatTypeNode.asText()));
						} else {
							contact.setChatType(ChatType.One2One.getId());
						}
						logger.debug("Set chat contact type as " + contact.getChatType() + " for contact " + contactId);

						JsonNode photoUrlNode = json.findPath("photoURL");
						if (photoUrlNode != null && !photoUrlNode.isMissingNode()) {
							UserPhoto photo = new UserPhoto();
							JsonNode profile = photoUrlNode.findPath("profile");
							photo.setProfile(profile.asText());
							JsonNode thumbnail = photoUrlNode.findPath("thumbnail");
							photo.setThumbnail(thumbnail.asText());
							contact.setPhotoURL(photo);
						}
						logger.debug("Set photoUrl as " + photoUrlNode + " for contact " + contactId);

						JsonNode userStatusNode = json.findPath("userStatus");
						if (userStatusNode != null && !userStatusNode.isMissingNode()) {
							contact.setUserStatus(userStatusNode.asText());
						}
						logger.debug("Set userStatusNode as " + userStatusNode + " for contact " + contactId);

						JsonNode prefs = json.findPath("userPreferences");
						if (prefs != null && prefs.isArray()) {
							for (int count = 0; count < prefs.size(); count++) {
								JsonNode pref = prefs.get(count);
								String name = pref.findPath("name").asText();
								if (name != null && name.equalsIgnoreCase("CustomStatus")) {
									contact.setUserStatus(pref.findPath("value").asText());
									break;
								}
							}
						}
						logger.debug("Set prefs as " + prefs + " for contact " + contactId);

						contacts.add(contact);
						logger.debug("Added contact " + contactId + " to return list");
					}
				}
			}
			logger.info("Returning chat contacts List of size " + (contacts == null ? null : contacts.size()));
		} catch (ApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		}

		return contacts;
	}

	@Override
	public List<Contact> getContacts(UserContext userContext, Integer offset, Integer limit, String searchKey) {
		List<Contact> contacts = new ArrayList<>();
		try {
			Integer contactId = null;
			logger.info("Getting contacts from Ims");
			String contactStr = getContactsFromIms(userContext, offset, limit, searchKey);
			logger.debug("Received contacts from Ims as " + contactStr);
			if (contactStr != null && !"".equalsIgnoreCase(contactStr)) {
				ObjectMapper mapper = new ObjectMapper();
				ArrayNode node = (ArrayNode) mapper.readTree(contactStr);

				if (node.isArray() && node.size() > 0) {
					for (int indx = 0; indx < node.size(); indx++) {
						JsonNode json = node.get(indx);
						Contact contact = new Contact();
						contactId = json.findPath("id").asInt();
						contact.setName(json.findPath("name").asText());
						contact.setId(json.findPath("userId").asInt());

						// set ChatType as one2One
						contact.setChatType(ChatType.One2One.getId());
						logger.debug("Set chat contact type as " + contact.getChatType() + " for user " + contactId);

						JsonNode userDetail = json.findPath("userDetail");
						if (userDetail != null && !userDetail.isMissingNode()) {
							JsonNode photoURL = userDetail.findPath("photoURL");
							if (photoURL != null && !photoURL.isMissingNode()) {
								UserPhoto photo = new UserPhoto();
								JsonNode profile = photoURL.findPath("profile");
								photo.setProfile(profile.asText());
								JsonNode thumbnail = photoURL.findPath("thumbnail");
								photo.setThumbnail(thumbnail.asText());
								contact.setPhotoURL(photo);
							}
						}
						logger.debug("Set userDetail as " + userDetail + " for contact " + contactId);

						JsonNode prefs = json.findPath("userPreferences");
						if (prefs != null && prefs.isArray()) {
							for (int count = 0; count < prefs.size(); count++) {
								JsonNode pref = prefs.get(count);
								String name = pref.findPath("name").asText();
								if (name != null && name.equalsIgnoreCase("CustomStatus")) {
									contact.setUserStatus(pref.findPath("value").asText());
									break;
								}
							}
						}
						logger.debug("Set prefs as " + prefs + " for contact " + contactId);
						contacts.add(contact);
					}
				}
			}
		} catch (ApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		}

		return contacts;
	}

	@Override
	public String getChatContactsFromIms(UserContext userContext, Integer offset, Integer limit, String searchKey, Integer contactId, Integer inputChatType) {
		logger.info("getting Chat Contacts From Ims for input searchKey " + searchKey
				+ " ,contactId " + contactId + " ,inputChatType" + inputChatType);
		if (searchKey != null) {// encode search key as user may input space or special characters
			try {
				searchKey = URLEncoder.encode(searchKey, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		String url = "";
		if (contactId == null && inputChatType == null) {
			String clientId = ThreadContext.getUserContext().getClientId();
			Boolean isMobileOrdering = false;
			if (ClientType.AndroidChatApp.getClientId().equalsIgnoreCase(clientId) || ClientType.iOSChatApp.getClientId().equalsIgnoreCase(clientId)) {
				isMobileOrdering = true;
			}
			url = contactChatUrl;
			url = MessageFormat.format(url, String.valueOf(userContext.getUser().getId()), String.valueOf(offset), String.valueOf(limit), searchKey,
					isMobileOrdering);
		} else {
			url = contactIdChatUrl;
			url = MessageFormat.format(url, String.valueOf(userContext.getUser().getId()), String.valueOf(contactId), String.valueOf(inputChatType));
		}
		logger.info("getting Chat Contacts From Ims using formatted contact url : " + url);

		CloseableHttpClient client = httpConnectionManager.getHttpClient();
		HttpGet httpget = new HttpGet(url);
		try {
			httpget.addHeader(Constants.CLIENT_ID, userContext.getClientId());
			httpget.addHeader(Constants.X_REQUEST_ID, userContext.getRequestId());
			httpget.addHeader(Constants.X_OWB_TOKEN, userContext.getToken());
			httpget.addHeader(Constants.X_FORWARDED_FOR, userContext.getClientIPAddress());
			logger.debug("httpget headers:"+httpget.toString());
			CloseableHttpResponse response = client.execute(httpget);
			logger.info("validate token ResponseCode : " + response.getStatusLine().getStatusCode());
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName());
			}

			int cnt = 0;
			StringBuffer result = new StringBuffer();
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			if (rd != null) {
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
					cnt++;
				}
				rd.close(); // close the stream
			}
			if (response != null) {
				response.close();
			}
			logger.info("Returned Chat Contacts From Ims of size " + cnt);
			logger.debug("Returned Chat Contacts From Ims as " + result);
			return result.toString();
		} catch (ApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			httpget.releaseConnection();
		}
	}

	@Override
	@Transactional(rollbackFor = { Exception.class })
	public List<ChatContact> getChatContacts(UserContext userContext, Integer offset, Integer limit, String syncDates, Boolean isFirstTimeSync) {
		logger.info("get Chat Contacts with input offset, limit, syncDates, isFirstTimeSync"
				+ offset + "," + limit + "," + syncDates + "," + isFirstTimeSync);
		List<SqlParameter> params = new ArrayList<>();
		params.add(new SqlParameter("P_OrganizationId", Types.INTEGER));
		params.add(new SqlParameter("P_UserId", Types.INTEGER));
		params.add(new SqlParameter("P_SyncDates", Types.VARCHAR));
		params.add(new SqlParameter("P_IsFirstTimeSync", Types.BIT));
		params.add(new SqlParameter("P_Offset", Types.INTEGER));
		params.add(new SqlParameter("P_Count", Types.INTEGER));
		params.add(new SqlParameter("P_CountOnly", Types.BIT));

		params.add(new SqlReturnResultSet("ChatContact", new RowMapper<ChatContact>() {
			@Override
			public ChatContact mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				ChatContact contact = new ChatContact(
						rs.getInt(1), ((rs.getString(2) != null) ? rs.getInt(2) : null), rs.getString(3), rs.getString(4), rs.getString(5),
						rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), 
						((rs.getString(12) != null) ? rs.getByte(12) : null),((rs.getString(13) != null) ? rs.getByte(13) : null), ((rs.getString(14) != null) ? rs.getByte(14) : null), 
						((rs.getString(15) != null) ? rs.getInt(15) : null),rs.getString(16), rs.getShort(17), rs.getString(18), ((rs.getString(19) != null) ? rs.getInt(19) : null), 
						rs.getString(20),((rs.getString(21) != null) ? rs.getLong(21) : null), rs.getString(22), rs.getBoolean(23), ((rs.getString(24) != null) ? rs.getInt(24) : null),
						((rs.getString(25) != null) ? rs.getString(25) : null), ((rs.getString(26) != null) ? rs.getInt(26) : null), ((rs.getString(27) != null) ? rs.getLong(27) : null),
						((rs.getString(28) != null) ? rs.getLong(28) : null), ((rs.getString(29) != null) ? rs.getLong(29) : null), (Long) rs.getObject(30), 
						rs.getString(31),((rs.getString(32) != null) ? rs.getByte(32) : null), null,((rs.getString(34) != null) ? rs.getByte(34) : null),
						((rs.getString(35) != null) ? rs.getString(35) : null));
				return contact;
			}
		}));

		CallableStatementCreatorFactory cscFactory = new CallableStatementCreatorFactory("call USP_GetChatContacts(?,?,?,?,?,?,?)", params);
		logger.info("Created Callable statement with orgid, userid:" + userContext.getUser().getOrganizationId() + "," + userContext.getUser()
		.getId());

		JdbcTemplate jdbcTemplate = new JdbcTemplate(((EntityManagerFactoryInfo) entityManager.getEntityManagerFactory()).getDataSource());
		Map<String, Object> inParams = new HashMap<String, Object>();
		inParams.put("P_OrganizationId", userContext.getUser().getOrganizationId());
		inParams.put("P_UserId", userContext.getUser().getId());
		inParams.put("P_SyncDates", syncDates);
		inParams.put("P_IsFirstTimeSync", isFirstTimeSync);
		inParams.put("P_Offset", offset);
		inParams.put("P_Count", limit);
		inParams.put("P_CountOnly", 0);

		CallableStatementCreator csc = cscFactory.newCallableStatementCreator(inParams);
		Map<String, Object> resultSets = jdbcTemplate.call(csc, params);
		logger.info("ChatContact map count:" + (resultSets == null ? null : resultSets.size()));

		List<ChatContact> contacts = (List<ChatContact>) resultSets.get("ChatContact");
		logger.info("ChatContact count: " + (contacts == null ? null : contacts.size()));

		return contacts;
	}

	@Override
	@Transactional(rollbackFor = { Exception.class })
	public Integer getContactCountToSync(UserContext userContext, String syncDates) {
		logger.info("Get Contact count to sync for sync dates " + syncDates);
		BigInteger count = new BigInteger("0");
		StoredProcedureQuery query = entityManager.createNamedStoredProcedureQuery("GetContactCountToSync");
		query.setParameter("P_OrganizationId", userContext.getUser().getOrganizationId());
		query.setParameter("P_UserId", userContext.getUser().getId());
		query.setParameter("P_SyncDates", syncDates);
		query.setParameter("P_IsFirstTimeSync", true);
		query.setParameter("P_Offset", 0);
		query.setParameter("P_Count", 0);
		query.setParameter("P_CountOnly", true);
		count = (BigInteger) query.getSingleResult();
		logger.info("Get Contact count to sync for sync dates " + syncDates + " returned count as " + count);
		return count.intValue();
	}

	@Override
	public ChatContact getChatContactDetail(UserContext userContext, Integer contactId, Integer inputChatType) {
		logger.info("Get Chat Contact Detail of contact " + contactId + " and chatType " + inputChatType);
		ChatContact contact = null;
		List<SqlParameter> params = new ArrayList<>();
		params.add(new SqlParameter("P_OrganizationId", Types.INTEGER));
		params.add(new SqlParameter("P_UserId", Types.INTEGER));
		params.add(new SqlParameter("P_ChatType", Types.TINYINT));
		params.add(new SqlParameter("P_ChatContactId", Types.INTEGER));
		params.add(new SqlParameter("P_ClientId", Types.TINYINT));

		params.add(new SqlReturnResultSet("ChatContact", new RowMapper<ChatContact>() {
			@Override
			public ChatContact mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				ChatContact contact = new ChatContact(
						rs.getInt(1), ((rs.getString(2) != null) ? rs.getInt(2) : null), rs.getString(3), rs.getString(4), rs.getString(5),
						rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), 
						((rs.getString(12) != null) ? rs.getByte(12) : null),((rs.getString(13) != null) ? rs.getByte(13) : null), ((rs.getString(14) != null) ? rs.getByte(14) : null), 
						((rs.getString(15) != null) ? rs.getInt(15) : null),rs.getString(16), rs.getShort(17), rs.getString(18), ((rs.getString(19) != null) ? rs.getInt(19) : null), 
						rs.getString(20),((rs.getString(21) != null) ? rs.getLong(21) : null), rs.getString(22), rs.getBoolean(23), ((rs.getString(24) != null) ? rs.getInt(24) : null),
						((rs.getString(25) != null) ? rs.getString(25) : null), ((rs.getString(26) != null) ? rs.getInt(26) : null), ((rs.getString(27) != null) ? rs.getLong(27) : null),
						((rs.getString(28) != null) ? rs.getLong(28) : null), ((rs.getString(29) != null) ? rs.getLong(29) : null), (Long) rs.getObject(30), rs.getString(31),
						((rs.getString(32) != null) ? rs.getByte(32) : null), ((rs.getString(33) != null) ? Byte.valueOf(rs.getString(33)) : null),
						((rs.getString(34) != null) ? Byte.valueOf(rs.getString(34)) : null),((rs.getString(35) != null) ? rs.getString(35) : null),((rs.getString(36) != null) ? rs.getString(36) : null),
						((rs.getString(37) != null) ? rs.getString(37) : null),((rs.getString(38) != null) ? rs.getLong(38) : null),((rs.getString(39) != null) ? rs.getByte(39) : null), ((rs.getString(40) != null) ? rs.getString(40) : null),
						((rs.getString(41) != null) ? rs.getByte(41) : null), ((rs.getString(42) != null) ? rs.getInt(42) : null), ((rs.getString(43) != null) ? rs.getString(43) : null),
						((rs.getString(44) != null) ? rs.getString(44) : null),((rs.getString(45) != null) ? rs.getString(45) : null));
				return contact;
			}
		}));

		CallableStatementCreatorFactory cscFactory = new CallableStatementCreatorFactory("call USP_GetUserChatContactDetail(?,?,?,?,?)", params);
		logger.debug("Created Callable Statement");

		JdbcTemplate jdbcTemplate = new JdbcTemplate(((EntityManagerFactoryInfo) entityManager.getEntityManagerFactory()).getDataSource());
		Map<String, Object> inParams = new HashMap<String, Object>();
		inParams.put("P_OrganizationId", userContext.getUser().getOrganizationId());
		inParams.put("P_UserId", userContext.getUser().getId());
		inParams.put("P_ChatType", inputChatType.byteValue());
		inParams.put("P_ChatContactId", contactId);

	//	Integer clientId = Integer.valueOf(ThreadContext.get().getUserContext().getClientId());
		Integer clientId = 101;
		inParams.put("P_ClientId", clientId);

		CallableStatementCreator csc = cscFactory.newCallableStatementCreator(inParams);
		Map<String, Object> resultSets = jdbcTemplate.call(csc, params);
		logger.debug("Get Chat Contact Detail of contact " + contactId + " and chatType " + inputChatType
				+ " returned result of size " + (resultSets == null ? null : resultSets.size()));
		List<ChatContact> contacts = ((List<ChatContact>) resultSets.get("ChatContact"));
		logger.info("Get Chat Contact Detail of contact " + contactId + " and chatType " + inputChatType
				+ " returned Contact List of size " + (contacts == null ? null : contacts.size()));
		if (!contacts.isEmpty()) {
			contact = contacts.get(0);
		}

		if(contact == null) {
			logger.debug("ContactId: "+contactId+" not found in DB");
			throw new ResourceNotFoundException(ErrorCode.Entity_Not_Found,"ContactId couldnot be fetched from DB ");
		}

		return contact;
	}

	//@Override
	public ChatContact getChatContactDetail2(UserContext userContext, Integer contactId, Integer chatType) {
		Integer clientId = Integer.valueOf(ThreadContext.get().getUserContext().getClientId());
		Integer orgId = userContext.getUser().getOrganizationId();
		Integer userId = userContext.getUser().getId();

		StoredProcedureQuery query = entityManager.createStoredProcedureQuery("USP_GetUserChatContactDetail");
		query.registerStoredProcedureParameter("P_OrganizationId", Integer.class, ParameterMode.IN);
		query.registerStoredProcedureParameter("P_UserId", Integer.class, ParameterMode.IN);
		query.registerStoredProcedureParameter("P_ChatType", Byte.class, ParameterMode.IN);
		query.registerStoredProcedureParameter("P_ChatContactId", Integer.class, ParameterMode.IN);
		query.registerStoredProcedureParameter("P_ClientId", Byte.class, ParameterMode.IN);

		query.setParameter("P_OrganizationId", orgId);
		query.setParameter("P_UserId", userId);
		query.setParameter("P_ChatType", chatType.byteValue());
		query.setParameter("P_ChatContactId", contactId);
		query.setParameter("P_ClientId", clientId.byteValue());

		List<Object[]> tuples = query.getResultList();
		if (tuples.isEmpty()){
			logger.error("Contact not found org {}, user {}, contact {}, type {}",
					orgId, userId, contactId, chatType);
			throw new ResourceNotFoundException(ErrorCode.Entity_Not_Found,
					String.format("Contact not found org %d, user %d, contact %d, type %d",
							orgId, userId, contactId, chatType));
		}
		return toChatContact(tuples.get(0));
	}

	private ChatContact toChatContact(Object[] rs) {
		return new ChatContact(
				(Integer) rs[0],
				(Integer) rs[1],
				(String) rs[2],
				(String) rs[3],
				(String) rs[4],
				(String) rs[5],
				(String) rs[6],
				(String) rs[7],
				(String) rs[8],
				(String) rs[9],
				(String) rs[10],
				(Byte) rs[11],
				(Byte) rs[12],
				(Byte) rs[13],
				(Integer) rs[14],
				(String) rs[15],
				(Short) rs[16],
				(String) rs[17],
				(Integer) rs[18],
				(String) rs[19],
				(Long) rs[20],
				(String) rs[21],
				(Boolean) rs[22],
				(Integer) rs[23],
				(String) rs[24],
				(Integer) rs[25],
				(Long) rs[26],
				(Long) rs[27],
				(Long) rs[28],
				(Long) rs[29],
				(String) rs[30],
				(Byte) rs[31],
				(Byte) rs[32],
				(Byte) rs[33],
				(String) rs[34],
				(String) rs[35],
				(String) rs[36],
				(Long) rs[37],
				(Byte) rs[38],
				(String) rs[39],
				(Byte) rs[40],
				(Integer) rs[41],
				(String) rs[42],
				(String) rs[43],
				(String) rs[44]
				
		);
	}

	@Override
	public Map<String, Object> getSyncChatContacts(UserContext userContext, Integer offset, Integer limit, String syncDates, Boolean isProfileInfo) {
		logger.info("get Chat Contacts with input offset, limit, syncDates"
				+ offset + "," + limit + "," + syncDates);
		List<SqlParameter> params = new ArrayList<>();
		params.add(new SqlParameter("P_OrganizationId", Types.INTEGER));
		params.add(new SqlParameter("P_UserId", Types.INTEGER));
		params.add(new SqlParameter("P_SyncDates", Types.VARCHAR));
		params.add(new SqlParameter("P_IsFirstTimeSync", Types.BIT));
		params.add(new SqlParameter("P_ResultType", Types.TINYINT));
		params.add(new SqlParameter("P_Offset", Types.INTEGER));
		params.add(new SqlParameter("P_Count", Types.INTEGER));
		params.add(new SqlParameter("P_CountOnly", Types.BIT));

		final Map<String, Object> result = new HashMap<String, Object>();
		params.add(new SqlReturnResultSet("ChatContact", new RowMapper<Map<String, Object>>() {
			@Override
			public Map<String, Object> mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				String contacts = rs.getString(1);
				result.put("Contacts", contacts == null || contacts.trim().length() == 0 ? "[]" : contacts);
				result.put("Count", rs.getObject(2));
				result.put("Heading", rs.getObject(3));
				return result;
			}
		}));

		CallableStatementCreatorFactory cscFactory = new CallableStatementCreatorFactory("call USP_SyncChatContacts(?,?,?,?,?,?,?,?)", params);
		logger.info("Created Callable statement with orgid, userid:" + userContext.getUser().getOrganizationId() + "," + userContext.getUser()
		.getId());

		JdbcTemplate jdbcTemplate = new JdbcTemplate(((EntityManagerFactoryInfo) entityManager.getEntityManagerFactory()).getDataSource());
		Map<String, Object> inParams = new HashMap<String, Object>();
		inParams.put("P_OrganizationId", userContext.getUser().getOrganizationId());
		inParams.put("P_UserId", userContext.getUser().getId());
		inParams.put("P_SyncDates", syncDates);
		inParams.put("P_IsFirstTimeSync", Boolean.TRUE);
		if (isProfileInfo) {
			inParams.put("P_ResultType", new Byte("2"));
		} else {
			inParams.put("P_ResultType", new Byte("1"));
		}
		inParams.put("P_Offset", offset);
		inParams.put("P_Count", limit);
		inParams.put("P_CountOnly", 0);

		CallableStatementCreator csc = cscFactory.newCallableStatementCreator(inParams);
		Map<String, Object> resultSets = jdbcTemplate.call(csc, params);
		logger.info("ChatContact map count:" + (resultSets == null ? null : resultSets.size()));

		return (Map<String, Object>) ((List) resultSets.get("ChatContact")).get(0);
	}

	@Override
	public List<ChatContact> getOrgContacts(Integer organizationId, Integer userId, Long lastSyncTime) {
		List<SqlParameter> params = new ArrayList<>();
		params.add(new SqlParameter("P_OrganizationId", Types.INTEGER));
		params.add(new SqlParameter("P_UserId", Types.INTEGER));
		params.add(new SqlParameter("P_StartDate", Types.BIGINT));
		params.add(new SqlParameter("P_EndDate", Types.BIGINT));
		params.add(new SqlParameter("P_Offset", Types.INTEGER));
		params.add(new SqlParameter("P_Count", Types.INTEGER));

		params.add(new SqlReturnResultSet("OrgContact", (RowMapper<ChatContact>) (rs, rowNum) -> {
//				1. u.Id,
//				2. u.UserType,
//				3. u.FirstName,
//				4. u.CreatedDate,
//				5. u.Active,
//				6. ud.Designation,
//				7. ud.PhotoURL,
//				8. ud.MobileNumber,
//				9. ud.LandlineNumber
//				10. d.DepartmentName
			ChatContact contact = new ChatContact();
			contact.setId(rs.getInt(1));
			contact.setUserType(rs.getString(2));
			contact.setName(rs.getString(3));
			contact.setCreatedDate(rs.getLong(4));
			contact.setActive(rs.getBoolean(5));
			contact.setDesignation(rs.getString(6));
			String photo = rs.getString(7);
			if (photo != null && !photo.isEmpty()) {
				UserPhoto userPhoto = Json.fromJson(Json.parse(photo), UserPhoto.class);
				userPhoto.setProfile(null);
				contact.setPhotoURL(userPhoto);
			}

			contact.setMobileNumber(rs.getString(8));
			contact.setDepartment(rs.getString(10));
			contact.setChatType(ChatType.One2One.getId());
			return contact;
		}));

		CallableStatementCreatorFactory cscFactory = new CallableStatementCreatorFactory("call USP_SyncOrganizationContact(?,?,?,?,?,?)", params);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(((EntityManagerFactoryInfo) readEntityManager.getEntityManagerFactory()).getDataSource());
		Map<String, Object> inParams = new HashMap<>();
		inParams.put("P_OrganizationId", organizationId);
		inParams.put("P_UserId", userId);
		inParams.put("P_StartDate", lastSyncTime);
		inParams.put("P_EndDate", System.currentTimeMillis());
		inParams.put("P_Offset", 0);
		inParams.put("P_Count", Integer.MAX_VALUE);

		CallableStatementCreator csc = cscFactory.newCallableStatementCreator(inParams);
		Map<String, Object> resultSets = jdbcTemplate.call(csc, params);
		List<ChatContact> orgContacts = (List<ChatContact>) resultSets.get("OrgContact");
		logger.info("OrgContact count: " + (orgContacts == null ? null : orgContacts.size()));
		return orgContacts;
	}

	@Override
	public List<ChatContact> getNonOrgContacts(Integer organizationId, Integer userId, Long lastSyncTime) {
		List<SqlParameter> params = new ArrayList<>();
		params.add(new SqlParameter("P_OrganizationId", Types.INTEGER));
		params.add(new SqlParameter("P_UserId", Types.INTEGER));
		params.add(new SqlParameter("P_StartDate", Types.BIGINT));
		params.add(new SqlParameter("P_EndDate", Types.BIGINT));
		params.add(new SqlParameter("P_Offset", Types.INTEGER));
		params.add(new SqlParameter("P_Count", Types.INTEGER));

		params.add(new SqlReturnResultSet("NonOrgContact", (RowMapper<ChatContact>) (rs, rowNum) -> {
//				1. u.Id,
//				2. u.UserType,
//				3. u.FirstName,
//				4. u.CreatedDate,
//				5. u.ContactType,
//				6. u.Active,
//				7. ud.Designation,
//				8. ud.PhotoURL,
//				9. ud.MobileNumber,
//				10. ud.LandlineNumber
//				11. u.UserCategory,
//				12. d.DepartmentName

			ChatContact contact = new ChatContact();
			contact.setId(rs.getInt(1));
			contact.setUserType(rs.getString(2));
			contact.setName(rs.getString(3));
			contact.setCreatedDate(rs.getLong(4));
			contact.setContactType(rs.getInt(5));
			contact.setActive(rs.getBoolean(6));
			contact.setDesignation(rs.getString(7));
			String photo = rs.getString(8);
			if (photo != null && !photo.isEmpty()) {
				UserPhoto userPhoto = Json.fromJson(Json.parse(photo), UserPhoto.class);
				userPhoto.setProfile(null);
				contact.setPhotoURL(userPhoto);
			}			
			contact.setMobileNumber(rs.getString(9));
			//landline Number			
			contact.setLandlineNumber(rs.getString(10));
			contact.setUserCategory(rs.getByte(11));
			contact.setDepartment(rs.getString(12));
			contact.setChatType(ChatType.One2One.getId());
			return contact;
		}));

		CallableStatementCreatorFactory cscFactory = new CallableStatementCreatorFactory("call USP_SyncNonOrganizationContact(?,?,?,?,?,?)", params);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(((EntityManagerFactoryInfo) readEntityManager.getEntityManagerFactory()).getDataSource());
		Map<String, Object> inParams = new HashMap<>();
		inParams.put("P_OrganizationId", organizationId);
		inParams.put("P_UserId", userId);
		inParams.put("P_StartDate", lastSyncTime);
		inParams.put("P_EndDate", System.currentTimeMillis());
		inParams.put("P_Offset", 0);
		inParams.put("P_Count", Integer.MAX_VALUE);

		CallableStatementCreator csc = cscFactory.newCallableStatementCreator(inParams);
		Map<String, Object> resultSets = jdbcTemplate.call(csc, params);
		List<ChatContact> orgContacts = (List<ChatContact>) resultSets.get("NonOrgContact");
		logger.info("OrgContact count: " + (orgContacts == null ? null : orgContacts.size()));
		return orgContacts;
	}

	@Override
	public List<ChatContact> getOrgGroupContacts(Integer organizationId, Integer userId, Long lastSyncTime) {
		List<SqlParameter> params = new ArrayList<>();
		//params.add(new SqlParameter("P_OrganizationId", Types.INTEGER));
		params.add(new SqlParameter("P_UserId", Types.INTEGER));
		params.add(new SqlParameter("P_StartDate", Types.BIGINT));
		params.add(new SqlParameter("P_EndDate", Types.BIGINT));
		params.add(new SqlParameter("P_Offset", Types.INTEGER));
		params.add(new SqlParameter("P_Count", Types.INTEGER));

		params.add(new SqlReturnResultSet("GroupContact", (RowMapper<ChatContact>) (rs, rowNum) -> {
			//1. Id
			//2. GroupName
			//3. CreatedDate
			//4. Active
			//5. MemberStatus
			//6. GroupStatus
			//7. CreatedById
			//8. GroupType
			//9. MemberRole
			ChatContact contact = new ChatContact();
			contact.setId(rs.getInt(1));
			contact.setName(rs.getString(2));
			contact.setCreatedDate(rs.getLong(3));
			contact.setActive(rs.getBoolean(4));
			contact.setMemberStatus(rs.getByte(5));
			contact.setGroupStatus(rs.getByte(6));
			contact.setGroupCreatedById(rs.getInt(7));
			contact.setGroupType(rs.getByte(8));
			contact.setMemberRole(rs.getByte(9));
			contact.setChatType(ChatType.GroupChat.getId());
			return contact;
		}));
		
		params.add(new SqlReturnResultSet("GroupPreference", (RowMapper<GroupPreference>) (rs, rowNum) -> {
			/**
			 * 	    	SELECT gp.GroupId, gp.Preference,  gp.value
			 */
			int colIndex = 1;
			GroupPreference groupPreference = new GroupPreference();
			groupPreference.setGroupId(rs.getLong(colIndex++));
			groupPreference.setPreference(rs.getString(colIndex++));
			groupPreference.setValue(rs.getString(colIndex));
			return groupPreference;
		}));

		CallableStatementCreatorFactory cscFactory = new CallableStatementCreatorFactory("call USP_SyncGroupContact(?,?,?,?,?)", params);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(((EntityManagerFactoryInfo) readEntityManager.getEntityManagerFactory()).getDataSource());
		Map<String, Object> inParams = new HashMap<>();
		//inParams.put("P_OrganizationId", organizationId);
		inParams.put("P_UserId", userId);
		inParams.put("P_StartDate", lastSyncTime);
		inParams.put("P_EndDate", System.currentTimeMillis());
		inParams.put("P_Offset", 0);
		inParams.put("P_Count", Integer.MAX_VALUE);
 
		CallableStatementCreator csc = cscFactory.newCallableStatementCreator(inParams);
		Map<String, Object> resultSets = jdbcTemplate.call(csc, params);
		List<ChatContact> orgContacts = (List<ChatContact>) resultSets.get("GroupContact");
		final List<GroupPreference> allGroupPreferences = (List<GroupPreference>) resultSets.get("GroupPreference");
		final Map<Long, List<GroupPreference>> groupPreferencesByGroupId = allGroupPreferences.stream().collect(Collectors.groupingBy(GroupPreference::getGroupId));
		logger.info("OrgContact count: " + (orgContacts == null ? null : orgContacts.size()));
		List<ChatContact>  chatContacts = new ArrayList<ChatContact>();
		for (ChatContact chatContact : orgContacts) {
		logger.info("get chat contact id: {}", chatContact.getId());
		logger.info("pref : {}", groupPreferencesByGroupId.get(Long.valueOf(chatContact.getId())));
			final List<GroupPreference> groupPreferences = groupPreferencesByGroupId.get(Long.valueOf(chatContact.getId()));
			logger.info("groupPreferences : {}", groupPreferences);
			chatContact.setGroupPreferences(toPrefJsonString(groupPreferences));
			chatContacts.add(chatContact);
			
		}
		return chatContacts;
	}

	private List<Map<String, String>> toPrefJsonString(List<GroupPreference> groupPreferences) {
		if (groupPreferences == null){
			return null;
		}
		try {
			List<Map<String, String>> preferences = new ArrayList<Map<String, String>>();						
			for (GroupPreference pref : groupPreferences) {
			//	JsonNode pref = groupPreferences.get(count);
					Map<String, String> mapCustStatus = new HashMap<String, String>();
					mapCustStatus.put("name", pref.getPreference());
					mapCustStatus.put("value", pref.getValue());
					preferences.add(mapCustStatus);			
			}
			return preferences;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create JSON of user preference");
		}
	}
	
	@Override
	public List<ChatContact> getContactsV2(UserContext userContext, Integer offset, Integer limit, String syncDates) {
		logger.info("get Chat Contacts with input offset, limit, syncDates"
				+ offset + "," + limit + "," + syncDates);
		List<SqlParameter> params = new ArrayList<>();
		params.add(new SqlParameter("P_OrganizationId", Types.INTEGER));
		params.add(new SqlParameter("P_UserId", Types.INTEGER));
		params.add(new SqlParameter("P_SyncDates", Types.VARCHAR));
		params.add(new SqlParameter("P_IsFirstTimeSync", Types.BIT));
		params.add(new SqlParameter("P_ResultType", Types.TINYINT));
		params.add(new SqlParameter("P_Offset", Types.INTEGER));
		params.add(new SqlParameter("P_Count", Types.INTEGER));
		params.add(new SqlParameter("P_CountOnly", Types.BIT));

		params.add(new SqlReturnResultSet("ChatContact", new RowMapper<ChatContact>() {
			@Override
			public ChatContact mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				ChatContact contact = new ChatContact(rs.getInt(1), rs.getString(2), rs.getString(3), ((Long) rs.getObject(4)).byteValue(), ((Long) rs
						.getObject(5)).intValue(),
						(Long) rs.getObject(6), (Boolean) rs.getObject(7), ((rs.getObject(8) != null) ? ((Integer) rs.getObject(8)).byteValue() : null), ((rs
								.getObject(9) != null) ? ((Integer) rs.getObject(9)).byteValue() : null), (Long) rs.getObject(10), rs.getString(11),
						(Integer) rs.getObject(12), (Long) rs.getObject(13), (Long) rs.getObject(14), rs.getString(15), rs.getString(16), rs.getString(17), rs
						.getString(18), rs.getString(19), ((rs.getObject(20) != null) ? rs.getInt(20) : null),
						((rs.getObject(21) != null) ? Byte.valueOf(rs.getString(21)) : null), ((rs.getObject(22) != null) ? Byte.valueOf(rs.getString(22)) : null));
				return contact;
			}
		}));

		CallableStatementCreatorFactory cscFactory = new CallableStatementCreatorFactory("call USP_SyncChatContacts(?,?,?,?,?,?,?,?)", params);
		logger.info("Created Callable statement with orgid, userid:" + userContext.getUser().getOrganizationId() + "," + userContext.getUser()
		.getId());

		JdbcTemplate jdbcTemplate = new JdbcTemplate(((EntityManagerFactoryInfo) entityManager.getEntityManagerFactory()).getDataSource());
		Map<String, Object> inParams = new HashMap<String, Object>();
		inParams.put("P_OrganizationId", userContext.getUser().getOrganizationId());
		inParams.put("P_UserId", userContext.getUser().getId());
		inParams.put("P_SyncDates", syncDates);
		inParams.put("P_IsFirstTimeSync", Boolean.FALSE);
		inParams.put("P_ResultType", new Byte("3"));
		inParams.put("P_Offset", offset);
		inParams.put("P_Count", limit);
		inParams.put("P_CountOnly", Boolean.FALSE);

		CallableStatementCreator csc = cscFactory.newCallableStatementCreator(inParams);
		Map<String, Object> resultSets = jdbcTemplate.call(csc, params);
		logger.info("ChatContact map count:" + (resultSets == null ? null : resultSets.size()));

		List<ChatContact> contacts = (List<ChatContact>) resultSets.get("ChatContact");
		logger.info("ChatContact count: " + (contacts == null ? null : contacts.size()));

		return contacts;
	}

	private String getContactsFromIms(UserContext userContext, Integer offset, Integer limit, String searchKey) {
		String url = contactUrl;
		url = MessageFormat.format(url, String.valueOf(userContext.getUser().getId()), String.valueOf(offset), String.valueOf(limit));
		CloseableHttpClient client = httpConnectionManager.getHttpClient();
		if (searchKey != null) {
			try {
				searchKey = URLEncoder.encode(searchKey, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			url = url + "&searchKey=" + searchKey;
		}
		logger.info("Get Contacts From Ims using modified url : " + url);

		HttpGet httpget = new HttpGet(url);
		try {
			httpget.addHeader(Constants.CLIENT_ID, userContext.getClientId());
			httpget.addHeader(Constants.X_REQUEST_ID, userContext.getRequestId());
			httpget.addHeader(Constants.X_OWB_TOKEN, userContext.getToken());
			httpget.addHeader(Constants.X_FORWARDED_FOR, userContext.getClientIPAddress());

			CloseableHttpResponse response = client.execute(httpget);
			logger.info("validate token ResponseCode:" + response.getStatusLine().getStatusCode());
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName());
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
			logger.debug("Returning Contacts From Ims as " + result);
			return result.toString();
		} catch (ApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			httpget.releaseConnection();
		}
	}
}
