package com.example.proyectofinal6to_ecobox.data.model;

public class MiembroUi {
    private long id;
    private String nombre;
    private String email;
    private String rol; // "Administrador" o "Miembro"
    private boolean esAdmin;
    private String ultimaActividad;
    private boolean estaActivo;
    private String avatarColor;

    public MiembroUi() {}

    public MiembroUi(long id, String nombre, String email, String rol, boolean esAdmin) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.esAdmin = esAdmin;
        this.ultimaActividad = "Recientemente";
        this.estaActivo = true;
        this.avatarColor = "#4CAF50"; // Verde por defecto
    }

    // Constructor completo
    public MiembroUi(long id, String nombre, String email, String rol,
                     boolean esAdmin, String ultimaActividad,
                     boolean estaActivo, String avatarColor) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.esAdmin = esAdmin;
        this.ultimaActividad = ultimaActividad;
        this.estaActivo = estaActivo;
        this.avatarColor = avatarColor;
    }

    public long getId() { return id; }

    public String getNombre() { return nombre; }

    public String getEmail() { return email; }

    public String getRol() { return rol; }

    public boolean isAdmin() { return esAdmin; }

    public String getUltimaActividad() { return ultimaActividad; }

    public boolean isEstaActivo() { return estaActivo; }

    public String getAvatarColor() { return avatarColor; }

    // Métodos setter por si los necesitas
    public void setUltimaActividad(String ultimaActividad) {
        this.ultimaActividad = ultimaActividad;
    }

    public void setEstaActivo(boolean estaActivo) {
        this.estaActivo = estaActivo;
    }

    public void setAvatarColor(String avatarColor) {
        this.avatarColor = avatarColor;
    }

    // Método útil para obtener la inicial del nombre
    public String getInicial() {
        if (nombre != null && !nombre.isEmpty()) {
            return nombre.substring(0, 1).toUpperCase();
        }
        return "?";
    }

    // Método para obtener color del rol (verde para admin, azul para miembro)
    public String getColorRol() {
        return esAdmin ? "#2E7D32" : "#2196F3";
    }
}