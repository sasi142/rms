package core.entities;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/*@Entity
 @Table(name = "broadcast_recipient")
 @NamedQueries({
 @NamedQuery(name = "BroadcastRecipient.getUnreadMessageCount", query= "select count(1) from BroadcastRecipient br where br.active=true and br.userId=:id and br.readFlag=false")
 })*/
@JsonInclude(Include.NON_NULL)
public class BroadcastRecipient extends BaseEntity {

	private static final long	serialVersionUID	= 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonProperty("Id")
	protected Long				Id;

	@Column(name = "BroadcastMessageId")
	protected Long				broadcastMessageId;

	@Column(name = "UserId")
	private Integer				userId;

	@Column(name = "readFlag", columnDefinition = "BIT", length = 1)
	private Boolean				readFlag;

	public Long getId() {
		return Id;
	}

	public void setId(Long id) {
		Id = id;
	}

	public Long getBroadcastMessageId() {
		return broadcastMessageId;
	}

	public void setBroadcastMessageId(Long broadcastMessageId) {
		this.broadcastMessageId = broadcastMessageId;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Boolean getReadFlag() {
		return readFlag;
	}

	public void setReadFlag(Boolean readFlag) {
		this.readFlag = readFlag;
	}

}
