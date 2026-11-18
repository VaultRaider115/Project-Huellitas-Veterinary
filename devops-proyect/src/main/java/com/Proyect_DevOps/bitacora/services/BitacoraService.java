package com.Proyect_DevOps.bitacora.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.Proyect_DevOps.bitacora.models.BitacoraModel;
import com.Proyect_DevOps.bitacora.repositories.BitacoraRepository;
import com.Proyect_DevOps.common.services.GenericServiceImpl;

/**
 * Servicio de negocio para la gestión y registro persistente de auditoría.
 * Proporciona métodos transaccionales independientes ({@link Propagation#REQUIRES_NEW})
 * para garantizar que los registros de auditoría no se pierdan incluso si la transacción de negocio falla.
 *
 * @author Equipo DevOps Huellitas
 * @version 1.0
 */
@Service
public class BitacoraService extends GenericServiceImpl<BitacoraModel, Integer, BitacoraRepository> {

    /**
     * Longitud máxima permitida para el campo de detalle antes del truncamiento.
     */
    private static final int MAX_DETALLE_LEN = 990;
    private static final int MAX_USUARIO_LEN = 254;
    private static final int MAX_CAMPO_LEN = 100;
    private static final int MAX_IP_LEN = 45;

    /** Caracteres de control que permiten inyectar líneas falsas en los logs. */
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\\t]]");

    /** Redacta valores sensibles que podrían llegar en un mensaje de error. */
    private static final Pattern SENSITIVE_VALUES = Pattern.compile(
        "(?i)(password|contrasena|contraseña|token|secret|authorization|api[_-]?key)\\s*[=:]\\s*[^\\s|,;]+");

    /**
     * {@inheritDoc}
     * Recupera la lista completa de eventos ordenada cronológicamente de forma descendente.
     */
    @Override
    @Transactional(readOnly = true)
    public List<BitacoraModel> findAll() {
        return repository.findAllByOrderByFechaHoraDesc();
    }

    /**
     * Registra un nuevo evento en la bitácora dentro de una transacción independiente.
     *
     * @param usuario Identificador o correo del usuario, o nulo para registrar como Anónimo.
     * @param rol Rol del usuario, o nulo para Desconocido.
     * @param accion Código de la acción auditada.
     * @param modulo Módulo en el que se ejecuta la acción.
     * @param detalle Información complementaria de la solicitud y respuesta.
     * @param ip Dirección IP de origen de la solicitud.
     * @return El registro de auditoría persistido en la base de datos.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BitacoraModel registrarAccion(String usuario, String rol, String accion, String modulo, String detalle, String ip) {
        String cleanUsuario = limpiar(usuario, MAX_USUARIO_LEN, "Anónimo");
        String cleanRol = limpiar(rol, MAX_CAMPO_LEN, "Desconocido");
        String cleanAccion = limpiar(accion, MAX_CAMPO_LEN, "ACCION");
        String cleanModulo = limpiar(modulo, MAX_CAMPO_LEN, "General");
        String cleanIp = limpiar(ip, MAX_IP_LEN, "Desconocida");
        String cleanDetalle = sanitizeDetalle(detalle);

        BitacoraModel bitacora = new BitacoraModel(cleanUsuario, cleanRol, cleanAccion, cleanModulo, cleanDetalle, cleanIp);
        return repository.save(bitacora);
    }

    /**
     * {@inheritDoc}
     * Asigna la fecha y hora si no fueron provistas y sanitiza la longitud del detalle.
     */
    @Override
    @Transactional
    public BitacoraModel save(BitacoraModel entity) {
        if (entity.getFechaHora() == null) {
            entity.setFechaHora(LocalDateTime.now());
        }
        if (entity.getDetalle() != null) {
            entity.setDetalle(sanitizeDetalle(entity.getDetalle()));
        }
        return super.save(entity);
    }

    /**
     * Sanitiza el texto del detalle asegurando que no exceda el límite de caracteres de la columna en base de datos.
     *
     * @param detalle Cadena original con el detalle a registrar.
     * @return Cadena truncada si sobrepasa el límite, o el texto original sin cambios.
     */
    private String sanitizeDetalle(String detalle) {
        if (detalle == null) return null;
        String limpio = CONTROL_CHARS.matcher(detalle).replaceAll(" ");
        limpio = SENSITIVE_VALUES.matcher(limpio).replaceAll("$1=[REDACTADO]");
        if (limpio.length() > MAX_DETALLE_LEN) {
            return limpio.substring(0, MAX_DETALLE_LEN) + "...";
        }
        return limpio;
    }

    /**
     * Normaliza un campo corto: elimina caracteres de control, recorta espacios y
     * limita su longitud para evitar inyección en los registros de auditoría.
     *
     * @param valor Valor original.
     * @param maxLongitud Longitud máxima permitida.
     * @param porDefecto Valor a usar si el campo está vacío.
     * @return Valor saneado.
     */
    private String limpiar(String valor, int maxLongitud, String porDefecto) {
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        String limpio = CONTROL_CHARS.matcher(valor).replaceAll(" ").trim();
        if (limpio.length() > maxLongitud) {
            limpio = limpio.substring(0, maxLongitud);
        }
        return limpio.isBlank() ? porDefecto : limpio;
    }
}
