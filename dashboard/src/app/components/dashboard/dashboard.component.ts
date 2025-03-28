import {Component, OnInit} from '@angular/core';
import {JobService} from '../../service/job.service';
import {interval} from "rxjs";

@Component({
    selector: 'app-dashboard',
    standalone: false,
    templateUrl: './dashboard.component.html',
    styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
    jobStats = {
        pending: 0,
        processing: 0,
        completed: 0,
        failed: 0,
        canceled: 0,
        total: 0
    };

    recentJobs: any[] = [];
    workers: any[] = [];
    queueLength: number = 0;

    loading = true;
    error: string | null = null;

    constructor(private jobService: JobService) {
    }

    ngOnInit(): void {
        this.loadDashboardData();

        // Refresh data every 10 seconds
        interval(10000).subscribe(() => {
            this.loadDashboardData();
        });
    }

    loadDashboardData(): void {
        this.loading = true;

        this.jobService.getJobStats().subscribe({
            next: (stats) => {
                this.jobStats = stats;
                this.loading = false;
            },
            error: (err) => {
                this.error = 'Failed to load job statistics';
                this.loading = false;
            }
        });

        this.jobService.getRecentJobs().subscribe({
            next: (jobs) => {
                this.recentJobs = jobs;
            },
            error: (err) => {
                this.error = 'Failed to load recent jobs';
            }
        });

        this.jobService.getWorkers().subscribe({
            next: (workers) => {
                this.workers = workers;
            },
            error: (err) => {
                this.error = 'Failed to load worker information';
            }
        });

        this.jobService.getQueueLength().subscribe({
            next: (length) => {
                this.queueLength = length;
            },
            error: (err) => {
                this.error = 'Failed to load queue length';
            }
        });
    }
}
