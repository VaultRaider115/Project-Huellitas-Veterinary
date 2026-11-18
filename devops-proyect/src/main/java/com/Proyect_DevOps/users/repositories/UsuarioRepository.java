package com.Proyect_DevOps.users.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Proyect_DevOps.users.models.UsuarioModel;

/**
 * Repositorio JPA para la persistencia y búsqueda de entidades {@link UsuarioModel}.
 * Permite la búsqueda segura por huella criptográfica HMAC del correo.
 *
 * @author Equipo DevOps Huellitas
 * @version 1.0
 */
@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioModel, Integer> {
    
    /**
     * Busca un usuario por la huella digital criptográfica (HMAC-SHA256) de su correo electrónico.
     *
     * @param correo Huella HMAC del correo a buscar.
     * @return {@link Optional} con el usuario correspondiente, o vacío si no se encuentra.
     */
    Optional<UsuarioModel> findByCorreoHMAC(String correo);

    /**
     * Comprueba si ya existe un usuario registrado con la huella digital del correo especificado.
     *
     * @param correo Huella HMAC del correo a validar.
     * @return {@code true} si ya existe un registro con dicha huella, {@code false} en caso contrario.
     */
    boolean existsByCorreoHMAC(String correo);

    /**
     * Busca usuarios asociados a un rol específico y a una sucursal determinada.
     *
     * @param idRol Identificador numérico del rol.
     * @param idSucursal Identificador de la sucursal.
     * @return Lista de usuarios coincidentes.
     */
    List<UsuarioModel> findByRol_IdRolAndSucursal_IdSucursal(int idRol, int idSucursal);

    /**
     * Actualiza exclusivamente los campos de MFA de un usuario.
     * Evita volver a persistir la entidad completa, que contiene el correo descifrado
     * en memoria y podría sobrescribir el valor cifrado en reposo.
     *
     * @param idUsuario Identificador del usuario.
     * @param secretoMFA Secreto TOTP activo cifrado, o {@code null}.
     * @param secretoPendienteMFA Secreto TOTP pendiente cifrado, o {@code null}.
     * @param mfaneed Indica si el usuario requiere el segundo factor.
     * @return Número de filas actualizadas.
     */
    @Modifying
    @Query("update UsuarioModel u set u.secretoMFA = :secretoMFA, "
        + "u.secretoPendienteMFA = :secretoPendienteMFA, u.mfaneed = :mfaneed "
        + "where u.idUsuario = :idUsuario")
    int actualizarCamposMFA(
        @Param("idUsuario") int idUsuario,
        @Param("secretoMFA") String secretoMFA,
        @Param("secretoPendienteMFA") String secretoPendienteMFA,
        @Param("mfaneed") boolean mfaneed
    );
}
