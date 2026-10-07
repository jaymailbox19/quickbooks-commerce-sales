import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        console.error('Unauthorized: Please log in again.');
      } else if (error.status === 403) {
        console.error('Forbidden: You do not have permission.');
      } else if (error.status >= 500) {
        console.error('Server error: Please try again later.');
      } else if (error.status === 0) {
        console.error('Network error: Unable to reach the server.');
      } else {
        console.error(`Request failed: ${error.status}`);
      }

      return throwError(() => error);
    })
  );
};