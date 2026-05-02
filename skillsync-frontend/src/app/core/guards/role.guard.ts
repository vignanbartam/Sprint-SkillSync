import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  if (typeof window === 'undefined') {
    return true;
  }

  const authService = inject(AuthService);
  const router = inject(Router);
  const expectedRole = route.data['role'] as string | undefined;

  if (!expectedRole) {
    return true;
  }

  return authService.hasRole(expectedRole)
    ? true
    : router.createUrlTree(['/dashboard']);
};
