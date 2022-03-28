/**
 * 
 */
package core.entities;

import java.io.IOException;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.utils.Enums.ChatMessageType;

@Entity
@Table(name = "group_chat")

@NamedQueries({
		@NamedQuery(name="GroupChat.updateWelcomeMessage",query= "update GroupChat gc set gc.senderId = :senderId, gc.createdDate = :createdDate  where gc.groupId = :groupId and gc.chatMessageType = :chatMessageType and Active=true")
})
@NamedStoredProcedureQueries({
		@NamedStoredProcedureQuery(name = "GroupChat.CreateGroupChat", procedureName = "USP_InsertGroupChat", parameters = {
				@StoredProcedureParameter(name = "P_SenderId", type = Integer.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_GroupId", type = Integer.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_Message", type = String.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_Data", type = String.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_MessageType", type = Byte.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_CreatedDate", type = Long.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_MemberId", type = Integer.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_EventType", type = Byte.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_ParentMessageId", type = Long.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_ExcludeFromMsgRecipient", type = String.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_GroupType", type = Integer.class, mode = ParameterMode.IN) }, resultSetMappings = {
						"GroupChat.InsertGroupChatResultMappings" }),
		@NamedStoredProcedureQuery(name = "GroupChat.CreateVideoKycGroupChat", procedureName = "USP_InsertVideoKycGroupChat", parameters = {
				@StoredProcedureParameter(name = "P_SenderId", type = Integer.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_GroupId", type = Integer.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_Message", type = String.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_Data", type = String.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_MessageType", type = Byte.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_CreatedDate", type = Long.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_MemberId", type = Integer.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_EventType", type = Byte.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_ParentMessageId", type = Long.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_ExcludeFromMsgRecipient", type = String.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_GroupType", type = Integer.class, mode = ParameterMode.IN) }, resultSetMappings = {
						"GroupChat.InsertGroupChatResultMappings" }),
		@NamedStoredProcedureQuery(name = "GroupChat.AddAttachmentRecipient", procedureName = "USP_AssignAttachmentRecipient", parameters = {
				@StoredProcedureParameter(name = "P_AttachmentIds", type = String.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_GroupId", type = Long.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_EntityType", type = String.class, mode = ParameterMode.IN),
				@StoredProcedureParameter(name = "P_CreatedById", type = Integer.class, mode = ParameterMode.IN)})})
@SqlResultSetMappings({ @SqlResultSetMapping(name = "GroupChat.InsertGroupChatResultMappings", classes = {
		@ConstructorResult(targetClass = core.entities.GroupChat.class, columns = {
				@ColumnResult(name = "Id", type = Long.class), @ColumnResult(name = "SenderId", type = Integer.class),
				@ColumnResult(name = "GroupId", type = Integer.class),
				@ColumnResult(name = "Message", type = String.class), @ColumnResult(name = "Data", type = String.class),
				@ColumnResult(name = "MessageType", type = Byte.class),
				@ColumnResult(name = "ParentMessageId", type = Long.class),
				@ColumnResult(name = "CreatedDate", type = Long.class),
				@ColumnResult(name = "Active", type = Boolean.class),
				@ColumnResult(name = "ParentMsg", type = String.class),
				@ColumnResult(name = "ReadDate", type = Long.class),
				@ColumnResult(name = "MemberReadDate", type = Long.class) }) }) })
@JsonInclude(Include.NON_NULL)
public class GroupChat extends BaseEntity {

	final static Logger logger = LoggerFactory.getLogger(GroupChat.class);

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonProperty("mid")
	protected Long Id;

	@Column(name = "senderId")
	@JsonProperty("from")
	private Integer senderId;

	@Column(name = "groupId")
	@JsonProperty("to")
	private Integer groupId;

	@Column(name = "message")
	private String text;

	@Column(name = "data")
	@JsonIgnore
	private String data;

	@Column(name = "messageType")
	private Byte chatMessageType;

	@JsonProperty("utcDate")
	@Column
	private Long createdDate;

	@Column(name = "ParentMessageId")
	@JsonProperty
	private Long parentMsgId;

	@Transient
	@JsonProperty("data")
	private JsonNode dataJson;

	@Transient
	private String date;

	@Transient
	@JsonIgnore
	private Integer memberId;

	@Transient
	@JsonIgnore
	private Byte eventNotificationType;

	@Transient
	private ChatMessage parentMsg;

