import { Injectable, inject } from '@angular/core';
import { API_ENDPOINTS } from '../../../core/constants/api-endpoints';
import { ApiService } from '../../../core/services/api.service';
import { User } from '../../../core/models/user.model';

export interface Skill {
  id: number;
  name: string;
  description: string;
}

export interface MentorApplicationRequest {
  userId: number;
  skillIds: number[];
  experience: number;
}

export interface MentorApplication {
  id: number;
  userId: number;
  skillIds: number[];
  experience: number;
  status: string;
}

export interface Review {
  id?: number;
  userId: number;
  mentorId: number;
  rating: number;
  comment: string;
}

export interface MentorProfile extends User {}

@Injectable({ providedIn: 'root' })
export class MentorService {
  private readonly api = inject(ApiService);

  getSkills() {
    return this.api.get<Skill[]>(API_ENDPOINTS.skills.base);
  }

  getMentors() {
    return this.api.get<MentorProfile[]>(API_ENDPOINTS.auth.mentors);
  }

  applyForMentor(payload: MentorApplicationRequest) {
    return this.api.postText(API_ENDPOINTS.mentors.apply, payload);
  }

  getMyApplications() {
    return this.api.get<MentorApplication[]>(API_ENDPOINTS.mentors.myApplications);
  }

  revokeApplication(applicationId: number) {
    return this.api.deleteText(API_ENDPOINTS.mentors.revokeApplication(applicationId));
  }

  updateMentorSkills(skillIds: number[]) {
    return this.api.putText(API_ENDPOINTS.mentors.updateSkills, skillIds);
  }

  getReviews(mentorId: number) {
    return this.api.get<Review[]>(API_ENDPOINTS.reviews.byMentor(mentorId));
  }

  addReview(payload: Review) {
    return this.api.post<Review>(API_ENDPOINTS.reviews.base, payload);
  }
}
