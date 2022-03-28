package core.exceptions;

import core.utils.Enums.ErrorCode;

public class BadRequestException extends ApplicationException {
	private static final long	serialVersionUID	= 1L;

	public BadRequestException(ErrorCode code, String message) {
		super(code, message);
	}

	public BadRequestException(ErrorCode code, String message, Exception ex) {
		super(code, message, ex);
	}

	public BadRequestException(ErrorCode code, Object... objects) {
		super(code, objects);
	}
}
