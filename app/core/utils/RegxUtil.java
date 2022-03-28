package core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * A class for providing utility methods for regular expressions.
 */
public class RegxUtil {
	private static Matcher			matcher							= null;
	private static final Pattern	ONLY_ALPHA_CHAR					= Pattern.compile("^([\\sA-Za-z])*([\\sA-Za-z])+");
	private static final Pattern	ONLY_NUMBER						= Pattern.compile("^([\\s0-9])*[\\s0-9]+");
	private static final Pattern	ONLY_CAPITAL_ALPHA				= Pattern.compile("^([\\sA-Z])*([\\sA-Z])+");
	private static final Pattern	CONTAINS_ALPHA_CHARS			= Pattern.compile("(?=.*[\\sA-Za-z]).*");
	private static final Pattern	CONTAINS_NUMERIC				= Pattern.compile("(?=.*[0-9]).*");
	// There is a digit, There is a letter, There are only letters and digits
	private static final Pattern	ONLY_ALPHA_NUMERIC				= Pattern.compile("(?=.*[0-9])" + "(?=.*[a-zA-Z])" + "[a-zA-Z0-9]*");
	private static final Pattern	ONLY_ALPHA_OR_NUMERIC			= Pattern.compile("^[a-zA-Z0-9]+");
	private static final Pattern	CONTAINS_ALPHA_OR_NUMERIC_CHARS	= Pattern.compile("(?=.*[A-Za-z0-9]).*");
	Pattern							CONTAINS_ALPHA_NUMERIC_CHARS	= Pattern.compile("(?=.*[A-Za-z]).*" + "(?=.*[0-9]).*");
	private static final Pattern	ONLY_SPECIAL_CHARS				= Pattern.compile("^([\\s@#$^&*%\\/\\\\] )*[\\s@#$^&*%\\/\\\\]+");
	private static final Pattern	CONTAINS_SPECIAL_CHARS			= Pattern.compile("(?=.*[@#$^&*%\\/\\\\]).*");
	private static final Pattern	CONTAINS_HTML_XML_CODE			= Pattern.compile(".*?<.*?>.*?</.*?>|.*?<.*?/>.*?|.*?<.*?>.*?");
	private static final Pattern	STARTS_WITH_SPECIAL_CHAR		= Pattern.compile("^([@#$^&*%\\/\\\\_;'!|+{}]).*");
	private static final Pattern	STARTS_WITH_NUMBER				= Pattern.compile("^([\\s0-9]).*[\\sA-Za-z0-9]+");
	private static final Pattern	ENDS_WITH_SPECIAL_CHAR			= Pattern.compile(".*([\\s@#$^&*%\\/\\\\_;'!|+{}])$");
	private static final Pattern	CONSECUTIVE_SPECIAL_CHARS		= Pattern.compile("(.*[@#$^&*%\\/\\\\][@#$^&*%\\/\\\\]).*");			// "(?=.*[@#$^&*%]{2,}).*"
	private static final Pattern	PASSWORD_CHARS					= Pattern.compile("((?=.*\\d)(?=.*[a-zA-Z]).{8,20})");					//- See more at: http://java2novice.com/java-collections-and-util/regex/valid-password/#sthash.v079xxwt.dpuf																		// //"(?=.*[\\s@#$*%][@#$*%]).*"

	/**
	 * Checks whether the text is AsciiPrintable.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean isAsciiPrintable(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			return StringUtils.isAsciiPrintable(inputText);
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text contains only alphabets.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean isOnlyAlphabets(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = ONLY_ALPHA_CHAR.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text is only numbers.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean isOnlyNumbers(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = ONLY_NUMBER.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text contains only alpha numeric.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean isOnlyAlphaNumeric(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = ONLY_ALPHA_NUMERIC.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text contains only alpha OR numeric.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean isOnlyAlphaORNumeric(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = ONLY_ALPHA_OR_NUMERIC.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text contains alpha OR numeric.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean containsAlphaORNumericChars(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = CONTAINS_ALPHA_OR_NUMERIC_CHARS.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text contains alpha numeric.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean containsAlphaNumericChars(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = CONTAINS_ALPHA_NUMERIC_CHARS.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text starts with number.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean startWithNumber(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = STARTS_WITH_NUMBER.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text contains special characters.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean containsSpecialChar(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = CONTAINS_SPECIAL_CHARS.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text starts with special character.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean startsWithSpecialChar(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = STARTS_WITH_SPECIAL_CHAR.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text ends with special character.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean endsWithSpecialChar(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = ENDS_WITH_SPECIAL_CHAR.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text has only special characters.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean onlySpecialChars(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = ONLY_SPECIAL_CHARS.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text contains only capital alphabets.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean onlyCapitalAlphabets(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = ONLY_CAPITAL_ALPHA.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text contains consecutive special characters.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean containsConsecutiveSpecialChars(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = CONSECUTIVE_SPECIAL_CHARS.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text has HTML or XML CODE.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean containsHtmlXmlCode(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = CONTAINS_HTML_XML_CODE.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text contains numeric.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean containsNumeric(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = CONTAINS_NUMERIC.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	/**
	 * Checks whether input text contains alpha chars.
	 * 
	 * @param inputText
	 * @return boolean result
	 */
	public boolean containsAlphaChar(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = CONTAINS_ALPHA_CHARS.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	public boolean validatePassword(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = PASSWORD_CHARS.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

}
