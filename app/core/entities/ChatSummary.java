package core.entities;

import java.io.IOException;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.libs.Json;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity
@Table(name = "chat_user_last_message")
@SqlResultSetMappings({ @SqlResultSetMapping(name = "ChatSummary.GetContactChatSummary", classes = {
		@ConstructorResult(targetClass = core.entities.ChatSummary.class, columns = {
				@ColumnResult(name = "lastMessageId", type = Long.class),
				@ColumnResult(name = "ChatType", type = Byte.class),
				@ColumnResult(name = "SenderId", type = Integer.class),
				@ColumnResult(name = "UnreadCount", type = Short.class),
				@ColumnResult(name = "Message", type = String.class),
				@ColumnResult(name = "LastMsgSenderId", type = Integer.class),
				@ColumnResult(name = "CreatedDate", type = Long.class) }) }) })
@NamedQueries({
		@NamedQuery(name = "ChatSummary.UpdateUnReadChat", query = "Update ChatSummary cs set cs.unReadMsgCount = 0 where cs.chatType=:chatType and cs.contactId=:contcatId and cs.recipientId=:recipientId"),
		@NamedQuery(name = "ChatSummary.DeactivateOne2OneChatSummary", query = "Update ChatSummary cs set cs.unReadMsgCount = 0, cs.active=false where cs.chatType=0 and cs.contactId=:deletedUserId"),
		@NamedQuery(name = "ChatSummary.getUnReadMsgsPerContact", query = "Select cs.contactId As SenderId, cs.chatType, cs.unReadMsgCount As UnreadCount from ChatSummary cs where cs.recipientId=:recipientId AND cs.unReadMsgCount>0"),
		@NamedQuery(name = "ChatSummary.getUnReadMsgContact", query = "SELECT cu.recipientId AS UserId, COUNT(1) AS ChatCount FROM ChatSummary cu WHERE cu.recipientId IN :userIds AND cu.unReadMsgCount > 0 AND cu.active = TRUE GROUP BY cu.recipientId"),
		@NamedQuery(name = "ChatSummary.getUnReadSenderCount", query = "Select count(cs.contactId) As UnreadCount from ChatSummary cs where cs.recipientId=:recipientId AND cs.unReadMsgCount>0 and cs.active=true"),
		@NamedQuery(name = "ChatSummary.getUnReadMsgsForContacts", query = "Select cs.contactId As SenderId, cs.chatType, cs.unReadMsgCount As UnreadCount from ChatSummary cs where cs.recipientId=:recipientId "
				+ " and ( ( cs.chatType = 0 AND cs.contactId IN (:userIds) ) OR ( cs.chatType = 1 AND cs.contactId IN (:groupIds) ) )"),
		@NamedQuery(name = "ChatSummary.getChatSummary", query = "Select cs from ChatSummary cs where cs.recipientId=:recipientId AND cs.contactId=:contactId AND cs.chatType=:chatType AND cs.active=true"),
		@NamedQuery(name = "ChatSummary.RecipientUnreadCount", query = "Select cs.contactId, cs.chatType, cs.unReadMsgCount from ChatSummary cs where cs.recipientId=:recipientId AND  cs.chatType IN (0, 1) AND cs.active=true AND cs.unReadMsgCount>0")
})
@NamedStoredProcedureQueries({
		@NamedStoredProcedureQuery(name = "ChatSummary.DeleteChatMessages", procedureName = "USP_DeleteChatMessages", parameters = {
				@StoredProcedureParameter(name = "P_UserId", type = Integer.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_ContactId", type = Integer.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_ChatType", type = Byte.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_ChatMessageIds", type = String.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_DeleteOption", type = Byte.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_DeletionDate", type = Long.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "O_Status", type = Byte.class, mode = ParameterMode.INOUT) }) })


@JsonInclude(Include.NON_NULL)
public class ChatSummary extends BaseEntity {

	final static Logger logger = LoggerFactory.getLogger(ChatSummary.class);

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	private Long Id;

	@JsonIgnore
	@Column(name = "chatType")
	private Byte chatType;

	@JsonIgnore
	@Column(name = "senderId")
	private Integer contactId;

	@JsonIgnore
	@Column
	private Integer recipientId;

	@Column
	private Long lastMessageId;

	@Column
	private Short unReadMsgCount;

	@JsonIgnore
	@Column
	private Long leftDate;

	@JsonIgnore
	@Transient
	private Long time;

	@Transient
	private String lastMessage;
	@Transient
	private Integer lastMsgSenderId;
	@Transient
	private String lastMsgSenderName;
	@Transient
	private String lastMsgDate;
	@Transient
	private Integer userId;

	@Deprecated
	@Transient
	private Boolean status;

	@Transient
	private JsonNode data;
	@Transient
	private Long utcDate;
	@Transient
	private Long parentMsgId;
	@Transient
	private ChatMessage parentMsg;
	@Transient
	private Long readDate;

	public ChatSummary() {

	}

	public ChatSummary(Byte chatType, Integer contactId, Integer recipientId) {
		super();
		this.chatType = chatType;
		this.contactId = contactId;
		this.recipientId = recipientId;
		this.unReadMsgCount = 1;
		this.active = true;
	}

	public ChatSummary(Long lastMessageId, Byte chatType, Integer senderId, Short unreadCount, String message,
			Integer lastMsgSenderId, Long createdDate) {
		super();
		this.chatType = chatType;
		this.contactId = senderId;
		this.unReadMsgCount = unreadCount;
		this.lastMessage = message;
		this.lastMsgSenderId = lastMsgSenderId;
		this.time = createdDate;
		this.lastMessageId = lastMessageId;
	}

	// GetContactsForSyncV2
	public ChatSummary(String message, Integer lastMsgSenderId, Long lastMessageDateTime, Long lastMessageId,
			Long readDate) {
		super();
		this.lastMessage = message;
		this.lastMsgSenderId = lastMsgSenderId;
		this.utcDate = lastMessageDateTime;
		this.lastMessageId = lastMessageId;
		this.readDate = readDate;
	}

	public ChatSummary(Integer senderId, Byte chatType, Short unreadCount) {
		super();
		this.contactId = senderId;
		this.chatType = chatType;
		this.unReadMsgCount = unreadCount;
	}

	public ChatSummary(Integer userId, Short unreadCount) {
		super();
		this.userId = userId;
		this.unReadMsgCount = unreadCount;
	}

	public ChatSummary(Short unreadCount, String message, Integer lastMsgSenderId, String lastMsgSenderName,
			Long lastMessageDateTime, String attachmentData, Long lastMessageId, Long ParentMsgId, String ParentMsg) {
		super();
		this.unReadMsgCount = unreadCount;
		this.lastMessage = message;
		this.lastMsgSenderId = lastMsgSenderId;
		this.lastMsgSenderName = lastMsgSenderName;
		this.utcDate = lastMessageDateTime;
		if (attachmentData != null && !attachmentData.isEmpty()) {
			this.data = Json.parse(attachmentData);
		}
		this.lastMessageId = lastMessageId;
		this.parentMsgId = ParentMsgId;
		if (ParentMsg != null && !ParentMsg.isEmpty()) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				this.parentMsg = mapper.readValue(ParentMsg, ChatMessage.class);
				if (this.parentMsg.getData().asText() != null && !this.parentMsg.getData().asText().trim().isEmpty()) {
					JsonNode node = (JsonNode) mapper.readTree(this.parentMsg.getData().asText());
					this.parentMsg.setData(node);
				} else {
					this.parentMsg.setData(null);
				}
			} catch (IOException e) {
				logger.warn(
						"Failed to map ParentMessage JSON to ChatMessage.class. Proc USP_InsertOne2onechat returned "
								+ ParentMsg,
						e);
				e.printStackTrace();
			}
		}
	}

	public Integer getContactId() {
		return contactId;
	}

	public void setContactId(Integer contactId) {
		this.contactId = contactId;
	}

	public Short getUnReadMsgCount() {
		return unReadMsgCount;
	}

	public void setUnReadMsgCount(Short unReadMsgCount) {
		this.unReadMsgCount = unReadMsgCount;
	}

	public String getLastMessage() {
		return lastMessage;
	}

	public void setLastMessage(String lastMessage) {
		this.lastMessage = lastMessage;
	}

	public Integer getLastMsgSenderId() {
		return lastMsgSenderId;
	}

	public void setLastMsgSenderId(Integer lastMsgSenderId) {
		this.lastMsgSenderId = lastMsgSenderId;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	public String getLastMsgDate() {
		return lastMsgDate;
	}

	public void setLastMsgDate(String lastMsgDate) {
		this.lastMsgDate = lastMsgDate;
	}

	public Byte getChatType() {
		return chatType;
	}

	public void setChatType(Byte chatType) {
		this.chatType = chatType;
	}

	public Integer getRecipientId() {
		return recipientId;
	}

	public void setRecipientId(Integer recipientId) {
		this.recipientId = recipientId;
	}

	public Long getLastMessageId() {
		return lastMessageId;
	}

	public void setLastMessageId(Long lastMessageId) {
		this.lastMessageId = lastMessageId;
	}

	public Long getLeftDate() {
		return leftDate;
	}

	public void setLeftDate(Long leftDate) {
		this.leftDate = leftDate;
	}

	public String getLastMsgSenderName() {
		return lastMsgSenderName;
	}

	public void setLastMsgSenderName(String lastMsgSenderName) {
		this.lastMsgSenderName = lastMsgSenderName;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Long getUtcDate() {
		return utcDate;
	}

	public void setUtcDate(Long utcDate) {
		this.utcDate = utcDate;
	}

	public JsonNode getData() {
		return data;
	}

	public void setData(JsonNode data) {
		this.data = data;
	}

	public Long getParentMsgId() {
		return parentMsgId;
	}

	public void setParentMsgId(Long parentMsgId) {
		this.parentMsgId = parentMsgId;
	}

	public ChatMessage getParentMsg() {
		return parentMsg;
	}

	public void setParentMsg(ChatMessage parentMsg) {
		this.parentMsg = parentMsg;
	}

	public Long getReadDate() {
		return readDate;
	}

	public void setReadDate(Long readDate) {
		this.readDate = readDate;
	}
}
