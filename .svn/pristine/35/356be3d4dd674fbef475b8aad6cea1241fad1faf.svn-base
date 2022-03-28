package core.akka.actors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;

public class LoggingActor extends AbstractActor {
	final static Logger logger = LoggerFactory.getLogger(LoggingActor.class);
	public ActorRef	fileActor;

	public LoggingActor() {
		logger.info("new object LoggingActor created");
		fileActor = getContext().actorOf(Props.create(FileActor.class), "FileActor");
	}
	
	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();		
		builder.match(JsonNode.class, message -> {
			logger.debug("json message: "+message);
		});
		builder.matchAny(message -> {			
			logger.debug("onReceive: LoggingActor - " + message);
			fileActor.tell(message, getSelf());
		});		
		return builder.build();
	}
}
