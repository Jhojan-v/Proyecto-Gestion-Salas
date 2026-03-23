package com.apiweb.backend.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apiweb.backend.Model.RecursoTecnologicoModel;

public interface RecursoTecnologicoRepository extends JpaRepository<RecursoTecnologicoModel, Integer> {

    Optional<RecursoTecnologicoModel> findByCodigoRecursoIgnoreCase(String codigoRecurso);
}
