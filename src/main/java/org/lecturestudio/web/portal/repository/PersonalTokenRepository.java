package org.lecturestudio.web.portal.repository;

import java.util.Optional;

import org.lecturestudio.web.portal.model.PersonalToken;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonalTokenRepository extends CrudRepository<PersonalToken, Long> {

	Optional<PersonalToken> findByToken(String token);

}
