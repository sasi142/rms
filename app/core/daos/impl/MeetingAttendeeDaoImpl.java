package core.daos.impl;

import core.daos.MeetingAttendeeDao;
import core.entities.MeetingAttendee;
import core.entities.Recording;
import core.exceptions.InternalServerErrorException;
import core.utils.Enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Repository
public class MeetingAttendeeDaoImpl extends AbstractJpaDAO<MeetingAttendee> implements MeetingAttendeeDao {
    final Logger logger = LoggerFactory.getLogger(MeetingAttendeeDaoImpl.class);

    @PersistenceContext
    protected EntityManager entityManager;

    public MeetingAttendeeDaoImpl() {
        super();
        setClazz(MeetingAttendee.class);
    }

    @Override
    public void updateMeetingAttendeeStatus(Integer meetingId, Integer userId, Byte attendeeStatus) {
        logger.debug("updateMeetingAttendeeStatus started.meetingId: {} userId: {} attendeeStatus:{}", meetingId, userId, attendeeStatus);
        try {
            Query updateMeetingAttendeeStatus = entityManager.createNamedQuery("MeetingAttendee.updateMeetingAttendeeStatus");
            updateMeetingAttendeeStatus.setParameter("MeetingId", meetingId);
            updateMeetingAttendeeStatus.setParameter("userId", userId);
            updateMeetingAttendeeStatus.setParameter("attendeeStatus", attendeeStatus);
            updateMeetingAttendeeStatus.executeUpdate();
        } catch (Exception e) {
            logger.debug("updateMeetingAttendeeStatus Failed.meetingId: {} userId: {} attendeeStatus:{}", meetingId, userId, attendeeStatus, e);
            throw new InternalServerErrorException(ErrorCode.FAILED_TO_UPDATE_MEETING_ATTENDEE_STATUS,
                    "Failed to update MeetingAttendeeStatus for meetingId:" + meetingId + " userId: " + userId + " attendeeStatus:" + attendeeStatus, e);
        }
        logger.debug("updateMeetingAttendeeStatus ended.meetingId: {} userId: {} attendeeStatus:{}", meetingId, userId, attendeeStatus);
    }

    @Override
    public List<Recording> getMeetingAttendeeByMeetingIdUserId(Integer meetingId, Integer userId) {
        logger.debug("getMeetingAttendeeByMeetingIdUserId Triggered for meetingId: {} userId: {}", meetingId, userId);
        try {
            Query query = entityManager.createNamedQuery("MeetingAttendee.GetMeetingAttendeeByMeetingIdUserId");
            query.setParameter("MeetingId", meetingId);
            query.setParameter("userId", userId);
            return query.getResultList();
        } catch (Exception e) {
            logger.debug("updateMeetingAttendeeStatus Failed.meetingId: {} userId: {}", meetingId, userId, e);
            throw new InternalServerErrorException(ErrorCode.MEETING_ATTENDEE_RETRIEVAL_RETRIEVAL_FAILED,
                    "Unable to Fetch MeetingAttendee for meetingId:" + meetingId + " userId: " + userId, e);
        }
    }
} 