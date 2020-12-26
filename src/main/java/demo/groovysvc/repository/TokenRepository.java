package demo.groovysvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import demo.groovysvc.entity.Token;

public interface TokenRepository extends JpaRepository<Token, Long> {

}
