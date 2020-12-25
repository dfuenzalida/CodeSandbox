package demo.groovysvc.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import demo.groovysvc.model.Token;

public interface TokenRepository extends JpaRepository<Token, Long> {

}
