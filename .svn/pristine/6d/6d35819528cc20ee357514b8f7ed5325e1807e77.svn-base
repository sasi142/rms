package core.exceptions;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import core.utils.Enums.ErrorCode;

@SuppressWarnings("serial")
public abstract class ApplicationException extends RuntimeException {

	final static Logger logger = LoggerFactory.getLogger(ApplicationException.class);

	protected ErrorCode code;
	protected Integer type;
	protected String message;
	protected Exception ex;

	public ApplicationException(ErrorCode code, String message) {
		super(message);
		this.code = code;
		this.message = message;
	}

	public ApplicationException(ErrorCode code, String message, Exception ex) {
		super(message, ex);
		this.code = code;
		this.message = message;
		this.ex = ex;
	}

	public ApplicationException(ErrorCode code, Object... objects) {
		this.code = code;
		this.message = code.getName();
		String paramVal = "";
		if (objects != null && objects.length > 0) {
			for (int cnt = 0; cnt < objects.length; cnt++) {
				Object object = objects[cnt];
				if (object != null && object instanceof Exception) {
					ex = (Exception) object;
				} else if (object == null) {
					paramVal = "null";
					message = message.replace("{" + cnt + "}", paramVal);
				} else {
					paramVal = object.toString();
					message = message.replace("{" + cnt + "}", paramVal);
				}
			}
		}
		if (ex != null) {
			logger.error(message, ex);
		} else {
			logger.error(message);
		}
	}

	public ErrorCode getCode() {
		return code;
	}

	public void setCode(ErrorCode code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Exception getEx() {
		return ex;
	}

	public void setEx(Exception ex) {
		this.ex = ex;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}
}
