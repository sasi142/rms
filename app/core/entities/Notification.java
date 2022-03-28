package core.entities;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import core.utils.Enums.NotificationType;

@JsonInclude(Include.NON_NULL)
public class Notification {
	private Properties properties;
	private String plainText;
	private String dataJsonString;
	private List<Integer> recipients;
	private NotificationType type;
	
	public Notification() {
		super();
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public String getPlainText() {
		return plainText;
	}

	public void setPlainText(String plainText) {
		this.plainText = plainText;
	}

	public String getDataJsonString() {
		return dataJsonString;
	}

	public void setDataJsonString(String dataJsonString) {
		this.dataJsonString = dataJsonString;
	}

	public List<Integer> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<Integer> recipients) {
		this.recipients = recipients;
	}

	public NotificationType getType() {
		return type;
	}

	public void setType(NotificationType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "Alert [properties=" + properties + ", plainText=" + plainText
				+ ", dataJsonString=" + dataJsonString + ", recipients="
				+ recipients + "]";
	}
}
