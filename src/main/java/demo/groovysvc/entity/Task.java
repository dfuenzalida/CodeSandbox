package demo.groovysvc.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import lombok.Data;

@Data
@Entity
public class Task {

	private @Id @GeneratedValue Long id;
	private String name;
	private TaskState state;
	private String lang;

	// Command execution
	private @Lob String code;
	private @Lob String stdout;
	private @Lob String stderr;
	private Integer exitCode;

	// Timestamps
	private Date createdDate;
	private Date startedDate;
	private Date endDate;
}
