import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Sidebar } from '../../layout/sidebar';
import { Topbar } from '../../layout/topbar';

@Component({
  selector: 'app-sale-list',
  imports: [Sidebar, Topbar, RouterLink],
  templateUrl: './sale-list.html',
  styleUrl: './sale-list.css',
})
export class SaleList {}
