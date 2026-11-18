#!/usr/bin/env fish

# Respaldo cifrado de la base de datos.
# - Cifra el volcado con AES-256 (GPG simétrico).
# - Guarda una suma SHA-256 para detectar corrupción o manipulación.
# - Elimina el volcado en claro al terminar.
#
# La clave se guarda en ~/.config/huellitas/backup.key (chmod 600).
# Consérvela fuera del servidor: sin ella los respaldos no se pueden restaurar.

set -l BACKUP_DIR "$HOME/backups"
set -l KEY_DIR "$HOME/.config/huellitas"
set -l KEY_FILE "$KEY_DIR/backup.key"
if set -q BACKUP_KEY_FILE
    set KEY_FILE "$BACKUP_KEY_FILE"
end
set -l RETENTION_DAYS 30
set -l DB_NAME huellitas_db

mkdir -p $BACKUP_DIR $KEY_DIR
chmod 700 $KEY_DIR

if not test -f $KEY_FILE
    openssl rand -base64 48 > $KEY_FILE
    chmod 600 $KEY_FILE
    echo "🔑 Clave de respaldo creada en $KEY_FILE"
    echo "   Guárdela en un lugar seguro y fuera de este servidor."
end

if not test -f "$HOME/.my.cnf"
    echo "❌ Falta ~/.my.cnf. Ejecute scripts/instalarelproyecto.fish primero." >&2
    exit 1
end

set -l STAMP (date +%Y-%m-%d_%H-%M-%S)
set -l RAW "$BACKUP_DIR/huellitas_$STAMP.sql"
set -l ENC "$RAW.gpg"
set -l BASE (basename $ENC)

if not mariadb-dump --defaults-extra-file=$HOME/.my.cnf --single-transaction --quick --triggers $DB_NAME > $RAW
    rm -f $RAW
    echo "❌ Error al generar el volcado de la base de datos." >&2
    exit 1
end

if not gpg --batch --pinentry-mode loopback --yes --symmetric --cipher-algo AES256 --passphrase-file $KEY_FILE --output $ENC $RAW
    rm -f $RAW $ENC
    echo "❌ Error al cifrar el respaldo." >&2
    exit 1
end

# El volcado en claro no debe sobrevivir al proceso.
rm -f $RAW

cd $BACKUP_DIR
sha256sum $BASE > "$BASE.sha256"
cd - > /dev/null

# Retención: eliminar respaldos y sumas antiguos.
find $BACKUP_DIR -name 'huellitas_*.sql.gpg' -mtime +$RETENTION_DAYS -delete
find $BACKUP_DIR -name 'huellitas_*.sql.gpg.sha256' -mtime +$RETENTION_DAYS -delete

echo "✅ Respaldo cifrado: $ENC"
echo "✅ Suma SHA-256: $ENC.sha256"
