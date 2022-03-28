package core.entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import core.utils.Enums.ChatMessageType;

@JsonInclude(Include.NON_NULL)
public class MergedGroupChat {

	private Integer			userId;
	private List<GroupChat>	chatHistory;
	private Integer			dateDiff		= 0;
	private Byte			chatMessageType;
    private User senderInfo; 
	
	public MergedGroupChat(Integer userId, List<GroupChat> userChatHistory) {
		this.userId = userId;
		this.chatHistory = userChatHistory;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public List<GroupChat> getChatHistory() {
		return chatHistory;
	}

	public void setChatHistory(List<GroupChat> chatHistory) {
		this.chatHistory = chatHistory;
	}

	public Integer getDateDiff() {
		return dateDiff;
	}

	public void setDateDiff(Integer dateDiff) {
		this.dateDiff = dateDiff;
	}

	public Byte getChatMessageType() {
		return chatMessageType;
	}

	public void setChatMessageType(Byte chatMessageType) {
		this.chatMessageType = chatMessageType;
	}

	public User getSenderInfo() {
		return senderInfo;
	}

	public void setSenderInfo(User senderInfo) {
		this.senderInfo = senderInfo;
	}
}
