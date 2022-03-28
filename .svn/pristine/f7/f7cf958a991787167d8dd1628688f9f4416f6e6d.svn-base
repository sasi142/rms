package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import messages.RoomMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import utils.RmsApplicationContext;
import play.mvc.Result;
import play.mvc.With;
import views.html.chatRoom;
import views.html.index;
import akka.actor.ActorRef;
import controllers.actions.UserAuthAction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Singleton;

import core.akka.actors.ChatMessageActor;
import core.akka.actors.RmsActorSystem;
import core.entities.PushNotification;
import core.entities.UserContext;
import core.services.ApnsPushNotificationServiceImpl;
import core.services.PushNotificationService;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums.MessageType;
import core.utils.Enums.NotificationType;
import core.utils.PropertyUtil;
import core.utils.ThreadContext;
import hello.BuildInfo;

@Singleton
public class MainController extends Controller {
	final static Logger logger = LoggerFactory.getLogger(MainController.class);

	private Environment				env;

	private Integer					systemUserId;

	private CommonUtil				commonUtil;

	//@Autowired
	//@Qualifier("ApnsPushNotification")
	//private PushNotificationService	pushNotificationService;


	public void init() {
		systemUserId = Integer.valueOf(env.getProperty(Constants.SYSTEM_USER_ID));
	}

	public MainController() {
		ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
		commonUtil = (CommonUtil) ctx.getBean(Constants.COMMON_UTIL_SPRING_BEAN);
		env = ctx.getBean(Environment.class);
		//pushNotificationService = (PushNotificationService) ctx.getBean(Constants.APNS_SPRING_BEAN);
		init();
	}

	public Result healthCheck() {
		logger.info("healthCheck");
		String rmsBuildNumber = PropertyUtil.getProperty(Constants.RMS_BUILD_NUMBER);		
		ObjectNode node = Json.newObject();
		node.put("name", hello.BuildInfo.name());
		node.put("version", hello.BuildInfo.version());
		Boolean isTerminated = RmsActorSystem.isActorSystemTerminated();
		if (isTerminated) {
			logger.error("Is Actor System Terminated: "+isTerminated);
			return internalServerError("Actor system terminated. ");
		}
		else {
			logger.debug("Is Actor System Terminated: "+isTerminated);
		}
		node.put("actorTerminated", isTerminated);
		return ok(node);
	}
	
	public Result shutdown() {
		logger.info("shut down actor system for testing");
		RmsActorSystem.get().terminate();

		return ok();
	}

	public Result catchAll(String path) {
		logger.info("invalid path accessed: "+path);
		return notFound();
	}

	public Result deviceCheck(String token, String message, String type) {
		logger.info("deviceCheck for token " + token + " message " + message + " type " + type);
		PushNotification pushNotification = new PushNotification();
		String strPushNotification = getPushNotification(message).toString();
		logger.debug("strPushNotification created as " + strPushNotification);
		pushNotification.setMessage(strPushNotification);
		pushNotification.setFrom(10);

		if ("chat".equalsIgnoreCase(type)) {
			pushNotification.setNotificationType(NotificationType.Chat);
		} else {
			pushNotification.setNotificationType(NotificationType.WorkApps);
		}

		List<String> rec = new ArrayList<>();
		rec.add(token);
		// pushNotification.setDevices(rec);

		// pushNotificationService.send(pushNotification);
		return ok();
	}

	@With(UserAuthAction.class)
	public Result removeConnectionInfo(String ip) {
		logger.info("clean up connection for ip: " + ip);
		UserContext userContext = ThreadContext.getUserContext();//(UserContext) ctx().args.get("usercontext");

		if (!systemUserId.equals(userContext.getUser().getId())) {
			return forbidden();
		}
		commonUtil.removeConnectionInfo(ip);
		logger.info("cleaned up connection for ip: " + ip);
		return ok();
	}

	@With(UserAuthAction.class)
	public Result index() {
		return ok(index.render());
	}

	public Result chatRoom(String username) {
		if (username == null || username.trim().equals("")) {
			flash("error", "Please choose a valid username.");
			return redirect(routes.MainController.index());
		}
		return ok(chatRoom.render(username));
	}

	public Result chatRoomJs(final String username) {
		return ok(views.js.chatRoom.render(username));
	}

	public Result getRoomMessage() {
		RoomMessage m = new RoomMessage("Workapps", "Pune", "Enterprise");
		JsonNode personJson = Json.toJson(m);
		return ok(personJson);
	}

	public class ActorBox {
		private ActorRef	actor;
		public ActorBox() {
			actor = null;
		}
		public ActorRef getActor() {
			return actor;
		}
		public void setActor(ActorRef actor) {
			this.actor = actor;
		}
	}

	public ObjectNode getPushNotification(String message) {
		ObjectNode pushNotification = Json.newObject();
		ObjectNode data = Json.newObject();
		data.put("type", MessageType.Notification.getId());
		data.put("subtype", 1);
		data.put("to", 100);
		data.put("from", 200);
		data.put("name", "Shankar");

		ObjectNode aps = Json.newObject();
		aps.put("alert", message);
		aps.put("sound", "default");
		aps.put("category", "reply");

		pushNotification.set("aps", aps);
		pushNotification.set("data", data);
		return pushNotification;
	}
}