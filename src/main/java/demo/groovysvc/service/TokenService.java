package demo.groovysvc.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import demo.groovysvc.entity.Token;
import demo.groovysvc.entity.User;
import demo.groovysvc.exceptions.InvalidTaskRequestException;
import demo.groovysvc.exceptions.InvalidTokenCreationRequestException;
import demo.groovysvc.repository.TokenRepository;
import demo.groovysvc.repository.UserRepository;

@Service
public class TokenService {

	private static final Logger log = LoggerFactory.getLogger(TokenService.class);

	@Autowired
	private TokenRepository tokenRepository;

	@Autowired
	private UserRepository userRepository;

	public String createTokenForUser(String username) throws InvalidTokenCreationRequestException {
		Example<User> userExample = Example.of(new User(null, username, null));
		User user = userRepository
				.findOne(userExample)
				.orElseThrow(() -> new InvalidTokenCreationRequestException(
					String.format("Invalid user: %s", username)));

		Token token = new Token();
		token.setToken(UUID.randomUUID().toString());
		token.setUser(user);
		tokenRepository.save(token);

		return token.getToken();
	}

	public User getUserByToken(String tokenHeader) throws InvalidTaskRequestException {
		if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
			throw new InvalidTaskRequestException("Auth token missing");
		}

		String token = tokenHeader.substring("Bearer ".length());
		log.info("Actual token: " + token);

		Token exampleToken = new Token();
		exampleToken.setToken(token);
		Token tokenInstance = tokenRepository
			.findOne(Example.of(exampleToken))
			.orElseThrow(() -> new InvalidTaskRequestException("Invalid token"));

		return tokenInstance.getUser();
	}
}
