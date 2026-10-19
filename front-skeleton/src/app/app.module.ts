import { NgModule } from "@angular/core"
import { BrowserModule } from "@angular/platform-browser"

import { AppRoutingModule } from "app-routing.module"
import { AppComponent } from "app.component"
import { BrowserAnimationsModule } from "@angular/platform-browser/animations"
import { NavbarComponent } from "navbar/navbar.component"
import { MatListModule } from "@angular/material/list"
import { HomeComponent } from "home/home.component"
import { StudentsComponent } from "students/students.component"
import { StudentDetailsComponent } from "students/student-details/student-details.component"
import { FormsModule, ReactiveFormsModule } from "@angular/forms"
import { MatIconModule } from "@angular/material/icon"
import { MatButtonModule } from "@angular/material/button"
import { MajorsComponent } from "majors/majors.component"
import { MajorStudentsComponent } from "majors/major-students/major-students.component"
import { HttpClientModule } from "@angular/common/http"
import { JeuxComponent } from "jeux/jeux.component"
import { FicheJeuComponent } from "jeux/fiche-jeu/fiche-jeu.component"
import { ImporterJeuComponent } from "jeux/importer-jeu/importer-jeu.component"
import { ProfilJoueurComponent } from "joueurs/profil-joueur/profil-joueur.component"
import { MatStepperModule } from "@angular/material/stepper"
import { NouvelleSoireeComponent } from "soirees/nouvelle-soiree/nouvelle-soiree.component"
import { DetailSoireeComponent } from "soirees/detail-soiree/detail-soiree.component"
import { SoireesComponent } from "soirees/soirees.component"

@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    HomeComponent,
    StudentsComponent,
    StudentDetailsComponent,
    MajorsComponent,
    MajorStudentsComponent,
    JeuxComponent,
    NouvelleSoireeComponent,
    FicheJeuComponent,
    ProfilJoueurComponent,
    DetailSoireeComponent,
    SoireesComponent,
    ImporterJeuComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MatListModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    HttpClientModule,
    ReactiveFormsModule,
    MatStepperModule,
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {
}
