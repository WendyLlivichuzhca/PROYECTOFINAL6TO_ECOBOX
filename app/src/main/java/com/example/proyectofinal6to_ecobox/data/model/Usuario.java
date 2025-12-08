package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

public class Usuario {

    private long id;

    // Campos de autenticación básicos
    private String password; // varchar(128) - Generalmente hash
    private String username; // varchar(150)
    private String email;    // varchar(254)

    // Datos personales
    @SerializedName("first_name")
    private String firstName; // varchar(30)

    @SerializedName("last_name")
    private String lastName;  // varchar(30)

    private String telefono;  // varchar(15)

    @SerializedName("fecha_nacimiento")
    private String fechaNacimiento; // date (lo manejamos como String "YYYY-MM-DD")

    // Permisos y estados (tinyint(1) -> boolean)
    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("is_staff")
    private boolean isStaff;

    @SerializedName("is_superuser")
    private boolean isSuperuser;

    // Fechas de sistema (datetime -> String ISO)
    @SerializedName("date_joined")
    private String dateJoined;

    @SerializedName("last_login")
    private String lastLogin;

    // Relaciones (Foreign Keys)
    @SerializedName("rol_id")
    private long rolId; // bigint

    // Recuperación de contraseña (opcionales/nulos)
    @SerializedName("reset_password_token")
    private String resetPasswordToken;

    @SerializedName("reset_password_expires")
    private String resetPasswordExpires;

    // --- Constructor Vacío (Requerido) ---
    public Usuario() {
    }

    // --- Constructor Completo ---
    public Usuario(long id, String password, String username, String email, String firstName,
                   String lastName, String telefono, String fechaNacimiento, boolean isActive,
                   boolean isStaff, boolean isSuperuser, String dateJoined, String lastLogin,
                   long rolId, String resetPasswordToken, String resetPasswordExpires) {
        this.id = id;
        this.password = password;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.telefono = telefono;
        this.fechaNacimiento = fechaNacimiento;
        this.isActive = isActive;
        this.isStaff = isStaff;
        this.isSuperuser = isSuperuser;
        this.dateJoined = dateJoined;
        this.lastLogin = lastLogin;
        this.rolId = rolId;
        this.resetPasswordToken = resetPasswordToken;
        this.resetPasswordExpires = resetPasswordExpires;
    }

    // --- Getters y Setters ---

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isStaff() { return isStaff; }
    public void setStaff(boolean staff) { isStaff = staff; }

    public boolean isSuperuser() { return isSuperuser; }
    public void setSuperuser(boolean superuser) { isSuperuser = superuser; }

    public String getDateJoined() { return dateJoined; }
    public void setDateJoined(String dateJoined) { this.dateJoined = dateJoined; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }

    public long getRolId() { return rolId; }
    public void setRolId(long rolId) { this.rolId = rolId; }

    public String getResetPasswordToken() { return resetPasswordToken; }
    public void setResetPasswordToken(String resetPasswordToken) { this.resetPasswordToken = resetPasswordToken; }

    public String getResetPasswordExpires() { return resetPasswordExpires; }
    public void setResetPasswordExpires(String resetPasswordExpires) { this.resetPasswordExpires = resetPasswordExpires; }

    // --- Métodos de utilidad ---

    public String getNombreCompleto() {
        if (firstName == null && lastName == null) return username;
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}