	@Transient
	private Long readDate;

	@Transient
	private String readDateStr;

	@Transient
	@JsonProperty("name")
	private String fromName;

	@Transient
	private Integer dateDiff;

	@Transient
	private Long memberReadDate;

	@Transient
	private String excludedRecipients;
	
	@Transient
	private Integer groupType;

	@Transient
	private Boolean				showText              = false;

	public GroupChat() {
	}

	// GroupChat.InsertGroupChatResultMappings
	public GroupChat(Long Id, Integer SenderId, Integer GroupId, String Message, String Data, Byte MessageType,
			Long ParentMessageId, Long CreatedDate, Boolean Active, String ParentMsg, Long readDate,
			Long MemberReadDate) {
		super();
		this.Id = Id;
		this.senderId = SenderId;
		this.groupId = GroupId;
		this.text = Message;
		this.data = Data;
		this.chatMessageType = MessageType;
		this.parentMsgId = ParentMessageId;
		this.createdDate = CreatedDate;
		this.active = Active;
		if (ParentMsg != null && !ParentMsg.isEmpty()) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				this.parentMsg = mapper.readValue(ParentMsg, ChatMessage.class);
				if (this.parentMsg.getData().asText() != null && !this.parentMsg.getData().asText().trim().isEmpty()) {
					JsonNode node = (JsonNode) mapper.readTree(this.parentMsg.getData().asText());
					parentMsg.setData(node);
				} else {
					parentMsg.setData(null);
				}
			} catch (IOException e) {
				logger.warn("Failed to map ParentMessage JSON to ChatMessage.class. Proc USP_InsertGroupChat returned "
						+ ParentMsg, e);
				e.printStackTrace();
			}
		}
		this.readDate = readDate;
		this.memberReadDate = MemberReadDate;
	}

	public GroupChat(Integer senderId, Integer groupId, String text, String data, Byte chatMessageType,
			Long createdDate, Integer memberId, Byte eventNotificationType) {
		super();
		this.senderId = senderId;
		this.groupId = groupId;
		this.text = text;
		this.data = data;
		this.chatMessageType = chatMessageType;
		this.createdDate = createdDate;
		this.active = true;
		this.memberId = memberId;
		this.eventNotificationType = eventNotificationType;
	}

	public Long getId() {
		return Id;
	}

	@JsonIgnore
	public void setId(Long id) {
		Id = id;
	}

	public Integer getSenderId() {
		return senderId;
	}

	public void setSenderId(Integer senderId) {
		this.senderId = senderId;
	}

	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public String getText() {
		// TODO change this to process DATA and create text
		if (text == null && data != null && chatMessageType == ChatMessageType.SystemMessage.getId()) {
			text = data;
		}
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Byte getChatMessageType() {
		return chatMessageType;
	}

	public void setChatMessageType(Byte chatMessageType) {
		this.chatMessageType = chatMessageType;
	}

	public Long getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}

	public JsonNode getDataJson() {
		return dataJson;
	}

	public void setDataJson(JsonNode dataJson) {
		this.dataJson = dataJson;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public Integer getMemberId() {
		return memberId;
	}

	public void setMemberId(Integer memberId) {
		this.memberId = memberId;
	}

	public Byte getEventNotificationType() {
		return eventNotificationType;
	}

	public void setEventNotificationType(Byte eventNotificationType) {
		this.eventNotificationType = eventNotificationType;
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

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public String getReadDateStr() {
		return readDateStr;
	}

	public void setReadDateStr(String readDateStr) {
		this.readDateStr = readDateStr;
	}

	public Integer getDateDiff() {
		return dateDiff;
	}

	public void setDateDiff(Integer dateDiff) {
		this.dateDiff = dateDiff;
	}

	public Long getMemberReadDate() {
		return memberReadDate;
	}

	public void setMemberReadDate(Long memberReadDate) {
		this.memberReadDate = memberReadDate;
	}

	public String getExcludedRecipients() {
		return excludedRecipients;
	}

	public void setExcludedRecipients(String excludedRecipients) {
		this.excludedRecipients = excludedRecipients;
	}

	public Integer getGroupType() {
		return groupType;
	}

	public void setGroupType(Integer groupType) {
		this.groupType = groupType;
	}

	public Boolean getShowText() {
		return showText;
	}

	public void setShowText(Boolean showText) {
		this.showText = showText;
	}	
	
}
