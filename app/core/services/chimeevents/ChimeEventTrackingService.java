package core.services.chimeevents;

import core.entities.ChimeMeetingEvent;

import java.util.List;

public interface ChimeEventTrackingService {
    void saveChimeMeetingEvent(ChimeMeetingEvent chimeMeetingEvent);
    List<ChimeMeetingEvent> getChimeMeetingEvents(Integer meetingId);
}
