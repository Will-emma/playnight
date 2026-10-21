import { Component } from "@angular/core"
import { Link } from "models/links.model"

@Component({
  selector: "navbar",
  templateUrl: "./navbar.component.html",
  styleUrls: ["./navbar.component.scss"],
})
export class NavbarComponent {
  links: Link[] = []

  constructor() {
    this.links.push({ name: "Ludothèque", href: "jeux" })
    this.links.push({ name: "Joueurs", href: "joueurs" })
    this.links.push({ name: "Filières", href: "filieres" })
  }
}
