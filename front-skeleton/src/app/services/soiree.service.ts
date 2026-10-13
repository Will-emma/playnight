import { HttpClient } from "@angular/common/http"
import { Injectable } from "@angular/core"
import { Observable } from "rxjs"
import { SoireeCreation } from "models/soiree.model"

@Injectable({
  providedIn: "root",
})
export class SoireeService {
  private readonly soireesUrl = "http://localhost:8080/soirees"

  constructor(private http: HttpClient) {}

  // Le back renvoie la soirée créée au format du détail ; ici seul son id sert (pour la redirection).
  // Le type complet du détail viendra avec la page détail d'une soirée (#13).
  create(soiree: SoireeCreation): Observable<{ id: number }> {
    return this.http.post<{ id: number }>(this.soireesUrl, soiree)
  }
}
