import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

/**
 * Declaración del objeto global `google` inyectado por el SDK de Google Identity Services.
 * No se instala como paquete npm — se carga como script externo en index.html.
 */
declare const google: {
  accounts: {
    id: {
      initialize(config: { client_id: string; callback: (response: { credential: string }) => void; auto_select: boolean }): void;
      renderButton(parent: HTMLElement, options: object): void;
      disableAutoSelect(): void;
    };
  };
};

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly error = signal('');
  protected readonly loading = signal(false);

  ngOnInit(): void {
    // Si ya hay sesión, redirigir directamente al dashboard.
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
      return;
    }

    // Esperar a que el SDK de Google esté disponible en el DOM.
    this.waitForGoogleSdk().then(() => this.initGoogleButton());
  }

  /** Espera hasta 5 segundos a que el SDK de Google Identity Services esté listo. */
  private waitForGoogleSdk(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (typeof google !== 'undefined') {
        resolve();
        return;
      }
      let attempts = 0;
      const interval = setInterval(() => {
        attempts++;
        if (typeof google !== 'undefined') {
          clearInterval(interval);
          resolve();
        } else if (attempts >= 50) {
          clearInterval(interval);
          reject(new Error('Google SDK no disponible'));
        }
      }, 100);
    });
  }

  private initGoogleButton(): void {
    try {
      google.accounts.id.initialize({
        client_id: environment.googleClientId,
        callback: (response) => this.handleGoogleCredential(response.credential),
        auto_select: false,
      });

      const buttonContainer = document.getElementById('google-signin-button');
      if (buttonContainer) {
        google.accounts.id.renderButton(buttonContainer, {
          type: 'standard',
          shape: 'rectangular',
          theme: 'outline',
          size: 'large',
          text: 'signin_with',
          locale: 'es',
          width: 280,
        });
      }
    } catch {
      this.error.set('No se pudo cargar el botón de Google. Verifica tu conexión.');
    }
  }

  private handleGoogleCredential(credential: string): void {
    this.loading.set(true);
    this.error.set('');

    this.authService.signIn(credential).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo iniciar sesión. El token de Google no fue aceptado por el servidor.');
      },
    });
  }
}
