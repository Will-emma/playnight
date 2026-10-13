import { HttpErrorResponse } from "@angular/common/http"
import { Component, OnInit } from "@angular/core"
import { AbstractControl, FormArray, FormBuilder, FormControl, FormGroup, ValidationErrors, Validators } from "@angular/forms"
import { ActivatedRoute, Router } from "@angular/router"
import { Jeu } from "models/jeu.model"
import { Joueur } from "models/joueur.model"
import { SoireeCreation } from "models/soiree.model"
import { SoireeService } from "services/soiree.service"
import { NouvelleSoireePageData } from "./nouvelle-soiree.resolver"

// Même règle que le back : une soirée doit avoir au moins un joueur présent
function auMoinsUnJoueur(control: AbstractControl<number[]>): ValidationErrors | null {
  return control.value.length > 0 ? null : { auMoinsUnJoueur: true }
}

// Règle du front (le back accepte une soirée sans partie) : on enregistre une soirée jouée
function auMoinsUnePartie(control: AbstractControl<unknown[]>): ValidationErrors | null {
  return control.value.length > 0 ? null : { auMoinsUnePartie: true }
}

function estEntier(valeur: number | null, minimum: number): boolean {
  return valeur != null && Number.isInteger(valeur) && valeur >= minimum
}

// Un score n'est contrôlé que si le joueur a joué la partie : rang obligatoire, points et équipe facultatifs
function scoreValide(control: AbstractControl): ValidationErrors | null {
  const score = (control as ScoreForm).getRawValue()
  if (!score.participe) return null
  const erreurs: ValidationErrors = {}
  if (!estEntier(score.rang, 1)) erreurs["rang"] = true
  if (score.points != null && !Number.isInteger(score.points)) erreurs["points"] = true
  if (score.numeroEquipe != null && !estEntier(score.numeroEquipe, 1)) erreurs["numeroEquipe"] = true
  return Object.keys(erreurs).length > 0 ? erreurs : null
}

// Une ligne de score : un joueur présent, et s'il a joué, son résultat
type ScoreForm = FormGroup<{
  joueurId: FormControl<number>
  participe: FormControl<boolean>
  rang: FormControl<number | null>
  points: FormControl<number | null>
  numeroEquipe: FormControl<number | null>
}>

// Une partie : jeu et durée (étape 2), puis les scores des joueurs présents (étape 3)
// null tant que le champ n'est pas rempli
type PartieForm = FormGroup<{
  jeuId: FormControl<number | null>
  duree: FormControl<number | null>
  scores: FormArray<ScoreForm>
}>

type ChampScore = "rang" | "points" | "numeroEquipe"

@Component({
  selector: "app-nouvelle-soiree",
  templateUrl: "./nouvelle-soiree.component.html",
  styleUrls: ["./nouvelle-soiree.component.scss"],
})
export class NouvelleSoireeComponent implements OnInit {
  joueurs: Joueur[] = []
  jeux: Jeu[] = []
  error = false

  // Étape 1 : la soirée et les joueurs présents
  readonly infos = this.fb.nonNullable.group({
    date: ["", Validators.required],
    heure: ["20:00", Validators.required],
    lieu: ["", [Validators.required, Validators.pattern(/\S/)]],
    // Un tableau comme valeur se déclare avec control(), sinon FormBuilder le lit comme [valeur, validateurs]
    joueurIds: this.fb.nonNullable.control<number[]>([], auMoinsUnJoueur),
  })

  // Étapes 2 et 3 : les parties jouées, une ligne par partie (le formulaire démarre avec une ligne vide)
  readonly parties = this.fb.array<PartieForm>([this.creerPartie()], auMoinsUnePartie)

