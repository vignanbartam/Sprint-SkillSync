import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { SessionResponse, SessionService } from '../../services/session';
import { MentorProfile, MentorService, Review } from '../../../mentors/services/mentor';
import { API_ENDPOINTS } from '../../../../core/constants/api-endpoints';
import { environment } from '../../../../../environments/environment';
import { User } from '../../../../core/models/user.model';

@Component({
  selector: 'app-book-session',
  imports: [CommonModule, RouterLink],
  templateUrl: './book-session.html',
  styleUrl: './book-session.scss',
})
export class BookSession {
  private readonly authService = inject(AuthService);
  private readonly sessionService = inject(SessionService);
  private readonly mentorService = inject(MentorService);
  private readonly baseUrl = environment.apiBaseUrl;

  protected readonly bookingResult = signal<SessionResponse | null>(null);
  protected readonly updateResult = signal<SessionResponse | null>(null);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly role = this.authService.role;
  protected readonly mentors = signal<MentorWithReview[]>([]);
  protected readonly mentorSessions = signal<SessionResponse[]>([]);
  protected readonly userSessions = signal<SessionResponse[]>([]);
  protected readonly acceptDrafts = signal<Record<number, AcceptDraft>>({});
  protected readonly loading = signal(true);
  protected readonly selectedPlans = signal<Record<number, SessionPlan>>({});
  protected readonly expandedSessions = signal<Record<number, boolean>>({});
  protected readonly sessionSearch = signal('');
  protected readonly sessionStatusFilter = signal('ALL');
  protected readonly sessionPage = signal(1);
  protected readonly reviewDrafts = signal<Record<number, ReviewDraft>>({});
  protected readonly reviewedSessionIds = signal<number[]>([]);
  protected readonly pageSize = 5;

  protected readonly pendingMentorSessions = computed(() =>
    this.mentorSessions().filter((session) => session.status === 'PENDING'),
  );

  protected readonly filteredMentorSessions = computed(() => {
    const search = this.sessionSearch().trim().toLowerCase();
    const status = this.sessionStatusFilter();

    return this.mentorSessions().filter((session) => {
      const profile = session.userProfile;
      const haystack = [
        this.displayId(session.id),
        this.displayId(session.userId),
        profile?.name,
        profile?.email,
        session.status,
      ].filter(Boolean).join(' ').toLowerCase();

      return (!search || haystack.includes(search))
        && (status === 'ALL' || session.status === status);
    });
  });

  protected readonly paginatedMentorSessions = computed(() => {
    const start = (this.sessionPage() - 1) * this.pageSize;
    return this.filteredMentorSessions().slice(start, start + this.pageSize);
  });

  protected readonly mentorSessionPageCount = computed(() =>
    Math.max(1, Math.ceil(this.filteredMentorSessions().length / this.pageSize)),
  );

  constructor() {
    this.loadView();
  }

