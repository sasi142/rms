package core.queue;

public class QueueMessage {
	private String type;
	private final String sessionId;
	private final String id;
	private String text;
	
	public QueueMessage(String type, String sessionId, String id, String text) {
		super();
		this.type = type;
		this.sessionId = sessionId;
		this.id = id;
		this.text = text;
	}
	public String getType() {
		return type;
	}
	public String getText() {
		return text;
	}

	public String getId() {
		return id;
	}

	public String getSessionId() {
		return sessionId;
	}
}
