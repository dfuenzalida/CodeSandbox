package demo.groovysvc;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class TaskController {

	private final TaskRepository repository;
	private final TaskRunner runner;

	@GetMapping("/api/tasks")
	List<Task> all() {
		return repository.findAll();
	}

	@PostMapping("/api/tasks")
	Task createTask(@RequestBody Task task) throws Exception {
		repository.save(task);
		Thread.sleep(5000L); // wait 5 seconds
		return runner.runTask(task.getId());
	}

	@GetMapping("/api/tasks/{id}")
	Task getTask(@PathVariable Long id) {
		return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
	}
}
