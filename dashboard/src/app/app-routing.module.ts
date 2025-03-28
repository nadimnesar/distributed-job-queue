import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {DashboardComponent} from "./components/dashboard/dashboard.component";

const routes: Routes = [
    {
        path: '',
        component: DashboardComponent,
        data: {
            title: 'Dashboard | Distributed Job Queue'
        }
    }
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}
