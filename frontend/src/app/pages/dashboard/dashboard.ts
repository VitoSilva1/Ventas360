import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Sidebar } from '../../layout/sidebar';
import { Topbar } from '../../layout/topbar';
import { Product, ProductService } from '../../services/product.service';
import { Sale, SaleItemRequest, SaleService } from '../../services/sale.service';

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, DatePipe, FormsModule, RouterLink, Sidebar, Topbar],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly saleService = inject(SaleService);

  protected readonly products = signal<Product[]>([]);
  protected readonly sales = signal<Sale[]>([]);
  protected readonly loading = signal(true);
  protected readonly productError = signal('');
  protected readonly salesError = signal('');
  protected readonly totalSales = computed(() => this.sales().reduce((t, s) => t + s.total, 0));
  protected readonly salesCount = computed(() => this.sales().length);
  protected readonly recentSales = computed(() => [...this.sales()].reverse().slice(0, 5));
  protected readonly activeProducts = computed(() => this.products().filter(p => p.active).length);

  protected newSaleProductId: number | null = null;
  protected newSaleQuantity = 1;
  protected readonly draftItems = signal<SaleItemRequest[]>([]);
  protected readonly activeAction = signal('');
  protected readonly actionMessage = signal('');
  protected saleMessage = '';
  protected saleError = '';

  ngOnInit(): void {
    this.productService.findAll().subscribe({
      next: (p) => { this.products.set(p); this.loading.set(false); },
      error: () => { this.productError.set('Error cargando productos desde product-service.'); this.loading.set(false); },
    });
    this.saleService.findAll().subscribe({
      next: (s) => this.sales.set(s),
      error: () => this.salesError.set('Error cargando ventas desde sales-service.'),
    });
  }

  protected productColor(index: number): string {
    return ['color-blue', 'color-purple', 'color-teal', 'color-orange', 'color-rose'][index % 5];
  }

  protected productInitials(name: string): string {
    return name.split(' ').slice(0, 2).map(w => w[0]).join('').toUpperCase();
  }

  protected scrollToSaleForm(): void {
    document.getElementById('nueva-venta')?.scrollIntoView({ behavior: 'smooth' });
  }

  protected addSaleItem(): void {
    if (!this.newSaleProductId) return;
    const product = this.products().find(p => p.id === this.newSaleProductId);
    if (!product) return;
    const existing = this.draftItems().find(i => i.productId === this.newSaleProductId!);
    if (existing) {
      this.draftItems.update(items => items.map(i => i.productId === this.newSaleProductId
        ? { ...i, quantity: i.quantity + this.newSaleQuantity }
        : i));
    } else {
      this.draftItems.update(items => [...items, {
        productId: product.id, quantity: this.newSaleQuantity, unitPrice: product.price,
      }]);
    }
    this.newSaleProductId = null;
    this.newSaleQuantity = 1;
  }

  protected removeSaleItem(productId: number): void {
    this.draftItems.update(items => items.filter(i => i.productId !== productId));
  }

  protected draftProductName(productId: number): string {
    return this.products().find(p => p.id === productId)?.name ?? `Producto #${productId}`;
  }

  protected draftItemSubtotal(item: SaleItemRequest): number {
    return item.quantity * item.unitPrice;
  }

  protected readonly draftTotal = computed(() =>
    this.draftItems().reduce((t, i) => t + i.quantity * i.unitPrice, 0));

  protected createSale(): void {
    if (this.draftItems().length === 0) return;
    this.activeAction.set('create-sale');
    this.saleMessage = '';
    this.saleError = '';
    this.saleService.create({ customerId: null, items: this.draftItems() }).subscribe({
      next: (sale) => {
        this.sales.update(s => [sale, ...s]);
        this.draftItems.set([]);
        this.saleMessage = `Venta #${sale.id} registrada correctamente.`;
        this.activeAction.set('');
      },
      error: () => { this.saleError = 'Error al registrar la venta. Intenta de nuevo.'; this.activeAction.set(''); },
    });
  }

  protected saleProductName(productId: number): string {
    return this.products().find(p => p.id === productId)?.name ?? `Producto #${productId}`;
  }
}
