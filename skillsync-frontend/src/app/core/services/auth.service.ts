import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { JwtPayload, LoginRequest, RegisterRequest } from '../models/auth.model';
import { AuthUser, User } from '../models/user.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly storageKey = 'skillsync_token';
  private readonly baseUrl = environment.apiBaseUrl;

  private readonly tokenState = signal<string | null>(this.readToken());
  private readonly profileState = signal<User | null>(this.readUserFromToken());

  readonly token = computed(() => this.tokenState());
  readonly profile = computed(() => this.profileState());
  readonly isAuthenticated = computed(() => !!this.tokenState());
  readonly role = computed(() => this.decodeToken()?.role ?? null);
  readonly userId = computed(() => this.decodeToken()?.userId ?? null);

  login(payload: LoginRequest) {
    return this.api.postText(API_ENDPOINTS.auth.login, payload).pipe(
      tap((token) => this.persistSession(token)),
    );
  }

  register(payload: RegisterRequest) {
    return this.api.postText(API_ENDPOINTS.auth.register, payload);
  }

  verifyRegistration(payload: { email: string; otp: string }) {
    return this.api.postText(API_ENDPOINTS.auth.verifyRegistration, payload);
  }

  refreshProfile() {
    const userId = this.userId();
    if (!userId) {
      return;
    }

    this.api.get<User>(API_ENDPOINTS.auth.userById(userId)).subscribe({
      next: (user) => {
        this.profileState.set(this.decorateUser({
          ...user,
          role: this.role() ?? user.role,
        }));
      },
      error: () => {
        this.profileState.set(this.readUserFromToken());
      },
    });
  }

  getCurrentProfile() {
    return this.api.get<User>(API_ENDPOINTS.auth.currentProfile);
  }

  getUserById(userId: number) {
    return this.api.get<User>(API_ENDPOINTS.auth.userById(userId));
  }

  updateProfile(payload: Partial<User>) {
    return this.api.put<User>(API_ENDPOINTS.auth.currentProfile, payload);
  }

  updateOwnRole(role: 'ROLE_USER' | 'ROLE_MENTOR') {
    return this.api.put<User>(API_ENDPOINTS.auth.updateOwnRole(role));
  }

  uploadProfilePicture(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.api.postFormData<User>(API_ENDPOINTS.auth.uploadProfilePicture, formData);
  }

  uploadBiodata(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.api.postFormData<User>(API_ENDPOINTS.auth.uploadBiodata, formData);
  }

  logout() {
    this.tokenState.set(null);
    this.profileState.set(null);
    if (typeof window !== 'undefined') {
      localStorage.removeItem(this.storageKey);
    }
    void this.router.navigate(['/login']);
  }

  hasRole(expectedRole: string) {
    return this.role() === expectedRole;
  }

  getAuthUser(): AuthUser | null {
    const profile = this.profileState();
    const token = this.tokenState();
    if (!profile || !token) {
      return null;
    }

    return { ...profile, token };
  }

  private persistSession(token: string) {
    this.tokenState.set(token);
    if (typeof window !== 'undefined') {
      localStorage.setItem(this.storageKey, token);
    }
    this.profileState.set(this.readUserFromToken());
    this.refreshProfile();
  }

  private readToken() {
    if (typeof window === 'undefined') {
      return null;
    }

    return localStorage.getItem(this.storageKey);
  }

  private readUserFromToken(): User | null {
    const payload = this.decodeToken();
    if (!payload) {
      return null;
    }

    return {
      id: payload.userId,
      email: payload.sub,
      role: payload.role,
      profilePictureUrl: this.buildProfilePictureUrl(payload.userId),
      biodataUrl: this.buildBiodataUrl(payload.userId),
    };
  }

  private decodeToken(): JwtPayload | null {
    const token = this.tokenState();
    if (!token || typeof window === 'undefined') {
      return null;
    }

    try {
      const payloadChunk = token.split('.')[1];
      const normalized = payloadChunk.replace(/-/g, '+').replace(/_/g, '/');
      const json = window.atob(normalized);
      return JSON.parse(json) as JwtPayload;
    } catch {
      return null;
    }
  }

  private decorateUser(user: User): User {
    return {
      ...user,
      profilePictureUrl: user.hasProfilePicture ? this.buildProfilePictureUrl(user.id) : null,
      biodataUrl: user.hasBiodata ? this.buildBiodataUrl(user.id) : null,
    };
  }

  private buildProfilePictureUrl(userId: number) {
    return `${this.baseUrl}${API_ENDPOINTS.auth.profilePictureByUser(userId)}`;
  }

  private buildBiodataUrl(userId: number) {
    return `${this.baseUrl}${API_ENDPOINTS.auth.biodataByUser(userId)}`;
  }
}
