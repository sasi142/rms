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

public class ApnsActor extends AbstractActor {
	final static Logger logger = LoggerFactory.getLogger(ApnsActor.class);
	private String			id	= UUID.randomUUID().toString();
	PushNotificationService	pushNotificationService;

	public ApnsActor() {
		logger.info("new object created: ApnsActor " + id);
	}

	@Override
	public void preStart() {
		pushNotificationService = (PushNotificationService) RmsApplicationContext.getInstance().getSpringContext().getBean(Constants.APNS_SPRING_BEAN);
	}

	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();		
		builder.match(JsonNode.class, message -> {
			logger.debug("json message: "+message);
		});
		builder.match(PushNotification.class, message -> {
			logger.info("ApnsActor received message, sending to pushNotificationService");
			PushNotification pushNotification = (PushNotification) message;
			logger.debug("apns msg: " + id + " , " + pushNotification.getMessage());
			pushNotificationService.send(pushNotification);
		});
		builder.matchAny(message -> {			
			logger.info("string message: "+message);	
		});		
		return builder.build();
	}	
}
