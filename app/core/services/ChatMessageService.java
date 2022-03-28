package core.services;

import core.entities.ChatMessage;
import messages.UserConnection;

public interface ChatMessageService {
	public void sendMessage(UserConnection connection, ChatMessage message);

	
}
