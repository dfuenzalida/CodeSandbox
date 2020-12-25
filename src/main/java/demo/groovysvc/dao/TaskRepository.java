package demo.groovysvc.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import demo.groovysvc.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {

}
