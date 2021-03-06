package codesandbox.controllers;

import java.util.List;
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

import codesandbox.dto.TaskRequest;
import codesandbox.dto.TaskResponse;
import codesandbox.dto.UserTasksResponse;
import codesandbox.entity.Task;
import codesandbox.entity.User;
import codesandbox.exceptions.InvalidTaskRequestException;
import codesandbox.exceptions.TaskNotFoundException;
import codesandbox.service.TaskService;
import codesandbox.service.TokenService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class TaskController {

	private static final Logger log = LoggerFactory.getLogger(TaskController.class);
	private final TaskService taskService;
	private final TokenService tokenService;
    private final ModelMapper modelMapper;

    @GetMapping("/api/tasks")
	UserTasksResponse allUserTasks(@RequestHeader("Authorization") String requestHeader) {
		User user = tokenService.getUserByToken(requestHeader);
		log.debug(String.format("All tasks for User %s", user));
		List<TaskResponse> userTasks =  user.getTasks().stream().map(this::toTaskResponse).collect(Collectors.toList());
		UserTasksResponse userTasksResponse = new UserTasksResponse();
		userTasksResponse.setTasks(userTasks);
		return userTasksResponse;
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
		return toTaskResponse(task);
	}

	@GetMapping("/api/tasks/{id}")
	TaskResponse getTask(@RequestHeader("Authorization") String requestHeader, @PathVariable Long id) {
		User user = tokenService.getUserByToken(requestHeader);
		Task result = user.getTasks().stream()
				.filter(task -> task.getId() == id)
				.findFirst()
				.orElseThrow(() -> new TaskNotFoundException(id));

		return toTaskResponse(result);
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

	private TaskResponse toTaskResponse(Task task) {
		return modelMapper.map(task, TaskResponse.class);
	}
}
