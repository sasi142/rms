/**
 * 
 */
package core.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Chandramohan.Murkute
 */
@JsonInclude(Include.NON_NULL)
public class GroupMember {
	@JsonProperty("userId")
	private Integer	id;
	private Byte	memberStatus;
	private Byte	memberRole;
	private Long	leftDate;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Byte getMemberStatus() {
		return memberStatus;
	}

	public void setMemberStatus(Byte memberStatus) {
		this.memberStatus = memberStatus;
	}

	public Long getLeftDate() {
		return leftDate;
	}

	public void setLeftDate(Long leftDate) {
		this.leftDate = leftDate;
	}

	public Byte getMemberRole() {
		return memberRole;
	}

	public void setMemberRole(Byte memberRole) {
		this.memberRole = memberRole;
	}
}
