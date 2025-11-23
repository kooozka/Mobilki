import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrderService, OrderResponse } from '../../services/order.service';

@Component({
  selector: 'app-order-history',
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './order-history.component.html',
  styleUrl: './order-history.component.css'
})
export class OrderHistoryComponent implements OnInit {
  orders: OrderResponse[] = [];
  filteredOrders: OrderResponse[] = [];
  statusFilter: string = 'ALL';

  constructor(
    private orderService: OrderService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.orderService.getMyOrders().subscribe({
      next: (orders) => {
        this.orders = orders.sort((a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        this.applyFilter();
      },
      error: (error) => {
        console.error('Error loading orders:', error);
      }
    });
  }

  applyFilter(): void {
    if (this.statusFilter === 'ALL') {
      this.filteredOrders = [...this.orders];
    } else {
      this.filteredOrders = this.orders.filter(o => o.status === this.statusFilter);
    }
  }

  viewDetails(orderId: number): void {
    this.router.navigate(['/client/order-details', orderId]);
  }

  getStatusClass(status: string): string {
    return status.toLowerCase().replace('_', '-');
  }

  getStatusDisplay(status: string): string {
    const map: {[key: string]: string} = {
      'PENDING': 'Oczekujące',
      'CONFIRMED': 'Potwierdzone',
      'ASSIGNED': 'Przypisane',
      'IN_PROGRESS': 'W realizacji',
      'COMPLETED': 'Zakończone',
      'CANCELLED': 'Anulowane'
    };
    return map[status] || status;
  }

  goBack(): void {
    this.router.navigate(['/client/dashboard']);
  }
}
