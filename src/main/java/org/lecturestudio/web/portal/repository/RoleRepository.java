package org.lecturestudio.web.portal.repository;

import java.util.Optional;

import org.lecturestudio.web.portal.model.Role;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends CrudRepository<Role, Long>{

    public Optional<Role> findByName(String name);

    public boolean existsByName(String name);

    @Query(value = "SELECT r FROM Role r ORDER BY id ASC")
    public Iterable<Role> findAllOrderedByIdAsc();

}
