#!/usr/bin/env fish

# Restauración de un respaldo cifrado de Huellitas.
#
# Uso:
#   fish scripts/restore.fish /ruta/huellitas_2026-01-01_12-00-00.sql.gpg
#   fish scripts/restore.fish --latest
#
# Antes de importar verifica la suma SHA-256; después reaplica los disparadores
# append-only de la bitácora.

set -l BACKUP_DIR "$HOME/backups"
set -l KEY_FILE "$HOME/.config/huellitas/backup.key"
if set -q BACKUP_KEY_FILE
    set KEY_FILE "$BACKUP_KEY_FILE"
end
set -l DB_NAME huellitas_db
set -l SCRIPT_DIR (dirname (status --current-filename))

set -l FILE ""
if test (count $argv) -ge 1
    if test "$argv[1]" = "--latest"
        set FILE (ls -t $BACKUP_DIR/huellitas_*.sql.gpg 2>/dev/null | head -n 1)
    else
        set FILE "$argv[1]"
    end
end

if test -z "$FILE"; or not test -f "$FILE"
    echo "Uso: fish scripts/restore.fish <archivo.sql.gpg> | --latest" >&2
    exit 1
end

if not test -f $KEY_FILE
    echo "❌ No existe la clave de respaldo: $KEY_FILE" >&2
    exit 1
end

if not test -f "$HOME/.my.cnf"
    echo "❌ Falta ~/.my.cnf con credenciales de MariaDB." >&2
    exit 1
end

set -l FILE_DIR (dirname $FILE)
set -l BASE (basename $FILE)
set -l SUM_FILE "$FILE.sha256"

if test -f $SUM_FILE
    echo "🔎 Verificando integridad de $BASE..."
    cd $FILE_DIR
    if not sha256sum -c "$BASE.sha256"
        echo "❌ La suma SHA-256 no coincide. Restauración cancelada." >&2
        exit 1
    end
    cd - > /dev/null
else
    echo "⚠️  No se encontró $SUM_FILE; no se puede verificar la integridad." >&2
    exit 1
end

set -l TMP_FILE (mktemp /tmp/huellitas-restore.XXXXXX.sql)
if not gpg --batch --pinentry-mode loopback --yes --decrypt --passphrase-file $KEY_FILE --output $TMP_FILE $FILE
    rm -f $TMP_FILE
    echo "❌ No se pudo descifrar el respaldo." >&2
    exit 1
end

if not sudo mariadb $DB_NAME < $TMP_FILE
    rm -f $TMP_FILE
    echo "❌ Error al importar el respaldo." >&2
    exit 1
end

rm -f $TMP_FILE

# Reaplicar los controles de integridad que no viajan en el volcado.
if test -f "$SCRIPT_DIR/hardening.sql"
    sudo mariadb $DB_NAME < "$SCRIPT_DIR/hardening.sql"
end

echo "✅ Restauración completada desde $FILE"
