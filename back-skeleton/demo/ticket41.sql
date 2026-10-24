-- Démonstration facultative #41. Ne jamais placer dans initdb/.
-- Prérequis : initdb/1, 2, 3 puis migrations société 4 et 5 (PR indépendante).
-- Les éditions/modes retenus et leurs sources sont documentés dans README-ticket41.md.
\set ON_ERROR_STOP on
BEGIN;

DO $$ BEGIN
    IF current_database() <> 'playnight_demo_ticket41'
       AND current_database() !~ '^playnight_demo_ticket41_test_[a-z0-9_]+$' THEN
        RAISE EXCEPTION 'Ticket #41 : base interdite (%). Utiliser une base dédiée ticket41.', current_database();
    END IF;
END $$;

-- Sérialiser les exécutions concurrentes et éviter les identités ambiguës.
LOCK TABLE jeux, joueurs, soirees, soiree_joueur, parties, scores IN SHARE ROW EXCLUSIVE MODE;

DO $$
DECLARE nom_attendu text;
BEGIN
    IF NOT EXISTS (SELECT FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'jeux' AND column_name = 'description') THEN
        RAISE EXCEPTION 'Prérequis absent : migration société 4_AJOUT_JEUX_SOCIETE.sql.';
    END IF;
    FOREACH nom_attendu IN ARRAY ARRAY['catan', 'dixit', 'codenames', '7 wonders', 'undercover', 'uno', 'dames', 'echecs'] LOOP
        IF (SELECT count(*) FROM jeux WHERE type = 'SOCIETE'
            AND translate(lower(btrim(nom)), 'éèêë', 'eeee') = nom_attendu) <> 1 THEN
            RAISE EXCEPTION 'Jeu de société absent ou ambigu : %. Appliquer les prérequis, sans recréer le jeu ici.', nom_attendu;
        END IF;
    END LOOP;
    FOREACH nom_attendu IN ARRAY ARRAY['Léa', 'MaxPower', 'Samouraï', 'Inès', 'TomTom', 'Noa'] LOOP
        IF NOT EXISTS (SELECT FROM joueurs WHERE pseudo = nom_attendu) THEN
            RAISE EXCEPTION 'Joueur initial absent : %. Aucun joueur ni avatar ne sera recréé.', nom_attendu;
        END IF;
    END LOOP;
END $$;

