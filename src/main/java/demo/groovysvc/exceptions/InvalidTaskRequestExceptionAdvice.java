package demo.groovysvc.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class InvalidTaskRequestExceptionAdvice {

	@ResponseBody
	@ExceptionHandler(InvalidTaskRequestException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ApiRequestError invalidTaskRequestExceptionHandler(InvalidTaskRequestException ex) {
		return new ApiRequestError("Bad request", ex.getMessage());
	}
}
