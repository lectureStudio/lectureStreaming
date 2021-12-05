package org.lecturestudio.web.portal.service;

import java.util.Optional;

import org.lecturestudio.web.portal.model.PersonalToken;
import org.lecturestudio.web.portal.repository.PersonalTokenRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonalTokenService {

	@Autowired
	private PersonalTokenRepository repository;


	public Optional<PersonalToken> findByToken(String token) {
		return repository.findByToken(token);
	}

	public PersonalToken saveToken(PersonalToken token) {
		return repository.save(token);
	}

	public void deleteToken(PersonalToken token) {
		repository.deleteById(token.getId());
	}
}
