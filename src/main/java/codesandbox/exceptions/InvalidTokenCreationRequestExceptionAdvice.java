package codesandbox.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class InvalidTokenCreationRequestExceptionAdvice {

	@ResponseBody
	@ExceptionHandler(InvalidTokenCreationRequestException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ApiRequestError invalidTokenRequestExceptionHandler(InvalidTokenCreationRequestException ex) {
		return new ApiRequestError("Invalid token creation request", ex.getMessage());
	}
}
