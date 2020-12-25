package demo.groovysvc.exceptions;

public class TaskNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TaskNotFoundException(Long id) {
		super(String.format("Could not find task: %s", id));
	}
}
