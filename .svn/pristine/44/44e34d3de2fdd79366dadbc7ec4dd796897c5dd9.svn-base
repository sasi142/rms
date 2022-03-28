
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.google.inject.AbstractModule;
import com.typesafe.config.Config;

import configs.AppConfig;
import play.Environment;
import utils.RmsApplicationContext;

public class Module extends AbstractModule {
	private static Logger logger = LoggerFactory.getLogger(Module.class);
	private Environment environment;
	private Config config;
	final RmsApplicationContext appContext;
	
	public Module() {
		logger.info("Module basic constructor is called");
		appContext = RmsApplicationContext.getInstance();
	}
	public Module(Environment environment, Config config) {
		logger.info("Module constructor is called");
		appContext = RmsApplicationContext.getInstance();
		this.environment = environment;
		this.config = config;
	}

	@Override
	protected void configure() {    	
		logger.info("Rms application is starting");		
		ApplicationContext	ctx = new AnnotationConfigApplicationContext(AppConfig.class);			
		appContext.setSpringContext(ctx);
		logger.info("spring context created");
		bind(ApplicationStart.class).asEagerSingleton();
	}
}
