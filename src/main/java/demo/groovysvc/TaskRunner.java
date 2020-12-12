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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskRunner {

	private static final Logger log = LoggerFactory.getLogger(TaskRunner.class);

	@Autowired
	private TaskRepository repository;

	public TaskRunner() {
		try {
			Path tempFolder = Paths.get("/tmp/groovyService/");
			Files.createDirectories(tempFolder);
		} catch (IOException ex) {
			log.error(ex.getMessage());
		}
	}

	public Task runTask(Long taskId) {
		Task task = repository.findById(taskId).orElseThrow();

		try {
			Path tempFile = Paths.get(String.format("/tmp/groovyService/%s.tmp", taskId));
			Files.write(tempFile, task.getCode().getBytes(StandardCharsets.UTF_8));

			// Run in container
			ProcessBuilder processBuilder = new ProcessBuilder();
			task.setStartedDate(new Date());

			// View of the script in the container volume
			String script = String.format("/home/groovy/scripts/%s.tmp", taskId);

			// Using docker volumes and workdir to run a script in the same disk as the host
			processBuilder.command("docker", "run", "--network", "host", "-v",
					"/tmp/groovyService:/home/groovy/scripts", "-w",
					"/home/groovy/scripts", task.getLang(), task.getLang(), script);

			Process process = processBuilder.start();
			int exitCode = process.waitFor();

			task.setExitCode(exitCode);
			task.setStderr(readToString(process.getErrorStream()));
			task.setStdout(readToString(process.getInputStream()));

		} catch (IOException | InterruptedException e) {
			log.error(e.getMessage());
		}

		task.setState(TaskState.COMPLETE);
		task.setEndDate(new Date());
		repository.save(task);
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
}
