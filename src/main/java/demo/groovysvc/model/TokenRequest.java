package demo.groovysvc.model;

import lombok.Data;

@Data
public class TokenRequest {

	private String username;
	private String password;
}
