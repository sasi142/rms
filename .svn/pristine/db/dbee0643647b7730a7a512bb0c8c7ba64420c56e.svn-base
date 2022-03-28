package utils;

import org.springframework.context.ApplicationContext;

import core.entities.ActorMonitor;
import core.utils.Constants;

public class RmsApplicationContext {
	private static RmsApplicationContext context = new RmsApplicationContext();
	private String clientId;
	private String 	ip;
	private int 	port;
	private String instanceId;
	private Long serverStartTime = System.currentTimeMillis();

	private ApplicationContext springContext;
	private ActorMonitor actorMonitor;
	
	private RmsApplicationContext() {
		actorMonitor = ActorMonitor.getMonitor();
	}
	
	public static RmsApplicationContext getInstance() {
		return context;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}


	public ApplicationContext getSpringContext() {
		return springContext;
	}

	public void setSpringContext(ApplicationContext springContext) {
		this.springContext = springContext;
	}

	public ActorMonitor getActorMonitor() {
		return actorMonitor;
	}

	public void setActorMonitor(ActorMonitor actorMonitor) {
		this.actorMonitor = actorMonitor;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}
	

	public Long getServerStartTime() {
		return serverStartTime;
	}

	public void setServerStartTime(Long serverStartTime) {
		this.serverStartTime = serverStartTime;
	}

	public String getChannel() {
		return  Constants.PUBSUB_MESSAGE_CHANNEL_PREFIX+ip+"_"+instanceId;
	}
}
