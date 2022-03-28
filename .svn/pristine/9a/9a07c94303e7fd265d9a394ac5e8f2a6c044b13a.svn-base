package core.akka.actors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.FromConfig;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import core.utils.Constants;
import core.utils.PropertyUtil;

public class RmsActorSystem {
	final static Logger logger = LoggerFactory.getLogger(RmsActorSystem.class);
	private static ActorSystem	rmsActorSystem;
	private static Materializer materializer;
	private static ActorRef		loggingActor;
	private static String		actorSystemName	= "RmsActorSystem";
	private static ActorRef		gcmRouterActorRef;
	private static ActorRef		apnsRouterActorRef;
	private static ActorRef		eventRouterActorRef;
	private static ActorRef		notificationRouterActorRef;
	private static ActorRef		chatMessageRouterActorRef;
	private static ActorRef		iqMessageRouterActorRef;
	private static ActorRef		webBrowserActorRef;
	private static ActorRef     videoSignallingActorRef;
	
	private static Boolean isActorSystemTerminated = false;

	//private static Map<String, UserConnection> userConnectionMap = new ConcurrentHashMap<>();

	public static ActorSystem create() {
		rmsActorSystem = ActorSystem.create("RmsActorSystem");
		
		rmsActorSystem.registerOnTermination(()->{
			logger.error("Akka System is terminated");
			isActorSystemTerminated = true;
		});		
		
		
		materializer = ActorMaterializer.create(rmsActorSystem);
		loggingActor = rmsActorSystem.actorOf(Props.create(LoggingActor.class), "LoggingActor");
		logger.info("Logging Actor created as " + loggingActor);

		String apnsActorDispatcher = PropertyUtil.getProperty(Constants.APNS_ACTOR_DISPATCHER);
		apnsRouterActorRef = rmsActorSystem.actorOf(FromConfig.getInstance().
				props(Props.create(ApnsActor.class).withDispatcher(apnsActorDispatcher)), "apnsRouter");
		logger.info("Apns Router Actor created as " + apnsRouterActorRef);

		String gcmActorDispatcher = PropertyUtil.getProperty(Constants.GCM_ACTOR_DISPATCHER);
		gcmRouterActorRef = rmsActorSystem.actorOf(FromConfig.getInstance().
				props(Props.create(GcmActor.class).withDispatcher(gcmActorDispatcher)), "gcmRouter");
		logger.info("GCM Router Actor created as " + gcmRouterActorRef);

		String eventActorDispatcher = PropertyUtil.getProperty(Constants.EVENT_ACTOR_DISPATCHER);
		eventRouterActorRef = rmsActorSystem.actorOf(FromConfig.getInstance().
				props(Props.create(EventActor.class).withDispatcher(eventActorDispatcher)), "eventRouter");
		logger.info("Event Router Actor created as " + eventRouterActorRef);

		String notificationActorDispatcher = PropertyUtil.getProperty(Constants.NOTIFICATION_ACTOR_DISPATCHER);
		notificationRouterActorRef = rmsActorSystem.actorOf(FromConfig.getInstance().
				props(Props.create(NotificationActor.class).withDispatcher(notificationActorDispatcher)), "notificationRouter");
		logger.info("Notification Router Actor created as " + notificationRouterActorRef);

		String chatMsgActorDispatcher = PropertyUtil.getProperty(Constants.CHAT_MESSAGE_ACTOR_DISPATCHER);
		chatMessageRouterActorRef = rmsActorSystem.actorOf(FromConfig.getInstance().
				props(Props.create(ChatMessageActor.class).withDispatcher(chatMsgActorDispatcher)), "chatMessageRouter");
		logger.info("Chat Message Router Actor created as " + chatMessageRouterActorRef);
		
		String iqMsgActorDispatcher = PropertyUtil.getProperty(Constants.IQ_MESSAGE_ACTOR_DISPATCHER);
		iqMessageRouterActorRef = rmsActorSystem.actorOf(FromConfig.getInstance().
				props(Props.create(IqMessageActor.class).withDispatcher(iqMsgActorDispatcher)), "iqMessageRouter");
		logger.info("Iq Message Router Actor created as " + iqMessageRouterActorRef);

		String webBrowserActorDispatcher = PropertyUtil.getProperty(Constants.WEB_BROWSER_ACTOR_DISPATCHER);
		webBrowserActorRef = rmsActorSystem.actorOf(FromConfig.getInstance().
				props(Props.create(WebBrowserActor.class).withDispatcher(webBrowserActorDispatcher)), "webBrowserRouter");
		logger.info("Web Browser Router Actor created as " + webBrowserActorRef);
		
		String videoSignallingActorDispatcher = PropertyUtil.getProperty(Constants.VIDEO_SIGNALLING_ACTOR_DISPATCHER);
		videoSignallingActorRef = rmsActorSystem.actorOf(FromConfig.getInstance().
				props(Props.create(VideoSignallingActor.class).withDispatcher(videoSignallingActorDispatcher)), "videoSignallingRouter");
		logger.info("video signalling actor ref " + videoSignallingActorRef);
		
		return rmsActorSystem;
	}

	public static ActorSystem get() {
		return rmsActorSystem;
	}

	public static Materializer getMaterializer() {
		return materializer;
	}

	public static ActorRef getLoggingActor() {
		return loggingActor;
	}

	public static String getActorSystemName() {
		return actorSystemName;
	}

	public static ActorRef getGcmRouterActorRef() {
		return gcmRouterActorRef;
	}

	public static ActorRef getApnsRouterActorRef() {
		return apnsRouterActorRef;
	}

	public static ActorRef getEventRouterActorRef() {
		return eventRouterActorRef;
	}

	public static ActorRef getNotificationRouterActorRef() {
		return notificationRouterActorRef;
	}
	
	public static ActorRef getChatMessageRouterActorRef() {
		return chatMessageRouterActorRef;
	}
	
	public static ActorRef getIqMessageRouterActorRef() {
		return iqMessageRouterActorRef;
	}

	public static ActorRef getWebBrowserActorRef() {
		return webBrowserActorRef;
	}

	public static ActorRef getVideoSignallingActorRef() {
		return videoSignallingActorRef;
	}
	
	public static Boolean isActorSystemTerminated() {
		return isActorSystemTerminated;
	}
}
