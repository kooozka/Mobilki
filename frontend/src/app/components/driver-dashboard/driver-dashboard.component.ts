import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { DriverApiService, DriverStatsResponse } from '../../services/driver-api.service';
import { OrderResponse } from '../../services/order.service';
import { RouteResponse, DriverScheduleResponse } from '../../services/dispatch.service';

@Component({
  selector: 'app-driver-dashboard',
  imports: [CommonModule, FormsModule],
  templateUrl: './driver-dashboard.component.html',
  styleUrl: './driver-dashboard.component.css'
})
export class DriverDashboardComponent implements OnInit {
  userEmail: string = '';
  activeTab: string = 'active';

  // Dane
  activeRoutes: RouteResponse[] = [];
  allRoutes: RouteResponse[] = [];
  activeOrders: OrderResponse[] = [];
  allOrders: OrderResponse[] = [];
  schedule: DriverScheduleResponse | null = null;
  stats: DriverStatsResponse | null = null;

  // Wybrany element do szczegółów
  selectedRoute: RouteResponse | null = null;
  showRouteDetails = false;

  // Komunikaty
  errorMessage = '';
  successMessage = '';
  loading = false;

  weekDaysMap: { [key: string]: string } = {
    'MONDAY': 'Poniedziałek',
    'TUESDAY': 'Wtorek',
    'WEDNESDAY': 'Środa',
    'THURSDAY': 'Czwartek',
    'FRIDAY': 'Piątek',
    'SATURDAY': 'Sobota',
    'SUNDAY': 'Niedziela'
  };

  constructor(
    private authService: AuthService,
    private driverApiService: DriverApiService,
    private router: Router
  ) {
    const user = this.authService.getCurrentUser();
    this.userEmail = user?.email || '';
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loadActiveRoutes();
    this.loadActiveOrders();
    this.loadSchedule();
    this.loadStats();
  }

  loadActiveRoutes(): void {
    this.driverApiService.getMyActiveRoutes().subscribe({
      next: (routes) => this.activeRoutes = routes,
      error: (err) => console.error('Error loading active routes:', err)
    });
  }

  loadAllRoutes(): void {
    this.driverApiService.getMyRoutes().subscribe({
      next: (routes) => this.allRoutes = routes,
      error: (err) => console.error('Error loading all routes:', err)
    });
  }

  loadActiveOrders(): void {
    this.driverApiService.getMyActiveOrders().subscribe({
      next: (orders) => this.activeOrders = orders,
      error: (err) => console.error('Error loading active orders:', err)
    });
  }

  loadAllOrders(): void {
    this.driverApiService.getMyOrders().subscribe({
      next: (orders) => this.allOrders = orders,
      error: (err) => console.error('Error loading all orders:', err)
    });
  }

  loadSchedule(): void {
    this.driverApiService.getMySchedule().subscribe({
      next: (schedule) => this.schedule = schedule,
      error: (err) => {
        if (err.status !== 400) {
          console.error('Error loading schedule:', err);
        }
      }
    });
  }

  loadStats(): void {
    this.driverApiService.getMyStats().subscribe({
      next: (stats) => this.stats = stats,
      error: (err) => console.error('Error loading stats:', err)
    });
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    this.clearMessages();

    if (tab === 'history' && this.allRoutes.length === 0) {
      this.loadAllRoutes();
      this.loadAllOrders();
    }
  }

  // ========== AKCJE NA TRASACH ==========

  startRoute(route: RouteResponse): void {
    if (!confirm(`Czy na pewno chcesz rozpocząć trasę #${route.id}?`)) return;

    this.loading = true;
    this.driverApiService.startRoute(route.id).subscribe({
      next: () => {
        this.successMessage = `Trasa #${route.id} została rozpoczęta`;
        this.loadActiveRoutes();
        this.loadActiveOrders();
        this.loading = false;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Błąd podczas rozpoczynania trasy';
        this.loading = false;
      }
    });
  }

  completeRoute(route: RouteResponse): void {
    if (!confirm(`Czy na pewno chcesz zakończyć trasę #${route.id}?`)) return;

    this.loading = true;
    this.driverApiService.completeRoute(route.id).subscribe({
      next: () => {
        this.successMessage = `Trasa #${route.id} została zakończona`;
        this.loadActiveRoutes();
        this.loadStats();
        this.loading = false;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Błąd podczas kończenia trasy';
        this.loading = false;
      }
    });
  }

  openRouteDetails(route: RouteResponse): void {
    this.selectedRoute = route;
    this.showRouteDetails = true;
  }

  closeRouteDetails(): void {
    this.selectedRoute = null;
    this.showRouteDetails = false;
  }

  // ========== AKCJE NA ZLECENIACH ==========

  pickupOrder(order: OrderResponse): void {
    if (!confirm(`Czy na pewno potwierdzasz odbiór zlecenia #${order.id}?`)) return;

    this.loading = true;
    this.driverApiService.pickupOrder(order.id).subscribe({
      next: () => {
        this.successMessage = `Zlecenie #${order.id} zostało odebrane`;
        this.loadActiveOrders();
        this.loading = false;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Błąd podczas odbierania zlecenia';
        this.loading = false;
      }
    });
  }

  deliverOrder(order: OrderResponse): void {
    if (!confirm(`Czy na pewno potwierdzasz dostarczenie zlecenia #${order.id}?`)) return;

    this.loading = true;
    this.driverApiService.deliverOrder(order.id).subscribe({
      next: () => {
        this.successMessage = `Zlecenie #${order.id} zostało dostarczone`;
        this.loadActiveOrders();
        this.loadActiveRoutes();
        this.loadStats();
        this.loading = false;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Błąd podczas dostarczania zlecenia';
        this.loading = false;
      }
    });
  }

  // ========== POMOCNICZE ==========

  getStatusDisplay(status: string): string {
    const map: { [key: string]: string } = {
      'PENDING': 'Oczekujące',
      'CONFIRMED': 'Potwierdzone',
      'ASSIGNED': 'Przypisane',
      'IN_PROGRESS': 'W realizacji',
      'COMPLETED': 'Zakończone',
      'CANCELLED': 'Anulowane'
    };
    return map[status] || status;
  }

  getRouteStatusDisplay(status: string): string {
    const map: { [key: string]: string } = {
      'PLANNED': 'Zaplanowana',
      'IN_PROGRESS': 'W realizacji',
      'COMPLETED': 'Zakończona',
      'CANCELLED': 'Anulowana'
    };
    return map[status] || status;
  }

  getVehicleTypeDisplay(type: string): string {
    const map: { [key: string]: string } = {
      'SMALL_VAN': 'Mały van',
      'MEDIUM_TRUCK': 'Średnia ciężarówka',
      'LARGE_TRUCK': 'Duża ciężarówka',
      'SEMI_TRUCK': 'Naczepa'
    };
    return map[type] || type;
  }

  formatTime(minutes: number): string {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hours}h ${mins}min`;
  }

  getDayLabel(day: string): string {
    return this.weekDaysMap[day] || day;
  }

  getWorkDaysShort(days: string[]): string {
    const dayShortMap: { [key: string]: string } = {
      'MONDAY': 'Pn',
      'TUESDAY': 'Wt',
      'WEDNESDAY': 'Śr',
      'THURSDAY': 'Cz',
      'FRIDAY': 'Pt',
      'SATURDAY': 'So',
      'SUNDAY': 'Nd'
    };
    return days.map(d => dayShortMap[d] || d).join(', ');
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
