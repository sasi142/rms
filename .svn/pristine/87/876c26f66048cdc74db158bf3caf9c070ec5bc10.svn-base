package core.daos.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.daos.One2OneChatUserDao;
import core.entities.One2OneChatUser;

@Repository
public class One2OneChatUserDaoImpl extends AbstractJpaDAO<One2OneChatUser> implements One2OneChatUserDao {

	final static Logger logger = LoggerFactory.getLogger(One2OneChatUserDaoImpl.class);

	public One2OneChatUserDaoImpl() {
		super();
		setClazz(One2OneChatUser.class);
	}

	@Override
	public List<Integer> getOne2OneChatUserCount(Integer to) {
		logger.debug("get unread messages user list for "+ to);
		List<Integer> userList = new ArrayList<>();
		try {
			TypedQuery<Integer> query = entityManager.createNamedQuery("One2OneChatUser.GetUnReadChatCount", Integer.class);
			query.setParameter("to", to);
			userList = query.getResultList();
			logger.debug("num of users with unread messages " + userList.size());
		} catch (NonUniqueResultException | NoResultException ex) {
			logger.info("no user found");
		}
		return userList;
	}

	@Override
	public void UpdateOne2OneChatUserReadStatus(Integer from, Integer to) {
		logger.debug("update read status for user from "+from + "&to: " + to);
		try {
			Query query = entityManager.createNamedQuery("One2OneChatUser.UpdateUnReadChat");
			query.setParameter("from", from);
			query.setParameter("to", to);
			query.executeUpdate();
		} catch (NonUniqueResultException | NoResultException ex) {
			logger.error("entity not found with given user id");
		}
		logger.debug("updated read status for user from "+from + "&to: " + to);
	}
}
