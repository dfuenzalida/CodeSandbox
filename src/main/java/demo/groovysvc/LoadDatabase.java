package demo.groovysvc;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoadDatabase {


	  private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

	  @Bean
	  CommandLineRunner initDatabase(TaskRepository repository) {
		  Task task1 = new Task();
		  task1.setId(1L);
		  task1.setLang("lang1");
		  task1.setCode("code1");

		  Task task2 = new Task();
		  task1.setId(2L);
		  task2.setLang("lang2");
		  task2.setCode("code2");
		  task2.setState(TaskState.RUNNING);
		  task2.setCreatedDate(new Date());

		  return args -> {
			  log.info("Preloading " + repository.save(task1));
			  log.info("Preloading " + repository.save(task2));
		  };
	  }
}
