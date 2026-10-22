-- Bonus 3 : projection Elo reconstruite depuis l'historique au démarrage de l'API.
create table if not exists classements
(
    id BIGSERIAL PRIMARY KEY,
    elo INT not null default 1000,
    nb_parties INT not null default 0 check (nb_parties >= 0),
    joueur_id BIGINT not null references joueurs (id) on delete cascade,
    jeu_id BIGINT not null references jeux (id) on delete cascade,
    unique (joueur_id, jeu_id)
);
create index if not exists classements_jeu_elo_idx on classements (jeu_id, elo desc);
