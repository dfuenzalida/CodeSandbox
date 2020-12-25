package demo.groovysvc.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import demo.groovysvc.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