  book(mentor: MentorWithReview) {
    const userId = this.authService.userId();
    const plan = this.selectedPlan(mentor);
    if (!userId || !mentor.id) {
      this.errorMessage.set('Valid user session and mentor ID are required.');
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');

    this.sessionService
      .book({
        userId,
        mentorId: mentor.id,
        durationMinutes: plan.durationMinutes,
        sessionPrice: plan.price,
      })
      .subscribe({
        next: (session) => {
          this.bookingResult.set(session);
          this.successMessage.set('Session booked successfully.');
          this.loadUserSessions();
        },
        error: (error: Error) => this.errorMessage.set(error.message),
      });
  }

  deleteSession(sessionId: number) {
    this.errorMessage.set('');
    this.successMessage.set('');

    this.sessionService.delete(sessionId).subscribe({
      next: () => {
        this.successMessage.set('Session deleted.');
        if (this.canManage()) {
          this.loadMentorSessions();
        }
        if (this.canBook()) {
          this.loadUserSessions();
        }
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  updateStatus(sessionId: number, status: 'ACCEPTED' | 'REJECTED') {
    this.errorMessage.set('');
    this.successMessage.set('');

    this.sessionService.updateStatus(sessionId, status).subscribe({
        next: (session) => {
          this.updateResult.set(session);
          this.successMessage.set('Session status updated.');
          this.loadMentorSessions();
        },
        error: (error: Error) => this.errorMessage.set(error.message),
      });
  }

  accept(sessionId: number) {
    const draft = this.acceptDrafts()[sessionId] ?? { timeSlot: '', meetingUrl: '' };
    if (!draft.timeSlot.trim() || !draft.meetingUrl.trim()) {
      this.errorMessage.set('Enter a time slot and meeting URL before accepting.');
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');

    this.sessionService.updateStatus(sessionId, 'ACCEPTED', {
      timeSlot: draft.timeSlot.trim(),
      meetingUrl: draft.meetingUrl.trim(),
    }).subscribe({
      next: (session) => {
        this.updateResult.set(session);
        this.successMessage.set('Session accepted and meeting details sent.');
        this.loadMentorSessions();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  updateAcceptDraft(sessionId: number, field: keyof AcceptDraft, value: string) {
    this.acceptDrafts.update((drafts) => ({
      ...drafts,
      [sessionId]: {
        ...(drafts[sessionId] ?? { timeSlot: '', meetingUrl: '' }),
        [field]: value,
      },
    }));
  }

  updateReviewDraft(sessionId: number, field: keyof ReviewDraft, value: string | number) {
    this.reviewDrafts.update((drafts) => ({
      ...drafts,
      [sessionId]: {
        ...(drafts[sessionId] ?? { rating: 5, comment: '' }),
        [field]: value,
      },
    }));
  }

  submitReview(session: SessionResponse) {
    const userId = this.authService.userId();
    const draft = this.reviewDrafts()[session.id] ?? { rating: 5, comment: '' };
    if (!userId || !draft.comment.trim()) {
      this.errorMessage.set('Rating and comment are required.');
      return;
    }

    this.mentorService.addReview({
      userId,
      mentorId: session.mentorId,
      rating: Number(draft.rating),
      comment: draft.comment.trim(),
    }).subscribe({
      next: () => {
        this.successMessage.set('Review submitted.');
        this.reviewedSessionIds.update((ids) => [...new Set([...ids, session.id])]);
        this.reviewDrafts.update((drafts) => {
          const next = { ...drafts };
          delete next[session.id];
          return next;
        });
        this.loadMentors();
      },
      error: (error: Error) => {
        this.errorMessage.set(error.message);
        if (error.message.toLowerCase().includes('already reviewed')) {
          this.reviewedSessionIds.update((ids) => [...new Set([...ids, session.id])]);
        }
      },
    });
  }

  selectPlan(mentorId: number, plan: SessionPlan) {
    this.selectedPlans.update((plans) => ({
      ...plans,
      [mentorId]: plan,
    }));
  }

  updateSessionSearch(value: string) {
    this.sessionSearch.set(value);
    this.sessionPage.set(1);
  }

  updateSessionStatusFilter(value: string) {
    this.sessionStatusFilter.set(value);
    this.sessionPage.set(1);
  }

  setSessionPage(page: number) {
    const next = Math.min(Math.max(page, 1), this.mentorSessionPageCount());
    this.sessionPage.set(next);
  }

  toggleSessionDetails(sessionId: number) {
    this.expandedSessions.update((sessions) => ({
      ...sessions,
      [sessionId]: !sessions[sessionId],
    }));
  }

  complete(sessionId: number) {
    this.errorMessage.set('');
    this.successMessage.set('');

    this.sessionService.complete(sessionId).subscribe({
      next: (session) => {
        this.updateResult.set(session);
        this.successMessage.set('Session marked as completed.');
        this.loadMentorSessions();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  protected readonly canBook = () => this.role() === 'ROLE_USER';
  protected readonly canManage = () => this.role() === 'ROLE_MENTOR';

  private loadView() {
    if (this.canBook()) {
      this.loadUserSessions();
      return;
    }

    if (this.canManage()) {
      this.loadMentorSessions();
      return;
    }

    this.loading.set(false);
  }

  private loadMentors() {
    this.loading.set(true);
    this.mentorService.getMentors().subscribe({
      next: (mentors) => {
        if (!mentors.length) {
          this.mentors.set([]);
          this.loading.set(false);
          return;
        }

        forkJoin(
          mentors.map((mentor) =>
            this.mentorService.getReviews(mentor.id).pipe(
              catchError(() => of([] as Review[])),
            ),
          ),
        ).subscribe({
          next: (reviewLists) => {
            this.mentors.set(
              mentors.map((mentor, index) => this.decorateMentor(mentor, reviewLists[index])),
            );
            this.syncReviewedSessions();
            this.loading.set(false);
          },
          error: (error: Error) => {
            this.errorMessage.set(error.message);
            this.loading.set(false);
          },
        });
      },
      error: (error: Error) => {
        this.errorMessage.set(error.message);
        this.loading.set(false);
      },
    });
  }

  private loadMentorSessions() {
    this.loading.set(true);
    this.sessionService.getMentorSessions().subscribe({
      next: (sessions) => {
        this.mentorSessions.set(sessions.map((session) => ({
          ...session,
          userProfile: session.userProfile ? this.decorateUser(session.userProfile) : null,
        })));
        this.loading.set(false);
      },
      error: (error: Error) => {
        this.errorMessage.set(error.message);
        this.loading.set(false);
      },
    });
  }

  private loadUserSessions() {
    this.sessionService.getUserSessions().subscribe({
      next: (sessions) => {
        const decoratedSessions = sessions.map((session) => ({
          ...session,
          mentorProfile: session.mentorProfile ? this.decorateUser(session.mentorProfile) : null,
        }));
        this.userSessions.set(decoratedSessions);
        this.loadReviewedSessions(decoratedSessions);
        this.loading.set(false);
      },
      error: (error: Error) => {
        this.errorMessage.set(error.message);
        this.loading.set(false);
      },
    });
  }

  protected averageRating(reviews: Review[]) {
    if (!reviews.length) {
      return 0;
    }
    return reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length;
  }

  protected reviewSummary(mentor: MentorWithReview) {
    const count = mentor.reviews.length;
    if (!count) {
      return 'No reviews yet';
    }

    return `${count} ${count === 1 ? 'review' : 'reviews'}`;
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

  protected displayId(id: number) {
    return String(((id * 7919) % 9000) + 1000);
  }

  protected canDelete(session: SessionResponse) {
    return this.canBook() || session.status === 'COMPLETED' || session.status === 'REJECTED';
  }

  protected mentorForSession(session: SessionResponse) {
    return this.mentors().find((mentor) => mentor.id === session.mentorId)
      ?? session.mentorProfile
      ?? null;
  }

  protected mentorNameForSession(session: SessionResponse) {
    const mentor = this.mentorForSession(session);
    return mentor?.name || mentor?.email || 'Mentor';
  }

  protected sessionTime(session: SessionResponse) {
    return session.timeSlot || 'Time not scheduled yet';
  }

  protected canReview(session: SessionResponse) {
    return session.status === 'COMPLETED' && !this.reviewedSessionIds().includes(session.id);
  }

  private decorateMentor(mentor: MentorProfile, reviews: Review[]): MentorWithReview {
    return {
      ...mentor,
      reviews,
      averageRating: this.averageRating(reviews),
      profilePictureUrl: mentor.hasProfilePicture ? `${this.baseUrl}${API_ENDPOINTS.auth.profilePictureByUser(mentor.id)}` : null,
      biodataUrl: mentor.hasBiodata ? `${this.baseUrl}${API_ENDPOINTS.auth.biodataByUser(mentor.id)}` : null,
    };
  }

  private decorateUser(user: User): User {
    return {
      ...user,
      profilePictureUrl: user.hasProfilePicture ? `${this.baseUrl}${API_ENDPOINTS.auth.profilePictureByUser(user.id)}` : null,
      biodataUrl: user.hasBiodata ? `${this.baseUrl}${API_ENDPOINTS.auth.biodataByUser(user.id)}` : null,
    };
  }

  private syncReviewedSessions() {
    const userId = this.authService.userId();
    if (!userId) {
      return;
    }

    const reviewedMentorIds = new Set(
      this.mentors()
        .filter((mentor) => mentor.reviews.some((review) => review.userId === userId))
        .map((mentor) => mentor.id),
    );

    const reviewedSessionIds = this.userSessions()
      .filter((session) => reviewedMentorIds.has(session.mentorId))
      .map((session) => session.id);

    this.reviewedSessionIds.set([...new Set([...this.reviewedSessionIds(), ...reviewedSessionIds])]);
  }

  private loadReviewedSessions(sessions: SessionResponse[]) {
    const userId = this.authService.userId();
    const completedSessions = sessions.filter((session) => session.status === 'COMPLETED');
    if (!userId || !completedSessions.length) {
      return;
    }

    forkJoin(
      completedSessions.map((session) =>
        this.mentorService.getReviews(session.mentorId).pipe(catchError(() => of([] as Review[]))),
      ),
    ).subscribe((reviewLists) => {
      const reviewedSessionIds = completedSessions
        .filter((session, index) => reviewLists[index].some((review) => review.userId === userId))
        .map((session) => session.id);
      this.reviewedSessionIds.update((ids) => [...new Set([...ids, ...reviewedSessionIds])]);
    });
  }
}

interface MentorWithReview extends MentorProfile {
  reviews: Review[];
  averageRating: number;
}

interface AcceptDraft {
  timeSlot: string;
  meetingUrl: string;
}

interface ReviewDraft {
  rating: number;
  comment: string;
}

interface SessionPlan {
  key: string;
  label: string;
  durationMinutes: number;
  price: number;
}
