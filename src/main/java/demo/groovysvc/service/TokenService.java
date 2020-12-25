package demo.groovysvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import demo.groovysvc.dao.TokenRepository;
import demo.groovysvc.exceptions.InvalidTaskRequestException;
import demo.groovysvc.model.Token;
import demo.groovysvc.model.User;

@Service
public class TokenService {

	private static final Logger log = LoggerFactory.getLogger(TokenService.class);

	@Autowired
	private TokenRepository tokenRepository;

	public User getUserByTokenOrThrow(String tokenHeader) {
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

		log.info("Token instance: " + tokenInstance);
		// log.info("User in Token instance: " + tokenInstance.getUser());
		return tokenInstance.getUser();
	}
}
