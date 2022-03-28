/**
 * 
 */
package controllers.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

import core.entities.Attachment;
import core.entities.Memo;
import core.entities.MemoChatUser;
import core.entities.User;
import core.utils.CommonUtil;
import core.utils.PropertyUtil;
import play.Logger;
import core.utils.Constants;
/**
 * @author Chandramohan.Murkute
 */
@JsonInclude(Include.NON_NULL)
public class MemoDto {

	private Integer			id;
	private String			subject;
	private String			text;
	private List<Long>		attachmentIds;
	private Boolean			sendToAll	= Boolean.FALSE;
	private List<Long>	recipientIds;
	private String			creationDate;
	private User			creator;
	private MemoSummaryDto	memoSummary;
	private Float			readPercent;
	private Boolean			readFlag;
	private Boolean 		isPublic    = Boolean.FALSE;
	private String			publicURL;

	// Memo Recipient Selection
	private List<Integer>	cityIds;
	private List<Integer>	officeIds;
	private List<Integer>	adGroupIds;
	private List<Integer>	departmentIds;
	private List<Integer>	subDepartmentIds;
	private List<String>	designations;
	
	private Integer 		channelId;
	private String 			snippet;
	//private String 			attachmentDetails;
	private String 			channelName;
	private String 			name;
	//private String 			email;
	private String 			photoURL;
	private String          iconUrl;
	
	private List<Integer>   one2OneChatIds;
	private List<Integer>   groupChatIds;
	
	private List<Long>   invalidReceiptentList;
	private Integer         invalidReceiptentsCount;
	private Integer         receiptentsCount;
	private Integer         createdById;
	private Byte            chatType;
	private Integer         entityId;
	private Boolean isChatEnabled;
	private List<Attachment> attachments;
	//private List<String> 		 excel_uploaded_ids;
	private Integer uploadId ;
	
	private String logoUrl;
	private Integer memoDumpAttachmentId;
	private Boolean			sendToAllPartner	= Boolean.FALSE;
	private Boolean			sendToAllEmployeeAndPartner	= Boolean.FALSE;
	private Boolean			showUserDetailOnSCP =  Boolean.FALSE;
	private String          scpSharedByUserId;
	private User			scpSharedUser;
	private Byte            memoType;


		
	public MemoDto() {
	}

	public MemoDto(Integer id, String subject, String text) {
		super();
		this.id = id;
		this.subject = subject;
		this.text = text;
	}

	public MemoDto(Memo memo) {
		this.id = memo.getId();
		this.subject = memo.getSubject();

		this.text = CommonUtil.escapeHtmlForWEB(memo.getMessage());
		if (memo.getMemoSummary() != null) {
			this.memoSummary = new MemoSummaryDto(memo.getMemoSummary());
		}
		this.sendToAll = null;
		this.readPercent = memo.getReadPercent();
		this.readFlag = memo.getReadFlag();
		this.isPublic = memo.getIsPublic();
		this.publicURL = memo.getPublicURL();
		this.channelId = memo.getChannelId();
		String snippet = memo.getSnippet();
		Integer snippet_req_length = Integer.parseInt(PropertyUtil.getProperty(Constants.SNIPPET_ALLOWED_LENGTH));
		Logger.underlying().info("snippet is: "+snippet+" allowed length:"+snippet_req_length);
		Integer start_index = 0;
		if(snippet != null &&  (snippet.length() > snippet_req_length)) {
			snippet = snippet.substring(start_index,snippet_req_length);
		}
		this.snippet = snippet;
		//this.attachmentDetails = memo.getAttachmentDetails();
		
		this.attachments = memo.getAttachments();
		
		this.channelName = memo.getChannelName();
		//this.firstName = memo.getFirstName();
		//this.email = memo.getEmail();
		//this.photoURL = memo.getPhotoURL();
		this.creator = memo.getCreator();
		this.iconUrl = memo.getIconUrl();
		this.logoUrl = memo.getLogoUrl();
		this.receiptentsCount = memo.getReceiptentsCount() ;
		this.invalidReceiptentsCount = memo.getInvalidReceiptentsCount() ;
		this.invalidReceiptentList = memo.getInvalidReceiptentList() ;
		if(memo.getCreatedById() != null) {
			this.createdById = memo.getCreatedById();
		}			
		this.isChatEnabled = memo.getIsChatEnabled();	
		this.showUserDetailOnSCP = memo.getShowUserDetailOnSCP();
		this.memoType=memo.getMemoType();
				
	}
	
	public MemoDto(MemoChatUser memoChatUser) {
		this.id = memoChatUser.getMemoId();
		this.chatType = memoChatUser.getChatType();
		this.entityId = memoChatUser.getEntityId();
		this.name = memoChatUser.getName();		
		this.photoURL = memoChatUser.getPhotoURL();		
	}

	public Memo toMemo() {
		Memo memo = new Memo();
		memo.setMessage(text);
		memo.setSubject(subject);
		memo.setRecipientIds(recipientIds);
		memo.setSendToAll(sendToAll);
		memo.setAttachmentIds(attachmentIds);
		memo.setCityIds(cityIds);
		memo.setOfficeIds(officeIds);
		memo.setAdGroupIds(adGroupIds);
		memo.setIsPublic(isPublic);
	
		List<Integer> deptList = new ArrayList<Integer>();
		if (!Objects.isNull(departmentIds)) {
			deptList.addAll(departmentIds);
		}
		if (!Objects.isNull(subDepartmentIds)) {
			deptList.addAll(subDepartmentIds);
		}
		memo.setDepartmentIds(deptList);
		
		memo.setDesignations(designations);
		
		memo.setChannelId(channelId);
		
		memo.setChannelName(channelName);
		memo.setSnippet(snippet);
		memo.setCreator(creator);
		memo.setIconUrl(iconUrl);
		memo.setOne2OneChatIds(one2OneChatIds);
		memo.setGroupChatIds(groupChatIds);
		if(Objects.nonNull(uploadId) && uploadId > 0) {
		memo.setUploadId(uploadId);
		}
		memo.setMemoDumpAttachmentId(memoDumpAttachmentId);
		memo.setSendToAllPartner(sendToAllPartner);
		memo.setSendToAllEmployeeAndPartner(sendToAllEmployeeAndPartner);
		memo.setShowUserDetailOnSCP(showUserDetailOnSCP);
		return memo;
	}
	

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public Integer getId() {
		return id;
	}

