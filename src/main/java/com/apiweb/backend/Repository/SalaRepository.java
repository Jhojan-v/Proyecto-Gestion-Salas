package com.apiweb.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.apiweb.backend.Model.SalaModel;

public interface SalaRepository extends JpaRepository<SalaModel, Long> {

    boolean existsByNombre(String nombre);
}