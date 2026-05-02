import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

export const errorInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let message = error.message || 'Something went wrong.';

      if (typeof error.error === 'string') {
        try {
          const parsed = JSON.parse(error.error);
          message = parsed.message || error.error;
        } catch {
          message = error.error;
        }
      } else if (error.error?.message) {
        message = error.error.message;
      }

      return throwError(() => new Error(message));
    }),
  );
