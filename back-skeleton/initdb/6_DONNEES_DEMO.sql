-- ==========================================================
-- PlayNight : données de démo complémentaires (octobre 2026)
-- Soirées du ticket #41, soirée Discord du 22/10 et jeux vidéo importés depuis RAWG,
-- pour que les classements, podiums et graphiques soient remplis dès l'installation.
-- Bases neuves ET bases existantes : rejouable sans doublon.
--   - un jeu ou un joueur déjà présent (même nom) est conservé tel quel ;
--   - une soirée déjà présente (même date et même lieu) est ignorée avec ses parties.
-- Les classements Elo sont recalculés par l'API à son démarrage.
-- ==========================================================
BEGIN;

LOCK TABLE jeux, joueurs, soirees, soiree_joueur, parties, scores IN SHARE ROW EXCLUSIVE MODE;

DO $$
DECLARE jeu record; soiree record; partie jsonb; resultat jsonb;
        _SOIREE bigint; _PARTIE bigint; _JEU bigint; _JOUEUR bigint;
BEGIN
    -- Joueur créé depuis l'application
    INSERT INTO joueurs (pseudo, avatar)
    SELECT 'Weedy', '/assets/avatars/avatar-01.png'
    WHERE NOT EXISTS (SELECT 1 FROM joueurs WHERE pseudo = 'Weedy');

    -- Jeux vidéo (images RAWG)
    FOR jeu IN SELECT * FROM (VALUES
        ('EA SPORTS FC 27', 'Nintendo Switch, Xbox One, PlayStation 4, Xbox Series S/X', 1, 4,
         'https://media.rawg.io/media/screenshots/d9b/d9b0ef34a249647310c5bc7577ce68e3.jpg', NULL),
        ('Tekken 8', 'PC, PlayStation 5, Xbox Series S/X', 1, 4,
         'https://media.rawg.io/media/games/ed3/ed3a5e9fab79022979de9ef420137f73.jpg', NULL),
        ('Valorant', 'PC, Xbox Series S/X, PlayStation 5', 1, 4,
         'https://media.rawg.io/media/games/b11/b11127b9ee3c3701bd15b9af3286d20e.jpg', NULL),
        ('League of Legends', 'macOS, PC', 1, 4,
         'https://media.rawg.io/media/games/78b/78bc81e247fc7e77af700cbd632a9297.jpg', NULL),
        ('Fortnite Battle Royale', 'PlayStation 4, Xbox One, iOS, PC', 1, 4,
         'https://media.rawg.io/media/games/dcb/dcbb67f371a9a28ea38ffd73ee0f53f3.jpg', NULL),
        ('Forza Horizon 5', 'PC, iOS, Xbox One, PlayStation 5', 1, 4,
         'https://media.rawg.io/media/games/082/082365507ff04d456c700157072d35db.jpg', NULL),
        ('Overcooked! 2', 'PC, PlayStation 4, Xbox One, Nintendo Switch', 1, 4,
         'https://media.rawg.io/media/games/d11/d11470677a829e34562e95a4002c2c7f.jpg',
         'Cuisine coopérative ou versus, de un à quatre joueurs. La démonstration utilise le versus en deux équipes de deux.'),
        ('It Takes Two', 'PC, PlayStation 4/5, Xbox One/Series, Nintendo Switch', 2, 2,
         'https://media.rawg.io/media/games/d47/d479582ed0a46496ad34f65c7099d7e5.jpg',
         'Aventure exclusivement coopérative à deux joueurs, en local ou en ligne. Aucun résultat compétitif fictif dans la démonstration.')
    ) AS v(nom, plateforme, minimum, maximum, image, description) LOOP
        INSERT INTO jeux (nom, type, plateforme, nb_joueurs_min, nb_joueurs_max, image, description)
        SELECT jeu.nom, 'JEU_VIDEO', jeu.plateforme, jeu.minimum, jeu.maximum, jeu.image, jeu.description
        WHERE NOT EXISTS (SELECT 1 FROM jeux WHERE type = 'JEU_VIDEO' AND lower(btrim(nom)) = lower(jeu.nom));
    END LOOP;

    -- Soirées : [pseudo, points, rang, numéro d'équipe] pour chaque score
    FOR soiree IN SELECT * FROM (VALUES
        (timestamp '2026-10-09 20:00', 'Vendredi chez Léa',
         ARRAY['Léa','MaxPower','Samouraï','Inès','TomTom','Noa'],
         '[{"jeu":"catan","duree":70,"scores":[["Léa",10,1,null],["MaxPower",8,2,null],["Samouraï",7,3,null],["Inès",5,4,null]]},
           {"jeu":"uno","duree":20,"scores":[["Léa",0,1,null],["Inès",18,2,null],["TomTom",31,3,null],["Noa",45,4,null]]}]'::jsonb),
        (timestamp '2026-10-11 15:00', 'Dimanche chez Max',
         ARRAY['Léa','MaxPower','Samouraï','Inès','TomTom','Noa'],
         '[{"jeu":"catan","duree":65,"scores":[["Léa",10,1,null],["Noa",9,2,null],["TomTom",7,3,null],["MaxPower",6,4,null]]},
           {"jeu":"undercover","duree":18,"scores":[["Léa",null,1,1],["MaxPower",null,1,1],["Inès",null,1,1],["TomTom",null,1,1],["Samouraï",null,2,2],["Noa",null,2,2]]}]'::jsonb),
        (timestamp '2026-10-17 16:00', 'Tournoi du samedi',
         ARRAY['Léa','MaxPower','Samouraï','Inès','TomTom','Noa'],
         '[{"jeu":"catan","duree":80,"scores":[["Léa",10,1,null],["Samouraï",9,2,null],["Noa",7,3,null],["Inès",6,4,null]]},
           {"jeu":"echecs","duree":32,"scores":[["Léa",null,1,null],["MaxPower",null,2,null]]},
           {"jeu":"overcooked! 2","video":true,"duree":12,"scores":[["Inès",840,1,1],["Noa",840,1,1],["Léa",690,2,2],["TomTom",690,2,2]]}]'::jsonb),
        (timestamp '2026-10-22 17:00', 'En ligne (Discord)',
         ARRAY['MaxPower','Samouraï','Noa','Weedy'],
         '[{"jeu":"tekken 8","video":true,"duree":120,"scores":[["Weedy",10,1,null],["Samouraï",5,2,null],["Noa",7,3,null],["MaxPower",3,4,null]]},
           {"jeu":"ea sports fc 27","video":true,"duree":60,"scores":[["Samouraï",9,1,null],["Weedy",9,1,null],["MaxPower",3,2,null],["Noa",3,2,null]]}]'::jsonb),
        (timestamp '2026-10-23 20:30', 'Vendredi manettes',
         ARRAY['Léa','MaxPower','Samouraï','Inès','TomTom','Noa'],
         '[{"jeu":"catan","duree":60,"scores":[["Léa",10,1,null],["MaxPower",9,2,null],["TomTom",9,2,null],["Samouraï",6,4,null]]},
           {"jeu":"ea sports fc 27","video":true,"duree":18,"scores":[["Léa",3,1,1],["Samouraï",3,1,1],["MaxPower",3,1,2],["TomTom",3,1,2]]}]'::jsonb)
    ) AS v(date, lieu, invites, parties) LOOP
        CONTINUE WHEN EXISTS (SELECT 1 FROM soirees WHERE date = soiree.date AND lieu = soiree.lieu);

        INSERT INTO soirees (date, lieu) VALUES (soiree.date, soiree.lieu) RETURNING id INTO _SOIREE;
        INSERT INTO soiree_joueur (soiree_id, joueur_id)
        SELECT _SOIREE, id FROM joueurs WHERE pseudo = ANY (soiree.invites);

        FOR partie IN SELECT * FROM jsonb_array_elements(soiree.parties) LOOP
            -- Échecs s'écrit avec un accent : comparaison sans accent ni casse
            SELECT id INTO STRICT _JEU FROM jeux
            WHERE translate(lower(btrim(nom)), 'éèêë', 'eeee') = partie->>'jeu'
              AND type = CASE WHEN (partie->>'video')::boolean THEN 'JEU_VIDEO' ELSE 'SOCIETE' END;
            INSERT INTO parties (duree, soiree_id, jeu_id)
            VALUES ((partie->>'duree')::integer, _SOIREE, _JEU) RETURNING id INTO _PARTIE;

            FOR resultat IN SELECT * FROM jsonb_array_elements(partie->'scores') LOOP
                SELECT id INTO STRICT _JOUEUR FROM joueurs WHERE pseudo = resultat->>0;
                INSERT INTO scores (points, rang, numero_equipe, partie_id, joueur_id)
                VALUES ((resultat->>1)::integer, (resultat->>2)::integer, (resultat->>3)::integer, _PARTIE, _JOUEUR);
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

COMMIT;
