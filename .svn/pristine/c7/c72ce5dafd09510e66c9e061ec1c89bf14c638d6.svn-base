package controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Singleton;

import controllers.actions.ApiAuthAction;
import controllers.aspects.ValidatorAspect;
import controllers.dto.VideoKycDto;
import core.services.VideokycService;
import core.utils.Constants;
import core.utils.Enums.VideokycAgentStatus;
import core.utils.PropertyUtil;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import utils.RmsApplicationContext;

@Singleton
public class VideokycController  extends Controller  {	
	private static Logger logger = LoggerFactory.getLogger(VideokycController.class);
	
	private VideokycService videokycService;
	private ValidatorAspect validatorAspect ;
	
	public VideokycController() {
		ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();		
		videokycService = (VideokycService) ctx.getBean(Constants.VIDEOKYC_SERVICE_SPRING_BEAN);
		validatorAspect = (ValidatorAspect) ctx.getBean(Constants.VALIDATOR_ASPECT_SPRING_BEAN);
	}
	
	@With(ApiAuthAction.class)
	public Result getGroupCallWaitTime(final Integer groupId, final Integer priority) {
		logger.info("get VideoKYC call wait");
		validatorAspect.validateGetGroupCallWaitTime(groupId, priority);
		VideoKycDto dto = new VideoKycDto();
		dto.setGroupId(groupId);
		dto.setPriority(priority);	
		
		Integer callWaitTime =  videokycService.getGroupCallWaitTime(groupId, priority);
		String waitTimeupperLimit = PropertyUtil.getProperty(Constants.CALL_WAIT_TIME_UPPER_LIMIT);
		if (callWaitTime == 0) {
			dto.setStatus(VideokycAgentStatus.Available.getId());
			dto.setWaitTimeInSec(0);
		} else if(callWaitTime >= Integer.valueOf(waitTimeupperLimit)) {
			dto.setStatus(VideokycAgentStatus.NotAvailable.getId());
			dto.setWaitTimeInSec(callWaitTime);
		} else {
			dto.setStatus(VideokycAgentStatus.Busy.getId());
			dto.setWaitTimeInSec(callWaitTime);
		}
		JsonNode result = Json.toJson(dto);
		logger.info("video kyc call wait : "+result.toString());
		
		return ok(result);
	}	
	
}