-- Manifeste propre à cette démonstration : aucune colonne métier supplémentaire.
-- Les clés restent stables même si un lieu est renommé dans l'application.
CREATE SCHEMA IF NOT EXISTS demo_ticket41;
CREATE TABLE IF NOT EXISTS demo_ticket41.catalogue (
    cle text PRIMARY KEY,
    jeu_id bigint NOT NULL UNIQUE REFERENCES public.jeux(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS demo_ticket41.soirees (
    cle text PRIMARY KEY,
    soiree_id bigint NOT NULL UNIQUE REFERENCES public.soirees(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS demo_ticket41.parties (
    cle text PRIMARY KEY,
    partie_id bigint NOT NULL UNIQUE REFERENCES public.parties(id) ON DELETE CASCADE
);

DO $$
DECLARE jeu record; identifiant bigint; occurrences integer;
BEGIN
    FOR jeu IN SELECT * FROM (VALUES
        ('overcooked! 2', 'Overcooked! 2', 1, 4,
         'PC, PlayStation 4, Xbox One, Nintendo Switch', 'Overcooked%21+2',
         'Cuisine coopérative ou versus, de un à quatre joueurs. La démonstration utilise le versus en deux équipes de deux.'),
        ('it takes two', 'It Takes Two', 2, 2,
         'PC, PlayStation 4/5, Xbox One/Series, Nintendo Switch', 'It+Takes+Two',
         'Aventure exclusivement coopérative à deux joueurs, en local ou en ligne. Aucun résultat compétitif fictif dans la démonstration.'),
        ('fortnite', 'Fortnite', 2, 2,
         'PlayStation 4/5, Xbox One/Series — écran partagé à deux', 'Fortnite',
         'Mode représenté : deux joueurs locaux en écran partagé sur console. Cette limite ne désigne pas la population totale du lobby Battle Royale. Aucune partie compétitive artificielle dans la démonstration.'),
        ('ea sports fc 25', 'EA Sports FC 25', 1, 4,
         'PlayStation 4/5 — Coup d’envoi local', 'EA+Sports+FC+25',
         'Football en mode local de un à quatre joueurs humains, avec adversaires contrôlés par ordinateur si nécessaire. Démonstration : deux équipes de deux. Les modes Clubs en ligne ne sont pas représentés.')
    ) AS v(cle, nom, minimum, maximum, plateforme, illustration, description) LOOP
        SELECT count(*), min(id) INTO occurrences, identifiant FROM jeux
        WHERE type = 'JEU_VIDEO' AND lower(btrim(nom)) = jeu.cle;
        IF occurrences > 1 THEN
            RAISE EXCEPTION 'Jeu vidéo ambigu : % ; aucun doublon ne sera choisi arbitrairement.', jeu.nom;
        END IF;
        IF occurrences = 0 THEN
            INSERT INTO jeux(nom, type, plateforme, nb_joueurs_min, nb_joueurs_max, image, description)
            VALUES (jeu.nom, 'JEU_VIDEO', jeu.plateforme, jeu.minimum, jeu.maximum,
                    'https://placehold.co/640x360/151827/ffbf3c?text=' || jeu.illustration, jeu.description)
            RETURNING id INTO identifiant;
        ELSIF EXISTS (SELECT FROM jeux WHERE id = identifiant
                      AND (nb_joueurs_min <> jeu.minimum OR nb_joueurs_max <> jeu.maximum)) THEN
            RAISE EXCEPTION 'Limites existantes différentes pour % : conserver le jeu et vérifier son mode avant de continuer.', jeu.nom;
        END IF;
        IF EXISTS (SELECT FROM demo_ticket41.catalogue WHERE cle = jeu.cle AND jeu_id <> identifiant) THEN
            RAISE EXCEPTION 'Manifeste incohérent pour %.', jeu.nom;
        END IF;
        INSERT INTO demo_ticket41.catalogue VALUES (jeu.cle, identifiant) ON CONFLICT (cle) DO NOTHING;
    END LOOP;
END $$;

DO $$
DECLARE soiree record; sid bigint; partie jsonb; pid bigint; gid bigint; resultat jsonb; jid bigint;
BEGIN
    -- Dates locales TIMESTAMP, sans conversion UTC. Deux mois sont déjà couverts
    -- avec les soirées initiales de septembre ; les mois sans partie restent vides.
    FOR soiree IN SELECT * FROM (VALUES
        ('vendredi_lea', timestamp '2026-10-09 20:00', 'Vendredi chez Léa',
         '[{"cle":"catan_1","jeu":"catan","duree":70,"scores":[["Léa",10,1,null],["MaxPower",8,2,null],["Samouraï",7,3,null],["Inès",5,4,null]]},
           {"cle":"uno","jeu":"uno","duree":20,"scores":[["Léa",0,1,null],["Inès",18,2,null],["TomTom",31,3,null],["Noa",45,4,null]]}]'::jsonb),
        ('dimanche_max', timestamp '2026-10-11 15:00', 'Dimanche chez Max',
         '[{"cle":"catan_2","jeu":"catan","duree":65,"scores":[["Léa",10,1,null],["Noa",9,2,null],["TomTom",7,3,null],["MaxPower",6,4,null]]},
           {"cle":"undercover","jeu":"undercover","duree":18,"scores":[["Léa",null,1,1],["MaxPower",null,1,1],["Inès",null,1,1],["TomTom",null,1,1],["Samouraï",null,2,2],["Noa",null,2,2]]}]'::jsonb),
        ('tournoi_samedi', timestamp '2026-10-17 16:00', 'Tournoi du samedi',
         '[{"cle":"catan_3","jeu":"catan","duree":80,"scores":[["Léa",10,1,null],["Samouraï",9,2,null],["Noa",7,3,null],["Inès",6,4,null]]},
           {"cle":"echecs","jeu":"echecs","duree":32,"scores":[["Léa",null,1,null],["MaxPower",null,2,null]]},
           {"cle":"overcooked","jeu":"overcooked! 2","duree":12,"scores":[["Inès",840,1,1],["Noa",840,1,1],["Léa",690,2,2],["TomTom",690,2,2]]}]'::jsonb),
        ('manettes_vendredi', timestamp '2026-10-23 20:30', 'Vendredi manettes',
         '[{"cle":"catan_4","jeu":"catan","duree":60,"scores":[["Léa",10,1,null],["MaxPower",9,2,null],["TomTom",9,2,null],["Samouraï",6,4,null]]},
           {"cle":"fc25","jeu":"ea sports fc 25","duree":18,"scores":[["Léa",3,1,1],["Samouraï",3,1,1],["MaxPower",3,1,2],["TomTom",3,1,2]]}]'::jsonb)
    ) AS v(cle, date, lieu, parties) LOOP
        SELECT soiree_id INTO sid FROM demo_ticket41.soirees WHERE cle = soiree.cle;
        IF sid IS NULL THEN
            IF EXISTS (SELECT FROM soirees WHERE date = soiree.date AND lieu = soiree.lieu) THEN
                RAISE EXCEPTION 'Soirée homonyme non identifiée : % ; refus de modifier des données externes.', soiree.lieu;
            END IF;
            INSERT INTO soirees(date, lieu) VALUES(soiree.date, soiree.lieu) RETURNING id INTO sid;
            INSERT INTO demo_ticket41.soirees VALUES(soiree.cle, sid);
            INSERT INTO soiree_joueur(soiree_id, joueur_id)
            SELECT sid, id FROM joueurs WHERE pseudo IN ('Léa','MaxPower','Samouraï','Inès','TomTom','Noa');
        END IF;
        FOR partie IN SELECT * FROM jsonb_array_elements(soiree.parties) LOOP
            -- Une réexécution ne réécrit jamais les résultats, même modifiés à la main.
            IF EXISTS (SELECT FROM demo_ticket41.parties WHERE cle = partie->>'cle') THEN CONTINUE; END IF;
            SELECT id INTO STRICT gid FROM jeux
            WHERE translate(lower(btrim(nom)), 'éèêë', 'eeee') = partie->>'jeu'
              AND type = CASE WHEN partie->>'jeu' IN ('overcooked! 2','ea sports fc 25') THEN 'JEU_VIDEO' ELSE 'SOCIETE' END;
            INSERT INTO parties(duree, soiree_id, jeu_id)
            VALUES((partie->>'duree')::integer, sid, gid) RETURNING id INTO pid;
            INSERT INTO demo_ticket41.parties VALUES(partie->>'cle', pid);
            FOR resultat IN SELECT * FROM jsonb_array_elements(partie->'scores') LOOP
                SELECT id INTO STRICT jid FROM joueurs WHERE pseudo = resultat->>0;
                INSERT INTO scores(partie_id, joueur_id, points, rang, numero_equipe)
                VALUES(pid, jid, (resultat->>1)::integer, (resultat->>2)::integer, (resultat->>3)::integer);
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

-- Les vérifications s'exécutent aussi dans la transaction : tout échec annule tout.
\ir ticket41-verification.sql
COMMIT;
