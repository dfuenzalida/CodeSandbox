package demo.groovysvc.configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import demo.groovysvc.entity.User;
import demo.groovysvc.repository.UserRepository;

@Configuration
public class LoadDatabase {

	private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

	@Bean
	CommandLineRunner initDatabase(UserRepository userRepository) {
		return args -> {
			List<String> demoUsers = Arrays.asList("denis", "webuser", "demo", "testuser");
			for (String username: demoUsers) {
				User user = new User(null, username, new HashSet<>());
				user = userRepository.saveAndFlush(user);
				log.info("Preloading " + userRepository.save(user));
			}

		};
	}
}
