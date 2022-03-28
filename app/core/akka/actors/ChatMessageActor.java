package core.akka.actors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import core.utils.CommonUtil;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import controllers.exceptionMapper.WebsocketExceptionHandler;
import core.entities.ChatMessage;
import core.entities.RmsMessage;
import core.services.ChatMessageService;
import core.utils.ActorThreadContext;
import core.utils.Constants;
import core.utils.Enums.RmsMessageType;
import core.utils.ThreadContext;
import messages.UserConnection;
import play.libs.Json;
import utils.RmsApplicationContext;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ChatMessageActor extends AbstractActor {
	final static Logger logger = LoggerFactory.getLogger(ChatMessageActor.class);
	private ChatMessageService			chatMessageService;
	private WebsocketExceptionHandler	websocketExceptionHandler;
	
	public ChatMessageActor() {
		logger.info("ChatMessageActor created");
	}

	@Override
	public void preStart() {
		chatMessageService = (ChatMessageService) RmsApplicationContext.getInstance().getSpringContext().getBean(Constants.CHAT_MESSAGE_SERVICE_SPRING_BEAN);
		websocketExceptionHandler = (WebsocketExceptionHandler) RmsApplicationContext.getInstance().getSpringContext()
				.getBean(Constants.WEBSOCKET_EXCEPTION_HANDLER_SPRING_BEAN);
	}


	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();		
		builder.match(JsonNode.class, message -> {
			logger.debug("json message: "+message);
		});
		builder.match(RmsMessage.class, this::handleRmsMessage);
		builder.matchAny(message -> {			
			logger.info("string message: "+message);	
		});		
		return builder.build();
	}

	private void handleRmsMessage(RmsMessage rmsMessage){
		MDC.put("ConnId", rmsMessage.getConnId().getId());
		handleRmsMessage2(rmsMessage);
	}

	private void handleRmsMessage2(RmsMessage rmsMessage) {
		UserConnection connection = RmsApplicationContext.getInstance().getActorMonitor().getUserConnection(rmsMessage.getConnectionId());
		if (connection != null && connection.getActorRef() != null) {
			CommonUtil.addUserInfoLoggingContext(connection.getUserContext());
			JsonNode jsonMessage = rmsMessage.getJsonNode();
			ThreadContext.set(connection);
			ActorThreadContext.unset();
			int type = jsonMessage.findPath("type").asInt();
			int subType = jsonMessage.findPath("subtype").asInt();
			RmsApplicationContext.getInstance().getActorMonitor().recordMsgCounts(rmsMessage.getRmsMessageType(), type, subType);
			logger.info("Process chat message : " + jsonMessage.toString() + ",RmsMsgType:" + rmsMessage.getRmsMessageType().name() + " and MessageType : " + type);
			try {
				ChatMessage chatMessage = Json.fromJson(jsonMessage, ChatMessage.class);
				chatMessageService.sendMessage(connection, chatMessage);
				logger.debug("Completed chat send message :" + jsonMessage.toString());

			} catch (Exception ex) {
				logger.error("Exception occuered on socket", ex);
				JsonNode errorJson = websocketExceptionHandler.handle(ex, rmsMessage);
				if (errorJson != null) {
					logger.error("error message: " + errorJson.toString());
					RmsMessage errorMessage = new RmsMessage(errorJson, RmsMessageType.Out);
					connection.getActorRef().tell(errorMessage, ActorRef.noSender());
				}
			}
		}
		else{
			logger.error("Connection not found for the message connection id {}", rmsMessage.getConnectionId());
		}
	}

	@Override
	public void postStop() {

	}

	@Override
	public void unhandled(Object message) {
		logger.error("Received unhandled message - {}", message);
	}
}
