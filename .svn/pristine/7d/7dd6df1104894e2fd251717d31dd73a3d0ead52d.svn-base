package core.akka.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.exceptionMapper.WebsocketExceptionHandler;
import core.entities.IqMessage;
import core.entities.RmsMessage;
import core.services.IqMessageService;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums;
import core.utils.Enums.IqActionType;
import core.utils.Enums.RmsMessageType;
import core.utils.ThreadContext;
import messages.UserConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import play.libs.Json;
import utils.RmsApplicationContext;

public class IqMessageActor extends AbstractActor {
    final static Logger logger = LoggerFactory.getLogger(IqMessageActor.class);
    private WebsocketExceptionHandler websocketExceptionHandler;
    private IqMessageService iqMessageService;

    public IqMessageActor() {
        logger.info("IqMessageActor created");
    }

    @Override
    public void preStart() {
        iqMessageService = (IqMessageService) RmsApplicationContext.getInstance().getSpringContext().getBean(Constants.IQ_MESSAGE_SERVICE_SPRING_BEAN);
        websocketExceptionHandler = (WebsocketExceptionHandler) RmsApplicationContext.getInstance().getSpringContext()
                .getBean(Constants.WEBSOCKET_EXCEPTION_HANDLER_SPRING_BEAN);
    }

    @Override
    public Receive createReceive() {
        ReceiveBuilder builder = ReceiveBuilder.create();
        builder.match(JsonNode.class, message -> {
            logger.debug("json message: " + message);
        });
        builder.match(RmsMessage.class, this::handleRmsMessage);
        builder.matchAny(message -> {
            logger.info("string message: " + message);
        });
        return builder.build();
    }

    private void handleRmsMessage(RmsMessage rmsMessage){
        MDC.put("ConnId", rmsMessage.getConnId().getId());
        handleRmsMessage2(rmsMessage);
    }

    private void handleRmsMessage2(RmsMessage rmsMessage) {
        UserConnection connection = RmsApplicationContext.getInstance().getActorMonitor().getUserConnection(rmsMessage.getConnectionId());
        if (connection == null) {
            logger.warn("Connection not found for connectionId {}", rmsMessage.getConnectionId());
            return;
        }
        if (connection.getActorRef() == null) {
            logger.warn("Connection actor not found for connectionId {}", rmsMessage.getConnectionId());
            return;
        }

        CommonUtil.addUserInfoLoggingContext(connection.getUserContext());

        //Get the user id
        Integer userId = null;
        if (connection.getUserContext() != null && connection.getUserContext().getUser() != null){
            userId = connection.getUserContext().getUser().getId();
        }


        ThreadContext.set(connection);
        JsonNode jsonMessage = rmsMessage.getJsonNode();
        int type = jsonMessage.findPath("type").asInt();
        int subType = jsonMessage.findPath("subtype").asInt();
        String action = jsonMessage.findPath("action").asText();
        if (logger.isDebugEnabled()){
            logger.debug("IQ message received for user {}, connection Id {}, type {}, subType {}, action {}, rmsMessageType {}, content {}",
                    userId, rmsMessage.getConnectionId(), type, subType, action, rmsMessage.getRmsMessageType(), jsonMessage.toString());
        }
        else {
            logger.info("IQ message received for user {}, connection Id {}, type {}, subType {}, action {}, rmsMessageType {}",
                    userId, rmsMessage.getConnectionId(), type, subType, action, rmsMessage.getRmsMessageType());
        }
        RmsApplicationContext.getInstance().getActorMonitor().recordMsgCounts(rmsMessage.getRmsMessageType(), type, subType);

        IqActionType iqActionType = Enums.get(IqActionType.class, action);

        ObjectNode params = (ObjectNode) jsonMessage.get("params");

        switch (iqActionType){
            case GetChatHistory:
            case GetContacts:
            case GetContactsForFirstTime:
            case GetContactCountToSync:
            case GetProfileInfo:
            case GetContactsV2:
            case GetOrgContacts:
            case GetNonOrgContacts:
            case GetGroupAndNonOrgContacts:
                paramsFix(params, "timeRanges");
                break;
            case GetPresenceLocAndUnreadCount:
            case GetUnreadCountV2:
                paramsFix(params, "contacts");
                break;
            case UpdateChatStatusReadV2:
                paramsFix(params, "chat-messages");
                break;
            case DeleteChatMessage:
                paramsFix(params, "mids");
                break;
            default:
                //TODO: What here??
        }

        try {
            IqMessage iqMessage = Json.fromJson(jsonMessage, IqMessage.class);
            iqMessageService.handleIqRequest(connection, iqMessage);
            logger.info("IQ message completed for user {}, connection Id {}, type {}, subType {}, action {}, rmsMessageType {}",
                    userId, rmsMessage.getConnectionId(), type, subType, action, rmsMessage.getRmsMessageType());
        } catch (Exception ex) {
            logger.error("IQ message failed for user {}, connection Id {}, type {}, subType {}, action {}, rmsMessageType {}",
                    userId, rmsMessage.getConnectionId(), type, subType, action, rmsMessage.getRmsMessageType(), ex);
            JsonNode errorJson = websocketExceptionHandler.handle(ex, rmsMessage);
            if (errorJson != null) {
                logger.error("IQ message failed for user {}, connection Id {}, type {}, subType {}, action {}, rmsMessageType {}, errorData {}",
                        userId, rmsMessage.getConnectionId(), type, subType, action, rmsMessage.getRmsMessageType(), errorJson);
                RmsMessage errorMessage = new RmsMessage(errorJson, RmsMessageType.Out);
                connection.getActorRef().tell(errorMessage, ActorRef.noSender());
            }
        }
    }

    private void paramsFix(ObjectNode params, String key){
        params.put(key, params.get(key) == null ? "" : params.get(key).toString());
    }

    @Override
    public void postStop() {
        logger.info("IqMessageActor stopped for connection");
    }
}
