package core.entities.projections;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;


@MappedSuperclass
@NamedStoredProcedureQuery(name = "ChatSummary.AllUnreadCountSummary", procedureName = "USP_GetUserUnReadMessageCount", parameters = {
        @StoredProcedureParameter(name = "P_OrganizationId", type = Integer.class, mode = ParameterMode.IN),
        @StoredProcedureParameter(name = "P_UserName", type = String.class, mode = ParameterMode.IN)
}, resultSetMappings = {"UnreadCountSummary"})
@SqlResultSetMapping(name = "UnreadCountSummary",
        classes = @ConstructorResult(targetClass = UnreadCountSummary.class,
                columns = {
                        @ColumnResult(name = "UnreadIndividual", type = Integer.class),
                        @ColumnResult(name = "UnreadGroup", type = Integer.class),
                        @ColumnResult(name = "UnreadGuest", type = Integer.class),
                        @ColumnResult(name = "UnreadMemo", type = Integer.class)
                }))
public class UnreadCountSummary {

    @JsonProperty
    private int unreadIndividual;   //returns count of unread messages in one2one chats
    @JsonProperty
    private int unreadGroup;   //returns count of unread messages in group chats
    @JsonProperty
    private int unreadGuest;   //returns count of unread messages in guest chats
    @JsonProperty
    private int unreadMemo;  //returns count of unread messages in memo

    public UnreadCountSummary(@JsonProperty("unreadIndividual") int unreadIndividual,
                              @JsonProperty("unreadGroup") int unreadGroup,
                              @JsonProperty("unreadGuest") int unreadGuest,
                              @JsonProperty("unreadMemo") int unreadMemo) {
        this.unreadIndividual = unreadIndividual;
        this.unreadGroup = unreadGroup;
        this.unreadGuest = unreadGuest;
        this.unreadMemo = unreadMemo;
    }

    public int getUnreadIndividual() {
        return unreadIndividual;
    }

    public int getUnreadGroup() {
        return unreadGroup;
    }

    public int getUnreadGuest() {
        return unreadGuest;
    }

    public int getUnreadMemo() {
        return unreadMemo;
    }
}