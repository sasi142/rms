package core.daos.impl;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import core.daos.JpaDao;
import core.entities.BaseEntity;
import core.exceptions.BadRequestException;
import core.exceptions.ResourceNotFoundException;
import core.utils.Enums.ErrorCode;

public abstract class AbstractJpaDAO<T extends BaseEntity> implements JpaDao<T> {
	final static Logger logger = LoggerFactory.getLogger(AbstractJpaDAO.class);
	private Class<T> clazz;
	@PersistenceContext
	protected EntityManager entityManager;

	@Override
	public void create(final T entity) {
		logger.debug("create entity");
		this.entityManager.persist(entity);
		logger.debug("created entity in db");
	}

	@Override
	public void deActivate(final T entity) {
		logger.debug("deactivate entity by setting active to false");
		entity.setActive(false);
		this.update(entity);
		logger.debug("deactivated entity");
	}

	@Override
	public void deActivateById(final Integer id) {
		logger.debug("deactivate entity by Id " + id);
		final T entity = this.findOne(id);
		if (entity == null) {
			logger.error("entity not found");
			throw new ResourceNotFoundException(ErrorCode.Entity_Not_Found,
					"Entity with id " + id + " does not exists.");
		}

		if (entity.getActive()) {
			logger.debug("entity is active. deactivate it");
			this.deActivate(entity);
		} else {
			logger.debug("entity is already deactivated");
			throw new BadRequestException(ErrorCode.Entity_Already_Deleted,
					"Entity with id " + id + " is already deleted.");
		}
	}

	@Override
	public void delete(final T entity) {
		logger.debug("delete the entity");
		this.entityManager.remove(entity);
		logger.debug("deleted entity");
	}

	@Override
	public void deleteById(final Integer entityId) {
		logger.debug("delete entity by id " + entityId);
		final T entity = this.findOne(entityId);
		this.delete(entity);
		logger.debug("deleted entity");
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<T> findAll() {
		logger.debug("find all the entities");
		return this.entityManager.createQuery("from " + this.clazz.getName() + " where active=true").getResultList();
	}
	
	@Override
	public List<T> findAllByGroupId(Long groupId){
		logger.debug("find All By GroupId ");
		return this.entityManager.createQuery("from " + this.clazz.getName() + " where GroupId = "+groupId+" and active=true").getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<T> findAllActiveDeactive() {
		return this.entityManager.createQuery("from " + this.clazz.getName()).getResultList();
	}

	@Override
	public T findOne(final Integer id) {
		logger.debug("find active entity by id " + id);
		T entity = null;
		final T entity1 = this.entityManager.find(this.clazz, id);
		if (entity1 != null && entity1.getActive()) {
			logger.debug("active entity found");
			entity = entity1;
		} else {
			logger.debug("active entity not found");
			throw new ResourceNotFoundException(ErrorCode.Entity_Not_Found,
					"Entity with id " + id + " does not exists.");
		}
		return entity;
	}

	@Override
	public T findOne(final Long id) {
		logger.debug("find active entity by id " + id);
		T entity = null;
		final T entity1 = this.entityManager.find(this.clazz, id);
		if (entity1 != null && entity1.getActive()) {
			logger.debug("active entity found");
			entity = entity1;
		} else {
			logger.debug("active entity not found");
			throw new ResourceNotFoundException(ErrorCode.Entity_Not_Found,
					"Entity with id " + id + " does not exists.");
		}
		return entity;
	}

	@Override
	public T findOneActiveDeactive(final Integer id) {
		T entity = null;
		final T entity1 = this.entityManager.find(this.clazz, id);
		if (entity1 != null) {
			entity = entity1;
		} else {
			throw new ResourceNotFoundException(ErrorCode.Entity_Not_Found,
					"Entity with id " + id + " does not exists.");
		}
		return entity;
	}

	@Override
	public void flush() {
		this.entityManager.flush();
	}

	@Override
	public final void setClazz(final Class<T> clazzToSet) {
		this.clazz = clazzToSet;
	}

	@Override
	public T update(final T entity) {
		logger.debug("update entity");
		T t = this.entityManager.merge(entity);
		logger.debug("updated the entity");
		return t;
	}

}