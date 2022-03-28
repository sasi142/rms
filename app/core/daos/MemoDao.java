package core.daos;

import java.util.List;

import core.entities.Memo;
import core.entities.MemoChatUser;

public interface MemoDao extends JpaDao<Memo> {

	Memo createMemo(Memo inMemo);

	List<Memo> getMemosByOrgId(Integer userId, Integer organizationId, Integer offset, Integer limit);

	List<Memo> getMemosByUserId(Integer userId, Integer offset, Integer limit);

	Memo getMemoDetails(Integer memoId, Boolean needSummary, Integer loggedInUserId);

	void updateMemoPublicState(Integer memoId, Boolean isPublic);

	Memo getMemoByPublicURL(String url);

	void updateMemoPageViewCount(Integer memoId);

	List<Memo> getMemosByUserIdV2(Integer id, Integer channelId, Integer offset, Integer limit);

	Memo getMemoDetailsV2(Integer memoId, Boolean needSummary, Integer currentUserId);

	List<MemoChatUser> getMemoChatUsers(Integer memoId, Integer offset, Integer limit);

	Memo getMessagePublicPage(String url);
	
	Memo getMemoDetailsById(Integer memoId, Boolean needSummary, Integer loggedUserId);

	String getMemoReportById(Integer memoId);
	
	String getRegularMemoReportById(Integer memoId);

}
