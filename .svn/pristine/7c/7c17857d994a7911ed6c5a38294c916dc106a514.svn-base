package core.akka.actors;

import akka.Done;
import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.actor.PoisonPill;
import akka.japi.Pair;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.Materializer;
import akka.stream.javadsl.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.exceptionMapper.WebsocketExceptionHandler;
import core.entities.ActorMonitor;
import core.entities.ConnectionInfo;
import core.entities.RmsMessage;
import core.entities.User;
import core.services.InfoService;
import core.services.UserConnectionService;
import core.services.VideokycService;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums;
import core.utils.Enums.*;
import messages.UserConnection;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import play.libs.Json;
import utils.RmsApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;


public class UserConnectionActor extends AbstractActor {
    final static Logger logger = LoggerFactory.getLogger(UserConnectionActor.class);
    private final UserConnection connection;
    private WebsocketExceptionHandler websocketExceptionHandler;
    private InfoService infoService;
    private UserConnectionService userConnectionService;
    private VideokycService videokycService;
    private ActorMonitor actorMonitor;
    private Flow<JsonNode, JsonNode, NotUsed> websocketFlow;
    private Sink<JsonNode, NotUsed> wsClientSink;
    private Long lastTokenValidationTime;
    private Long tokenValidationTimeLimit;

    private Materializer materializer;

    public UserConnectionActor(UserConnection connection) {
        this.connection = connection;
    }

    @Override
    public void preStart() {
        ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
        actorMonitor = RmsApplicationContext.getInstance().getActorMonitor();
        lastTokenValidationTime = System.currentTimeMillis();
        websocketExceptionHandler = (WebsocketExceptionHandler) ctx.getBean(Constants.WEBSOCKET_EXCEPTION_HANDLER_SPRING_BEAN);
        userConnectionService = (UserConnectionService) ctx.getBean(Constants.USER_CONNECTION_SERVICE_SPRING_BEAN);
        infoService = (InfoService) ctx.getBean(Constants.INFO_SERVICE_SPRING_BEAN);
        videokycService = (VideokycService) ctx.getBean(Constants.VIDEOKYC_SERVICE_SPRING_BEAN);

        logger.info("Application Context: " + ctx);
        Environment env = ctx.getBean(Environment.class);
        tokenValidationTimeLimit = Long.parseLong(env.getProperty(Constants.TOKEN_VALIDATION_TIME_LIMIT));
        logger.info("tokenValidationTimeLimit: " + tokenValidationTimeLimit);
        if (connection.getEx() == null) { // no exception
            logger.info("preStart:" + connection.getUserContext().getUser().getId() + "uuid:" + connection.getUuid());
            ConnectionInfo connInfo = new ConnectionInfo(connection, System.currentTimeMillis());
            actorMonitor.getActorMap().put(connection.getUuid(), connInfo);
            logger.info("preStart uuid:" + connection.getUuid());
        }
    }

    public static class Create {
        final String id;

