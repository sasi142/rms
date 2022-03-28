package controllers.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import core.entities.ActorMonitor;
import core.utils.CommonUtil;
import core.utils.Constants;
import utils.RmsApplicationContext;

@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({ "ip", "totalActors", "totalDbActors", "inMsgCounts", "outMsgCounts", "actorIds", "dbActorIds" })
public class ActorMonitorDto {
	private String				serverStartTime;
	private String				ip;
	private String				instanceId;
	private Integer				totalActors;
	private Integer				totalDbActors;
	private Set<String>			actorIds;
	private Set<String>			dbActorIds;
	private Map<String, Long>	inMsgCounts;
	private Map<String, Long>	outMsgCounts;

	public ActorMonitorDto(ActorMonitor actorMonitor, Boolean details) {
		this.serverStartTime = CommonUtil.getDateTimeWithTimeZone(RmsApplicationContext.getInstance().getServerStartTime(), Constants.TIMEZONE_INDIA);
		this.ip = actorMonitor.getIp();
		this.instanceId = actorMonitor.getInstanceId();
		this.totalActors = actorMonitor.getTotalActors();
		this.totalDbActors = actorMonitor.getTotalDbActors();		
		if (details) {
			this.actorIds = actorMonitor.getActorIds() == null ? new HashSet<String>() : new HashSet<String>(actorMonitor.getActorIds());
			this.dbActorIds = actorMonitor.getDbActorIds() == null ? new HashSet<String>() : new HashSet<String>(actorMonitor.getDbActorIds());
		}
		this.inMsgCounts = new HashMap<String, Long>(actorMonitor.getInMsgCounts());
		this.outMsgCounts = new HashMap<String, Long>(actorMonitor.getOutMsgCounts());
		// addTotalMsgCount(this.inMsgCounts);
		// addTotalMsgCount(this.outMsgCounts);
	}

	private void addTotalMsgCount(Map<String, Long> map) {
		Long total = 0L;
		for (Long value : map.values()) {
			total = total + value;
		}
		map.put("TotalMsgCount", total);
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Integer getTotalActors() {
		return totalActors;
	}

	public void setTotalActors(Integer totalActors) {
		this.totalActors = totalActors;
	}

	public Integer getTotalDbActors() {
		return totalDbActors;
	}

	public void setTotalDbActors(Integer totalDbActors) {
		this.totalDbActors = totalDbActors;
	}

	public Set<String> getActorIds() {
		return actorIds;
	}

	public void setActorIds(Set<String> actorIds) {
		this.actorIds = actorIds;
	}

	public Set<String> getDbActorIds() {
		return dbActorIds;
	}

	public void setDbActorIds(Set<String> dbActorIds) {
		this.dbActorIds = dbActorIds;
	}

	public Map<String, Long> getInMsgCounts() {
		return inMsgCounts;
	}

	public void setInMsgCounts(Map<String, Long> inMsgCounts) {
		this.inMsgCounts = inMsgCounts;
	}

	public Map<String, Long> getOutMsgCounts() {
		return outMsgCounts;
	}

	public void setOutMsgCounts(Map<String, Long> outMsgCounts) {
		this.outMsgCounts = outMsgCounts;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getServerStartTime() {
		return serverStartTime;
	}

	public void setServerStartTime(String serverStartTime) {
		this.serverStartTime = serverStartTime;
	}
	
	
}