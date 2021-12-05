package org.lecturestudio.web.portal.repository;

import org.lecturestudio.web.portal.model.CourseFeature;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseFeatureRepository extends CrudRepository<CourseFeature, Long> {

}
