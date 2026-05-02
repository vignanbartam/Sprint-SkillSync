import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { catchError, forkJoin, of } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import {
  NotificationItem,
  NotificationService,
} from '../../../../core/services/notification.service';
import { Group, GroupService } from '../../../groups/services/group';
import { MentorService, Skill } from '../../../mentors/services/mentor';
import { Loader } from '../../../../shared/components/loader/loader';

@Component({
  selector: 'app-learner-dashboard',
  imports: [CommonModule, Loader],
  templateUrl: './learner-dashboard.html',
  styleUrl: './learner-dashboard.scss',
})
export class LearnerDashboard {
  private readonly authService = inject(AuthService);
  private readonly mentorService = inject(MentorService);
  private readonly groupService = inject(GroupService);
  private readonly notificationService = inject(NotificationService);

  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly skills = signal<Skill[]>([]);
  protected readonly groups = signal<Group[]>([]);
  protected readonly notifications = signal<NotificationItem[]>([]);

  protected readonly user = this.authService.profile;

  constructor() {
    this.loadDashboard();
  }

  protected displayId(id: number) {
    return String(((id * 7919) % 9000) + 1000);
  }

  protected deleteNotification(notificationId: number) {
    this.notificationService.delete(notificationId).subscribe({
      next: () => {
        this.notifications.update((notifications) =>
          notifications.filter((notification) => notification.id !== notificationId),
        );
      },
      error: () => this.errorMessage.set('Unable to delete notification.'),
    });
  }

  private loadDashboard() {
    const userId = this.authService.userId();

    forkJoin({
      skills: this.mentorService.getSkills().pipe(catchError(() => of([]))),
      groups: this.groupService.getAll().pipe(catchError(() => of([]))),
      notifications: userId
        ? this.notificationService.getByUser(userId).pipe(catchError(() => of([])))
        : of([]),
    }).subscribe({
      next: ({ skills, groups, notifications }) => {
        this.skills.set(skills);
        this.groups.set(groups);
        this.notifications.set(notifications);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Dashboard data is temporarily unavailable.');
        this.loading.set(false);
      },
    });
  }
}
