package core.services;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.daos.CacheConnectionInfoDao;
import core.daos.CacheUserDao;
import core.entities.ChatMessage;

import core.entities.One2OneChat;
import core.entities.User;
import core.daos.MeetingInfoDao;
import core.exceptions.ApplicationException;
import core.exceptions.BadRequestException;
import core.utils.ActorThreadContext;
import core.utils.CommonUtil;
import core.utils.Enums.ChatMessageType;
import core.utils.Enums.ChatType;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.EventType;
import core.utils.Enums.MessageType;
import core.utils.Enums.NotificationType;
import core.utils.Enums.PushNotificationVisibility;
import core.utils.Enums.UserCategory;
import core.utils.Constants;
import core.utils.Enums.VideoCallMessageType;
import core.utils.Enums.VideoCallStatus;
import core.utils.PropertyUtil;
import messages.UserConnection;

import play.libs.Json;

@Service
@Transactional(rollbackFor = {Exception.class})
public class One2OneChatMessageServiceImpl implements One2OneChatMessageService {
    final static Logger logger = LoggerFactory.getLogger(One2OneChatMessageServiceImpl.class);

    @Autowired
    @Qualifier("CacheImpl")
    private CacheConnectionInfoDao cacheConnectionInfoDao;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private UserConnectionService userConnectionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private CacheUserDao cacheUserDao;

    @Autowired
    @Qualifier("RmsCacheService")
    private CacheService cacheService;

    @Autowired
    private EventNotificationBuilder eventNotificationBuilder;

    @Autowired
    private MeetingInfoDao meetingInfoDao;

    @Override
    public void sendMessage(UserConnection connection, ChatMessage message) {
        try {
            logger.info("In sendMessage, ChatMessage is valid. type = " + message.getType() + ", Subtype = "
                    + message.getSubtype());
            if (message.getType() == MessageType.Chat.getId()) {
                message.setFrom(connection.getUserContext().getUser().getId());
                message.setName(connection.getUserContext().getUser().getName());
                if (message.getSubtype().byteValue() == ChatType.One2One.getId().byteValue()
                        || message.getSubtype().byteValue() == ChatType.One2OneVideoChat.getId().byteValue()) {
                    sendOne2OneMessage(connection, message);
                }
            } else if (message.getType() == MessageType.Typing.getId()
                    || message.getType() == MessageType.StopTyping.getId()) {
                sendOne2OneTypingMessage(connection, message);
            } else if ((message.getType() == MessageType.ACK.getId())) {
                chatHistoryService.updateChatReadStatus(message.getTo(), ChatType.One2One, MessageType.ACK,
                        connection.getUserContext());
                logger.debug("Completed UpdateChatReadStatus, ACK/Chat Message :" + message.getText());
            } else {
                throw new BadRequestException(ErrorCode.UnsupportedTypeSubtype,
                        ErrorCode.UnsupportedTypeSubtype.getName());
            }
            logger.debug("sendMessage completed successfully");
        } catch (Exception ex) {
            throw ex;
        }

    }

