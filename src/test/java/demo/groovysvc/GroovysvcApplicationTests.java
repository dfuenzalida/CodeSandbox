package demo.groovysvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

import demo.groovysvc.controllers.TaskController;
import demo.groovysvc.dto.TaskRequest;
import demo.groovysvc.dto.TaskResponse;
import demo.groovysvc.entity.Task;
import demo.groovysvc.entity.TaskState;
import demo.groovysvc.entity.User;
import demo.groovysvc.repository.TaskRepository;
import demo.groovysvc.repository.UserRepository;
import demo.groovysvc.service.TaskService;
import demo.groovysvc.service.TokenService;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GroovysvcApplicationTests {

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
		assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/", String.class))
			.contains("<title>Groovy Service</title>");
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
		ResponseEntity<TaskResponse> resposeTask = restTemplate.exchange(
				"http://localhost:" + port + "/api/tasks/" + createdTask.getId(), HttpMethod.GET, entity, TaskResponse.class);
		TaskResponse apiTask = resposeTask.getBody();
		TaskRequest actual = modelMapper.map(apiTask, TaskRequest.class);
		assertEquals(expected, actual);
	}

	@Test
	@SuppressWarnings("rawtypes")
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
		ResponseEntity<List> resposeList = restTemplate.exchange(
				"http://localhost:" + port + "/api/tasks", HttpMethod.GET, entity, List.class);
		List tasks = resposeList.getBody();

		assertEquals(tasks.size(), randNumber);
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
		assertThat(taskResponse.getId() != null);
	}

	@Test
	void postingInvalidLangThrows() throws Exception {
		Task task = new Task();
		task.setName(UUID.randomUUID().toString());
		task.setLang(UUID.randomUUID().toString()); // invalid lang
		task.setCode(UUID.randomUUID().toString());

		// Post and compare
		String result = this.restTemplate.postForObject(
				"http://localhost:" + port + "/api/tasks", task, String.class);
		assertThat(result.contains("Invalid lang"));
	}

	@Test
	void postingEmptyCodeThrows() throws Exception {

		// Various null, empty or blank strings
		List<String> badTaskCodes = Arrays.asList(null, "", " ", "   \t \n \r");

		for (String badCode: badTaskCodes) {
			Task task = new Task();
			task.setName(UUID.randomUUID().toString());
			task.setLang(TaskService.validLangs.get(0));
			task.setCode(badCode);

			// Post and compare
			String result = this.restTemplate.postForObject(
					"http://localhost:" + port + "/api/tasks", task, String.class);
			assertThat(result.contains("No code entered for this task request"));
		}
	}

	@Test
	void postingTaskWithTooMuchCodeThrows() throws Exception {
		StringBuilder longCode = new StringBuilder();
		while (longCode.length() < 100_000) {
			longCode.append("println('1'); // unused payload\n");
		}

		Task task = new Task();
		task.setName(UUID.randomUUID().toString());
		task.setLang(TaskService.validLangs.get(0));
		task.setCode(longCode.toString());

		// Post and check for error
		String result = this.restTemplate.postForObject(
				"http://localhost:" + port + "/api/tasks", task, String.class);
		assertThat(result.contains("Code contents are too big"));
	}

	@Test
	void retrievingInvalidTaskIdShouldThrow() throws Exception {
		String result = this.restTemplate.getForObject("http://localhost:" + port + "/api/tasks/-1", String.class);
		assertThat(result.contains("Could not find task"));
	}

	@Test
	void retrievingInvalidTaskNumberShouldThrow() throws Exception {
		String result = this.restTemplate.getForObject("http://localhost:" + port + "/api/tasks/dummy", String.class);
		assertThat(result.contains("NumberFormatException"));
	}
}
