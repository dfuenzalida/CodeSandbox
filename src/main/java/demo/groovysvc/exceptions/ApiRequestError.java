package demo.groovysvc.exceptions;

import lombok.Value;

@Value
public class ApiRequestError {

	private final String error;
	private final String cause;
}
