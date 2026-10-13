import { HttpClient } from "@angular/common/http"
import { Injectable } from "@angular/core"
import { Observable } from "rxjs"
import { SoireeCreation, SoireeDetail } from "models/soiree.model"

@Injectable({
  providedIn: "root",
})
export class SoireeService {
  private readonly soireesUrl = "http://localhost:8080/soirees"

  constructor(private http: HttpClient) {}

  findById(id: number): Observable<SoireeDetail> {
    return this.http.get<SoireeDetail>(`${this.soireesUrl}/${id}`)
  }

  // Le back renvoie la soirée créée au format du détail
  create(soiree: SoireeCreation): Observable<SoireeDetail> {
    return this.http.post<SoireeDetail>(this.soireesUrl, soiree)
  }
}
