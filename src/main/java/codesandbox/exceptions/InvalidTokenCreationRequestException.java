package codesandbox.exceptions;

public class InvalidTokenCreationRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidTokenCreationRequestException(String message) {
		super(message);
	}
}
