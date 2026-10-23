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
  description?: string | null
  dureeEstimee?: number | null // minutes indicatives, distinctes de la durée d’une partie
}

// Un jeu vidéo trouvé sur RAWG, pas encore dans la ludothèque : GET /jeux/rawg?recherche=...
export interface RawgJeu {
  idRawg: number
  nom: string
  image: string | null
  plateforme: string | null // jusqu'à 4 plateformes : "PC, PlayStation 5, Xbox One, Nintendo Switch"
  dateSortie: string | null // "2017-03-03"
}

// Corps de POST /jeux/rawg/{idRawg} : RAWG ne donne pas le nombre de joueurs, l'utilisateur le saisit
export interface RawgImport {
  nbJoueursMin: number
  nbJoueursMax: number
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

// GET /jeux/{id}/classement ; rang partagé en cas d'égalité d'Elo.
export interface ClassementElo {
  joueurId: number
  pseudo: string
  avatar: string | null
  elo: number
  nbParties: number
  rang: number
}
