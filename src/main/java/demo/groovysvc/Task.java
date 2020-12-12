package demo.groovysvc;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Task {

	private @Id @GeneratedValue Long id;
	private String lang;
	private String code;
}
