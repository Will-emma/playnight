import { NgModule } from "@angular/core"
import { RouterModule, Routes } from "@angular/router"
import { HomeComponent } from "home/home.component"
import { StudentsComponent } from "students/students.component"
import { StudentsResolver } from "students/students.resolver"
import { StudentDetailsComponent } from "students/student-details/student-details.component"
import { StudentDetailsResolver } from "students/student-details/student-details.resolver"
import { MajorsComponent } from "majors/majors.component"
import { MajorsResolver } from "majors/majors.resolver"
import { MajorStudentsResolver } from "majors/major-students/major-students.resolver"
import { MajorStudentsComponent } from "majors/major-students/major-students.component"
import { JeuxComponent } from "jeux/jeux.component"
import { JeuxResolver } from "jeux/jeux.resolver"
import { FicheJeuComponent } from "jeux/fiche-jeu/fiche-jeu.component"
import { FicheJeuResolver } from "jeux/fiche-jeu/fiche-jeu.resolver"
import { ProfilJoueurComponent } from "joueurs/profil-joueur/profil-joueur.component"
import { ProfilJoueurResolver } from "joueurs/profil-joueur/profil-joueur.resolver"
import { NouvelleSoireeComponent } from "soirees/nouvelle-soiree/nouvelle-soiree.component"
import { NouvelleSoireeResolver } from "soirees/nouvelle-soiree/nouvelle-soiree.resolver"

const routes: Routes = [
  { path: "", component: HomeComponent },
  {
    path: "jeux",
    component: JeuxComponent,
    resolve: { jeuxPage: JeuxResolver },
  },
  {
    path: "jeux/:id",
    component: FicheJeuComponent,
    resolve: { ficheJeuPage: FicheJeuResolver },
  },
  {
    path: "joueurs/:id",
    component: ProfilJoueurComponent,
    resolve: { profilJoueurPage: ProfilJoueurResolver },
  },
  {
    // À garder avant une future route "soirees/:id", sinon "nouvelle" serait lu comme un id
    path: "soirees/nouvelle",
    component: NouvelleSoireeComponent,
    resolve: { nouvelleSoireePage: NouvelleSoireeResolver },
  },
  {
    path: "etudiants",
    component: StudentsComponent,
    resolve: {
      students: StudentsResolver,
    },
  },
  {
    path: "details-etudiant/:id",
    component: StudentDetailsComponent,
    resolve: {
      student: StudentDetailsResolver,
    },
  },
  {
    path: "filieres",
    component: MajorsComponent,
    resolve: {
      majors: MajorsResolver,
    },
  },
  {
    path: "etudiants-filiere/:id",
    component: MajorStudentsComponent,
    resolve: {
      studentsFromMajor: MajorStudentsResolver,
    },
  },
]

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
