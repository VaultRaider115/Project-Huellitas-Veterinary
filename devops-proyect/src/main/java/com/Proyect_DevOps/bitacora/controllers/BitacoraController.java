package com.Proyect_DevOps.bitacora.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Proyect_DevOps.bitacora.models.BitacoraModel;
import com.Proyect_DevOps.bitacora.services.BitacoraService;

/**
 * Controlador REST de solo lectura para la consulta de la bitácora de auditoría.
 * No expone operaciones de creación, actualización o borrado: los eventos los
 * registra el interceptor de auditoría y la base de datos los protege como
 * append-only.
 *
 * @author Equipo DevOps Huellitas
 * @version 1.0
 */
@RestController
@RequestMapping("/bitacora")
public class BitacoraController {

    private static final String ADMIN_ONLY =
        "hasAuthority('TOKEN_access') and (hasAuthority('Admin') or hasAuthority('Administrador'))";

    private final BitacoraService service;

    /**
     * Constructor con inyección del servicio de auditoría.
     *
     * @param service Servicio de consulta y registro de eventos.
     */
    public BitacoraController(BitacoraService service) {
        this.service = service;
    }

    /**
     * Endpoint HTTP GET para obtener todos los eventos de auditoría, del más
     * reciente al más antiguo.
     *
     * @return {@link ResponseEntity} con la lista de eventos.
     */
    @GetMapping
    @PreAuthorize(ADMIN_ONLY)
    public ResponseEntity<List<BitacoraModel>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    /**
     * Endpoint HTTP GET para consultar un evento de auditoría por su identificador.
     *
     * @param id Identificador único del evento.
     * @return {@link ResponseEntity} con el evento encontrado o 404 si no existe.
     */
    @GetMapping("/{id:[0-9]+}")
    @PreAuthorize(ADMIN_ONLY)
    public ResponseEntity<BitacoraModel> getById(@PathVariable("id") Integer id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
