import { HttpInterceptorFn } from '@angular/common/http';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  if (typeof window === 'undefined') {
    return next(req);
  }

  const token = localStorage.getItem('skillsync_token');
  if (!token) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
        'X-User-Id': readUserId(token),
        'X-User-Role': readRole(token),
      },
    }),
  );
};

function readUserId(token: string) {
  try {
    const payload = JSON.parse(window.atob(token.split('.')[1])) as { userId?: number };
    return String(payload.userId ?? '');
  } catch {
    return '';
  }
}

function readRole(token: string) {
  try {
    const payload = JSON.parse(window.atob(token.split('.')[1])) as { role?: string };
    return payload.role ?? '';
  } catch {
    return '';
  }
}
