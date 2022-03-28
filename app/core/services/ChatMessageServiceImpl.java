package core.services;import org.slf4j.Logger;import org.slf4j.LoggerFactory;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.beans.factory.annotation.Qualifier;import org.springframework.stereotype.Service;import core.daos.CacheConnectionInfoDao;import core.entities.ChatMessage;import core.exceptions.BadRequestException;import core.exceptions.ForbiddenException;import core.utils.ThreadContext;import core.utils.Enums.ACKType;import core.utils.Enums.ChatType;import core.utils.Enums.ErrorCode;import core.utils.Enums.MessageType;import core.validator.Validator;import messages.UserConnection;@Servicepublic class ChatMessageServiceImpl implements ChatMessageService {	final static Logger logger = LoggerFactory.getLogger(ChatMessageServiceImpl.class);	@Autowired	@Qualifier("CacheImpl")	private CacheConnectionInfoDao cacheConnectionInfoDao;	@Autowired	private Validator validator;	@Autowired	@Qualifier("RmsCacheService")	private CacheService cacheService;	@Autowired	private ChatHistoryService chatHistoryService;	@Autowired	private One2OneChatMessageService one2OneChatMessageService;	@Autowired	private GroupChatMessageService groupChatMessageService;	public ChatMessageServiceImpl() {	}	@Override	public void sendMessage(UserConnection connection, ChatMessage message) {		try {			ThreadContext.set(connection);			validator.validate(connection, message);			logger.info("In sendMessage, ChatMessage is valid. type = " + message.getType() + ", Subtype = "					+ message.getSubtype());			if ((message.getType() == MessageType.ACK.getId())) {				if (message.getSubtype().byteValue() == ACKType.One2OneChat.getId().byteValue()) {					chatHistoryService.updateChatReadStatus(message.getTo(), ChatType.One2One, MessageType.ACK,							connection.getUserContext());				} else if (message.getSubtype().byteValue() == ACKType.GroupChat.getId().byteValue()) {					chatHistoryService.updateChatReadStatus(message.getFrom(), ChatType.GroupChat, MessageType.ACK,							connection.getUserContext());				}				logger.debug("Completed UpdateChatReadStatus, ACK/Chat Message :" + message.getText());			} else if (message.getSubtype().byteValue() == ChatType.One2One.getId().byteValue()					|| message.getSubtype().byteValue() == ChatType.One2OneVideoChat.getId().byteValue()) {				one2OneChatMessageService.sendMessage(connection, message);			} else if (message.getSubtype().byteValue() == ChatType.GroupChat.getId().byteValue()) {				groupChatMessageService.sendMessage(connection, message);			} else {				throw new BadRequestException(ErrorCode.UnsupportedTypeSubtype,						ErrorCode.UnsupportedTypeSubtype.getName());			}			logger.debug("sendMessage completed successfully");		} catch(ForbiddenException e){			throw new ForbiddenException(ErrorCode.Send_Message_Not_Allowed, message.getFrom(), message.getTo());		} catch (Exception ex) {			throw ex;		}	}}