import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrderResponse } from './order.service';
import { RouteResponse, DriverScheduleResponse } from './dispatch.service';

export interface DriverStatsResponse {
  totalOrders: number;
  completedOrders: number;
  activeOrders: number;
  totalRoutes: number;
  completedRoutes: number;
  activeRoutes: number;
  totalDistanceKm: number;
}

@Injectable({
  providedIn: 'root'
})
export class DriverApiService {
  private apiUrl = 'http://localhost:8080/api/driver';

  constructor(private http: HttpClient) { }

  // ========== TRASY ==========

  getMyRoutes(): Observable<RouteResponse[]> {
    return this.http.get<RouteResponse[]>(`${this.apiUrl}/routes`);
  }

  getMyActiveRoutes(): Observable<RouteResponse[]> {
    return this.http.get<RouteResponse[]>(`${this.apiUrl}/routes/active`);
  }

  getRouteById(id: number): Observable<RouteResponse> {
    return this.http.get<RouteResponse>(`${this.apiUrl}/routes/${id}`);
  }

  startRoute(id: number): Observable<RouteResponse> {
    return this.http.put<RouteResponse>(`${this.apiUrl}/routes/${id}/start`, {});
  }

  completeRoute(id: number): Observable<RouteResponse> {
    return this.http.put<RouteResponse>(`${this.apiUrl}/routes/${id}/complete`, {});
  }

  // ========== ZLECENIA ==========

  getMyOrders(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`${this.apiUrl}/orders`);
  }

  getMyActiveOrders(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`${this.apiUrl}/orders/active`);
  }

  pickupOrder(id: number): Observable<OrderResponse> {
    return this.http.put<OrderResponse>(`${this.apiUrl}/orders/${id}/pickup`, {});
  }

  deliverOrder(id: number): Observable<OrderResponse> {
    return this.http.put<OrderResponse>(`${this.apiUrl}/orders/${id}/deliver`, {});
  }

  // ========== GRAFIK ==========

  getMySchedule(): Observable<DriverScheduleResponse> {
    return this.http.get<DriverScheduleResponse>(`${this.apiUrl}/schedule`);
  }

  // ========== STATYSTYKI ==========

  getMyStats(): Observable<DriverStatsResponse> {
    return this.http.get<DriverStatsResponse>(`${this.apiUrl}/stats`);
  }
}
