-- Enrichissement facultatif, réservé à la démonstration. Aucun jeu n'est créé.
-- Les résultats existants et les statistiques des sept joueurs de Catan sont conservés.
-- Exécuter API arrêtée ; son redémarrage reconstruit la projection Elo existante.
BEGIN;

DO $$
DECLARE
    lieux TEXT[] := ARRAY['Chez Léa', 'Soirée chez Tom', 'Vendredi jeux', 'Chez Max',
        'Le rendez-vous du samedi', 'Chez Inès', 'Jeux entre amis'];
    pseudos TEXT[] := ARRAY['Nina', 'Jules', 'Zoé', 'Alex'];
    ids BIGINT[] := ARRAY[]::BIGINT[];
    identifiant BIGINT;
    soiree_id_demo BIGINT;
    partie_id_demo BIGINT;
    jeu_id_demo BIGINT;
    minimum INT;
    maximum INT;
    nombre INT;
    definition JSONB;
    partie JSONB;
    i INT;
BEGIN
    IF current_database() <> 'playnight_demo_ticket16' THEN
        RAISE EXCEPTION 'Script réservé à playnight_demo_ticket16, base actuelle : %', current_database();
    END IF;

    -- Le champ lieu sert de titre dans l'application. Dates et résultats sont inchangés.
    WITH anciennes AS (
        SELECT id, row_number() OVER (ORDER BY date, id) AS numero
        FROM soirees WHERE lieu IN ('Démo ticket #16 · Catan', 'Démo ticket #16 • Catan')
    )
    UPDATE soirees s SET lieu = lieux[((a.numero - 1) % array_length(lieux, 1))::INT + 1]
    FROM anciennes a WHERE s.id = a.id;

    FOR i IN 1..array_length(pseudos, 1) LOOP
        INSERT INTO joueurs (pseudo, avatar)
        VALUES (pseudos[i], '/assets/avatars/avatar-' || lpad((i + 8)::TEXT, 2, '0') || '.png')
        ON CONFLICT (pseudo) DO NOTHING;
        SELECT id INTO STRICT identifiant FROM joueurs WHERE pseudo = pseudos[i];
        -- Une collision avec un participant existant ne doit jamais changer ses statistiques.
        IF EXISTS (SELECT 1 FROM scores WHERE joueur_id = identifiant)
           AND NOT EXISTS (SELECT 1 FROM soirees WHERE lieu = 'Soirée chez Nina' AND date = TIMESTAMP '2026-10-22 20:00:00') THEN
            RAISE EXCEPTION 'Le participant % possède déjà des résultats : vérifier avant de continuer', pseudos[i];
        END IF;
        ids := array_append(ids, identifiant);
    END LOOP;

    FOR definition IN SELECT value FROM jsonb_array_elements('[
      {"date":"2026-10-16T19:00:00","lieu":"Vendredi jeux","parties":[
        {"jeu":"Dixit","duree":35,"joueurs":[0,1,2,3],"rangs":[1,2,2,4],"points":[28,24,24,18]},
        {"jeu":"7 Wonders","duree":45,"joueurs":[0,1,2],"rangs":[2,1,3],"points":[55,62,48]}]},
      {"date":"2026-10-18T15:00:00","lieu":"Après-midi jeux","parties":[
        {"jeu":"Codenames","duree":25,"joueurs":[0,1,2,3],"rangs":[1,1,2,2],"points":[9,9,6,6],"equipes":[1,1,2,2]},
        {"jeu":"Mario Kart 8 Deluxe","duree":20,"joueurs":[0,1,2,3],"rangs":[3,2,1,4],"points":[39,48,54,30]}]},
      {"date":"2026-10-22T20:00:00","lieu":"Soirée chez Nina","parties":[
        {"jeu":"Rocket League","duree":15,"joueurs":[0,1,2,3],"rangs":[2,1,2,1],"points":[2,4,2,4],"equipes":[1,2,1,2]},
        {"jeu":"Super Smash Bros. Ultimate","duree":12,"joueurs":[0,1,2,3],"rangs":[2,3,4,1]}]}
    ]'::JSONB) LOOP
        -- Rejouable : ne remplace jamais une soirée déjà présente à cette date et ce lieu.
        IF EXISTS (SELECT 1 FROM soirees WHERE date = (definition->>'date')::TIMESTAMP AND lieu = definition->>'lieu') THEN
            CONTINUE;
        END IF;
        INSERT INTO soirees (date, lieu) VALUES ((definition->>'date')::TIMESTAMP, definition->>'lieu')
        RETURNING id INTO soiree_id_demo;
        INSERT INTO soiree_joueur (soiree_id, joueur_id) SELECT soiree_id_demo, unnest(ids);

        FOR partie IN SELECT value FROM jsonb_array_elements(definition->'parties') LOOP
            SELECT id, nb_joueurs_min, nb_joueurs_max INTO STRICT jeu_id_demo, minimum, maximum
            FROM jeux WHERE nom = partie->>'jeu';
            nombre := jsonb_array_length(partie->'joueurs');
            IF nombre < minimum OR nombre > maximum THEN
                RAISE EXCEPTION 'Nombre de joueurs invalide pour %', partie->>'jeu';
            END IF;
            INSERT INTO parties (duree, soiree_id, jeu_id)
            VALUES ((partie->>'duree')::INT, soiree_id_demo, jeu_id_demo) RETURNING id INTO partie_id_demo;
            FOR i IN 0..nombre - 1 LOOP
                INSERT INTO scores (points, rang, numero_equipe, partie_id, joueur_id)
                VALUES ((partie->'points'->>i)::INT, (partie->'rangs'->>i)::INT,
                    (partie->'equipes'->>i)::INT, partie_id_demo, ids[(partie->'joueurs'->>i)::INT + 1]);
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

COMMIT;
