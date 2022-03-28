package controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.google.inject.Singleton;

import controllers.actions.UserAuthAction;
import core.exceptions.BadRequestException;
import core.services.CacheService;
import core.utils.Constants;
import core.utils.Enums.ErrorCode;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.With;
import utils.RmsApplicationContext;

@Singleton
public class CacheController {
	final static Logger logger = LoggerFactory.getLogger(CacheController.class);

	private CacheService				cacheService;

	public CacheController() {
		ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
		cacheService = (CacheService) ctx.getBean(Constants.CACHE_SERVICE_SPRING_BEAN);
	}
	
	@With(UserAuthAction.class)
	public Result refreshCacheByEntityType(String entityType) {
		logger.info("refresh cache for entity: " + entityType);
		Integer entityCount = 0;
		if (entityType.equalsIgnoreCase("clientCertificate")) {
			entityCount = cacheService.refreshClientCertificateCache();
		}else {
			throw new BadRequestException(ErrorCode.Invalid_entity_type_for_cache_refresh, entityType);
		}
		logger.info("cache is updated successfully for " + entityCount + " " + entityType);
		return Results.ok(entityCount.toString());
	}
}	
