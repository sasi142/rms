package core.entities;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class BroadcastMessage extends BaseEntity {
	private static final long	serialVersionUID	= 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonProperty("Id")
	protected Long				Id;

	@Column(name = "Subject")
	@JsonProperty("subject")
	private String				subject;

	@Column(name = "Message")
	@JsonProperty("text")
	private String				message;

	@Column(name = "CreatedById")
	private Integer				createdById;

	@JsonProperty("Date")
	@Column(name = "CreatedDate")
	private Long				createdDate;

	@Transient
	@JsonProperty("readFlag")
	private Boolean				readFlag;

	// TODO : set this value in service
	@Transient
	@JsonProperty("senderName")
	private Boolean				senderName;

	@Transient
	@JsonProperty("messageType")
	private Boolean				messageType;

	@Transient
	@JsonProperty("recipientId")
	private Integer				recipientId;

	/*
	 * @Basic
	 * @Column(name = "Active", columnDefinition = "BIT", length = 1) private
	 * Boolean active;
	 */

	public Long getId() {
		return Id;
	}

	public void setId(Long id) {
		Id = id;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Integer getCreatedById() {
		return createdById;
	}

	public void setCreatedById(Integer createdById) {
		this.createdById = createdById;
	}

	public Long getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}

	public Boolean getReadFlag() {
		return readFlag;
	}

	public void setReadFlag(Boolean readFlag) {
		this.readFlag = readFlag;
	}

	public Boolean getSenderName() {
		return senderName;
	}

	public void setSenderName(Boolean senderName) {
		this.senderName = senderName;
	}

	public Boolean getMessageType() {
		return messageType;
	}

	public void setMessageType(Boolean messageType) {
		this.messageType = messageType;
	}

	public Integer getRecipientId() {
		return recipientId;
	}

	public void setRecipientId(Integer recipientId) {
		this.recipientId = recipientId;
	}


}
