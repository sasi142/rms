package core.entities;

import java.io.IOException;

import javax.persistence.Basic;
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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity
@Table(name = "One2One_Chat")
@NamedQueries({
	@NamedQuery(name = "One2OneChat.GetChatHistory", query = "select u from One2OneChat u where u.active=true AND u.createdDate < :lastMsgDate AND  "
			+ "((u.from=:from AND u.to=:to) OR (u.from=:to AND u.to=:from)) ORDER BY u.createdDate DESC"),

	@NamedQuery(name = "One2OneChat.UpdateUnReadChatById", query = "Update One2OneChat ou set ou.status=true,ou.lastUpdated=:time "
			+ "where ou.active=true AND ou.to=:to AND ou.Id=:Id"),
	@NamedQuery(name = "One2OneChat.GetLastMsgIdBySender", query = "select MAX(Id) AS lastMsgId "
			+ "from One2OneChat c WHERE c.from = :senderId and c.to = :recipientId AND c.readDate IS NULL AND c.active = TRUE"),
	@NamedQuery(name = "One2OneChat.GetMsgById", query = "select u from One2OneChat u where u.active=true AND u.Id = :msgId AND u.from = :senderId") })
@NamedStoredProcedureQueries({
	@NamedStoredProcedureQuery(name = "One2OneChat.InsertOne2oneChat", procedureName = "USP_InsertOne2onechat", parameters = {
			@StoredProcedureParameter(name = "P_Sender", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_Recipient", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_Message", type = String.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_Data", type = String.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_CreatedDate", type = Long.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_ParentMessageId", type = Long.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_MessageType", type = Byte.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_NotRegisteredSysMsgChkDuration", type = Long.class, mode = ParameterMode.IN) }, resultSetMappings = {
	"InsertOne2oneChatResultMappings" }),
	@NamedStoredProcedureQuery(name = "GetContactCountToSync", procedureName = "USP_GetChatContacts", parameters = {
			@StoredProcedureParameter(name = "P_OrganizationId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_UserId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_SyncDates", type = String.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_IsFirstTimeSync", type = Boolean.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_Offset", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_Count", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_CountOnly", type = Boolean.class, mode = ParameterMode.IN) }) })
@SqlResultSetMappings({ @SqlResultSetMapping(name = "InsertOne2oneChatResultMappings", classes = {
		@ConstructorResult(targetClass = core.entities.One2OneChat.class, columns = {
				@ColumnResult(name = "Id", type = Long.class), @ColumnResult(name = "Active", type = Boolean.class),
				@ColumnResult(name = "CreatedDate", type = Long.class),
				@ColumnResult(name = "Sender", type = Integer.class),
				@ColumnResult(name = "LastUpdated", type = Long.class),
				@ColumnResult(name = "Status", type = Boolean.class),
				@ColumnResult(name = "Message", type = String.class),
				@ColumnResult(name = "Recipient", type = Integer.class),
				@ColumnResult(name = "Data", type = String.class),
				@ColumnResult(name = "ParentMessageId", type = Long.class),
				@ColumnResult(name = "DeliveredDate", type = Long.class),
				@ColumnResult(name = "ReadDate", type = Long.class),
				@ColumnResult(name = "ParentMsg", type = String.class),
				@ColumnResult(name = "MessageType", type = Byte.class) }) }) })
@JsonInclude(Include.NON_NULL)
public class One2OneChat extends BaseEntity {

	final static Logger logger = LoggerFactory.getLogger(One2OneChat.class);

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonProperty("mid")
	protected Long Id;

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

	@Transient
	private String date;

	@Column(name = "sender")
	private Integer from;

	@Column(name = "recipient")
	private Integer to;

	@Column
	@JsonIgnore
	private Long lastUpdated;

	@Basic
	@Column(name = "status", columnDefinition = "BIT", length = 1)
	private Boolean status;

	@Column(name = "ParentMessageId")
	@JsonProperty
	private Long parentMsgId;

	@Column
	private Long readDate;

	@Transient
	@JsonProperty("data")
	private JsonNode dataJson;

	@Transient
	private ChatMessage parentMsg;

	@Transient
	private String readDateStr;

	@Transient
	@JsonProperty("name")
	private String				fromName;
	
	@Transient
	private Integer 			dateDiff;
	
	@Transient
	private Boolean				showText              = false;

	public One2OneChat() {

	}

	// InsertOne2oneChatResultMappings
	public One2OneChat(Long Id, Boolean Active, Long CreatedDate, Integer Sender, Long LastUpdated, Boolean Status,
			String Message, Integer Recipient, String Data, Long ParentMessageId, Long DeliveredDate, Long ReadDate,
			String ParentMsg, Byte MessageType) {
		super();
		this.Id = Id;
		this.active = Active;
		this.createdDate = CreatedDate;
		this.from = Sender;
		this.lastUpdated = LastUpdated;
		this.status = Status;
		this.text = Message;
		this.to = Recipient;
		this.data = Data;
		this.chatMessageType = MessageType;
		this.parentMsgId = ParentMessageId;
		// DeliveredDate
		this.readDate = ReadDate;
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
				logger.warn(
						"Failed to map ParentMessage JSON to ChatMessage.class. Proc USP_InsertOne2onechat returned "
								+ ParentMsg,
								e);
				e.printStackTrace();
			}
		}
	}

	public Long getId() {
		return Id;
	}

	@JsonProperty("mid")
	public void setId(Long id) {
		Id = id;
	}

	public String getText() {
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

	public Integer getFrom() {
		return from;
	}

	public void setFrom(Integer from) {
		this.from = from;
	}

	public Integer getTo() {
		return to;
	}

	public void setTo(Integer to) {
		this.to = to;
	}

	public Long getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Long lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	public JsonNode getDataJson() {
		return dataJson;
	}

	public void setDataJson(JsonNode dataJson) {
		this.dataJson = dataJson;
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

	public String getReadDateStr() {
		return readDateStr;
	}

	public void setReadDateStr(String readDateStr) {
		this.readDateStr = readDateStr;
	}

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public Integer getDateDiff() {
		return dateDiff;
	}

	public void setDateDiff(Integer dateDiff) {
		this.dateDiff = dateDiff;
	}
	
	public Boolean getShowText() {
		return showText;
	}

	public void setShowText(Boolean showText) {
		this.showText = showText;
	}
	
	
}
