package org.lecturestudio.web.portal.service;

import java.util.Optional;

import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	@Autowired
	private UserRepository repository;


	public long getUserCount() {
		return repository.count();
	}

	public Optional<User> findById(String id) {
		return repository.findById(id);
	}

	public Iterable<User> getAllUsers() {
		return repository.findAll();
	}

	public User saveUser(User user) {
		return repository.save(user);
	}

	public void deleteUser(User user) {
		repository.delete(user);
	}
}
