package codesandbox.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import codesandbox.entity.Task;
import codesandbox.entity.TaskState;
import codesandbox.entity.User;
import codesandbox.repository.TaskRepository;
import codesandbox.repository.UserRepository;
import lombok.Getter;
import lombok.Setter;

@Component
public class TaskService {

	private static final Logger log = LoggerFactory.getLogger(TaskService.class);
	private ExecutorService executorService;
	private String timeoutSecs;

	@Getter @Setter
	private boolean killSwitch = false;

	public static List<String> validLangs;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private UserRepository userRepository;

	public TaskService(
			@Value("${codeSandbox.threadPoolSize}") Integer threadPoolSizeProp,
			@Value("${codeSandbox.validLangs}") String validLangsProp,
			@Value("${codeSandbox.timeoutSecs}") String timeoutSecsProp) {

		log.info(String.format("Created fixed pool of %s executors for tasks", threadPoolSizeProp));
		executorService = Executors.newFixedThreadPool(threadPoolSizeProp);

		log.info(String.format("List of valid languages for tasks: %s", validLangs));
		validLangs = Arrays.asList(validLangsProp.split(","));

		log.info(String.format("Setting timeout for tasks to: %s", validLangs));
		timeoutSecs = timeoutSecsProp;
	}

	public Task createAndRunTask(User user, Task task) {
		task.setCreatedDate(new Date());
		task.setState(TaskState.CREATED);
		taskRepository.saveAndFlush(task);

		Set<Task> userTasks = user.getTasks();
		userTasks.add(task);
		userRepository.saveAndFlush(user);

		return runTask(task.getId());
	}

	private Task runTask(Long taskId) {
		Task task = taskRepository.findById(taskId).orElseThrow();

		// Create a the task as a runnable
		Runnable runnableTask = createRunnableTask(task);

		// Submit a new task to the pool
		executorService.submit(runnableTask);

		return task;
	}

	String readAsString(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		List<String> lines = reader.lines().collect(Collectors.toList());
		return String.join("\n", lines);
	}

	Runnable createRunnableTask(Task task) {
		return new Runnable() {

			@Override
			public void run() {
				try {
					// kill switch
					if (killSwitch) return;

					// Save the code in disk for the container to use
					writeScriptForTask(task);

					task.setStartedDate(new Date());
					task.setState(TaskState.RUNNING);
					taskRepository.save(task);

					// Create one directory per task and mount /tmp/codeSandbox/<taskId> as /codeSandbox
					// so that one script can't read other script files by opening ("../something/1234.tmp")
					String volume = String.format("%s:/groovyScripts:ro", directoryForTask(task));

					// View of the script in the container volume
					String script = "/groovyScripts/script.groovy";

					// Run the script in the container
					ProcessBuilder processBuilder = new ProcessBuilder();

					// Using docker volumes and workdir to run a script in the same disk as the host
					processBuilder.command(
							"docker", "run",
							"--rm",
							"--network", "host",
							// "-m", "256M",  // 256 MB memory limit
							"-v", volume,
							"-w", "/groovyScripts",
							"groovy", // container name
							"timeout", timeoutSecs, // call `/usr/bin/timeout` with a timeout
							task.getLang(), // executable name
							script);

					log.info(String.format("Starting process for task #%s with timeout of %s seconds", task.getId(), timeoutSecs));
					Process process = processBuilder.start();
					int exitCode = process.waitFor();

					log.info(String.format("Process for task #%s finished", task.getId()));
					task.setExitCode(exitCode);
					task.setStderr(readAsString(process.getErrorStream()));
					task.setStdout(readAsString(process.getInputStream()));

				} catch (IOException | InterruptedException e) {
					log.error(String.format("Exception when running task #%s:", e.getMessage()));
				}

				// Cleanup
				deleteScriptForTask(task);

				// update the task status
				task.setState(TaskState.COMPLETE);
				task.setEndDate(new Date());
				taskRepository.save(task);
			}
		};
	}

	String directoryForTask(Task task) {
		return String.format("/tmp/codeSandbox/%s", task.getId());
	}

	void writeScriptForTask(Task task) {
		try {
			// Create directory
			String directory = directoryForTask(task);
			log.info(String.format("Creating directory for task #%s on %s", task.getId(), directory));
			Path tempFolder = Paths.get(directory);
			Files.createDirectories(tempFolder);

			// Write script
			log.info(String.format("Writing script for task #%s", task.getId(), directory));
			Path tempFile = Paths.get(String.format("%s/script.groovy", directory));
			Files.write(tempFile, task.getCode().getBytes(StandardCharsets.UTF_8));
		} catch (IOException ex) {
			log.error(ex.getMessage());
		}
	}

	void deleteScriptForTask(Task task) {
		try {
			// Delete file and directory
			String directory = directoryForTask(task);
			Path directoryPath = Paths.get(directory);
			Path tempFile = Paths.get(String.format("%s/script.groovy", directory));

			log.info(String.format("Deleting script for task #%s", task.getId(), directory));
			Files.delete(tempFile);

			log.info(String.format("Deleting directory for task #%s", task.getId(), directory));
			Files.delete(directoryPath);
		} catch (IOException ex) {
			log.error(ex.getMessage());
		}
	}
}
