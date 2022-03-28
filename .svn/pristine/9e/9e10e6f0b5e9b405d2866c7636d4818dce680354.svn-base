package core.entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class MergedOne2OneChat {

	private Integer userId;
	private List<One2OneChat> chatHistory;
	private Integer dateDiff;
	
	public MergedOne2OneChat(Integer userId, List<One2OneChat> userChatHistory) {
		this.userId = userId;
		this.chatHistory = userChatHistory;
	}
	
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	public List<One2OneChat> getChatHistory() {
		return chatHistory;
	}
	public void setChatHistory(List<One2OneChat> chatHistory) {
		this.chatHistory = chatHistory;
	}
	public Integer getDateDiff() {
		return dateDiff;
	}
	public void setDateDiff(Integer dateDiff) {
		this.dateDiff = dateDiff;
	}
}