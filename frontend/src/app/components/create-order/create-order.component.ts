import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OrderService, CreateOrderRequest } from '../../services/order.service';

@Component({
  selector: 'app-create-order',
  imports: [CommonModule, FormsModule],
  templateUrl: './create-order.component.html',
  styleUrl: './create-order.component.css'
})
export class CreateOrderComponent {
  orderData: CreateOrderRequest = {
    title: '',
    pickupLocation: '',
    pickupAddress: '',
    pickupDate: '',
    deliveryLocation: '',
    deliveryAddress: '',
    deliveryDeadline: '',
    cargoWeight: 0,
    description: ''
  };

  errorMessage = '';
  successMessage = '';

  constructor(
    private orderService: OrderService,
    private router: Router
  ) {}

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.validateForm()) {
      return;
    }

    console.log('Wysyłane dane zlecenia:', this.orderData);

    this.orderService.createOrder(this.orderData).subscribe({
      next: (response) => {
        this.successMessage = 'Zlecenie zostało utworzone pomyślnie!';
        setTimeout(() => {
          this.router.navigate(['/client/dashboard']);
        }, 1500);
      },
      error: (error) => {
        console.error('Pełny błąd:', error);
        console.error('error.error:', error.error);
        console.error('error.status:', error.status);

        if (error.error?.message) {
          this.errorMessage = error.error.message;
        } else if (error.error && typeof error.error === 'string') {
          this.errorMessage = error.error;
        } else if (error.error) {
          // Próba wyciągnięcia wiadomości z różnych możliwych struktur
          const errorObj = error.error;
          this.errorMessage = errorObj.error || errorObj.message || JSON.stringify(errorObj);
        } else if (error.message) {
          this.errorMessage = error.message;
        } else {
          this.errorMessage = 'Błąd podczas tworzenia zlecenia. Status: ' + (error.status || 'nieznany');
        }
      }
    });
  }

  validateForm(): boolean {
    // New validation: require title
    if (!this.orderData.title || !this.orderData.title.trim()) {
      this.errorMessage = 'Proszę podać nazwę zlecenia';
      return false;
    }

    if (!this.orderData.pickupLocation || !this.orderData.deliveryLocation) {
      this.errorMessage = 'Proszę podać lokalizacje odbioru i dostawy';
      return false;
    }

    if (!this.orderData.pickupAddress || !this.orderData.deliveryAddress) {
      this.errorMessage = 'Proszę podać adresy odbioru i dostawy';
      return false;
    }

    if (!this.orderData.pickupDate || !this.orderData.deliveryDeadline) {
      this.errorMessage = 'Proszę podać datę odbioru i termin dostawy';
      return false;
    }

    // Walidacja czy data odbioru nie jest w przeszłości (minimum jutro)
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(0, 0, 0, 0);

    const pickupDate = new Date(this.orderData.pickupDate);
    if (pickupDate < tomorrow) {
      this.errorMessage = 'Data odbioru nie może być wcześniejsza niż jutro';
      return false;
    }

    const deliveryDate = new Date(this.orderData.deliveryDeadline);
    if (pickupDate > deliveryDate) {
      this.errorMessage = 'Data odbioru nie może być późniejsza niż termin dostawy';
      return false;
    }

    if (this.orderData.cargoWeight <= 0) {
      this.errorMessage = 'Waga ładunku musi być większa od 0';
      return false;
    }

    if (this.orderData.cargoWeight > 25000) {
      this.errorMessage = 'Maksymalna waga ładunku to 25000 kg';
      return false;
    }

    return true;
  }

  cancel(): void {
    this.router.navigate(['/client/dashboard']);
  }
}
