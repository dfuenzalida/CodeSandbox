package codesandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import codesandbox.controllers.TaskController;
import codesandbox.dto.TaskRequest;
import codesandbox.dto.TaskResponse;
import codesandbox.dto.UserTasksResponse;
import codesandbox.entity.Task;
import codesandbox.entity.TaskState;
import codesandbox.entity.User;
import codesandbox.exceptions.ApiRequestError;
import codesandbox.repository.TaskRepository;
import codesandbox.repository.UserRepository;
import codesandbox.service.TaskService;
import codesandbox.service.TokenService;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CodeSandboxApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TaskService taskRunner;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private TaskService taskService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private TaskController taskController;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private ModelMapper modelMapper;

	@BeforeEach
	void beforeEach() throws Exception {
		taskRunner.setKillSwitch(true); // don't launch containers during tests
	}

	@Test
	void contextLoads() {
		assertThat(taskController).isNotNull();
	}

	@Test
	void defaultMappingShouldReturnIndexHtml() throws Exception {
		String defaultPage = this.restTemplate.getForObject("http://localhost:" + port + "/", String.class);
		assertThat(defaultPage).contains("<title>Groovy Service</title>");
	}

	@Test
	void retrievingTaskThroughEndpoint() throws Exception {
		User testUser = new User();
		testUser.setUsername("testuser");
		testUser = userRepository.findOne(Example.of(testUser)).get();

		Task task = new Task();
		task.setName(UUID.randomUUID().toString());
		task.setLang(UUID.randomUUID().toString());
		task.setCode(UUID.randomUUID().toString());

		Task createdTask = taskService.createAndRunTask(testUser, task);
		TaskRequest expected = modelMapper.map(createdTask, TaskRequest.class);

		// Retrieve and compare
		String token = tokenService.createTokenForUser("testuser");
		String authHeader = String.format("Bearer %s", token);
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		headers.add("Authorization", authHeader);

		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of(), headers);
		String url = String.format("http://localhost:%s/api/tasks/%s", port, createdTask.getId());
		ResponseEntity<TaskResponse> resposeTask = restTemplate.exchange(url, HttpMethod.GET, entity, TaskResponse.class);
		TaskResponse apiTask = resposeTask.getBody();
		TaskRequest actual = modelMapper.map(apiTask, TaskRequest.class);
		assertEquals(expected, actual);
	}

	@Test
	void retrievingAllTasksThroughEndpoint() throws Exception {
		User exampleUser = new User();
		exampleUser.setUsername("testuser");
		User testUser = userRepository.findOne(Example.of(exampleUser)).get();

		Integer randNumber = 10 + Math.abs(new Random().nextInt()) % 50;
		for (int i = 0; i < randNumber; i++) {
			Task task = new Task();
			task.setName(UUID.randomUUID().toString());
			task.setLang(UUID.randomUUID().toString());
			task.setCode(UUID.randomUUID().toString());
			taskRepository.saveAndFlush(task);
			testUser.getTasks().add(task);
		}
		userRepository.save(testUser);

		// Retrieve and compare
		String token = tokenService.createTokenForUser("testuser");
		String authHeader = String.format("Bearer %s", token);
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		headers.add("Authorization", authHeader);

		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of(), headers);
		String url = String.format("http://localhost:%s/api/tasks", port);
		ResponseEntity<UserTasksResponse> resposeList = restTemplate.exchange(url, HttpMethod.GET, entity, UserTasksResponse.class);
		UserTasksResponse userTasksResponse = resposeList.getBody();

		assertThat(userTasksResponse.getTasks().size()).isEqualTo(randNumber);
	}

	@Test
	void postingAndRetrievingTaskThroughEndpoint() throws Exception {
		Integer randomLangIndex = (int) new Random().nextFloat() * TaskService.validLangs.size();
		TaskRequest taskRequest = new TaskRequest();
		taskRequest.setName(UUID.randomUUID().toString());
		taskRequest.setLang(TaskService.validLangs.get(randomLangIndex));
		taskRequest.setCode(UUID.randomUUID().toString());

		// Post and compare
		String token = tokenService.createTokenForUser("testuser");
		String authHeader = String.format("Bearer %s", token);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		headers.add("Authorization", authHeader);
		HttpEntity<TaskRequest> entity = new HttpEntity<>(taskRequest, headers);

		TaskResponse taskResponse = this.restTemplate.postForObject(
				"http://localhost:" + port + "/api/tasks", entity, TaskResponse.class);
		assertEquals(taskRequest.getName(), taskResponse.getName());
		assertEquals(taskRequest.getCode(), taskResponse.getCode());
		assertEquals(TaskState.CREATED, taskResponse.getState());
		assertTrue(taskResponse.getId() != null);
	}

	@Test
	void postingInvalidLangThrows() throws Exception {
		TaskRequest taskRequest = new TaskRequest();
		taskRequest.setName(UUID.randomUUID().toString());
		taskRequest.setLang(UUID.randomUUID().toString()); // invalid lang
		taskRequest.setCode(UUID.randomUUID().toString());

		// Post and compare
		ApiRequestError error = postTaskRequest(taskRequest);
		assertTrue(error.getCause().contains("Invalid lang"));
	}

	@Test
	void postingEmptyCodeThrows() throws Exception {

		// Various null, empty or blank strings
		List<String> badTaskCodes = Arrays.asList(null, "", " ", "   \t \n \r");

		for (String badCode: badTaskCodes) {
			TaskRequest taskRequest = new TaskRequest();
			taskRequest.setName(UUID.randomUUID().toString());
			taskRequest.setLang(TaskService.validLangs.get(0));
			taskRequest.setCode(badCode);

			// Post and compare
			ApiRequestError result = postTaskRequest(taskRequest);
			assertTrue(result.getCause().contains("No code entered for this task request"));
		}
	}

	@Test
	void postingTaskWithTooMuchCodeThrows() throws Exception {
		StringBuilder longCode = new StringBuilder();
		while (longCode.length() <= 100_000) {
			longCode.append("println('1'); // unused payload\n");
		}

		TaskRequest taskRequest = new TaskRequest();
		taskRequest.setName(UUID.randomUUID().toString());
		taskRequest.setLang(TaskService.validLangs.get(0));
		taskRequest.setCode(longCode.toString());

		// Post and check for error
		ApiRequestError result = postTaskRequest(taskRequest);
		assertTrue(result.getCause().contains("Code contents are too big"));
	}

	@Test
	void retrievingInvalidTaskIdShouldThrow() throws Exception {
		Long largeRandomNum = (long) (Math.abs(Math.random()) * Long.MAX_VALUE);
		String error = getTaskByPath(String.format("/api/tasks/%s", largeRandomNum));
		assertTrue(error.contains("Could not find task"));
	}

	@Test
	void retrievingInvalidTaskNumberShouldThrow() throws Exception {
		String error = getTaskByPath("/api/tasks/dummy");
		assertTrue(error.contains("Bad Request"));
	}

	private ApiRequestError postTaskRequest(TaskRequest taskRequest) {
		String token = tokenService.createTokenForUser("testuser");
		String authHeader = String.format("Bearer %s", token);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		headers.add("Authorization", authHeader);
		HttpEntity<TaskRequest> entity = new HttpEntity<TaskRequest>(taskRequest, headers);

		ApiRequestError result = this.restTemplate.postForObject(
				"http://localhost:" + port + "/api/tasks", entity, ApiRequestError.class);

		return result;
	}


	private String getTaskByPath(String path) {
		String token = tokenService.createTokenForUser("testuser");
		String authHeader = String.format("Bearer %s", token);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		headers.add("Authorization", authHeader);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of(), headers);

		String url = String.format("http://localhost:%s/%s", port, path);
		// ApiRequestError
		ResponseEntity<String> responseError = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		String error = responseError.getBody();
		return error;
	}

}
