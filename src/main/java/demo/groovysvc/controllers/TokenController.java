package demo.groovysvc.controllers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import demo.groovysvc.dao.TokenRepository;
import demo.groovysvc.dao.UserRepository;
import demo.groovysvc.exceptions.InvalidTokenCreationRequestException;
import demo.groovysvc.model.Token;
import demo.groovysvc.model.TokenRequest;
import demo.groovysvc.model.TokenResponse;
import demo.groovysvc.model.User;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class TokenController {

	private static final Logger log = LoggerFactory.getLogger(TokenController.class);
	private final TokenRepository tokenRepository;
	private final UserRepository userRepository;

	@PostMapping("/api/tokens")
	TokenResponse createToken(@RequestBody TokenRequest tokenRequest) throws Exception {
		log.info("Token request: " + tokenRequest);

		Example<User> userExample = Example.of(new User(null, tokenRequest.getUsername(), null));
		User user = userRepository
				.findOne(userExample)
				.orElseThrow(() -> new InvalidTokenCreationRequestException(
					String.format("Invalid user: %s", tokenRequest.getUsername())));

		log.info("User found:" + user);

		Token token = new Token();
		token.setToken(UUID.randomUUID().toString());
		token.setUser(user);
		tokenRepository.save(token);

		return new TokenResponse(token.getToken());
	}
}
