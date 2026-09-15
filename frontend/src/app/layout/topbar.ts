import { Component, Input, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-topbar',
  imports: [DatePipe],
  templateUrl: './topbar.html',
  styleUrl: './topbar.css',
})
export class Topbar {
  @Input() title = '';

  protected readonly authService = inject(AuthService);
  protected readonly today = new Date();

  protected readonly userInitials = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return '?';
    if (user.name) {
      return user.name.split(' ').slice(0, 2).map((w) => w[0]).join('').toUpperCase();
    }
    return user.email.slice(0, 2).toUpperCase();
  });
}
