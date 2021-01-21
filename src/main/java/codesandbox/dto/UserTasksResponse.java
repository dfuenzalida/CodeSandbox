package codesandbox.dto;

import java.util.List;

import lombok.Data;

@Data
public class UserTasksResponse {

	private List<TaskResponse> tasks;
}
