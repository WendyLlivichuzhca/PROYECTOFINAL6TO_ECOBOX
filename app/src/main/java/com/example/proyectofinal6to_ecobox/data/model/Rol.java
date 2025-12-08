package com.example.proyectofinal6to_ecobox.data.model;

public class Rol {
    private long id;
    private String nombre; // Ej: "Administrador", "Jardinero"
    private String descripcion;

    public Rol() {}

    public Rol(long id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() { return nombre; }
}