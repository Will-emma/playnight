#!/usr/bin/env bash
# Crée une base NEUVE uniquement ; ne réinitialise et ne supprime jamais une base.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DEMO_DIR="$ROOT/back-skeleton/demo"
DEMO_DB="${1:-playnight_demo_ticket41_test_$(date +%Y%m%d_%H%M%S)}"
CONTAINER="${TICKET41_CONTAINER:-playnight_database}"
SOCIETE_REF="${TICKET41_SOCIETE_REF:-7bbdb6d}"
PG_BIN="${TICKET41_PG_BIN:-}"

if [[ "$DEMO_DB" != playnight_demo_ticket41 && ! "$DEMO_DB" =~ ^playnight_demo_ticket41_test_[a-z0-9_]+$ ]]; then
    echo 'Refus : utiliser playnight_demo_ticket41 ou playnight_demo_ticket41_test_<suffixe>.' >&2
    exit 1
fi

# Aucun secret ne transite par les arguments ou les journaux de l'hôte.
db() {
    if [[ -n "$PG_BIN" ]]; then
        "$PG_BIN/psql" -X -v ON_ERROR_STOP=1 -d "$DEMO_DB" "$@"
    else
        docker exec -i "$CONTAINER" sh -c 'exec psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$1"' sh "$DEMO_DB"
    fi
}
demo() {
    # Résoudre le seul include local avant d'envoyer le SQL à psql dans Docker.
    # L'exécution directe psql -f ticket41.sql fonctionne également grâce à \ir.
    awk -v include="$DEMO_DIR/ticket41-verification.sql" '
        $0 == "\\ir ticket41-verification.sql" { while ((getline line < include) > 0) print line; close(include); next }
        { print }
    ' "$DEMO_DIR/ticket41.sql" | db
}
snapshot() {
    # Contenu applicatif et manifeste, ordonné ; les séquences sont contrôlées à part.
    db <<'SQL' | tail -1
\pset tuples_only on
\pset format unaligned
SELECT md5(string_agg(data, E'\n' ORDER BY data)) FROM (
 SELECT 'joueurs:' || to_jsonb(t)::text data FROM joueurs t UNION ALL
 SELECT 'jeux:' || to_jsonb(t)::text FROM jeux t UNION ALL
 SELECT 'soirees:' || to_jsonb(t)::text FROM soirees t UNION ALL
 SELECT 'participations:' || to_jsonb(t)::text FROM soiree_joueur t UNION ALL
 SELECT 'parties:' || to_jsonb(t)::text FROM parties t UNION ALL
 SELECT 'scores:' || to_jsonb(t)::text FROM scores t UNION ALL
 SELECT 'classements:' || to_jsonb(t)::text FROM classements t UNION ALL
 SELECT 'catalogue41:' || to_jsonb(t)::text FROM demo_ticket41.catalogue t UNION ALL
 SELECT 'soirees41:' || to_jsonb(t)::text FROM demo_ticket41.soirees t UNION ALL
 SELECT 'parties41:' || to_jsonb(t)::text FROM demo_ticket41.parties t UNION ALL
 SELECT 'sequences:' || to_jsonb(t)::text FROM pg_sequences t WHERE schemaname = 'public'
) contenu;
SQL
}

# Vérifier les prérequis Git AVANT de créer la base ; aucune extraction dans le dépôt.
for migration in 4_AJOUT_JEUX_SOCIETE.sql 5_IMAGES_JEUX_SOCIETE.sql; do
    git -C "$ROOT" cat-file -e "$SOCIETE_REF:back-skeleton/initdb/$migration"
done
if [[ -n "$PG_BIN" ]]; then
    "$PG_BIN/createdb" "$DEMO_DB"
else
    docker exec "$CONTAINER" sh -c 'exec createdb -U "$POSTGRES_USER" "$1"' sh "$DEMO_DB"
fi
echo "Base de test créée : $DEMO_DB (conservée après les tests)."
for initialisation in 1_TABLES.sql 2_DEFAULT_ENTRIES.sql 3_CLASSEMENTS.sql; do
    db < "$ROOT/back-skeleton/initdb/$initialisation"
done

# Vérifier le refus des prérequis absents. La transaction doit rester sans effet.
if demo > /dev/null 2>&1; then
    echo 'ÉCHEC : le script a accepté les migrations société absentes.' >&2; exit 1
fi
db <<'SQL'
DO $$ BEGIN
 IF to_regnamespace('demo_ticket41') IS NOT NULL OR (SELECT count(*) FROM jeux) <> 8 THEN
   RAISE EXCEPTION 'Le refus des prérequis a laissé des écritures.';
 END IF;
END $$;
SQL

for migration in 4_AJOUT_JEUX_SOCIETE.sql 5_IMAGES_JEUX_SOCIETE.sql; do
    git -C "$ROOT" show "$SOCIETE_REF:back-skeleton/initdb/$migration" | db
done

db <<'SQL'
CREATE SCHEMA ticket41_test;
CREATE TABLE ticket41_test.initial (table_name text NOT NULL, contenu jsonb NOT NULL);
DO $$ DECLARE nom text; BEGIN
 FOREACH nom IN ARRAY ARRAY['joueurs','jeux','soirees','soiree_joueur','parties','scores','classements'] LOOP
  EXECUTE format('INSERT INTO ticket41_test.initial SELECT %L, to_jsonb(t) FROM public.%I t', nom, nom);
 END LOOP;
