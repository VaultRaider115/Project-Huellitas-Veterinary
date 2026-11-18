package com.Proyect_DevOps.bitacora;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.Proyect_DevOps.bitacora.models.BitacoraModel;
import com.Proyect_DevOps.bitacora.repositories.BitacoraRepository;
import com.Proyect_DevOps.bitacora.services.BitacoraService;

class BitacoraServiceSanitizationTest {

    @Test
    void removesControlCharactersAndRedactsSecrets() throws Exception {
        BitacoraRepository repository = mock(BitacoraRepository.class);
        when(repository.save(any(BitacoraModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        BitacoraService service = new BitacoraService();
        Field field = service.getClass().getSuperclass().getDeclaredField("repository");
        field.setAccessible(true);
        field.set(service, repository);

        BitacoraModel registro = service.registrarAccion(
            "usuario@example.com",
            "Admin\nfalso",
            "LOGIN_FALLIDO",
            "Autenticación",
            "fallo\npassword=super-secreto token=abc.def",
            "192.0.2.10"
        );

        assertFalse(registro.getDetalle().contains("\n"));
        assertFalse(registro.getDetalle().contains("super-secreto"));
        assertFalse(registro.getRol().contains("\n"));
        assertTrue(registro.getDetalle().contains("[REDACTADO]"));
    }
}
