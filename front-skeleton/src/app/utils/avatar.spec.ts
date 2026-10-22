import { MAX_PHOTO_OCTETS, validerPhoto } from "./avatar"

describe("Validation des photos", () => {
  async function photo(type: string): Promise<File> {
    const canvas = document.createElement("canvas")
    canvas.width = 8
    canvas.height = 8
    const blob = await new Promise<Blob>((resolve) => canvas.toBlob((b) => resolve(b!), type))
    return new File([blob], "photo", { type })
  }

  for (const type of ["image/png", "image/jpeg", "image/webp"]) {
    it(`décode une véritable image ${type}`, async () => {
      await expectAsync(validerPhoto(await photo(type))).toBeResolved()
    })
  }

  it("refuse les fichiers trop lourds", async () => {
    await expectAsync(validerPhoto(new File([new Uint8Array(MAX_PHOTO_OCTETS + 1)], "grand.png", { type: "image/png" })))
      .toBeRejectedWithError("La photo ne doit pas dépasser 2 Mo.")
  })

  it("refuse un faux PNG et un type mensonger", async () => {
    await expectAsync(validerPhoto(new File(["texte"], "faux.png", { type: "image/png" }))).toBeRejected()
    const png = await photo("image/png")
    await expectAsync(validerPhoto(new File([png], "faux.jpg", { type: "image/jpeg" }))).toBeRejected()
  })

  it("refuse une image corrompue malgré une signature valide", async () => {
    await expectAsync(validerPhoto(new File([new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10])], "casse.png", { type: "image/png" })))
      .toBeRejectedWithError("Cette image est illisible ou endommagée.")
  })
})
