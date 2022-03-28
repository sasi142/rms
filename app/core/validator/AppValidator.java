package core.validator;

import java.util.regex.Pattern;

import org.apache.commons.validator.Field;
import org.apache.commons.validator.GenericTypeValidator;
import org.apache.commons.validator.GenericValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.commons.validator.util.ValidatorUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.utils.RegxUtil;

/**
 * This class contains all the validation rules that we need to validate various
 * field values. CAUTION: - For validateMinLength: min is obtained from key of
 * arg passed to the field at position MIN_POS. - For validateMaxLength: max is
 * obtained from key of arg passed to the field at position MAX_POS. - For
 * validateRegexp: regexp is obtained from key of arg passed to the field at
 * position REGEXP_POS. For validateIntRange: min is obtained from key of atg
 * passed to the field at position MIN_INT_POS max is obtained from key of arg
 * passed to the field at position MAX_INT_POS.
 */

public class AppValidator {
	private static final Logger	log				= LoggerFactory.getLogger(AppValidator.class);
	private final static int	MIN_POS			= 0;
	private final static int	MAX_POS			= 1;
	private final static int	REGEXP_POS		= 2;
	private final static int	MIN_INT_POS		= 3;
	private final static int	MAX_INT_POS		= 4;
	private final static int	minPhoneLength	= 10;
	private final static int	maxPhoneLength	= 10;
	private RegxUtil			regxUtil		= null;

	public AppValidator() {
		this.regxUtil = new RegxUtil();
	}

	/**
	 * Checks whether integer value of a field is between a min-max. that is min
	 * <= value <= max . CAUTION: min is obtained from key of atg passed to the
	 * field at position MIN_INT_POS max is obtained from key of arg passed to
	 * the field at position MAX_INT_POS.
	 */
	public static boolean validateIntRange(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null) {
			return true;
		}

