package core.daos;

import java.util.List;

import core.entities.MemoRecipient;

public interface MemoRecipientDao extends JpaDao<MemoRecipient> {

	void changeReadStatus(Integer memoId, Integer userId, Boolean readStatus);

	Integer isReceipient(Integer memoId, Integer userId);

	Long getMemoCountByStatus(Integer userId, Boolean readStatus);
}
