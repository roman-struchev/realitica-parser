package com.realitica.parser.repo;

import com.realitica.parser.entity.Stun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "stuns")
public interface StunRepository extends JpaRepository<Stun, Long> {
    Stun findByRealiticaId(String realiticaId);
}
