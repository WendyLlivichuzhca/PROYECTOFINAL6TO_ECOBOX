package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

public class LogSistema {
    private long id;
    private String nivel; // INFO, WARNING, ERROR
    private String mensaje;
    private String fecha;
    private String ip;

    @SerializedName("usuario_id")
    private long usuarioId;

    public LogSistema() {}

    public LogSistema(long id, String nivel, String mensaje, String fecha, String ip, long usuarioId) {
        this.id = id;
        this.nivel = nivel;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.ip = ip;
        this.usuarioId = usuarioId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long usuarioId) { this.usuarioId = usuarioId; }
}