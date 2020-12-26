package demo.groovysvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import demo.groovysvc.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
