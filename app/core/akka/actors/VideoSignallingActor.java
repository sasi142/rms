package core.akka.actors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import core.entities.PushNotification;
import core.entities.RmsMessage;
import core.entities.VideoSignallingMessage;
import core.services.VideoSignallingService;
import core.utils.Constants;
import utils.RmsApplicationContext;
import play.libs.Json;
import messages.UserConnection;
import core.utils.CommonUtil; 

public class VideoSignallingActor extends AbstractActor {
	final static Logger logger = LoggerFactory.getLogger(VideoSignallingActor.class);
	
	private VideoSignallingService	videoSignallingService;

	public VideoSignallingActor() {
		logger.info("new object created: VideoSignallingActor ");
	}

	@Override
	public void preStart() {
		videoSignallingService = (VideoSignallingService) RmsApplicationContext.getInstance().getSpringContext().getBean(Constants.VIDEO_SIGNALLING_SERVICE_SPRING_BEAN);
	}

	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();		
		builder.match(JsonNode.class, message -> {
			logger.debug("json message: "+message);
		});
		builder.match(RmsMessage.class, rmsMessage -> {
			logger.info("VideoSignallingActor received message");
			UserConnection connection = RmsApplicationContext.getInstance().getActorMonitor().getUserConnection(rmsMessage.getConnectionId());
			CommonUtil.addUserInfoLoggingContext(connection.getUserContext());
			JsonNode jsonMessage = rmsMessage.getJsonNode();
			JsonNode node = jsonMessage.findPath("signallingData");
			VideoSignallingMessage signallingMessage = Json.fromJson(jsonMessage, VideoSignallingMessage.class);
			signallingMessage.setSignallingData(node);
			videoSignallingService.handleMessage(connection, signallingMessage);
		});
		builder.matchAny(message -> {			
			logger.info("string message: "+message);	
		});		
		return builder.build();
	}	
}
