package org.lecturestudio.web.portal.repository;

import org.lecturestudio.web.portal.model.CoursePrivilege;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoursePrivilegeRepository extends CrudRepository<CoursePrivilege, Long>{

    @Query(value = "SELECT p from CoursePrivilege p ORDER BY id ASC")
    public Iterable<CoursePrivilege> findAllOrderedByIdAsc();
    
}
