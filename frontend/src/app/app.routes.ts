import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  // Raíz → dashboard.
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  // Login — pública.
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.LoginComponent),
  },

  // Dashboard — protegido.
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./app').then((m) => m.App),
  },

  // Productos — protegidos (CRUD completo en Fase 3).
  {
    path: 'products',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/products/product-list').then((m) => m.ProductList),
  },
  {
    path: 'products/new',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/products/product-form').then((m) => m.ProductForm),
  },
  {
    path: 'products/:id/edit',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/products/product-form').then((m) => m.ProductForm),
  },

  // Ventas — protegidas (detalle y filtros en Fase 4).
  {
    path: 'sales',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/sales/sale-list').then((m) => m.SaleList),
  },
  {
    path: 'sales/new',
    canActivate: [authGuard],
    redirectTo: 'dashboard',  // El formulario de nueva venta vive en el dashboard por ahora.
  },

  // Comodín.
  { path: '**', redirectTo: 'dashboard' },
];

