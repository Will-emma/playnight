import { CommonModule } from "@angular/common"
import { Component, OnInit } from "@angular/core"
import { ActivatedRoute, RouterModule } from "@angular/router"
import { MeilleurJoueur } from "models/jeu.model"
import { pluriel } from "utils/format"
import { ClassementVictoiresPageData } from "./classement-victoires.resolver"

@Component({
  selector: "app-classement-victoires",
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: "./classement-victoires.component.html",
  styleUrls: ["./classement-victoires.component.scss"],
})
export class ClassementVictoiresComponent implements OnInit {
  page: ClassementVictoiresPageData = { jeuId: null, jeu: null, classement: [], erreur: null }
  lignes: (MeilleurJoueur & { rang: number })[] = []
  readonly avatarsIndisponibles = new Set<string>()
  readonly pluriel = pluriel

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.route.data.subscribe((data) => {
      this.page = data["classementVictoiresPage"]
      this.avatarsIndisponibles.clear()
      let rang = 0
      this.lignes = this.page.classement.map((ligne, index, classement) => {
        if (index === 0 || ligne.nbVictoires !== classement[index - 1].nbVictoires) rang = index + 1
        return { ...ligne, rang }
      })
    })
  }

  avatarDisponible(avatar: string | null): boolean {
    return !!avatar && !this.avatarsIndisponibles.has(avatar)
  }

  initiale(pseudo: string): string {
    return pseudo.trim().charAt(0).toLocaleUpperCase("fr-FR")
  }
}
