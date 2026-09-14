import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  // Raíz: redirigir al dashboard (el guard se encargará de redirigir a /login si no hay sesión).
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },

  // Página de login — pública, sin guard.
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login').then((m) => m.LoginComponent),
  },

  // Dashboard principal — protegido.
  // Mientras se implementa la Fase 5, redirige al componente App existente como placeholder.
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./app').then((m) => m.App),
  },

  // Ruta comodín: redirigir al dashboard (el guard manejará la autenticación).
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
