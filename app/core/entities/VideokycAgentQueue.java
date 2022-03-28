package core.entities;

import javax.persistence.Transient;

import com.fasterxml.jackson.databind.JsonNode;

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

@NamedQueries({
	@NamedQuery(name="VideokycAgentQueue.GetByAgentId", query= "select a from VideokycAgentQueue a where a.agentId=:agentId and a.active=true"),
	@NamedQuery(name="VideokycAgentQueue.makeAllAgentNotAvailable", query= "update VideokycAgentQueue a set a.agentStatus=3, a.updatedDate=:updatedDate,a.customerId=null"),
	@NamedQuery(name="VideokycAgentQueue.GetByGroupId", query= "select a from VideokycAgentQueue a where a.groupId=:groupId and a.agentStatus=:agentStatus and a.active=true"),
	@NamedQuery(name="VideokycAgentQueue.UpdateQueueByGroupId", query= "update VideokycAgentQueue a set a.agentStatus=:agentStatus, a.customerId= null, a.updatedDate=:updatedDate where a.agentId=:agentId and a.active=true"),
	@NamedQuery(name="VideokycAgentQueue.getByGroupAndAgentId", query= "select a from VideokycAgentQueue a where a.groupId=:groupId and a.agentId=:agentId and a.active=true")

})        



@NamedStoredProcedureQueries({
	@NamedStoredProcedureQuery(name = "VideokycAgentQueue.GetCallWaitTime", procedureName = "USP_GetVidoeKycCallWait", parameters = {
			@StoredProcedureParameter(name = "P_BreathingTime", type = Short.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_AvgCallDuration", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_SyncStatus", type = Boolean.class, mode = ParameterMode.IN)},
			resultSetMappings = {"AssignVidoeKycAgentResultMappings" }),
	@NamedStoredProcedureQuery(name = "VideokycAgentQueue.UpdateAgentQueueStatus", procedureName = "USP_UpdateAgentQueueStatus", parameters = {
			@StoredProcedureParameter(name = "P_AgentId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_AgentStatus", type = Byte.class, mode = ParameterMode.IN)}),
	@NamedStoredProcedureQuery(name = "VideokycAgentQueue.GetVideoKycGroupCallWait", procedureName = "USP_GetVideoKycGroupCallWait", parameters = {		
			@StoredProcedureParameter(name = "P_GroupId", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_Priority", type = Byte.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_BreathingTime", type = Short.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "P_AvgCallDuration", type = Integer.class, mode = ParameterMode.IN),
			@StoredProcedureParameter(name = "O_CallWaitDuration", type = Integer.class, mode = ParameterMode.OUT)}
			
	)
			
})
@SqlResultSetMappings({ @SqlResultSetMapping(name = "AssignVidoeKycAgentResultMappings", classes = {
	@ConstructorResult(targetClass = core.entities.VideokycAgentQueue.class, columns = {
			@ColumnResult(name = "CustomerId", type = Integer.class),
			@ColumnResult(name = "AgentId", type = Integer.class),			
			@ColumnResult(name = "CallWaitDuration", type = Long.class),
			@ColumnResult(name = "VideoKycId", type = Integer.class),
			@ColumnResult(name = "GuestGroupId", type = Long.class)})
}) })



@Entity
@Table(name = "videokyc_agent_queue")
public class VideokycAgentQueue extends BaseEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Integer Id;
	
	@Column
	private Integer agentId;
	
	@Column
	private Long groupId;
	
	@Column
	private Byte agentStatus;
	
	@Column
	private Integer customerId;
	
	@Column
	private Long updatedDate;

	@Column
	private Long lastPingDate;

	@Column
	private Long availableDate;
	
	@Transient
	private Long callWaitDuration;
	
	@Transient
	private Integer videoKycId;
	
	@Transient
	private Long guestGroupId;
	
	public VideokycAgentQueue() {
		
	}
	public VideokycAgentQueue(Integer customerId, Integer agentId, Long callWaitDuration, Integer videoKycId, Long guestGroupId) {
		this.customerId = customerId;
		this.agentId = agentId;
		this.callWaitDuration = callWaitDuration;	
		this.videoKycId = videoKycId;
		this.guestGroupId = guestGroupId;
	}

	public Integer getId() {
		return Id;
	}

	public void setId(Integer id) {
		Id = id;
	}

	public Integer getAgentId() {
		return agentId;
	}

	public void setAgentId(Integer agentId) {
		this.agentId = agentId;
	}

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public Byte getAgentStatus() {
		return agentStatus;
	}

	public void setAgentStatus(Byte agentStatus) {
		this.agentStatus = agentStatus;
	}

	public Integer getGuestUserId() {
		return customerId;
	}

	public void setGuestUserId(Integer customerId) {
		this.customerId = customerId;
	}

	public Long getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(Long updatedDate) {
		this.updatedDate = updatedDate;
	}

	public Long getLastPingDate() {
		return lastPingDate;
	}

	public void setLastPingDate(Long lastPingDate) {
		this.lastPingDate = lastPingDate;
	}

	public Long getAvailableDate() {
		return availableDate;
	}

	public void setAvailableDate(Long availableDate) {
		this.availableDate = availableDate;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public final Integer getVideoKycId() {
		return videoKycId;
	}
	public final void setVideoKycId(Integer videoKycId) {
		this.videoKycId = videoKycId;
	}	
	
	public final Integer getCustomerId() {
		return customerId;
	}
	public final void setCustomerId(Integer customerId) {
		this.customerId = customerId;
	}
	public final Long getGuestGroupId() {
		return guestGroupId;
	}
	public final void setGuestGroupId(Long guestGroupId) {
		this.guestGroupId = guestGroupId;
	}
	public Long getCallWaitDuration() {
		return callWaitDuration;
	}

	public void setCallWaitDuration(Long callWaitDuration) {
		this.callWaitDuration = callWaitDuration;
	}
	@Override
	public String toString() {
		return "VideokycAgentQueue [Id=" + Id + ", agentId=" + agentId + ", groupId=" + groupId + ", agentStatus="
				+ agentStatus + ", customerId=" + customerId + ", updatedDate=" + updatedDate + ", callWaitDuration="
				+ callWaitDuration + "]";
	}
}
