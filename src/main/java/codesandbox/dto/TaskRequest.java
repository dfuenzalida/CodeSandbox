package codesandbox.dto;

import lombok.Data;

@Data
public class TaskRequest {
	private String name;
	private String lang;
	private String code;
}
