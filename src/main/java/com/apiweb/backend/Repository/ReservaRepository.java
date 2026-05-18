package com.apiweb.backend.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apiweb.backend.Model.EstadoReserva;
import com.apiweb.backend.Model.ReservaModel;

public interface ReservaRepository extends JpaRepository<ReservaModel, Integer> {

    Optional<ReservaModel> findFirstBySalaIdSalaAndEstadoOrderByFechaDescHoraInicioDesc(Integer idSala, EstadoReserva estado);
    
    List<ReservaModel> findBySalaIdSalaAndFechaAndEstadoInOrderByHoraInicioAsc(
            Integer idSala, LocalDate fecha, List<EstadoReserva> estados);
    
    List<ReservaModel> findByIdUsuarioAndEstadoInOrderByFechaDescHoraInicioDesc(
            Integer idUsuario, List<EstadoReserva> estados);
    
    @Query("SELECT r FROM ReservaModel r WHERE r.sala.facultadId = :facultadId AND r.fecha = :fecha AND r.estado IN :estados ORDER BY r.horaInicio ASC")
    List<ReservaModel> findBySalaFacultadIdAndFechaAndEstadoInOrderByHoraInicioAsc(
            @Param("facultadId") Integer facultadId, 
            @Param("fecha") LocalDate fecha, 
            @Param("estados") List<EstadoReserva> estados);

    @Query("SELECT r FROM ReservaModel r WHERE r.sala.facultadId = :facultadId " +
           "AND (:fechaInicio IS NULL OR r.fecha >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR r.fecha <= :fechaFin) " +
           "AND (:sala IS NULL OR LOWER(r.sala.nombre) LIKE LOWER(CONCAT('%', :sala, '%'))) " +
           "AND (:profesorFiltrado = false OR r.idUsuario IN :profesorIds) " +
           "ORDER BY r.fecha DESC, r.horaInicio DESC")
    List<ReservaModel> buscarHistorialFacultad(
            @Param("facultadId") Integer facultadId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("sala") String sala,
            @Param("profesorFiltrado") boolean profesorFiltrado,
            @Param("profesorIds") List<Integer> profesorIds);
    
    Optional<ReservaModel> findByIdReservaAndIdUsuario(Integer idReserva, Integer idUsuario);
    
    @Query("SELECT r FROM ReservaModel r WHERE r.sala.idSala = :idSala " +
           "AND r.fecha = :fecha AND r.estado IN :estados " +
           "AND ((r.horaInicio < :horaFin AND r.horaFin > :horaInicio))")
    List<ReservaModel> findReservasSolapadas(
            @Param("idSala") Integer idSala,
            @Param("fecha") LocalDate fecha,
            @Param("estados") List<EstadoReserva> estados,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);
}
