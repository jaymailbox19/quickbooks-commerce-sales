import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

interface LoginResponse {
  token: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, {
      username,
      password
    }).pipe(
      tap(response => {
        sessionStorage.setItem('jwt_token', response.token);
        sessionStorage.setItem('logged_in_username', username);
      })
    );
  }

  getToken(): string | null {
    return sessionStorage.getItem('jwt_token');
  }

  getUsername(): string | null {
    return sessionStorage.getItem('logged_in_username');
  }

  logout(): void {
    sessionStorage.removeItem('jwt_token');
    sessionStorage.removeItem('logged_in_username');
  }
}