    private void sendOne2OneMessage(UserConnection connection, ChatMessage message) {
        logger.debug("One2One message received with attachment data " + message.getData());
        if (message.getData() != null) {
            JsonNode attachmentNode = message.getData().findPath("attachments");
            if (attachmentNode.isArray()) {
                ArrayNode node = (ArrayNode) attachmentNode;
                String msgText = message.getText();
                if (msgText != null && !msgText.isBlank()) {
                    message.setText(msgText);
                } /*else if (msgText.equalsIgnoreCase("Live Screenshot")
                        || msgText.equalsIgnoreCase("Live Recording")) {
                    message.setText(msgText);
                }*/ else {
                    msgText = node.size() + " file(s) attached";
                    message.setText(msgText);
                }
            }
        }
        // save OnetoOneChat history
        Long time = System.currentTimeMillis();
        String date = CommonUtil.getDateTimeWithTimeZone(time,
                connection.getUserContext().getUser().getTimezone());
        message.setDate(date);
        message.setUtcDate(time);

        List<String> clientIds = CommonUtil.getSupportedClientIds(ChatType.One2One);
        if (message.getChatMessageType() == ChatMessageType.VideoCallMessage.getId().intValue()) {
            handleVideoCallMessage(connection, message, clientIds);
        } else if (message.getChatMessageType() == ChatMessageType.ClientEvent.getId().intValue()) {
            String webPushClientIds = PropertyUtil.getProperty(Constants.WEB_PUSH_NOTIFICATION_CLIENT_IDS);
            String[] webApps = webPushClientIds.split(",");
            List<String> clientEventsClientIds = Arrays.asList(webApps);
            userConnectionService.sendMessageToActor(message.getTo(), connection, message,
                    clientEventsClientIds);
        } else {
            logger.debug("Create One2One Chat history for message " + message.getText());
            if (ChatMessageType.ScreenShot.getId().intValue() == message.getChatMessageType()) {
                message.setText("Live Screenshot");
                commonUtil.switchUserForScreenshotMessage(connection, message);
            }
            One2OneChat dbChatMessage = chatHistoryService.createOne2OneChatHistory(message);
            logger.debug("Created One2One Chat history returned messageId " + dbChatMessage.getId());
            message.setMid(dbChatMessage.getId());
            if (dbChatMessage.getParentMsg() != null) {
                JsonNode userJson = cacheUserDao.find(dbChatMessage.getParentMsg().getFrom());
                String parentMsgSenderName = userJson.findPath("firstName").asText();
                if (parentMsgSenderName != null) {
                    dbChatMessage.getParentMsg().setName(parentMsgSenderName);
                }
            }
            message.setParentMsg(dbChatMessage.getParentMsg());
            User toUser = ActorThreadContext.get().getUserFromMap(message.getTo());
            if (toUser == null) {
                toUser = cacheService.getUser(message.getTo(), false);
                logger.info("User from ActorThreadContext is null..got user with id:" + toUser.getId()
                        + " details from cache");
            }

            // send message to self. In case of screenshots the user details are switched
            // because the screenshots are
            // taken by the receiver on behalf of the caller. For that we have to send a
            // messages to the caller.
            if ((UserCategory.Employee.getId() == toUser.getUserCategory().intValue())
                    && ChatMessageType.ScreenShot.getId().intValue() == message.getChatMessageType()) {
                userConnectionService.sendMessageToActor(message.getFrom(), connection, message, clientIds);
            } else {
                userConnectionService.sendMessageToActor(connection.getUserContext().getUser().getId(),
                        connection, message, clientIds);
            }

            logger.debug("sent One2One message to self : {}", message.getText());

            logger.info("user category is: {}", toUser.getUserCategory());
            boolean sendMsgToRecipient = true;
            if ((UserCategory.Guest.getId() == toUser.getUserCategory().intValue())
                    && (ChatMessageType.ScreenShot.getId().intValue() == message.getChatMessageType()
                    || ChatMessageType.VideoRecording.getId().intValue() == message.getChatMessageType())) {
                logger.info("screen shot or video recording. dont send it to guest");
                sendMsgToRecipient = false;
            }

            if (sendMsgToRecipient) {
                message.setUuid(connection.getUuid());
                Set<String> msgReceiverClients = userConnectionService.sendMessageToActor(message.getTo(),
                        connection, message, clientIds);
                logger.debug("Send One2One Message To Actor returned clients who received messages as {}", msgReceiverClients);
                Boolean isSendPN = commonUtil.isAlwaysSendPushNofication(message, msgReceiverClients);
                logger.debug("push notification flag = " + isSendPN);
                if (isSendPN) {
                    List<Integer> recipients = Collections.singletonList(message.getTo());
                    logger.info("contact: " + message.getTo() + "  is offline");
                    notificationService.sendMobileNotification(NotificationType.Chat.getId(),
                            message, recipients, PushNotificationVisibility.Recipients);
                    logger.debug("called sent mobile push notification for users {} ", recipients);
                }
                if (!(ChatMessageType.SystemMessage.getId().intValue() == message.getChatMessageType())) {
                    if (commonUtil.isUserNotRegistered(message.getTo())) {
                        if (!connection.getRecipientIds().contains(message.getTo())
                                && !connection.getHasSentUnregisteredMessage()) {
                            createMessageWhenUserNotRegistered(connection, message, clientIds);
                        }
                    }
                }
            }
        }
    }

