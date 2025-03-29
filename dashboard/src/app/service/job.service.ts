// dashboard/src/app/services/job.service.ts
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class JobService {
    private apiUrl = "url";

    constructor(private http: HttpClient) {
    }

    getJobStats(): Observable<any> {
        return this.http.get(`${this.apiUrl}/api/jobs/stats`);
    }

    getRecentJobs(limit: number = 10): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/api/jobs/recent?limit=${limit}`);
    }

    getWorkers(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/api/workers`);
    }

    getQueueLength(): Observable<number> {
        return this.http.get<number>(`${this.apiUrl}/api/queue/length`);
    }
}
