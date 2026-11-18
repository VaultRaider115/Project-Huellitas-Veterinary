package com.Proyect_DevOps.gestor.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO para la creación o actualización de veterinarios por el gestor.
 * Las restricciones de tamaño y formato se validan antes de llegar al servicio.
 *
 * @author Equipo DevOps Huellitas
 * @version 1.0
 */
public class GuardarVeterinarioDTO {
    @Size(max = 100)
    private String nombre;
    @Size(max = 100)
    private String paterno;
    @Size(max = 100)
    private String materno;
    @Email
    @Size(max = 254)
    private String correo;
    @Size(min = 12, max = 128)
    private String contrasena;

    public GuardarVeterinarioDTO() {
    }

    public GuardarVeterinarioDTO(String nombre, String paterno, String materno, String correo, String contrasena) {
        this.nombre = nombre;
        this.paterno = paterno;
        this.materno = materno;
        this.correo = correo;
        this.contrasena = contrasena;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPaterno() {
        return paterno;
    }

    public void setPaterno(String paterno) {
        this.paterno = paterno;
    }

    public String getMaterno() {
        return materno;
    }

    public void setMaterno(String materno) {
        this.materno = materno;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