  envoiEnCours = false
  erreurEnvoi: string | null = null
  soireeCreeeId: number | null = null

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private soireeService: SoireeService,
  ) {}

  ngOnInit(): void {
    const data: NouvelleSoireePageData = this.route.snapshot.data["nouvelleSoireePage"]
    this.joueurs = data.joueurs
    this.jeux = data.jeux
    this.error = data.error

    // Les lignes de scores suivent les joueurs présents, même si on revient modifier l'étape 1
    this.infos.controls.joueurIds.valueChanges.subscribe(() => {
      this.parties.controls.forEach((partie) => this.synchroniserScores(partie))
    })
  }

  // ---------- Étape 1 ----------

  estPresent(joueur: Joueur): boolean {
    return this.infos.controls.joueurIds.value.includes(joueur.id)
  }

  basculerPresence(joueur: Joueur): void {
    const control = this.infos.controls.joueurIds
    const ids = control.value
    control.setValue(this.estPresent(joueur) ? ids.filter((id) => id !== joueur.id) : [...ids, joueur.id])
    control.markAsTouched()
  }

  // Un message d'erreur ne s'affiche qu'une fois le champ touché, pas dès l'ouverture de la page
  aUneErreur(champ: "date" | "heure" | "lieu" | "joueurIds"): boolean {
    const control = this.infos.controls[champ]
    return control.invalid && control.touched
  }

  // ---------- Étape 2 ----------

  // L'étape 2 ne vérifie que le jeu et la durée : les scores font partie de l'étape 3
  get etapePartiesValide(): boolean {
    return (
      this.parties.length > 0 &&
      this.parties.controls.every((partie) => partie.controls.jeuId.valid && partie.controls.duree.valid)
    )
  }

  toucherEtapeParties(): void {
    this.parties.controls.forEach((partie) => {
      partie.controls.jeuId.markAsTouched()
      partie.controls.duree.markAsTouched()
    })
  }

  ajouterPartie(): void {
    const partie = this.creerPartie()
    this.synchroniserScores(partie)
    this.parties.push(partie)
  }

  supprimerPartie(index: number): void {
    this.parties.removeAt(index)
  }

  aUneErreurPartie(partie: PartieForm, champ: "jeuId" | "duree"): boolean {
    const control = partie.controls[champ]
    return control.invalid && control.touched
  }

  nombreJoueurs(jeu: Jeu): string {
    if (jeu.nbJoueursMin == null || jeu.nbJoueursMax == null) return "nombre de joueurs libre"
    if (jeu.nbJoueursMin === jeu.nbJoueursMax) return `${jeu.nbJoueursMin} joueurs`
    return `${jeu.nbJoueursMin} à ${jeu.nbJoueursMax} joueurs`
  }

  // ---------- Étape 3 ----------

  jeuDe(partie: PartieForm): Jeu | undefined {
    return this.jeux.find((jeu) => jeu.id === partie.controls.jeuId.value)
  }

  pseudo(joueurId: number): string {
    return this.joueurs.find((joueur) => joueur.id === joueurId)?.pseudo ?? `Joueur ${joueurId}`
  }

  nbParticipants(partie: PartieForm): number {
    return partie.controls.scores.controls.filter((score) => score.controls.participe.value).length
  }

  aUneErreurScore(score: ScoreForm, champ: ChampScore): boolean {
    return !!score.errors?.[champ] && score.controls[champ].touched
  }

  // ---------- Enregistrement ----------

  enregistrer(): void {
    this.infos.markAllAsTouched()
    this.parties.markAllAsTouched()
    if (this.infos.invalid || this.parties.invalid || this.envoiEnCours) return

    this.envoiEnCours = true
    this.erreurEnvoi = null
    this.soireeService.create(this.construireSoiree()).subscribe({
      next: (soiree) => this.allerAuDetail(soiree.id),
      error: (erreur: HttpErrorResponse) => {
        this.envoiEnCours = false
        this.erreurEnvoi = this.messageErreur(erreur)
      },
    })
  }

  // Le stepper est masqué pendant le message de succès : en réapparaissant, il repart de la première étape
  recommencer(): void {
    this.infos.reset()
    this.parties.clear()
    this.ajouterPartie()
    this.erreurEnvoi = null
    this.soireeCreeeId = null
  }

  // Formulaire → JSON attendu par POST /soirees (voir docs/format-dto.md)
  private construireSoiree(): SoireeCreation {
    const infos = this.infos.getRawValue()
    return {
      // "2026-10-17" + "20:00" → "2026-10-17T20:00:00" (LocalDateTime côté back)
      date: `${infos.date}T${infos.heure.length === 5 ? infos.heure + ":00" : infos.heure}`,
      lieu: infos.lieu.trim(),
      joueurIds: infos.joueurIds,
      parties: this.parties.controls.map((partie) => ({
        // Le formulaire est valide ici : le jeu, la durée et les rangs sont forcément remplis
        jeuId: partie.controls.jeuId.value as number,
        duree: partie.controls.duree.value as number,
        // Seuls les joueurs qui ont joué la partie ont un score ; "participe" ne sert qu'au formulaire
        scores: partie.controls.scores.controls
          .filter((score) => score.controls.participe.value)
          .map((score) => ({
            joueurId: score.controls.joueurId.value,
            rang: score.controls.rang.value as number,
            points: score.controls.points.value,
            numeroEquipe: score.controls.numeroEquipe.value,
          })),
      })),
    }
  }

  // Le détail d'une soirée est la page #13 : tant que sa route n'existe pas, on affiche un message de succès ici
  private allerAuDetail(id: number): void {
    this.router.navigate(["/soirees", id]).catch(() => {
      this.envoiEnCours = false
      this.soireeCreeeId = id
    })
  }

  private messageErreur(erreur: HttpErrorResponse): string {
    // 400 : une règle du back n'est pas respectée, son message en français s'affiche tel quel
    if (erreur.status === 400 && erreur.error?.message) return erreur.error.message
    if (erreur.status === 0) return "L'API PlayNight ne répond pas. Vérifiez qu'elle est lancée, puis réessayez."
    return "La soirée n'a pas pu être enregistrée. Réessayez dans un instant."
  }

  private creerPartie(): PartieForm {
    return this.fb.group(
      {
        jeuId: this.fb.control<number | null>(null, Validators.required),
        // Nombre entier de minutes, strictement positif (contrôle "duree > 0" du back)
        duree: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]),
        scores: this.fb.array<ScoreForm>([]),
      },
      { validators: (control) => this.verifierNbJoueurs(control as PartieForm) },
    )
  }

  private creerScore(joueurId: number): ScoreForm {
    return this.fb.group(
      {
        joueurId: this.fb.nonNullable.control(joueurId),
        participe: this.fb.nonNullable.control(true),
        rang: this.fb.control<number | null>(null),
        points: this.fb.control<number | null>(null),
        numeroEquipe: this.fb.control<number | null>(null),
      },
      { validators: scoreValide },
    )
  }

  // Une ligne par joueur présent, dans l'ordre de la liste des joueurs.
  // Les lignes déjà saisies sont gardées telles quelles, celles des joueurs retirés disparaissent.
  private synchroniserScores(partie: PartieForm): void {
    const scores = partie.controls.scores
    const existants = new Map(scores.controls.map((score) => [score.controls.joueurId.value, score]))
    const presents = this.joueurs.filter((joueur) => this.estPresent(joueur))
    scores.clear()
    presents.forEach((joueur) => scores.push(existants.get(joueur.id) ?? this.creerScore(joueur.id)))
  }

  // Même règle que le back : le nombre de joueurs de la partie doit être autorisé par le jeu
  private verifierNbJoueurs(partie: PartieForm): ValidationErrors | null {
    const nb = this.nbParticipants(partie)
    if (nb === 0) return { aucunJoueur: true }
    const jeu = this.jeuDe(partie)
    if (jeu?.nbJoueursMin == null || jeu.nbJoueursMax == null) return null
    if (nb < jeu.nbJoueursMin || nb > jeu.nbJoueursMax) {
      return { nbJoueurs: { min: jeu.nbJoueursMin, max: jeu.nbJoueursMax, nb } }
    }
    return null
  }
}
