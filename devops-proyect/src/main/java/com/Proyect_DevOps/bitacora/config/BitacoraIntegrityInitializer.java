package com.Proyect_DevOps.bitacora.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Asegura que la tabla de bitácora sea append-only a nivel de base de datos.
 * Instala disparadores que rechazan cualquier UPDATE o DELETE, de modo que ni
 * un error de aplicación ni una consulta manual puedan alterar la auditoría.
 *
 * @author Equipo DevOps Huellitas
 * @version 1.0
 */
@Component
@Order(1)
public class BitacoraIntegrityInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BitacoraIntegrityInitializer.class);

    private static final String TRIGGER_UPDATE = "bitacora_append_only_update";
    private static final String TRIGGER_DELETE = "bitacora_append_only_delete";

    @Autowired(required = false)
    private DataSource dataSource;

    @Override
    public void run(String... args) {
        if (dataSource == null) {
            log.warn("No hay DataSource disponible: no se pudieron instalar los disparadores de bitácora.");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            instalarTrigger(conn, TRIGGER_UPDATE, "UPDATE");
            instalarTrigger(conn, TRIGGER_DELETE, "DELETE");
        } catch (Exception e) {
            log.error("No se pudieron verificar los disparadores append-only de bitácora: {}", e.getMessage());
        }
    }

    private void instalarTrigger(Connection conn, String nombre, String operacion) {
        try {
            if (existeTrigger(conn, nombre)) {
                return;
            }

            String sql = "CREATE TRIGGER " + nombre + " BEFORE " + operacion + " ON bitacora "
                + "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = "
                + "'La bitacora es append-only: no se permite modificar ni eliminar eventos'";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
                log.info("Disparador append-only '{}' instalado en la bitácora.", nombre);
            }
        } catch (Exception e) {
            log.error("No se pudo instalar el disparador '{}' en la bitácora: {}", nombre, e.getMessage());
        }
    }

    private boolean existeTrigger(Connection conn, String nombre) throws Exception {
        String sql = "SELECT COUNT(*) FROM information_schema.TRIGGERS "
            + "WHERE TRIGGER_SCHEMA = DATABASE() AND EVENT_OBJECT_TABLE = 'bitacora' "
            + "AND TRIGGER_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
