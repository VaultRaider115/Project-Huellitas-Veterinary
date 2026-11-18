package com.Proyect_DevOps.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Límite de intentos por cuenta para autenticación y verificación MFA.
 * Mantiene el estado en memoria, con expiración automática de las entradas
 * inactivas. En despliegues con más de una réplica debe sustituirse por un
 * almacén compartido (por ejemplo Redis) para que el límite sea global.
 *
 * @author Equipo DevOps Huellitas
 * @version 1.0
 */
@Service
public class AttemptLimitService {

    private static final int CLEANUP_INTERVAL = 256;

    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();
    private final int maxAttempts;
    private final Duration lockout;

    /**
     * Constructor con la política configurable de intentos.
     *
     * @param maxAttempts Número máximo de fallos antes del bloqueo.
     * @param lockoutMinutes Minutos que dura el bloqueo.
     */
    public AttemptLimitService(
            @Value("${app.security.attempts.max:5}") int maxAttempts,
            @Value("${app.security.attempts.lockout-minutes:15}") long lockoutMinutes) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("El número máximo de intentos debe ser mayor que cero");
        }
        if (lockoutMinutes < 1) {
            throw new IllegalArgumentException("La duración del bloqueo debe ser mayor que cero");
        }
        this.maxAttempts = maxAttempts;
        this.lockout = Duration.ofMinutes(lockoutMinutes);
    }

    /**
     * Indica si una clave está temporalmente bloqueada.
     *
     * @param key Clave de la cuenta o flujo (por ejemplo {@code login:correo}).
     * @return {@code true} si debe rechazarse el intento.
     */
    public boolean isBlocked(String key) {
        AttemptState state = attempts.get(normalize(key));
        if (state == null) {
            return false;
        }
        synchronized (state) {
            if (state.blockedUntil == null) {
                return false;
            }
            if (state.blockedUntil.isAfter(Instant.now())) {
                return true;
            }
            attempts.remove(normalize(key), state);
            return false;
        }
    }

    /**
     * Segundos restantes de bloqueo, para la cabecera {@code Retry-After}.
     *
     * @param key Clave de la cuenta o flujo.
     * @return Segundos restantes, o 0 si no está bloqueada.
     */
    public long retryAfterSeconds(String key) {
        AttemptState state = attempts.get(normalize(key));
        if (state == null) {
            return 0;
        }
        synchronized (state) {
            if (state.blockedUntil == null || !state.blockedUntil.isAfter(Instant.now())) {
                return 0;
            }
            long seconds = Duration.between(Instant.now(), state.blockedUntil).toSeconds();
            return Math.max(1, seconds);
        }
    }

    /**
     * Registra un intento fallido y bloquea la clave al alcanzar el máximo.
     *
     * @param key Clave de la cuenta o flujo.
     */
    public void recordFailure(String key) {
        String normalized = normalize(key);
        AttemptState state = attempts.computeIfAbsent(normalized, ignored -> new AttemptState());
        synchronized (state) {
            Instant now = Instant.now();
            if (state.blockedUntil != null && state.blockedUntil.isAfter(now)) {
                return;
            }
            if (state.blockedUntil != null) {
                state.blockedUntil = null;
                state.failures = 0;
            }
            state.failures++;
            state.lastFailure = now;
            if (state.failures >= maxAttempts) {
                state.blockedUntil = now.plus(lockout);
            }
        }
        cleanup();
    }

    /**
     * Limpia los intentos fallidos tras una autenticación correcta.
     *
     * @param key Clave de la cuenta o flujo.
     */
    public void reset(String key) {
        attempts.remove(normalize(key));
    }

    private String normalize(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("La clave de intentos es obligatoria");
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }

    private void cleanup() {
        if (operations.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> {
            AttemptState state = entry.getValue();
            synchronized (state) {
                if (state.blockedUntil != null) {
                    return !state.blockedUntil.isAfter(now.minus(lockout));
                }
                return state.lastFailure != null
                    && state.lastFailure.isBefore(now.minus(lockout));
            }
        });
    }

    private static final class AttemptState {
        private int failures;
        private Instant lastFailure;
        private Instant blockedUntil;
    }
}
