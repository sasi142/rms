package core.queue;

public interface QueueMessageHandler {
	void handleQueueMessage(String message, String messageId);
}
