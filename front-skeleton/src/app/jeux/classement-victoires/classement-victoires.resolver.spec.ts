import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing"
import { TestBed } from "@angular/core/testing"
import { ActivatedRouteSnapshot, convertToParamMap, RouterStateSnapshot } from "@angular/router"
import { firstValueFrom, Observable } from "rxjs"
import { ClassementVictoiresPageData, ClassementVictoiresResolver } from "./classement-victoires.resolver"

describe("Chargement du classement par victoires", () => {
  let http: HttpTestingController
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] })
    http = TestBed.inject(HttpTestingController)
  })
  afterEach(() => http.verify())

  function charger(id: string): Promise<ClassementVictoiresPageData> {
    return firstValueFrom(TestBed.runInInjectionContext(() => ClassementVictoiresResolver(
      { paramMap: convertToParamMap({ id }) } as ActivatedRouteSnapshot,
      {} as RouterStateSnapshot,
    )) as Observable<ClassementVictoiresPageData>)
  }

  it("charge le jeu et le classement dédié sans appeler la fiche ni l'Elo", async () => {
    const resultat = charger("88")
    const jeu = http.expectOne("http://localhost:8080/jeux/88")
    const classement = http.expectOne("http://localhost:8080/jeux/88/classement-victoires")
    expect(jeu.request.method).toBe("GET")
    expect(classement.request.method).toBe("GET")
    jeu.flush({ id: 88, nom: "Catan" })
    classement.flush([{ joueur: { id: 42, pseudo: "Léa", avatar: null }, nbVictoires: 3 }])
    const page = await resultat
    expect(page.jeu?.nom).toBe("Catan")
    expect(page.classement[0].joueur.id).toBe(42)
    expect(page.erreur).toBeNull()
  })

  it("accepte un classement vide pour un jeu existant", async () => {
    const resultat = charger("88")
    http.expectOne("http://localhost:8080/jeux/88").flush({ id: 88, nom: "Catan" })
    http.expectOne("http://localhost:8080/jeux/88/classement-victoires").flush([])
    expect((await resultat).classement).toEqual([])
    expect((await resultat).erreur).toBeNull()
  })

  it("distingue un 404 d'une indisponibilité de l'API", async () => {
    for (const statut of [404, 500]) {
      const resultat = charger("88")
      http.expectOne("http://localhost:8080/jeux/88").flush({ id: 88, nom: "Catan" })
      http.expectOne("http://localhost:8080/jeux/88/classement-victoires")
        .flush({}, { status: statut, statusText: "Erreur" })
      expect((await resultat).erreur).toBe(statut === 404 ? "introuvable" : "indisponible")
    }
  })

  it("rejette les identifiants invalides sans requête HTTP", async () => {
    for (const id of ["abc", "0", "-1", "1.5", "9007199254740992"]) {
      expect((await charger(id)).erreur).toBe("introuvable")
    }
    http.expectNone(() => true)
  })
})
