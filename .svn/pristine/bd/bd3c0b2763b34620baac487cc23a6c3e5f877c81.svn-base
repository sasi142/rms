package core.akka.utils;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import messages.ConnId;
import messages.UserConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import scala.concurrent.Await;
import scala.concurrent.Future;
import utils.RmsApplicationContext;
import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Identify;
import akka.pattern.AskableActorSelection;
import akka.util.Timeout;

import com.fasterxml.jackson.databind.JsonNode;

import core.akka.actors.RmsActorSystem;
import core.entities.ActorMonitor;
import core.entities.ConnectionInfo;
import core.utils.Constants;
import core.utils.ThreadContext;

@Component
public class AkkaUtil implements InitializingBean {
	
	final static Logger logger = LoggerFactory.getLogger(AkkaUtil.class);
	
	@Autowired
	private Environment	env;

	private Integer	actorSelectionTimeout;
	
	private ActorMonitor actorMonitor;
	
	public ActorRef getActorRef(String uuid) {
		ActorRef ref= null;		
		ConnectionInfo info = actorMonitor.getActorMap().get(uuid);
		if (info != null && info.getConnection() != null) {
			ref = info.getConnection().getActorRef();
		}
		return ref;
	}

	public ConnId getConnId(String uuid) {
		ConnectionInfo info = actorMonitor.getActorMap().get(uuid);
		if (info != null && info.getConnection() != null) {
			return info.getConnection().getConnId();
		}
		return null;
	}

	public ActorRef getActorRef(JsonNode node) {
		String uuid = node.findPath("uuid").asText();
		return getActorRef(uuid);
	}

	public ConnId getConnId(JsonNode node) {
		String uuid = node.findPath("uuid").asText();
		return getConnId(uuid);
	}

	public ActorSelection getActorSelection(JsonNode node) {
		logger.debug("Get Actor with input " + node);
		long t1 = System.currentTimeMillis();
		ActorSelection sel = null;
		try {
			String url = getActorUrl(node);
			sel = RmsActorSystem.get().actorSelection(url);
			return sel;

		} catch (Exception e) {
			logger.info("failed to get actor: ", e);
		}

		long t2 = System.currentTimeMillis();
		logger.debug("time to get actor: " + (t2 - t1));
		return sel;
	}

	public ActorRef getActor(JsonNode node) {
		logger.debug("Get Actor with input " + node);
		long t1 = System.currentTimeMillis();
		ActorRef ref = null;
		try {
			String uuid = node.findPath("uuid").asText();
			logger.debug("Getting Actor for uuid" + uuid);
			UserConnection con = ThreadContext.get();
			if (con != null) {
				String uuid1 = ThreadContext.get().getUuid();
				if (uuid.equalsIgnoreCase(uuid1)) {
					ActorRef ref1 = ThreadContext.get().getActorRef();
					if (ref1 != null) {
						return ref1;
					}
				}
			}
			String url = getActorUrl(node);
			logger.debug("Got Actor url " + url + " for uuid" + uuid);
			ActorSelection sel = RmsActorSystem.get().actorSelection(url);
			logger.debug("check actor exist for url " + url);
			Timeout t = new Timeout(actorSelectionTimeout, TimeUnit.SECONDS);
			AskableActorSelection asker = new AskableActorSelection(sel);
			SecureRandom rand = new SecureRandom();
			int n = rand.nextInt(50) + 1;
			Long num = System.currentTimeMillis() + Thread.currentThread().getId() + n;
			Future<Object> fut = asker.ask(new Identify(1), t);
			ActorIdentity ident = (ActorIdentity) Await.result(fut, t.duration());
			ref = ident.getRef();
		
			logger.debug("Returning Actor Reference " + ref + " for uuid" + uuid);
		} catch (Exception e) {
			logger.warn("failed to get actor: ", e);
		}

		long t2 = System.currentTimeMillis();
		logger.debug("time to get actor: " + (t2 - t1));
		return ref;
	}

	private String getActorUrl(String ip, Integer port, String uuid) {
		String akkaUrl = "akka.tcp://" + RmsActorSystem.getActorSystemName() + "@" + ip + ":" + port + "/user/" + uuid;
		logger.debug("returning getActorUrl : Akka url: " + akkaUrl);
		return akkaUrl;
	}

	private String getActorUrl(JsonNode node) {
		JsonNode ip = node.findPath("ip");
		JsonNode port = node.findPath("port");
		String uuid = node.findPath("uuid").asText();
		String url = null;
		if (ip != null && port != null && port.asInt() != 0) {
			url = getActorUrl(ip.asText(), port.asInt(), uuid);
		}
		else {
			url = "user/" + uuid;
		}
		logger.debug("returning getActorUrl : akka url: " + url);
		return url;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		actorSelectionTimeout = Integer.valueOf(env.getProperty(Constants.ACTOR_SELECTION_TIMEOUT));
		actorMonitor = RmsApplicationContext.getInstance().getActorMonitor();
		logger.info("actorSelectionTimeout:" + actorSelectionTimeout);
	}
}
