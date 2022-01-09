package com.realitica.parser.repository;

import com.realitica.parser.entity.AdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "ads")
public interface AdRepository extends JpaRepository<AdEntity, Long> {
    AdEntity findByRealiticaId(String realiticaId);
}