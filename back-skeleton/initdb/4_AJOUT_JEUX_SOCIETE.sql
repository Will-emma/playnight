-- Catalogue additionnel : bases neuves ET bases existantes.
-- Rejouable explicitement, sans modifier les jeux déjà présents.
-- Aucun réensemencement automatique au démarrage de l'API.
BEGIN;

ALTER TABLE jeux ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE jeux ADD COLUMN IF NOT EXISTS duree_estimee INT CHECK (duree_estimee > 0);

-- Sérialise aussi deux exécutions simultanées du script, sans imposer
-- de nouvelle contrainte d'unicité aux noms des jeux historiques.
LOCK TABLE jeux IN SHARE ROW EXCLUSIVE MODE;

INSERT INTO jeux (nom, type, plateforme, nb_joueurs_min, nb_joueurs_max, image, description, duree_estimee)
SELECT catalogue.nom, 'SOCIETE', NULL, catalogue.minimum, catalogue.maximum,
       catalogue.image, catalogue.description, catalogue.duree
FROM (VALUES
    ('Undercover', 'undercover', 3, 12, '/assets/jeux/undercover.png',
     'Décrivez votre mot secret, démasquez les infiltrés et bluffez pour protéger votre camp.', 15),
    ('UNO', 'uno', 2, 10, '/assets/jeux/uno.png',
     'Associez couleurs et valeurs pour vous débarrasser de vos cartes avant les autres joueurs.', 20),
    ('Dames', 'dames', 2, 2, '/assets/jeux/dames.png',
     'Déplacez vos pions en diagonale, capturez les pièces adverses et faites-les devenir dames.', 20),
    ('Échecs', 'echecs', 2, 2, '/assets/jeux/echecs.png',
     'Coordonnez vos pièces sur l’échiquier pour mettre le roi adverse échec et mat.', 30)
) AS catalogue(nom, cle, minimum, maximum, image, description, duree)
WHERE NOT EXISTS (
    SELECT 1 FROM jeux existant
    WHERE existant.type = 'SOCIETE'
      AND translate(lower(btrim(existant.nom)), 'éèêë', 'eeee') = catalogue.cle
);

COMMIT;
