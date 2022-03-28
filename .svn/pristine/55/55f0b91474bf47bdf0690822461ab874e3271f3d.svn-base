package core.services;

import java.util.ArrayList;
import java.util.List;

import messages.UserConnection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;

import core.entities.Memo;
import core.entities.User;
import core.entities.UserContext;
import core.utils.ThreadContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jpa-config.xml", "classpath:application-context.xml" })
public class MemoServiceImplTest {
	private UserConnection	conn	= null;

	@Autowired
	private MemoService		memoService;

	@Before
	public void setup() {
		User user = new User();
		user.setId(26);
		UserContext userContext = new UserContext("101", user);
		conn = new UserConnection(userContext, "123");
		ThreadContext.setUsercontext(userContext);
	}

	@Test
	public void testCreateMemo() {
		Memo inMemo = new Memo();
		inMemo.setMessage("<html><body><heading>This is memo text</heading></body></html>");
		inMemo.setSubject("MemoSubject");

		List<Long> recipientIds = new ArrayList<Long>();
		recipientIds.add(26L);
		inMemo.setRecipientIds(recipientIds);

		inMemo.setSendToAll(false);

		inMemo.setAttachmentIds(null);
		inMemo.setCityIds(null);
		inMemo.setOfficeIds(null);
		inMemo.setAdGroupIds(null);

		List<Integer> deptList = new ArrayList<Integer>();
		inMemo.setDepartmentIds(deptList);

		JsonNode s1 = Json.toJson(deptList);
		System.out.println(s1.toString());
		JsonNode s2 = Json.toJson(recipientIds);
		System.out.println(s2.toString());
		inMemo.setDesignations(null);

		Memo outMemo = memoService.createMemo(inMemo);
		Assert.isNull(outMemo);
		Assert.isNull(outMemo.getId());
	}
}
