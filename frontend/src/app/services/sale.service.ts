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

export interface SaleEventRequest {
  saleId: string;
  customerId: number | null;
  customerEmail: string;
  saleDate: string;
  items: SaleEventItemRequest[];
}

export interface SaleEventItemRequest {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface SaleEventResponse {
  eventId: string;
  exchange: string;
}

@Injectable({ providedIn: 'root' })
export class SaleService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/sales';
  private readonly eventsUrl = '/api/sales/events';

  findAll(): Observable<Sale[]> {
    return this.http.get<Sale[]>(this.apiUrl);
  }

  findById(id: number): Observable<Sale> {
    return this.http.get<Sale>(`${this.apiUrl}/${id}`);
  }

  publishEvent(request: SaleEventRequest): Observable<SaleEventResponse> {
    return this.http.post<SaleEventResponse>(this.eventsUrl, request);
  }
}

export interface SaleItemRequest {
  productId: number;
  quantity: number;
  unitPrice: number;
}
