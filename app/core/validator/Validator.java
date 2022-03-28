package core.validator;

import messages.UserConnection;
import core.entities.ChatMessage;
import core.entities.Event;
import core.entities.IqMessage;

public interface Validator {
	public void validate(UserConnection connection, ChatMessage message);
	public void validate(Event event);
	public void validate(UserConnection connection, IqMessage iqMessage);
}
