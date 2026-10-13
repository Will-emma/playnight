import { DOCUMENT } from "@angular/common"
import { Inject, Injectable } from "@angular/core"

// Thème de toute l'appli : la classe "light-theme" posée sur <body> active le mode clair (couleurs Takima).
// Le mode sombre reste le mode par défaut.
@Injectable({
  providedIn: "root",
})
export class ThemeService {
  private readonly cleStockage = "playnight-theme"
  darkMode = true

  constructor(@Inject(DOCUMENT) private document: Document) {
    this.darkMode = this.lireChoix() !== "light"
    this.appliquer()
  }

  basculer(): void {
    this.darkMode = !this.darkMode
    this.appliquer()
    try {
      localStorage.setItem(this.cleStockage, this.darkMode ? "dark" : "light")
    } catch {
      // Stockage indisponible (navigation privée…) : le choix ne sera simplement pas mémorisé
    }
  }

  private lireChoix(): string | null {
    try {
      return localStorage.getItem(this.cleStockage)
    } catch {
      return null
    }
  }

  private appliquer(): void {
    this.document.body.classList.toggle("light-theme", !this.darkMode)
  }
}
