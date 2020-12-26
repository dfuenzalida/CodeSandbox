package demo.groovysvc.controllers;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import demo.groovysvc.entity.Task;
import demo.groovysvc.entity.User;
import demo.groovysvc.exceptions.InvalidTaskRequestException;
import demo.groovysvc.exceptions.TaskNotFoundException;
import demo.groovysvc.service.TaskService;
import demo.groovysvc.service.TokenService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class TaskController {

	private static final Logger log = LoggerFactory.getLogger(TaskController.class);
	private final TaskService taskService;
	private final TokenService tokenService;

	@GetMapping("/api/tasks")
	Collection<Task> allUserTasks(@RequestHeader("Authorization") String requestHeader) {
		User user = tokenService.getUserByToken(requestHeader);
		log.debug(String.format("All tasks for User %s", user));
		return user.getTasks();
	}

	@PostMapping("/api/tasks")
	Task createTask(
			@RequestHeader("Authorization") String requestHeader,
			@RequestBody Task task) throws Exception {
		User user = tokenService.getUserByToken(requestHeader);
		log.debug(String.format("Validating new for User %s", user));

		// Validate the task contents before submitting
		validateTask(task);

		// Ready to create and run
		return taskService.createAndRunTask(user, task);
	}

	@GetMapping("/api/tasks/{id}")
	Task getTask(@RequestHeader("Authorization") String requestHeader, @PathVariable Long id) {
		User user = tokenService.getUserByToken(requestHeader);
		Task result = user.getTasks().stream()
				.filter(task -> task.getId() == id)
				.findFirst()
				.orElseThrow(() -> new TaskNotFoundException(id));

		return result;
	}

	private void validateTask(Task task) {

		// The task needs a valid language
		if (!TaskService.validLangs.contains(task.getLang())) {
			throw new InvalidTaskRequestException(String.format("Invalid lang: %s", task.getLang()));
		}

		// The task requires a non-empty, not-blank code payload
		if (task.getCode() == null || task.getCode().isEmpty() || task.getCode().isBlank()) {
			throw new InvalidTaskRequestException("No code entered for this task request");
		}

		// The code in the task should be less than 100_000 chars long
		if (task.getCode().length() > 100_000) {
			throw new InvalidTaskRequestException("Code contents are too big");
		}
	}
}
