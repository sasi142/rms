/**
 * 
 */
package core.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@NamedQueries({
	@NamedQuery(name = "MemoRecipient.ChangeReadStatus", query = "UPDATE MemoRecipient mr SET mr.readFlag = :readFlag, mr.updatedDate = :updatedDate WHERE mr.memoId = :memoId AND mr.userId = :userId AND mr.active = TRUE"),
	@NamedQuery(name = "MemoRecipient.IsReceipient", query = "Select count(1) from  MemoRecipient mr WHERE mr.memoId = :memoId AND mr.userId = :userId AND mr.active = TRUE"),	
	@NamedQuery(name = "MemoRecipient.GetMemoCountByStatus", query = "SELECT COUNT(1) FROM MemoRecipient mr WHERE mr.userId = :userId AND mr.readFlag = :readFlag AND mr.active = TRUE") })
@Table(name = "memo_recipient")
public class MemoRecipient extends BaseEntity {

	private static final long	serialVersionUID	= 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long				Id;

	@Column
	private Integer				memoId;
	@Column
	private Integer				userId;
	@Column
	private Boolean				readFlag;
	@Column
	private Long				updatedDate;

	@Transient
	private User				user;

	public MemoRecipient() {

	}

	public Long getId() {
		return Id;
	}

	public void setId(Long id) {
		Id = id;
	}

	public Integer getMemoId() {
		return memoId;
	}

	public void setMemoId(Integer memoId) {
		this.memoId = memoId;
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

	public Long getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(Long updatedDate) {
		this.updatedDate = updatedDate;
	}

	public void setUser(User memoUser) {
		this.user = memoUser;
	}

	public User getUser() {
		return user;
	}
}
