package com.apiweb.backend.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apiweb.backend.Model.EstadoReserva;
import com.apiweb.backend.Model.ReservaModel;

public interface ReservaRepository extends JpaRepository<ReservaModel, Integer> {

    Optional<ReservaModel> findFirstBySalaIdSalaAndEstadoOrderByFechaDescHoraInicioDesc(Integer idSala, EstadoReserva estado);
}
