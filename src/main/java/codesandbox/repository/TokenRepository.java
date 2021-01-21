package codesandbox.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import codesandbox.entity.Token;

public interface TokenRepository extends JpaRepository<Token, Long> {

}
