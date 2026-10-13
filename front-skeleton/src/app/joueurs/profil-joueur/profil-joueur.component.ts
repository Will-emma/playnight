import { Location } from "@angular/common"
import { Component, OnInit } from "@angular/core"
import { ActivatedRoute } from "@angular/router"
import { JeuResume } from "models/jeu.model"
import { JeuFavori, PartieHistorique, ProfilJoueur } from "models/joueur.model"
import { formaterDate, pluriel } from "utils/format"
import { ProfilJoueurPageData } from "./profil-joueur.resolver"

@Component({
  selector: "app-profil-joueur",
  templateUrl: "./profil-joueur.component.html",
  styleUrls: ["./profil-joueur.component.scss"],
})
export class ProfilJoueurComponent implements OnInit {
  // Le back renvoie tous les jeux joués et toute l'historique : la page en montre une partie, sauf si on demande à tout voir
  readonly nbJeuxFavorisAffiches = 3
  readonly nbPartiesAffichees = 10
  readonly formaterDate = formaterDate
  readonly pluriel = pluriel

  profil: ProfilJoueur | null = null
  erreur: ProfilJoueurPageData["erreur"] = null
  tousLesFavoris = false
  toutLHistorique = false
  readonly imagesIndisponibles = new Set<number>()

  constructor(
    private route: ActivatedRoute,
    private location: Location,
  ) {}

  ngOnInit(): void {
    // data (et non snapshot.data) : la page se met à jour si on passe d'un profil à un autre sans la quitter
    this.route.data.subscribe((data) => {
      const page: ProfilJoueurPageData = data["profilJoueurPage"]
      this.profil = page.profil
      this.erreur = page.erreur
      this.tousLesFavoris = false
      this.toutLHistorique = false
    })
  }

  // On arrive sur un profil depuis plusieurs pages (fiche jeu, liste des joueurs…) : on revient à la précédente
  retour(): void {
    this.location.back()
  }

  // Pourcentage de parties gagnées, arrondi : calculé ici à partir des deux nombres renvoyés par l'API
  get tauxVictoire(): number {
    if (!this.profil || this.profil.nbParties === 0) return 0
    return Math.round((this.profil.nbVictoires / this.profil.nbParties) * 100)
  }

  get jeuxFavorisAffiches(): JeuFavori[] {
    const favoris = this.profil?.jeuxFavoris ?? []
    return this.tousLesFavoris ? favoris : favoris.slice(0, this.nbJeuxFavorisAffiches)
  }

  get historiqueAffiche(): PartieHistorique[] {
    const historique = this.profil?.historique ?? []
    return this.toutLHistorique ? historique : historique.slice(0, this.nbPartiesAffichees)
  }

  // 1 → "1er", 2 → "2e"
  formaterRang(rang: number): string {
    return rang === 1 ? "1er" : `${rang}e`
  }

  initiale(jeu: JeuResume): string {
    return jeu.nom.trim().charAt(0).toLocaleUpperCase("fr-FR")
  }

  afficherImage(jeu: JeuResume): boolean {
    return !!jeu.image && !this.imagesIndisponibles.has(jeu.id)
  }
}
