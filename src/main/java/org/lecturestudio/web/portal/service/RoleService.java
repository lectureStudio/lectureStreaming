package org.lecturestudio.web.portal.service;

import org.lecturestudio.web.portal.model.Role;
import org.lecturestudio.web.portal.repository.RoleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

	@Autowired
	private RoleRepository repository;


	public Role findByName(String name) {
		return repository.findByName(name);
	}
}
