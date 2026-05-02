import { User } from '../../../core/models/user.model';
import { Injectable, inject } from '@angular/core';
import { API_ENDPOINTS } from '../../../core/constants/api-endpoints';
import { ApiService } from '../../../core/services/api.service';

export interface SessionRequest {
  userId: number;
  mentorId: number;
  durationMinutes?: number;
  sessionPrice?: number;
}

export interface SessionUpdateRequest {
  timeSlot?: string;
  meetingUrl?: string;
}

export interface SessionResponse {
  id: number;
  userId: number;
  mentorId: number;
  status: string;
  timeSlot?: string | null;
  meetingUrl?: string | null;
  durationMinutes?: number | null;
  sessionPrice?: number | null;
  userProfile?: User | null;
  mentorProfile?: User | null;
}

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly api = inject(ApiService);

  book(payload: SessionRequest) {
    return this.api.post<SessionResponse>(API_ENDPOINTS.sessions.base, payload);
  }

  updateStatus(id: number, status: string, payload?: SessionUpdateRequest) {
    return this.api.put<SessionResponse>(API_ENDPOINTS.sessions.updateStatus(id, status), payload);
  }

  complete(id: number) {
    return this.api.put<SessionResponse>(API_ENDPOINTS.sessions.complete(id));
  }

  delete(id: number) {
    return this.api.delete<void>(API_ENDPOINTS.sessions.delete(id));
  }

  getMentorSessions() {
    return this.api.get<SessionResponse[]>(API_ENDPOINTS.sessions.mentorSessions);
  }

  getUserSessions() {
    return this.api.get<SessionResponse[]>(API_ENDPOINTS.sessions.userSessions);
  }

  getAdminSessions() {
    return this.api.get<SessionResponse[]>(API_ENDPOINTS.sessions.adminSessions);
  }
}
