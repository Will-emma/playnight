import { HttpClient, HttpParams } from "@angular/common/http"
import { Injectable } from "@angular/core"
import { Observable } from "rxjs"
import { Jeu, TypeJeu } from "models/jeu.model"

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
}