    private void createMessageWhenUserNotRegistered(UserConnection connection, ChatMessage message,
                                                    List<String> clientIds) {
        connection.getRecipientIds().add(message.getTo());
        ChatMessage msg = new ChatMessage();
        msg.setType(MessageType.Chat.getId());
        msg.setSubtype(Integer.valueOf(ChatType.One2One.getId().intValue()));
        msg.setName(message.getName());
        msg.setUtcDate(System.currentTimeMillis());
        msg.setFrom(message.getFrom());
        msg.setTo(message.getTo());
        msg.setText("");
        msg.setChatMessageType(Integer.valueOf(ChatMessageType.SystemMessage.getId()));

        JsonNode userJson = cacheUserDao.find(message.getTo());
        Long receiverOrgId = userJson.findPath("orgId").asLong();

        ObjectNode node = Json.newObject();
        node.put("receiver", message.getTo());
        node.put("orgId", receiverOrgId);
        node.put("eventType", EventType.NotRegisteredMessage.getId());
        node.put("actionTakerId", message.getFrom());

        msg.setData(node);
        One2OneChat dbMsg = chatHistoryService.createOne2OneChatHistory(EventType.NotRegisteredMessage, msg);

        msg.setMid(dbMsg.getId());
        msg.setUuid(connection.getUuid());
        String messageText = eventNotificationBuilder.buildMessage(EventType.NotRegisteredMessageSent,
                msg.getData().toString(), message.getFrom());
        msg.setText(messageText);
        userConnectionService.sendMessageToActor(connection.getUserContext().getUser().getId(), connection, msg,
                clientIds);
        logger.debug("Sent One2One System Message messageId " + msg.getMid());

        connection.setHasSentUnregisteredMessage(true);
    }

    private void sendOne2OneTypingMessage(UserConnection connection, ChatMessage message) {
        try {
            logger.debug("send One2One typing message");
            message.setFrom(connection.getUserContext().getUser().getId());
            message.setName(connection.getUserContext().getUser().getName());
            message.setStatus(null);
            List<String> clientIds = CommonUtil.getSupportedClientIds(ChatType.One2One);
            logger.debug("sending One2One Message To Actor ");
            userConnectionService.sendMessageToActor(message.getTo(), connection, message, clientIds);
            logger.debug("sent One2One typing message");
        } catch (ApplicationException ex) {
            throw ex;
        }
    }

