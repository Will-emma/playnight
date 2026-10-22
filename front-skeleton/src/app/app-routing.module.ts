
import { NgModule } from "@angular/core"
import { RouterModule, Routes } from "@angular/router"
import { HomeComponent } from "home/home.component"
import { HomeResolver } from "home/home.resolver"
import { JeuxComponent } from "jeux/jeux.component"
import { JeuxResolver } from "jeux/jeux.resolver"
import { FicheJeuComponent } from "jeux/fiche-jeu/fiche-jeu.component"
import { FicheJeuResolver } from "jeux/fiche-jeu/fiche-jeu.resolver"
import { ImporterJeuComponent } from "jeux/importer-jeu/importer-jeu.component"
import { JoueursComponent } from "joueurs/joueurs.component"
import { JoueursResolver } from "joueurs/joueurs.resolver"
import { ProfilJoueurComponent } from "joueurs/profil-joueur/profil-joueur.component"
import { ProfilJoueurResolver } from "joueurs/profil-joueur/profil-joueur.resolver"
import { NouvelleSoireeComponent } from "soirees/nouvelle-soiree/nouvelle-soiree.component"
import { NouvelleSoireeResolver } from "soirees/nouvelle-soiree/nouvelle-soiree.resolver"
import { DetailSoireeComponent } from "soirees/detail-soiree/detail-soiree.component"
import { DetailSoireeResolver } from "soirees/detail-soiree/detail-soiree.resolver"
import { SoireesComponent } from "soirees/soirees.component"
import { SoireesResolver } from "soirees/soirees.resolver"

const routes: Routes = [
  {
    path: "",
    component: HomeComponent,
    resolve: { homePage: HomeResolver },
  },
  {
    path: "jeux",
    component: JeuxComponent,
    resolve: { jeuxPage: JeuxResolver },
  },
  {
    // À garder avant "jeux/:id", sinon "importer" serait lu comme un id
    // Pas de resolver : la page ne charge rien avant que l'utilisateur lance une recherche
    path: "jeux/importer",
    component: ImporterJeuComponent,
  },
  {
    path: "jeux/:id",
    component: FicheJeuComponent,
    resolve: { ficheJeuPage: FicheJeuResolver },
  },
  {
    path: "joueurs",
    component: JoueursComponent,
    resolve: { joueursPage: JoueursResolver },
  },
  {
    path: "joueurs/:id",
    component: ProfilJoueurComponent,
    resolve: { profilJoueurPage: ProfilJoueurResolver },
  },
  {
    path: "soirees",
    component: SoireesComponent,
    resolve: { soireesPage: SoireesResolver },
  },
  {
    // À garder avant "soirees/:id", sinon "nouvelle" serait lu comme un id
    path: "soirees/nouvelle",
    component: NouvelleSoireeComponent,
    resolve: { nouvelleSoireePage: NouvelleSoireeResolver },
  },
  {
    path: "soirees/:id",
    component: DetailSoireeComponent,
    resolve: { detailSoireePage: DetailSoireeResolver },
  },
  {
    path: "regles-elo",
    loadComponent: () => import("./regles-elo/regles-elo.component").then(m => m.ReglesEloComponent),
    title: "Règles Elo · PlayNight",
  },
  {
    // À garder en dernier : toute adresse inconnue renvoie vers l'accueil
    path: "**",
    redirectTo: "",
  },
]

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
