package core.services.chimeevents;

import core.entities.ChimeMeetingEvent;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChimeEventTrackingService {
    void saveChimeMeetingEvent(ChimeMeetingEvent chimeMeetingEvent);
    List<ChimeMeetingEvent> getChimeMeetingEvents(Integer meetingId);
}
