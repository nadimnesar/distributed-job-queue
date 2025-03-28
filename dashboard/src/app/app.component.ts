import {Component} from '@angular/core';
import {Title} from "@angular/platform-browser";
import {ActivatedRoute, NavigationEnd, Router} from "@angular/router";
import {filter, map} from "rxjs";

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    standalone: false,
    styleUrl: './app.component.scss'
})
export class AppComponent {
    constructor(
        private titleService: Title,
        private router: Router,
        private activatedRoute: ActivatedRoute
    ) {
        this.router.events.pipe(
            filter(event => event instanceof NavigationEnd),
            map(() => {
                let route = this.activatedRoute;
                while (route.firstChild) {
                    route = route.firstChild;
                }
                return route.snapshot.data['title'] || 'Distributed Job Queue';
            })
        ).subscribe((title: string) => {
            this.titleService.setTitle(title);
        });
    }
}
