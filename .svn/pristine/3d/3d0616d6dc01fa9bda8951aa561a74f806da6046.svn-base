/**
 * 
 */
package core.entities;

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

@Entity


@NamedStoredProcedureQueries({

	@NamedStoredProcedureQuery(name = "Memo.GetMemoChatList", procedureName = "USP_GetMemoChatList", parameters = {
			@StoredProcedureParameter(name = "P_MemoId", type = Integer.class, mode = ParameterMode.IN),		
			@StoredProcedureParameter(name="P_Offset", type=Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name="P_count", type=Integer.class, mode = ParameterMode.IN)},resultSetMappings = {"MemoChatListMapping"}),

})

@SqlResultSetMappings({

	//for follow
	@SqlResultSetMapping(name = "MemoChatListMapping", classes = { @ConstructorResult(targetClass = core.entities.MemoChatUser.class, columns = {
			@ColumnResult(name = "Id", type = Integer.class), @ColumnResult(name = "ChatType", type = Byte.class),
			@ColumnResult(name = "EntityId", type = Integer.class), @ColumnResult(name = "Name", type = String.class),
			@ColumnResult(name = "PhotoURL", type = String.class)})})	
})

@Table(name = "memo_chat_user")
public class MemoChatUser extends BaseEntity {

	private static final long	serialVersionUID	= 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer				Id;
	@Column
	private Integer				memoId;
	@Column
	private Long				createdDate;
	@Column
	private Byte 				chatType;
	@Column
	private Integer				entityId;
	@Transient
	private String 				name;
	@Transient
	private String              photoURL;



	public MemoChatUser() {
		super();
	}	

	//Memo chat user for get chatlist for follow
	public MemoChatUser(Integer memoId, Byte chatType, Integer entityId, String name, String photoURL) {		   
		super();
		this.memoId = memoId;			
		this.chatType = chatType;
		this.entityId = entityId;
		this.name = name;
		this.photoURL = photoURL;			
	}	

	public Integer getId() {
		return Id;
	}

	public void setId(Integer id) {
		Id = id;
	}

	public Integer getMemoId() {
		return memoId;
	}

	public void setMemoId(Integer memoId) {
		this.memoId = memoId;
	}

	public Byte getChatType() {
		return chatType;
	}

	public void setChatType(Byte chatType) {
		this.chatType = chatType;
	}

	public Integer getEntityId() {
		return entityId;
	}

	public void seEntityId(Integer entityId) {
		this.entityId = entityId;
	}

	public Long getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhotoURL() {
		return photoURL;
	}

	public void setPhotoURL(String photoURL) {
		this.photoURL = photoURL;
	}					
}
