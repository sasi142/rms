package core.exceptions;
import core.utils.Enums.ErrorCode;
public class InternalServerErrorException extends ApplicationException {
	private static final long	serialVersionUID	= 1L;

	public InternalServerErrorException(ErrorCode code, String message) {
		super(code, message);
	}

	public InternalServerErrorException(ErrorCode code, String message, Exception ex) {
		super(code, message, ex);
	}

}
