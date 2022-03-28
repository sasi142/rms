package controllers;

import com.google.inject.Singleton;
import controllers.actions.ApiAuthAction;
import core.entities.projections.UnreadCountSummary;
import core.services.UserService;
import core.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import utils.RmsApplicationContext;

import java.util.Optional;

@Singleton
public class SystemApiController extends Controller {
    final static Logger logger = LoggerFactory.getLogger(SystemApiController.class);

    private UserService userService;

    public SystemApiController() {
        ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
        this.userService = (UserService) ctx.getBean(Constants.USER_SERVICE_BEAN);
    }

    @With(ApiAuthAction.class)
    public Result getAllUnreadCount(Integer orgId, String userName) {
        Optional<UnreadCountSummary> summary = userService.unreadCountSummary(orgId, userName);
        logger.info("Summary found for org {}, user {} is {} ", orgId, userName, summary);
        if (summary.isPresent()){
            return ok(Json.toJson(summary));
        }
        else {
            return notFound("No info found for the input combination.");
        }
    }
}
