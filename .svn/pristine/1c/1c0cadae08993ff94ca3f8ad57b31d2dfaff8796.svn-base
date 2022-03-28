package core.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name="one2onechat_user")
@NamedQueries({
	@NamedQuery(name = "One2OneChatUser.GetUnReadChatCount", query = "Select ou.to From One2OneChatUser ou where ou.to =:to AND ou.status=false AND active=true"),
	@NamedQuery(name = "One2OneChatUser.UpdateUnReadChat", query = "Update One2OneChatUser ou set ou.status=true where ou.from=:from AND ou.to=:to")
})
public class One2OneChatUser extends BaseEntity {	
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Long Id;
	
	@Column(name="sender")
	private Integer from;
	
	@Column(name="recipient")
	private Integer to;	
	
	@Basic
	@Column(name = "status", columnDefinition = "BIT", length = 1)
	private Boolean status;

	public Long getId() {
		return Id;
	}

	public void setId(Long id) {
		Id = id;
	}

	public Integer getFrom() {
		return from;
	}

	public void setFrom(Integer from) {
		this.from = from;
	}

	public Integer getTo() {
		return to;
	}

	public void setTo(Integer to) {
		this.to = to;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}
}

