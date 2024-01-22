package com.realitica.parser.repository;

import com.realitica.parser.entity.AdEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Date;
import java.util.List;

@RepositoryRestResource(path = "ads")
public interface AdRepository extends JpaRepository<AdEntity, Long> {
    AdEntity findByRealiticaId(String realiticaId);

    List<AdEntity> findAllByTypeContainsIgnoreCase(String type, Sort sort);

    List<AdEntity> findAllByLastModifiedIsAfter(Date lastModifiedFrom, Sort sort);
}