package demo.groovysvc;

public class TaskNotFoundException extends RuntimeException {

	public TaskNotFoundException(Long id) {
		super(String.format("Could not find task: %s", id));
	}
}
