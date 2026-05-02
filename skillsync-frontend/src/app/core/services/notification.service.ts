import { Injectable, inject } from '@angular/core';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { ApiService } from './api.service';

export interface NotificationItem {
  id: number;
  userId: number;
  email: string;
  message: string;
  type: string;
  status: string;
  createdAt?: string | null;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly api = inject(ApiService);

  getByUser(userId: number) {
    return this.api.get<NotificationItem[]>(API_ENDPOINTS.notifications.byUser(userId));
  }

  delete(id: number) {
    return this.api.delete<void>(API_ENDPOINTS.notifications.delete(id));
  }
}
