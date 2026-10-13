import { Joueur } from "./joueur.model"

export enum TypeJeu {
  SOCIETE = "SOCIETE",
  JEU_VIDEO = "JEU_VIDEO",
}

export interface Jeu {
  id: number
  nom: string
  type: TypeJeu
  plateforme: string | null
  nbJoueursMin: number | null
  nbJoueursMax: number | null
  image: string | null
}

// Résumé d'un jeu, renvoyé quand un jeu est lié à autre chose (profil d'un joueur…)
export type JeuResume = Pick<Jeu, "id" | "nom" | "type" | "image">

// Fiche d'un jeu : GET /jeux/{id}/fiche (voir docs/format-dto.md)
export interface FicheJeu {
  jeu: Jeu
  nbParties: number
  meilleursJoueurs: MeilleurJoueur[] // tous les vainqueurs, du plus de victoires au moins ; la page choisit combien en afficher
  parties: PartieJouee[] // de la plus récente à la plus ancienne
}

export interface MeilleurJoueur {
  joueur: Joueur
  nbVictoires: number
}

export interface PartieJouee {
  partieId: number
  date: string // format ISO sans fuseau : "2026-09-12T20:00:00"
  duree: number
  vainqueurs: string[] // pseudos des joueurs classés premiers (plusieurs en cas d'ex aequo ou d'équipe)
}
