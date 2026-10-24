import { Jeu, TypeJeu } from "models/jeu.model"
import { filtrerJeux, tirerJeu } from "./tirage-jeu"

function jeu(id: number, modifications: Partial<Jeu> = {}): Jeu {
  return {
    id, nom: `Jeu ${id}`, type: TypeJeu.SOCIETE, plateforme: null,
    nbJoueursMin: 2, nbJoueursMax: 4, image: null, ...modifications,
  }
}

describe("Filtrage des jeux pour le tirage", () => {
  const societe = jeu(1)
  const video = jeu(2, { type: TypeJeu.JEU_VIDEO })
  const catalogue = [societe, video]

  it("conserve tous les types par défaut et avec TOUS", () => {
    expect(filtrerJeux(catalogue)).toEqual(catalogue)
    expect(filtrerJeux(catalogue, { type: "TOUS" })).toEqual(catalogue)
  })

  it("sélectionne uniquement les jeux de société", () => {
    expect(filtrerJeux(catalogue, { type: TypeJeu.SOCIETE })).toEqual([societe])
  })

  it("sélectionne uniquement les jeux vidéo", () => {
    expect(filtrerJeux(catalogue, { type: TypeJeu.JEU_VIDEO })).toEqual([video])
  })

  for (const nombreJoueurs of [2, 3, 4]) {
    it(`inclut ${nombreJoueurs} joueurs dans la plage 2–4, bornes comprises`, () => {
      expect(filtrerJeux(catalogue, { nombreJoueurs })).toEqual(catalogue)
    })
  }

  it("écarte les jeux sous leur minimum ou au-dessus de leur maximum", () => {
    expect(filtrerJeux(catalogue, { nombreJoueurs: 1 })).toEqual([])
    expect(filtrerJeux(catalogue, { nombreJoueurs: 5 })).toEqual([])
  })

  it("gère une limite fixe, minimum égal au maximum", () => {
    const duel = jeu(3, { nbJoueursMin: 2, nbJoueursMax: 2 })
    expect(filtrerJeux([duel], { nombreJoueurs: 2 })).toEqual([duel])
    expect(filtrerJeux([duel], { nombreJoueurs: 3 })).toEqual([])
  })

  it("accepte un jeu solo pour un joueur", () => {
    const solo = jeu(3, { nbJoueursMin: 1, nbJoueursMax: 1 })
    expect(filtrerJeux([solo], { nombreJoueurs: 1 })).toEqual([solo])
  })

  for (const nombreJoueurs of [0, -1, 2.5, NaN, Infinity, -Infinity, Number.MAX_SAFE_INTEGER + 1]) {
    it(`ne retourne aucun candidat avec un effectif invalide : ${nombreJoueurs}`, () => {
      expect(filtrerJeux(catalogue, { nombreJoueurs })).toEqual([])
    })
  }

  it("désactive le filtre d'effectif quand le nombre est null ou undefined", () => {
    expect(filtrerJeux(catalogue, { nombreJoueurs: null })).toEqual(catalogue)
    expect(filtrerJeux(catalogue, { nombreJoueurs: undefined })).toEqual(catalogue)
  })

  it("conserve les limites inconnues sans filtre mais les écarte avec un effectif précis", () => {
    const incomplets = [
      jeu(3, { nbJoueursMin: null }),
      jeu(4, { nbJoueursMax: null }),
      jeu(5, { nbJoueursMin: null, nbJoueursMax: null }),
    ]
    expect(filtrerJeux(incomplets)).toEqual(incomplets)
    expect(filtrerJeux(incomplets, { nombreJoueurs: 3 })).toEqual([])
  })

  it("écarte les limites incohérentes ou non entières avec un effectif précis", () => {
    const invalides = [
      jeu(3, { nbJoueursMin: 4, nbJoueursMax: 2 }),
      jeu(4, { nbJoueursMin: 0 }),
      jeu(5, { nbJoueursMax: -1 }),
      jeu(6, { nbJoueursMin: 1.5 }),
      jeu(7, { nbJoueursMax: 4.5 }),
      jeu(8, { nbJoueursMin: NaN }),
      jeu(9, { nbJoueursMax: Infinity }),
    ]
    expect(filtrerJeux(invalides, { nombreJoueurs: 3 })).toEqual([])
  })

  it("exclut par identifiant et ignore les identifiants absents du catalogue", () => {
    expect(filtrerJeux(catalogue, { idsExclus: new Set([societe.id, 999]) })).toEqual([video])
  })

  it("distingue deux jeux de même nom par leur identifiant", () => {
    const homonymes = [jeu(10, { nom: "Même nom" }), jeu(11, { nom: "Même nom" })]
    expect(filtrerJeux(homonymes, { idsExclus: new Set([10]) })).toEqual([homonymes[1]])
  })

  it("combine type, effectif et exclusions", () => {
    const autreVideo = jeu(3, { type: TypeJeu.JEU_VIDEO, nbJoueursMax: 8 })
    expect(filtrerJeux([...catalogue, autreVideo], {
      type: TypeJeu.JEU_VIDEO, nombreJoueurs: 4, idsExclus: new Set([video.id]),
    })).toEqual([autreVideo])
  })

  it("gère un catalogue vide et l'exclusion de tous les jeux", () => {
    expect(filtrerJeux([])).toEqual([])
    expect(filtrerJeux(catalogue, { idsExclus: new Set([1, 2]) })).toEqual([])
  })

  it("ne modifie ni la liste, ni les jeux, ni les exclusions et conserve l'ordre", () => {
    const jeux = Object.freeze([Object.freeze(jeu(12)), Object.freeze(jeu(13))])
    const idsExclus = new Set([999])
    const resultat = filtrerJeux(jeux, { nombreJoueurs: 3, idsExclus })
    expect(resultat).toEqual(jeux)
    expect(resultat).not.toBe(jeux)
    expect(resultat[0]).toBe(jeux[0])
    expect([...idsExclus]).toEqual([999])
  })
})

