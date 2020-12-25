package demo.groovysvc.exceptions;

public class InvalidTaskRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidTaskRequestException(String message) {
		super(message);
	}
}
