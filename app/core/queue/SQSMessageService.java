package core.queue;

public interface SQSMessageService {
    String sendMessage(String entityId,String queueMessage);
}
