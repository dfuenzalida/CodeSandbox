package codesandbox.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import codesandbox.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
