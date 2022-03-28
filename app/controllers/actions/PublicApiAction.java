package controllers.actions;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import controllers.exceptionMapper.HttpExceptionHandler;
import core.entities.UserContext;
import core.utils.AuthUtil;
import core.utils.Constants;
import core.utils.Enums.ClientType;
import core.utils.ThreadContext;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import utils.RmsApplicationContext;

public class PublicApiAction extends Action.Simple {
	final static Logger logger = LoggerFactory.getLogger(PublicApiAction.class);

	private AuthUtil				authUtil;

	private HttpExceptionHandler	httpExceptionHandler;

	public PublicApiAction() {
		ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
		authUtil = ctx.getBean(AuthUtil.class);
		httpExceptionHandler = ctx.getBean(HttpExceptionHandler.class);
	}

	public CompletionStage<Result> call(Request request) {
		logger.debug("Intercepting public api exception");
		long t1 = System.currentTimeMillis();
		Integer status = 200;
		String clientId = null;
		Integer userId = null;
		Optional<String> requestId = Optional.empty();
		try {		
			requestId = request.header(Constants.X_REQUEST_ID);
			if (requestId.isEmpty()) {
				requestId = Optional.of(UUID.randomUUID().toString());
				logger.debug("Server generated x-request-id " + requestId.get());
			}
			clientId = ClientType.Web.getClientId();
			UserContext context = new UserContext();
			context.setClientId(clientId);
			ThreadContext.setUsercontext(context);
			
			return delegate.call(request);
		} 
		catch (Throwable ex) {
			status = httpExceptionHandler.getStatus(ex);
			Result result = httpExceptionHandler.handleException(ex, status);
			result.withHeader(Constants.X_REQUEST_ID, requestId.get());
			return CompletableFuture.supplyAsync(()->result);
		} finally {
			long diff = System.currentTimeMillis() - t1;
			authUtil.printRequest(request, (int)diff, userId, status, clientId, requestId.get());
			logger.debug("Saved API Request for userId, status, clientId " + userId + "," + status + "," + clientId);
		}
	}
}
