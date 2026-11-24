import { Component, input, output } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { AnalyticsSummary } from '../../services/analytics.service';

/**
 * Presentational component: renders the Kafka-fed analytics summary it receives.
 * It performs no HTTP calls; the container (dashboard) owns loading and errors.
 */
@Component({
  selector: 'app-analytics-panel',
  imports: [CurrencyPipe, DatePipe],
  templateUrl: './analytics-panel.html',
  styleUrl: './analytics-panel.css',
})
export class AnalyticsPanel {
  readonly summary = input<AnalyticsSummary | null>(null);
  readonly loading = input(false);
  readonly error = input('');
  readonly refresh = output<void>();
}
