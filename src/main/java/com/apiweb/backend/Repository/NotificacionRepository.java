package com.apiweb.backend.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apiweb.backend.Model.NotificacionModel;

public interface NotificacionRepository extends JpaRepository<NotificacionModel, Integer> {

    List<NotificacionModel> findByIdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);

    long countByIdUsuarioAndLeidaFalse(Integer idUsuario);

    @Modifying
    @Query("UPDATE NotificacionModel n SET n.leida = true WHERE n.idUsuario = :idUsuario AND n.leida = false")
    int marcarTodasLeidas(@Param("idUsuario") Integer idUsuario);
}
