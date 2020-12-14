package demo.groovysvc;

public class InvalidTaskRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidTaskRequestException(String lang) {
		super(String.format("Invalid lang: %s", lang));
	}
}
