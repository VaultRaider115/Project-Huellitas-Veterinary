-- Migración 001 — tabla de unión `Cita-Servicio`
--
-- Producción corre con ddl-auto=validate, así que Hibernate exige que existan
-- todas las tablas mapeadas. CitaModel la declara con:
--   @JoinTable(name = "Cita-Servicio", joinColumns = IdCita, inverseJoinColumns = IdServicio)
--
-- Antes de crear nada, mira qué hay realmente en el servidor:
--   SHOW TABLES LIKE '%ervicio%';
--
-- Si aparece `cita-servicio` (minúsculas, típico de un dump hecho en un MariaDB
-- con lower_case_table_names=1), renómbrala en vez de crearla:
--   RENAME TABLE `cita-servicio` TO `Cita-Servicio`;

CREATE TABLE IF NOT EXISTS `Cita-Servicio` (
  `IdCita` INT NOT NULL,
  `IdServicio` INT NOT NULL,
  PRIMARY KEY (`IdCita`, `IdServicio`),
  KEY `idx_cita_servicio_servicio` (`IdServicio`)
) ENGINE=InnoDB;

-- Claves foráneas: Hibernate no las valida, pero evitan filas huérfanas.
-- Ajusta los nombres de las PK si en tu esquema no son id_cita / id_servicio.
-- ALTER TABLE `Cita-Servicio`
--   ADD CONSTRAINT `fk_cita_servicio_cita`
--     FOREIGN KEY (`IdCita`) REFERENCES `citas` (`id_cita`) ON DELETE CASCADE,
--   ADD CONSTRAINT `fk_cita_servicio_servicio`
--     FOREIGN KEY (`IdServicio`) REFERENCES `servicios` (`id_servicio`) ON DELETE CASCADE;
