package demo.groovysvc.exceptions;

public class InvalidTaskCreationRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidTaskCreationRequestException(String message) {
		super(message);
	}
}