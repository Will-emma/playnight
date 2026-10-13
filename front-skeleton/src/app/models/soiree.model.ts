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
