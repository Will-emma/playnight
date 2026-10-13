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
