
import { HttpClient } from "@angular/common/http"
import { Injectable } from "@angular/core"
import { Observable } from "rxjs"
import { ClassementGlobal, ClassementJoueur, Joueur, ProfilJoueur, ProgressionElo } from "models/joueur.model"

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

  choisirAvatar(id: number, avatarId: number): Observable<Joueur> {
    return this.http.put<Joueur>(`${this.joueursUrl}/${id}/avatar`, { avatarId })
  }

  importerAvatar(id: number, fichier: File): Observable<Joueur> {
    const donnees = new FormData()
    donnees.append("fichier", fichier)
    return this.http.post<Joueur>(`${this.joueursUrl}/${id}/avatar`, donnees)
  }

  getProgressionElo(id: number): Observable<ProgressionElo> {
    return this.http.get<ProgressionElo>(`${this.joueursUrl}/${id}/progression-elo`)
  }

  getClassementGlobal(id: number): Observable<ClassementGlobal> {
    return this.http.get<ClassementGlobal>(`${this.joueursUrl}/${id}/classement-global`)
  }

  getClassements(id: number): Observable<ClassementJoueur[]> {
    return this.http.get<ClassementJoueur[]>(`${this.joueursUrl}/${id}/classements`)
  }

  getProfil(id: number): Observable<ProfilJoueur> {
    return this.http.get<ProfilJoueur>(`${this.joueursUrl}/${id}/profil`)
  }
}