	public String getSubject() {
		return subject;
	}

	public String getText() {
		return text;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public User getCreator() {
		return creator;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public void setText(String text) {// TODO HTML Escape
		this.text = text;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public void setCreator(User creator) {
		this.creator = creator;
	}

	public Boolean getSendToAll() {
		return sendToAll;
	}

	public void setSendToAll(Boolean sendToAll) {
		this.sendToAll = sendToAll;
	}

	public List<Long> getRecipientIds() {
		return recipientIds;
	}

	public void setRecipientIds(List<Long> recipientIds) {
		this.recipientIds = recipientIds;
	}

	public MemoSummaryDto getMemoSummary() {
		return memoSummary;
	}

	public void setMemoSummary(MemoSummaryDto memoSummary) {
		this.memoSummary = memoSummary;
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

	public List<Integer> getSubDepartmentIds() {
		return subDepartmentIds;
	}

	public void setSubDepartmentIds(List<Integer> subDepartmentIds) {
		this.subDepartmentIds = subDepartmentIds;
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
	
	/*public String getAttachmentDetails() {
		return attachmentDetails;
	}

	public void setAttachmentDetails(String attachmentDetails) {
		this.attachmentDetails = attachmentDetails;
	}**/

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Byte getChatType() {
		return chatType;
	}

	public void setEmail(Byte chatType) {
		this.chatType = chatType;
	}

	public String getPhotoURL() {
		return photoURL;
	}

	public void setPhotoURL(String photoURL) {
		this.photoURL = photoURL;
	}
	
	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}
	
	public List<Integer> getOne2OneChatIds() {
		return one2OneChatIds;
	}

	public void setOne2OneChatIds(List<Integer> one2OneChatIds) {
		this.one2OneChatIds = one2OneChatIds;
	}

	public List<Integer> getGroupChatIds() {
		return groupChatIds;
	}

	public void setGroupChatIds(List<Integer> groupChatIds) {
		this.groupChatIds = groupChatIds;
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
	
	public Integer getCreatedById() {
		return createdById;
	}

	public final Boolean getSendToAllEmployeeAndPartner() {
		return sendToAllEmployeeAndPartner;
	}

	public final void setSendToAllEmployeeAndPartner(Boolean sendToAllEmployeeAndPartner) {
		this.sendToAllEmployeeAndPartner = sendToAllEmployeeAndPartner;
	}

	public void setCreatedById(Integer createdById) {
		this.createdById = createdById;
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

	public Boolean getSendToAllPartner() {
		return sendToAllPartner;
	}

	public void setSendToAllPartner(Boolean sendToAllPartner) {
		this.sendToAllPartner = sendToAllPartner;
	}		
	
	public final Boolean getShowUserDetailOnSCP() {
		return showUserDetailOnSCP;
	}

	public final void setShowUserDetailOnSCP(Boolean showUserDetailOnSCP) {
		this.showUserDetailOnSCP = showUserDetailOnSCP;
	}	

	public final String getScpSharedByUserId() {
		return scpSharedByUserId;
	}

	public final void setScpSharedByUserId(String scpSharedByUserId) {
		this.scpSharedByUserId = scpSharedByUserId;
	}

	
	public final User getScpSharedUser() {
		return scpSharedUser;
	}

	public final void setScpSharedUser(User scpSharedUser) {
		this.scpSharedUser = scpSharedUser;
	}

	public final Byte getMemoType() {
		return memoType;
	}

	public final void setMemoType(Byte memoType) {
		this.memoType = memoType;
	}

	@Override
	public String toString() {
		return "MemoDto [id=" + id + ", subject=" + subject + ", text=" + text + ", attachmentIds=" + attachmentIds
				+ ", sendToAll=" + sendToAll + ", recipientIds=" + recipientIds + ", creationDate=" + creationDate
				+ ", creator=" + creator + ", memoSummary=" + memoSummary + ", readPercent=" + readPercent
				+ ", readFlag=" + readFlag + ", isPublic=" + isPublic + ", publicURL=" + publicURL + ", cityIds="
				+ cityIds + ", officeIds=" + officeIds + ", adGroupIds=" + adGroupIds + ", departmentIds="
				+ departmentIds + ", subDepartmentIds=" + subDepartmentIds + ", designations=" + designations
				+ ", channelId=" + channelId + ", snippet=" + snippet + ", channelName=" + channelName + ", name="
				+ name + ", photoURL=" + photoURL + ", iconUrl=" + iconUrl + ", one2OneChatIds=" + one2OneChatIds
				+ ", groupChatIds=" + groupChatIds + ", invalidReceiptentList=" + invalidReceiptentList
				+ ", invalidReceiptentsCount=" + invalidReceiptentsCount + ", receiptentsCount=" + receiptentsCount
				+ ", createdById=" + createdById + ", chatType=" + chatType + ", entityId=" + entityId
				+ ", isChatEnabled=" + isChatEnabled + ", attachments=" + attachments + ", uploadId="
				+ uploadId + "]";
	}


}





