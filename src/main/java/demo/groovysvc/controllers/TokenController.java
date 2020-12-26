package demo.groovysvc.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import demo.groovysvc.dto.TokenRequest;
import demo.groovysvc.dto.TokenResponse;
import demo.groovysvc.service.TokenService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class TokenController {

	private static final Logger log = LoggerFactory.getLogger(TokenController.class);
	private final TokenService tokenService;

	@PostMapping("/api/tokens")
	TokenResponse createToken(@RequestBody TokenRequest tokenRequest) throws Exception {
		log.info("Token request: " + tokenRequest);
		String token = tokenService.createTokenForUser(tokenRequest.getUsername());
		return new TokenResponse(token);
	}
}