describe("Sélection aléatoire d'un jeu", () => {
  const candidats = [jeu(1), jeu(2), jeu(3), jeu(4)]

  it("retourne null sans candidat, sans appeler le hasard", () => {
    const hasard = jasmine.createSpy("hasard")
    expect(tirerJeu([], hasard)).toBeNull()
    expect(hasard).not.toHaveBeenCalled()
  })

  it("retourne l'unique candidat sans appeler le hasard", () => {
    const hasard = jasmine.createSpy("hasard")
    expect(tirerJeu([candidats[0]], hasard)).toBe(candidats[0])
    expect(hasard).not.toHaveBeenCalled()
  })

  it("attribue un intervalle égal à chaque candidat, y compris aux frontières", () => {
    const exemples = [
      { valeur: 0, index: 0 }, { valeur: 0.249999, index: 0 },
      { valeur: 0.25, index: 1 }, { valeur: 0.499999, index: 1 },
      { valeur: 0.5, index: 2 }, { valeur: 0.749999, index: 2 },
      { valeur: 0.75, index: 3 }, { valeur: 0.999999, index: 3 },
    ]
    for (const { valeur, index } of exemples) {
      expect(tirerJeu(candidats, () => valeur)).toBe(candidats[index])
    }
  })

  it("utilise un seul appel au hasard par tirage", () => {
    const hasard = jasmine.createSpy("hasard").and.returnValue(0.5)
    expect(tirerJeu(candidats, hasard)).toBe(candidats[2])
    expect(hasard).toHaveBeenCalledTimes(1)
  })

  it("utilise Math.random par défaut", () => {
    const hasard = spyOn(Math, "random").and.returnValue(0.25)
    expect(tirerJeu(candidats)).toBe(candidats[1])
    expect(hasard).toHaveBeenCalledTimes(1)
  })

  for (const valeur of [-0.1, 1, 2, NaN, Infinity, -Infinity]) {
    it(`signale une source de hasard hors contrat : ${valeur}`, () => {
      expect(() => tirerJeu(candidats, () => valeur)).toThrowError(RangeError)
    })
  }

  it("ne sélectionne que les jeux filtrés, sans les retirer du prochain tirage", () => {
    const eligibles = Object.freeze(filtrerJeux(candidats, { idsExclus: new Set([1, 3]) }))
    expect(tirerJeu(eligibles, () => 0)).toBe(candidats[1])
    expect(tirerJeu(eligibles, () => 0)).toBe(candidats[1])
    expect(tirerJeu(eligibles, () => 0.999999)).toBe(candidats[3])
    expect(eligibles).toEqual([candidats[1], candidats[3]])
  })
})
