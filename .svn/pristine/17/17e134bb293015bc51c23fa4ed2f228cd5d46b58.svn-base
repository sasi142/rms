package core.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Entity
@Table(name = "client_certificate")
@JsonInclude(Include.NON_NULL)
public class ClientCertificate extends BaseEntity {
	private static final long	serialVersionUID	= 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	protected Long				Id;

	@Column(name = "ClientId")
	private String				clientId;

	@Column(name = "ClientType")
	private String				clientType;

	@Column(name = "BundleId")
	private String				bundleId;

	@Column(name = "CertificatePath")
	private String				certificatePath;
	
	@Column(name = "CertificatePassword")
	private String				certificatePassword;
	
	@Column(name = "VoIPCertificatePath")
	private String				voIpCertificatePath;
	
	@Column(name = "VoIpCertificatePassword")
	private String				voIpCertificatePassword;
	
	@Column(name = "AppKey")
	private String				appKey;
	
	@Column(name = "CertificateType")
	private String				certificateType;
	
	@Column(name = "OrganizationId")
	private Integer				organizationId;

	public Long getId() {
		return Id;
	}

	public void setId(Long id) {
		Id = id;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientType() {
		return clientType;
	}

	public void setClientType(String clientType) {
		this.clientType = clientType;
	}

	public String getBundleId() {
		return bundleId;
	}

	public void setBundleId(String bundleId) {
		this.bundleId = bundleId;
	}

	public String getCertificatePath() {
		return certificatePath;
	}

	public void setCertificatePath(String certificatePath) {
		this.certificatePath = certificatePath;
	}

	public String getCertificatePassword() {
		return certificatePassword;
	}

	public void setCertificatePassword(String certificatePassword) {
		this.certificatePassword = certificatePassword;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getCertificateType() {
		return certificateType;
	}

	public void setCertificateType(String certificateType) {
		this.certificateType = certificateType;
	}

	public Integer getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(Integer organizationId) {
		this.organizationId = organizationId;
	}	

	public String getVoIpCertificatePath() {
		return voIpCertificatePath;
	}

	public void setVoIpCertificatePath(String voIpCertificatePath) {
		this.voIpCertificatePath = voIpCertificatePath;
	}

	public String getVoIpCertificatePassword() {
		return voIpCertificatePassword;
	}

	public void setVoIpCertificatePassword(String voIpCertificatePassword) {
		this.voIpCertificatePassword = voIpCertificatePassword;
	}

	@Override
	public String toString() {
		return "ClientCertificate [clientId=" + clientId + ", clientType="
				+ clientType + ", bundleId=" + bundleId + ", certificateType="
				+ certificateType + ", organizationId=" + organizationId + "]";
	}
}
