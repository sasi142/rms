package controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Singleton;

import controllers.actions.UserAuthAction;
import controllers.dto.ActorMonitorDto;
import core.entities.ActorMonitor;
import core.services.MonitoringService;
import core.utils.Constants;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.RmsApplicationContext;
import play.mvc.With;
import controllers.actions.UserAuthAction;

@Singleton
public class MonitoringController extends Controller {
	final static Logger logger = LoggerFactory.getLogger(MonitoringController.class);
	
	private MonitoringService	monitoringService;
	
	public MonitoringController() {
		ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
		monitoringService = (MonitoringService) ctx.getBean(Constants.MONITORING_SERVICE_BEAN);
	}
	
	@With(UserAuthAction.class)
	public Result getMonitoringReport(Boolean actors, Boolean details) {
		logger.debug("get monitor json for actors : " + actors + " details : " + details);
		ActorMonitor actorMonitor = monitoringService.getActorMonitor(actors, details);
		ActorMonitorDto actorMonitorDto = new ActorMonitorDto(actorMonitor, details);
		JsonNode monitorJson = Json.toJson(actorMonitorDto);
		logger.debug("return monitor json ");
		return ok(monitorJson);
	}

}
