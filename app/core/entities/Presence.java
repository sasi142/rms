package core.entities;

import java.util.List;

import core.utils.Enums.PresenceStatus;

public class Presence {
	private List<Integer> clientIds;	
	private Integer show = PresenceStatus.Unavailable.ordinal();
	public Presence() {
		
	}
	public List<Integer> getClientIds() {
		return clientIds;
	}
	public void setClientIds(List<Integer> clientIds) {
		this.clientIds = clientIds;
	}
	public Integer getShow() {
		return show;
	}
	public void setShow(Integer show) {
		this.show = show;
	}
	@Override
	public String toString() {
		return "Presence [clientIds=" + clientIds + ", show=" + show + "]";
	}		
}
