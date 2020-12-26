package demo.groovysvc.controllers;

import java.util.Collection;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import demo.groovysvc.dto.TaskRequest;
import demo.groovysvc.dto.TaskResponse;
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
    private final ModelMapper modelMapper;

    @GetMapping("/api/tasks")
	Collection<TaskResponse> allUserTasks(@RequestHeader("Authorization") String requestHeader) {
		User user = tokenService.getUserByToken(requestHeader);
		log.debug(String.format("All tasks for User %s", user));
		return user.getTasks().stream()
				.map(this::asTaskResponse)
				.collect(Collectors.toList());
	}

	@PostMapping("/api/tasks")
	TaskResponse createTask(
			@RequestHeader("Authorization") String requestHeader,
			@RequestBody TaskRequest taskRequest) throws Exception {
		User user = tokenService.getUserByToken(requestHeader);
		log.debug(String.format("Validating new for User %s", user));

		// Validate the task contents before submitting
		validateTask(taskRequest);

		// Ready to create and run
		Task task = modelMapper.map(taskRequest, Task.class);
		taskService.createAndRunTask(user, task);
		return asTaskResponse(task);
	}

	@GetMapping("/api/tasks/{id}")
	TaskResponse getTask(@RequestHeader("Authorization") String requestHeader, @PathVariable Long id) {
		User user = tokenService.getUserByToken(requestHeader);
		Task result = user.getTasks().stream()
				.filter(task -> task.getId() == id)
				.findFirst()
				.orElseThrow(() -> new TaskNotFoundException(id));

		return asTaskResponse(result);
	}

	private void validateTask(TaskRequest taskRequest) {

		// The task needs a valid language
		if (!TaskService.validLangs.contains(taskRequest.getLang())) {
			throw new InvalidTaskRequestException(String.format("Invalid lang: %s", taskRequest.getLang()));
		}

		// The task requires a non-empty, not-blank code payload
		if (taskRequest.getCode() == null || taskRequest.getCode().isEmpty() || taskRequest.getCode().isBlank()) {
			throw new InvalidTaskRequestException("No code entered for this task request");
		}

		// The code in the task should be less than 100_000 chars long
		if (taskRequest.getCode().length() > 100_000) {
			throw new InvalidTaskRequestException("Code contents are too big");
		}
	}

	private TaskResponse asTaskResponse(Task task) {
		return modelMapper.map(task, TaskResponse.class);
	}
}
