package demo.groovysvc;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Task {

	private @Id @GeneratedValue Long id;
	private String name;
	private TaskState state;
	private String lang;

	// Command execution
	private String code;
	private String stdout;
	private String stderr;
	private Integer exitCode;

	// Timestamps
	private Date createdDate;
	private Date startedDate;
	private Date endDate;
}
