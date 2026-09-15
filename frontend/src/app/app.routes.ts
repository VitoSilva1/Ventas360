import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  { path: 'login', loadComponent: () => import('./pages/login/login').then((m) => m.LoginComponent) },

  // Dashboard — ahora en su propio componente (Fase 5).
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard),
  },

  // Productos — CRUD completo (Fase 3).
  { path: 'products',          canActivate: [authGuard], loadComponent: () => import('./pages/products/product-list').then((m) => m.ProductList) },
  { path: 'products/new',      canActivate: [authGuard], loadComponent: () => import('./pages/products/product-form').then((m) => m.ProductForm) },
  { path: 'products/:id/edit', canActivate: [authGuard], loadComponent: () => import('./pages/products/product-form').then((m) => m.ProductForm) },

  // Ventas — lista, detalle y nueva venta (Fases 4).
  { path: 'sales',     canActivate: [authGuard], loadComponent: () => import('./pages/sales/sale-list').then((m) => m.SaleList) },
  { path: 'sales/:id', canActivate: [authGuard], loadComponent: () => import('./pages/sales/sale-detail').then((m) => m.SaleDetail) },

  { path: '**', redirectTo: 'dashboard' },
];
