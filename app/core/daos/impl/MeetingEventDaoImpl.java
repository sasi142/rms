package core.daos.impl;

import core.exceptions.InternalServerErrorException;
import core.exceptions.ResourceNotFoundException;
import core.utils.Enums;
import org.springframework.stereotype.Repository;

import core.daos.MeetingEventDao;
import core.entities.MeetingEvent;
import org.springframework.util.CollectionUtils;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;


@Repository
public class MeetingEventDaoImpl extends AbstractJpaDAO<MeetingEvent> implements MeetingEventDao {

	public MeetingEventDaoImpl() {
		super();
		setClazz(MeetingEvent.class);
	}

}
