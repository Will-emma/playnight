import { EloProgressionComponent } from "../elo-progression/elo-progression.component"
import { MatDialog } from "@angular/material/dialog"
import { AvatarDialogComponent } from "../avatar-dialog/avatar-dialog.component"
import { Location } from "@angular/common"
import { Component, OnInit } from "@angular/core"
import { ActivatedRoute } from "@angular/router"
import { ClassementGlobal, ClassementJoueur, JeuFavori, PartieHistorique, ProfilJoueur } from "models/joueur.model"
import { formaterDate, pluriel } from "utils/format"
import { ProfilJoueurPageData } from "./profil-joueur.resolver"

// Mêmes couleurs que les couvertures de la Ludothèque et les avatars de la page Joueurs
const COULEURS = ["#e9562e", "#397df2", "#2fc0b5", "#f5b73e", "#9c5be1", "#ed3f70"]

// Jauge du taux de victoire : cercle de rayon 26 (voir le gabarit), dont on remplit une part du périmètre
const PERIMETRE_JAUGE = 2 * Math.PI * 26

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
  readonly perimetreJauge = PERIMETRE_JAUGE

  profil: ProfilJoueur | null = null
  classementGlobal: ClassementGlobal | null = null
  classements: ClassementJoueur[] = []
  erreurClassements = false
  erreur: ProfilJoueurPageData["erreur"] = null
  tousLesFavoris = false
  toutLHistorique = false
  // Images qui n'ont pas pu être chargées (couvertures, avatar) : on affiche la couleur et l'initiale à la place
  readonly imagesIndisponibles = new Set<string>()

  constructor(
    private route: ActivatedRoute,
    private location: Location,
    private dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    // data (et non snapshot.data) : la page se met à jour si on passe d'un profil à un autre sans la quitter
    this.route.data.subscribe((data) => {
      const page: ProfilJoueurPageData = data["profilJoueurPage"]
      this.profil = page.profil
      this.classementGlobal = page.classementGlobal
      this.classements = page.classements
      this.erreurClassements = page.erreurClassements
      this.erreur = page.erreur
      this.tousLesFavoris = false
      this.toutLHistorique = false
    })
  }

  ouvrirProgression(): void {
    if (!this.profil) return
    const id = this.profil.joueur.id
    this.dialog.open(EloProgressionComponent, {
      data: this.profil.joueur,
      width: "840px",
      maxWidth: "94vw",
      maxHeight: "90vh",
      ariaLabelledBy: "progression-elo-titre",
      panelClass: "avatar-dialog-panel",
    }).afterClosed().subscribe((classement: ClassementGlobal | undefined) => {
      if (classement && this.profil?.joueur.id === id) this.classementGlobal = classement
    })
  }

  modifierAvatar(): void {
    if (!this.profil) return
    const id = this.profil.joueur.id
    this.dialog.open(AvatarDialogComponent, {
      data: this.profil.joueur,
      width: "560px",
      maxWidth: "94vw",
      maxHeight: "90vh",
      ariaLabelledBy: "avatar-dialog-title",
      panelClass: "avatar-dialog-panel",
    }).afterClosed().subscribe((joueur) => {
      if (joueur && this.profil?.joueur.id === id) {
        this.profil = { ...this.profil, joueur }
        this.imagesIndisponibles.delete(joueur.avatar)
      }
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

  // Longueur du trait de la jauge : la part du périmètre qui correspond au taux de victoire
  get remplissageJauge(): string {
    return `${(this.tauxVictoire / 100) * PERIMETRE_JAUGE} ${PERIMETRE_JAUGE}`
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

  aUneImage(url: string | null): boolean {
    return !!url && !this.imagesIndisponibles.has(url)
  }

  marquerIndisponible(url: string | null): void {
    if (url) this.imagesIndisponibles.add(url)
  }

  couleur(id: number): string {
    return COULEURS[Math.max(0, id - 1) % COULEURS.length]
  }

  initiale(texte: string): string {
    return texte.trim().charAt(0).toLocaleUpperCase("fr-FR")
  }
}
