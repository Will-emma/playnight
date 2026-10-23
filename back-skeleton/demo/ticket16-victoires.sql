-- Démonstration du ticket #16, exclusivement sur playnight_demo_ticket16.
-- Précondition : nouvelle base playnight_demo_ticket16 initialisée uniquement
-- avec les scripts actuels 1_TABLES.sql, 2_DEFAULT_ENTRIES.sql et 3_CLASSEMENTS.sql.
-- Ne modifie ni les migrations de production ni la base principale.
BEGIN;

DO $$
DECLARE
    jeu_id_demo BIGINT;
    joueur_ids BIGINT[] := ARRAY[]::BIGINT[];
    pseudos TEXT[] := ARRAY['Léa', 'MaxPower', 'Samouraï', 'Inès', 'TomTom', 'Noa', 'Camille Démo'];
    objectifs INT[] := ARRAY[4, 3, 3, 2, 1, 1, 1];
    id_courant BIGINT;
    soiree_demo BIGINT;
    partie_demo BIGINT;
    deja_gagne INT;
    supplement INT;
    numero_partie INT := 0;
    lieux_demo TEXT[] := ARRAY['Chez Léa', 'Soirée chez Tom', 'Vendredi jeux', 'Chez Max',
        'Le rendez-vous du samedi', 'Chez Inès', 'Jeux entre amis'];
    position_joueur INT;
    i INT;
    j INT;
    k INT;
BEGIN
    -- Le contrôle précède toute écriture, même si le fichier est exécuté par erreur.
    IF current_database() <> 'playnight_demo_ticket16' THEN
        RAISE EXCEPTION 'Exécution interdite sur la base % : base de démonstration exigée', current_database();
    END IF;

    SELECT id INTO STRICT jeu_id_demo FROM jeux WHERE nom = 'Catan' AND type = 'SOCIETE';
    INSERT INTO joueurs (pseudo, avatar)
    VALUES ('Camille Démo', '/assets/avatars/avatar-07.png')
    ON CONFLICT (pseudo) DO NOTHING;

    FOR i IN 1..array_length(pseudos, 1) LOOP
        SELECT id INTO STRICT id_courant FROM joueurs WHERE pseudo = pseudos[i];
        joueur_ids := array_append(joueur_ids, id_courant);
    END LOOP;

    -- Complète les victoires existantes (Léa possède déjà une victoire de démo).
    -- Une réexécution sans changement des données n'ajoute aucune partie.
    FOR i IN 1..array_length(joueur_ids, 1) LOOP
        SELECT count(*) INTO deja_gagne
        FROM scores s JOIN parties p ON p.id = s.partie_id
        WHERE p.jeu_id = jeu_id_demo AND s.joueur_id = joueur_ids[i] AND s.rang = 1;
        IF deja_gagne > objectifs[i] THEN
            RAISE EXCEPTION 'Le joueur % dépasse déjà la cible de démonstration', pseudos[i];
        END IF;
        supplement := objectifs[i] - deja_gagne;
        IF supplement > 0 THEN
            FOR j IN 1..supplement LOOP
                INSERT INTO soirees (date, lieu)
                VALUES (TIMESTAMP '2026-10-07 20:00:00' + numero_partie * INTERVAL '1 day', lieux_demo[(numero_partie % array_length(lieux_demo, 1)) + 1])
                RETURNING id INTO soiree_demo;
                numero_partie := numero_partie + 1;

                INSERT INTO parties (duree, soiree_id, jeu_id)
                VALUES (60, soiree_demo, jeu_id_demo) RETURNING id INTO partie_demo;

                -- Trois participants distincts : vainqueur puis deux adversaires.
                -- Les trois appartiennent à la soirée ; aucun résultat en équipe.
                FOR k IN 0..2 LOOP
                    position_joueur := ((i - 1 + k) % array_length(joueur_ids, 1)) + 1;
                    INSERT INTO soiree_joueur (soiree_id, joueur_id)
                    VALUES (soiree_demo, joueur_ids[position_joueur]);
                    INSERT INTO scores (points, rang, numero_equipe, partie_id, joueur_id)
                    VALUES (10 - 2 * k, k + 1, NULL, partie_demo, joueur_ids[position_joueur]);
                END LOOP;
            END LOOP;
        END IF;
    END LOOP;

    FOR i IN 1..array_length(joueur_ids, 1) LOOP
        SELECT count(*) INTO deja_gagne
        FROM scores s JOIN parties p ON p.id = s.partie_id
        WHERE p.jeu_id = jeu_id_demo AND s.joueur_id = joueur_ids[i] AND s.rang = 1;
        IF deja_gagne <> objectifs[i] THEN
            RAISE EXCEPTION 'Total incorrect pour % : % au lieu de %', pseudos[i], deja_gagne, objectifs[i];
        END IF;
    END LOOP;

    IF (SELECT count(DISTINCT s.joueur_id)
        FROM scores s JOIN parties p ON p.id = s.partie_id
        WHERE p.jeu_id = jeu_id_demo AND s.rang = 1) <> 7 THEN
        RAISE EXCEPTION 'La démonstration doit contenir exactement sept vainqueurs sur Catan';
    END IF;
END $$;

SELECT j.pseudo, COUNT(*) AS victoires,
       RANK() OVER (ORDER BY COUNT(*) DESC) AS rang
FROM scores s JOIN parties p ON p.id = s.partie_id
JOIN jeux jeu ON jeu.id = p.jeu_id JOIN joueurs j ON j.id = s.joueur_id
WHERE jeu.nom = 'Catan' AND jeu.type = 'SOCIETE' AND s.rang = 1
GROUP BY j.id, j.pseudo
ORDER BY victoires DESC, j.pseudo, j.id;

COMMIT;
