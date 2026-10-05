Do $$

    DECLARE _STUDENT_1 int := NEXTVAL('students_id_seq');
    DECLARE _MAJOR_1 int := NEXTVAL('majors_id_seq');
    DECLARE _COURSE_1 int := NEXTVAL('courses_id_seq');

    BEGIN

    INSERT INTO majors (id, name, description) VALUES (_MAJOR_1, 'Informatique', 'Ouaiiis du code partout');
    INSERT INTO majors (name, description) VALUES ('Construction', 'Beaucoup de béton et des poutres');
    INSERT INTO majors (name, description) VALUES ('Aéronautique', 'Vive le vent');
    INSERT INTO majors (name, description) VALUES ('Data', 'Trop cool plein de données à ordonner');
    INSERT INTO majors (name, description) VALUES ('Energie & Environnement', 'On est full green');
    INSERT INTO majors (name, description) VALUES ('Management', 'Des managers de qualité');
    INSERT INTO majors (name, description) VALUES ('Santé', 'On connait tous les os et tous les muscles du corps humain');
    INSERT INTO majors (name, description) VALUES ('Architecture durable', 'Objectif 0 carbone');
    INSERT INTO majors (name, description) VALUES ('Design Industriel Durable', 'On resistera à la fin du pétrole');

    INSERT INTO students (id, first_name, last_name, birthdate, major_id) VALUES (_STUDENT_1, 'Paul', 'Harrohide', '2002-06-15', _MAJOR_1);
    INSERT INTO students (first_name, last_name, birthdate, major_id) VALUES ('Jean', 'Bonbeur', '2001-08-21',_MAJOR_1);
    INSERT INTO students (first_name, last_name, birthdate, major_id) VALUES ('Alain', 'Térieur', '2000-01-11', _MAJOR_1);

    INSERT INTO courses (id, name, hours) VALUES (_COURSE_1, 'Java', 30);
    INSERT INTO courses (name, hours) VALUES ('German', 30);
    INSERT INTO courses (name, hours) VALUES ('Internet of Things', 30);
    INSERT INTO courses (name, hours) VALUES ('Thermodynamic', 30);
    INSERT INTO courses (name, hours) VALUES ('Anatomy', 30);
    INSERT INTO courses (name, hours) VALUES ('Maths', 30);
    INSERT INTO courses (name, hours) VALUES ('Spanish', 30);
    INSERT INTO courses (name, hours) VALUES ('Lean Management', 30);
    INSERT INTO student_course (student_id, course_id) VALUES (_STUDENT_1, _COURSE_1);

    END $$;

