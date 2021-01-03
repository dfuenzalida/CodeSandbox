package demo.groovysvc.dto;

import java.util.List;

import lombok.Data;

@Data
public class UserTasksResponse {

	private List<TaskResponse> tasks;
}
