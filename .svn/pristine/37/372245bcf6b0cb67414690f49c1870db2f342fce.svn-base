package core.entities;

import java.util.HashMap;
import java.util.Map;

public class ActorExecutionContext {
	private Map<Integer, User> userMap = new HashMap<>(); // storing this to avoid frequent cache hit
	private Group group;
	public ActorExecutionContext(User user) {
		setUserMap(user);
	}
	public ActorExecutionContext(Group group) {
		this.group = group;
	}
	public User getUserFromMap(Integer id) {
		User user = null;
		if (userMap != null) {
			user = userMap.get(id);
		}
		return user;
	}
	public void setUserMap(User user) {
		if (this.userMap == null) {
			this.userMap = new HashMap<>();
		}
		this.userMap.put(user.getId(), user);
	}
	public void resetMap() {
		this.userMap = null;
	}		
	public Group getGroup() {
		return group;
	}
	public void setGroup(Group group) {
		this.group = group;
	}
}
