package com.apiweb.backend.DTO;

public class RetirarRecursoResponse {

    private Integer idRecursoSala;
    private String codigoRecurso;
    private String nombreRecurso;
    private String mensaje;

    public RetirarRecursoResponse() {
    }

    public RetirarRecursoResponse(Integer idRecursoSala, String codigoRecurso, String nombreRecurso, String mensaje) {
        this.idRecursoSala = idRecursoSala;
        this.codigoRecurso = codigoRecurso;
        this.nombreRecurso = nombreRecurso;
        this.mensaje = mensaje;
    }

    public Integer getIdRecursoSala() {
        return idRecursoSala;
    }

    public void setIdRecursoSala(Integer idRecursoSala) {
        this.idRecursoSala = idRecursoSala;
    }

    public String getCodigoRecurso() {
        return codigoRecurso;
    }

    public void setCodigoRecurso(String codigoRecurso) {
        this.codigoRecurso = codigoRecurso;
    }

    public String getNombreRecurso() {
        return nombreRecurso;
    }

    public void setNombreRecurso(String nombreRecurso) {
        this.nombreRecurso = nombreRecurso;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}