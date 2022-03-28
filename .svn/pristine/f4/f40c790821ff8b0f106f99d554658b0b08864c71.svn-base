package core.daos.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import core.daos.ClosedConnectionDao;
import core.entities.ClosedConnection;
import core.utils.CommonUtil;

@Repository
public class ClosedConnectionDaoImpl extends AbstractJpaDAO<ClosedConnection> implements ClosedConnectionDao {
	
	final static Logger logger = LoggerFactory.getLogger(ClosedConnectionDaoImpl.class);

	@Autowired
	private Environment	env;

	public ClosedConnectionDaoImpl() {
		super();
		setClazz(ClosedConnection.class);
	}

	@Override
	public List<ClosedConnection> getExpiredConnections(Long time) {
		logger.info("get user connection info, input time " + time);
		List<ClosedConnection> userList = new ArrayList<>();
		try {
			TypedQuery<ClosedConnection> query = entityManager.createNamedQuery("ClosedConnection.getExpiredConnections", ClosedConnection.class);
			query.setParameter("time", time);
			userList = query.getResultList();
			logger.info("num users found " + (userList == null ? null : userList.size()));
		} catch (NonUniqueResultException | NoResultException ex) {
			logger.error("unread messages users not found ", ex);
		} catch (Exception e) {
			logger.error("unhandelled exception ", e);
		}
		return userList;
	}

	@Override
	public void usert(ClosedConnection connection) {
		logger.debug("add or update closed connection info");
		String queryStr = "INSERT INTO closed_connection (UserId, ClientId, LastUpdatedDate,retry) VALUES (:userId,:clientId,:lastUpdatedTime,:retry) ON DUPLICATE KEY UPDATE"
				+ " LastUpdatedDate=:lastUpdatedTime,active=true,retry=:retry";
		Query query = entityManager.createNativeQuery(queryStr);
		query.setParameter("userId", connection.getUserId());
		query.setParameter("clientId", connection.getClientId());
		query.setParameter("lastUpdatedTime", connection.getLastUpdatedDate());
		query.setParameter("retry", connection.getRetry());
		query.executeUpdate();
		logger.debug("added or updated closed connection info");
	}

	/*
	 * (non-Javadoc)
	 * @see core.daos.ClosedConnectionDao#getByUserIds(java.util.List, java.lang.Long)
	 */
	@Override
	public Map getByUserIds(List userIds, Long time) {
		logger.debug("get user connection info for users " + (userIds == null ? null : userIds.toString()));
		String userStr = CommonUtil.convertListToString(userIds);
		Map<Integer, ClosedConnection> connectionMap = new HashMap<Integer, ClosedConnection>();
		try {
			String msg = "SELECT cc.UserId, cc.ClientId FROM Closed_Connection cc WHERE cc.UserId IN (" + userStr
					+ ") AND cc.LastUpdatedDate >= :time AND cc.active=true";
			Query query = entityManager.createNativeQuery(msg);
			query.setParameter("time", time);
			List<Object[]> users = query.getResultList();
			if (users != null) {
				for (Object[] row : users) {
					ClosedConnection con = new ClosedConnection();
					if (row[0] != null) {
						con.setUserId(Integer.valueOf(row[0].toString()));
					}
					if (row[1] != null) {
						con.setClientId(row[1].toString());
					}
					connectionMap.put(con.getUserId(), con);
				}
			}
			logger.debug("num users found " + connectionMap.size());
		} catch (NonUniqueResultException | NoResultException ex) {
			logger.info("unread messages users not found ");
		}
		return connectionMap;

	}
}