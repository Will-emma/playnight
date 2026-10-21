import { Component, OnInit } from "@angular/core"
import { ActivatedRoute } from "@angular/router"
import { HttpErrorResponse } from "@angular/common/http"
import { forkJoin, map } from "rxjs"
import { Joueur } from "models/joueur.model"
import { JoueurService } from "services/joueur.service"

interface JoueurClassement {
  joueur: Joueur
  nbVictoires: number
  nbParties: number
}

@Component({
  selector: "app-joueurs",
  templateUrl: "./joueurs.component.html",
  styleUrls: ["./joueurs.component.scss"],
})
export class JoueursComponent implements OnInit {
  afficherClassement = false
  chargementClassement = false
  erreurClassement = false
  classementCharge = false
  classement: JoueurClassement[] = []
  joueurs: Joueur[] = []
  loading = true
  error = false
  afficherFormulaire = false
  enregistrement = false
  pseudo = ""
  avatar = ""
  messageErreurAjout = ""
  readonly avatarsIndisponibles = new Set<number>()

  constructor(
    private route: ActivatedRoute,
    private joueurService: JoueurService,
  ) {}

  ngOnInit(): void {
    const data = this.route.snapshot.data["joueursPage"]
    this.joueurs = data.joueurs
    this.error = data.error
    this.loading = false
  }

  ouvrirFormulaire(): void {
    this.afficherFormulaire = !this.afficherFormulaire
    this.messageErreurAjout = ""
  }

  basculerClassement(): void {
    this.afficherClassement = !this.afficherClassement
    if (this.afficherClassement && !this.classementCharge) this.chargerClassement()
  }

  chargerClassement(): void {
    if (this.joueurs.length === 0) {
      this.classement = []
      this.classementCharge = true
      return
    }

    this.chargementClassement = true
    this.erreurClassement = false
    forkJoin(
      this.joueurs.map((joueur) =>
        this.joueurService.getProfil(joueur.id).pipe(
          map((profil) => ({
            joueur,
            nbVictoires: profil.nbVictoires,
            nbParties: profil.nbParties,
          })),
        ),
      ),
    ).subscribe({
      next: (classement) => {
        this.classement = classement.sort(
          (a, b) =>
            b.nbVictoires - a.nbVictoires ||
            b.nbParties - a.nbParties ||
            a.joueur.pseudo.localeCompare(b.joueur.pseudo, "fr", { sensitivity: "base" }),
        )
        this.classementCharge = true
        this.chargementClassement = false
      },
      error: () => {
        this.erreurClassement = true
        this.chargementClassement = false
      },
    })
  }

  get podium(): JoueurClassement[] {
    const [premier, deuxieme, troisieme] = this.classement
    return [deuxieme, premier, troisieme].filter(
      (joueur): joueur is JoueurClassement => joueur !== undefined,
    )
  }

  ajouterJoueur(): void {
    const pseudo = this.pseudo.trim()
    if (!pseudo || this.enregistrement) return

    this.enregistrement = true
    this.messageErreurAjout = ""
    this.joueurService.create({
      pseudo,
      avatar: this.avatar.trim() || null,
    }).subscribe({
      next: (joueur) => {
        this.joueurs = [...this.joueurs, joueur].sort((a, b) =>
          a.pseudo.localeCompare(b.pseudo, "fr", { sensitivity: "base" }),
        )
        this.classementCharge = false
        this.pseudo = ""
        this.avatar = ""
        this.afficherFormulaire = false
        this.enregistrement = false
      },
      error: (erreur: HttpErrorResponse) => {
        this.messageErreurAjout =
          erreur.status === 409
            ? "Ce pseudo est déjà utilisé. Choisissez-en un autre."
            : "Le joueur n’a pas pu être ajouté. Vérifiez les informations puis réessayez."
        this.enregistrement = false
      },
    })
  }

  recharger(): void {
    this.loading = true
    this.error = false
    this.joueurService.findAll().subscribe({
      next: (joueurs) => {
        this.joueurs = joueurs
        this.loading = false
      },
      error: () => {
        this.error = true
        this.loading = false
      },
    })
  }

  marquerAvatarIndisponible(joueur: Joueur): void {
    this.avatarsIndisponibles.add(joueur.id)
  }

  initiale(joueur: Joueur): string {
    return joueur.pseudo.trim().charAt(0).toLocaleUpperCase("fr-FR")
  }

  couleurAvatar(joueur: Joueur): string {
    const couleurs = ["#e9562e", "#397df2", "#2fc0b5", "#f5b73e", "#9c5be1", "#ed3f70"]
    return couleurs[Math.max(0, joueur.id - 1) % couleurs.length]
  }
}
