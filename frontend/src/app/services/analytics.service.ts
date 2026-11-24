import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Read model built by analytics-service from the Kafka sales and product streams. */
export interface AnalyticsSummary {
  totalSales: number;
  totalRevenue: number;
  averageTicket: number;
  revenueToday: number;
  topProducts: TopProduct[];
  lowStock: LowStockProduct[];
  generatedAt: string;
}

export interface TopProduct {
  productId: number;
  name: string;
  unitsSold: number;
  revenue: number;
}

export interface LowStockProduct {
  productId: number;
  name: string;
  stock: number;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/analytics';

  summary(): Observable<AnalyticsSummary> {
    return this.http.get<AnalyticsSummary>(`${this.apiUrl}/summary`);
  }
}
