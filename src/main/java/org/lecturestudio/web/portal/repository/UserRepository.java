package org.lecturestudio.web.portal.repository;

import org.lecturestudio.web.portal.model.User;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, String> {

}
