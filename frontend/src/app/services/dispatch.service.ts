import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrderResponse } from './order.service';
import {AutoPlanningEvent, AutoPlanResponse} from './dispatch.classes';

export interface DriverResponse {
  id: number;
  email: string;
  hasActiveSchedule: boolean;
  // assignedVehicle usunięte - pojazd przypisywany do trasy
}

export interface VehicleResponse {
  id: number;
  registrationNumber: string;
  brand: string;
  model: string;
  type: string;
  maxWeight: number;
  available: boolean;
  notes: string | null;
}

export interface VehicleRequest {
  registrationNumber: string;
  brand: string;
  model: string;
  type: string;
  maxWeight: number;
  available: boolean;
  notes?: string;
}

export interface DriverScheduleResponse {
  id: number;
  driverId: number;
  driverEmail: string;
  // vehicleId/vehicleRegistration usunięte - pojazd przypisywany do trasy
  workDays: string[];
  workStartTime: string;
  workEndTime: string;
  active: boolean;
}

export interface DriverScheduleRequest {
  driverId: number;
  // vehicleId usunięte - pojazd przypisywany do trasy
  workDays: string[];
  workStartTime: string;
  workEndTime: string;
  active: boolean;
}

export interface RouteResponse {
  id: number;
  driverId: number;
  driverEmail: string;
  vehicleId: number;
  vehicleRegistration: string;
  routeDate: string;
  orders: OrderResponse[];
  totalDistance: number;
  estimatedTimeMinutes: number;
  status: string;
  createdAt: string;
}

export interface PlanRouteRequest {
  routeDate: string;
  driverId: number;
  vehicleId: number;
  orderIds: number[];
}

@Injectable({
  providedIn: 'root'
})
export class DispatchService {
  private apiUrl = 'http://localhost:8080/api/dispatch';

  constructor(private http: HttpClient) { }

  // ========== ZLECENIA ==========

  getPendingOrders(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`${this.apiUrl}/orders/pending`);
  }

  getConfirmedOrders(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`${this.apiUrl}/orders/confirmed`);
  }

  getUnassignedOrders(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`${this.apiUrl}/orders/unassigned`);
  }

  confirmOrder(orderId: number): Observable<OrderResponse> {
    return this.http.put<OrderResponse>(`${this.apiUrl}/orders/${orderId}/confirm`, {});
  }

  assignOrder(orderId: number, driverId: number): Observable<OrderResponse> {
    return this.http.put<OrderResponse>(`${this.apiUrl}/orders/${orderId}/assign`, {
      orderId,
      driverId
    });
  }

  // ========== KIEROWCY ==========

  getAllDrivers(): Observable<DriverResponse[]> {
    return this.http.get<DriverResponse[]>(`${this.apiUrl}/drivers`);
  }

  // ========== GRAFIKI PRACY ==========

  getAllSchedules(): Observable<DriverScheduleResponse[]> {
    return this.http.get<DriverScheduleResponse[]>(`${this.apiUrl}/schedules`);
  }

  getActiveSchedules(): Observable<DriverScheduleResponse[]> {
    return this.http.get<DriverScheduleResponse[]>(`${this.apiUrl}/schedules/active`);
  }

  getScheduleById(id: number): Observable<DriverScheduleResponse> {
    return this.http.get<DriverScheduleResponse>(`${this.apiUrl}/schedules/${id}`);
  }

  createSchedule(request: DriverScheduleRequest): Observable<DriverScheduleResponse> {
    return this.http.post<DriverScheduleResponse>(`${this.apiUrl}/schedules`, request);
  }

  updateSchedule(id: number, request: DriverScheduleRequest): Observable<DriverScheduleResponse> {
    return this.http.put<DriverScheduleResponse>(`${this.apiUrl}/schedules/${id}`, request);
  }

  deleteSchedule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/schedules/${id}`);
  }

  // ========== POJAZDY ==========

  getAllVehicles(): Observable<VehicleResponse[]> {
    return this.http.get<VehicleResponse[]>(`${this.apiUrl}/vehicles`);
  }

  getAvailableVehicles(): Observable<VehicleResponse[]> {
    return this.http.get<VehicleResponse[]>(`${this.apiUrl}/vehicles/available`);
  }

  createVehicle(request: VehicleRequest): Observable<VehicleResponse> {
    return this.http.post<VehicleResponse>(`${this.apiUrl}/vehicles`, request);
  }

  updateVehicle(id: number, request: VehicleRequest): Observable<VehicleResponse> {
    return this.http.put<VehicleResponse>(`${this.apiUrl}/vehicles/${id}`, request);
  }

  deleteVehicle(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/vehicles/${id}`);
  }

  // ========== PLANOWANIE TRAS ==========

  // Pobierz kierowców dostępnych na dany dzień
  getAvailableDriversForDate(date: string): Observable<DriverResponse[]> {
    return this.http.get<DriverResponse[]>(`${this.apiUrl}/routes/available-drivers/${date}`);
  }

  // Pobierz pojazdy dostępne na dany dzień
  getAvailableVehiclesForDate(date: string): Observable<VehicleResponse[]> {
    return this.http.get<VehicleResponse[]>(`${this.apiUrl}/routes/available-vehicles/${date}`);
  }

  // Planuj trasę z ręcznym wyborem daty, kierowcy i pojazdu
  planRoute(request: PlanRouteRequest): Observable<RouteResponse> {
    return this.http.post<RouteResponse>(`${this.apiUrl}/routes/plan`, request);
  }

  getAllRoutes(): Observable<RouteResponse[]> {
    return this.http.get<RouteResponse[]>(`${this.apiUrl}/routes`);
  }

  getRoutesByDate(date: string): Observable<RouteResponse[]> {
    return this.http.get<RouteResponse[]>(`${this.apiUrl}/routes/date/${date}`);
  }

  getRouteById(id: number): Observable<RouteResponse> {
    return this.http.get<RouteResponse>(`${this.apiUrl}/routes/${id}`);
  }

  cancelRoute(id: number): Observable<RouteResponse> {
    return this.http.put<RouteResponse>(`${this.apiUrl}/routes/${id}/cancel`, {});
  }

  // ========== AUTO-PLANOWANIE ==========

  autoPlanRoutes(orderIds: number[], routeDate: string): Observable<RouteResponse[]> {
    return this.http.post<RouteResponse[]>(`${this.apiUrl}/routes/auto-plan`, {
      orderIds,
      routeDate
    });
  }

  getPendingRoutes(): Observable<AutoPlanResponse[]> {
    return this.http.get<AutoPlanResponse[]>(`${this.apiUrl}/routes/auto-plan/pending`);
  }

  getAwaitingRoutes(): Observable<AutoPlanResponse> {
    return this.http.get<AutoPlanResponse>(`${this.apiUrl}/routes/auto-plan/awaiting`);
  }

  getAutoPlanningEvents(): Observable<AutoPlanningEvent[]> {
    return this.http.get<AutoPlanningEvent[]>(`${this.apiUrl}/routes/auto-plan/events`);
  }

  acceptAutoPlannedRoutes(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/routes/auto-plan/${id}/accept`, {});
  }

  rejectAutoPlannedRoutes(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/routes/auto-plan/${id}/reject`, {});
  }

  consumeAutoPlannedRoutes(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/routes/auto-plan/${id}/consume`, {});
  }

  getAutoPlannedRoutes(date: string): Observable<AutoPlanResponse> {
    return this.http.get<AutoPlanResponse>(`${this.apiUrl}/routes/auto-plan/${date}`);
  }

}

