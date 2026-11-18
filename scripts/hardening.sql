-- Endurecimiento de base de datos para Huellitas.
-- Ejecutar como usuario administrador de la base de datos después de crear el
-- esquema y antes de exponer la aplicación en producción:
--
--   mariadb --defaults-extra-file=/root/.my.cnf huellitas_db < scripts/hardening.sql
--
-- La bitácora de auditoría es append-only: no se permite UPDATE ni DELETE.

DROP TRIGGER IF EXISTS bitacora_append_only_update;
DROP TRIGGER IF EXISTS bitacora_append_only_delete;

DELIMITER //
CREATE TRIGGER bitacora_append_only_update
BEFORE UPDATE ON bitacora
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'La bitacora es append-only: no se permite modificar eventos';
END//

CREATE TRIGGER bitacora_append_only_delete
BEFORE DELETE ON bitacora
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'La bitacora es append-only: no se permite eliminar eventos';
END//
DELIMITER ;
