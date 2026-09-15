import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Sidebar } from '../../layout/sidebar';
import { Topbar } from '../../layout/topbar';

@Component({
  selector: 'app-product-list',
  imports: [Sidebar, Topbar, RouterLink],
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
})
export class ProductList {}