    private void handleVideoCallMessage(UserConnection connection, ChatMessage message, List<String> clientIds) {
        // ChatType.One2OneVideoChat in the following line is misleading but it return
        // clientid's used for all video chats.
        List<String> videoCallClientIds = CommonUtil.getSupportedClientIds(ChatType.One2OneVideoChat);

        // For messages requesting to start a video call the system must send 2
        // different messages. The original chat message
        // and a separate system message.
        if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.TwoWayVideoCallRequest.getId()
                .intValue()
                || message.getVideoCallMessageType().intValue() == VideoCallMessageType.OneWayVideoCallRequest.getId()
                .intValue()
                || message.getVideoCallMessageType().intValue() == VideoCallMessageType.AudioCallRequest.getId()
                .intValue()) {

            sendVideoCallRequestSystemMessage(connection, message, videoCallClientIds);

            // send regular chat message to self
            userConnectionService.sendMessageToActor(connection.getUserContext().getUser().getId(), connection, message,
                    videoCallClientIds);
            logger.debug("sent One2One video call message to self :" + message.getText());
            message.setUuid(connection.getUuid());

            // send regular chat message to receiver
            Set<String> msgReceiverClients = userConnectionService.sendMessageToActor(message.getTo(), connection, message, videoCallClientIds);

            Boolean isSendPN = commonUtil.isAlwaysSendPushNofication(message, msgReceiverClients);
            logger.debug("push notification flag = " + isSendPN);
            if (isSendPN) {
                List<Integer> recipients = new ArrayList<>();
                recipients.add(message.getTo());
                logger.warn("contact: " + message.getTo() + "  is offline");
                notificationService.sendVideoCallingPushNotification(NotificationType.Chat.getId().intValue(),
                        message, recipients, PushNotificationVisibility.Recipients, message.getData());
                logger.debug("called sent mobile push notification for users = " + recipients.toString());
            }
            logger.debug("sent One2One video call message to receiver :" + message.getText());
        } else {
            if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallEnded.getId().intValue()) {
                logger.debug("update meeting status to Ended");
                Integer meetingId = updateMeetingInfo(message.getFrom(), message.getData(), VideoCallStatus.Ended.getId());
                logger.debug("updated meeting status to ended for meetingId:" + meetingId);

                sendVideoCallRequestSystemMessage(connection, message, videoCallClientIds);
            } else if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallRejected.getId().intValue()) {
                logger.debug("update meeting status to Rejected");
                Integer meetingId = updateMeetingInfo(message.getFrom(), message.getData(), VideoCallStatus.Rejected.getId());
                logger.debug("updated meeting status to rejected for meetingId:" + meetingId);
            } else if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallAccepted.getId().intValue()) {
                logger.debug("update meeting status to Accepted");
                Integer meetingId = updateMeetingInfo(message.getFrom(), message.getData(), VideoCallStatus.Accepted.getId());
                logger.debug("updated meeting status to accepted for meetingId:" + meetingId);
            } else if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallConnected.getId().intValue()) {
                logger.debug("update meeting status to Connected");
                Integer meetingId = updateMeetingInfo(message.getFrom(), message.getData(), VideoCallStatus.Connected.getId());
                logger.debug("updated meeting status to connected for meetingId:" + meetingId);
            } else if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallDisconnected.getId().intValue()) {
                logger.debug("update meeting status to Disconnected");
                Integer meetingId = updateMeetingInfo(message.getFrom(), message.getData(), VideoCallStatus.Disconnected.getId());
                logger.debug("updated meeting status to disconnected for meetingId:" + meetingId);
            } else if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallIgnored.getId().intValue()) {
                logger.debug("update meeting status to Missed");
                Integer meetingId = updateMeetingInfo(message.getFrom(), message.getData(), VideoCallStatus.Missed.getId());
                logger.debug("updated meeting status to missed for meetingId:" + meetingId);
            } else if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallRating.getId().intValue()) {
                logger.debug("update meeting rating");
                Integer meetingId;
                JsonNode meetingIdNode = message.getData();
                if (meetingIdNode != null) {
                    meetingIdNode = meetingIdNode.findPath("meetingId");

                } else {
                    meetingId = 0;
                }

                if (meetingIdNode != null && meetingIdNode.isMissingNode() == false) {
                    meetingId = meetingIdNode.asInt();
                } else {
                    meetingId = 0;
                }
                meetingInfoDao.updateMeetingRating(meetingId, (byte) 0);
                logger.debug("updated meeting rating for meetingId:" + meetingId);
            }

            // send regular chat message to self
            userConnectionService.sendMessageToActor(connection.getUserContext().getUser().getId(), connection, message,
                    videoCallClientIds);
            logger.debug("sent One2One video call message to self :" + message.getText());
            message.setUuid(connection.getUuid());
            // send regular chat message to receiver
            userConnectionService.sendMessageToActor(message.getTo(), connection, message, videoCallClientIds);
            logger.debug("sent One2One video call message to receiver :" + message.getText());
        }

    }

    private void sendVideoCallRequestSystemMessage(UserConnection connection, ChatMessage message,
                                                   List<String> clientIds) {
        Long time = System.currentTimeMillis();
        String date = commonUtil.getDateTimeWithTimeZone(time, connection.getUserContext().getUser().getTimezone());
        logger.debug("Sending video call request " + message.getVideoCallMessageType() + "system message for user "
                + message.toString());

        ChatMessage msg = new ChatMessage();
        msg.setType(MessageType.Chat.getId());
        msg.setSubtype(Integer.valueOf(ChatType.One2One.getId().intValue()));
        msg.setName(message.getName());
        msg.setUtcDate(message.getUtcDate());
        msg.setDate(message.getDate());
        msg.setFrom(message.getFrom());
        msg.setUtcDate(time);
        msg.setDate(date);
        msg.setTo(message.getTo());
        msg.setVideoCallMessageType(message.getVideoCallMessageType());
        EventType eventType = null;
        JsonNode data = message.getData();
        if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.TwoWayVideoCallRequest.getId()
                .intValue()
                || message.getVideoCallMessageType().intValue() == VideoCallMessageType.OneWayVideoCallRequest.getId()
                .intValue()
                || message.getVideoCallMessageType().intValue() == VideoCallMessageType.AudioCallRequest.getId()
                .intValue()) {
            eventType = EventType.VideoCallRequest;
            msg.setText("");
            msg.setChatMessageType(message.getChatMessageType());

            JsonNode isAudioRecording = data.findPath("isAutoRecording");
            JsonNode meetingIdNode = data.findPath("meetingId");
            ObjectNode dataToSend = getJsonNodeForOne2OneChatMessage(msg);
            if (isAudioRecording != null) {
                dataToSend.put("autoRecordingEnabled", isAudioRecording.asBoolean());
            }
            if (meetingIdNode != null && meetingIdNode.isMissingNode() == false) {
                dataToSend.put("meetingId", meetingIdNode.asInt());
            } else {
                dataToSend.put("meetingId", 0);
            }
            msg.setData(dataToSend);

        } else if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallEnded.getId().intValue()) {
            eventType = EventType.VideoCallEnded;
            msg.setChatMessageType(message.getChatMessageType());

            ObjectNode dataToSend = getJsonNodeForOne2OneChatMessage(msg);
            JsonNode callDurationNode = data.findPath("callDuration");
            JsonNode meetingIdNode = data.findPath("meetingId");
            if (callDurationNode != null) {
                dataToSend.put("callDuration", callDurationNode.asText());
            }
            if (meetingIdNode != null && meetingIdNode.isMissingNode() == false) {
                dataToSend.put("meetingId", meetingIdNode.asInt());
            } else {
                dataToSend.put("meetingId", 0);
            }
            msg.setData(dataToSend);
        }
        msg.setUuid(connection.getUuid());
        msg.setVideoCallMessageType(message.getVideoCallMessageType());
        One2OneChat savedChatMessage = chatHistoryService.createOne2OneChatHistory(eventType, msg);

        msg.setChatMessageType(ChatMessageType.SystemMessage.getId().intValue());
        msg.setMid(savedChatMessage.getId());
        message.setMid(savedChatMessage.getId());

        // Build messages using templates to be sent
        msg.setText(eventNotificationBuilder.buildMessage(eventType, savedChatMessage.getData(),
                savedChatMessage.getTo()));
        Set<String> msgReceiverClients = userConnectionService.sendMessageToActor(msg.getTo(), connection, msg,
                clientIds);


        msg.setText(eventNotificationBuilder.buildMessage(eventType, savedChatMessage.getData(),
                savedChatMessage.getFrom()));
        msgReceiverClients = userConnectionService.sendMessageToActor(msg.getFrom(), connection, msg, clientIds);
        logger.debug("send One2One Message To Actor returned clients who received messages as "
                + msgReceiverClients.toString());
    }
    //}

    @Transactional
    public Integer updateMeetingInfo(Integer from, JsonNode meetingIdNode, Integer videoCallStatus) {

        Integer meetingId;
        if (meetingIdNode != null) {
            meetingIdNode = meetingIdNode.findPath("meetingId");
        } else {
            meetingId = 0;
        }
        if (meetingIdNode != null && meetingIdNode.isMissingNode() == false) {
            meetingId = meetingIdNode.asInt();
        } else {
            meetingId = 0;
        }
        meetingInfoDao.updateMeetingStatus(meetingId, videoCallStatus.byteValue(), from);
        return meetingId;
    }

    private ObjectNode getJsonNodeForOne2OneChatMessage(ChatMessage message) {
        ObjectNode node = Json.newObject();
        node.put("actionTakerId", message.getFrom());
        node.put("affectedMemberId", message.getTo()); // This may sound like it is meant for a group but it was used so
        // the same group message template can be re-used.
        node.put("videoCallMessageType", message.getVideoCallMessageType());
        return node;
    }
}