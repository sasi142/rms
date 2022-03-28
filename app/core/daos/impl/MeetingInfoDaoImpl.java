package core.daos.impl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import core.daos.MeetingInfoDao;
import core.entities.Meeting;
import core.exceptions.InternalServerErrorException;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.VideoCallStatus;
import javax.persistence.*;
@Repository
public class MeetingInfoDaoImpl extends AbstractJpaDAO<Meeting> implements MeetingInfoDao{
	final Logger logger = LoggerFactory.getLogger(MeetingInfoDaoImpl.class);

	@PersistenceContext
	protected  EntityManager entityManager;

	public MeetingInfoDaoImpl() {
		super();
		setClazz(Meeting.class);
	}

	@Transactional(rollbackFor = { Exception.class })
	@Override
	public void updateMeetingStatus(Integer meetingId, Byte meetingStatus, Integer updatedById) {
		logger.debug("updateMeetingStatus start");
		try {
			Query updateStatusQuery = entityManager.createNamedQuery("updateMeetingStatus");
			updateStatusQuery.setParameter("meetingStatus", meetingStatus);
			
			Long startDate = 0L, endDate = 0L;
			
			if(VideoCallStatus.Connected.getId().byteValue() == meetingStatus.byteValue()) {
				startDate = System.currentTimeMillis();
			}
			else if(VideoCallStatus.Ended.getId().byteValue() == meetingStatus.byteValue()) {
				endDate = System.currentTimeMillis();
			}
			
			updateStatusQuery.setParameter("startDate", startDate);
			updateStatusQuery.setParameter("endDate", endDate);
			updateStatusQuery.setParameter("updatedById", updatedById);
			updateStatusQuery.setParameter("updatedDate", System.currentTimeMillis());
			updateStatusQuery.setParameter("meetingId", meetingId);
			updateStatusQuery.executeUpdate();

		}catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to update MeetingStatus to "+ meetingStatus, e);
		}
		logger.debug("updateMeetingStatus end");
	}


	@Override
	public void updateMeetingRating(Integer meetingId, Byte meetingRating) {
		logger.debug("updateMeetingRating start");
		try {
			Query updateMeetingRating = entityManager.createNamedQuery("updateMeetingRating");
			updateMeetingRating.setParameter("meetingId", meetingId);
			updateMeetingRating.setParameter("meetingRating", meetingRating);
			updateMeetingRating.executeUpdate();

		}catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to update MeetingRating "+ e);
		}
		logger.debug("updateMeetingRating end");
	}

	@Transactional(rollbackFor = { Exception.class })
	@Override
	public void updateChimeMeetingId(Integer meetingId, String chimeMeetingId, Integer updatedById) {
		logger.debug("updateChimeMeetingId started for meetingId: {} chimeMeetingId: {}",meetingId,chimeMeetingId);
		try {
			Query updateChimeMeetingIdQuery = entityManager.createNamedQuery("updateChimeMeetingId");
			updateChimeMeetingIdQuery.setParameter("meetingId", meetingId);
			updateChimeMeetingIdQuery.setParameter("chimeMeetingId", chimeMeetingId);
			updateChimeMeetingIdQuery.setParameter("updatedById", updatedById);
			updateChimeMeetingIdQuery.setParameter("updatedDate", System.currentTimeMillis());
			updateChimeMeetingIdQuery.executeUpdate();
		}catch (Exception e) {
				throw new InternalServerErrorException(ErrorCode.UPDATE_CHIME_MEETING_ID_ERROR,
					"Failed to update updateChimeMeetingId "+ e);
		}
		logger.debug("updateChimeMeetingId Completed for meetingId: {} chimeMeetingId: {}",meetingId,chimeMeetingId);
	}

	@Override
	public Integer getExistingMeetingId(Long groupId) {
		logger.info("getExistingMeetingId start: {}",groupId);
		try {
			Query query = entityManager.createNamedQuery("getMeetingDetails", Integer.class);
			query.setParameter("groupId", groupId);
			query.setMaxResults(1);
			Integer meetingId = (Integer)query.getSingleResult();
			return meetingId;
		}  catch (NonUniqueResultException | NoResultException ex) {
            return null;
	} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to get meeting "+ e);
		}
		
	}


	
	@Transactional(rollbackFor = { Exception.class })
	@Override
	public void updateMeetingBandwidthInfo(Integer meetingId, Short callerMaxBandwidth, Short callerMinBandwidth, Short receiverMaxBandwidth, Short receiverMinBandwidth) {
		logger.debug("update Meeting BandwidthInfo start");
		try {
			Query updateMeetingRating = entityManager.createNamedQuery("updateMeetingBandwidthInfo");
			updateMeetingRating.setParameter("meetingId", meetingId);
			updateMeetingRating.setParameter("callerMinBandwidth", callerMinBandwidth);
			updateMeetingRating.setParameter("callerMaxBandwidth", callerMaxBandwidth);
			updateMeetingRating.setParameter("receiverMinBandwidth", receiverMinBandwidth);
			updateMeetingRating.setParameter("receiverMaxBandwidth", receiverMaxBandwidth);
			updateMeetingRating.executeUpdate();
		}catch (Exception e) {
			logger.error("error updating MeetingBandwidthInfo in DB");
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to update MeetingBandwidthInfo in DB",e);
		}
		logger.debug("update Meeting BandwidthInfo end");
	}
	
} 