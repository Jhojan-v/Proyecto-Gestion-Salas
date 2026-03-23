package com.apiweb.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apiweb.backend.Model.RecursoSalaModel;

public interface RecursoSalaRepository extends JpaRepository<RecursoSalaModel, Integer> {

    List<RecursoSalaModel> findBySalaIdSalaOrderByRecursoNombreRecursoAsc(Integer idSala);

    Optional<RecursoSalaModel> findBySalaIdSalaAndRecursoCodigoRecursoIgnoreCase(Integer idSala, String codigoRecurso);
}
