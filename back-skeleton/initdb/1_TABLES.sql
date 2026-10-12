create table students
(
    id SERIAL PRIMARY KEY,
    first_name TEXT not null,
    last_name TEXT not null,
    birthdate date null,
    major_id int null,
    image bytea null
);

create table majors
(
    id SERIAL PRIMARY KEY,
    name TEXT not null,
    description TEXT not null
);

create table courses
(
    id SERIAL PRIMARY KEY,
    name TEXT not null,
    hours int not null
);

create table student_course
(
    id SERIAL PRIMARY KEY,
    student_id int not null,
    course_id int not null
);

-- ==========================================================
-- PlayNight (voir docs/modele-donnees.md)
-- Les tables de l'exemple ci-dessus seront supprimées à la fin (ticket #15)
-- ==========================================================

create table joueurs
(
    id BIGSERIAL PRIMARY KEY,
    pseudo TEXT not null unique,
    avatar TEXT null
);

create table jeux
(
    id BIGSERIAL PRIMARY KEY,
    nom TEXT not null,
    type VARCHAR(20) not null check (type in ('SOCIETE', 'JEU_VIDEO')),
    plateforme TEXT null,
    nb_joueurs_min INT not null check (nb_joueurs_min >= 1),
    nb_joueurs_max INT not null,
    image TEXT null,
    check (nb_joueurs_max >= nb_joueurs_min)
);

create table soirees
(
    id BIGSERIAL PRIMARY KEY,
    date TIMESTAMP not null,
    lieu TEXT not null
);

-- Joueurs présents à une soirée (relation ManyToMany Soiree <-> Joueur)
create table soiree_joueur
(
    soiree_id BIGINT not null references soirees (id) on delete cascade,
    joueur_id BIGINT not null references joueurs (id) on delete cascade,
    primary key (soiree_id, joueur_id)
);

create table parties
(
    id BIGSERIAL PRIMARY KEY,
    duree INT not null check (duree > 0),
    soiree_id BIGINT not null references soirees (id) on delete cascade,
    -- pas de cascade : on ne peut pas supprimer un jeu déjà joué
    jeu_id BIGINT not null references jeux (id)
);

-- Résultat d'un joueur dans une partie (entité d'association Partie <-> Joueur)
create table scores
(
    id BIGSERIAL PRIMARY KEY,
    points INT null,
    rang INT not null check (rang >= 1),
    -- null = partie en chacun pour soi
    numero_equipe INT null,
    partie_id BIGINT not null references parties (id) on delete cascade,
    -- pas de cascade : on ne peut pas supprimer un joueur qui a des scores
    joueur_id BIGINT not null references joueurs (id),
    unique (partie_id, joueur_id)
);
