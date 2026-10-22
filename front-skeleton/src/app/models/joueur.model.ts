import { JeuResume } from "./jeu.model"

// Même format en lecture et en écriture ; c'est aussi le "résumé" d'un joueur renvoyé dans les soirées
export interface Joueur {
  id: number
  pseudo: string
  avatar: string | null
}

// Profil d'un joueur : GET /joueurs/{id}/profil (voir docs/format-dto.md)
export interface ProfilJoueur {
  joueur: Joueur
  nbParties: number
  nbVictoires: number // parties où le joueur est classé premier (ex aequo et équipes compris)
  jeuxFavoris: JeuFavori[] // tous les jeux joués, du plus joué au moins joué ; la page choisit combien en afficher
  historique: PartieHistorique[] // de la plus récente à la plus ancienne
}

export interface JeuFavori {
  jeu: JeuResume
  nbParties: number
}

export interface PartieHistorique {
  partieId: number
  date: string // format ISO sans fuseau : "2026-09-12T20:00:00"
  jeu: JeuResume
  rang: number
  points: number | null
}

// GET /joueurs/{id}/classements : Elo et rang calculés exclusivement par le backend.
export interface ClassementJoueur {
  jeu: JeuResume
  elo: number
  rang: number
  nbParties: number
}

// Moyenne des Elo par jeu, calculée par l'API. Sans résultat : 1000, rang null.
export interface ClassementGlobal {
  elo: number
  rang: number | null
}

export interface PointElo {
  partieId: number | null
  date: string | null
  jeu: string | null
  elo: number
  variation: number
}

export interface ProgressionElo {
  classement: ClassementGlobal
  meilleurElo: number
  variationTotale: number
  nbParties: number
  points: PointElo[]
}
