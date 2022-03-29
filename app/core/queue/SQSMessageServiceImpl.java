package core.queue;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import core.exceptions.InternalServerErrorException;
import core.utils.Enums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("sqsMessageServiceImpl")
public class SQSMessageServiceImpl implements SQSMessageService
{
    private static final Logger logger = LoggerFactory.getLogger(SQSMessageServiceImpl.class);

    @Value("${integration.enabled}")
    private Boolean enabled;

    @Value("${integration.channel}")
    private String channel;

    @Value("${integration.sqs.url}")
    private String url;

    @Value("${integration.sqs.max-poll-time}")
    private Integer maxPollTime;

    @Value("${integration.sqs.max-messages}")
    private Integer maxMessages;

    @Value("${integration.sqs.fetch-wait-on-error}")
    private Integer fetchWaitOnError;

    private final SQSConfig sqsConfig;

    private boolean running = false;

    @Autowired
    @Qualifier("queueMessageHandlerImpl")
    private QueueMessageHandler messageHandler;

    public AmazonSQS amazonSQS;

    public SQSMessageServiceImpl(SQSConfig sqsConfig)
    {
        this.sqsConfig = sqsConfig;
    }

    @PostConstruct
    public void start()
    {
            if (enabled && channel.equals("sqs")) {
                running = true;
                amazonSQS = sqsConfig.amazonSQS();
                new Thread(this::startListener, "sqs-listener").start();
            }
    }

    @PreDestroy
    public void stop()
    {
        running = false;
    }

    @Override
    public String sendMessage(String meetingId,String queueMessage) {
        logger.info("events sent to sqs queue for the meetingId: {}",meetingId);
        SendMessageResult sendMessage;
        try {
            logger.info("send message on sqs");
            Map<String, MessageAttributeValue> attributes = new HashMap<>();
            logger.info("send message on SQS: "+queueMessage);
            attributes.put("ContentBasedDeduplication", new MessageAttributeValue().withDataType("String").withStringValue("true"));
            SendMessageRequest sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(url)
                    .withMessageBody(queueMessage)
              //      .withMessageGroupId("1")   //TODO: Unable to setup messageGroupId for the SQS Queue "Chime-sqs"
                    .withMessageAttributes(attributes);
            sendMessage = amazonSQS.sendMessage(sendMessageRequest);
            logger.info("data sent to queue for message Id: "+ sendMessage.getSequenceNumber());
        } catch(Exception ex) {
            logger.error("failed to send message :"+ex);
            throw new InternalServerErrorException(Enums.ErrorCode.FAILED_TO_SEND_MESSAGE_ON_QUEUE, Enums.ErrorCode.FAILED_TO_SEND_MESSAGE_ON_QUEUE.getName());
        }
        return sendMessage.getMessageId();
    }

    private void startListener()
    {
        while (running) {
            try {
                ReceiveMessageRequest receiveMessageRequest =
                    new ReceiveMessageRequest(url).withWaitTimeSeconds(maxPollTime).withMaxNumberOfMessages(maxMessages)
                        .withMessageAttributeNames("MessageLabel");
                List<Message> sqsMessages = amazonSQS.receiveMessage(receiveMessageRequest).getMessages();
                for (Message message : sqsMessages) {
                    try {
                        onMessage(message);
                        amazonSQS.deleteMessage(
                            new DeleteMessageRequest().withQueueUrl(url).withReceiptHandle(message.getReceiptHandle()));
                    } catch (Exception ex) {
                        logger.error("Failed to process the message having id {}", message.getMessageId(),ex);
                        throw new InternalServerErrorException(Enums.ErrorCode.FAILED_TO_RECEIVE_MESSAGE_FROM_QUEUE, Enums.ErrorCode.FAILED_TO_RECEIVE_MESSAGE_FROM_QUEUE.getName());
                    }
                }
            } catch (Exception ex) {
                logger.warn("Error in fetching messages from SQS Queue. Will sleep and retry again.", ex);
                try {
                    Thread.sleep(fetchWaitOnError * 1000);
                } catch (InterruptedException ie) {
                    logger.error("Unable to sleep the sqs-listener", ie);
                }
            }
        }
        amazonSQS.shutdown();
    }

    private void onMessage(Message message)
    {
        Map<String, MessageAttributeValue> messageAttributes = message.getMessageAttributes();
        if (!StringUtils.isEmpty(message.getBody())) {
            messageHandler.handleQueueMessage(message.getBody(), message.getMessageId());
        } else {
            logger.warn("Message Body is missing for messageId - {}", message.getMessageId());
        }
    }
}
