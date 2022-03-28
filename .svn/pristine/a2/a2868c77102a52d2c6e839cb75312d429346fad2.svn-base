package core.entities.projections;

import javax.persistence.*;

@MappedSuperclass

@NamedStoredProcedureQuery(name = "VideoKyc.GetStatusByGroupId", procedureName = "USP_GetVideoKycStatusByGroupId", parameters = {
        @StoredProcedureParameter(name = "P_GroupId", type = Long.class, mode = ParameterMode.IN)
}, resultSetMappings = {"VideoKycStatus"})

@SqlResultSetMapping(name = "VideoKycStatus",
        classes = @ConstructorResult(targetClass = VideoKyc.class,
                columns = {
                        @ColumnResult(name = "Id", type = Integer.class),
                        @ColumnResult(name = "VideoKycStatus", type = Byte.class),
                        @ColumnResult(name = "AgentKycStatus", type = Byte.class),
                        @ColumnResult(name = "CreatedDate", type = Long.class)
                }))

public class VideoKyc {

    private Integer id;
    private Byte status;
    private Byte agentStatus;
    private Long createdDate;

    public VideoKyc(Integer id, Byte status, Byte agentStatus, Long createdDate) {
        this.id = id;
        this.status = status;
        this.agentStatus = agentStatus;
        this.createdDate = createdDate;
    }

    public Integer getId() {
        return id;
    }

    public Byte getStatus() {
        return status;
    }

    public Byte getAgentStatus() {
        return agentStatus;
    }

    public Long getCreatedDate() {
        return createdDate;
    }
}
