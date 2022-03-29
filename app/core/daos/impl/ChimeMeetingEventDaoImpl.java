package core.daos.impl;

import core.daos.ChimeMeetingEventDao;
import core.entities.ChimeMeetingEvent;
import core.entities.MeetingEvent;
import core.exceptions.InternalServerErrorException;
import core.exceptions.ResourceNotFoundException;
import core.utils.Enums;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;


@Repository
public class ChimeMeetingEventDaoImpl extends AbstractJpaDAO<ChimeMeetingEvent> implements ChimeMeetingEventDao {

	public ChimeMeetingEventDaoImpl() {
		super();
		setClazz(ChimeMeetingEvent.class);
	}

	@Override
	public List<ChimeMeetingEvent> getChimeMeetingEventsByMeetingId(Integer meetingId) {
		logger.info("getChimeMeetingEventsByMeetingId triggerd for meetingId: "+meetingId);
		MeetingEvent meetingEvent = null;
		List<ChimeMeetingEvent> results = new ArrayList<>();
		try {
			Query query = entityManager.createNamedQuery("ChimeMeetingEvent.getChimeMeetingEvents");
			query.setParameter("MeetingId", meetingId);
		//  Pagination will be used later
		//	query.setFirstResult(offset);
		//	query.setMaxResults(limit);
			results = query.getResultList();
			if (CollectionUtils.isEmpty(results)) {
				logger.debug("Chime Meeting Events Table is Empty");
				throw new ResourceNotFoundException(Enums.ErrorCode.Entity_Not_Found, "No Chime Meeting Events Found.");
			}
		} catch (NonUniqueResultException | NoResultException ex) {
			logger.error("Exception during fetching Chime Meeting Events for meetingId: "+meetingId);
			throw new InternalServerErrorException(Enums.ErrorCode.Entity_Not_Found,"Exception during Chime Meeting Events Retrieval.");
		}
		logger.info("getChimeMeetingEventsByMeetingId completed for meetingId:{}. No. of chime-meeting-events found: {}",meetingId, results.size());
		return results;
	}

}
