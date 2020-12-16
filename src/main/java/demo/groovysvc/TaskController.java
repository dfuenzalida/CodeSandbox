package demo.groovysvc;

import java.util.Date;
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
		if (!TaskRunner.validLangs.contains(task.getLang())) {
			throw new InvalidTaskCreationRequestException(String.format("Invalid lang: %s", task.getLang()));
		} else if (task.getCode() == null || task.getCode().isEmpty() || task.getCode().isBlank()) {
			throw new InvalidTaskCreationRequestException("No code entered for this task request");
		}

		task.setCreatedDate(new Date());
		task.setState(TaskState.CREATED);
		repository.save(task);
		return runner.runTask(task.getId());
	}

	@GetMapping("/api/tasks/{id}")
	Task getTask(@PathVariable Long id) {
		return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
	}
}
