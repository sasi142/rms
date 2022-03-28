package core.akka.actors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

public class FileActor extends AbstractActor {
	final static Logger logger = LoggerFactory.getLogger(FileActor.class);
	public FileActor() {
		logger.info("new object created: FileActor");
	}

	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();		
		builder.match(JsonNode.class, message -> {
			logger.debug("json message: "+message);
		});
		builder.matchAny(message -> {			
			logger.info("string message: "+message);	
		});		
		return builder.build();
	}
}
