package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

public class FamiliaUsuario {
    private long id;

    @SerializedName("fecha_union")
    private String fechaUnion;

    @SerializedName("es_administrador")
    private boolean esAdministrador;

    @SerializedName("familia_id")
    private long familiaId;

    @SerializedName("usuario_id")
    private long usuarioId;

    private boolean activo;

    @SerializedName("rol_id")
    private long rolId;

    public FamiliaUsuario() {
    }

    public FamiliaUsuario(long id, String fechaUnion, boolean esAdministrador, long familiaId, long usuarioId, boolean activo, long rolId) {
        this.id = id;
        this.fechaUnion = fechaUnion;
        this.esAdministrador = esAdministrador;
        this.familiaId = familiaId;
        this.usuarioId = usuarioId;
        this.activo = activo;
        this.rolId = rolId;
    }

    // Getters y Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFechaUnion() { return fechaUnion; }
    public void setFechaUnion(String fechaUnion) { this.fechaUnion = fechaUnion; }

    public boolean isEsAdministrador() { return esAdministrador; }
    public void setEsAdministrador(boolean esAdministrador) { this.esAdministrador = esAdministrador; }

    public long getFamiliaId() { return familiaId; }
    public void setFamiliaId(long familiaId) { this.familiaId = familiaId; }

    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long usuarioId) { this.usuarioId = usuarioId; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public long getRolId() { return rolId; }
    public void setRolId(long rolId) { this.rolId = rolId; }

    @Override
    public String toString() {
        return "FamiliaUsuario{" + "usuarioId=" + usuarioId + ", familiaId=" + familiaId + '}';
    }
}