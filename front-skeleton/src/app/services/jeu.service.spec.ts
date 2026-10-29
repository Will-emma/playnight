import { TestBed } from "@angular/core/testing"
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing"
import { JeuService } from "./jeu.service"
import { JeuCreation, TypeJeu } from "models/jeu.model"

describe("Création d’un jeu avec image", () => {
  it("envoie jeu et fichier en multipart sur POST /jeux", async () => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] })
    const http = TestBed.inject(HttpTestingController)
    const jeu: JeuCreation = { nom: "Mon jeu", type: TypeJeu.SOCIETE, nbJoueursMin: 2, nbJoueursMax: 4, image: null, description: null, plateforme: null, dureeEstimee: null }
    const fichier = new File(["image"], "jeu.png", { type: "image/png" })
    TestBed.inject(JeuService).createAvecImage(jeu, fichier).subscribe()
    const requete = http.expectOne("http://localhost:8080/jeux")
    expect(requete.request.method).toBe("POST")
    const corps = requete.request.body as FormData
    expect(corps.get("fichier")).toBe(fichier)
    const donnees = corps.get("jeu") as Blob
    expect(donnees.type).toBe("application/json")
    expect(JSON.parse(await donnees.text())).toEqual(jeu)
    expect(requete.request.headers.has("Content-Type")).toBeFalse()
    requete.flush({ id: 89, ...jeu })
    http.verify()
  })
})
