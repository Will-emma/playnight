import { Component, OnInit } from "@angular/core"
import { ActivatedRoute } from "@angular/router"
import { SoireeResume } from "models/soiree.model"
import { formaterDate, pluriel } from "utils/format"
import { HomePageData } from "./home.resolver"

// Nombre de soirées récentes montrées sur l'accueil ; les autres sont dans la liste des soirées
const NB_SOIREES_RECENTES = 3

@Component({
  selector: "app-home",
  templateUrl: "./home.component.html",
  styleUrls: ["./home.component.scss"],
})
export class HomeComponent implements OnInit {
  readonly formaterDate = formaterDate
  readonly pluriel = pluriel

  nbJeux = 0
  nbSoirees = 0
  nbParties = 0
  soireesRecentes: SoireeResume[] = []
  erreur: HomePageData["erreur"] = null

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.route.data.subscribe((data) => {
      const page: HomePageData = data["homePage"]
      this.nbJeux = page.nbJeux
      this.nbSoirees = page.soirees.length
      // Total des parties de toutes les soirées
      this.nbParties = page.soirees.reduce((total, soiree) => total + soiree.nbParties, 0)
      // Les soirées arrivent de la plus récente à la plus ancienne : les premières sont les plus récentes
      this.soireesRecentes = page.soirees.slice(0, NB_SOIREES_RECENTES)
      this.erreur = page.erreur
    })
  }
}
