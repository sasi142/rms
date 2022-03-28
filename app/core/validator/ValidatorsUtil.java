/**
 * ValidatorsUtil
 */
package core.validator;

import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;

import org.apache.commons.validator.Validator;
import org.apache.commons.validator.ValidatorException;
import org.apache.commons.validator.ValidatorResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import core.entities.GroupPreference;
import core.exceptions.BadRequestException;
import core.exceptions.InternalServerErrorException;
import core.utils.Enums.ErrorCode;
//import core.utils.Enums.GroupPreferencesName;
//import web.dto.GroupPreferenceDto;

public class ValidatorsUtil {
	private static final Logger	log	= LoggerFactory.getLogger(ValidatorsUtil.class);

	@SuppressWarnings("unchecked")
	public static void validate(String formName, Object objectToValidate) {
		log.debug("started validating the form" + formName);
		Validator v = ValidatorFactory.getValidator(formName);
		log.info("v.getUseContextClassLoader():"+v.getUseContextClassLoader()+",v.getClassLoader():"+v.getClassLoader());
		v.setUseContextClassLoader(true);
		v.setParameter(Validator.BEAN_PARAM, objectToValidate);
		
		try {
			v.setOnlyReturnErrors(true);
			ValidatorResults vrs = v.validate();
			if (!vrs.isEmpty()) {
				String errMsg = "";
				Set<String> failures = vrs.getPropertyNames();
				for (String s : failures)
					errMsg += (s + ",");
				String msg = errMsg.substring(0, errMsg.length() - 1);
				log.error("Failed to validate the form " + formName + " for fields:" + msg);
				throw new BadRequestException(ErrorCode.Validation_Failed, "Failed to validate the form " + formName + " for the fields:" + msg);
			}
			else {
				log.info("validated the form " + formName + " successfully");
			}
		} catch (ValidatorException ex) {
			String errMsg = "Failed to validate the form " + formName;
			log.error(errMsg, ex);
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, errMsg, ex);
		}
	}
	
	/**
	 * Method to check whether given collection has non null and positive integer elements. If it is not throws exception.
	 * 
	 * @param collection
	 * @throws BadRequestException
	 */
	@SuppressWarnings("rawtypes")
	public static void validateMemberIds(Collection collection) throws BadRequestException {
		if (collection == null || collection.isEmpty())
			throw new BadRequestException(ErrorCode.Validation_Failed, "Entered list of members is either null or empty.");

		if (collection.contains(null))
			throw new BadRequestException(ErrorCode.Bad_Input, "Invalid input value null. Value has to be greater than zero.");

		for (Object i : collection) {
			assertPositiveInteger(i);
		}
	}
	
	/**
	 * Checks whether the input Integer/Long/String value is either zero or negative if it is throws BadRequestException.
	 * 
	 * @param id
	 */
	public static void assertPositiveInteger(Object obj) {
		Integer id = null;
		if (obj == null) {
			throw new BadRequestException(ErrorCode.Bad_Input, "Invalid input value "+obj+". Value has to be not null.");
		}

		if (obj instanceof Integer) {
			id = (Integer) obj;
		} else if (obj instanceof Long) {
			id = ((Long) obj).intValue();
		} else {
			try {
				id = Integer.parseInt(obj.toString());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(ErrorCode.Bad_Input, "Invalid input value "+id+". Value has to be greater than zero.");
			}
		}

		if (id == null) {
			throw new BadRequestException(ErrorCode.Bad_Input, "Invalid input value "+id+". Value has to be greater than zero.");
		}

		if (id < 1) {
			throw new BadRequestException(ErrorCode.Bad_Input, "Invalid input value "+id+". Value has to be greater than zero.");
		}
	}
	
	public static void assertNonNegativeInteger(Object obj) {
		Integer id = null;
		if (obj == null) {
			throw new BadRequestException(ErrorCode.Bad_Input, "Invalid input value "+obj+". Value has to be not null.");
		}

		if (obj instanceof Integer) {
			id = (Integer) obj;
		} else if (obj instanceof Long) {
			id = ((Long) obj).intValue();
		} else {
			try {
				id = Integer.parseInt(obj.toString());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(ErrorCode.Bad_Input, "Invalid input value "+id+". Value has to be greater than zero.");
			}
		}

		if (id == null) {
			throw new BadRequestException(ErrorCode.Bad_Input, "Invalid input value "+id+". Value has to be greater than zero.");
		}

		if (id < 0) {
			throw new BadRequestException(ErrorCode.Bad_Input, "Invalid input value "+id+". Value has to be non-negative.");
		}
	}
	
	/**
	 * Method to check whether given collection has non null valid preference values. If it is not throws exception.
	 * 
	 * @param GroupPreferenceDto
	 * @throws BadRequestException
	 */
/*	public static Boolean validateGroupPreferences(List<GroupPreferenceDto> preferences) throws BadRequestException {
		if (preferences == null || preferences.isEmpty())
			throw new BadRequestException(ErrorCode.Validation_Failed, "Entered list of preferences is either null or empty.");

		if (preferences.contains(null))
			throw new BadRequestException(ErrorCode.Bad_Input, "Invalid input value null. Value has to be greater than zero.");

		for (GroupPreferenceDto preference : preferences) {
			if (GroupPreferencesName.findByName(preference.getPreference()) == null) {			
				throw new BadRequestException(ErrorCode.Bad_Input, "Entered list of preferences is not valid.");
			}
		}
		return true;
	}	*/
}
