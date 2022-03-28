/**
 * 
 */
package core.entities;

public class MemoSummary {
	private Float unreadPercentage;
	private Integer unreadCount;
	private Integer readCount;
	private Integer sentToCount;

	public MemoSummary() {
	}

	public MemoSummary(Float unreadPercentage, Integer unreadCount, Integer readCount, Integer sentToCount) {
		super();
		this.unreadPercentage = unreadPercentage;
		this.unreadCount = unreadCount;
		this.readCount = readCount;
		this.sentToCount = sentToCount;
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
