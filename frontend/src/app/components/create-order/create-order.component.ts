import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OrderService, CreateOrderRequest, AddressValidationResponse } from '../../services/order.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

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
  isSubmitting = false;

  // Status walidacji adresów
  pickupValidation: AddressValidationResponse | null = null;
  deliveryValidation: AddressValidationResponse | null = null;
  pickupValidating = false;
  deliveryValidating = false;

  // Debounce dla walidacji
  private pickupSubject = new Subject<string>();
  private deliverySubject = new Subject<string>();

  constructor(
    private orderService: OrderService,
    private router: Router
  ) {
    // Setup debounce dla walidacji pickup
    this.pickupSubject.pipe(
      debounceTime(800),
      distinctUntilChanged(),
      switchMap(address => {
        if (!address || address.length < 3) {
          return of(null);
        }
        this.pickupValidating = true;
        return this.orderService.validateAddress(address).pipe(
          catchError(err => {
            console.error('Validation error:', err);
            return of(null);
          })
        );
      })
    ).subscribe(result => {
      this.pickupValidating = false;
      this.pickupValidation = result;
      if (result?.valid && result.formattedAddress) {
        // Opcjonalnie: użyj sformatowanego adresu
        // this.orderData.pickupLocation = result.formattedAddress;
      }
    });

    // Setup debounce dla walidacji delivery
    this.deliverySubject.pipe(
      debounceTime(800),
      distinctUntilChanged(),
      switchMap(address => {
        if (!address || address.length < 3) {
          return of(null);
        }
        this.deliveryValidating = true;
        return this.orderService.validateAddress(address).pipe(
          catchError(err => {
            console.error('Validation error:', err);
            return of(null);
          })
        );
      })
    ).subscribe(result => {
      this.deliveryValidating = false;
      this.deliveryValidation = result;
    });
  }

  onPickupLocationChange(): void {
    this.pickupValidation = null;
    this.pickupSubject.next(this.orderData.pickupLocation);
  }

  onDeliveryLocationChange(): void {
    this.deliveryValidation = null;
    this.deliverySubject.next(this.orderData.deliveryLocation);
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.validateForm()) {
      return;
    }

    this.isSubmitting = true;
    console.log('Wysyłane dane zlecenia:', this.orderData);

    this.orderService.createOrder(this.orderData).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        this.successMessage = 'Zlecenie zostało utworzone pomyślnie!';
        setTimeout(() => {
          this.router.navigate(['/client/dashboard']);
        }, 1500);
      },
      error: (error) => {
        this.isSubmitting = false;
        console.error('Pełny błąd:', error);

        if (error.error?.message) {
          this.errorMessage = error.error.message;
        } else if (error.error && typeof error.error === 'string') {
          this.errorMessage = error.error;
        } else if (error.error) {
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
    if (!this.orderData.title || !this.orderData.title.trim()) {
      this.errorMessage = 'Proszę podać nazwę zlecenia';
      return false;
    }

    if (!this.orderData.pickupLocation || !this.orderData.deliveryLocation) {
      this.errorMessage = 'Proszę podać lokalizacje odbioru i dostawy';
      return false;
    }

    // Sprawdź walidację adresów
    if (this.pickupValidation && !this.pickupValidation.valid) {
      this.errorMessage = 'Adres odbioru jest nieprawidłowy. Sprawdź lokalizację.';
      return false;
    }

    if (this.deliveryValidation && !this.deliveryValidation.valid) {
      this.errorMessage = 'Adres dostawy jest nieprawidłowy. Sprawdź lokalizację.';
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
