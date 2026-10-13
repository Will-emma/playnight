import { Component, HostBinding, OnInit } from "@angular/core"
import { ActivatedRoute } from "@angular/router"
import { Jeu, TypeJeu } from "models/jeu.model"
import { JeuService } from "services/jeu.service"

type FiltreJeu = TypeJeu | "TOUS"

@Component({
  selector: "app-jeux",
  templateUrl: "./jeux.component.html",
  styleUrls: ["./jeux.component.scss"],
})
export class JeuxComponent implements OnInit {
  private readonly couleursCouverture = [
    "#e9562e",
    "#397df2",
    "#2fc0b5",
    "#f5b73e",
    "#9c5be1",
    "#ed3f70",
  ]

  jeux: Jeu[] = []
  filtre: FiltreJeu = "TOUS"
  loading = true
  error = false
  darkMode = true
  readonly imagesIndisponibles = new Set<number>()
  readonly filtres: { valeur: FiltreJeu; libelle: string }[] = [
    { valeur: "TOUS", libelle: "Tous les jeux" },
    { valeur: TypeJeu.SOCIETE, libelle: "Jeux de société" },
    { valeur: TypeJeu.JEU_VIDEO, libelle: "Jeux vidéo" },
  ]

  constructor(
    private route: ActivatedRoute,
    private jeuService: JeuService,
  ) {}

  ngOnInit(): void {
    this.darkMode = localStorage.getItem("playnight-library-theme") !== "light"
    const data = this.route.snapshot.data["jeuxPage"]
    this.jeux = data.jeux
    this.error = data.error
    this.loading = false
  }

  @HostBinding("class.light-theme")
  get isLightTheme(): boolean {
    return !this.darkMode
  }

  basculerTheme(): void {
    this.darkMode = !this.darkMode
    localStorage.setItem("playnight-library-theme", this.darkMode ? "dark" : "light")
  }

  get jeuxFiltres(): Jeu[] {
    return this.filtre === "TOUS"
      ? this.jeux
      : this.jeux.filter((jeu) => jeu.type === this.filtre)
  }

  choisirFiltre(filtre: FiltreJeu): void {
    this.filtre = filtre
  }

  recharger(): void {
    this.loading = true
    this.error = false
    this.jeuService.findAll().subscribe({
      next: (jeux) => {
        this.jeux = jeux
        this.loading = false
      },
      error: () => {
        this.error = true
        this.loading = false
      },
    })
  }

  labelType(type: TypeJeu): string {
    return type === TypeJeu.SOCIETE ? "Société" : "Jeu vidéo"
  }

  couleurCouverture(jeu: Jeu): string {
    const index = Math.max(0, jeu.id - 1) % this.couleursCouverture.length
    return this.couleursCouverture[index]
  }

  initiale(jeu: Jeu): string {
    return jeu.nom.trim().charAt(0).toLocaleUpperCase("fr-FR")
  }

  nombreJoueurs(jeu: Jeu): string {
    const minimum = jeu.nbJoueursMin
    const maximum = jeu.nbJoueursMax

    if (minimum == null && maximum == null) return "Nombre de joueurs non renseigné"
    if (minimum == null) return `Jusqu'à ${maximum} joueurs`
    if (maximum == null || minimum === maximum) return `${minimum} joueur${minimum > 1 ? "s" : ""}`
    return `${minimum}–${maximum} joueurs`
  }

  marquerImageIndisponible(jeu: Jeu): void {
    this.imagesIndisponibles.add(jeu.id)
  }
}
