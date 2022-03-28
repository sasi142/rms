package core.services.chimeevents;

import core.daos.ChimeMeetingEventDao;
import core.entities.ChimeMeetingEvent;
import core.exceptions.InternalServerErrorException;
import core.utils.Enums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChimeEventTrackingServiceImpl implements ChimeEventTrackingService {
    final Logger logger = LoggerFactory.getLogger(ChimeEventTrackingServiceImpl.class);

    @Autowired
    private ChimeMeetingEventDao chimeMeetingEventDao;

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void saveChimeMeetingEvent(ChimeMeetingEvent chimeMeetingEvent) {
        logger.debug("Saving Chime Meeting event Started. Event Source: {} Event Type: {} Event Id: {}", chimeMeetingEvent.getEventSource(), chimeMeetingEvent.getEventType(), chimeMeetingEvent.getEventId());
        try {
            chimeMeetingEventDao.create(chimeMeetingEvent);
        } catch (Exception ex) {
            logger.error("failed to save to Chime Meeting Event. Event Source: {} Event Type: {} Event Id: {} ",chimeMeetingEvent.getEventSource(), chimeMeetingEvent.getEventType(), chimeMeetingEvent.getEventId(), ex);
            throw new InternalServerErrorException(Enums.ErrorCode.FAILED_TO_SAVE_CHIME_MEETING_EVENT, Enums.ErrorCode.FAILED_TO_SAVE_CHIME_MEETING_EVENT.getName(), ex);
        }
        logger.debug("Saving Chime Meeting event Completed. Event Source: {} Event Type: {} Event Id: {}" + chimeMeetingEvent.getEventSource(), chimeMeetingEvent.getEventType(), chimeMeetingEvent.getEventId());
    }

    @Override
    public List<ChimeMeetingEvent> getChimeMeetingEvents(Integer meetingId) {
        logger.debug("GetEvents API Triggered for  meetingId: " + meetingId);
        return chimeMeetingEventDao.getChimeMeetingEventsByMeetingId(meetingId);
    }
}
