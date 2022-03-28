package core.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@PropertySource({ Constants.RMS_PROPERTIES, "file:${CONFIG_DIR}/" + Constants.RMS_EXT_PROPERTIES,
	"file:${CONFIG_DIR}/" + Constants.RMS_ENV_PROPERTIES })
public class PropertyUtil {
	public static Environment environment;

	@Autowired
	public PropertyUtil(final Environment env) {
		environment = env;
	}

	public static String getProperty(String key) {
		String value = environment.getProperty(key);
		if(!StringUtils.isEmpty(value)) {
			value.trim();
		}
		return value;
	}

	public static String getProperty(String key, String defaultValue) {		
		String value = environment.getProperty(key, defaultValue);
		if(!StringUtils.isEmpty(value)) {
			value.trim();
		}
		return value;
	}
}
