import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import {
  MentorService,
  MentorProfile,
  Review,
  Skill,
  MentorApplication,
} from '../../services/mentor';
import { Loader } from '../../../../shared/components/loader/loader';
import { RatingStars } from '../../../../shared/components/rating-stars/rating-stars';
import { catchError, forkJoin, of } from 'rxjs';

@Component({
  selector: 'app-mentor-list',
  imports: [CommonModule, ReactiveFormsModule, Loader, RatingStars],
  templateUrl: './mentor-list.html',
  styleUrl: './mentor-list.scss',
})
export class MentorList {
  private readonly formBuilder = inject(FormBuilder);
  private readonly mentorService = inject(MentorService);
  private readonly authService = inject(AuthService);

  protected readonly skills = signal<Skill[]>([]);
  protected readonly reviews = signal<Review[]>([]);
  protected readonly loadingSkills = signal(true);
  protected readonly loadingReviews = signal(false);
  protected readonly loadingMentors = signal(true);
  protected readonly mentors = signal<MentorWithReview[]>([]);
  protected readonly mentorSearch = signal('');
  protected readonly skillSearch = signal('');
  protected readonly myApplications = signal<MentorApplication[]>([]);
  protected readonly applyMessage = signal('');
  protected readonly reviewMessage = signal('');
  protected readonly errorMessage = signal('');
  protected readonly selectedLookupMentor = signal<MentorWithReview | null>(null);

  protected readonly currentProfile = this.authService.profile;

  protected readonly activeApplication = computed(() =>
    this.myApplications().find((application) => ['PENDING', 'APPROVED'].includes(application.status)) ?? null,
  );

  protected readonly pastApplications = computed(() => {
    const activeId = this.activeApplication()?.id;
    return this.myApplications().filter((application) => application.id !== activeId);
  });

  protected readonly canApply = computed(() => !this.activeApplication());

  protected readonly averageRating = computed(() => {
    const reviewList = this.reviews();
    if (!reviewList.length) {
      return 0;
    }

    return reviewList.reduce((sum, review) => sum + review.rating, 0) / reviewList.length;
  });

  protected readonly topSkillNames = computed(() =>
    this.skills().slice(0, 3).map((skill) => skill.name),
  );

  protected readonly filteredSkills = computed(() => {
    const search = this.skillSearch().trim().toLowerCase();
    const skills = search
      ? this.skills().filter((skill) => [skill.name, skill.description].join(' ').toLowerCase().includes(search))
      : this.skills();
    return skills;
  });

  protected readonly filteredMentors = computed(() => {
    const search = this.mentorSearch().trim().toLowerCase();
    if (!search) {
      return this.mentors();
    }

    return this.mentors().filter((mentor) =>
      [mentor.name, mentor.email, mentor.about, this.displayId(mentor.id)]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(search),
    );
  });

  protected readonly mentorApplicationForm = this.formBuilder.nonNullable.group({
    skillIds: [<number[]>[], [Validators.required]],
    experience: [1, [Validators.required, Validators.min(1)]],
  });

  protected readonly reviewLookupForm = this.formBuilder.nonNullable.group({
    mentorName: ['', [Validators.required]],
  });

  protected readonly reviewForm = this.formBuilder.nonNullable.group({
    mentorId: [0, [Validators.required, Validators.min(1)]],
    rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['', [Validators.required]],
  });

  constructor() {
    this.mentorService.getSkills().subscribe({
      next: (skills) => {
        this.skills.set(skills);
        this.loadingSkills.set(false);
      },
      error: (error: Error) => {
        this.errorMessage.set(error.message);
        this.loadingSkills.set(false);
      },
    });
    this.loadMentors();
    this.loadMyApplications();
  }

  loadMentors() {
    this.loadingMentors.set(true);
    this.mentorService.getMentors().subscribe({
      next: (mentors) => {
        if (!mentors.length) {
          this.mentors.set([]);
          this.loadingMentors.set(false);
          return;
        }

        forkJoin(
          mentors.map((mentor) =>
            this.mentorService.getReviews(mentor.id).pipe(catchError(() => of([] as Review[]))),
          ),
        ).subscribe((reviewLists) => {
          this.mentors.set(mentors.map((mentor, index) => ({
            ...mentor,
            reviews: reviewLists[index],
            averageRating: this.averageRatingFor(reviewLists[index]),
          })));
          this.loadingMentors.set(false);
        });
      },
      error: (error: Error) => {
        this.errorMessage.set(error.message);
        this.loadingMentors.set(false);
      },
    });
  }

