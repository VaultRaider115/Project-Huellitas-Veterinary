package com.Proyect_DevOps.users.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.Proyect_DevOps.MFA.MFAService;
import com.Proyect_DevOps.common.security.PasswordPolicy;
import com.Proyect_DevOps.common.services.GenericServiceImpl;
import com.Proyect_DevOps.sucursales.repositories.SucursalRepository;
import com.Proyect_DevOps.users.models.RolModel;
import com.Proyect_DevOps.users.models.UsuarioModel;
import com.Proyect_DevOps.users.repositories.RolRepository;
import com.Proyect_DevOps.users.repositories.UsuarioRepository;
import com.Proyect_DevOps.utilities.AESUtil;
import com.Proyect_DevOps.utilities.HMACUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Servicio de negocio para la gestión integral de usuarios.
 * Administra el registro con cifrado AES-256 en reposo, indexación con HMAC-SHA256,
 * hash seguro de contraseñas con BCrypt, y validación de autenticación de login.
 *
 * @author Equipo DevOps Huellitas
 * @version 1.0
 */
@Service
public class UsuarioService extends GenericServiceImpl<UsuarioModel, Integer, UsuarioRepository> {

    /**
     * Repositorio de roles para vincular el rol a los usuarios.
     */
    @Autowired
    private RolRepository rolRepository;

    /**
     * Codificador seguro de contraseñas BCrypt.
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Argon2PasswordEncoder argon2PasswordEncoder;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private SucursalRepository sucursalRepository;

    @Autowired
    private MFAService service;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Hash de referencia usado para igualar el tiempo de respuesta del login
     * cuando el correo no existe. Se calcula una única vez por instancia.
     */
    private volatile String dummyHash;

    /**
     * {@inheritDoc}
     * Recupera todos los usuarios de la base de datos y descifra sus correos en memoria.
     */
    @Override
    public List<UsuarioModel> findAll() {
        List<UsuarioModel> usuarios = new ArrayList<>();
        for (UsuarioModel entidad : repository.findAll()) {
            try {
                entidad.setCorreo(AESUtil.desencriptar(entidad.getCorreo()));
            } catch (Exception e) {
                // ignore
            }
            usuarios.add(entidad);
        }
        return usuarios;
    }

    /**
     * {@inheritDoc}
     * Busca un usuario por su identificador numérico y descifra su correo electrónico.
     */
    @Override
    public Optional<UsuarioModel> findById(Integer id) {
        return repository.findById(id).map(entidad -> {
            try {
                entidad.setCorreo(AESUtil.desencriptar(entidad.getCorreo()));
            } catch (Exception e) {
                // Mantener correo si no se puede desencriptar
            }
            return entidad;
        });
    }

    /**
     * {@inheritDoc}
     * Valida que el correo no esté previamente registrado, asocia el rol correspondiente,
     * calcula la huella HMAC, cifra el correo con AES y codifica la contraseña con Argon2id antes de persistir.
     *
     * @throws RuntimeException Si el correo ya existe, el rol es inválido o no existe en la base de datos.
     */
    @Override
    public UsuarioModel save(UsuarioModel entity) {
        PasswordPolicy.requireStrong(entity.getContraseña());
        if (repository.existsByCorreoHMAC(HMACUtil.GenerarHuella(entity.getCorreo()))) {
            throw new RuntimeException("El correo que se registro ya existe");
        }
        if (entity.getRol() == null || entity.getRol().getIdRol() <= 0) {
            throw new RuntimeException("El rol es obligatorio");
        }
        Optional<RolModel> rolOpt = rolRepository.findById(entity.getRol().getIdRol());
        if (rolOpt.isPresent()) {
            RolModel rol = rolOpt.get();
            entity.setRol(rol);
        } else {
            throw new RuntimeException("El rol que ingreso no se encontro en la base de datos");
        }

        entity.setCorreoHMAC(HMACUtil.GenerarHuella(entity.getCorreo()));
        entity.setCorreo(AESUtil.encriptar(entity.getCorreo()));
        entity.setContraseña(passwordEncoder.encode(entity.getContraseña()));
        return repository.save(entity);
    }

