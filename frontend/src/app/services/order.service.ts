import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreateOrderRequest {
  pickupLocation: string;
  pickupAddress: string;
  pickupTimeFrom: string;
  pickupTimeTo: string;
  deliveryLocation: string;
  deliveryAddress: string;
  deliveryTimeFrom: string;
  deliveryTimeTo: string;
  vehicleType: string;
  cargoWeight: number;
  description?: string;
}

export interface OrderResponse {
  id: number;
  pickupLocation: string;
  pickupAddress: string;
  pickupTimeFrom: string;
  pickupTimeTo: string;
  deliveryLocation: string;
  deliveryAddress: string;
  deliveryTimeFrom: string;
  deliveryTimeTo: string;
  vehicleType: string;
  cargoWeight: number;
  description: string;
  status: string;
  clientEmail: string;
  driverEmail?: string;
  createdAt: string;
  updatedAt: string;
  cancellationReason?: string;
}

export interface CancelOrderRequest {
  reason: string;
}

export interface VehicleType {
  name: string;
  maxWeight: number;
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private apiUrl = 'http://localhost:8080/api/orders';

  constructor(private http: HttpClient) { }

  createOrder(request: CreateOrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(this.apiUrl, request);
  }

  getMyOrders(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(this.apiUrl);
  }

  getOrderById(id: number): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`${this.apiUrl}/${id}`);
  }

  cancelOrder(id: number, reason: string): Observable<OrderResponse> {
    const request: CancelOrderRequest = { reason };
    return this.http.put<OrderResponse>(`${this.apiUrl}/${id}/cancel`, request);
  }

  updateOrder(id: number, request: CreateOrderRequest): Observable<OrderResponse> {
    return this.http.put<OrderResponse>(`${this.apiUrl}/${id}`, request);
  }

  getVehicleTypes(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/vehicle-types`);
  }
}
