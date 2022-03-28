package controllers.actions;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
//import org.springframework.context.annotation.Scope;
//import org.springframework.stereotype.Component;

import controllers.exceptionMapper.HttpExceptionHandler;
import core.entities.UserContext;
import core.utils.AuthUtil;
import core.utils.Constants;
import core.utils.ThreadContext;
import play.mvc.Action;
//import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import utils.RmsApplicationContext;

// http://www.bernhardwenzel.com/blog/2014/02/24/using-spring-with-play-framework
// http://rijware.com/securing-a-play-framework-site-with-https-redirects/

//@Scope("prototype") // this has to be prototype else gives problem
//@Component
public class UserAuthAction extends Action.Simple {
	
	final static Logger logger = LoggerFactory.getLogger(UserAuthAction.class);
	
	@Autowired
	private AuthUtil				authUtil;

	@Autowired
	private HttpExceptionHandler	httpExceptionHandler;

	public UserAuthAction() {		
		ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
		authUtil = ctx.getBean(AuthUtil.class);
		httpExceptionHandler = ctx.getBean(HttpExceptionHandler.class);
	}

	@Override
	public CompletionStage<Result> call(Request request)  {
		logger.info("intercepting action composition for auth check");
		//Request req = null;
		Integer status = 200;
		Integer userId = null;
		String clientId = null;
		Optional<String> requestId = Optional.empty();
		Long t1 = System.currentTimeMillis();
		try {
			//req = ctx.request();
			requestId = request.header(Constants.X_REQUEST_ID);
			if (requestId.isEmpty()) {
				requestId = Optional.of(UUID.randomUUID().toString());
			}
		//	final UserContext userContext = authUtil.checkUserAuth(request, requestId.get());
			final UserContext userContext = authUtil.mockUserAuth(request, requestId.get());
			userContext.setClientIPAddress(authUtil.getClientIP(request));
			logger.debug("received user context as " + (userContext == null ? null : userContext.toString()));
			//ctx.args.put("usercontext", userContext);

			if (userContext != null) {
				if (userContext.getUser() != null) {
					userId = userContext.getUser().getId();
				}
				clientId = userContext.getClientId();
				ThreadContext.setUsercontext(userContext);
			}
			logger.debug("processing call with userId " + userId + ",clientId " + clientId);
			return delegate.call(request);
		} catch (Throwable ex) {
			status = httpExceptionHandler.getStatus(ex);
			Result result = httpExceptionHandler.handleException(ex, status);
			result.withHeader(Constants.X_REQUEST_ID, requestId.get());
			return CompletableFuture.supplyAsync(()-> result);
		} finally {
			Integer diff = (int) (System.currentTimeMillis() - t1);
			authUtil.printRequest(request, diff, userId, status, clientId, requestId.get());
			logger.debug("API request creates with userId " + userId + ",clientId " + clientId + ",status " + status);
		}
	}
}
