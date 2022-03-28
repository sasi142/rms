/**
 * http://logz.io/blog/how-to-secure-api-in-play-framework/
 * http://rijware.com/securing-a-play-framework-site-with-https-redirects/
 */
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
import play.mvc.Action;
//import play.mvc.Http;
import play.mvc.Http.Request;
//import play.mvc.Http.Response;
import play.mvc.Result;
import utils.RmsApplicationContext;

// http://www.bernhardwenzel.com/blog/2014/02/24/using-spring-with-play-framework
// http://rijware.com/securing-a-play-framework-site-with-https-redirects/

//@Scope("prototype")
// this has to be prototype else gives problem
//@Component
public class ApiAuthAction extends Action.Simple {
	final static Logger logger = LoggerFactory.getLogger(ApiAuthAction.class);
	//@Autowired
	private AuthUtil				authUtil;

	//@Autowired
	private HttpExceptionHandler	httpExceptionHandler;

	public ApiAuthAction() {
		ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
		authUtil = ctx.getBean(AuthUtil.class);
		httpExceptionHandler = ctx.getBean(HttpExceptionHandler.class);
	}

	public CompletionStage<Result> call(Request request) {
		logger.info("intercepting action composition api for auth check");
		Long t1 = System.currentTimeMillis();
		//Request req = null;
		Integer status = 200;
		String clientId = null;
		Integer userId = null;
		Optional<String> requestId = Optional.empty();
		try {
			//Http.RequestHeader request = ctx.request();
			requestId = request.header(Constants.X_REQUEST_ID);
			if (requestId.isEmpty()) {
				requestId = Optional.of(UUID.randomUUID().toString());
			}
			//if (requestId != null) {
				//ctx.response().setHeader(Constants.X_REQUEST_ID, requestId.get());
			//}

			logger.info("calling api auth check for " + requestId);
			UserContext userContext = authUtil.checkApiAuth(request, requestId.get());
			logger.info("api auth check succcessful for " + requestId);
			//req = ctx.request();
			//UserContext userContext = (UserContext) ctx.args.get("usercontext");
			if (userContext != null) {
				if (userContext.getUser() != null) {
					userId = userContext.getUser().getId();
				}
				clientId = userContext.getClientId();
			}
			logger.info("created user context " + userContext);
			return delegate.call(request);
		} catch (Throwable ex) {
			status = httpExceptionHandler.getStatus(ex);
			Result result = httpExceptionHandler.handleException(ex, status);
			result.withHeader(Constants.X_REQUEST_ID, requestId.get());
			return CompletableFuture.supplyAsync(()->result);
		} finally {
			Integer diff = (int) (System.currentTimeMillis() - t1);
			authUtil.printRequest(request, diff, userId, status, clientId, requestId.get());
			logger.info("created API Request for userId, status, clientId " + userId + "," + status + "," + clientId);
		}
	}
}
