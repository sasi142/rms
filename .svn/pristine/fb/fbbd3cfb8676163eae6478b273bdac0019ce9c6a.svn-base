/**
 * @author Chandramohan.Murkute
 */
package controllers.dto;

import core.entities.MemoSummary;

public class MemoSummaryDto {

	private Float	unreadPercentage;
	private Integer	unreadCount;
	private Integer	readCount;
	private Integer	sentToCount;

	public MemoSummaryDto() {
	}

	public MemoSummaryDto(Float unreadPercentage, Integer unreadCount, Integer readCount, Integer sentToCount) {
		super();
		this.unreadPercentage = unreadPercentage;
		this.unreadCount = unreadCount;
		this.readCount = readCount;
		this.sentToCount = sentToCount;
	}

	public MemoSummaryDto(MemoSummary memoSummary) {
		this.unreadPercentage = memoSummary.getUnreadPercentage();
		this.unreadCount = memoSummary.getUnreadCount();
		this.readCount = memoSummary.getReadCount();
		this.sentToCount = memoSummary.getSentToCount();
	}

	public Float getUnreadPercentage() {
		return unreadPercentage;
	}

	public Integer getUnreadCount() {
		return unreadCount;
	}

	public Integer getReadCount() {
		return readCount;
	}

	public Integer getSentToCount() {
		return sentToCount;
	}

	public void setUnreadPercentage(Float unreadPercentage) {
		this.unreadPercentage = unreadPercentage;
	}

	public void setUnreadCount(Integer unreadCount) {
		this.unreadCount = unreadCount;
	}

	public void setReadCount(Integer readCount) {
		this.readCount = readCount;
	}

	public void setSentToCount(Integer sentToCount) {
		this.sentToCount = sentToCount;
	}
}
