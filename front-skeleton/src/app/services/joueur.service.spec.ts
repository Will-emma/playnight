import { TestBed } from "@angular/core/testing"
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing"
import { JoueurService } from "./joueur.service"

describe("Contrats HTTP des avatars et du classement personnel", () => {
  let service: JoueurService
  let http: HttpTestingController
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] })
    service = TestBed.inject(JoueurService)
    http = TestBed.inject(HttpTestingController)
  })
  afterEach(() => http.verify())

  it("envoie le DTO de sélection en PUT sur le joueur concerné", () => {
    service.choisirAvatar(42, 7).subscribe()
    const requete = http.expectOne("http://localhost:8080/joueurs/42/avatar")
    expect(requete.request.method).toBe("PUT")
    expect(requete.request.body).toEqual({ avatarId: 7 })
    requete.flush({ id: 42, pseudo: "Léa", avatar: "/assets/avatars/avatar-07.png" })
  })

  it("importe le champ multipart attendu sans imposer une frontière Content-Type", () => {
    const fichier = new File(["test"], "photo.png", { type: "image/png" })
    service.importerAvatar(42, fichier).subscribe()
    const requete = http.expectOne("http://localhost:8080/joueurs/42/avatar")
    expect(requete.request.method).toBe("POST")
    expect((requete.request.body as FormData).get("fichier")).toBe(fichier)
    expect(requete.request.headers.has("Content-Type")).toBeFalse()
    requete.flush({ id: 42, pseudo: "Léa", avatar: "http://localhost:8080/joueurs/avatars/photo.png" })
  })

  it("récupère tous les jeux en un seul appel au classement personnel", () => {
    service.getClassements(42).subscribe()
    const requete = http.expectOne("http://localhost:8080/joueurs/42/classements")
    expect(requete.request.method).toBe("GET")
    requete.flush([])
  })
})
