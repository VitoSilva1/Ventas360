import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Interceptor HTTP funcional que adjunta el Google ID token como Bearer token
 * en el header Authorization de cada petición saliente, cuando hay sesión activa.
 *
 * El API Gateway y el auth-service esperan este header en las rutas protegidas:
 *   Authorization: Bearer <google-id-token>
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.idToken();

  if (!token) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  });

  return next(authReq);
};
