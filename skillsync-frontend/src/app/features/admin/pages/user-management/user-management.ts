import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminService, MentorApplication } from '../../services/admin';
import { MentorService, Skill } from '../../../mentors/services/mentor';
import { User } from '../../../../core/models/user.model';
import { RouterLink } from '@angular/router';
import { environment } from '../../../../../environments/environment';
import { API_ENDPOINTS } from '../../../../core/constants/api-endpoints';
import { SessionResponse, SessionService } from '../../../sessions/services/session';

@Component({
  selector: 'app-user-management',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './user-management.html',
  styleUrl: './user-management.scss',
})
export class UserManagement {
  private readonly formBuilder = inject(FormBuilder);
  private readonly adminService = inject(AdminService);
  private readonly mentorService = inject(MentorService);
  private readonly sessionService = inject(SessionService);
  private readonly baseUrl = environment.apiBaseUrl;

  protected readonly message = signal('');
  protected readonly errorMessage = signal('');
  protected readonly skills = signal<Skill[]>([]);
  protected readonly applications = signal<MentorApplication[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly userNames = signal<Record<number, string>>({});
  protected readonly userFilter = signal('ALL');
  protected readonly userSearch = signal('');
  protected readonly skillSearch = signal('');
  protected readonly pendingAction = signal<AdminAction | null>(null);
  protected readonly sessions = signal<SessionResponse[]>([]);
  protected readonly sessionSearch = signal('');
  protected readonly sessionPage = signal(1);
  protected readonly sessionPageSize = 6;

  protected readonly form = this.formBuilder.nonNullable.group({
    applicationId: [0, [Validators.required, Validators.min(1)]],
  });

  protected readonly skillForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required]],
    description: ['', [Validators.required]],
  });

  protected readonly deleteSkillForm = this.formBuilder.nonNullable.group({
    skillId: [0, [Validators.required, Validators.min(1)]],
  });

  protected readonly reasonForm = this.formBuilder.nonNullable.group({
    reason: ['', [Validators.required, Validators.minLength(6)]],
  });

  constructor() {
    this.loadApplications();
    this.loadSkills();
    this.loadUsers();
    this.loadSessions();
  }

  approve() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage.set('Enter a valid application ID.');
      return;
    }

    this.message.set('');
    this.errorMessage.set('');

    this.adminService.approveMentorApplication(this.form.controls.applicationId.value).subscribe({
      next: (message) => this.message.set(message),
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  addSkill() {
    if (this.skillForm.invalid) {
      this.skillForm.markAllAsTouched();
      this.errorMessage.set('Skill name and description are required.');
      return;
    }

    this.message.set('');
    this.errorMessage.set('');

    this.adminService.addSkill(this.skillForm.getRawValue()).subscribe({
      next: (skill) => {
        this.message.set(`Skill "${skill.name}" added successfully.`);
        this.skillForm.reset({ name: '', description: '' });
        this.loadSkills();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  deleteSkill() {
    if (this.deleteSkillForm.invalid) {
      this.deleteSkillForm.markAllAsTouched();
      this.errorMessage.set('Enter a valid skill ID to delete.');
      return;
    }

    this.message.set('');
    this.errorMessage.set('');

    this.adminService.deleteSkill(this.deleteSkillForm.controls.skillId.value).subscribe({
      next: (message) => {
        this.message.set(message);
        this.deleteSkillForm.reset({ skillId: 0 });
        this.loadSkills();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  deleteSkillDirect(skillId: number) {
    this.message.set('');
    this.errorMessage.set('');

    this.adminService.deleteSkill(skillId).subscribe({
      next: (message) => {
        this.message.set(message);
        this.loadSkills();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  loadSkills() {
    this.mentorService.getSkills().subscribe({
      next: (skills) => this.skills.set(skills),
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  loadApplications() {
    this.adminService.getMentorApplications().subscribe({
      next: (applications) => this.applications.set(applications),
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  loadUsers() {
    this.adminService.getUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.userNames.set(Object.fromEntries(users.map((user) => [user.id, user.name || user.email])));
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  loadSessions() {
    this.sessionService.getAdminSessions().subscribe({
      next: (sessions) => this.sessions.set(sessions),
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  approveFromList(applicationId: number) {
    this.message.set('');
    this.errorMessage.set('');

    this.adminService.approveMentorApplication(applicationId).subscribe({
      next: (message) => {
        this.message.set(message);
        this.loadApplications();
        this.loadUsers();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  rejectFromList(applicationId: number) {
    this.pendingAction.set({
      kind: 'REJECT_APPLICATION',
      id: applicationId,
      label: `Reject application ${this.displayId(applicationId)}`,
    });
    this.reasonForm.reset({ reason: '' });
  }

  protected confirmPendingAction() {
    const action = this.pendingAction();
    if (!action) {
      return;
    }

    if (this.reasonForm.invalid) {
      this.reasonForm.markAllAsTouched();
      return;
    }

    const reason = this.reasonForm.controls.reason.value;
    this.message.set('');
    this.errorMessage.set('');

    if (action.kind === 'REJECT_APPLICATION') {
      this.adminService.rejectMentorApplication(action.id, reason).subscribe({
        next: (message) => {
          this.message.set(message);
          this.pendingAction.set(null);
          this.loadApplications();
        },
        error: (error: Error) => this.errorMessage.set(error.message),
      });
      return;
    }

    this.adminService.deleteUser(action.id, reason).subscribe({
      next: (message) => {
        this.message.set(message);
        this.pendingAction.set(null);
        this.loadUsers();
        this.loadApplications();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  protected cancelPendingAction() {
    this.pendingAction.set(null);
    this.reasonForm.reset({ reason: '' });
  }

  protected displayId(id: number) {
    return String(((id * 7919) % 9000) + 1000);
  }

  protected skillName(skillId: number) {
    return this.skills().find((skill) => skill.id === skillId)?.name ?? `Skill ${this.displayId(skillId)}`;
  }

  protected userName(userId: number) {
    return this.userNames()[userId] ?? `User ${this.displayId(userId)}`;
  }

  protected biodataUrl(userId: number) {
    const user = this.users().find((item) => item.id === userId);
    return user?.hasBiodata ? `${this.baseUrl}${API_ENDPOINTS.auth.biodataByUser(userId)}` : null;
  }

  protected filteredUsers = computed(() => {
    const filter = this.userFilter();
    const search = this.userSearch().trim().toLowerCase();

    return this.users().filter((user) => {
      const roleMatch = filter === 'ALL' || user.role === filter;
      const haystack = [this.displayId(user.id), user.name, user.email, user.role]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return roleMatch && (!search || haystack.includes(search));
    });
  });

  protected filteredSkills = computed(() => {
    const search = this.skillSearch().trim().toLowerCase();
    if (!search) {
      return this.skills();
    }
    return this.skills().filter((skill) =>
      [this.displayId(skill.id), skill.name, skill.description]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(search),
    );
  });

  protected filteredSessions = computed(() => {
    const search = this.sessionSearch().trim().toLowerCase();
    if (!search) {
      return this.sessions();
    }

    return this.sessions().filter((session) => {
      const haystack = [
        this.displayId(session.id),
        session.id,
        session.userId,
        session.userProfile?.name,
        session.userProfile?.email,
        session.mentorId,
        session.mentorProfile?.name,
        session.mentorProfile?.email,
        session.timeSlot,
        session.status,
      ].filter(Boolean).join(' ').toLowerCase();

      return haystack.includes(search);
    });
  });

  protected sessionPageCount = computed(() =>
    Math.max(1, Math.ceil(this.filteredSessions().length / this.sessionPageSize)),
  );

  protected paginatedSessions = computed(() => {
    const start = (this.sessionPage() - 1) * this.sessionPageSize;
    return this.filteredSessions().slice(start, start + this.sessionPageSize);
  });

  protected updateUserFilter(value: string) {
    this.userFilter.set(value);
  }

  protected updateUserSearch(value: string) {
    this.userSearch.set(value);
  }

  protected updateSkillSearch(value: string) {
    this.skillSearch.set(value);
  }

  protected updateSessionSearch(value: string) {
    this.sessionSearch.set(value);
    this.sessionPage.set(1);
  }

  protected setSessionPage(page: number) {
    this.sessionPage.set(Math.min(Math.max(page, 1), this.sessionPageCount()));
  }

  protected deleteUser(user: User) {
    this.pendingAction.set({
      kind: 'DELETE_USER',
      id: user.id,
      label: `Delete ${user.name || user.email}`,
    });
    this.reasonForm.reset({ reason: '' });
  }

  protected makeUser(user: User) {
    this.adminService.updateUserRole(user.id, 'ROLE_USER').subscribe({
      next: (message) => {
        this.message.set(message);
        this.loadUsers();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }
}

interface AdminAction {
  kind: 'DELETE_USER' | 'REJECT_APPLICATION';
  id: number;
  label: string;
}
