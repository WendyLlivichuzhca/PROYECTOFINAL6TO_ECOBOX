package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Planta implements Serializable {  // <-- AÑADE implements Serializable

    private static final long serialVersionUID = 1L;  // <-- Añade esto también

    private long id;
    private String nombre;
    private String especie;

    @SerializedName("fecha_creacion")
    private String fechaCreacion;

    private String descripcion;

    @SerializedName("familia_id")
    private long familiaId;

    private String ubicacion;
    private String aspecto;
    private String estado;
    private String foto;

    public Planta() {
        this.aspecto = "normal";
        this.estado = "normal";
        this.foto = "";
        this.ubicacion = "";
    }

    // Constructor con 10 parámetros (todos los campos)
    public Planta(long id, String nombre, String especie, String fechaCreacion,
                  String descripcion, long familiaId, String ubicacion,
                  String aspecto, String estado, String foto) {
        this.id = id;
        this.nombre = nombre;
        this.especie = especie;
        this.fechaCreacion = fechaCreacion;
        this.descripcion = descripcion;
        this.familiaId = familiaId;
        this.ubicacion = ubicacion;
        this.aspecto = aspecto;
        this.estado = estado;
        this.foto = foto;
    }

    // Constructor con 7 parámetros (backward compatible - EL QUE USAS ACTUALMENTE)
    public Planta(long id, String nombre, String especie, String fechaCreacion,
                  String descripcion, long familiaId, String ubicacion) {
        this(id, nombre, especie, fechaCreacion, descripcion, familiaId,
                ubicacion, "normal", "normal", "");  // FOTO vacía por defecto
    }

    // Constructor con 6 parámetros (backward compatible)
    public Planta(long id, String nombre, String especie, String fechaCreacion,
                  String descripcion, long familiaId) {
        this(id, nombre, especie, fechaCreacion, descripcion, familiaId,
                "", "normal", "normal", "");
    }

    // MÉTODO NUEVO - ¡IMPORTANTE!
    public boolean tieneFoto() {
        return foto != null && !foto.isEmpty();
    }

    // Getters y Setters (todos, incluyendo foto)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEspecie() {
        return especie;
    }

    public void setEspecie(String especie) {
        this.especie = especie;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public long getFamiliaId() {
        return familiaId;
    }

    public void setFamiliaId(long familiaId) {
        this.familiaId = familiaId;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getAspecto() {
        return aspecto;
    }

    public void setAspecto(String aspecto) {
        this.aspecto = aspecto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    @Override
    public String toString() {
        return "Planta{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", especie='" + especie + '\'' +
                ", fechaCreacion='" + fechaCreacion + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", familiaId=" + familiaId +
                ", ubicacion='" + ubicacion + '\'' +
                ", aspecto='" + aspecto + '\'' +
                ", estado='" + estado + '\'' +
                ", foto='" + foto + '\'' +
                '}';
    }
}