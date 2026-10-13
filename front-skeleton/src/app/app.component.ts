import { Component } from "@angular/core"
import { NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router } from "@angular/router"

@Component({
  selector: "root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.scss"],
})
export class AppComponent {
  title = "PlayNight"
  loadingRoute = false

  constructor(router: Router) {
    router.events.subscribe((event) => {
      if (event instanceof NavigationStart) this.loadingRoute = true
      if (event instanceof NavigationEnd || event instanceof NavigationCancel || event instanceof NavigationError) {
        this.loadingRoute = false
      }
    })
  }
}
