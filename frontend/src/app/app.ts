import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Sidebar } from './layout/sidebar';
import { Topbar } from './layout/topbar';
import { Product, ProductService } from './services/product.service';
import { Sale, SaleItemRequest, SaleService } from './services/sale.service';

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, DatePipe, FormsModule, Sidebar, Topbar],
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
  protected newSaleProductId: number | null = null;
  protected newSaleQuantity = 1;
  protected readonly draftItems = signal<SaleItemRequest[]>([]);
  protected readonly activeAction = signal('');
  protected saleMessage = '';
  protected saleError = '';

  ngOnInit(): void {
    // La restauración de sesión la maneja Shell (componente raíz).
    // Aquí solo cargamos datos del dashboard.
    this.productService.findAll().subscribe({

      next: (products) => {
        this.products.set(products.filter((product) => product.active));
        this.newSaleProductId = this.products()[0]?.id ?? null;
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

  protected startSale(productId: number): void {
    if (this.activeAction()) return;
    this.newSaleProductId = productId;
    document.getElementById('nueva-venta')?.scrollIntoView({ behavior: 'smooth' });
  }

  protected createSale(): void {
    if (this.activeAction()) return;
    this.saleMessage = '';
    this.saleError = '';
    if (this.draftItems().length === 0) {
      this.saleError = 'Agrega al menos un producto a la venta.';
      return;
    }

    this.activeAction.set('create-sale');
    this.saleService.create({ customerId: null, items: this.draftItems() }).subscribe({
      next: (sale) => {
        this.sales.update((sales) => [...sales, sale]);
        this.saleMessage = `Venta #${sale.id} creada correctamente.`;
        this.newSaleQuantity = 1;
        this.draftItems.set([]);
        this.activeAction.set('');
      },
      error: (error) => {
        const detail = typeof error?.error === 'string' ? error.error : error?.error?.message;
        this.saleError = detail
          ? `No fue posible crear la venta: ${detail}`
          : 'No fue posible crear la venta. Verifica que sales-service esté disponible.';
        this.activeAction.set('');
      }
    });
  }

  protected addSaleItem(): void {
    if (this.activeAction()) return;
    this.saleMessage = '';
    this.saleError = '';
    const product = this.products().find((item) => item.id === this.newSaleProductId);

    if (!product || this.newSaleQuantity < 1) {
      this.saleError = 'Selecciona un producto y una cantidad válida.';
      return;
    }

    this.activeAction.set('add-item');
    window.setTimeout(() => {
      const existing = this.draftItems().find((item) => item.productId === product.id);
      if (existing) {
        this.draftItems.update((items) => items.map((item) => item.productId === product.id
          ? { ...item, quantity: item.quantity + this.newSaleQuantity }
          : item));
      } else {
        this.draftItems.update((items) => [...items, {
          productId: product.id,
          quantity: this.newSaleQuantity,
          unitPrice: product.price
        }]);
      }
      this.newSaleQuantity = 1;
      this.activeAction.set('');
    }, 350);
  }

  protected removeSaleItem(productId: number): void {
    if (this.activeAction()) return;
    this.activeAction.set('remove-item');
    window.setTimeout(() => {
      this.draftItems.update((items) => items.filter((item) => item.productId !== productId));
      this.activeAction.set('');
    }, 250);
  }

  protected actionMessage(): string {
    return {
      'add-item': 'Agregando producto a la venta...',
      'remove-item': 'Eliminando producto de la venta...',
      'create-sale': 'Registrando venta en la base de datos...'
    }[this.activeAction()] ?? '';
  }

  protected draftProductName(productId: number): string {
    return this.products().find((product) => product.id === productId)?.name ?? `Producto #${productId}`;
  }

  protected draftItemSubtotal(item: SaleItemRequest): number {
    return item.quantity * item.unitPrice;
  }

  protected draftTotal(): number {
    return this.draftItems().reduce((total, item) => total + this.draftItemSubtotal(item), 0);
  }

  protected saleProductName(productId: number): string {
    return this.products().find((product) => product.id === productId)?.name ?? `Producto #` + productId;
  }
}
