import { Component } from "@angular/core"
import { Link } from "models/links.model"
import { MatDialog } from "@angular/material/dialog"
import { TirageJeuDialogComponent } from "jeux/tirage-jeu/tirage-jeu-dialog.component"

@Component({
  selector: "navbar",
  templateUrl: "./navbar.component.html",
  styleUrls: ["./navbar.component.scss"],
})
export class NavbarComponent {
  links: Link[] = []

  constructor(private dialog: MatDialog) {
    this.links.push({ name: "Ludothèque", href: "jeux" })
    this.links.push({ name: "Joueurs", href: "joueurs" })
    this.links.push({ name: "Soirées", href: "soirees" })
  }

  ouvrirTirage(): void {
    if (this.dialog.getDialogById("tirage-jeu-global")) return
    this.dialog.open(TirageJeuDialogComponent, {
      id: "tirage-jeu-global", data: { typeInitial: "TOUS" },
      width: "680px", maxWidth: "94vw", maxHeight: "90vh",
      ariaLabelledBy: "tirage-jeu-titre", restoreFocus: true,
    })
  }
}
