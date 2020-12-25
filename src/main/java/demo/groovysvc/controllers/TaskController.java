package demo.groovysvc.controllers;

import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import demo.groovysvc.dao.TaskRepository;
import demo.groovysvc.exceptions.InvalidTaskCreationRequestException;
import demo.groovysvc.exceptions.TaskNotFoundException;
import demo.groovysvc.model.Task;
import demo.groovysvc.model.TaskState;
import demo.groovysvc.service.TaskService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class TaskController {

	private final TaskRepository repository;
	private final TaskService runner;

	@GetMapping("/api/tasks")
	List<Task> all() {
		return repository.findAll();
	}

	@PostMapping("/api/tasks")
	Task createTask(@RequestBody Task task) throws Exception {
		// Validate the task contents before submitting
		validateTask(task);

		task.setCreatedDate(new Date());
		task.setState(TaskState.CREATED);
		repository.save(task);
		return runner.runTask(task.getId());
	}

	@GetMapping("/api/tasks/{id}")
	Task getTask(@PathVariable Long id) {
		return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
	}

	private void validateTask(Task task) {

		// The task needs a valid language
		if (!TaskService.validLangs.contains(task.getLang())) {
			throw new InvalidTaskCreationRequestException(String.format("Invalid lang: %s", task.getLang()));
		}

		// The task requires a non-empty, not-blank code payload
		if (task.getCode() == null || task.getCode().isEmpty() || task.getCode().isBlank()) {
			throw new InvalidTaskCreationRequestException("No code entered for this task request");
		}

		// The code in the task should be less than 100_000 chars long
		if (task.getCode().length() > 100_000) {
			throw new InvalidTaskCreationRequestException("Code contents are too big");
		}
	}
}
