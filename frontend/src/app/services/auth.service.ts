import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

/** Perfil de usuario retornado por el auth-service tras verificar el ID token de Google. */
export interface GoogleProfile {
  subject: string;
  email: string;
  name: string | null;
  picture: string | null;
}

const STORAGE_KEY = 'ventas360_id_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /** Google ID token almacenado en localStorage. Null si no hay sesión activa. */
  readonly idToken = signal<string | null>(localStorage.getItem(STORAGE_KEY));

  /** Perfil del usuario autenticado. Se carga al hacer sign-in y se persiste en memoria. */
  readonly currentUser = signal<GoogleProfile | null>(null);

  /** True cuando hay un token válido guardado. */
  readonly isAuthenticated = computed(() => this.idToken() !== null);

  /**
   * Envía el credential (ID token de Google) al auth-service para su verificación.
   * Si el servidor lo acepta, guarda el token y el perfil en memoria y localStorage.
   */
  signIn(credential: string): Observable<GoogleProfile> {
    return this.http
      .post<GoogleProfile>('/api/auth/google/sign-in', { credential })
      .pipe(
        tap((profile) => {
          localStorage.setItem(STORAGE_KEY, credential);
          this.idToken.set(credential);
          this.currentUser.set(profile);
        }),
      );
  }

  /**
   * Notifica al backend el cierre de sesión (stateless/local) y limpia el estado local.
   * El token de Google seguirá siendo válido hasta que expire (~1h), pero el frontend
   * ya no lo enviará en ninguna petición.
   */
  logout(): void {
    // Llamada al backend para registrar el logout (responde 204, sin body).
    this.http.post('/api/auth/logout', {}).subscribe({
      error: () => {
        // Ignoramos errores de red en el logout — igual limpiamos sesión local.
      },
    });

    localStorage.removeItem(STORAGE_KEY);
    this.idToken.set(null);
    this.currentUser.set(null);

    // Desactiva la selección automática de cuenta de Google si el SDK está cargado.
    const google = (window as any).google;
    if (google?.accounts?.id) {
      google.accounts.id.disableAutoSelect();
    }

    this.router.navigate(['/login']);
  }

  /**
   * Intenta restaurar el perfil del usuario a partir del token guardado en localStorage.
   * Útil al recargar la página: el token existe pero el perfil en memoria está vacío.
   * Llama a sign-in con el token guardado para re-verificar y recargar el perfil.
   */
  restoreSession(): Observable<GoogleProfile> | null {
    const token = this.idToken();
    if (!token) return null;
    return this.http
      .post<GoogleProfile>('/api/auth/google/sign-in', { credential: token })
      .pipe(
        tap((profile) => this.currentUser.set(profile)),
      );
  }
}
