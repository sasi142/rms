package core.akka.actors;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import core.entities.PushNotification;
import core.services.PushNotificationService;
import core.utils.Constants;
import utils.RmsApplicationContext;

public class WebBrowserActor extends AbstractActor {
	final static Logger logger = LoggerFactory.getLogger(WebBrowserActor.class);	
	private String			id	= UUID.randomUUID().toString();
	PushNotificationService	pushNotificationService;

	public WebBrowserActor() {
		logger.info("new object created: WebBrowserActor " + id);
	}

	@Override
	public void preStart() {
		pushNotificationService = (PushNotificationService) RmsApplicationContext.getInstance().getSpringContext().getBean(Constants.WEB_BROWSER_SPRING_BEAN);
	}
	
	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();		
		builder.match(JsonNode.class, message -> {
			logger.debug("json message: "+message);
		});
		builder.match(PushNotification.class, message -> {
			logger.info("WebBrowserActor received message, sending to pushNotificationService");
			PushNotification pushNotification = (PushNotification) message;
			logger.debug("web browser msg: " + id + " , " + pushNotification.getMessage());
			pushNotificationService.send(pushNotification);
		});
		builder.matchAny(message -> {			
			logger.info("string message: "+message);	
		});		
		return builder.build();
	}
}