package core.akka.actors;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import core.entities.Event;
import core.services.EventService;
import core.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import utils.RmsApplicationContext;

import java.util.UUID;

public class EventActor extends AbstractActor {
	final static Logger logger = LoggerFactory.getLogger(EventActor.class);
	private String			id	= UUID.randomUUID().toString();

	private EventService	eventService;

	public EventActor() {
		logger.info("new object created: EventActor " + id);
	}

	@Override
	public void preStart() {
		eventService = (EventService) RmsApplicationContext.getInstance().getSpringContext().getBean(Constants.EVENT_SERVICE_SPRING_BEAN);
	}
	
	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();		
		builder.match(JsonNode.class, message -> {
			logger.debug("json message: "+message);
		});
		builder.match(Event.class, event -> {
			logger.info("EventActor received message id : {}, type: {}", event.getId(), event.getType());
			logger.debug("EventActor Message {}", event);
			MDC.put("ConnId", event.getConnId().getId());
			eventService.handleEvent(event);
		});
		builder.matchAny(message -> {
			logger.info("string message: "+message);	
		});		
		return builder.build();
	}	
}
