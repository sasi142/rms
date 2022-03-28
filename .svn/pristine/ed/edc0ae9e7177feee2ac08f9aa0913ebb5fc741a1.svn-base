/**
 * 
 */
package core.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import core.utils.CommonUtil;

@Entity

@NamedStoredProcedureQueries({


	@NamedStoredProcedureQuery(name = "Memo.GetMemoPublicPage", procedureName = "USP_GetMemoByPublicURL", parameters = {
			@StoredProcedureParameter(name = "P_PublicURL", type = String.class, mode = ParameterMode.IN) }, resultSetMappings = {
	"MemoPublicPageMappings" }),

	@NamedStoredProcedureQuery(name = "Memo.GetMemoList", procedureName = "USP_GetMemoList", parameters = {
			@StoredProcedureParameter(name = "P_UserId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_ChannelId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_Offset", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_count", type = Integer.class, mode = ParameterMode.IN) }, resultSetMappings = {
	"MemoListMappings" }),

	@NamedStoredProcedureQuery(name = "Memo.GetMemoById", procedureName = "USP_GetMemoById", parameters = {
			@StoredProcedureParameter(name = "P_UserId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_MemoId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_NeedSummay", type = Boolean.class, mode = ParameterMode.IN) }, resultSetMappings = {
	"MemoByIdMapping" }),
	
	
	
	@NamedStoredProcedureQuery(name = "Memo.GetCustomMemoReport", procedureName = "USP_GetCustomMemoReport", parameters = {
			@StoredProcedureParameter(name = "P_MemoId", type = Integer.class, mode = ParameterMode.IN) }),
	
	@NamedStoredProcedureQuery(name = "Memo.GetRegularMemoReport", procedureName = "USP_GetMemoReadReport", parameters = {
			@StoredProcedureParameter(name = "P_MemoId", type = Integer.class, mode = ParameterMode.IN) }),
	
	@NamedStoredProcedureQuery(name = "Memo.GetMemoDetailsById", procedureName = "USP_GetMemoDetailById", parameters = {
			@StoredProcedureParameter(name = "P_MemoId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_UserId", type = Integer.class, mode = ParameterMode.IN),			
			@StoredProcedureParameter(name = "P_NeedSummay", type = Boolean.class, mode = ParameterMode.IN) }, resultSetMappings = {
	"GetMemoDetailsResultMapping" }),

	@NamedStoredProcedureQuery(name = "Memo.CreateMemo", procedureName = "USP_InsertMemo", parameters = {
			@StoredProcedureParameter(name = "P_UserId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_OrganizationId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_Subject", type = String.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_Message", type = String.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_RecipientIds", type = String.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_GroupIds", type = String.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_AttachmentIds", type = String.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_IsPublic", type = Boolean.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_PublicURL", type = String.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_ChannelId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_Snippet", type = String.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_SendAs", type = Byte.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_One2OneChatIds", type = String.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_GroupChatIds", type = String.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_UploadId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_ShowUserDetailOnSCP", type = Boolean.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_MemoType", type = Byte.class, mode = ParameterMode.IN)}, resultSetMappings = {"CreateMemoResultMappings" }) })

@SqlResultSetMappings({

	// for follow

	@SqlResultSetMapping(name = "MemoPublicPageMappings", classes = {
			@ConstructorResult(targetClass = core.entities.Memo.class, columns = {
					@ColumnResult(name = "Id", type = Integer.class),
					@ColumnResult(name = "OrganizationId", type = Integer.class),
					@ColumnResult(name = "Subject", type = String.class),
					@ColumnResult(name = "Message", type = String.class),
					@ColumnResult(name = "Snippet", type = String.class),
					@ColumnResult(name = "IsPublic", type = Boolean.class),
					@ColumnResult(name = "PublicURL", type = String.class),
					@ColumnResult(name = "PageViews", type = Integer.class),
					@ColumnResult(name = "CreatedById", type = Integer.class),
					@ColumnResult(name = "CreatedDate", type = Long.class),
					@ColumnResult(name = "UpdatedDate", type = Long.class),
					@ColumnResult(name = "ChannelId", type = Integer.class),
					@ColumnResult(name = "Active", type = Boolean.class),
					@ColumnResult(name = "Attachment", type = String.class),
					@ColumnResult(name = "channelName", type = String.class),
					@ColumnResult(name = "channelCategory", type = String.class),
					@ColumnResult(name = "IconUrl", type = String.class) }) }),

	@SqlResultSetMapping(name = "MemoListMappings", classes = {
			@ConstructorResult(targetClass = core.entities.Memo.class, columns = {
					@ColumnResult(name = "Id", type = Integer.class),
					@ColumnResult(name = "Subject", type = String.class),
					@ColumnResult(name = "CreatedById", type = Integer.class),
					@ColumnResult(name = "FirstName", type = String.class),
					@ColumnResult(name = "Email", type = String.class),
					@ColumnResult(name = "PhotoURL", type = String.class),
					@ColumnResult(name = "CreatedDate", type = Long.class),
					@ColumnResult(name = "Active", type = Boolean.class),
					@ColumnResult(name = "ReadFlag", type = Boolean.class),
					@ColumnResult(name = "Snippet", type = String.class),
					@ColumnResult(name = "Attachment", type = String.class),
					@ColumnResult(name = "ChannelId", type = Integer.class),
					@ColumnResult(name = "ChannelName", type = String.class),
					@ColumnResult(name = "IconUrl", type = String.class),
					@ColumnResult(name = "LogoURL", type = String.class) }) }),
	// for follow
	@SqlResultSetMapping(name = "MemoByIdMapping", classes = {
			@ConstructorResult(targetClass = core.entities.Memo.class, columns = {
					@ColumnResult(name = "Id", type = Integer.class),
					@ColumnResult(name = "Subject", type = String.class),
					@ColumnResult(name = "Message", type = String.class),
					@ColumnResult(name = "CreatedById", type = Integer.class),
					@ColumnResult(name = "CreatedDate", type = Long.class),
					@ColumnResult(name = "OrganizationId", type = Integer.class),
					@ColumnResult(name = "Active", type = Boolean.class),
					@ColumnResult(name = "IsPublic", type = Boolean.class),
					@ColumnResult(name = "PublicURL", type = String.class),
					@ColumnResult(name = "TotalRecipient", type = Integer.class),
					@ColumnResult(name = "ReadCount", type = Integer.class),
					@ColumnResult(name = "UnReadCount", type = Integer.class),
					@ColumnResult(name = "ReadPercent", type = Float.class),
					@ColumnResult(name = "ReadFlag", type = Boolean.class),
					@ColumnResult(name = "ChannelId", type = Integer.class),
					@ColumnResult(name = "ChannelName", type = String.class),
					@ColumnResult(name = "IconUrl", type = String.class),
					@ColumnResult(name = "IsChatEnabled", type = Boolean.class),
					@ColumnResult(name = "ChannelCategory", type = String.class) }) }),

	@SqlResultSetMapping(name = "CreateMemoResultMappings", classes = {
			@ConstructorResult(targetClass = core.entities.Memo.class, columns = {
					@ColumnResult(name = "Id", type = Integer.class),
					@ColumnResult(name = "Subject", type = String.class),
					@ColumnResult(name = "Message", type = String.class),
					@ColumnResult(name = "CreatedById", type = Integer.class),
					@ColumnResult(name = "CreatedDate", type = Long.class),
					@ColumnResult(name = "OrganizationId", type = Integer.class),
					@ColumnResult(name = "Active", type = Boolean.class),
					@ColumnResult(name = "RecipientIds", type = String.class),
					@ColumnResult(name = "IsPublic", type = Boolean.class),
					@ColumnResult(name = "PublicURL", type = String.class),
					@ColumnResult(name = "PageViews", type = Integer.class),
					@ColumnResult(name = "ChannelId", type = Integer.class),
					@ColumnResult(name = "Snippet", type = String.class),
					@ColumnResult(name = "RecipientCount", type = Integer.class),
					@ColumnResult(name = "InvalidRecipientCount", type = Integer.class),
					@ColumnResult(name = "InvalidRecipientList", type = String.class),
					@ColumnResult(name = "MemoType", type = Byte.class)}) }),

	@SqlResultSetMapping(name = "Memo.GetMemosByOrgId", classes = {
			@ConstructorResult(targetClass = core.entities.Memo.class, columns = {
					@ColumnResult(name = "Id", type = Integer.class),
					@ColumnResult(name = "Subject", type = String.class),
					@ColumnResult(name = "CreatedById", type = Integer.class),
					@ColumnResult(name = "CreatedDate", type = Long.class),
					@ColumnResult(name = "OrganizationId", type = Integer.class),
					@ColumnResult(name = "Active", type = Boolean.class),
					@ColumnResult(name = "ReadPercent", type = Float.class) }) }),
	@SqlResultSetMapping(name = "Memo.GetMemosByUserId", classes = {
			@ConstructorResult(targetClass = core.entities.Memo.class, columns = {
					@ColumnResult(name = "Id", type = Integer.class),
					@ColumnResult(name = "Subject", type = String.class),
					@ColumnResult(name = "CreatedById", type = Integer.class),
					@ColumnResult(name = "CreatedDate", type = Long.class),
					@ColumnResult(name = "Active", type = Boolean.class),
					@ColumnResult(name = "ReadFlag", type = Boolean.class) }) }),
	@SqlResultSetMapping(name = "Memo.GetMemoDetails", classes = {
			@ConstructorResult(targetClass = core.entities.Memo.class, columns = {
					@ColumnResult(name = "Id", type = Integer.class),
					@ColumnResult(name = "Subject", type = String.class),
					@ColumnResult(name = "Message", type = String.class),
					@ColumnResult(name = "CreatedById", type = Integer.class),
					@ColumnResult(name = "CreatedDate", type = Long.class),
					@ColumnResult(name = "OrganizationId", type = Integer.class),
					@ColumnResult(name = "Active", type = Boolean.class),
					@ColumnResult(name = "IsPublic", type = Boolean.class),
					@ColumnResult(name = "PublicURL", type = String.class),				
					@ColumnResult(name = "TotalRecipient", type = Long.class),
					@ColumnResult(name = "ReadCount", type = Long.class),
					@ColumnResult(name = "UnReadCount", type = Long.class),
					@ColumnResult(name = "ReadPercent", type = Float.class),
					@ColumnResult(name = "ReadFlag", type = Boolean.class) }) }),
	
			@SqlResultSetMapping(name = "GetMemoDetailsResultMapping", classes = {
					@ConstructorResult(targetClass = core.entities.Memo.class, columns = {
							@ColumnResult(name = "Id", type = Integer.class),
							@ColumnResult(name = "Subject", type = String.class),
							@ColumnResult(name = "Message", type = String.class),
							@ColumnResult(name = "CreatedById", type = Integer.class),
							@ColumnResult(name = "CreatedDate", type = Long.class),
							@ColumnResult(name = "OrganizationId", type = Integer.class),
							@ColumnResult(name = "Active", type = Boolean.class),
							@ColumnResult(name = "IsPublic", type = Boolean.class),
							@ColumnResult(name = "PublicURL", type = String.class),
							@ColumnResult(name = "ShowUserDetailOnSCP", type = Boolean.class),
							@ColumnResult(name = "Snippet", type = String.class),
							@ColumnResult(name = "memoType", type = Byte.class),
							@ColumnResult(name = "TotalRecipient", type = Long.class),
							@ColumnResult(name = "ReadCount", type = Long.class),
							@ColumnResult(name = "UnReadCount", type = Long.class),
							@ColumnResult(name = "ReadPercent", type = Float.class),
							@ColumnResult(name = "ReadFlag", type = Boolean.class) }) }) })
@NamedQueries({
	@NamedQuery(name = "Memo.UpdatePublicState", query = "UPDATE Memo m SET m.isPublic = :isPublic, m.updatedDate = :updatedDate WHERE m.Id = :memoId AND m.active = TRUE"),
	@NamedQuery(name = "Memo.GetMemoByPublicURL", query = "SELECT m FROM Memo m WHERE m.publicURL = :url AND m.isPublic = TRUE AND m.active = TRUE"),
	@NamedQuery(name = "Memo.UpdatePageViews", query = "UPDATE Memo m SET m.pageViews = m.pageViews + 1, m.updatedDate = :updatedDate WHERE m.Id = :memoId AND m.active = TRUE") })
@Table(name = "memo")
public class Memo extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer Id;
	
	@Column
	private String subject;
	@Column(name = "Message")
	@Type(type = "text")
	private String message;
	@Column
	private Integer createdById;
	@Column
	private Long createdDate;
	@Column
	private Integer organizationId;
	@Column
	private Boolean isPublic;
	@Column
	private String publicURL;
	@Column
	private Integer pageViews;
	@Column
	private Long updatedDate;
	@Column
	private Byte memoType;
	@Transient
	private MemoSummary memoSummary;
	@Transient
	private List<Long> recipientIds;
	@Transient
	private Boolean sendToAll;
	@Transient
	private Float readPercent;
	@Transient
	private Boolean readFlag;
	@Transient
	private User creator;
	@Transient
	private List<Long> attachmentIds;
	@Transient
	private List<Integer> cityIds;
	@Transient
	private List<Integer> officeIds;
	@Transient
	private List<Integer> adGroupIds;
	@Transient
	private List<Integer> departmentIds;
	@Transient
	private List<String> designations;
	@Column
	private Integer channelId;
	@Column
	private Boolean showUserDetailOnSCP;
	@Column
	private String snippet;
	@Transient
	private String channelName;
	@Transient
	private String iconUrl;

	@Transient
	private Byte SendAs;
	@Transient
	private List<Integer> One2OneChatIds;
	@Transient
	private List<Integer> GroupChatIds;

	@Transient
	private List<Long> invalidReceiptentList;
	@Transient
	private Integer invalidReceiptentsCount;
	@Transient
	private Integer receiptentsCount;
	@Transient
	private Boolean isChatEnabled;
	@Transient
	private List<Attachment> attachments;
	@Transient
	private String channelCategory;
	@Transient
	private Integer uploadId;
	@Transient
	private String logoUrl;	
	@Transient
	private Integer memoDumpAttachmentId;
	@Transient
	private Boolean sendToAllPartner;	
	@Transient
	private Boolean sendToAllEmployeeAndPartner;
	@Transient
	private String sharedById;

	public Memo() {
	}

	// for publicPage
	public Memo(Integer id, Integer organizationId, String subject, String message, String snippet, Boolean isPublic,
			String publicURL, Integer pageViews, Integer createdById, Long createdDate, Long UpdatedDate,
			Integer channelId, Boolean active, String AttachmentDetails, String channelName, String channelCategory,
			String iconUrl) {

		super();
		Id = id;
		this.organizationId = organizationId;
		this.subject = subject;
		this.createdById = createdById;
		this.message = message;
		this.snippet = snippet;
		this.isPublic = isPublic;
		this.publicURL = publicURL;
		this.createdDate = createdDate;
		this.active = active;
		this.updatedDate = updatedDate;
		this.channelId = channelId;
		// this.attachmentDetails = AttachmentDetails;
		if (AttachmentDetails != null) {
			this.attachments = CommonUtil.getAttachments(AttachmentDetails);
		}
		this.channelName = channelName;
		this.channelCategory = channelCategory;
		this.iconUrl = iconUrl;
	}

	// added fname,photoUrl,email,Attachment Json,Snippet fields for follow
	// Used by Memo.GetMemoList V2 api
	public Memo(Integer id, String subject, Integer createdById, String FirstName, String Email, String PhotoURL,
			Long createdDate, Boolean active, Boolean ReadFlag, String snippet, String AttachmentDetails,
			Integer channelId, String ChannelName, String iconUrl, String logoUrl) {
		super();
		Id = id;
		this.subject = subject;
		this.createdById = createdById;
		this.createdDate = createdDate;
		this.active = active;
		this.readFlag = ReadFlag;
		this.snippet = snippet;
		if (AttachmentDetails != null) {
			this.attachments = CommonUtil.getAttachments(AttachmentDetails);
		}
		this.channelId = channelId;
		this.channelName = ChannelName;
		this.iconUrl = iconUrl;
		this.logoUrl = logoUrl;
		this.creator = new User();
		this.creator.setName(FirstName);
		this.creator.setEmail(Email);	
		Gson gsonObj = new Gson();
		if(PhotoURL != null) {
			UserPhoto photoUrls = gsonObj.fromJson(PhotoURL, UserPhoto.class);
			this.creator.setPhotoURL(photoUrls);
		}			
	}

	// Used by Memo.GetMemoById V2 api
	public Memo(Integer id, String subject, String message, Integer createdById, Long createdDate,
			Integer organizationId, Boolean active, Boolean isPublic, String publicURL, Integer TotalRecipient,
			Integer ReadCount, Integer UnReadCount, Float ReadPercent, Boolean ReadFlag, Integer channelId,
			String ChannelName, String iconUrl, Boolean isChatEnabled, String channelCategory) {

		// TotalRecipient,readCount,UnreadCount can be removed from procedure
		// GetMemoById

		super();
		Id = id;
		this.subject = subject;
		this.message = message;
		this.createdById = createdById;
		this.createdDate = createdDate;
		this.organizationId = organizationId;
		this.active = active;
		this.isPublic = isPublic;
		this.publicURL = publicURL;
		this.channelId = channelId;
		this.readPercent = ReadPercent;
		this.readFlag = ReadFlag;
		this.channelName = ChannelName;
		this.iconUrl = iconUrl;
		this.isChatEnabled = isChatEnabled;
		this.channelCategory = channelCategory;
	}

	public Memo(Integer id, String subject, String message, Integer createdById, Long createdDate,
			Integer organizationId, Boolean active, String recipientIds, Boolean isPublic, String publicURL,
			Integer PageViews, Integer channelId, String snippet, Integer receiptentsCount,
			Integer invalidReceiptentsCount, String invalidReceiptentList, Byte memoType) {
		super();
		Id = id;
		this.subject = subject;
		this.message = message;
		this.createdById = createdById;
		this.createdDate = createdDate;
		this.organizationId = organizationId;
		this.active = active;
		if (recipientIds != null && !recipientIds.isEmpty()) {
			List<String> strRecipientIds = Arrays.asList(recipientIds.split(","));
			this.recipientIds = new ArrayList<Long>();
			for (String strRecipientId : strRecipientIds) {
				this.recipientIds.add(Long.valueOf(strRecipientId));
			}
		}

		if (invalidReceiptentList != null && !invalidReceiptentList.isEmpty()) {

			List<String> strinvalidReceiptentList = Arrays.asList(invalidReceiptentList.split(","));
			this.invalidReceiptentList = new ArrayList<Long>();
			for (String invalidReceiptentId : strinvalidReceiptentList) {
				this.invalidReceiptentList.add(Long.valueOf(invalidReceiptentId));
			}
		}

		this.invalidReceiptentsCount = invalidReceiptentsCount;
		this.receiptentsCount = receiptentsCount;
		this.isPublic = isPublic;
		this.publicURL = publicURL;

		this.channelId = channelId;
		this.snippet = snippet;
		this.pageViews = PageViews;
		this.memoType= memoType;
		//this.showUserDetailOnSCP =showUserDetailOnSCP;
	}

	public Memo(Integer id, String subject, Integer createdById, Long createdDate, Integer organizationId,
			Boolean active, Float readPercent) {
		super();
		Id = id;
		this.subject = subject;  
		this.createdById = createdById;
		this.createdDate = createdDate;
		this.organizationId = organizationId;
		this.active = active;
		this.readPercent = readPercent;
	}

	public Memo(Integer id, String subject, Integer createdById, Long createdDate, Boolean active, Boolean readFlag) {
		super();
		Id = id;
		this.subject = subject;
		this.createdById = createdById;
		this.createdDate = createdDate;
		this.active = active;
		this.readFlag = readFlag;
	}

	
	public Memo(Integer id, String subject, String message, Integer createdById, Long createdDate,
			Integer organizationId, Boolean active, Boolean isPublic, String publicURL, Boolean showUserDetailOnSCP, String snippet,
			Byte memoType, Long totalRecipient, Long readCount, Long unReadCount, Float readPercent, Boolean readFlag) {
		super();
		Id = id;
		this.subject = subject;
		this.message = message;
		this.createdById = createdById;
		this.createdDate = createdDate;
		this.organizationId = organizationId;
		this.active = active;
		if (unReadCount != null && readCount != null && totalRecipient != null) {
			MemoSummary summary = new MemoSummary(readPercent, unReadCount.intValue(), readCount.intValue(),
					totalRecipient.intValue());
			this.memoSummary = summary;
		}
		this.isPublic = isPublic;
		this.publicURL = publicURL;
		this.showUserDetailOnSCP=showUserDetailOnSCP;
		this.snippet = snippet;
		this.memoType=memoType;
		this.readFlag = readFlag;
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

	public Integer getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(Integer organizationId) {
		this.organizationId = organizationId;
	}

	public MemoSummary getMemoSummary() {
		return memoSummary;
	}

	public void setMemoSummary(MemoSummary memoSummary) {
		this.memoSummary = memoSummary;
	}

	public List<Long> getRecipientIds() {
		return recipientIds;
	}

	public void setRecipientIds(List<Long> recipientIds) {
		this.recipientIds = recipientIds;
	}

	public Boolean getSendToAll() {
		return sendToAll;
	}

	public void setSendToAll(Boolean sendToAll) {
		this.sendToAll = sendToAll;
	}

	public Float getReadPercent() {
		return readPercent;
	}

	public void setReadPercent(Float readPercent) {
		this.readPercent = readPercent;
	}

	public Boolean getReadFlag() {
		return readFlag;
	}

	public void setReadFlag(Boolean readFlag) {
		this.readFlag = readFlag;
	}

	public User getCreator() {
		return creator;
	}

	public void setCreator(User creator) {
		this.creator = creator;
	}

	public List<Long> getAttachmentIds() {
		return attachmentIds;
	}

	public void setAttachmentIds(List<Long> attachmentIds) {
		this.attachmentIds = attachmentIds;
	}

	public List<Integer> getCityIds() {
		return cityIds;
	}

	public void setCityIds(List<Integer> cityIds) {
		this.cityIds = cityIds;
	}

	public List<Integer> getOfficeIds() {
		return officeIds;
	}

	public void setOfficeIds(List<Integer> officeIds) {
		this.officeIds = officeIds;
	}

	public List<Integer> getAdGroupIds() {
		return adGroupIds;
	}

	public void setAdGroupIds(List<Integer> adGroupIds) {
		this.adGroupIds = adGroupIds;
	}

	public List<Integer> getDepartmentIds() {
		return departmentIds;
	}

	public void setDepartmentIds(List<Integer> departmentIds) {
		this.departmentIds = departmentIds;
	}

	public List<String> getDesignations() {
		return designations;
	}

	public void setDesignations(List<String> designations) {
		this.designations = designations;
	}

	public Boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}

	public String getPublicURL() {
		return publicURL;
	}

	public void setPublicURL(String publicURL) {
		this.publicURL = publicURL;
	}

	public Integer getPageViews() {
		return pageViews;
	}

	public void setPageViews(Integer pageViews) {
		this.pageViews = pageViews;
	}

	public Long getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(Long updatedDate) {
		this.updatedDate = updatedDate;
	}

	public Integer getChannelId() {
		return channelId;
	}

	public void setChannelId(Integer channelId) {
		this.channelId = channelId;
	}

	public String getSnippet() {
		return snippet;
	}

	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public Byte getSendAs() {
		return SendAs;
	}

	public void setSendAs(Byte sendAs) {
		this.SendAs = sendAs;
	}

	public List<Integer> getOne2OneChatIds() {
		return One2OneChatIds;
	}

	public void setOne2OneChatIds(List<Integer> One2OneChatIds) {
		this.One2OneChatIds = One2OneChatIds;
	}

	public List<Integer> getGroupChatIds() {
		return GroupChatIds;
	}

	public void setGroupChatIds(List<Integer> GroupChatIds) {
		this.GroupChatIds = GroupChatIds;
	}

	public List<Long> getInvalidReceiptentList() {
		return invalidReceiptentList;
	}

	public void setInvalidReceiptentList(List<Long> invalidReceiptentList) {
		this.invalidReceiptentList = invalidReceiptentList;
	}

	public Integer getInvalidReceiptentsCount() {
		return invalidReceiptentsCount;
	}

	public void setInvalidReceiptentsCount(Integer invalidReceiptentsCount) {
		this.invalidReceiptentsCount = invalidReceiptentsCount;
	}

	public Integer getReceiptentsCount() {
		return receiptentsCount;
	}

	public void setReceiptentsCount(Integer receiptentsCount) {
		this.receiptentsCount = receiptentsCount;
	}

	public Boolean getIsChatEnabled() {
		return isChatEnabled;
	}

	public void setIsChatEnabled(Boolean isChatEnabled) {
		this.isChatEnabled = isChatEnabled;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public String getChannelCategory() {
		return channelCategory;
	}

	public void setChannelCategory(String channelCategory) {
		this.channelCategory = channelCategory;
	}

	public Integer getUploadId() {
		return uploadId;
	}

	public void setUploadId(Integer uploadId) {
		this.uploadId = uploadId;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}
	
	public Integer getMemoDumpAttachmentId() {
		return memoDumpAttachmentId;
	}

	public void setMemoDumpAttachmentId(Integer memoDumpAttachmentId) {
		this.memoDumpAttachmentId = memoDumpAttachmentId;
	}

	public final Boolean getSendToAllPartner() {
		return sendToAllPartner;
	}

	public final void setSendToAllPartner(Boolean sendToAllPartner) {
		this.sendToAllPartner = sendToAllPartner;
	}

	public final Boolean getSendToAllEmployeeAndPartner() {
		return sendToAllEmployeeAndPartner;
	}

	public final void setSendToAllEmployeeAndPartner(Boolean sendToAllEmployeeAndPartner) {
		this.sendToAllEmployeeAndPartner = sendToAllEmployeeAndPartner;
	}

	public final Boolean getShowUserDetailOnSCP() {
		return showUserDetailOnSCP;
	}

	public final void setShowUserDetailOnSCP(Boolean showUserDetailOnSCP) {
		this.showUserDetailOnSCP = showUserDetailOnSCP;
	}
	public final String getSharedById() {
		return sharedById;
	}
	public final void setSharedById(String sharedById) {
		this.sharedById = sharedById;
	}

	public final Byte getMemoType() {
		return memoType;
	}

	public final void setMemoType(Byte memoType) {
		this.memoType = memoType;
	}

	
	
}
