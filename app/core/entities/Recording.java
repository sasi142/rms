package core.entities;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.math.BigDecimal;

@Entity
@Table(name = "recording")

@NamedStoredProcedureQueries({
        @NamedStoredProcedureQuery(name = "Recording.MarkForReprocessing", procedureName = "USP_MarkRecordingForReprocessing", parameters = {
                @StoredProcedureParameter(name = "P_RecordingId", type = Integer.class),
                @StoredProcedureParameter(name = "P_GroupId", type = Long.class),
                @StoredProcedureParameter(name = "P_EnableTranscoding", type = Boolean.class),
                @StoredProcedureParameter(name = "P_ReprocessRetry", type = Integer.class),
                @StoredProcedureParameter(name = "O_IsSuccess", type = Boolean.class, mode = ParameterMode.OUT),
                @StoredProcedureParameter(name = "O_Message", type = String.class, mode = ParameterMode.OUT)
        })
})
@NamedQueries({
        @NamedQuery(name = "Recording.UpdateRecordingStage", query = "update Recording r set r.endDate=:endDate, r.recordingStage=:recordingStage where r.Id=:recordingId and r.active=true"),
        @NamedQuery(name = "Recording.updateRecordingStageByMeetingId", query = "update Recording r set r.recordingStage=:recordingStage where r.meetingId=:meetingId and r.active=true"),
        @NamedQuery(name = "Recording.updateRecordingStageByECSTaskId", query = "update Recording r set r.recordingStage=:recordingStage where r.chimeRecordingTaskId=:chimeRecordingTaskId and r.active=true"),
        @NamedQuery(name = "Recording.updateRecordingStageChimeRecordingTaskId", query = "update Recording r set r.chimeRecordingTaskId=:chimeRecordingTaskId, r.recordingStage=:recordingStage where r.Id=:recordingId and r.active=true"),
        @NamedQuery(name = "Recording.UpdateRecordingOnStopEvent", query = "update Recording r set r.endDate=:endDate, r.recordingStage=:recordingStage, r.chatId=:chatId, r.attachmentId=:attId where r.Id=:recordingId and r.active=true"),
        @NamedQuery(name = "Recording.GetRecordingsByGroupId", query = "select r from Recording r where r.groupId=:groupId AND r.active=TRUE"),
        @NamedQuery(name = "Recording.GetRecordingsListByMeetingId", query = "select r from Recording r where r.meetingId=:meetingId AND r.active=TRUE"),
        @NamedQuery(name = "Recording.GetRecordingsByMeetingId",query= "select r from Recording r where r.meetingId=:meetingId ORDER BY r.id DESC")})
@JsonInclude(Include.NON_NULL)
public class Recording extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Integer Id;

    @Column
    private Integer callerId;

    @Column
    private Integer receiverId;

    @Column
    private Long startDate;

    @Column
    private Long endDate;

    @Column
    private Byte recordingType;

    @Column
    private Byte recordingStage;

    @Column
    private Long chatId;

    @Column
    private Integer attachmentId;

    @Column
    private Integer senderAttachmentId;

    @Column
    private Long updatedDate;

    @Column
    private Integer groupId;

    @Column
    private Integer meetingId;

    @Column
    private Byte machineId;

    @Column
    private Short orientation;

    @Column
    private Byte stopEventTriggerBy;

    @Column
    private BigDecimal Duration;

    @Column
    private String failureReason;

    @Column
    private Byte recordingMethod;

    @Column
    private Byte reprocessRetry = 0;

    @Column
    private boolean forcedTranscoding;

    @Column
    private boolean enableTranscoding;

    @Column
    private String transcodingReason;

    @Column
    private String chimeRecordingTaskId;

    public Recording() {
    }

    public Recording(Integer callerId, Integer receiverId,
                     Long startDate, Byte recordingType,
                     Byte recordingStage, Integer groupdId, Integer meetingId, Byte machineId, Byte recordingMethod) {
        super();
        this.callerId = callerId;
        this.receiverId = receiverId;
        this.startDate = startDate;
        this.recordingType = recordingType;
        this.recordingStage = recordingStage;
        this.active = true;
        this.groupId = groupdId;
        this.meetingId = meetingId;
        this.machineId = machineId;
        this.recordingMethod = recordingMethod;
    }

    public Integer getId() {
        return Id;
    }

    public void setId(Integer id) {
        Id = id;
    }

    public Integer getCallerId() {
        return callerId;
    }

    public void setCallerId(Integer callerId) {
        this.callerId = callerId;
    }

    public Integer getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Integer receiverId) {
        this.receiverId = receiverId;
    }

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public Long getEndDate() {
        return endDate;
    }

    public void setEndDate(Long endDate) {
        this.endDate = endDate;
    }

    public Byte getRecordingType() {
        return recordingType;
    }

    public void setRecordingType(Byte recordingType) {
        this.recordingType = recordingType;
    }

    public Byte getRecordingStage() {
        return recordingStage;
    }

    public void setRecordingStage(Byte recordingStage) {
        this.recordingStage = recordingStage;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Integer getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(Integer attachmentId) {
        this.attachmentId = attachmentId;
    }

    public Integer getSenderAttachmentId() {
        return senderAttachmentId;
    }

    public void setSenderAttachmentId(Integer senderAttachmentId) {
        this.senderAttachmentId = senderAttachmentId;
    }

    public Long getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Long updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Byte getStopEventTriggerBy() {
        return stopEventTriggerBy;
    }

    public void setStopEventTriggerBy(Byte stopEventTriggerBy) {
        this.stopEventTriggerBy = stopEventTriggerBy;
    }

    public Integer getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Integer meetingId) {
        this.meetingId = meetingId;
    }

    public Byte getMachineId() {
        return machineId;
    }

    public void setMachineId(Byte machineId) {
        this.machineId = machineId;
    }

    public Short getOrientation() {
        return orientation;
    }

    public void setOrientation(Short orientation) {
        this.orientation = orientation;
    }

    public BigDecimal getDuration() {
        return Duration;
    }

    public void setDuration(BigDecimal duration) {
        Duration = duration;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Byte getRecordingMethod() {
        return recordingMethod;
    }

    public void setRecordingMethod(Byte recordingMethod) {
        this.recordingMethod = recordingMethod;
    }

    public Byte getReprocessRetry() {
        return reprocessRetry;
    }

    public void setReprocessRetry(Byte reprocessRetry) {
        this.reprocessRetry = reprocessRetry;
    }

    public void setForcedTranscoding(boolean forcedTranscoding) {
        this.forcedTranscoding = forcedTranscoding;
    }

    public boolean getForcedTranscoding() {
        return forcedTranscoding;
    }


    public void setTranscodingReason(String transcodingReason) {
        this.transcodingReason = transcodingReason;
    }

    public void setChimeRecordingTaskId(String chimeRecordingTaskId) {
        this.chimeRecordingTaskId = chimeRecordingTaskId;
    }

    public String getChimeRecordingTaskId() {
        return chimeRecordingTaskId;
    }

    public String getTranscodingReason() {
        return transcodingReason;
    }

    public boolean isEnableTranscoding() {
        return enableTranscoding;
    }

    public void setEnableTranscoding(boolean enableTranscoding) {
        this.enableTranscoding = enableTranscoding;
    }
}
