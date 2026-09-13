import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Sale {
  id: number;
  customerId: number;
  total: number;
  status: 'CREATED' | 'CONFIRMED' | 'CANCELLED';
  createdAt: string;
  items: SaleItem[];
}

export interface SaleItem {
  id: number;
  productId: number;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

@Injectable({ providedIn: 'root' })
export class SaleService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/sales';

  findAll(): Observable<Sale[]> {
    return this.http.get<Sale[]>(this.apiUrl);
  }
}
