import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { User } from '../../../../core/models/user.model';
import { environment } from '../../../../../environments/environment';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MentorService, Skill } from '../../../mentors/services/mentor';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile {
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly mentorService = inject(MentorService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly baseUrl = environment.apiBaseUrl;

  protected readonly profile = signal<User | null>(null);
  protected readonly loading = signal(true);
  protected readonly message = signal('');
  protected readonly errorMessage = signal('');
  protected readonly viewingOwnProfile = signal(true);
  protected readonly skills = signal<Skill[]>([]);
  protected readonly selectedSkillIds = signal<number[]>([]);

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required]],
    age: [18, [Validators.required, Validators.min(1)]],
    dob: [''],
    address: [''],
    phoneNumber: [''],
    about: [''],
    linkedinUrl: [''],
    xUrl: [''],
    instagramUrl: [''],
    quickSessionPrice: [15],
    focusedSessionPrice: [25],
    deepSessionPrice: [40],
  });

  constructor() {
    this.mentorService.getSkills().subscribe({
      next: (skills) => this.skills.set(skills),
      error: () => this.skills.set([]),
    });
    this.loadProfile();
  }

  loadProfile() {
    this.loading.set(true);
    const routeId = Number(this.route.snapshot.paramMap.get('id'));
    const currentUserId = this.authService.userId();
    const viewingOtherProfile = Number.isFinite(routeId) && routeId > 0 && routeId !== currentUserId;
    this.viewingOwnProfile.set(!viewingOtherProfile);

    const request = viewingOtherProfile
      ? this.authService.getUserById(routeId)
      : this.authService.getCurrentProfile();

    request.subscribe({
      next: (profile) => {
        const enhanced = this.decorate(profile);
        this.profile.set(enhanced);
        this.selectedSkillIds.set(enhanced.skillIds ?? []);
        this.form.reset({
          name: enhanced.name || '',
          age: enhanced.age || 18,
          dob: enhanced.dob || '',
          address: enhanced.address || '',
          phoneNumber: enhanced.phoneNumber || '',
          about: enhanced.about || '',
          linkedinUrl: enhanced.linkedinUrl || '',
          xUrl: enhanced.xUrl || '',
          instagramUrl: enhanced.instagramUrl || '',
          quickSessionPrice: enhanced.quickSessionPrice ?? 15,
          focusedSessionPrice: enhanced.focusedSessionPrice ?? 25,
          deepSessionPrice: enhanced.deepSessionPrice ?? 40,
        });
        this.loading.set(false);
      },
      error: (error: Error) => {
        this.errorMessage.set(error.message);
        this.loading.set(false);
      },
    });
  }

  saveProfile() {
    if (!this.viewingOwnProfile()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage.set('Please complete the required profile fields.');
      return;
    }

    this.message.set('');
    this.errorMessage.set('');

    if (this.isMentor() && !this.selectedSkillIds().length) {
      this.errorMessage.set('Select at least one mentor skill.');
      return;
    }

    this.authService.updateProfile(this.form.getRawValue()).subscribe({
      next: (profile) => {
        this.profile.set(this.decorate(profile));
        this.authService.refreshProfile();
        if (this.isMentor()) {
          this.saveMentorSkills();
        }
        this.message.set('Profile updated successfully.');
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  uploadProfilePicture(event: Event) {
    const file = this.readFile(event);
    if (!file) {
      return;
    }

    this.authService.uploadProfilePicture(file).subscribe({
      next: (profile) => {
        this.profile.set(this.decorate(profile));
        this.authService.refreshProfile();
        this.message.set('Profile picture uploaded.');
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  uploadBiodata(event: Event) {
    const file = this.readFile(event);
    if (!file) {
      return;
    }

    this.authService.uploadBiodata(file).subscribe({
      next: (profile) => {
        this.profile.set(this.decorate(profile));
        this.authService.refreshProfile();
        this.message.set('Biodata uploaded.');
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  protected isMentor() {
    return this.profile()?.role === 'ROLE_MENTOR';
  }

  protected isPublicMentorProfile() {
    return this.isMentor() && !this.viewingOwnProfile();
  }

  protected skillNames(profile: User) {
    return (profile.skillIds ?? []).map((id) =>
      this.skills().find((skill) => skill.id === id)?.name ?? `Skill ${this.displayId(id)}`,
    );
  }

  protected sessionPlans(profile: User) {
    return [
      { label: 'Quick Chat', duration: 15, price: profile.quickSessionPrice ?? 15 },
      { label: 'Focused', duration: 30, price: profile.focusedSessionPrice ?? 25 },
      { label: 'Deep Dive', duration: 60, price: profile.deepSessionPrice ?? 40 },
    ];
  }

  protected toggleMentorSkill(skillId: number, checked: boolean) {
    this.selectedSkillIds.update((ids) =>
      checked ? [...ids, skillId] : ids.filter((id) => id !== skillId),
    );
  }

  protected displayId(id: number) {
    return String(((id * 7919) % 9000) + 1000);
  }

  switchToUser() {
    this.authService.updateOwnRole('ROLE_USER').subscribe({
      next: () => {
        this.message.set('Role changed to user. Please login again.');
        this.authService.logout();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  private readFile(event: Event) {
    const input = event.target as HTMLInputElement;
    return input.files?.[0] ?? null;
  }

  private decorate(profile: User): User {
    return {
      ...profile,
      profilePictureUrl: profile.hasProfilePicture && profile.id
        ? `${this.baseUrl}/authservice/auth/profile-picture/${profile.id}`
        : null,
      biodataUrl: profile.hasBiodata && profile.id
        ? `${this.baseUrl}/authservice/auth/biodata/${profile.id}`
        : null,
    };
  }

  private saveMentorSkills() {
    this.mentorService.updateMentorSkills(this.selectedSkillIds()).subscribe({
      next: () => this.authService.refreshProfile(),
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }
}
