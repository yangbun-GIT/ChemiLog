package com.chemilog.main.repository;

import com.chemilog.main.domain.food.Additive;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdditiveRepository extends JpaRepository<Additive, Long> {

    List<Additive> findAllByAdditiveIdIn(Collection<Long> additiveIds);
}
