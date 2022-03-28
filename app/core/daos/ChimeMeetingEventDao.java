package core.daos;

import core.entities.ChimeMeetingEvent;

import java.util.List;

public interface ChimeMeetingEventDao  extends JpaDao<ChimeMeetingEvent> {
    List<ChimeMeetingEvent> getChimeMeetingEventsByMeetingId(Integer meetingId);
}
