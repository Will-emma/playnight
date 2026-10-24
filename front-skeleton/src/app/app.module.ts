
import { EloProgressionComponent } from "joueurs/elo-progression/elo-progression.component"
import { MatDialogModule } from "@angular/material/dialog"
import { MatTabsModule } from "@angular/material/tabs"
import { MatTooltipModule } from "@angular/material/tooltip"
import { AvatarDialogComponent } from "joueurs/avatar-dialog/avatar-dialog.component"
import { NgModule } from "@angular/core"
import { BrowserModule } from "@angular/platform-browser"

import { AppRoutingModule } from "app-routing.module"
import { AppComponent } from "app.component"
import { BrowserAnimationsModule } from "@angular/platform-browser/animations"
import { NavbarComponent } from "navbar/navbar.component"
import { MatListModule } from "@angular/material/list"
import { HomeComponent } from "home/home.component"
import { FormsModule, ReactiveFormsModule } from "@angular/forms"
import { HttpClientModule } from "@angular/common/http"
import { JeuxComponent } from "jeux/jeux.component"
import { FicheJeuComponent } from "jeux/fiche-jeu/fiche-jeu.component"
import { ImporterJeuComponent } from "jeux/importer-jeu/importer-jeu.component"
import { ProfilJoueurComponent } from "joueurs/profil-joueur/profil-joueur.component"
import { JoueursComponent } from "joueurs/joueurs.component"
import { MatStepperModule } from "@angular/material/stepper"
import { NouvelleSoireeComponent } from "soirees/nouvelle-soiree/nouvelle-soiree.component"
import { DetailSoireeComponent } from "soirees/detail-soiree/detail-soiree.component"
import { SoireesComponent } from "soirees/soirees.component"
import { SuppressionComponent } from "./suppression/suppression.component"

@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    HomeComponent,
    JeuxComponent,
    NouvelleSoireeComponent,
    FicheJeuComponent,
    ProfilJoueurComponent,
    DetailSoireeComponent,
    SoireesComponent,
    ImporterJeuComponent,
    JoueursComponent,
    AvatarDialogComponent,
    EloProgressionComponent,
    SuppressionComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MatListModule,
    MatDialogModule,
    MatTabsModule,
    MatTooltipModule,
    FormsModule,
    HttpClientModule,
    ReactiveFormsModule,
    MatStepperModule,
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