  updateMentorSearch(value: string) {
    this.mentorSearch.set(value);
  }

  updateSkillSearch(value: string) {
    this.skillSearch.set(value);
  }

  loadMyApplications() {
    if (this.authService.role() !== 'ROLE_USER') {
      return;
    }

    this.mentorService.getMyApplications().subscribe({
      next: (applications) => this.myApplications.set(applications),
      error: () => this.myApplications.set([]),
    });
  }

  toggleSkill(skillId: number, checked: boolean) {
    const current = this.mentorApplicationForm.controls.skillIds.value;
    const next = checked ? [...current, skillId] : current.filter((id) => id !== skillId);
    this.mentorApplicationForm.controls.skillIds.setValue(next);
  }

  submitApplication() {
    if (this.mentorApplicationForm.invalid) {
      this.mentorApplicationForm.markAllAsTouched();
      return;
    }

    if (!this.mentorApplicationForm.controls.skillIds.value.length) {
      this.errorMessage.set('Select at least one skill before applying.');
      return;
    }

    if (!this.currentProfile()?.hasBiodata) {
      this.errorMessage.set('Upload biodata before applying as a mentor.');
      return;
    }

    const userId = this.authService.userId();
    if (!userId) {
      this.errorMessage.set('Login is required.');
      return;
    }

    this.applyMessage.set('');
    this.errorMessage.set('');

    this.mentorService
      .applyForMentor({
        userId,
        ...this.mentorApplicationForm.getRawValue(),
      })
      .subscribe({
        next: (message) => {
          this.applyMessage.set(message);
          this.loadMyApplications();
        },
        error: (error: Error) => {
          this.errorMessage.set(error.message);
        },
      });
  }

  uploadBiodata(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.messageReset();
    this.authService.uploadBiodata(file).subscribe({
      next: () => {
        this.applyMessage.set('Biodata uploaded.');
        this.authService.refreshProfile();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  loadReviews() {
    if (this.reviewLookupForm.invalid) {
      this.reviewLookupForm.markAllAsTouched();
      return;
    }

    const search = this.reviewLookupForm.controls.mentorName.value.trim().toLowerCase();
    const mentor = this.mentors().find((item) =>
      (item.name || '').toLowerCase() === search,
    ) ?? this.mentors().find((item) =>
      (item.name || '').toLowerCase().includes(search),
    );

    if (!mentor) {
      this.reviews.set([]);
      this.selectedLookupMentor.set(null);
      this.errorMessage.set('No approved mentor found with that name.');
      return;
    }

    this.selectedLookupMentor.set(mentor);
    this.loadingReviews.set(true);
    this.reviewMessage.set('');
    this.errorMessage.set('');

    this.mentorService.getReviews(mentor.id).subscribe({
      next: (reviews) => {
        this.reviews.set(reviews);
        this.loadingReviews.set(false);
      },
      error: (error: Error) => {
        this.reviews.set([]);
        this.errorMessage.set(error.message);
        this.loadingReviews.set(false);
      },
    });
  }

  lookupMentor(mentor: MentorWithReview) {
    this.reviewLookupForm.controls.mentorName.setValue(mentor.name || mentor.email);
    this.loadReviews();
  }

  protected mentorName(mentor: MentorWithReview | null) {
    return mentor?.name || mentor?.email || 'Mentor';
  }

  revokeApplication(applicationId: number) {
    this.mentorService.revokeApplication(applicationId).subscribe({
      next: (message) => {
        this.applyMessage.set(message);
        this.loadMyApplications();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  protected displayId(id: number) {
    return String(((id * 7919) % 9000) + 1000);
  }

  protected skillName(skillId: number) {
    return this.skills().find((skill) => skill.id === skillId)?.name ?? `Skill ${this.displayId(skillId)}`;
  }

  protected averageRatingFor(reviews: Review[]) {
    if (!reviews.length) {
      return 0;
    }
    return reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length;
  }

  protected priceLine(mentor: MentorWithReview) {
    const price = mentor.focusedSessionPrice ?? mentor.quickSessionPrice ?? mentor.deepSessionPrice ?? 25;
    return `$${Number(price).toFixed(0)} / 30 min`;
  }

  private messageReset() {
    this.applyMessage.set('');
    this.reviewMessage.set('');
    this.errorMessage.set('');
  }
}

interface MentorWithReview extends MentorProfile {
  reviews: Review[];
  averageRating: number;
}
