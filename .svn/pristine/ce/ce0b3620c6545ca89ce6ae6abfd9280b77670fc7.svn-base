package controllers;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.With;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Singleton;

import controllers.actions.ApiAuthAction;
import core.akka.actors.RmsActorSystem;
import core.entities.Notification;
import core.exceptions.ApplicationException;
import core.exceptions.InternalServerErrorException;
import core.utils.Enums.ErrorCode;

@Singleton
public class NotificationController extends Controller {
	final static Logger logger = LoggerFactory.getLogger(NotificationController.class);
	
	@With(ApiAuthAction.class)
	public Result sendNotification(Request request) {
		try {
			JsonNode jsonNode = request.body().asJson();
			logger.debug("sending notification: " + jsonNode.toString());
			if (jsonNode != null) {
				if (jsonNode.isArray() && jsonNode.size() > 0) {
					for (int indx = 0; indx < jsonNode.size(); indx++) {
						JsonNode json = jsonNode.get(indx);
						Notification notification = Json.fromJson(json, Notification.class);
						logger.debug("sending notification: " + notification);
						RmsActorSystem.getNotificationRouterActorRef().tell(notification, null);
					}
				}
			}
			Result status = created();
			logger.debug("sent notification  with status " + status.status());
			return created();
		} catch (ApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, "Internal server error", ex);
		}
	}
}