package core.akka.actors;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import core.entities.Notification;
import core.services.NotificationService;
import core.utils.Constants;
import utils.RmsApplicationContext;

public class NotificationActor extends AbstractActor {
	
	final static Logger logger = LoggerFactory.getLogger(LoggingActor.class);	
	
	private String		id	= UUID.randomUUID().toString();	
	private NotificationService	notificationService;

	public NotificationActor() {
		logger.info("new object created: NotificationActor " + id);
	}

	@Override
	public void preStart() {
		notificationService = (NotificationService) RmsApplicationContext.getInstance().getSpringContext().getBean(Constants.NOTIFICATION_SERVICE_SPRING_BEAN);
	}
	
	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();		
		builder.match(JsonNode.class, message -> {
			logger.debug("json message: "+message);
		});
		builder.match(Notification.class, message -> {
			logger.info("NotificationActor received message, sending notification");
			Notification notification = (Notification) message;
			notificationService.sendNotifications(notification);
		});
		builder.matchAny(message -> {			
			logger.info("string message: "+message);	
		});		
		return builder.build();
	}
}
