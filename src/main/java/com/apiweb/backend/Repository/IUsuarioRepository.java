package com.apiweb.backend.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apiweb.backend.Model.UsuarioModel;

public interface IUsuarioRepository extends JpaRepository<UsuarioModel, Integer> {

    UsuarioModel findByCorreo(String correo);

    List<UsuarioModel> findByIdFacultadOrderByNombreAsc(Integer facultadId);

    @Query("SELECT u FROM UsuarioModel u WHERE u.idFacultad = :facultadId " +
            "AND (LOWER(u.nombre) LIKE LOWER(CONCAT('%', :profesor, '%')) " +
            "OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :profesor, '%')))")
    List<UsuarioModel> buscarProfesoresPorFacultad(
            @Param("facultadId") Integer facultadId,
            @Param("profesor") String profesor);

    @Query("SELECT u FROM UsuarioModel u WHERE u.idFacultad = :facultadId " +
            "AND (LOWER(u.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
            "OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
            "OR LOWER(u.rol) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
            "ORDER BY u.nombre ASC")
    List<UsuarioModel> buscarUsuariosPorFacultad(
            @Param("facultadId") Integer facultadId,
            @Param("busqueda") String busqueda);

    @Query("SELECT u.idUsuario FROM UsuarioModel u " +
            "WHERE u.idFacultad = :facultadId AND UPPER(u.rol) = :rol")
    List<Integer> buscarIdsPorFacultadYRol(
            @Param("facultadId") Integer facultadId,
            @Param("rol") String rol);
}
