package core.entities;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.annotation.JsonIgnore;

@MappedSuperclass
public abstract class BaseEntity implements Serializable {

	private static final long	serialVersionUID	= 8616976940040974881L;

	@Basic
	@Column(name = "active", columnDefinition = "BIT", length = 1)
	@JsonIgnore
	protected Boolean			active;

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

}
