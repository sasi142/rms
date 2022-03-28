package core.daos;

import java.util.List;

public interface JpaDao<T> {
	public void setClazz(final Class<T> clazzToSet);

	public T findOne(final Integer id);

	public T findOne(final Long id);

	public T findOneActiveDeactive(final Integer id);

	public List<T> findAll();

	public void create(final T entity);

	public T update(final T entity);

	public void delete(final T entity);

	public void deleteById(final Integer Id);

	public void deActivate(final T entity);

	public void deActivateById(final Integer Id);

	public void flush();

	public List<T> findAllActiveDeactive();
	
	public List<T> findAllByGroupId(Long groupId);
}
