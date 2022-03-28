package controllers.exceptionMapper;

import core.exceptions.ForbiddenException;
import core.exceptions.UnAuthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.entities.ChatMessage;
import core.entities.RmsMessage;
import core.exceptions.ApplicationException;
import core.exceptions.ContactOfflineException;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.MessageType;

@Component
public class WebsocketExceptionHandler {
	final static Logger logger = LoggerFactory.getLogger(WebsocketExceptionHandler.class);
	public JsonNode handle(Throwable ex, RmsMessage rmsMessage) {
		JsonNode jsonMessage = rmsMessage.getJsonNode();

		JsonNode errorJson = null;
		Exception detailed = null;
		logger.error("exception is ", ex);
		if (ex instanceof ApplicationException ||
				ex instanceof ForbiddenException) {
			ApplicationException e = (ApplicationException) ex;
			ObjectNode node = getErrorNode(jsonMessage);
			node.put("code", e.getCode().getId());
			node.put("text", e.getMessage());
			errorJson = Json.toJson(node);
			detailed = e.getEx();
		} else {
			ObjectNode node = getErrorNode(jsonMessage);
			node.put("code", ErrorCode.Internal_Server_Error.getId());
			node.put("text", ErrorCode.Internal_Server_Error.getName());
			errorJson = Json.toJson(node);
		}
		if (ex instanceof ContactOfflineException) {
			logger.info("contact is offline: ");
		}
		else if (ex instanceof UnAuthorizedException) {
			logger.error("UnAuthorizedException: "+ex.getMessage());
		}
		else if (detailed != null && detailed instanceof UnAuthorizedException) {
			logger.error("UnAuthorizedException: "+detailed.getMessage());
		}
		else {
			logger.error("failed to send message: ", ex);
			if (detailed != null) {
				logger.error("detailed exception is", detailed);
			}
		}
		return errorJson;
	}

	public JsonNode handle(Exception ex) {		
		JsonNode errorJson = null;
		ApplicationException appException = (ApplicationException) ex;
		ObjectNode node = Json.newObject();
		node.put("code", appException.getCode().getId());
		node.put("text", appException.getMessage());
		node.put("type", MessageType.Error.getId());
		if (ex instanceof UnAuthorizedException) {
			node.put("httpCode", 401);
			node.put("message", "UnAuthorizedException");
			logger.error("socket UnAuthorizedException: "+ex.getMessage());
		} else if (ex instanceof ForbiddenException) {
			node.put("httpCode", 403);
			node.put("message", "ForbiddenException");
			logger.error("socket ForbiddenException: "+ex.getMessage());
		} else {
			node.put("httpCode", 500);
			node.put("message", "InternalServerErrorException");
			logger.error("Exception occuered on socket", ex);
		}
		errorJson = Json.toJson(node);
		return errorJson;
	}

	private ObjectNode getErrorNode(JsonNode jsonMessage) {
		int type = jsonMessage.findPath("type").asInt();
		int subType = jsonMessage.findPath("subtype").asInt();

		ObjectNode node = Json.newObject();
		node.put("type", MessageType.Error.ordinal());
		node.put("subtype", type);
		if (type == MessageType.Chat.getId()) {
			ChatMessage chatMessage = Json.fromJson(jsonMessage, ChatMessage.class);
			node.put("cid", chatMessage.getCid() == null ? "" : chatMessage.getCid());
			node.put("chatType", subType);
		}
		if (type == MessageType.IQ.getId()) {
			String action = jsonMessage.findPath("action").asText();
			node.put("action", action);
		}
		return node;
	}
}
