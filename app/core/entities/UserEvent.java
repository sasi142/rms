package core.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_event")
public class UserEvent extends BaseEntity {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Integer Id;

	@Column
	private Integer GroupId;

	@Column
	private Integer userId;

	@Column
	private Byte eventType;
	
	@Column
	private Byte eventSource;
	
	@Column
	private String data;

	@Column
	private Long CreatedDate;

	public Integer getId() {
		return Id;
	}

	public void setId(Integer id) {
		Id = id;
	}

	public Integer getGroupId() {
		return GroupId;
	}

	public void setGroupId(Integer groupId) {
		GroupId = groupId;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Byte getEventType() {
		return eventType;
	}

	public void setEventType(Byte eventType) {
		this.eventType = eventType;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Long getCreatedDate() {
		return CreatedDate;
	}

	public void setCreatedDate(Long createdDate) {
		CreatedDate = createdDate;
	}

	public Byte getEventSource() {
		return eventSource;
	}

	public void setEventSource(Byte eventSource) {
		this.eventSource = eventSource;
	}

	@Override
	public String toString() {
		return "UserEvent [Id=" + Id + ", GroupId=" + GroupId + ", userId=" + userId + ", eventType=" + eventType
				+ ", eventSource=" + eventSource + ", data=" + data + ", CreatedDate=" + CreatedDate + "]";
	}	
	
}
