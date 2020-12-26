package demo.groovysvc.dto;

import lombok.Data;

@Data
public class TokenRequest {

	private String username;
	private String password;
}
