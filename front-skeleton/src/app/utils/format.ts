// Mises en forme partagées par les pages (fiche jeu, profil joueur…)

// Format fourni par le navigateur : "samedi 12 septembre 2026 à 20:00" (la locale française d'Angular n'est pas activée)
const FORMAT_DATE = new Intl.DateTimeFormat("fr-FR", {
  weekday: "long",
  day: "numeric",
  month: "long",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
})

// "2026-09-12T20:00:00" → "samedi 12 septembre 2026 à 20:00"
export function formaterDate(date: string): string {
  return FORMAT_DATE.format(new Date(date))
}

// (1, "victoire") → "1 victoire" ; (3, "victoire") → "3 victoires"
export function pluriel(nombre: number, mot: string): string {
  return `${nombre} ${mot}${nombre > 1 ? "s" : ""}`
}
