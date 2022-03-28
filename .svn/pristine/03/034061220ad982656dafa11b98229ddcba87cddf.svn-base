package core.utils;

import core.entities.ActorExecutionContext;

public class ActorThreadContext {
	private static final ThreadLocal<ActorExecutionContext> actorThreadLocal = new ThreadLocal<ActorExecutionContext>();

	public static void set(ActorExecutionContext actorExecutionContext) {
		actorThreadLocal.set(actorExecutionContext);
	}

	public static void unset() {
		actorThreadLocal.remove();

	}

	public static ActorExecutionContext get() {
		return actorThreadLocal.get();
	}
}
