package com.apiweb.backend.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apiweb.backend.Model.SalaModel;

public interface SalaRepository extends JpaRepository<SalaModel, Integer> {

    boolean existsByNombreIgnoreCaseAndFacultadIdAndIdSalaNot(String nombre, Integer facultadId, Integer idSala);

    List<SalaModel> findByFacultadIdOrderByNombreAsc(Integer facultadId);
}
