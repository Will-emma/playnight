
import { HttpClient } from "@angular/common/http"
import { Injectable } from "@angular/core"
import { Observable } from "rxjs"
import { Joueur, ProfilJoueur } from "models/joueur.model"

@Injectable({
  providedIn: "root",
})
export class JoueurService {
  private readonly joueursUrl = "http://localhost:8080/joueurs"

  constructor(private http: HttpClient) {}

  findAll(): Observable<Joueur[]> {
    return this.http.get<Joueur[]>(this.joueursUrl)
  }

  create(joueur: Pick<Joueur, "pseudo" | "avatar">): Observable<Joueur> {
    return this.http.post<Joueur>(this.joueursUrl, joueur)
  }

  getProfil(id: number): Observable<ProfilJoueur> {
    return this.http.get<ProfilJoueur>(`${this.joueursUrl}/${id}/profil`)
  }
}
