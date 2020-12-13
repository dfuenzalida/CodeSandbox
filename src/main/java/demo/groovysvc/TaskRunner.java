package demo.groovysvc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskRunner {

	private static final Logger log = LoggerFactory.getLogger(TaskRunner.class);
	private ExecutorService executorService;

	@Autowired
	private TaskRepository repository;

	public TaskRunner() {
		executorService = Executors.newFixedThreadPool(10);
		try {
			Path tempFolder = Paths.get("/tmp/groovyService/");
			Files.createDirectories(tempFolder);
		} catch (IOException ex) {
			log.error(ex.getMessage());
		}
	}

	public Task runTask(Long taskId) {
		Task task = repository.findById(taskId).orElseThrow();

		// Create a the task as a runnable
		Runnable runnableTask = createRunnableTask(task);

		// Submit a new task to the pool
		executorService.submit(runnableTask);

		return task;
	}

	String readToString(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder builder = new StringBuilder();
		String eol = "";
		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(eol);
			builder.append(line);
			eol = "\n";
		}
		return builder.toString();
	}

	Runnable createRunnableTask(Task task) {
		return new Runnable() {

			@Override
			public void run() {
				try {
					// TODO add logging
					Path tempFile = Paths.get(String.format("/tmp/groovyService/%s.tmp", task.getId()));
					Files.write(tempFile, task.getCode().getBytes(StandardCharsets.UTF_8));

					task.setStartedDate(new Date());
					task.setState(TaskState.RUNNING);
					repository.save(task);

					// View of the script in the container volume
					String script = String.format("/groovyScripts/%s.tmp", task.getId());

					// TODO Create one directory per task and mount /tmp/groovyService/<taskId> as /groovyService
					// so that one script can't read other script files by opening ("../something/1234.tmp")

					// Run the script in the container
					ProcessBuilder processBuilder = new ProcessBuilder();

					// TODO add parameters to set execution limits on the container: memory/disk/cpu/time
					// Using docker volumes and workdir to run a script in the same disk as the host
					processBuilder.command(
							"docker", "run",
							"--network", "host",
							"-v", "/tmp/groovyService:/groovyScripts", // mount tmp folder as /home/groovy/scripts
							"-w", "/groovyScripts",
							task.getLang(), // container name
							task.getLang(), // executable name
							script);

					Process process = processBuilder.start();
					int exitCode = process.waitFor();

					task.setExitCode(exitCode);
					task.setStderr(readToString(process.getErrorStream()));
					task.setStdout(readToString(process.getInputStream()));

				} catch (IOException | InterruptedException e) {
					log.error(e.getMessage());
				}

				// TODO remove temporary files and folders

				task.setState(TaskState.COMPLETE);
				task.setEndDate(new Date());
				repository.save(task);
			}
		};
	}
}
