#!/usr/bin/env fish

# Instalación local de Huellitas.
# - No usa credenciales por defecto.
# - Genera secretos criptográficos aleatorios en devops-proyect/.env (permisos 600).
# - Crea un usuario de base de datos con privilegios mínimos.
# - Usa el perfil local de Spring, que es el único que puede actualizar el esquema.

set -l REPO_URL https://github.com/Nightmare11512/Project-Huellitas-Veterinary
set -l DIR "$HOME/Documentos/programa/Project-Huellitas-Veterinary"

sudo pacman -Syu --noconfirm

if test -d $DIR
    echo "El proyecto ya existe, actualizando..."
    cd $DIR
    git pull --ff-only
else
    mkdir -p (dirname $DIR)
    cd (dirname $DIR)
    git clone $REPO_URL
    cd $DIR
end

sudo pacman -S --noconfirm --needed git fnm mariadb jdk21-openjdk avahi nss-mdns openssl gnupg
sudo systemctl enable --now avahi-daemon

# Habilitar mDNS en nsswitch
if not grep -q "mdns_minimal" /etc/nsswitch.conf
    sudo sed -i 's/resolve/mdns_minimal resolve/' /etc/nsswitch.conf
end

set -l BACKEND "$DIR/devops-proyect"
set -l FRONTEND "$DIR/devops-frontend"
set -l ENV_FILE "$BACKEND/.env"

# ---------------------------------------------------------------
# Secretos: reutilizar .env si ya existe; si no, generar nuevos.
# ---------------------------------------------------------------
if test -f $ENV_FILE
    source $ENV_FILE
end

if not set -q SPRING_DATASOURCE_PASSWORD; or test -z "$SPRING_DATASOURCE_PASSWORD"
    set -gx SPRING_DATASOURCE_PASSWORD (openssl rand -hex 24)
end
if not set -q AES_SECRET; or test -z "$AES_SECRET"
    set -gx AES_SECRET (openssl rand -hex 32)
end
if not set -q HMAC_SECRET; or test -z "$HMAC_SECRET"
    set -gx HMAC_SECRET (openssl rand -hex 32)
end
if not set -q JWT_SECRET; or test -z "$JWT_SECRET"
    set -gx JWT_SECRET (openssl rand -hex 32)
end

set -l DB_PASSWORD "$SPRING_DATASOURCE_PASSWORD"
set -l DB_USER huellitas
set -l DB_NAME huellitas_db

# ---------------------------------------------------------------
# MariaDB
# ---------------------------------------------------------------
fnm env --use-on-cd | source

if not test -d /var/lib/mysql/mysql
    sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql
end

set -l MARIADB_CONF "/etc/my.cnf.d/server.cnf"
if not grep -q "lower_case_table_names" $MARIADB_CONF
    echo "Configurando insensibilidad a mayúsculas en MariaDB..."
    printf '\n[mysqld]\nlower_case_table_names=1\n' | sudo tee -a $MARIADB_CONF > /dev/null
end

sudo systemctl enable --now mariadb

# Privilegios mínimos: la aplicación no puede conceder permisos ni borrar la base.
sudo mariadb -e "
CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
ALTER USER '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES, TRIGGER ON $DB_NAME.* TO '$DB_USER'@'localhost';
DROP USER IF EXISTS 'Admin'@'localhost';
FLUSH PRIVILEGES;
"

# Conexión local para respaldos, sin exponer la contraseña en la línea de comandos.
printf '[client]\nuser=%s\npassword=%s\n' "$DB_USER" "$DB_PASSWORD" > $HOME/.my.cnf
chmod 600 $HOME/.my.cnf

# ---------------------------------------------------------------
# Archivo .env (nunca se versiona, permisos 600)
# ---------------------------------------------------------------
printf '%s\n' \
    "SPRING_DATASOURCE_URL=jdbc:mariadb://localhost:3306/$DB_NAME" \
    "SPRING_DATASOURCE_USERNAME=$DB_USER" \
    "SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD" \
    "AES_SECRET=$AES_SECRET" \
    "HMAC_SECRET=$HMAC_SECRET" \
    "JWT_SECRET=$JWT_SECRET" \
    "TURNSTILE_ENABLED=false" \
    "APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173" \
    "APP_SECURITY_ATTEMPTS_MAX=5" \
    "APP_SECURITY_ATTEMPTS_LOCKOUT_MINUTES=15" \
    > $ENV_FILE
chmod 600 $ENV_FILE

# ---------------------------------------------------------------
# Restaurar el respaldo más reciente, si existe.
# ---------------------------------------------------------------
set -l BACKUP_DIR "$HOME/backups"
if test -d "$BACKUP_DIR"
    set -l BACKUPS (ls -t $BACKUP_DIR/huellitas_*.sql.gpg 2>/dev/null)
    if test (count $BACKUPS) -gt 0
        echo "📁 Respaldo encontrado: $BACKUPS[1]"
        fish "$DIR/scripts/restore.fish" "$BACKUPS[1]"
    else
        echo "No hay respaldos cifrados. Spring creará el esquema con el perfil local."
    end
end

# ---------------------------------------------------------------
# Frontend
# ---------------------------------------------------------------
cd $FRONTEND
fnm install 24
fnm use 24
npm ci
npm run build
npm run dev -- --host 0.0.0.0 </dev/null > /tmp/frontend.log 2>&1 &

# ---------------------------------------------------------------
# Backend (perfil local: único autorizado a actualizar el esquema)
# ---------------------------------------------------------------
cd $BACKEND
set -gx SPRING_PROFILES_ACTIVE local
./mvnw spring-boot:run </dev/null > /tmp/backend.log 2>&1 &

echo "✅ Frontend corriendo - logs en /tmp/frontend.log"
echo "✅ Backend corriendo - logs en /tmp/backend.log"
echo "🔐 Secretos guardados en $ENV_FILE (chmod 600)"
echo "🌐 Frontend: http://localhost:5173"
echo "🚀 Backend: http://localhost:8080"