-- ==========================================================
-- PlayNight : données de démo
-- Les ids générés sont récupérés avec RETURNING id INTO, pour ne jamais les écrire à la main
-- ==========================================================
Do $$

    -- Joueurs
    DECLARE _LEA bigint;
    DECLARE _MAX bigint;
    DECLARE _SAM bigint;
    DECLARE _INES bigint;
    DECLARE _TOM bigint;
    DECLARE _NOA bigint;

    -- Jeux
    DECLARE _CATAN bigint;
    DECLARE _DIXIT bigint;
    DECLARE _CODENAMES bigint;
    DECLARE _7_WONDERS bigint;
    DECLARE _MARIO_KART bigint;
    DECLARE _SMASH bigint;
    DECLARE _ROCKET_LEAGUE bigint;
    DECLARE _STREET_FIGHTER bigint;

    -- Réutilisées pour chaque soirée et chaque partie
    DECLARE _SOIREE bigint;
    DECLARE _PARTIE bigint;

    BEGIN

    INSERT INTO joueurs (pseudo, avatar) VALUES ('Léa', 'https://api.dicebear.com/9.x/pixel-art/svg?seed=Lea') RETURNING id INTO _LEA;
    INSERT INTO joueurs (pseudo, avatar) VALUES ('MaxPower', 'https://api.dicebear.com/9.x/pixel-art/svg?seed=MaxPower') RETURNING id INTO _MAX;
    INSERT INTO joueurs (pseudo, avatar) VALUES ('Samouraï', 'https://api.dicebear.com/9.x/pixel-art/svg?seed=Samourai') RETURNING id INTO _SAM;
    INSERT INTO joueurs (pseudo, avatar) VALUES ('Inès', 'https://api.dicebear.com/9.x/pixel-art/svg?seed=Ines') RETURNING id INTO _INES;
    INSERT INTO joueurs (pseudo, avatar) VALUES ('TomTom', 'https://api.dicebear.com/9.x/pixel-art/svg?seed=TomTom') RETURNING id INTO _TOM;
    INSERT INTO joueurs (pseudo, avatar) VALUES ('Noa', 'https://api.dicebear.com/9.x/pixel-art/svg?seed=Noa') RETURNING id INTO _NOA;

    -- Jeux de société
    INSERT INTO jeux (nom, type, plateforme, nb_joueurs_min, nb_joueurs_max, image) VALUES ('Catan', 'SOCIETE', null, 3, 4, 'https://placehold.co/300x200?text=Catan') RETURNING id INTO _CATAN;
    INSERT INTO jeux (nom, type, plateforme, nb_joueurs_min, nb_joueurs_max, image) VALUES ('Dixit', 'SOCIETE', null, 3, 8, 'https://placehold.co/300x200?text=Dixit') RETURNING id INTO _DIXIT;
    INSERT INTO jeux (nom, type, plateforme, nb_joueurs_min, nb_joueurs_max, image) VALUES ('Codenames', 'SOCIETE', null, 4, 8, 'https://placehold.co/300x200?text=Codenames') RETURNING id INTO _CODENAMES;
    INSERT INTO jeux (nom, type, plateforme, nb_joueurs_min, nb_joueurs_max, image) VALUES ('7 Wonders', 'SOCIETE', null, 3, 7, 'https://placehold.co/300x200?text=7+Wonders') RETURNING id INTO _7_WONDERS;

    -- Jeux vidéo
    INSERT INTO jeux (nom, type, plateforme, nb_joueurs_min, nb_joueurs_max, image) VALUES ('Mario Kart 8 Deluxe', 'JEU_VIDEO', 'Nintendo Switch', 1, 4, 'https://placehold.co/300x200?text=Mario+Kart') RETURNING id INTO _MARIO_KART;
    INSERT INTO jeux (nom, type, plateforme, nb_joueurs_min, nb_joueurs_max, image) VALUES ('Super Smash Bros. Ultimate', 'JEU_VIDEO', 'Nintendo Switch', 2, 8, 'https://placehold.co/300x200?text=Smash+Bros') RETURNING id INTO _SMASH;
    INSERT INTO jeux (nom, type, plateforme, nb_joueurs_min, nb_joueurs_max, image) VALUES ('Rocket League', 'JEU_VIDEO', 'PC', 2, 8, 'https://placehold.co/300x200?text=Rocket+League') RETURNING id INTO _ROCKET_LEAGUE;
    INSERT INTO jeux (nom, type, plateforme, nb_joueurs_min, nb_joueurs_max, image) VALUES ('Street Fighter 6', 'JEU_VIDEO', 'PS5', 2, 2, 'https://placehold.co/300x200?text=Street+Fighter+6') RETURNING id INTO _STREET_FIGHTER;

    -- ---------- Soirée 1 : jeux de société chez Léa ----------
    INSERT INTO soirees (date, lieu) VALUES ('2026-09-12 20:00', 'Chez Léa') RETURNING id INTO _SOIREE;
    INSERT INTO soiree_joueur (soiree_id, joueur_id) VALUES (_SOIREE, _LEA), (_SOIREE, _MAX), (_SOIREE, _SAM), (_SOIREE, _INES), (_SOIREE, _TOM);

    INSERT INTO parties (duree, soiree_id, jeu_id) VALUES (75, _SOIREE, _CATAN) RETURNING id INTO _PARTIE;
    INSERT INTO scores (points, rang, numero_equipe, partie_id, joueur_id) VALUES
        (10, 1, null, _PARTIE, _LEA),
        (8, 2, null, _PARTIE, _MAX),
        (7, 3, null, _PARTIE, _SAM),
        (5, 4, null, _PARTIE, _INES);

    INSERT INTO parties (duree, soiree_id, jeu_id) VALUES (25, _SOIREE, _MARIO_KART) RETURNING id INTO _PARTIE;
    INSERT INTO scores (points, rang, numero_equipe, partie_id, joueur_id) VALUES
        (60, 1, null, _PARTIE, _TOM),
        (52, 2, null, _PARTIE, _LEA),
        (45, 3, null, _PARTIE, _MAX),
        (38, 4, null, _PARTIE, _INES);

    -- Partie en équipe : équipe 1 (Léa, MaxPower, Inès) contre équipe 2 (Samouraï, TomTom)
    INSERT INTO parties (duree, soiree_id, jeu_id) VALUES (30, _SOIREE, _CODENAMES) RETURNING id INTO _PARTIE;
    INSERT INTO scores (points, rang, numero_equipe, partie_id, joueur_id) VALUES
        (9, 1, 1, _PARTIE, _LEA),
        (9, 1, 1, _PARTIE, _MAX),
        (9, 1, 1, _PARTIE, _INES),
        (6, 2, 2, _PARTIE, _SAM),
        (6, 2, 2, _PARTIE, _TOM);

    -- ---------- Soirée 2 : e-sport en ligne ----------
    INSERT INTO soirees (date, lieu) VALUES ('2026-09-26 21:00', 'En ligne (Discord)') RETURNING id INTO _SOIREE;
    INSERT INTO soiree_joueur (soiree_id, joueur_id) VALUES (_SOIREE, _MAX), (_SOIREE, _SAM), (_SOIREE, _TOM), (_SOIREE, _NOA);

    -- Partie en équipe : Rocket League en 2v2
    INSERT INTO parties (duree, soiree_id, jeu_id) VALUES (15, _SOIREE, _ROCKET_LEAGUE) RETURNING id INTO _PARTIE;
    INSERT INTO scores (points, rang, numero_equipe, partie_id, joueur_id) VALUES
        (4, 1, 1, _PARTIE, _MAX),
        (4, 1, 1, _PARTIE, _NOA),
        (2, 2, 2, _PARTIE, _TOM),
        (2, 2, 2, _PARTIE, _SAM);

    INSERT INTO parties (duree, soiree_id, jeu_id) VALUES (10, _SOIREE, _STREET_FIGHTER) RETURNING id INTO _PARTIE;
    INSERT INTO scores (points, rang, numero_equipe, partie_id, joueur_id) VALUES
        (2, 1, null, _PARTIE, _TOM),
        (1, 2, null, _PARTIE, _NOA);

    -- Smash : seul le classement compte, pas de points
    INSERT INTO parties (duree, soiree_id, jeu_id) VALUES (12, _SOIREE, _SMASH) RETURNING id INTO _PARTIE;
    INSERT INTO scores (points, rang, numero_equipe, partie_id, joueur_id) VALUES
        (null, 1, null, _PARTIE, _SAM),
        (null, 2, null, _PARTIE, _MAX),
        (null, 3, null, _PARTIE, _TOM),
        (null, 4, null, _PARTIE, _NOA);

    -- ---------- Soirée 3 : bar à jeux, tout le monde est là ----------
    INSERT INTO soirees (date, lieu) VALUES ('2026-10-03 19:30', 'Bar à jeux Le Dé Pipé') RETURNING id INTO _SOIREE;
    INSERT INTO soiree_joueur (soiree_id, joueur_id) VALUES
        (_SOIREE, _LEA), (_SOIREE, _MAX), (_SOIREE, _SAM), (_SOIREE, _INES), (_SOIREE, _TOM), (_SOIREE, _NOA);

    INSERT INTO parties (duree, soiree_id, jeu_id) VALUES (40, _SOIREE, _DIXIT) RETURNING id INTO _PARTIE;
    INSERT INTO scores (points, rang, numero_equipe, partie_id, joueur_id) VALUES
        (30, 1, null, _PARTIE, _INES),
        (27, 2, null, _PARTIE, _NOA),
        (25, 3, null, _PARTIE, _LEA),
        (22, 4, null, _PARTIE, _SAM),
        (18, 5, null, _PARTIE, _TOM),
        (15, 6, null, _PARTIE, _MAX);

    INSERT INTO parties (duree, soiree_id, jeu_id) VALUES (45, _SOIREE, _7_WONDERS) RETURNING id INTO _PARTIE;
    INSERT INTO scores (points, rang, numero_equipe, partie_id, joueur_id) VALUES
        (58, 1, null, _PARTIE, _NOA),
        (54, 2, null, _PARTIE, _LEA),
        (49, 3, null, _PARTIE, _MAX),
        (47, 4, null, _PARTIE, _INES),
        (41, 5, null, _PARTIE, _SAM),
        (39, 6, null, _PARTIE, _TOM);

    END $$;

