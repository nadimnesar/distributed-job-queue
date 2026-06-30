import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { CommonResponse } from '../models/api.model';
import { Job, JobRequest, JobStatus, JobType } from '../models/job.model';

@Injectable({ providedIn: 'root' })
export class JobService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/v1`;

  create(request: JobRequest): Observable<CommonResponse<Job>> {
    return this.http.post<CommonResponse<Job>>(`${this.base}/job/create`, request);
  }

  list(page = 0, size = 20): Observable<CommonResponse<Job[]>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<CommonResponse<Job[]>>(`${this.base}/jobs`, { params });
  }

  get(id: string): Observable<CommonResponse<Job>> {
    return this.http.get<CommonResponse<Job>>(`${this.base}/jobs/${id}`);
  }

  filter(status?: JobStatus, type?: JobType, page = 0, size = 20): Observable<CommonResponse<Job[]>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (type) params = params.set('type', type);
    return this.http.get<CommonResponse<Job[]>>(`${this.base}/jobs/filter`, { params });
  }

  cancel(id: string): Observable<CommonResponse<any>> {
    return this.http.post<CommonResponse<any>>(`${this.base}/jobs/${id}/cancel`, {});
  }

  revive(id: string): Observable<CommonResponse<string>> {
    return this.http.post<CommonResponse<string>>(`${this.base}/jobs/${id}/revive`, {});
  }

  reviveAll(): Observable<CommonResponse<string[]>> {
    return this.http.post<CommonResponse<string[]>>(`${this.base}/jobs/revive`, {});
  }
}
