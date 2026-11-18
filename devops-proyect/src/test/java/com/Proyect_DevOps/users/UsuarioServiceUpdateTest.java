package com.Proyect_DevOps.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.Proyect_DevOps.MFA.MFAService;
import com.Proyect_DevOps.sucursales.repositories.SucursalRepository;
import com.Proyect_DevOps.users.models.UsuarioModel;
import com.Proyect_DevOps.users.repositories.RolRepository;
import com.Proyect_DevOps.users.repositories.UsuarioRepository;
import com.Proyect_DevOps.users.services.UsuarioService;

import jakarta.persistence.EntityManager;

class UsuarioServiceUpdateTest {

    private UsuarioService usuarioService;
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() throws Exception {
        usuarioRepository = mock(UsuarioRepository.class);
        usuarioService = new UsuarioService();

        setField(usuarioService, "repository", usuarioRepository);
        setField(usuarioService, "rolRepository", mock(RolRepository.class));
        setField(usuarioService, "sucursalRepository", mock(SucursalRepository.class));
        setField(usuarioService, "service", mock(MFAService.class));
        setField(usuarioService, "entityManager", mock(EntityManager.class));
        setField(usuarioService, "argon2PasswordEncoder", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        setField(usuarioService, "bCryptPasswordEncoder", new BCryptPasswordEncoder(12));
        setField(usuarioService, "passwordEncoder", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
    }

    @Test
    void partialUpdateKeepsPasswordEmailAndMfaValues() {
        UsuarioModel actual = new UsuarioModel();
        actual.setIdUsuario(5);
        actual.setNombre("Ana");
        actual.setCorreo("correo-cifrado");
        actual.setCorreoHMAC("huella-hmac");
        actual.setContraseña("$argon2id$hash-existente");
        actual.setMFANeeded(true);
        actual.setSecretoMFA("secreto-cifrado");

        when(usuarioRepository.findById(5)).thenReturn(Optional.of(actual));
        when(usuarioRepository.save(any(UsuarioModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioModel cambios = new UsuarioModel();
        cambios.setNombre("Ana María");

        UsuarioModel guardado = usuarioService.update(5, cambios).orElseThrow();

        assertEquals("Ana María", guardado.getNombre());
        assertEquals("$argon2id$hash-existente", guardado.getContraseña());
        assertEquals("correo-cifrado", guardado.getCorreo());
        assertEquals("huella-hmac", guardado.getCorreoHMAC());
        assertEquals("secreto-cifrado", guardado.getSecretoMFA());
        assertEquals(true, guardado.isMFANeeded());
    }

    @Test
    void rejectsPreHashedPasswordFromClient() {
        UsuarioModel actual = new UsuarioModel();
        actual.setIdUsuario(5);
        actual.setContraseña("$argon2id$hash-existente");

        when(usuarioRepository.findById(5)).thenReturn(Optional.of(actual));

        UsuarioModel cambios = new UsuarioModel();
        cambios.setContraseña("$argon2id$hash-inyectado");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> usuarioService.update(5, cambios));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(usuarioRepository, never()).save(any(UsuarioModel.class));
    }

    @Test
    void mfaUpdateOnlyWritesMfaColumns() {
        UsuarioModel usuario = new UsuarioModel();
        usuario.setIdUsuario(9);
        usuario.setSecretoMFA("activo");
        usuario.setSecretoPendienteMFA("pendiente");
        usuario.setMFANeeded(true);

        usuarioService.actualizarMFA(usuario);

        verify(usuarioRepository).actualizarCamposMFA(9, "activo", "pendiente", true);
        verify(usuarioRepository, never()).save(any(UsuarioModel.class));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> current = target.getClass();
        Field field = null;
        while (current != null && field == null) {
            try {
                field = current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(fieldName);
        }
        field.setAccessible(true);
        field.set(target, value);
    }
}
