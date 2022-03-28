import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

import core.akka.utils.ActorCleanupUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import core.akka.actors.RmsActorSystem;
import core.services.PubSubService;
import core.services.PubSubServiceImpl;
import core.services.VideokycService;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.PropertyUtil;
import play.api.Configuration;
import play.inject.ApplicationLifecycle;
import utils.RmsApplicationContext;

@Singleton
public class ApplicationStart {
	private static Logger logger = LoggerFactory.getLogger(ApplicationStart.class);
	private ApplicationContext	ctx;
	private CommonUtil			commonUtil;
	private PubSubService		pubSubService;
	//private VideokycService videokycService;

	@Inject
	public ApplicationStart(ApplicationLifecycle appLifecycle, final Configuration configuration) {
		logger.info("RMS app is started");	
		onStart(appLifecycle, configuration);
		appLifecycle.addStopHook(() -> {
			logger.info("RMS App is stopped");
			return CompletableFuture.runAsync(commonUtil::cleanUpConnectionsOnShutdown);
			//return CompletableFuture.completedFuture(null);
		});
	}

	private void onStart(ApplicationLifecycle appLifecycle, final Configuration configuration) {
		try {
			final RmsApplicationContext appContext = RmsApplicationContext.getInstance();			
			ctx = RmsApplicationContext.getInstance().getSpringContext();
			RmsActorSystem.create();
			String clientId = PropertyUtil.getProperty(Constants.CLIENT_ID);
			String instanceIdFile = PropertyUtil.getProperty(Constants.INSTANCE_ID_FILE, "/tmp/rms.id");
			logger.info("InstanceId file path {}", instanceIdFile);
			String prevInstanceId = null;
			if (Files.exists(Paths.get(instanceIdFile))){
				prevInstanceId = Files.readString(Paths.get(instanceIdFile));
				logger.info("Found prev instanceId {} in the file ", prevInstanceId);
			}
			logger.debug("client id: " + clientId);
			commonUtil = ctx.getBean(CommonUtil.class); 
			pubSubService = getPubSubServiceBean();

			if (prevInstanceId != null && prevInstanceId.trim().length() != 0){
				logger.info("Clean connections for previous instance id {}", prevInstanceId);
				commonUtil.cleanUpConnectionsOnStartup(prevInstanceId);
			}

			//videokycService = getVideoKycService();
			// videokycService.cleanupCustomerQueue();
			appContext.setClientId(clientId); 	
			InetAddress host = null;
			host = InetAddress.getLocalHost();
			if (host != null) {
				logger.info("Ip address of machine: " + host.getHostAddress());
				logger.info("host name of machine of machine: " + host.getHostName());	
				appContext.setIp(host.getHostAddress());
				String instanceId = String.valueOf(getRandomNumber());
				Files.writeString(Paths.get(instanceIdFile), instanceId);
				logger.info("------------------------- Instance Id---------------------------"+instanceId);
				appContext.setInstanceId(instanceId);
				//commonUtil.cleanUpConnectionInfo();
				pubSubService.subscribe();		
			}
			else{
				throw new RuntimeException("Localhost is not found");
			}

		} catch (Exception e) {
			logger.error("error to initialize context ", e);
			System.exit(1);
		}
		logger.info("Rms application context initialized");
	}

	public PubSubService getPubSubServiceBean() {
		pubSubService = (PubSubServiceImpl) RmsApplicationContext.getInstance().getSpringContext().getBean(Constants.PUBSUB_SERVICE_SPRING_BEAN);
		return pubSubService;
	}

	public CommonUtil getCommonUtil() {
		commonUtil = (CommonUtil) RmsApplicationContext.getInstance().getSpringContext().getBean(Constants.COMMON_UTIL_SPRING_BEAN);
		return commonUtil;
	}
	//public VideokycService getVideoKycService() {
		//videokycService = (VideokycService) RmsApplicationContext.getInstance().getSpringContext().getBean(Constants.VIDEOKYC_SERVICE_SPRING_BEAN);
		//return videokycService;
	//}
	public int getRandomNumber() {
		SecureRandom r = new SecureRandom();
		return 10000 + r.nextInt(20000);
	}
}