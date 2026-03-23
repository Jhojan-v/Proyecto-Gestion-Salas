package com.apiweb.backend.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auditoria")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(nullable = false, length = 40)
    private String entidad;

    @Column(name = "id_registro", nullable = false)
    private Long entidadId;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(name = "usuario_actor")
    private Integer usuarioActor;

    @Column(name = "correo_actor", length = 150)
    private String correoActor;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "datos_anteriores", columnDefinition = "json")
    private String datosAnteriores;

    @Column(name = "datos_nuevos", columnDefinition = "json")
    private String datosNuevos;

    @Column(length = 255)
    private String observacion;
}
