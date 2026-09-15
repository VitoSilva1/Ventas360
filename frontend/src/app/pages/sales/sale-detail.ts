import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { Sidebar } from '../../layout/sidebar';
import { Topbar } from '../../layout/topbar';
import { Sale, SaleService } from '../../services/sale.service';
import { Product, ProductService } from '../../services/product.service';

@Component({
  selector: 'app-sale-detail',
  imports: [Sidebar, Topbar, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './sale-detail.html',
  styleUrl: './sale-detail.css',
})
export class SaleDetail implements OnInit {
  private readonly saleService = inject(SaleService);
  private readonly productService = inject(ProductService);
  private readonly route = inject(ActivatedRoute);

  protected readonly sale = signal<Sale | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  /** Mapa id→nombre para resolver nombres en el template sin llamadas extra. */
  private productMap = new Map<number, string>();

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    forkJoin({
      sale: this.saleService.findById(id),
      products: this.productService.findAll(),
    }).subscribe({
      next: ({ sale, products }) => {
        this.productMap = new Map(products.map((p: Product) => [p.id, p.name]));
        this.sale.set(sale);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el detalle de la venta.');
        this.loading.set(false);
      },
    });
  }

  /** Devuelve el nombre del producto o un fallback si no está en el catálogo. */
  protected productName(productId: number): string {
    return this.productMap.get(productId) ?? `Producto #${productId}`;
  }

  protected statusLabel(s: Sale['status']): string {
    return { CREATED: 'Creada', CONFIRMED: 'Confirmada', CANCELLED: 'Cancelada' }[s] ?? s;
  }

  protected statusClass(s: Sale['status']): string {
    return 'status-' + s.toLowerCase();
  }
}

