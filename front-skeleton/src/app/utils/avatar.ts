// Galerie originale créée pour PlayNight, sans ressource externe.
export const AVATARS = [
  "Pixel à lunettes", "Tresse rousse", "Boucles violettes", "Casque doré",
  "Carré argenté", "Chignons roses", "Barbe et émeraude", "Bandana rouge",
  "Crête verte", "Casquette turquoise", "Boucles argentées", "Lunettes noires",
].map((nom, index) => ({ id: index + 1, nom, url: `/assets/avatars/avatar-${String(index + 1).padStart(2, "0")}.png` }))

export const MAX_PHOTO_OCTETS = 2 * 1024 * 1024

export async function validerPhoto(fichier: File): Promise<void> {
  if (fichier.size > MAX_PHOTO_OCTETS) throw new Error("La photo ne doit pas dépasser 2 Mo.")
  const signature = new Uint8Array(await fichier.slice(0, 12).arrayBuffer())
  const correspond = (octets: number[], debut = 0): boolean => octets.every((v, i) => signature[debut + i] === v)
  const type = correspond([137, 80, 78, 71, 13, 10, 26, 10]) ? "image/png"
    : correspond([255, 216, 255]) ? "image/jpeg"
    : correspond([82, 73, 70, 70]) && correspond([87, 69, 66, 80], 8) ? "image/webp" : null
  if (!type || type !== fichier.type) throw new Error("Sélectionnez une véritable image JPEG, PNG ou WebP.")
  let image: ImageBitmap
  try {
    image = await createImageBitmap(fichier)
  } catch {
    throw new Error("Cette image est illisible ou endommagée.")
  }
  try {
    if (image.width > 4096 || image.height > 4096 || image.width * image.height > 16_000_000) {
      throw new Error("La photo doit mesurer au maximum 4096 pixels de côté et 16 mégapixels.")
    }
  } finally {
    image.close()
  }
}
