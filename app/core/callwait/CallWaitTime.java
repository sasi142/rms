package core.callwait;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallWaitTime {
	private List<Customer> customers = new ArrayList<Customer>();
	private Map<Integer, List<Agent>> groups = new HashMap<>();
	private Map<Integer, List<Integer>> agentWaitTime = new HashMap<>();
	private List<Agent> agents = new ArrayList<>();
	private Integer averageWaitTime;

	public void findCallWaitTime() {
		setCurrentWaitTime();
		for (Customer customer: customers) {
			List<Agent> agents = groups.get(customer.groupId);
			Integer lowestTime = 100000; // any max number
			Integer agentId = 0;
			for (Agent agent: agents) {		
				List<Integer> waitTime = agentWaitTime.get(agent.agentId);				
				Integer newWaitTime = waitTime.get(waitTime.size()-1);				
				if (newWaitTime == 0) { // available & assign and break;		
					lowestTime = newWaitTime;
					agentId = agent.agentId;
					break;
				}
				else if (lowestTime > newWaitTime){
					lowestTime = newWaitTime;
					agentId = agent.agentId;
				}
			}		
			List<Integer> waitTimeList  = agentWaitTime.get(agentId);
			if (lowestTime > 0) {
				waitTimeList.add(lowestTime);
			}
		}
	}

	public void setCurrentWaitTime() {
		for (Agent agent: agents) {			
			Integer waitTime = 0;
			if (agent.status) { // Busy
				Integer time = (int) (System.currentTimeMillis() - agent.updatedTime)/1000;
				waitTime = averageWaitTime - time;
			}
			List<Integer> waitTimeList = new ArrayList<>();
			waitTimeList.add(waitTime);
			agentWaitTime.put(agent.agentId, waitTimeList);
		}		
	}
}

class Agent {
	Integer agentId;
	Integer groupId;
	List<Integer> waitTime;
	Boolean status;
	Long updatedTime;
}

class Customer {
	Integer id;
	Integer groupId;
}

class Group {
	Integer id;
	List<Integer> agents;
}
