import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { Role } from '../../directives/role';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, Role],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
})
export class Sidebar {
  protected readonly authService = inject(AuthService);
}
