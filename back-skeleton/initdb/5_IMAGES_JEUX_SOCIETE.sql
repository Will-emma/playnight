-- Complète uniquement les illustrations absentes ou temporaires, sans remplacer les images
-- personnalisées ni recréer les jeux supprimés. Rejouable sur une base existante.
BEGIN;

UPDATE jeux AS jeu
SET image = illustration.image
FROM (VALUES
    ('catan', '/assets/jeux/catan.png'),
    ('dixit', '/assets/jeux/dixit.png'),
    ('codenames', '/assets/jeux/codenames.png'),
    ('7 wonders', '/assets/jeux/7-wonders.png')
) AS illustration(nom, image)
WHERE jeu.type = 'SOCIETE'
  AND lower(btrim(jeu.nom)) = illustration.nom
  AND (jeu.image IS NULL OR btrim(jeu.image) = ''
       OR jeu.image LIKE 'https://placehold.co/%'
       OR jeu.image LIKE 'http://placehold.co/%');

COMMIT;
