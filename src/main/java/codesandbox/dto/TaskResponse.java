package codesandbox.dto;

import java.util.Date;

import codesandbox.entity.TaskState;
import lombok.Data;

@Data
public class TaskResponse {

	private Long id;
	private String name;
	private TaskState state;
	private String lang;

	// Command execution
	private String code;
	private String stdout;
	private String stderr;
	private Integer exitCode;

	// Timestamps
	private Date created;
	private Date started;
	private Date end;
}
