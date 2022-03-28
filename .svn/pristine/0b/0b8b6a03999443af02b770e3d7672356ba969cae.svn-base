package core.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class MessageReadInfo {
	private Long	msgId;
	private Integer	userId;
	private Long	readDate	= 0L;
	private Byte	status;
	private String	readDateStr;

	public MessageReadInfo() {
	}

	public MessageReadInfo(Integer userId, Long readDate, Byte status) {
		this.userId = userId;
		this.readDate = (readDate != null ? readDate : 0L);
		this.status = status;
	}

	public void setMsgId(Long msgId) {
		this.msgId = msgId;
	}

	public Long getMsgId() {
		return msgId;
	}

	public Integer getUserId() {
		return userId;
	}

	public Long getReadDate() {
		return readDate;
	}

	public Byte getStatus() {
		return status;
	}

	public String getReadDateStr() {
		return readDateStr;
	}

	public void setReadDateStr(String readDateStr) {
		this.readDateStr = readDateStr;
	}
}
