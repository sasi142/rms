package messages;

public class ConnId {

    private final Integer userId;
    private final String clientId;
    private final String sessionId;

    private final String id;

    public ConnId(Integer userId, String clientId, String sessionId) {
        this.userId = userId;
        this.clientId = clientId;
        this.sessionId = sessionId;
        this.id = String.format("U:%d#C:%s#S:%s", userId, clientId, sessionId);
    }

    public Integer getUserId() {
        return userId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getId() {
        return id;
    }
}
