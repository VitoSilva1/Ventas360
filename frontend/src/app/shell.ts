import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';

/**
 * Componente raíz mínimo — solo contiene el router-outlet.
 * Su único rol es ser el punto de entrada del router y restaurar la sesión al inicio.
 * El dashboard real vive en App, el login en LoginComponent.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class Shell implements OnInit {
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    // Restaurar sesión desde localStorage al recargar la página.
    const restore$ = this.authService.restoreSession();
    if (restore$) {
      restore$.subscribe({ error: () => {} }); // el servicio ya redirige si falla
    }
  }
}
