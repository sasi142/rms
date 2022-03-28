package core.services;

import core.entities.Event;

public interface EventService {
	public void handleEvent(Event event);

	void agentAssignedToVideoKyc(Event event);
}
