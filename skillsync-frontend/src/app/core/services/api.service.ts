import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  get<T>(path: string, params?: Record<string, string | number | boolean>) {
    return this.http.get<T>(`${this.baseUrl}${path}`, {
      params: this.buildParams(params),
    });
  }

  post<T>(path: string, body: unknown) {
    return this.http.post<T>(`${this.baseUrl}${path}`, body);
  }

  postText(path: string, body: unknown) {
    return this.http.post(`${this.baseUrl}${path}`, body, {
      responseType: 'text',
    });
  }

  postFormData<T>(path: string, body: FormData) {
    return this.http.post<T>(`${this.baseUrl}${path}`, body);
  }

  put<T>(path: string, body?: unknown) {
    return this.http.put<T>(`${this.baseUrl}${path}`, body ?? {});
  }

  putText(path: string, body?: unknown) {
    return this.http.put(`${this.baseUrl}${path}`, body ?? {}, {
      responseType: 'text',
    });
  }

  delete<T>(path: string) {
    return this.http.delete<T>(`${this.baseUrl}${path}`);
  }

  deleteWithBody<T>(path: string, body: unknown) {
    return this.http.delete<T>(`${this.baseUrl}${path}`, { body });
  }

  deleteText(path: string) {
    return this.http.delete(`${this.baseUrl}${path}`, {
      responseType: 'text',
    });
  }

  deleteTextWithBody(path: string, body: unknown) {
    return this.http.delete(`${this.baseUrl}${path}`, {
      body,
      responseType: 'text',
    });
  }

  private buildParams(params?: Record<string, string | number | boolean>) {
    if (!params) {
      return undefined;
    }

    let httpParams = new HttpParams();

    for (const [key, value] of Object.entries(params)) {
      httpParams = httpParams.set(key, String(value));
    }

    return httpParams;
  }
}
