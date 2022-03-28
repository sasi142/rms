package core.daos;

import core.entities.PubSubMessage;

public interface PubSubDao {
	public void publishConnectionInfo(PubSubMessage message);

	public void subscribe();

	public void unsubscribe();
}
