import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { OrderService, OrderResponse } from '../../services/order.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-client-dashboard',
  imports: [CommonModule, RouterModule],
  templateUrl: './client-dashboard.component.html',
  styleUrl: './client-dashboard.component.css'
})
export class ClientDashboardComponent implements OnInit {
  orders: OrderResponse[] = [];
  userEmail: string = '';
  stats = {
    total: 0,
    pending: 0,
    confirmed: 0,
    inProgress: 0,
    completed: 0,
    cancelled: 0
  };

  constructor(
    private orderService: OrderService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    this.userEmail = user?.email || '';
    this.loadOrders();
  }

  loadOrders(): void {
    this.orderService.getMyOrders().subscribe({
      next: (orders) => {
        this.orders = orders;
        this.calculateStats();
      },
      error: (error) => {
        console.error('Error loading orders:', error);
      }
    });
  }

  calculateStats(): void {
    this.stats.total = this.orders.length;
    this.stats.pending = this.orders.filter(o => o.status === 'PENDING').length;
    this.stats.confirmed = this.orders.filter(o => o.status === 'CONFIRMED').length;
    this.stats.inProgress = this.orders.filter(o => o.status === 'IN_PROGRESS').length;
    this.stats.completed = this.orders.filter(o => o.status === 'COMPLETED').length;
    this.stats.cancelled = this.orders.filter(o => o.status === 'CANCELLED').length;
  }

  calculatePrice(order: OrderResponse): string {
    // Prosta kalkulacja ceny bazująca na wadze
    const basePrice = 100;
    const pricePerKg = 2;
    const total = basePrice + (order.cargoWeight * pricePerKg);
    return total.toFixed(2);
  }

  getStatusClass(status: string): string {
    return status.toLowerCase().replace('_', '-');
  }

  getStatusDisplay(status: string): string {
    const map: {[key: string]: string} = {
      'PENDING': 'Oczekujące',
      'CONFIRMED': 'Potwierdzone',
      'ASSIGNED': 'Przypisane',
      'IN_PROGRESS': 'Realizacja',
      'COMPLETED': 'Dostarczone',
      'CANCELLED': 'Anulowane'
    };
    return map[status] || status;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
