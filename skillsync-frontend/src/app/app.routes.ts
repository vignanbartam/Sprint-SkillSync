import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { UserManagement } from './features/admin/pages/user-management/user-management';
import { Login } from './features/auth/pages/login/login';
import { Register } from './features/auth/pages/register/register';
import { LearnerDashboard } from './features/dashboard/pages/learner-dashboard/learner-dashboard';
import { GroupList } from './features/groups/pages/group-list/group-list';
import { MentorList } from './features/mentors/pages/mentor-list/mentor-list';
import { MentorSearch } from './features/mentors/pages/mentor-search/mentor-search';
import { Profile } from './features/profile/pages/profile/profile';
import { BookSession } from './features/sessions/pages/book-session/book-session';
import { Shell } from './layout/shell/shell';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: LearnerDashboard },
      { path: 'profile', component: Profile },
      { path: 'profile/:id', component: Profile },
      { path: 'mentor-search', component: MentorSearch },
      { path: 'mentors', component: MentorList },
      { path: 'sessions', component: BookSession },
      { path: 'groups', component: GroupList },
      {
        path: 'admin',
        component: UserManagement,
        canActivate: [roleGuard],
        data: { role: 'ROLE_ADMIN' },
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
