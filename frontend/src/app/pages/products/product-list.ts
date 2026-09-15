import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { Sidebar } from '../../layout/sidebar';
import { Topbar } from '../../layout/topbar';
import { Product, ProductService } from '../../services/product.service';

@Component({
  selector: 'app-product-list',
  imports: [Sidebar, Topbar, RouterLink, CurrencyPipe],
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
})
export class ProductList implements OnInit {
  private readonly productService = inject(ProductService);

  protected readonly products = signal<Product[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly deletingId = signal<number | null>(null);
  protected confirmDeleteId: number | null = null;

  ngOnInit(): void {
    this.loadProducts();
  }

  protected loadProducts(): void {
    this.loading.set(true);
    this.error.set('');
    this.productService.findAll().subscribe({
      next: (list) => { this.products.set(list); this.loading.set(false); },
      error: () => { this.error.set('No se pudo cargar el catálogo. Verifica la conexión al backend.'); this.loading.set(false); },
    });
  }

  protected requestDelete(id: number): void {
    this.confirmDeleteId = id;
  }

  protected cancelDelete(): void {
    this.confirmDeleteId = null;
  }

  protected confirmDelete(): void {
    const id = this.confirmDeleteId;
    if (id == null) return;
    this.confirmDeleteId = null;
    this.deletingId.set(id);
    this.productService.delete(id).subscribe({
      next: () => { this.products.update((list) => list.filter((p) => p.id !== id)); this.deletingId.set(null); },
      error: () => { this.error.set('No se pudo eliminar el producto. Intenta de nuevo.'); this.deletingId.set(null); },
    });
  }
}
