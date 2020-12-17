package demo.groovysvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GroovysvcApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TaskRunner taskRunner;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private TaskController taskController;

	@BeforeEach
	void beforeEach() throws Exception {
		taskRepository.deleteAll();
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
	void retrievingObjectThroughEndpoint() throws Exception {
		Task task = new Task();
		task.setName(UUID.randomUUID().toString());
		task.setLang(UUID.randomUUID().toString());
		task.setCode(UUID.randomUUID().toString());
		taskRepository.save(task);

		// Retrieve and compare
		Task apiTask = this.restTemplate.getForObject(
				"http://localhost:" + port + "/api/tasks/1", Task.class);
		assertEquals(task, apiTask);
	}

	@Test
	void retrievingAllTasksThroughEndpoint() throws Exception {
		Integer randNumber = 10 + Math.abs(new Random().nextInt()) % 50;
		for (int i = 0; i < randNumber; i++) {
			Task task = new Task();
			task.setName(UUID.randomUUID().toString());
			task.setLang(UUID.randomUUID().toString());
			task.setCode(UUID.randomUUID().toString());
			taskRepository.saveAndFlush(task);
		}
		// Retrieve and compare
		@SuppressWarnings({ "rawtypes" })
		List tasks = this.restTemplate.getForObject(
				"http://localhost:" + port + "/api/tasks", List.class);
		assertEquals(tasks.size(), randNumber);
	}

	@Test
	void postingAndRetrievingObjectThroughEndpoint() throws Exception {
		Integer randomLangIndex = (int) new Random().nextFloat() * TaskRunner.validLangs.size();
		Task task = new Task();
		task.setName(UUID.randomUUID().toString());
		task.setLang(TaskRunner.validLangs.get(randomLangIndex));
		task.setCode(UUID.randomUUID().toString());

		// Post and compare
		Task apiTask = this.restTemplate.postForObject(
				"http://localhost:" + port + "/api/tasks", task, Task.class);
		assertEquals(task.getName(), apiTask.getName());
		assertEquals(task.getCode(), apiTask.getCode());
		assertEquals(TaskState.CREATED, apiTask.getState());
		assertThat(apiTask.getId() != null);
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
			task.setLang(TaskRunner.validLangs.get(0));
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
		task.setLang(TaskRunner.validLangs.get(0));
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
