import { JeuResume } from "./jeu.model"
import { Joueur } from "./joueur.model"

// Une soirée dans la liste : GET /soirees (de la plus récente à la plus ancienne)
export interface SoireeResume {
  id: number
  date: string // format ISO sans fuseau : "2026-09-12T20:00:00"
  lieu: string
  joueurs: Joueur[] // joueurs présents
  nbParties: number
}

// Création d'une soirée en une seule requête : POST /soirees (et PUT /soirees/{id})
// En écriture, les objets liés sont désignés par leur id (voir docs/format-dto.md)
export interface SoireeCreation {
  date: string // format ISO sans fuseau : "2026-10-10T20:00:00"
  lieu: string
  joueurIds: number[]
  parties: PartieCreation[]
}

export interface PartieCreation {
  jeuId: number
  duree: number
  scores: ScoreCreation[]
}

export interface ScoreCreation {
  joueurId: number
  rang: number
  points: number | null
  numeroEquipe: number | null
}

// Détail d'une soirée : GET /soirees/{id} (et réponse de POST /soirees)
// En lecture, les objets liés sont renvoyés sous forme de résumé
export interface SoireeDetail {
  id: number
  date: string // format ISO sans fuseau : "2026-09-12T20:00:00"
  lieu: string
  joueurs: Joueur[] // joueurs présents
  parties: PartieDetail[] // dans l'ordre où elles ont été jouées
}

export interface PartieDetail {
  id: number
  duree: number
  jeu: JeuResume
  scores: ScoreDetail[] // déjà triés par rang par le back
}

export interface ScoreDetail {
  id: number
  points: number | null
  rang: number
  numeroEquipe: number | null // null : partie en chacun pour soi
  joueur: Joueur
}
