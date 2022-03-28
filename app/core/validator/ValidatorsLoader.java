package core.validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import org.apache.commons.validator.Validator;
import org.apache.commons.validator.ValidatorResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class ValidatorsLoader {
	private static final Logger							log				= LoggerFactory.getLogger(ValidatorsLoader.class);

	private static HashMap<String, Validator>			validatorMap	= new HashMap<String, Validator>();
	private static HashMap<String, ValidatorResources>	resourceMap		= new HashMap<String, ValidatorResources>();

	/*
	 * getValidator This method gives validator for given validator file and form name
	 */
	public static Validator getValidator(String fileName, String formName) {
		log.debug("get Validator object for validator file: " + fileName + " and form: " + formName);
		if (fileName != null && formName != null) {
			String combinedName = fileName + "-" + formName;
			log.debug("key for validator: " + combinedName);
			Validator validator = validatorMap.get(combinedName);
			if (validator == null) {
				validator = loadValidator(fileName, formName);
				if (validator != null) {
					validatorMap.put(combinedName, validator);
				}
			}
			return validator;
		}
		return null;
	}

	private static Validator loadValidator(String fileName, String formName) {
		log.debug("load Validator object for file: " + fileName + " and form: " + formName);
		ValidatorResources resources = resourceMap.get(fileName);
		if (resources == null) {
			resources = getValidatorsResources(fileName);
			if (resources != null) {
				resourceMap.put(fileName, resources);
			}
		}
		Validator validator = null;
		synchronized (fileName) {
			validator = new Validator(resources, formName);
		}
		return validator;
	}

	private static ValidatorResources getValidatorsResources(String validatorFileName) {
		InputStream in = ValidatorsLoader.class.getClassLoader().getResourceAsStream(validatorFileName);
		if (in == null) {
			String errMsg = "Could not find validation file: " + validatorFileName;
			throw new RuntimeException(errMsg);
		}
		ValidatorResources resources = null;
		try {
			resources = new ValidatorResources(in);
			return resources;
		} catch (SAXException ex) {
			String errMsg = "Could not parse validation file." + validatorFileName;
			log.error(errMsg, ex);
			throw new RuntimeException(errMsg);
		} catch (IOException ex) {
			String errMsg = "Could not read validation file: " + validatorFileName;
			log.error(errMsg, ex);
			throw new RuntimeException(errMsg);
		} finally {
			closeStream(in);
		}
	}

	private static void closeStream(InputStream in) {
		try {
			in.close();
		} catch (IOException ex) {
			throw new RuntimeException("could not close input stream");
		}
	}
}
