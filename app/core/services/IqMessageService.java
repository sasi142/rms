package core.services;

import messages.UserConnection;
import core.entities.IqMessage;

public interface IqMessageService {
	public void handleIqRequest(UserConnection userConnection, IqMessage iqMessage);
}
