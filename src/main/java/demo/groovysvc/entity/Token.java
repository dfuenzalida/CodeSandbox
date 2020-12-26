package demo.groovysvc.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Data;

@Data
@Entity
public class Token {
	private @Id @GeneratedValue Long id;
	private String token;

	@ManyToOne
	private User user;
}
