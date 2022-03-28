package core.services;

import messages.UserConnection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import core.entities.IqMessage;
import core.entities.User;
import core.entities.UserContext;
import core.utils.Enums;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jpa-config.xml", "classpath:application-context.xml" })
public class ChatHistoryServiceImplTest {

	@Autowired
	private ChatHistoryService		chatHistoryService;

	@Autowired
	private IqMessageServiceImpl	iqMsgService;

	@Test
	public void testCreateOne2OneChatHistory() {
		// ChatMessage message = new ChatMessage("running from test case", 1512047720110L, null, 26, 4458, 0, null, null, null, null);
		// One2OneChat result = chatHistoryService.createOne2OneChatHistory(message);
		// Assert.notNull(result);
		// Assert.notNull(result.getId());

	}

	private UserConnection	conn	= null;

	@Before
	public void setup() {
		User user = new User();
		user.setId(26);
		UserContext userContext = new UserContext("101", user);
		conn = new UserConnection(userContext, "123");
		// ThreadContext.setUsercontext(userContext);
	}

	// @Test
	public void testgetGroupChatMsgReadInfo() {
		Long msgId = 10L;
		IqMessage iqMsg = new IqMessage();
		iqMsg.setType(4);
		iqMsg.setSubtype(0);
		iqMsg.setMid(msgId);
		iqMsg.setAction(Enums.IqActionType.MsgReadInfo.name());

		iqMsgService.handleIqRequest(conn, iqMsg);

		// List<MessageReadInfo> msgReadInfoList = chatHistoryService.getGroupChatMsgReadInfo(msgId);
		// System.out.println(msgReadInfoList);
		// Logger.underlying().debug("got GroupChat Msg ReadInfo for message : " + msgId + " as " + (msgReadInfoList == null ? null : msgReadInfoList.size()));
		// ObjectMapper mapper = new ObjectMapper();
		// ObjectNode node = mapper.createObjectNode();
		// node.put("RmsServerTime", System.currentTimeMillis());
		// String msgReadInfoJson = null;
		// try {
		// msgReadInfoJson = mapper.writeValueAsString(msgReadInfoList);
		// } catch (JsonProcessingException e) {
		// msgReadInfoJson = "Failed to create JSON";
		// }
		// node.put("readInfo", msgReadInfoJson);
		// iqMessage.setBody(node);
		// iqMessage.setSubtype(IqType.Response.getId());
		// JsonNode iqJson = Json.toJson(iqMessage);
		//
		// // send message to only requester
		// Logger.underlying().info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
		// RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
	}

}
