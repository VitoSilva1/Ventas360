import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Sidebar } from '../../layout/sidebar';
import { Topbar } from '../../layout/topbar';
import { Sale, SaleService } from '../../services/sale.service';

type StatusFilter = 'ALL' | 'CREATED' | 'CONFIRMED' | 'CANCELLED';

@Component({
  selector: 'app-sale-list',
  imports: [Sidebar, Topbar, RouterLink, CurrencyPipe, DatePipe, FormsModule],
  templateUrl: './sale-list.html',
  styleUrl: './sale-list.css',
})
export class SaleList implements OnInit {
  private readonly saleService = inject(SaleService);

  protected readonly sales = signal<Sale[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected statusFilter = signal<StatusFilter>('ALL');

  protected readonly filtered = computed(() => {
    const f = this.statusFilter();
    return f === 'ALL' ? this.sales() : this.sales().filter((s) => s.status === f);
  });

  ngOnInit(): void { this.loadSales(); }

  protected loadSales(): void {
    this.loading.set(true);
    this.error.set('');
    this.saleService.findAll().subscribe({
      next: (list) => { this.sales.set(list); this.loading.set(false); },
      error: () => { this.error.set('No se pudo cargar el historial de ventas.'); this.loading.set(false); },
    });
  }

  protected setFilter(f: StatusFilter): void { this.statusFilter.set(f); }

  protected statusLabel(s: Sale['status']): string {
    return { CREATED: 'Creada', CONFIRMED: 'Confirmada', CANCELLED: 'Cancelada' }[s] ?? s;
  }
}