END $$;
SQL

demo
premier="$(snapshot)"
demo
second="$(snapshot)"
[[ -n "$premier" && "$premier" = "$second" ]] || { echo 'ÉCHEC : réexécution différente.' >&2; exit 1; }
echo 'OK : réexécution strictement identique, séquences comprises.'

db <<'SQL'
DO $$ DECLARE nom text; manquants integer; BEGIN
 FOREACH nom IN ARRAY ARRAY['joueurs','jeux','soirees','soiree_joueur','parties','scores','classements'] LOOP
  EXECUTE format('SELECT count(*) FROM ticket41_test.initial b WHERE b.table_name = %L
    AND NOT EXISTS (SELECT FROM public.%I t WHERE to_jsonb(t) = b.contenu)', nom, nom) INTO manquants;
  IF manquants <> 0 THEN RAISE EXCEPTION 'Données initiales modifiées : %', nom; END IF;
 END LOOP;
 IF (SELECT count(*) FROM jeux) <> 16
    OR (SELECT count(*) FROM jeux WHERE type = 'SOCIETE') <> 8
    OR (SELECT count(*) FROM jeux WHERE type = 'JEU_VIDEO') <> 8
    OR (SELECT count(*) FROM joueurs) <> 6
    OR (SELECT count(*) FROM soirees) <> 7
    OR (SELECT count(*) FROM parties) <> 17
    OR (SELECT count(*) FROM scores) <> 71 THEN
  RAISE EXCEPTION 'Totaux de la démonstration neuve incorrects.';
 END IF;
 IF (SELECT array_agg(j.nom ORDER BY j.nom) FROM jeux j WHERE NOT EXISTS (SELECT FROM parties p WHERE p.jeu_id = j.id))
    <> ARRAY['Dames','Fortnite','It Takes Two'] THEN
  RAISE EXCEPTION 'Les trois jeux sans partie sont incorrects.';
 END IF;
 IF (SELECT count(*) FROM scores s JOIN parties p ON p.id = s.partie_id
     JOIN jeux g ON g.id = p.jeu_id JOIN joueurs j ON j.id = s.joueur_id
     WHERE g.nom = 'Catan' AND j.pseudo = 'Léa' AND s.rang = 1) <> 5 THEN
  RAISE EXCEPTION 'Léa doit avoir exactement cinq victoires à Catan.';
 END IF;
END $$;
SQL
echo 'OK : totaux, trois jeux sans partie et conservation intégrale des données initiales.'

# Une image RAWG ajoutée après coup et un lieu renommé ne doivent jamais être écrasés.
db <<'SQL'
UPDATE jeux SET image = 'https://media.rawg.io/ticket41-preservation-test.jpg'
 WHERE id = (SELECT jeu_id FROM demo_ticket41.catalogue WHERE cle = 'fortnite');
UPDATE soirees SET lieu = 'Chez Max après renommage'
 WHERE id = (SELECT soiree_id FROM demo_ticket41.soirees WHERE cle = 'dimanche_max');
UPDATE scores SET points = 1
 WHERE partie_id = (SELECT partie_id FROM demo_ticket41.parties WHERE cle = 'uno') AND rang = 1;
SQL
avant_personnalisation="$(snapshot)"
demo
[[ "$avant_personnalisation" = "$(snapshot)" ]] || { echo 'ÉCHEC : personnalisation écrasée.' >&2; exit 1; }
echo 'OK : image personnalisée, lieu renommé et score modifié conservés.'
# Rétablir uniquement ces personnalisations du test, jamais des données initiales.
db <<'SQL'
UPDATE jeux SET image = 'https://placehold.co/640x360/151827/ffbf3c?text=Fortnite'
 WHERE id = (SELECT jeu_id FROM demo_ticket41.catalogue WHERE cle = 'fortnite');
UPDATE soirees SET lieu = 'Dimanche chez Max'
 WHERE id = (SELECT soiree_id FROM demo_ticket41.soirees WHERE cle = 'dimanche_max');
UPDATE scores SET points = 0
 WHERE partie_id = (SELECT partie_id FROM demo_ticket41.parties WHERE cle = 'uno') AND rang = 1;
SQL

# Tester la détection d'une équipe incohérente sans laisser la corruption du test.
if {
    printf 'BEGIN; UPDATE scores SET rang = 2 WHERE id = (SELECT min(s.id) FROM scores s JOIN demo_ticket41.parties d ON d.partie_id = s.partie_id WHERE d.cle = '\''overcooked'\'');\n'
    cat "$DEMO_DIR/ticket41-verification.sql"
    printf 'ROLLBACK;\n'
} | db > /dev/null 2>&1; then
    echo 'ÉCHEC : équipe incohérente acceptée.' >&2; exit 1
fi
[[ "$premier" = "$(snapshot)" ]] || { echo 'ÉCHEC : données finales différentes.' >&2; exit 1; }
echo 'OK : équipe incohérente rejetée et transaction annulée.'
echo "SUCCÈS ticket #41 — base disponible : $DEMO_DB"
