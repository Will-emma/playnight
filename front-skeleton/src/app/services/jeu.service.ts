import { HttpClient, HttpParams } from "@angular/common/http"
import { Injectable } from "@angular/core"
import { Observable } from "rxjs"
import { ClassementElo, FicheJeu, Jeu, RawgImport, RawgJeu, TypeJeu } from "models/jeu.model"

@Injectable({
  providedIn: "root",
})
export class JeuService {
  private readonly jeuxUrl = "http://localhost:8080/jeux"

  constructor(private http: HttpClient) {}

  findAll(type?: TypeJeu): Observable<Jeu[]> {
    const params = type ? new HttpParams().set("type", type) : undefined
    return this.http.get<Jeu[]>(this.jeuxUrl, { params })
  }

  getFiche(id: number): Observable<FicheJeu> {
    return this.http.get<FicheJeu>(`${this.jeuxUrl}/${id}/fiche`)
  }

  getClassement(id: number): Observable<ClassementElo[]> {
    return this.http.get<ClassementElo[]>(`${this.jeuxUrl}/${id}/classement`)
  }

  rechercherSurRawg(recherche: string): Observable<RawgJeu[]> {
    // HttpParams encode la recherche (espaces, accents, &…)
    return this.http.get<RawgJeu[]>(`${this.jeuxUrl}/rawg`, { params: new HttpParams().set("recherche", recherche) })
  }

  // Renvoie le jeu créé dans la ludothèque
  importerDepuisRawg(idRawg: number, importJeu: RawgImport): Observable<Jeu> {
    return this.http.post<Jeu>(`${this.jeuxUrl}/rawg/${idRawg}`, importJeu)
  }

  // Refusé par l'API (409) si le jeu a déjà été joué
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.jeuxUrl}/${id}`)
  }
}
