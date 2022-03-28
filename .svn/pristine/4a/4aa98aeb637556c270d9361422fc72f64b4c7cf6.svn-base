package core.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import play.Logger;
import redis.clients.jedis.Jedis;

import com.github.jedis.lock.JedisLock;

import core.exceptions.InternalServerErrorException;
import core.redis.RedisConnection;
import core.utils.Enums.DatabaseType;
import core.utils.Enums.ErrorCode;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jpa-config.xml", "classpath:application-context.xml" })
public class PresenceServiceImplTest {

	@Autowired
	private PresenceService	presenceService;

	@Autowired
	public RedisConnection	redisConnection;

	@Autowired
	public Environment		env;

	private String			openChatWindowBucketName	= "ims-user-chat-window-store";

	@Test
	public void testSendPresenceInfoToContact() {
		presenceService.sendPresenceInfoToContact(1);
	}

	// @Test
	public void testGetPresence() {
		presenceService.getPresence(26, true);
	}

	// @Test
	public void createOpenWindowCache() {
		createOpenWindowCache(20000, 5);
	}

	private void createOpenWindowCache(Integer userCount, Integer windowCount) {
		Random rand = new Random();

		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Ims);
		for (Integer userId = 1; userId <= userCount; userId++) {
			JedisLock lock = getLock(userId, jedis);
			try {
				lock.acquire();
				Logger.underlying().debug("Got JEDIS Lock for user " + userId);

				Integer window = 0;
				List<Integer> openWindows = new ArrayList<Integer>();
				do {
					++window;
					int randomWindow = rand.nextInt(userCount);
					if (randomWindow != 0 && randomWindow != userId.intValue() && !openWindows.contains(randomWindow)) {
						openWindows.add(randomWindow);
					} else {
						--window;
					}
				} while (window < windowCount);

				String value = StringUtils.join(openWindows);
				jedis.hset(openChatWindowBucketName, String.valueOf(userId), value);
				Logger.underlying().info("Created ConnectionInfo in Cache for user " + userId);
			} catch (Exception ex) {
				throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
			} finally {
				lock.release();
			}
		}
		redisConnection.releaseMasterConnection(jedis, DatabaseType.Rms);
	}

	public static void main(String[] args) {
		List<Integer> contactIds = new ArrayList<>();
		String strContactIds = "[16656, 978, 19413, 13226, 7976]";
		strContactIds = strContactIds.replace("[", "");
		strContactIds = strContactIds.replace("]", "");
		List<String> strArrayContacts = Arrays.asList(strContactIds.split("\\s*,\\s*"));
		System.out.println("strArrayContacts.toString() = " + strArrayContacts.toString());
		for (String strContact : strArrayContacts) {
			System.out.println(strContact);
			contactIds.add(Integer.valueOf(strContact));
		}
		System.out.println("contactIds.toString() = " + contactIds.toString());
	}

	// LOCK DETAILS : https://github.com/abelaska/jedis-lock/blob/master/src/main/java/com/github/jedis/lock/JedisLock.java
	private synchronized JedisLock getLock(Integer userId, Jedis jedis) {
		JedisLock lock = new JedisLock(jedis, String.valueOf(userId), 10000, 30000);
		return lock;
	}

}
