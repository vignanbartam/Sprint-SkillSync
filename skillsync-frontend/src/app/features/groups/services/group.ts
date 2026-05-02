import { Injectable, inject } from '@angular/core';
import { API_ENDPOINTS } from '../../../core/constants/api-endpoints';
import { ApiService } from '../../../core/services/api.service';

export interface Group {
  id: number;
  name: string;
  description: string;
  createdBy: number;
}

export interface GroupMember {
  id: number;
  groupId: number;
  userId: number;
  displayName?: string | null;
  status: string;
  joinedAt: string;
}

export interface GroupMessage {
  id: number;
  groupId: number;
  senderId: number;
  senderName?: string | null;
  content: string;
  timestamp: string;
}

export interface GroupRequest {
  name: string;
  description: string;
}

@Injectable({ providedIn: 'root' })
export class GroupService {
  private readonly api = inject(ApiService);

  getAll() {
    return this.api.get<Group[]>(API_ENDPOINTS.groups.base);
  }

  getMine() {
    return this.api.get<Group[]>(API_ENDPOINTS.groups.mine);
  }

  getMessages(groupId: number) {
    return this.api.get<GroupMessage[]>(API_ENDPOINTS.groups.messages(groupId));
  }

  getMembers(groupId: number) {
    return this.api.get<GroupMember[]>(API_ENDPOINTS.groups.members(groupId));
  }

  create(payload: GroupRequest) {
    return this.api.post<Group>(API_ENDPOINTS.groups.base, payload);
  }

  join(groupId: number) {
    return this.api.postText(API_ENDPOINTS.groups.join(groupId), {});
  }

  remove(groupId: number, userId: number) {
    return this.api.deleteText(API_ENDPOINTS.groups.remove(groupId, userId));
  }

  deleteGroup(groupId: number) {
    return this.api.deleteText(API_ENDPOINTS.groups.delete(groupId));
  }
}
