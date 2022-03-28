package core.entities;

import java.io.IOException;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.daos.impl.BroadcastMessageDaoImpl;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage implements Serializable {
	
	final static Logger logger = LoggerFactory.getLogger(ChatMessage.class);
	
	private static final long serialVersionUID = 1L;

	private String cid;
	private Integer type;
	private Integer subtype;
	private Integer to;
	private String text;
	private Integer from;
	private String name;
	private String groupName;
	private Long mid;
	private String date;
	private String uuid;
	private Boolean status = false;
	private JsonNode data;
	protected Boolean active;
	private Integer videoCallMessageType;
	private String excludedRecipients;

	// Added for sync chat history
	private Integer chatType;
	private Integer chatMessageType;
	private Long utcDate;
	private Long deliveryDate;
	private Long readDate;
	private Long memberReadDate;
	private String readDateStr;
	private Long parentMsgId;
	private ChatMessage parentMsg;
	private Integer	 parentGrpId;
	private Integer  grpCreatorId;
	
	
	//added for videoKyc
	private Byte groupType;
	
	//private Integer meetingId;


	// Added for sync chat history
	public ChatMessage(String text, Long utcDate, Long mid, Integer from, Integer to, Integer chatType,
			Integer chatMessageType, String data, Long deliveryDate, Long readDate, Long ParentMsgId, String ParentMsg,
			Long memberReadDate, Boolean active) {
		super();
		this.text = text;
		this.utcDate = utcDate;
		this.mid = mid;
		this.from = from;
		this.to = to;
		this.chatType = chatType;
		this.chatMessageType = chatMessageType;
		this.deliveryDate = deliveryDate;
		this.readDate = readDate;
		this.memberReadDate = memberReadDate;
		if (data != null && !data.isEmpty()) {
			this.data = Json.parse(data);
		}
		this.parentMsgId = ParentMsgId;
		this.active = active;
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

	public ChatMessage(Integer chatType, Integer from, Integer to, Long mid, Long readDate) {
		this.mid = mid;
		this.from = from;
		this.to = to;
		this.chatType = chatType;
		this.readDate = readDate;
	}

	public ChatMessage() {

	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Integer getSubtype() {
		return subtype;
	}

	public void setSubtype(Integer subtype) {
		this.subtype = subtype;
	}

	public Integer getTo() {
		return to;
	}

	public void setTo(Integer to) {
		this.to = to;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Integer getFrom() {
		return from;
	}

	public void setFrom(Integer from) {
		this.from = from;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getMid() {
		return mid;
	}

	public void setMid(Long mid) {
		this.mid = mid;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	public JsonNode getData() {
		return data;
	}

	public void setData(JsonNode data) {
		this.data = data;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public Integer getChatType() {
		return chatType;
	}

	public void setChatType(Integer chatType) {
		this.chatType = chatType;
	}

	public Integer getChatMessageType() {
		return chatMessageType;
	}

	public void setChatMessageType(Integer chatMessageType) {
		this.chatMessageType = chatMessageType;
	}

	public Long getUtcDate() {
		return utcDate;
	}

	public void setUtcDate(Long utcDate) {
		this.utcDate = utcDate;
	}

	public Long getDeliveryDate() {
		return deliveryDate;
	}

	public void setDeliveryDate(Long deliveryDate) {
		this.deliveryDate = deliveryDate;
	}

	public Long getReadDate() {
		return readDate;
	}

	public void setReadDate(Long readDate) {
		this.readDate = readDate;
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

	public String getReadDateStr() {
		return readDateStr;
	}

	public void setReadDateStr(String readDateStr) {
		this.readDateStr = readDateStr;
	}

	public Long getMemberReadDate() {
		return memberReadDate;
	}

	public void setMemberReadDate(Long memberReadDate) {
		this.memberReadDate = memberReadDate;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Integer getVideoCallMessageType() {
		return videoCallMessageType;
	}

	public void setVideoCallMessageType(Integer videoCallType) {
		this.videoCallMessageType = videoCallType;
	}

	public String getExcludedRecipients() {
		return excludedRecipients;
	}

	public void setExcludedRecipients(String excludeRecipients) {
		this.excludedRecipients = excludeRecipients;
	}	

	public Integer getParentGrpId() {
		return parentGrpId;
	}

	public void setParentGrpId(Integer parentGrpId) {
		this.parentGrpId = parentGrpId;
	}
	
	public Integer getGrpCreatorId() {
		return grpCreatorId;
	}

	public void setGrpCreatorId(Integer grpCreatorId) {
		this.grpCreatorId = grpCreatorId;
	}	

	public Byte getGroupType() {
		return groupType;
	}

	public void setGroupType(Byte groupType) {
		this.groupType = groupType;
	}

	@Override
	public String toString() {
		return "ChatMessage [cid=" + cid + ", type=" + type + ", subtype=" + subtype + ", to=" + to + ", text=" + text
				+ ", from=" + from + ", name=" + name + ", mid=" + mid + ", date=" + date + ", uuid=" + uuid
				+ ", status=" + status + ", data=" + data + "]";
	}
}
