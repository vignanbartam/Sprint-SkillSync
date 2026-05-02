import { Injectable, inject } from '@angular/core';
import { API_ENDPOINTS } from '../../../core/constants/api-endpoints';
import { ApiService } from '../../../core/services/api.service';
import { User } from '../../../core/models/user.model';

export interface SkillRequest {
  name: string;
  description: string;
}

export interface SkillResponse {
  id: number;
  name: string;
  description: string;
}

export interface MentorApplication {
  id: number;
  userId: number;
  skillIds: number[];
  experience: number;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly api = inject(ApiService);

  getMentorApplications() {
    return this.api.get<MentorApplication[]>(API_ENDPOINTS.admin.listMentorApplications);
  }

  getUsers() {
    return this.api.get<User[]>(API_ENDPOINTS.admin.users);
  }

  approveMentorApplication(applicationId: number) {
    return this.api.putText(API_ENDPOINTS.admin.approveMentor(applicationId));
  }

  rejectMentorApplication(applicationId: number, reason = '') {
    return this.api.putText(API_ENDPOINTS.admin.rejectMentor(applicationId), { reason });
  }

  deleteUser(userId: number, reason: string) {
    return this.api.deleteTextWithBody(API_ENDPOINTS.admin.deleteUser(userId), { reason });
  }

  updateUserRole(userId: number, role: 'ROLE_USER' | 'ROLE_MENTOR') {
    return this.api.putText(API_ENDPOINTS.admin.updateUserRole(userId, role));
  }

  addSkill(payload: SkillRequest) {
    return this.api.post<SkillResponse>(API_ENDPOINTS.admin.addSkill, payload);
  }

  deleteSkill(skillId: number) {
    return this.api.deleteText(API_ENDPOINTS.admin.deleteSkill(skillId));
  }
}
