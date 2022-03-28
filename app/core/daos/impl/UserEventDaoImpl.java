package core.daos.impl;

import org.springframework.stereotype.Repository;

import core.daos.UserEventDao;
import core.entities.UserEvent;

@Repository
public class UserEventDaoImpl extends AbstractJpaDAO<UserEvent> implements UserEventDao {
	public UserEventDaoImpl() {
		super();
		setClazz(UserEvent.class);
	}
}
