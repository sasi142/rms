package core.akka.actors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.Member;
import akka.japi.pf.ReceiveBuilder;
import messages.NotifyAll;
import messages.UserConnection;
import play.libs.Json;

public class RmsCluster extends AbstractActor {
	final static Logger logger = LoggerFactory.getLogger(RmsCluster.class);	
	public static ActorRef		RMS_CLUSTER;
	private final Cluster		cluster;
	private final List<Member>	clusterMemberList;

	public RmsCluster() {
		cluster = Cluster.get(getContext().system());
		clusterMemberList = new ArrayList<Member>();
	}

	public JsonNode getClusterState() {
		CurrentClusterState state = cluster.state();
		logger.info("Getting Cluseter State as " + state);
		ObjectNode node = Json.newObject();
		Iterable<Member> ite = state.getMembers();
		if (ite != null) {
			Iterator<Member> itr = ite.iterator();
			if (itr.hasNext()) {
				Member member = itr.next();
				logger.info("member address " + member.address().toString());
			}
		}
		Set<Member> unreachableMem = state.getUnreachable();
		if (unreachableMem != null && !unreachableMem.isEmpty()) {
			logger.warn("mem: " + unreachableMem.toString());
		}
		return node;
	}

	public static void userConnetion(UserConnection connection) {
		RMS_CLUSTER.tell(connection, null);
	}

	public static void sendMessage(NotifyAll message) {
		RMS_CLUSTER.tell(message, null);
	}

	@Override
	public void preStart() {
		logger.info("subscribe to cluster");
		cluster.subscribe(getSelf(),
				ClusterEvent.initialStateAsEvents(),
				ClusterEvent.MemberEvent.class,
				ClusterEvent.UnreachableMember.class);
	}

	@Override
	public void postStop() {
		logger.info("unsubscribe from cluster");
		cluster.unsubscribe(getSelf());
	}
	
	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();		
		builder.match(JsonNode.class, message -> {
			logger.debug("json message: "+message);
		});
		builder.match(ClusterEvent.MemberUp.class, message -> {
			ClusterEvent.MemberUp memberUp = (ClusterEvent.MemberUp) message;
			logger.info("---------------> Member is Up: {}", memberUp.member());
			clusterMemberList.add(memberUp.member());
		});
		builder.match(ClusterEvent.UnreachableMember.class, message -> {
			ClusterEvent.UnreachableMember memberUnreachable = (ClusterEvent.UnreachableMember) message;
			logger.info("---------------> Member detected as unreachable: {}", memberUnreachable.member());
		});
		builder.match(ClusterEvent.MemberRemoved.class, message -> {
			ClusterEvent.MemberRemoved memberRemoved = (ClusterEvent.MemberRemoved) message;
			logger.info("---------------> Member is Removed: {}", memberRemoved.member());
			clusterMemberList.remove(memberRemoved.member());
		});
		builder.match(ClusterEvent.MemberEvent.class, message -> {
			ClusterEvent.MemberRemoved memberRemoved = (ClusterEvent.MemberRemoved) message;
			logger.info("---------------> Member is Removed: {}", memberRemoved.member());
			clusterMemberList.remove(memberRemoved.member());
			logger.info("member event " + message);
		});
		builder.matchAny(message -> {			
			logger.info("unhandled member event " + message);	
		});		
		return builder.build();
	}
}
