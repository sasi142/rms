package core.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.Table;
import javax.persistence.Transient;

@NamedQueries({
		@NamedQuery(name = "VideoKycCustomerQueue.MarkInactiveByIds", query = "UPDATE VideoKycCustomerQueue SET active=false, updatedDate=:updatedDate WHERE active=true AND customerId in (:customerIds)"),
		@NamedQuery(name = "VideoKycCustomerQueue.MarkInactive", query = "UPDATE VideoKycCustomerQueue SET active=false, updatedDate=:updatedDate WHERE active=true AND customerId=:customerId"),
		@NamedQuery(name = "VideoKycCustomerQueue.GetByCustomerId", query = "select v from VideoKycCustomerQueue v where v.customerId=:customerId order by v.createdDate desc")
})

@NamedStoredProcedureQueries({
	@NamedStoredProcedureQuery(name = "VideoKycCustomerQueue.Add", procedureName = "USP_InsertVidoeKycCustomerInQueue", parameters = {
			@StoredProcedureParameter(name = "P_VideoKycId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_GuestGroupId", type = Long.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_CustomerId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_GroupId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_Priority", type = Byte.class, mode = ParameterMode.IN)
		} )			 
})
	
@Entity
@Table(name = "videokyc_customer_queue")
public class VideoKycCustomerQueue extends BaseEntity {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Integer Id;
	
	@Column
	private Integer customerId;
	
	@Column
	private Integer groupId;
	
	@Column
	private Integer callWaitTime;
	
	@Column
	private Long createdDate;

	@Column
	private Long updatedDate;

	@Column
	private Byte version;

	@Column
	private Long lastPingDate;
	
	@Transient
	private Integer videoKycId;
	
	@Transient
	private Long guestGroupId;

	public Integer getId() {
		return Id;
	}

	public void setId(Integer id) {
		Id = id;
	}

	public Integer getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Integer customerId) {
		this.customerId = customerId;
	}

	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public Integer getCallWaitTime() {
		return callWaitTime;
	}

	public void setCallWaitTime(Integer callWaitTime) {
		this.callWaitTime = callWaitTime;
	}

	public Long getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}

	public Long getLastPingDate() {
		return lastPingDate;
	}

	public void setLastPingDate(Long lastPingDate) {
		this.lastPingDate = lastPingDate;
	}

	public Byte getVersion() {
		return version;
	}

	public void setVersion(Byte version) {
		this.version = version;
	}	

	public final Integer getVideoKycId() {
		return videoKycId;
	}

	public final void setVideoKycId(Integer videoKycId) {
		this.videoKycId = videoKycId;
	}

	public final Long getGuestGroupId() {
		return guestGroupId;
	}

	public final void setGuestGroupId(Long guestGroupId) {
		this.guestGroupId = guestGroupId;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Long getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(Long updatedDate) {
		this.updatedDate = updatedDate;
	}

	
}
