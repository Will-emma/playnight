// Même format en lecture et en écriture ; c'est aussi le "résumé" d'un joueur renvoyé dans les soirées
export interface Joueur {
  id: number
  pseudo: string
  avatar: string | null
}
