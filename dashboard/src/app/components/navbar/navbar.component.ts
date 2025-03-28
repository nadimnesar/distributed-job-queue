import {Component} from '@angular/core';
import {Router} from "@angular/router";

@Component({
    selector: 'app-navbar',
    standalone: false,
    templateUrl: './navbar.component.html',
    styleUrl: './navbar.component.scss'
})
export class NavbarComponent {
    isLoading = false;

    constructor(private router: Router) {
    }

    refreshDashboard(): void {
        this.isLoading = true;
        const currentUrl = this.router.url;
        this.router.navigateByUrl('/', {skipLocationChange: true}).then(() => {
            this.router.navigate([currentUrl]).then(() => {
                setTimeout(() => {
                    this.isLoading = false;
                }, 500);
            });
        });
    }
}