		final int val = Integer.parseInt(value);
		final int min = Integer.parseInt(field.getArg(MIN_INT_POS).getKey());
		final int max = Integer.parseInt(field.getArg(MAX_INT_POS).getKey());
		final boolean isValid = GenericValidator.isInRange(val, min, max);
		if (!isValid) {
			log.error("Failed to validate the integer range : " + val + " , " + " min : " + min + " , " + "max : " + max);
		}
		return isValid;
	}

	/**
	 * Checks whether length of string value of the field is less than or equal
	 * to the max. CAUTION: max is obtained from key of arg passed to the field
	 * at position MAX_POS.
	 */
	public static boolean validateMaxLength(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty()) {
			return true;
		}

		final int max = Integer.parseInt(field.getArg(MAX_POS).getKey());
		final boolean isValid = GenericValidator.maxLength(value, max);
		if (!isValid) {
			log.error("Failed to validate the maxLength : " + value);
		}
		return isValid;
	}

	/**
	 * Checks whether length of string value of the field is greater than or
	 * equal to the min. CAUTION: min is obtained from key of arg passed to the
	 * field at position MIN_POS.
	 */
	public static boolean validateMinLength(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty()) {
			return true;
		}

		final int min = Integer.parseInt(field.getArg(MIN_POS).getKey());
		final boolean isValid = GenericValidator.minLength(value, min);
		if (!isValid) {
			log.error("Failed to validate the minLength : " + value);
		}
		return isValid;
	}

	public boolean validatePassword(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty()) {
			return true;
		}
		boolean valid = regxUtil.validatePassword(value);
		if (valid) {
			if (value.contains(" ")) {// check for empty space anywhere in password string
				valid = false;
			}
		}

		return valid;
	}

	/**
	 * Checks whether phone number satisfies following rules. minLength : 10
	 * maxLength : 18 It is a number.
	 */
	public static boolean validatePhoneNumber(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty()) {
			return true;
		}
		final boolean isValid = (value.length() >= minPhoneLength && value.length() <= maxPhoneLength && GenericTypeValidator.formatLong(value) != null);

		if (!isValid) {
			log.error("Failed to validate the phone number : " + value + " , " + "minlength : " + minPhoneLength + " , " + "maxLength : " + maxPhoneLength);
		}
		return isValid;
	}

	/**
	 * Checks whether length of string value of the field is less than or equal
	 * to the max. CAUTION: regexp is obtained from key of arg passed to the
	 * field at position REGEXP_POS.
	 */
	public static boolean validateRegexp(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty()) {
			return true;
		}

		final String regexp = field.getArg(REGEXP_POS).getKey();
		boolean isValid = false;
		if (regexp != null && regexp.length() > 0 && Pattern.matches(regexp, value)) {
			isValid = true;
		}
		if (!isValid) {
			log.error("Failed to validate the Regexp : " + value);
		}
		return isValid;
	}

	/**
	 * Checks whether the field isn't null and length of the field is greater
	 * than zero not including whitespace.
	 */
	public static boolean validateRequired(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		final boolean isValid = GenericValidator.isBlankOrNull(value);
		if (isValid) {
			log.error("Failed to get the required field : " + field.toString());
		}
		return !(isValid); // Negation
	}

	/**
	 * Checks whether the due date is greater than todays date or not if not
	 * throws exception.
	 */
	public static boolean validateDueDate(final Object bean, final Field field) {
		boolean isValid = false;
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		try {
			final DateTime dueDate = new DateTime(value);
			final DateTime todaysteDate = new DateTime();
			final int result = DateTimeComparator.getDateOnlyInstance().compare(dueDate, todaysteDate);
			if (result == -1) {
				log.error("Invalid duedate. Due date  : " + value + " should be greater than todays' date. ");
			} else {
				isValid = true;
			}
		} catch (final Exception ex) {
			log.error("Invalid duedate. Entered due date : " + value + " is not in proper format.");
		}
		return isValid;
	}

	/**
	 * Checks whether the date isn't null and valid and can be parsed into JODA
	 * date .
	 */
	public static boolean dateValidator(final Object bean, final Field field) {
		boolean isValid = false;
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		try {
			new DateTime(value);
			isValid = true;
		} catch (final IllegalArgumentException ex) {
			log.error("Invalid date. Entered date : " + value + "must be a valid date.");
		}
		return isValid;
	}

	/**
	 * Checks whether the field has valid e-mail address.
	 */
	public static boolean validateEmail(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty()) {
			return true;
		}

		boolean isValid = false;

		if (value.trim().length() == value.length()) {
			isValid = EmailValidator.getInstance().isValid(value);
		}
		if (!isValid) {
			log.error("Failed to validate the email id : " + value);
		}
		return isValid;
	}

	/**
	 * Checks whether the field has valid url address.
	 */
	public static boolean validateUrl(final Object bean, final Field field) {
		boolean isValid = false;
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());

		if (value == null || value.isEmpty())
			return true;

		//final UrlValidator urlValidator = new UrlValidator();
		//isValid = urlValidator.isValid(value.trim());
		isValid = true;
		if (!isValid) {
			log.error("Failed to validate the Url : " + value);
		}
		return isValid;
	}

	/**
	 * Validates whether the object is null or not.
	 * 
	 * @param bean
	 * @param field
	 * @return
	 */
	public static boolean isObjectNull(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (GenericValidator.isBlankOrNull(value)) {
			log.error("Validation failed for the object type field. Entered value is either null or empty.");
			return false;
		}
		return true;
	}

	/**
	 * Validates whether the object is null or not.
	 * 
	 * @param bean
	 * @param field
	 * @return
	 */
	public static boolean noNullOrEmpty(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty()) {
			return true;
		}
		if (GenericValidator.isBlankOrNull(value)) {
			log.error("Validation failed for the object type field. Entered value is either null or empty.");
			return false;
		}
		return true;
	}

	/**
	 * Checks whether the text is AsciiPrintable.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean isAsciiPrintable(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		return regxUtil.isAsciiPrintable(value);
	}

	/**
	 * Checks whether input text contains only alphabets.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean isOnlyAlphabets(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		return regxUtil.isOnlyAlphabets(value);
	}

	/**
	 * Checks whether input text is only numbers.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean isOnlyNumbers(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty()) {
			return true;
		}
		return regxUtil.isOnlyNumbers(value);
	}

	/**
	 * Checks whether input text is only numbers.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean containsOnlyNumbers(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty()) {
			return true;
		}
		return !regxUtil.isOnlyNumbers(value); // Applied negation over here
	}

	/**
	 * Checks whether input text contains only alpha numeric.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean isOnlyAlphaNumeric(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty()) {
			return true;
		}
		return regxUtil.isOnlyAlphaNumeric(value);
	}

	/**
	 * Checks whether input text contains only alpha numeric.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean isOnlyAlphaORNumeric(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		return regxUtil.isOnlyAlphaORNumeric(value);
	}

	/**
	 * Checks whether input text contains alpha OR numeric.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean containsAlphaORNumericChars(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		return regxUtil.containsAlphaORNumericChars(value);
	}

	/**
	 * Checks whether input text contains alpha numeric.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean containsAlphaNumericChars(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty()) {
			return true;
		}
		return regxUtil.containsAlphaNumericChars(value);
	}

	/**
	 * Checks whether input text starts with number.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean startWithNumber(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		return regxUtil.startWithNumber(value);
	}

	/**
	 * Checks whether input text contains special characters.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean containsSpecialChar(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		return !regxUtil.containsSpecialChar(value); // Applied negation over
		// here
	}

	/**
	 * Checks whether input text starts with special character.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean startsWithSpecialChar(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		return !(regxUtil.startsWithSpecialChar(value)); // Applied negation
		// over here
	}

	/**
	 * Checks whether input text ends with special character.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean endsWithSpecialChar(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		return !(regxUtil.endsWithSpecialChar(value)); // Applied negation over
		// here
	}

	/**
	 * Checks whether input text has only special characters.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean onlySpecialChars(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		return !(regxUtil.onlySpecialChars(value)); // Applied negation over
		// here
	}

	/**
	 * Checks whether input text contains only capital alphabets.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean onlyCapitalAlphabets(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		return regxUtil.onlyCapitalAlphabets(value);
	}

	/**
	 * Checks whether input text has consecutive special characters.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean containsConsecutiveSpecialChars(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		return !(regxUtil.containsConsecutiveSpecialChars(value)); // Applied
		// negation
		// over here
	}

	/**
	 * Checks whether input text has XML or HTML code.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean containsHtmlXmlCode(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty()) {
			return true;
		}
		boolean match = regxUtil.containsHtmlXmlCode(value); // Applied negation
		// over here
		return match;
	}

	/**
	 * Checks whether input text has XML or HTML code.
	 * 
	 * @param field
	 * @return boolean result
	 */
	public boolean noHtmlXmlCode(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty()) {
			return true;
		}
		boolean match = regxUtil.containsHtmlXmlCode(value); // Applied negation
		// over here
		return !match;
	}

	/**
	 * Checks whether input text contains alpha char.
	 * 
	 * @param bean
	 * @param field
	 * @return
	 */
	public boolean containsAlphaChar(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		return (regxUtil.containsAlphaChar(value));
	}

	public boolean isBoolean(final Object bean, final Field field) {
		final String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
		if (value == null || value.isEmpty())
			return true;
		// Considered 0, 1 as boolean values.
		else if (value.equals("1") || value.equals("0") || value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))
			return true;
		else
			return false;
	}

	public static String getUrl(String url) {
		String tempUrl = url;
		if (url.startsWith("http://")) {
			String url1[] = url.split("http://");
			tempUrl = url1[1];
		} else if (url.startsWith("https://")) {
			String url1[] = url.split("https://");
			tempUrl = url1[1];
		}
		System.out.println("temp: " + tempUrl);

		if (!tempUrl.startsWith("www.")) {
			tempUrl = "www." + tempUrl;
		}
		return tempUrl;
	}

	public static void main(String s[]) {
		//String value = "ideacts.com";
		//String value = "www.ideacts.com";
		//String value = "https://www.ideacts.com";
		//String value = "http://www.ideacts.com";
		String value = "http://ideacts.com";
		//String value = "https://.ideacts.com";
		//String value = "http://www.ideacts.com";

		String tempUrl = getUrl(value);

		String url = "https://" + tempUrl;
		final UrlValidator urlValidator = new UrlValidator();
		Boolean val = urlValidator.isValid(url);

		//System.out.println(tempUrl+" : " + val);

	}

}
