import { Component, OnInit, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { inject } from '@angular/core';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar implements OnInit {
  private readonly http = inject(HttpClient);

  /** true = backend responde, false = sin conexion, null = verificando */
  protected readonly backendOnline = signal<boolean | null>(null);

  ngOnInit(): void { this.checkHealth(); }

  private checkHealth(): void {
    this.http.get('/api/actuator/health', { observe: 'response' }).subscribe({
      next: (r) => this.backendOnline.set(r.status === 200),
      error: () => this.backendOnline.set(false),
    });
  }
}
