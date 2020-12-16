package demo.groovysvc;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class InvalidTaskCreationRequestExceptionAdvice {

	@ResponseBody
	@ExceptionHandler(InvalidTaskCreationRequestException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	String invalidTaskRequestExceptionHandler(InvalidTaskCreationRequestException ex) {
		return ex.getMessage();
	}
}
