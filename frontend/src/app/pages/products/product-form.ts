import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Sidebar } from '../../layout/sidebar';
import { Topbar } from '../../layout/topbar';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-product-form',
  imports: [Sidebar, Topbar, RouterLink, ReactiveFormsModule],
  templateUrl: './product-form.html',
  styleUrl: './product-form.css',
})
export class ProductForm implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected editId: number | null = null;
  protected readonly isEdit = signal(false);
  protected readonly loading = signal(false);
  protected readonly loadError = signal('');
  protected readonly saveError = signal('');
  protected readonly saving = signal(false);

  protected readonly form = this.fb.group({
    name:        ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', [Validators.required, Validators.maxLength(500)]],
    price:       [0,  [Validators.required, Validators.min(0.01)]],
    stock:       [0,  [Validators.required, Validators.min(0)]],
    active:      [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId = Number(id);
      this.isEdit.set(true);
      this.loading.set(true);
      this.productService.findById(this.editId).subscribe({
        next: (product) => {
          this.form.setValue({
            name: product.name,
            description: product.description,
            price: product.price,
            stock: product.stock,
            active: product.active,
          });
          this.loading.set(false);
        },
        error: () => {
          this.loadError.set('No se pudo cargar el producto. Verifica la conexion al backend.');
          this.loading.set(false);
        },
      });
    }
  }

  protected save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const payload = this.form.getRawValue() as {
      name: string; description: string; price: number; stock: number; active: boolean;
    };
    this.saving.set(true);
    this.saveError.set('');
    const request = { ...payload };
    const op$ = this.editId
      ? this.productService.update(this.editId, request)
      : this.productService.create(request);

    op$.subscribe({
      next: () => this.router.navigate(['/products']),
      error: () => { this.saveError.set('Error al guardar. Verifica los datos e intenta de nuevo.'); this.saving.set(false); },
    });
  }

  /** Acceso rapido a los controles del form para el template. */
  protected get f() { return this.form.controls; }

  protected hasError(field: keyof typeof this.form.controls, error: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl && ctrl.touched && ctrl.hasError(error));
  }
}
