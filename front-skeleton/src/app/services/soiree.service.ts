import { HttpClient } from "@angular/common/http"
import { Injectable } from "@angular/core"
import { Observable } from "rxjs"
import { SoireeCreation, SoireeDetail, SoireeResume } from "models/soiree.model"

@Injectable({
  providedIn: "root",
})
export class SoireeService {
  private readonly soireesUrl = "http://localhost:8080/soirees"

  constructor(private http: HttpClient) {}

  findAll(): Observable<SoireeResume[]> {
    return this.http.get<SoireeResume[]>(this.soireesUrl)
  }

  findById(id: number): Observable<SoireeDetail> {
    return this.http.get<SoireeDetail>(`${this.soireesUrl}/${id}`)
  }

  // Le back renvoie la soirée créée au format du détail
  create(soiree: SoireeCreation): Observable<SoireeDetail> {
    return this.http.post<SoireeDetail>(this.soireesUrl, soiree)
  }

  // Supprime aussi les parties et les scores de la soirée
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.soireesUrl}/${id}`)
  }
}
