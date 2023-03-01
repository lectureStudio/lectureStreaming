package org.lecturestudio.web.portal.admin.repository;

import org.lecturestudio.web.portal.admin.model.CourseBroadcast;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseBraodcastRepository extends CrudRepository<CourseBroadcast, Long> {
}
