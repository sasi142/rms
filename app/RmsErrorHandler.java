
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

@Singleton
public class RmsErrorHandler extends DefaultHttpErrorHandler {
	private static Logger logger = LoggerFactory.getLogger(RmsErrorHandler.class);
	
	@Inject
	public RmsErrorHandler(Config config, Environment environment, OptionalSourceMapper sourceMapper, Provider<Router> routes) {
		
		super(config, environment, sourceMapper, routes);
	}

	protected CompletionStage<Result> onProdServerError(RequestHeader request, UsefulException ex) {
		logger.error("prod server error", ex);
		return CompletableFuture.completedFuture(Results.internalServerError("internal server error"));
	}

	protected CompletionStage<Result> onForbidden(RequestHeader request, String message) {
		logger.info("You're not allowed to access this resource");
		return CompletableFuture.completedFuture(Results.forbidden("You're not allowed to access this resource."));
	}
	protected CompletionStage<Result> onNotFound(RequestHeader request, String message) {
		logger.info("Request not found");
		return CompletableFuture.completedFuture(Results.notFound("Uri not found"));
	}
	
	public CompletionStage<Result> onBadRequest(RequestHeader request, String message) {
		logger.info("bad Request message : {}", message );
		    return CompletableFuture.completedFuture(
		        Results.badRequest("Input is not valid"));
		  }
	
	/*public CompletionStage<Result> onClientError(RequestHeader request, int statusCode, String message) {
		logger.info("client error: {}, message : {}",statusCode, message );
		    return CompletableFuture.completedFuture(
		        Results.status(statusCode, "A client error occurred: " + message));
		  }*/
}
