import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = sessionStorage.getItem('jwt_token');

  const isLoginRequest = req.url.endsWith('/api/auth/login');

  if (
    token &&
    !isLoginRequest &&
    req.url.startsWith('http://localhost:8080/api/')
  ) {
    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });

    return next(authReq);
  }

  return next(req);
};