        public Create(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public UserConnectionActor(UserConnection connection, Materializer materializer) {
        this.connection = connection;

        this.materializer = materializer;

        Pair<Sink<JsonNode, NotUsed>, Source<JsonNode, NotUsed>> sinkSourcePair =
                MergeHub.of(JsonNode.class, 16)
                        .toMat(BroadcastHub.of(JsonNode.class, 256), Keep.both())
                        .run(materializer);

        Sink<JsonNode, NotUsed> hubSink = sinkSourcePair.first();
        Source<JsonNode, NotUsed> hubSource = sinkSourcePair.second();


        Sink<JsonNode, CompletionStage<Done>> jsonSink = Sink.foreach(json -> {
            setGuid(json);
            handleIncomingMessage(json);
        });

        // Put the source and sink together to make a flow of hub source as output (aggregating all
        // stocks as JSON to the browser) and the actor as the sink (receiving any JSON messages
        // from the browse), using a coupled sink and source.
        this.websocketFlow = Flow.fromSinkAndSource(jsonSink, hubSource)
                .watchTermination((n, stage) -> {// When the flow shuts down, make sure this actor also stops.
                    stage.thenAccept(f -> context().stop(self()));
                    return NotUsed.getInstance();
                });

        RunnableGraph<Sink<JsonNode, NotUsed>> runnableGraph = MergeHub.of(JsonNode.class, 16).to(hubSink);
        wsClientSink = runnableGraph.run(this.materializer);
    }

    @Override
    public Receive createReceive() {
        ReceiveBuilder builder = ReceiveBuilder.create();
        builder.match(JsonNode.class, message -> {
            logger.debug("json message: " + message);
        });


        builder.match(RmsMessage.class, rmsMessage -> {
            setGuid(rmsMessage);
            handleOutgoingRmsMessage(rmsMessage);
        });
        builder.match(UserConnectionActor.Create.class, this::handleOpenSocketMessage);
        builder.match(PoisonPill.class, message -> {
            stopActor();
            getContext().stop(connection.getActorRef());
        });
        builder.matchAny(message -> {
            logger.info("string message: " + message);
        });

        return builder.build();
    }

    private void handleIncomingMessage(JsonNode message) {
        int type = message.get("type").asInt();
        MessageType messageType = Enums.get(MessageType.class, type);
        //Get the user id
        Integer userId = null;
        if (connection.getUserContext() != null && connection.getUserContext().getUser() != null) {
            userId = connection.getUserContext().getUser().getId();
        }
        try {
            handleIncomingMessage2(message, messageType);
        } catch (Exception ex) {
            logger.error("UserActor message failed for user {}, connection Id {}, type {}",
                    userId, connection.getUuid(), type, ex);
            RmsMessage rmsMessage = new RmsMessage(message, RmsMessageType.In);
            JsonNode errorJson = websocketExceptionHandler.handle(ex, rmsMessage);
            if (errorJson != null) {
                logger.error("UserActor message failed for user {}, connection Id {}, type {}, errorData {}",
                        userId, rmsMessage.getConnectionId(), type, errorJson);
            }
        }
    }

    private void handleIncomingMessage2(JsonNode message, MessageType messageType) {
        if (messageType == MessageType.Ping) {
            handlePing(message);
        } else {
            CommonUtil.addUserInfoLoggingContext(connection.getUserContext());
            logger.debug("Incoming message over connection {} is {}", connection.getUuid(), message);
            RmsMessage rmsMessage = new RmsMessage(connection.getConnId(), message, RmsMessageType.In);
            rmsMessage.setConnectionId(connection.getUuid());
            int subType = message.findPath("subtype").asInt();
           
            actorMonitor.recordMsgCounts(rmsMessage.getRmsMessageType(), messageType.getId(), subType);
            switch (messageType) {            
                case IQ:
                    RmsActorSystem.getIqMessageRouterActorRef().tell(rmsMessage, null);
                    break;
                case Chat:
                case Typing:
                case StopTyping:
                case ACK:
                    rmsMessage.setConnectionId(connection.getUuid());
                    RmsActorSystem.getChatMessageRouterActorRef().tell(rmsMessage, null);
                    break;
                case VideoSignalling:
                	logger.info("videoSignalling Message: "+ rmsMessage.toString());
                    RmsActorSystem.getVideoSignallingActorRef().tell(rmsMessage, null);
                    break;
                case Info:
                    infoService.sendInfo(rmsMessage);
                    break;
                case MeetingInfoTrackingEvent:
                case UserInfoTrackingEvent:
                    infoService.createTrackingEvent(rmsMessage);
                    break;
                default:
                    break;

            }
        }
    }

    private void handlePing(JsonNode message) {
        ActorMonitor actorMonitor = RmsApplicationContext.getInstance().getActorMonitor();
        ConnectionInfo actorInfo = actorMonitor.getActorMap().get(connection.getUuid());
        validateToken();

        if (actorInfo != null) {
            actorInfo.setPingTime(System.currentTimeMillis());
            logger.info("ping recived for userId: " + actorInfo.getUserId());

            // TODO: As per yogesh, ping time is not required in DB. So removing now
            //updatePingTimeInQueue(actorInfo);
        }
        ActorMonitor.incrementCount(RmsMessageType.In, MessageType.Ping.name(), "");
        ((ObjectNode) message).remove("waguid");
        sendMessageToClient(message);
    }

    private void sendMessageToClient(JsonNode message) {
        Source.single(message).runWith(this.wsClientSink, this.materializer);
    }

    private void handleOutgoingRmsMessage(RmsMessage message) {
        try {
            logger.info("waguid: " + message.getWaguid());
            JsonNode jsonMessage = message.getJsonNode();// Json.parse(rmsMessage.getJsonNode());
            //int type = jsonMessage.findPath("type").asInt();
            int type = jsonMessage.get("type").asInt();
            logger.info("message type  :" + type);
            int subType = jsonMessage.findPath("subtype").asInt();
            actorMonitor.recordMsgCounts(message.getRmsMessageType(), type, subType);
            logger.info("Process message ,RmsMsgType : " + message.getRmsMessageType().name() + " and MessageType : " + type);
            logger.debug("Processing message : " + jsonMessage.toString());

            if (message.getRmsMessageType() == RmsMessageType.Out) {
                if (type == MessageType.Chat.getId().intValue()) {
                    if (ClientType.isWebClient(connection.getUserContext().getClientId())) {
                        String originalText = jsonMessage.findPath("text").asText();
                        String htmlEscapedText = StringEscapeUtils.escapeHtml4(originalText);
                        ((ObjectNode) jsonMessage).put("text", htmlEscapedText);
                    }
                }
                if (type == MessageType.Event.getId().intValue() && jsonMessage.findPath("subtype").asInt() == EventType.UserUpdate.getId().intValue()) {
                    Integer Id = jsonMessage.get("id").asInt();
                    logger.debug("Message Id : " + Id + " with MessageType.Event and EventType.UserUpdate");
                    if (connection.getUserContext().getUser().getId().equals(Id)) {
                        userConnectionService.updateUserInUserContext(connection);
                    }
                }
                if (type == MessageType.Event.getId().intValue() && jsonMessage.findPath("subtype").asInt() == EventType.Logout.getId().intValue()) {
                    String authToken = jsonMessage.get("AuthToken").asText();
                    if (connection.getUserContext().getToken().equalsIgnoreCase(authToken)) {
                        stopActor();
                    }
                } else if (type == MessageType.IQ.getId().intValue() && jsonMessage.findPath("subtype").asInt() == IqType.Response.getId().intValue()) {
                    String action = jsonMessage.findPath("action").asText();
                    if (IqActionType.GetChatHistory.getName().equalsIgnoreCase(action)
                            || IqActionType.GetContacts.getName().equalsIgnoreCase(action)
                            || IqActionType.GetContactsForFirstTime.getName().equalsIgnoreCase(action)
                            || IqActionType.GetContactCountToSync.getName().equalsIgnoreCase(action)
                            || IqActionType.GetProfileInfo.getName().equalsIgnoreCase(action)
                            || IqActionType.GetContactsV2.getName().equalsIgnoreCase(action)
                            || IqActionType.GetOrgContacts.getName().equalsIgnoreCase(action)
                            || IqActionType.GetNonOrgContacts.getName().equalsIgnoreCase(action)
                            || IqActionType.GetOrgGroupContacts.getName().equalsIgnoreCase(action)
                            || IqActionType.GetGroupAndNonOrgContacts.getName().equalsIgnoreCase(action)) {
                        ObjectNode node = (ObjectNode) (jsonMessage.get("params"));
                        node.set("timeRanges", Json.parse(node.get("timeRanges").asText()));
                    }
                    if (IqActionType.GetPresenceLocAndUnreadCount.getName().equalsIgnoreCase(action)
                            || IqActionType.GetUnreadCountV2.getName().equalsIgnoreCase(action)) {
                        ObjectNode node = (ObjectNode) (jsonMessage.get("params"));
                        node.set("contacts", Json.parse(node.get("contacts").asText()));
                    }
                    if (IqActionType.UpdateChatStatusReadV2.getName().equalsIgnoreCase(action)) {
                        ObjectNode node = (ObjectNode) (jsonMessage.get("params"));
                        node.set("chat-messages", Json.parse(node.get("chat-messages").asText()));
                    }
                    if (IqActionType.DeleteChatMessage.getName().equalsIgnoreCase(action)) {
                        ObjectNode node = (ObjectNode) (jsonMessage.get("params"));
                        node.set("mids", Json.parse(node.get("mids").asText()));
                        ObjectNode bodyNode = (ObjectNode) (jsonMessage.get("body"));
                        bodyNode.set("mids", Json.parse(bodyNode.get("mids").asText()));
                    }
                    logger.debug("Completed IQ sent message to socket out:" + jsonMessage.toString());
                }
                logger.debug("Just before writing message on socket :" + jsonMessage.toString());
                sendMessageToClient(jsonMessage);
            } else {
                logger.info("unhandled message: " + message.getJsonNode().asText());
            }
        } catch (Exception ex) {
            logger.error("Exception occuered on socket", ex);
            JsonNode errorJson = websocketExceptionHandler.handle(connection.getEx(), message);

            if (errorJson != null) {
                logger.error("exception on socket: " + errorJson);
                sendMessageToClient(errorJson);
            }
        }
    }

    private void handleOpenSocketMessage(UserConnectionActor.Create message) {
    	logger.info("unhandled message: " + message);
    	
        if (connection.getEx() == null) {
            User user = connection.getUserContext().getUser();
            videokycService.onUserConnection(user);
//            userConnectionService.onSocketConnection(user, connection.getConnId());
            JsonNode nd = Json.toJson(connection.getUserContext());
            ObjectNode objectNode = (ObjectNode) nd;
            objectNode.put("type", MessageType.AcceptConnection.getId());
            objectNode.put("RmsServerTime", System.currentTimeMillis());
            sendMessageToClient(objectNode);
            sender().tell(this.websocketFlow, self());
        } else {
            JsonNode errorJson = websocketExceptionHandler.handle(connection.getEx());

            //JsonNode errorJson = httpExceptionHandler.handleWebsocketException(connection.getEx());
            if (errorJson != null) {
                logger.error("exception on socket: " + errorJson);
                sendMessageToClient(errorJson);
                sender().tell(this.websocketFlow, self());
                try {
                    logger.error("wait for 2 secons.");
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    logger.error("threas sleep failed ", e1);
                }
                stopActor();
                getContext().stop(connection.getActorRef());
            }
        }
    }

    private void stopActor() {
        logger.info("stopping actor with uuid : " + connection.getUuid());
        actorMonitor.getActorMap().remove(connection.getUuid());
        logger.info("stopped actor with uuid : " + connection.getUuid());
    }

    private void validateToken() {
        //	if (connection.getEx() == null) {
        try {
            Long timeDiff = System.currentTimeMillis() - lastTokenValidationTime;
            logger.info("time diff: " + timeDiff);
            if (timeDiff > tokenValidationTimeLimit) {
                logger.info("Validate user token started  ");
                userConnectionService.validateUserToken(connection, lastTokenValidationTime);
                lastTokenValidationTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            logger.error("Validate user token Failed. stop the actor", e);
            stopActor();
            //getContext().stop(connection.getActorRef());
        }
	/*	} else {
			JsonNode errorJson = websocketExceptionHandler.handle(connection.getEx());
			//JsonNode errorJson = httpExceptionHandler.handleWebsocketException(connection.getEx());
			if (errorJson != null) {
				logger.error("exception on socket: " + errorJson);
				sendMessageToClient(errorJson);
				sender().tell(this.websocketFlow, self());
				try {
					logger.error("wait for 2 seconds.");
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					logger.error("threas sleep failed ", e1);
				}
				stopActor();
				getContext().stop(connection.getActorRef());
			}
		}*/
    }


    @Override
    public void postStop() {
        if (connection.getUserContext() != null && connection.getUserContext().getUser() != null) {
            stopActor();
            userConnectionService.closeUserConnection(connection);
            logger.info("postStop : " + connection.getUserContext().getUser().getId() + "uuid : " + connection.getUuid());
            User user = connection.getUserContext().getUser();
            userConnectionService.onSocketClosed(user, connection.getConnId());
        } else {
            logger.info("stop non user actor");
        }
        logger.info("connection info removing done");
    }

    private void setGuid(JsonNode json) {
        if (connection != null && connection.getUserContext() != null && connection.getUserContext().getWaguid() != null
                && !"".equalsIgnoreCase(connection.getUserContext().getWaguid())) {
            ((ObjectNode) json).put("waguid", connection.getUserContext().getWaguid());
        }
    }

    private void setGuid(RmsMessage rmsMessage) {
        if (connection != null && connection.getUserContext() != null && connection.getUserContext().getWaguid() != null
                && !"".equalsIgnoreCase(connection.getUserContext().getWaguid())) {
            rmsMessage.setWaguid(connection.getUserContext().getWaguid());
        }
    }
}
