package codesandbox.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import codesandbox.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {

}
