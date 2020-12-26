package demo.groovysvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import demo.groovysvc.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {

}
