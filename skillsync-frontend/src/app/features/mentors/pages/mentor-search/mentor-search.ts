import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { API_ENDPOINTS } from '../../../../core/constants/api-endpoints';
import { AuthService } from '../../../../core/services/auth.service';
import { RatingStars } from '../../../../shared/components/rating-stars/rating-stars';
import { environment } from '../../../../../environments/environment';
import { SessionService } from '../../../sessions/services/session';
import { MentorProfile, MentorService, Review, Skill } from '../../services/mentor';

@Component({
  selector: 'app-mentor-search',
  imports: [CommonModule, RouterLink, RatingStars],
  templateUrl: './mentor-search.html',
  styleUrl: './mentor-search.scss',
})
export class MentorSearch {
  private readonly authService = inject(AuthService);
  private readonly mentorService = inject(MentorService);
  private readonly sessionService = inject(SessionService);
  private readonly baseUrl = environment.apiBaseUrl;

  protected readonly mentors = signal<MentorWithReview[]>([]);
  protected readonly skills = signal<Skill[]>([]);
  protected readonly search = signal('');
  protected readonly selectedSkillIds = signal<number[]>([]);
  protected readonly selectedPlans = signal<Record<number, SessionPlan>>({});
  protected readonly page = signal(1);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly pageSize = 4;

  protected readonly filteredMentors = computed(() => {
    const search = this.search().trim().toLowerCase();
    const selectedSkills = this.selectedSkillIds();

    return this.mentors().filter((mentor) => {
      const skillNames = this.skillNames(mentor.skillIds ?? []);
      const haystack = [
        mentor.name,
        mentor.email,
        mentor.about,
        mentor.age,
        mentor.mentorExperience,
        this.displayId(mentor.id),
        ...skillNames,
      ].filter(Boolean).join(' ').toLowerCase();

      return (!search || haystack.includes(search))
        && (!selectedSkills.length || selectedSkills.every((skillId) => mentor.skillIds?.includes(skillId)));
    });
  });

  protected readonly pageCount = computed(() =>
    Math.max(1, Math.ceil(this.filteredMentors().length / this.pageSize)),
  );

  protected readonly paginatedMentors = computed(() => {
    const start = (this.page() - 1) * this.pageSize;
    return this.filteredMentors().slice(start, start + this.pageSize);
  });

  constructor() {
    forkJoin({
      skills: this.mentorService.getSkills().pipe(catchError(() => of([] as Skill[]))),
      mentors: this.mentorService.getMentors().pipe(catchError((error: Error) => {
        this.errorMessage.set(error.message);
        return of([] as MentorProfile[]);
      })),
    }).subscribe(({ skills, mentors }) => {
      this.skills.set(skills);
      this.loadReviews(mentors);
    });
  }

  protected updateSearch(value: string) {
    this.search.set(value);
    this.page.set(1);
  }

  protected toggleSkill(skillId: number, checked: boolean) {
    this.selectedSkillIds.update((ids) =>
      checked ? [...ids, skillId] : ids.filter((id) => id !== skillId),
    );
    this.page.set(1);
  }

  protected clearFilters() {
    this.search.set('');
    this.selectedSkillIds.set([]);
    this.page.set(1);
  }

  protected setPage(page: number) {
    this.page.set(Math.min(Math.max(page, 1), this.pageCount()));
  }

  protected selectPlan(mentorId: number, plan: SessionPlan) {
    this.selectedPlans.update((plans) => ({
      ...plans,
      [mentorId]: plan,
    }));
  }

  protected book(mentor: MentorWithReview) {
    const userId = this.authService.userId();
    const plan = this.selectedPlan(mentor);
    if (!userId) {
      this.errorMessage.set('Login is required to book a session.');
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');
    this.sessionService.book({
      userId,
      mentorId: mentor.id,
      durationMinutes: plan.durationMinutes,
      sessionPrice: plan.price,
    }).subscribe({
      next: () => this.successMessage.set('Session booked. Check Sessions for booking status.'),
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  protected plansForMentor(mentor: MentorWithReview): SessionPlan[] {
    return [
      { key: 'quick', label: 'Quick Chat', durationMinutes: 15, price: Number(mentor.quickSessionPrice ?? 15) },
      { key: 'focused', label: 'Focused', durationMinutes: 30, price: Number(mentor.focusedSessionPrice ?? 25) },
      { key: 'deep', label: 'Deep Dive', durationMinutes: 60, price: Number(mentor.deepSessionPrice ?? 40) },
    ];
  }

  protected selectedPlan(mentor: MentorWithReview) {
    return this.selectedPlans()[mentor.id] ?? this.plansForMentor(mentor)[1];
  }

  protected reviewSummary(mentor: MentorWithReview) {
    const count = mentor.reviews.length;
    return `${count} ${count === 1 ? 'review' : 'reviews'}`;
  }

  protected skillNames(skillIds: number[]) {
    return skillIds.map((id) => this.skills().find((skill) => skill.id === id)?.name ?? `Skill ${this.displayId(id)}`);
  }

  protected displayId(id: number) {
    return String(((id * 7919) % 9000) + 1000);
  }

  private loadReviews(mentors: MentorProfile[]) {
    if (!mentors.length) {
      this.mentors.set([]);
      this.loading.set(false);
      return;
    }

    forkJoin(
      mentors.map((mentor) =>
        this.mentorService.getReviews(mentor.id).pipe(catchError(() => of([] as Review[]))),
      ),
    ).subscribe((reviewLists) => {
      this.mentors.set(mentors.map((mentor, index) => this.decorateMentor(mentor, reviewLists[index])));
      this.loading.set(false);
    });
  }

  private decorateMentor(mentor: MentorProfile, reviews: Review[]): MentorWithReview {
    return {
      ...mentor,
      reviews,
      averageRating: reviews.length
        ? reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length
        : 0,
      profilePictureUrl: mentor.hasProfilePicture ? `${this.baseUrl}${API_ENDPOINTS.auth.profilePictureByUser(mentor.id)}` : null,
      biodataUrl: mentor.hasBiodata ? `${this.baseUrl}${API_ENDPOINTS.auth.biodataByUser(mentor.id)}` : null,
    };
  }
}

interface MentorWithReview extends MentorProfile {
  reviews: Review[];
  averageRating: number;
}

interface SessionPlan {
  key: string;
  label: string;
  durationMinutes: number;
  price: number;
}
