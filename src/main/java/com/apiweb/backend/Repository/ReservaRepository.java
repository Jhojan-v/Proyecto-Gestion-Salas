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

    List<ReservaModel> findByIdUsuarioOrderByFechaDescHoraInicioDesc(Integer idUsuario);

    List<ReservaModel> findBySalaFacultadIdOrderByFechaDescHoraInicioDesc(Integer facultadId);

    List<ReservaModel> findByIdUsuarioAndSalaFacultadIdOrderByFechaDescHoraInicioDesc(
            Integer idUsuario, Integer facultadId);

    @Query("SELECT r FROM ReservaModel r WHERE r.sala.facultadId = :facultadId " +
           "AND r.estado IN :estados " +
           "AND (:fechaInicio IS NULL OR r.fecha >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR r.fecha <= :fechaFin) " +
           "ORDER BY r.sala.nombre ASC, r.fecha DESC, r.horaInicio ASC")
    List<ReservaModel> buscarReservasParaReporte(
            @Param("facultadId") Integer facultadId,
            @Param("estados") List<EstadoReserva> estados,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);
    
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

    @Query("SELECT DISTINCT r.idUsuario FROM ReservaModel r " +
           "WHERE r.sala.idSala = :idSala AND r.estado IN :estados " +
           "AND r.fecha >= :fechaDesde")
    List<Integer> buscarUsuariosConReservasActivas(
            @Param("idSala") Integer idSala,
            @Param("estados") List<EstadoReserva> estados,
            @Param("fechaDesde") LocalDate fechaDesde);
    
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
