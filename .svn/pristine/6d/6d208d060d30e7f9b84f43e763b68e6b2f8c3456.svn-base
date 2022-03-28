package core.utils;

import messages.UserConnection;
import core.entities.UserContext;

public class ThreadContext {

	private static final ThreadLocal<UserConnection> userThreadLocal = new ThreadLocal<UserConnection>();

	public static void set(UserConnection connection) {
		userThreadLocal.set(connection);
	}

	public static void unset() {
		userThreadLocal.remove();

	}

	public static UserConnection get() {
		return userThreadLocal.get();
	}

	public static void setUsercontext(UserContext userContext) {
		UserConnection connection = new UserConnection(userContext, null);
		userThreadLocal.set(connection);
	}

	public static UserContext getUserContext() {
		if (userThreadLocal.get() != null) {
			return userThreadLocal.get().getUserContext();
		} else {
			return null;
		}
	}
}
