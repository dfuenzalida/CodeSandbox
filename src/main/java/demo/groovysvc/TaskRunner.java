package demo.groovysvc;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskRunner {

	@Autowired
	private TaskRepository repository;

	public Task runTask(Long taskId) {
		Task task = repository.findById(taskId).orElseThrow();
		task.setState(TaskState.COMPLETE);
		task.setEndDate(new Date());
		repository.save(task);
		return task;
	}
}
