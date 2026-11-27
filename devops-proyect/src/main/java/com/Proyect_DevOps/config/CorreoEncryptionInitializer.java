package com.Proyect_DevOps.config;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.Proyect_DevOps.users.models.UsuarioModel;
import com.Proyect_DevOps.users.repositories.UsuarioRepository;
import com.Proyect_DevOps.utilities.AESUtil;
import com.Proyect_DevOps.utilities.HMACUtil;

/**
 * Verifica al iniciar la aplicación que todos los correos almacenados en la base
 * de datos estén cifrados con AES-256-GCM. Los correos que se encuentren en texto
 * plano se cifran y se recalcula su huella HMAC para que el inicio de sesión siga
 * funcionando.
 *
 * <p>Los valores que no se pueden descifrar y tampoco parecen un correo en texto
 * plano (por ejemplo, un dato cifrado con otra clave) se omiten con una advertencia
 * para evitar un doble cifrado que dejaría el dato irrecuperable.</p>
 *
 * @author Equipo DevOps Huellitas
 * @version 1.0
 */
@Component
@Order(2)
public class CorreoEncryptionInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CorreoEncryptionInitializer.class);

    /**
     * Formato mínimo de un correo en texto plano. Ningún cifrado AES-GCM en Base64
     * contiene el carácter '@', por lo que este patrón distingue un correo plano de
     * un valor cifrado o corrupto.
     */
    private static final Pattern CORREO_PLANO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) {
        int revisados = 0;
        int yaCifrados = 0;
        int migrados = 0;
        int omitidos = 0;

        try {
            for (UsuarioModel usuario : usuarioRepository.findAll()) {
                revisados++;
                String correo = usuario.getCorreo();

                if (correo == null || correo.isBlank()) {
                    omitidos++;
                    log.warn("El usuario {} no tiene correo almacenado; se omite.", usuario.getIdUsuario());
                    continue;
                }

                if (AESUtil.esCifradoAESGCM(correo)) {
                    yaCifrados++;
                    continue;
                }

                if (!CORREO_PLANO.matcher(correo).matches()) {
                    omitidos++;
                    log.warn("El correo del usuario {} no se puede descifrar con la clave actual y no "
                        + "parece texto plano; se omite para evitar un doble cifrado.", usuario.getIdUsuario());
                    continue;
                }

                try {
                    String hmacEsperado = HMACUtil.GenerarHuella(correo);
                    usuario.setCorreo(AESUtil.encriptar(correo));
                    if (!hmacEsperado.equals(usuario.getCorreoHMAC())) {
                        usuario.setCorreoHMAC(hmacEsperado);
                    }
                    usuarioRepository.save(usuario);
                    migrados++;
                    log.info("Correo del usuario {} cifrado con AES-256-GCM.", usuario.getIdUsuario());
                } catch (Exception e) {
                    omitidos++;
                    log.error("No se pudo cifrar el correo del usuario {}: {}",
                        usuario.getIdUsuario(), e.getMessage());
                }
            }
        } catch (Exception e) {
            // La verificación nunca debe impedir el arranque de la aplicación.
            log.error("No se pudo verificar el cifrado de los correos al iniciar: {}", e.getMessage());
            return;
        }

        log.info("Verificación de cifrado de correos finalizada: {} revisados, {} ya cifrados, "
            + "{} migrados, {} omitidos.", revisados, yaCifrados, migrados, omitidos);
    }
}
