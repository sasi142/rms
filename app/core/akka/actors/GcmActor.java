package core.akka.actors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import core.entities.PushNotification;
import core.services.PushNotificationService;
import core.utils.Constants;
import utils.RmsApplicationContext;

public class GcmActor extends AbstractActor {
	final static Logger logger = LoggerFactory.getLogger(GcmActor.class);
	PushNotificationService	pushNotificationService;

	public GcmActor() {
		logger.info("new object created: GcmActor");
	}

	@Override
	public void preStart() {
		pushNotificationService = (PushNotificationService) RmsApplicationContext.getInstance().getSpringContext().getBean(Constants.GCM_SPRING_BEAN);
	}

	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();		
		builder.match(JsonNode.class, message -> {
			logger.debug("json message: "+message);
		});
		builder.match(PushNotification.class, message -> {
			logger.info("GcmActor received message, sending to pushNotificationService");
			PushNotification pushNotification = (PushNotification) message;
			logger.debug("gcm msg: " + pushNotification.getMessage());
			pushNotificationService.send(pushNotification);
		});
		builder.matchAny(message -> {			
			logger.info("string message: "+message);	
		});		
		return builder.build();
	}
}
