import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Sale {
  id: number;
  customerId: number | null;
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
  private readonly apiUrl = '/api/sales';

  findAll(): Observable<Sale[]> {
    return this.http.get<Sale[]>(this.apiUrl);
  }

  findById(id: number): Observable<Sale> {
    return this.http.get<Sale>(`${this.apiUrl}/${id}`);
  }

  create(request: { customerId: number | null; items: SaleItemRequest[] }): Observable<Sale> {
    return this.http.post<Sale>(this.apiUrl, request);
  }
}

export interface SaleItemRequest {
  productId: number;
  quantity: number;
  unitPrice: number;
}
