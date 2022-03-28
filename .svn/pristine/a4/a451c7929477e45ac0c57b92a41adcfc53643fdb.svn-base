/**
 * 
 */
package core.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

import org.hibernate.annotations.Type;


@NamedStoredProcedureQueries({
	@NamedStoredProcedureQuery(name = "BulkMemoDump.GetMemoDump", procedureName = "USP_GetMemoDump",
			resultSetMappings = {"GetMemoDumpResultSet" }),
	@NamedStoredProcedureQuery(name = "BulkMemoDump.InserBulkMemo", procedureName = "USP_InsertBulkMemo", parameters = {			
			@StoredProcedureParameter(name = "P_OrganizationId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_UserId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_MemoObject", type = String.class, mode = ParameterMode.IN)}, resultSetMappings = {
	"InsertMemoResultSetMapping" })})

@SqlResultSetMappings({ @SqlResultSetMapping(name = "GetMemoDumpResultSet", classes = {
		@ConstructorResult(targetClass = core.entities.BulkMemoDump.class, columns = {
				@ColumnResult(name = "Id", type = Integer.class),
				@ColumnResult(name = "organizationId", type = Integer.class),			
				@ColumnResult(name = "subject", type = String.class),
				@ColumnResult(name = "message", type = String.class),
				@ColumnResult(name = "snippet", type = String.class),
				@ColumnResult(name = "attachments", type = String.class),
				@ColumnResult(name = "fileName", type = String.class),
				@ColumnResult(name = "filePath", type = String.class),
				@ColumnResult(name = "uploadType", type = Byte.class),
				@ColumnResult(name = "isPublic", type = Boolean.class),
				@ColumnResult(name = "CreatedById", type = Integer.class),
				@ColumnResult(name = "totalMemoSent", type = Integer.class),
				@ColumnResult(name = "showUserDetailOnSCP", type = Boolean.class)}) }),


	@SqlResultSetMapping(name = "InsertMemoResultSetMapping", classes = {
			@ConstructorResult(targetClass = core.entities.BulkMemoDump.class, columns = {
					@ColumnResult(name = "Id", type = Integer.class),							
					@ColumnResult(name = "subject", type = String.class),
					@ColumnResult(name = "createdById", type = Integer.class),
					@ColumnResult(name = "firstName", type = String.class),
					@ColumnResult(name = "recipientIds", type = String.class),
					@ColumnResult(name = "memoType", type = Byte.class),})
	}) })

@NamedQueries({
	@NamedQuery(name = "BulkMemoDump.UpdateBulkMemoStatus", query = "UPDATE BulkMemoDump b SET b.status = :status, b.updatedDate = :updatedDate WHERE b.Id = :bulkMemoDumpId AND b.active = TRUE"),
	@NamedQuery(name = "BulkMemoDump.UpdateMemoSentCount", query = "UPDATE BulkMemoDump b SET b.totalMemoSent = totalMemoSent +:memoSentCount, b.updatedDate = :updatedDate WHERE b.Id = :bulkMemoDumpId AND b.active = TRUE")})
@Entity
@Table(name = "bulk_memo_dump")
public class BulkMemoDump extends BaseEntity {
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer Id;
	@Column
	private Integer organizationId;
	@Column
	private String subject;
	@Column(name = "Message")
	@Type(type = "text")
	private String message;
	@Column	
	private String snippet;
	@Type(type = "text")
	private String attachments;
	@Column
	private String fileName;
	@Column
	private String filePath;
	@Column
	private Byte uploadType;
	@Column
	private Boolean isPublic;
	@Column
	private Byte status;
	@Column
	private Long createdDate;
	@Column
	private Integer createdById;
	@Column
	private Long updatedDate;
	@Column
	private Integer totalMemoSent;
	@Column
	private Boolean showUserDetailOnSCP;
	@Transient
	private Set<Integer> inputRecipientIds;
	@Transient
	private String firstName;
	@Transient
	private Byte memoType;

	public BulkMemoDump() {
	}

	public BulkMemoDump(Integer Id, Integer organizationId, String subject, String message, String snippet, String attachments, String fileName,
			String filePath, Byte uploadType, Boolean isPublic, Integer createdById, Integer totalMemoSent, Boolean showUserDetailOnSCP) {
		this.Id=Id;
		this.organizationId=organizationId;
		this.subject=subject;
		this.message=message;
		this.snippet=snippet;
		this.attachments=attachments;
		this.fileName=fileName;
		this.filePath=filePath;
		this.uploadType=uploadType;
		this.isPublic=isPublic;
		this.createdById=createdById;
		this.totalMemoSent=totalMemoSent;
		this.showUserDetailOnSCP=showUserDetailOnSCP;
	}

	public BulkMemoDump(Integer Id, String subject, Integer createdById, String firstName, String recipientIds, Byte memoType) {
		this.Id=Id;
		this.subject=subject;
		this.createdById=createdById;		
		this.firstName=firstName;
	//	this.recipientIds=recipientIds;
		if (recipientIds != null && !recipientIds.isEmpty()) {
			List<String> receiptentList = Arrays.asList(recipientIds.split(","));
			this.inputRecipientIds = receiptentList.stream().map(s -> Integer.parseInt(s)).collect(Collectors.toSet());
		} 
		this.memoType=memoType;
	}

	public Integer getId() {
		return Id;
	}
	public void setId(Integer id) {
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
	public String getSnippet() {
		return snippet;
	}
	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public Byte getUploadType() {
		return uploadType;
	}
	public void setUploadType(Byte uploadType) {
		this.uploadType = uploadType;
	}
	public Boolean getIsPublic() {
		return isPublic;
	}
	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}
	public Byte getStatus() {
		return status;
	}
	public void setStatus(Byte status) {
		this.status = status;
	}
	public Long getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}
	public Integer getCreatedById() {
		return createdById;
	}
	public void setCreatedById(Integer createdById) {
		this.createdById = createdById;
	}
	public Long getUpdatedDate() {
		return updatedDate;
	}
	public void setUpdatedDate(Long updatedDate) {
		this.updatedDate = updatedDate;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Integer getOrganizationId() {
		return organizationId;
	}
	public void setOrganizationId(Integer organizationId) {
		this.organizationId = organizationId;
	}
	public String getAttachments() {
		return attachments;
	}
	public void setAttachments(String attachments) {
		this.attachments = attachments;
	}
	public Set<Integer> getRecipientIds() {
		return inputRecipientIds;
	}
	public void setRecipientIds(Set<Integer> inputRecipientIds) {
		this.inputRecipientIds = inputRecipientIds;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public Integer getTotalMemoSent() {
		return totalMemoSent;
	}

	public void setTotalMemoSent(Integer totalMemoSent) {
		this.totalMemoSent = totalMemoSent;
	}

	public final Byte getMemoType() {
		return memoType;
	}

	public final void setMemoType(Byte memoType) {
		this.memoType = memoType;
	}

	public final Boolean getShowUserDetailOnSCP() {
		return showUserDetailOnSCP;
	}

	public final void setShowUserDetailOnSCP(Boolean showUserDetailOnSCP) {
		this.showUserDetailOnSCP = showUserDetailOnSCP;
	}
	
	
	
}
