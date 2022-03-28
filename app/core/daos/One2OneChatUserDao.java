package core.daos;

import java.util.List;
import core.entities.One2OneChatUser;

public interface One2OneChatUserDao extends JpaDao<One2OneChatUser> {
	public List<Integer> getOne2OneChatUserCount(Integer to);

	public void UpdateOne2OneChatUserReadStatus(Integer from, Integer to);
}
