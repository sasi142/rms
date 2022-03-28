package core.exceptions;

import core.utils.Enums.ErrorCode;

public class UnAuthorizedException extends ApplicationException {
	private static final long	serialVersionUID	= 1L;

	public UnAuthorizedException(ErrorCode code, String message) {
		super(code, message);
	}

	public UnAuthorizedException(ErrorCode code, String message, Exception ex) {
		super(code, message, ex);
	}

	public UnAuthorizedException(ErrorCode code, Object... objects) {
		super(code, objects);
	}
}
