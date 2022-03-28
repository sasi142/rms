package configs;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@ComponentScan({"controllers", "core","utils"})
@ImportResource({"classpath:application-context.xml","classpath:jpa-config.xml"})
@EnableAsync
public class AppConfig {
	public AppConfig() {

	}
}