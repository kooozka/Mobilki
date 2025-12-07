import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrderService, OrderResponse } from '../../services/order.service';

@Component({
  selector: 'app-order-details',
  imports: [CommonModule, FormsModule],
  templateUrl: './order-details.component.html',
  styleUrl: './order-details.component.css'
})
export class OrderDetailsComponent implements OnInit {
  order?: OrderResponse;
  showCancelDialog = false;
  showEditDialog = false;
  cancellationReason = '';
  errorMessage = '';
  successMessage = '';
  editOrderData: any = {};

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadOrder(+id);
    }
  }

  loadOrder(id: number): void {
    this.orderService.getOrderById(id).subscribe({
      next: (order) => {
        this.order = order;
      },
      error: (error) => {
        console.error('Error loading order:', error);
        this.errorMessage = 'Nie udało się załadować szczegółów zlecenia';
      }
    });
  }

  canCancelOrder(): boolean {
    if (!this.order) return false;
    return this.order.status !== 'COMPLETED' && 
           this.order.status !== 'CANCELLED' && 
           this.order.status !== 'IN_PROGRESS';
  }

  canEditOrder(): boolean {
    if (!this.order) return false;
    return this.order.status === 'PENDING' || this.order.status === 'CONFIRMED';
  }

  openCancelDialog(): void {
    this.showCancelDialog = true;
    this.cancellationReason = '';
    this.errorMessage = '';
  }

  closeCancelDialog(): void {
    this.showCancelDialog = false;
    this.cancellationReason = '';
  }

  openEditDialog(): void {
    if (!this.order) return;
    this.showEditDialog = true;
    this.errorMessage = '';
    this.editOrderData = {
      title: this.order.title,
      pickupLocation: this.order.pickupLocation,
      pickupAddress: this.order.pickupAddress,
      pickupDate: this.formatDateForInput(this.order.pickupDate),
      deliveryLocation: this.order.deliveryLocation,
      deliveryAddress: this.order.deliveryAddress,
      deliveryDeadline: this.formatDateForInput(this.order.deliveryDeadline),
      cargoWeight: this.order.cargoWeight,
      description: this.order.description
    };
  }

  closeEditDialog(): void {
    this.showEditDialog = false;
    this.editOrderData = {};
  }

  formatDateForInput(dateString: string): string {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  confirmEdit(): void {
    if (!this.order) return;
    
    const request = {
      title: this.editOrderData.title,
      pickupLocation: this.editOrderData.pickupLocation,
      pickupAddress: this.editOrderData.pickupAddress,
      pickupDate: this.editOrderData.pickupDate,
      deliveryLocation: this.editOrderData.deliveryLocation,
      deliveryAddress: this.editOrderData.deliveryAddress,
      deliveryDeadline: this.editOrderData.deliveryDeadline,
      cargoWeight: this.editOrderData.cargoWeight,
      description: this.editOrderData.description
    };

    this.orderService.updateOrder(this.order.id, request).subscribe({
      next: (response) => {
        this.order = response;
        this.successMessage = 'Zlecenie zostało zaktualizowane pomyślnie';
        this.closeEditDialog();
        setTimeout(() => {
          this.successMessage = '';
        }, 3000);
      },
      error: (error) => {
        console.error('Error updating order:', error);
        if (error.error && error.error.message) {
          this.errorMessage = error.error.message;
        } else if (error.error && typeof error.error === 'string') {
          this.errorMessage = error.error;
        } else {
          this.errorMessage = 'Wystąpił błąd podczas aktualizacji zlecenia';
        }
      }
    });
  }

  confirmCancel(): void {
    if (!this.order) return;
    
    if (!this.cancellationReason.trim()) {
      this.errorMessage = 'Proszę podać powód anulowania';
      return;
    }

    this.orderService.cancelOrder(this.order.id, this.cancellationReason).subscribe({
      next: (updatedOrder) => {
        this.order = updatedOrder;
        this.successMessage = 'Zlecenie zostało anulowane';
        this.showCancelDialog = false;
        this.errorMessage = '';
      },
      error: (error) => {
        if (error.error?.message) {
          this.errorMessage = error.error.message;
        } else if (error.error) {
          this.errorMessage = typeof error.error === 'string' ? error.error : 'Nie udało się anulować zlecenia';
        } else {
          this.errorMessage = 'Nie udało się anulować zlecenia. Sprawdź status zlecenia.';
        }
      }
    });
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

  getStatusClass(status: string): string {
    return status.toLowerCase().replace('_', '-');
  }

  getVehicleTypeDisplay(type: string): string {
    const map: {[key: string]: string} = {
      'SMALL_VAN': 'Mały van',
      'MEDIUM_TRUCK': 'Średnia ciężarówka',
      'LARGE_TRUCK': 'Duża ciężarówka',
      'SEMI_TRUCK': 'Naczepa'
    };
    return map[type] || type;
  }

  goBack(): void {
    this.router.navigate(['/client/dashboard']);
  }
}
