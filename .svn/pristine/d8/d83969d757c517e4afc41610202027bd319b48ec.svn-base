package core.validator;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.validator.Validator;
import org.apache.commons.validator.ValidatorResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import core.utils.Constants;

public class ValidatorFactory {

	private static final Logger			log			= LoggerFactory.getLogger(ValidatorFactory.class);

	private static ValidatorResources	resources	= null;

	static {
		InputStream in = ValidatorFactory.class.getClassLoader().
				getResourceAsStream(Constants.XML_VALIDATOR);
		if (in == null) {
			String errMsg = "could not find validation xml.";
			log.error(errMsg);
			throw new RuntimeException(errMsg);
		}

		try {
			resources = new ValidatorResources(in);
		} catch (SAXException ex) {
			String errMsg = "could not parse validation xml.";
			log.error(errMsg, ex);
			throw new RuntimeException(errMsg);
		} catch (IOException ex) {
			String errMsg = "could not read validation xml.";
			log.error(errMsg, ex);
			throw new RuntimeException(errMsg);
		} finally {
			closeStream(in);
		}
	}

	public static Validator getValidator(String formName) {
		Validator v = new Validator(resources, formName);
		v.setOnlyReturnErrors(true);
		return v;
	}

	private static void closeStream(InputStream in) {
		try {
			in.close();
		} catch (IOException ex) {
			String errMsg = "could not close input stream for validation xml.";
			log.error(errMsg);
			throw new RuntimeException(errMsg);
		}
	}
}
