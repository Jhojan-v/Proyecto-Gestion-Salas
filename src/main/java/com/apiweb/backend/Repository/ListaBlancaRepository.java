package com.apiweb.backend.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apiweb.backend.Model.ListaBlancaModel;

public interface ListaBlancaRepository extends JpaRepository<ListaBlancaModel, Integer> {

    Optional<ListaBlancaModel> findByCorreoAutorizadoIgnoreCaseAndActivoTrue(String correoAutorizado);
}
