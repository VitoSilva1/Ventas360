import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Product, ProductService } from './services/product.service';
import { Sale, SaleService } from './services/sale.service';

@Component({
  selector: 'app-root',
  imports: [CurrencyPipe],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly saleService = inject(SaleService);
  protected readonly products = signal<Product[]>([]);
  protected readonly sales = signal<Sale[]>([]);
  protected readonly loading = signal(true);
  protected readonly productError = signal('');
  protected readonly salesError = signal('');
  protected readonly totalSales = computed(() => this.sales().reduce((total, sale) => total + sale.total, 0));
  protected readonly salesCount = computed(() => this.sales().length);

  ngOnInit(): void {
    this.productService.findAll().subscribe({
      next: (products) => {
        this.products.set(products.filter((product) => product.active));
        this.loading.set(false);
      },
      error: () => {
        this.productError.set('No fue posible cargar los productos. Verifica que product-service esté ejecutándose.');
        this.loading.set(false);
      }
    });

    this.saleService.findAll().subscribe({
      next: (sales) => this.sales.set(sales),
      error: () => this.salesError.set('No fue posible cargar las ventas desde sales-service.')
    });
  }

  protected scrollToProducts(): void {
    document.getElementById('productos')?.scrollIntoView({ behavior: 'smooth' });
  }

  protected productInitials(name: string): string {
    return name.split(' ').slice(0, 2).map((word) => word[0]).join('').toUpperCase();
  }

  protected productColor(index: number): string {
    return ['product-blue', 'product-purple', 'product-orange', 'product-green'][index % 4];
  }
}
