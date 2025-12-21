package com.example.proyectofinal6to_ecobox.data.model;

public class MiembroUi {
    private long id;
    private String nombre;
    private String email;
    private String rol; // "Administrador" o "Miembro"
    private boolean esAdmin;

    public MiembroUi() {}

    public MiembroUi(long id, String nombre, String email, String rol, boolean esAdmin) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.esAdmin = esAdmin;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getRol() { return rol; }
    public boolean isAdmin() { return esAdmin; }
}