-- Vérifications générales, également exécutables seules avec psql -v ON_ERROR_STOP=1.
\set ON_ERROR_STOP on
DO $$
DECLARE n integer;
BEGIN
    IF current_database() <> 'playnight_demo_ticket41'
       AND current_database() !~ '^playnight_demo_ticket41_test_[a-z0-9_]+$' THEN
        RAISE EXCEPTION 'Vérification #41 : base interdite (%).', current_database();
    END IF;
    IF (SELECT count(*) FROM demo_ticket41.catalogue) <> 4
       OR (SELECT count(*) FROM demo_ticket41.soirees) <> 4
       OR (SELECT count(*) FROM demo_ticket41.parties) <> 9 THEN
        RAISE EXCEPTION 'Manifeste incomplet : attendre 4 jeux vidéo, 4 soirées, 9 parties.';
    END IF;
    SELECT count(*) INTO n FROM parties p JOIN demo_ticket41.parties d ON d.partie_id = p.id
    JOIN jeux j ON j.id = p.jeu_id
    WHERE (SELECT count(*) FROM scores s WHERE s.partie_id = p.id)
          NOT BETWEEN j.nb_joueurs_min AND j.nb_joueurs_max;
    IF n <> 0 THEN RAISE EXCEPTION 'Nombre de joueurs hors limites pour % partie(s).', n; END IF;
    IF EXISTS (SELECT FROM scores s JOIN parties p ON p.id = s.partie_id
               JOIN demo_ticket41.parties d ON d.partie_id = p.id
               WHERE NOT EXISTS (SELECT FROM soiree_joueur sj WHERE sj.soiree_id = p.soiree_id AND sj.joueur_id = s.joueur_id)) THEN
        RAISE EXCEPTION 'Un joueur de partie est absent de sa soirée.';
    END IF;
    IF EXISTS (SELECT FROM scores s JOIN demo_ticket41.parties d ON d.partie_id = s.partie_id
               GROUP BY s.partie_id HAVING min(s.rang) <> 1
               OR (count(s.numero_equipe) > 0 AND count(s.numero_equipe) <> count(*))) THEN
        RAISE EXCEPTION 'Vainqueur absent ou mélange résultats individuels/équipes.';
    END IF;
    IF EXISTS (SELECT FROM scores s JOIN demo_ticket41.parties d ON d.partie_id = s.partie_id
               WHERE s.numero_equipe IS NOT NULL GROUP BY s.partie_id, s.numero_equipe
               HAVING count(DISTINCT s.rang) <> 1 OR count(DISTINCT s.points) > 1) THEN
        RAISE EXCEPTION 'Résultats incohérents au sein d’une équipe.';
    END IF;
    IF EXISTS (SELECT FROM scores s JOIN demo_ticket41.parties d ON d.partie_id = s.partie_id
               WHERE s.numero_equipe IS NOT NULL GROUP BY s.partie_id
               HAVING count(DISTINCT s.numero_equipe) < 2) THEN
        RAISE EXCEPTION 'Une partie compétitive en équipe doit comporter au moins deux camps.';
    END IF;
    IF EXISTS (SELECT FROM scores s JOIN demo_ticket41.parties d ON d.partie_id = s.partie_id
               WHERE d.cle IN ('overcooked','fc25') GROUP BY d.cle, s.numero_equipe HAVING count(*) <> 2) THEN
        RAISE EXCEPTION 'Overcooked et FC 25 doivent avoir deux personnes par équipe.';
    END IF;
    IF NOT EXISTS (SELECT FROM scores s JOIN demo_ticket41.parties d ON d.partie_id = s.partie_id
                   WHERE d.cle = 'catan_4' GROUP BY s.partie_id
                   HAVING array_agg(s.rang ORDER BY s.rang) = ARRAY[1,2,2,4]) THEN
        RAISE EXCEPTION 'Ex æquo individuels attendus : 1, 2, 2, 4.';
    END IF;
    IF NOT EXISTS (SELECT FROM scores s JOIN demo_ticket41.parties d ON d.partie_id = s.partie_id
                   WHERE d.cle = 'fc25' GROUP BY s.partie_id
                   HAVING count(*) = 4 AND count(DISTINCT s.numero_equipe) = 2 AND bool_and(s.rang = 1)) THEN
        RAISE EXCEPTION 'Match nul FC 25 attendu entre deux équipes de deux.';
    END IF;
    IF (SELECT count(*) FROM scores s JOIN demo_ticket41.parties d ON d.partie_id = s.partie_id
        JOIN joueurs j ON j.id = s.joueur_id
        WHERE d.cle LIKE 'catan_%' AND j.pseudo = 'Léa' AND s.rang = 1) <> 4 THEN
        RAISE EXCEPTION 'Léa doit remporter les quatre nouvelles parties de Catan.';
    END IF;
END $$;

SELECT j.nom, count(p.id) AS parties FROM jeux j LEFT JOIN parties p ON p.jeu_id = j.id
GROUP BY j.id, j.nom ORDER BY parties DESC, j.nom;
SELECT j.pseudo, count(*) AS victoires FROM scores s JOIN joueurs j ON j.id = s.joueur_id
JOIN parties p ON p.id = s.partie_id JOIN jeux g ON g.id = p.jeu_id
WHERE g.type = 'SOCIETE' AND lower(btrim(g.nom)) = 'catan' AND s.rang = 1
GROUP BY j.id, j.pseudo ORDER BY victoires DESC, j.pseudo;
