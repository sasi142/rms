package core.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@NamedQueries({ @NamedQuery(name = "ClosedConnection.getExpiredConnections", query = "SELECT cc FROM ClosedConnection cc WHERE cc.lastUpdatedDate < :time AND cc.active=true") })
@Entity
@Table(name = "closed_connection")
public class ClosedConnection extends BaseEntity {
	private static final long	serialVersionUID	= 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer				userId;

	@Column
	private String				clientId;

	@Column
	private Long				lastUpdatedDate;

	@Column
	private byte				retry				= 0;

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public Long getLastUpdatedDate() {
		return lastUpdatedDate;
	}

	public void setLastUpdatedDate(Long lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}

	public byte getRetry() {
		return retry;
	}

	public void setRetry(byte retry) {
		this.retry = retry;
	}
}
