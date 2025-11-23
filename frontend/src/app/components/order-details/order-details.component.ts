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
  vehicleTypes: string[] = [];

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
      title: this.order.title, // NEW: include title for editing
      pickupLocation: this.order.pickupLocation,
      pickupAddress: this.order.pickupAddress,
      pickupTimeFrom: this.formatDateForInput(this.order.pickupTimeFrom),
      pickupTimeTo: this.formatDateForInput(this.order.pickupTimeTo),
      deliveryLocation: this.order.deliveryLocation,
      deliveryAddress: this.order.deliveryAddress,
      deliveryTimeFrom: this.formatDateForInput(this.order.deliveryTimeFrom),
      deliveryTimeTo: this.formatDateForInput(this.order.deliveryTimeTo),
      vehicleType: this.order.vehicleType,
      cargoWeight: this.order.cargoWeight,
      description: this.order.description
    };
    this.loadVehicleTypes();
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
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  loadVehicleTypes(): void {
    this.orderService.getVehicleTypes().subscribe({
      next: (types) => {
        this.vehicleTypes = types;
      },
      error: (error) => {
        console.error('Error loading vehicle types:', error);
      }
    });
  }

  confirmEdit(): void {
    if (!this.order) return;

    const request = {
      title: this.editOrderData.title,
      pickupLocation: this.editOrderData.pickupLocation,
      pickupAddress: this.editOrderData.pickupAddress,
      pickupTimeFrom: this.editOrderData.pickupTimeFrom,
      pickupTimeTo: this.editOrderData.pickupTimeTo,
      deliveryLocation: this.editOrderData.deliveryLocation,
      deliveryAddress: this.editOrderData.deliveryAddress,
      deliveryTimeFrom: this.editOrderData.deliveryTimeFrom,
      deliveryTimeTo: this.editOrderData.deliveryTimeTo,
      vehicleType: this.editOrderData.vehicleType,
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

  // NEW: determine payment label based on order status
  getPaymentStatusDisplay(status: string): string {
    const map: { [key: string]: string } = {
      'PENDING': 'Oczekuje płatności',
      'CONFIRMED': 'Opłacone',
      'ASSIGNED': 'Opłacone',
      'IN_PROGRESS': 'Opłacone',
      'COMPLETED': 'Opłacone',
      'CANCELLED': 'Zwrócone / Anulowane'
    };
    return map[status] || 'Nieznany';
  }

  // NEW: small helper for CSS class selection
  getPaymentStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
      case 'CONFIRMED':
      case 'ASSIGNED':
      case 'IN_PROGRESS':
        return 'paid';
      case 'CANCELLED':
        return 'refunded';
      case 'PENDING':
        return 'unpaid';
      default:
        return 'unpaid';
    }
  }

  getVehicleTypeDisplay(type: string): string {
    return this.orderService.getVehicleTypeDisplay(type);
  }

  goBack(): void {
    this.router.navigate(['/client/dashboard']);
  }
}
