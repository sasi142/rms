package core.services;

import java.util.List;

import core.entities.Memo;
import core.entities.MemoChatUser;
import core.entities.MemoRecipient;

public interface MemoService {

	public Memo getMemoDetails(Integer memoId, Boolean needSummary);

	public Memo createChannelMessage(Memo inMemo, Integer channelId);

	public Memo createMemo(Memo inMemo);

	public List<Memo> getMemosByOrgId(Integer offset, Integer limit);

	public List<Memo> getMemosByUserId(Integer userId, Integer offset, Integer limit);

	public void changeReadStatus(Integer memoId, Integer userId, Boolean readStatus);	

	public Long getMemoCountByStatus(Integer userId, Boolean readStatus);

	public void updateMemoPublicState(Integer memoId, Boolean isPublic);

	public Memo getMemoByPublicURL(String url);

	public List<Memo> getMemosByUserIdV2(Integer userId, Integer channelId, Integer offset, Integer limit);

	public Memo getMemoDetailsV2(Integer memoId, Boolean needSummary);

	public List<MemoChatUser> getMemoChatUsers(Integer memoId, Integer offset, Integer limit);

	public Memo getMessagePublicPage(String url);
	
	public Integer isReceipient(Integer memoId, Integer userId) ;

	Integer createMemoFileDetailsInDump(String filePath, String fileName, Byte uploadType);
	
	public void updateUserMemoDetailsInDump(Memo inMemo);
	
	void processBulkMemo();

	String getCustomMemoReportById(Integer memoId);
	
	String getRegularMemoReportById(Integer memoId);

}
