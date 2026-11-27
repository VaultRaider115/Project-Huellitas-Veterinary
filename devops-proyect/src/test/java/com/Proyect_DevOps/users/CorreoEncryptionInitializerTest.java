package com.Proyect_DevOps.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.Proyect_DevOps.config.CorreoEncryptionInitializer;
import com.Proyect_DevOps.users.models.UsuarioModel;
import com.Proyect_DevOps.users.repositories.UsuarioRepository;
import com.Proyect_DevOps.utilities.AESUtil;
import com.Proyect_DevOps.utilities.HMACUtil;

/**
 * Pruebas del inicializador que cifra al arrancar los correos que no están
 * protegidos con AES-256-GCM.
 */
public class CorreoEncryptionInitializerTest {

    private UsuarioRepository usuarioRepository;
    private CorreoEncryptionInitializer initializer;

    @BeforeEach
    void setUp() throws Exception {
        usuarioRepository = mock(UsuarioRepository.class);
        initializer = new CorreoEncryptionInitializer();
        Field field = CorreoEncryptionInitializer.class.getDeclaredField("usuarioRepository");
        field.setAccessible(true);
        field.set(initializer, usuarioRepository);
    }

    private UsuarioModel usuario(int id, String correo, String hmac) {
        UsuarioModel usuario = new UsuarioModel();
        usuario.setIdUsuario(id);
        usuario.setCorreo(correo);
        usuario.setCorreoHMAC(hmac);
        return usuario;
    }

    @Test
    void cifraCorreoEnTextoPlanoYRecalculaSuHMAC() {
        String correoPlano = "paciente@huellitas.mx";
        UsuarioModel usuario = usuario(1, correoPlano, "hmac-incorrecto");
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        initializer.run();

        assertNotEquals(correoPlano, usuario.getCorreo());
        assertTrue(AESUtil.esCifradoAESGCM(usuario.getCorreo()),
            "El correo almacenado debe ser cifrado AES-256-GCM válido");
        assertEquals(correoPlano, AESUtil.desencriptar(usuario.getCorreo()));
        assertEquals(HMACUtil.GenerarHuella(correoPlano), usuario.getCorreoHMAC());
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void noModificaUnCorreoYaCifrado() {
        String cifrado = AESUtil.encriptar("vet@huellitas.mx");
        UsuarioModel usuario = usuario(2, cifrado, HMACUtil.GenerarHuella("vet@huellitas.mx"));
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        initializer.run();

        assertEquals(cifrado, usuario.getCorreo());
        verify(usuarioRepository, never()).save(any(UsuarioModel.class));
    }

    @Test
    void omiteValoresQueNoSonCorreoNiCifradoValidoParaEvitarDobleCifrado() {
        // Base64 válido con longitud suficiente, pero no descifrable con la clave actual.
        String ajeno = java.util.Base64.getEncoder().encodeToString(new byte[64]);
        UsuarioModel usuario = usuario(3, ajeno, "hmac");
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        initializer.run();

        assertEquals(ajeno, usuario.getCorreo(),
            "Un valor no descifrable no debe volver a cifrarse");
        verify(usuarioRepository, never()).save(any(UsuarioModel.class));
    }
}