    /**
     * {@inheritDoc}
     * Actualiza únicamente los campos enviados. Los campos omitidos (contraseña,
     * correo, rol, sucursal, MFA) conservan su valor almacenado, de modo que una
     * edición parcial no puede borrar credenciales ni datos cifrados.
     */
    @Override
    @Transactional
    public Optional<UsuarioModel> update(Integer id, UsuarioModel entity) {
        Optional<UsuarioModel> existenteOpt = repository.findById(id);
        if (existenteOpt.isEmpty()) {
            return Optional.empty();
        }

        UsuarioModel actual = existenteOpt.get();

        if (entity.getCorreo() != null && !entity.getCorreo().isBlank()) {
            String nuevoCorreo = entity.getCorreo().trim();
            String nuevoHmac = HMACUtil.GenerarHuella(nuevoCorreo);
            if (!nuevoHmac.equals(actual.getCorreoHMAC()) && repository.existsByCorreoHMAC(nuevoHmac)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
            }
            actual.setCorreo(AESUtil.encriptar(nuevoCorreo));
            actual.setCorreoHMAC(nuevoHmac);
        }

        if (entity.getNombre() != null) {
            actual.setNombre(entity.getNombre().trim());
        }
        if (entity.getPaterno() != null) {
            actual.setPaterno(entity.getPaterno().trim());
        }
        if (entity.getMaterno() != null) {
            actual.setMaterno(entity.getMaterno().trim());
        }

        String nuevaContrasena = entity.getContraseña();
        if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
            // Un cliente nunca debe poder inyectar un hash ya codificado.
            if (nuevaContrasena.startsWith("$")) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña debe enviarse en texto plano"
                );
            }
            PasswordPolicy.requireStrong(nuevaContrasena);
            actual.setContraseña(passwordEncoder.encode(nuevaContrasena));
        }

        if (entity.getRol() != null && entity.getRol().getIdRol() > 0) {
            RolModel rol = rolRepository.findById(entity.getRol().getIdRol())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El rol indicado no existe"));
            actual.setRol(rol);
        }

        if (entity.getSucursal() != null && entity.getSucursal().getIdSucursal() > 0) {
            actual.setSucursal(sucursalRepository.findById(entity.getSucursal().getIdSucursal())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La sucursal indicada no existe")));
        }

        // Los campos de MFA solo se modifican mediante /usuario/mfa.
        UsuarioModel guardado = repository.save(actual);
        // Se confirma el cambio y se separa la entidad antes de descifrar el correo
        // para que Hibernate no vuelva a escribir el texto plano en la columna cifrada.
        entityManager.flush();
        entityManager.detach(guardado);
        try {
            guardado.setCorreo(AESUtil.desencriptar(guardado.getCorreo()));
        } catch (Exception e) {
            // ignore
        }
        return Optional.of(guardado);
    }

    /**
     * {@inheritDoc}
     * Elimina el usuario identificado por su ID.
     */
    @Override
    public boolean deleteById(Integer id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    /**
     * Valida las credenciales proporcionadas para el inicio de sesión.
     * Busca al usuario por la huella HMAC del correo y compara la contraseña con el hash BCrypt almacenado.
     *
     * @param correo Correo electrónico introducido por el usuario.
     * @param contrasena Contraseña en texto plano proporcionada en la solicitud.
     * @return El objeto {@link UsuarioModel} validado si las credenciales son correctas.
     * @throws IllegalArgumentException Si el usuario no existe o las credenciales no coinciden.
     */
    public UsuarioModel validacionDeLogin(String correo, String contrasena, int mfacode){
        Optional<UsuarioModel> usuarioOpt = repository.findByCorreoHMAC(HMACUtil.GenerarHuella(correo));
        if (usuarioOpt.isPresent()){
            UsuarioModel usuario = usuarioOpt.get();
            String hash = usuario.getContraseña();
            if (contrasena != null && (contrasena.length() >= 8 && contrasena.length() <= 128)) {
                boolean match = false;
                boolean upgradeNeeded = false;

                if (hash != null && hash.startsWith("$argon2")) {
                    match = argon2PasswordEncoder.matches(contrasena, hash);
                } else if (hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"))) {
                    match = bCryptPasswordEncoder.matches(contrasena, hash);
                    if (match) {
                        upgradeNeeded = true;
                    }
                } else {
                    if (argon2PasswordEncoder.matches(contrasena, hash)) {
                        match = true;
                    } else if (bCryptPasswordEncoder.matches(contrasena, hash)) {
                        match = true;
                        upgradeNeeded = true;
                    }
                }

                if (match) {
                    if (upgradeNeeded) {
                        // Migración progresiva a Argon2id
                        usuario.setContraseña(argon2PasswordEncoder.encode(contrasena));
                        repository.save(usuario);
                    }
                    if (usuario.isMFANeeded() && mfacode != 0) {
                        if (usuario.getSecretoMFA() == null) {
                            throw new IllegalArgumentException("No se pudo verificar su MFA contacte con el administrador");
                        }
                        if (service.verificarCodigo(AESUtil.desencriptar(usuario.getSecretoMFA()), mfacode)) {
                            return usuario;
                        } else {
                            throw new IllegalArgumentException("Codigo incorrecto");
                        }
                    } else {
                        return usuario;
                    }
                }
            }
            throw new IllegalArgumentException("Correo o Contraseña Incorrectas");
        } else {
            // Comparación ficticia para que un usuario inexistente tarde lo mismo que
            // uno existente con contraseña incorrecta y no se pueda enumerar cuentas.
            if (contrasena != null && !contrasena.isBlank()) {
                argon2PasswordEncoder.matches(contrasena, hashFicticio());
            }
            throw new IllegalArgumentException("Correo o Contraseñas Incorrectas");
        }
    }

    /**
     * Hash de referencia usado solo para igualar el tiempo de respuesta cuando el
     * correo no existe. Se calcula una única vez por instancia.
     *
     * @return Hash Argon2id de un valor aleatorio.
     */
    private String hashFicticio() {
        String hash = dummyHash;
        if (hash == null) {
            synchronized (this) {
                if (dummyHash == null) {
                    dummyHash = argon2PasswordEncoder.encode(java.util.UUID.randomUUID().toString());
                }
                hash = dummyHash;
            }
        }
        return hash;
    }

    /**
     * Obtiene el identificador numérico del rol del usuario a partir de su correo electrónico.
     *
     * @param correo Correo del usuario a buscar.
     * @return Identificador del rol asociado al usuario, o -1 si el usuario no fue localizado.
     */
    public Integer buscarIdRol (String correo){
        Optional<UsuarioModel> usuarioOpt = repository.findByCorreoHMAC(HMACUtil.GenerarHuella(correo));
        if (usuarioOpt.isPresent()){
            UsuarioModel usuario = usuarioOpt.get();
            return usuario.getRol() != null ? usuario.getRol().getIdRol() : -1;
        } else {
            return -1;
        }
    }

    /**
     * Obtiene el nombre completo del usuario a partir de su correo electrónico.
     *
     * @param correo Correo del usuario a buscar.
     * @return Nombre y apellidos concatenados, o {@code "User not found"} si no existe.
     */
    public String extraerNombre(String correo){
        Optional<UsuarioModel> usuarioOpt = repository.findByCorreoHMAC(HMACUtil.GenerarHuella(correo));
        if (usuarioOpt.isPresent()){
            UsuarioModel usuario = usuarioOpt.get();
            return usuario.getNombre() + " " + (usuario.getPaterno() != null ? usuario.getPaterno() : "") + " " + (usuario.getMaterno() != null ? usuario.getMaterno() : "");
        } else {
            return "User not found";
        }
    }

    /**
     * Busca un usuario por su correo electrónico usando la huella HMAC.
     * Descifra el correo en memoria antes de retornar.
     *
     * @param correo Correo electrónico en texto plano.
     * @return El {@link UsuarioModel} encontrado, o {@code null} si no existe.
     */
    public UsuarioModel buscarPorCorreo(String correo) {
        Optional<UsuarioModel> usuarioOpt = repository.findByCorreoHMAC(HMACUtil.GenerarHuella(correo));
        if (usuarioOpt.isPresent()) {
            UsuarioModel usuario = usuarioOpt.get();
            try {
                usuario.setCorreo(AESUtil.desencriptar(usuario.getCorreo()));
            } catch (Exception e) {
                // Mantener correo si no se puede desencriptar
            }
            return usuario;
        }
        return null;
    }

    /**
     * Actualiza exclusivamente los campos relacionados con MFA del usuario
     * (secretoMFA, secretoPendienteMFA, mfaneed) sin alterar el cifrado del correo ni la contraseña.
     *
     * @param usuario Entidad con los campos MFA actualizados.
     */
    @Transactional
    public void actualizarMFA(UsuarioModel usuario) {
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario requerido");
        }
        repository.actualizarCamposMFA(
            usuario.getIdUsuario(),
            usuario.getSecretoMFA(),
            usuario.getSecretoPendienteMFA(),
            usuario.isMFANeeded()
        );
    }
}
