import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { DispatchService, DriverResponse, VehicleResponse, DriverScheduleResponse, RouteResponse, DriverScheduleRequest, VehicleRequest } from '../../services/dispatch.service';
import { OrderResponse } from '../../services/order.service';

@Component({
  selector: 'app-dispatch-manager-dashboard',
  imports: [CommonModule, FormsModule],
  templateUrl: './dispatch-manager-dashboard.component.html',
  styleUrl: './dispatch-manager-dashboard.component.css'
})
export class DispatchManagerDashboardComponent implements OnInit {
  userEmail: string = '';
  activeTab: string = 'orders';

  // Zlecenia
  // Zlecenia
  pendingOrders: OrderResponse[] = [];
  confirmedOrders: OrderResponse[] = [];
  filteredConfirmedOrders: OrderResponse[] = [];
  selectedOrders: Set<number> = new Set();
  pickupDateFilter: string = '';

  // Kierowcy
  drivers: DriverResponse[] = [];

  // Pojazdy
  vehicles: VehicleResponse[] = [];
  showVehicleDialog = false;
  editingVehicle: VehicleResponse | null = null;
  vehicleForm: VehicleRequest = {
    registrationNumber: '',
    brand: '',
    model: '',
    type: 'SMALL_VAN',
    maxWeight: 1000,
    available: true,
    notes: ''
  };

  // Grafiki
  schedules: DriverScheduleResponse[] = [];
  showScheduleDialog = false;
  editingSchedule: DriverScheduleResponse | null = null;
  scheduleForm: DriverScheduleRequest = {
    driverId: 0,
    // vehicleId usunięte - pojazd przypisywany do trasy
    workDays: [],
    workStartTime: '08:00',
    workEndTime: '16:00',
    active: true
  };
  selectedWorkDays: { [key: string]: boolean } = {
    'MONDAY': false,
    'TUESDAY': false,
    'WEDNESDAY': false,
    'THURSDAY': false,
    'FRIDAY': false,
    'SATURDAY': false,
    'SUNDAY': false
  };

  // Trasy
  routes: RouteResponse[] = [];
  plannedRoutes: RouteResponse[] = [];
  showRouteResult = false;
  showRoutePlanDialog = false;

  // Formularz planowania trasy
  routePlanForm = {
    routeDate: '',
    driverId: 0,
    vehicleId: 0
  };
  availableDriversForDate: DriverResponse[] = [];
  availableVehiclesForDate: VehicleResponse[] = [];

  // Komunikaty
  errorMessage = '';
  successMessage = '';

  vehicleTypes = ['SMALL_VAN', 'MEDIUM_TRUCK', 'LARGE_TRUCK', 'SEMI_TRUCK'];
  vehicleTypeMaxWeights: { [key: string]: number } = {
    'SMALL_VAN': 1500,
    'MEDIUM_TRUCK': 5000,
    'LARGE_TRUCK': 12000,
    'SEMI_TRUCK': 25000
  };
  weekDays = [
    { key: 'MONDAY', label: 'Poniedziałek' },
    { key: 'TUESDAY', label: 'Wtorek' },
    { key: 'WEDNESDAY', label: 'Środa' },
    { key: 'THURSDAY', label: 'Czwartek' },
    { key: 'FRIDAY', label: 'Piątek' },
    { key: 'SATURDAY', label: 'Sobota' },
    { key: 'SUNDAY', label: 'Niedziela' }
  ];

  constructor(
    private authService: AuthService,
    private dispatchService: DispatchService,
    private router: Router
  ) {
    const user = this.authService.getCurrentUser();
    this.userEmail = user?.email || '';
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loadOrders();
    this.loadDrivers();
    this.loadVehicles();
    this.loadSchedules();
    this.loadRoutes();
  }

  loadOrders(): void {
    this.dispatchService.getPendingOrders().subscribe({
      next: (orders) => this.pendingOrders = orders,
      error: (err) => console.error('Error loading pending orders:', err)
    });
    this.dispatchService.getConfirmedOrders().subscribe({
      next: (orders) => {
        this.confirmedOrders = orders;
        this.applyPickupDateFilter();
      },
      error: (err) => console.error('Error loading confirmed orders:', err)
    });
  }

  // ========== FILTROWANIE ZLECEŃ ==========

  applyPickupDateFilter(): void {
    if (!this.pickupDateFilter) {
      this.filteredConfirmedOrders = [...this.confirmedOrders];
    } else {
      this.filteredConfirmedOrders = this.confirmedOrders.filter(order => {
        // Porównaj daty (pickupDate może być w formacie "2025-12-08" lub "2025-12-08T00:00:00")
        const orderDate = order.pickupDate.split('T')[0];
        return orderDate === this.pickupDateFilter;
      });
    }
    // Wyczyść selekcję jeśli filtr się zmienił
    this.selectedOrders.clear();
  }

  clearPickupDateFilter(): void {
    this.pickupDateFilter = '';
    this.applyPickupDateFilter();
  }

  selectAllFiltered(): void {
    this.filteredConfirmedOrders.forEach(order => {
      this.selectedOrders.add(order.id);
    });
  }

  loadDrivers(): void {
    this.dispatchService.getAllDrivers().subscribe({
      next: (drivers) => this.drivers = drivers,
      error: (err) => console.error('Error loading drivers:', err)
    });
  }

  loadVehicles(): void {
    this.dispatchService.getAllVehicles().subscribe({
      next: (vehicles) => this.vehicles = vehicles,
      error: (err) => console.error('Error loading vehicles:', err)
    });
  }

  loadSchedules(): void {
    this.dispatchService.getAllSchedules().subscribe({
      next: (schedules) => this.schedules = schedules,
      error: (err) => console.error('Error loading schedules:', err)
    });
  }

  loadRoutes(): void {
    this.dispatchService.getAllRoutes().subscribe({
      next: (routes) => this.routes = routes,
      error: (err) => console.error('Error loading routes:', err)
    });
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    this.clearMessages();
  }

  // ========== ZLECENIA ==========

  confirmOrder(order: OrderResponse): void {
    this.dispatchService.confirmOrder(order.id).subscribe({
      next: () => {
        this.successMessage = 'Zlecenie zostało zatwierdzone';
        this.loadOrders();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Błąd podczas zatwierdzania zlecenia';
      }
    });
  }

  toggleOrderSelection(orderId: number): void {
    if (this.selectedOrders.has(orderId)) {
      this.selectedOrders.delete(orderId);
    } else {
      this.selectedOrders.add(orderId);
    }
  }

  isOrderSelected(orderId: number): boolean {
    return this.selectedOrders.has(orderId);
  }

  // ========== POJAZDY ==========

  openVehicleDialog(vehicle?: VehicleResponse): void {
    this.editingVehicle = vehicle || null;
    if (vehicle) {
      this.vehicleForm = {
        registrationNumber: vehicle.registrationNumber,
        brand: vehicle.brand,
        model: vehicle.model,
        type: vehicle.type,
        maxWeight: this.vehicleTypeMaxWeights[vehicle.type] || vehicle.maxWeight,
        available: vehicle.available,
        notes: vehicle.notes || ''
      };
    } else {
      this.vehicleForm = {
        registrationNumber: '',
        brand: '',
        model: '',
        type: 'SMALL_VAN',
        maxWeight: this.vehicleTypeMaxWeights['SMALL_VAN'],
        available: true,
        notes: ''
      };
    }
    this.showVehicleDialog = true;
  }

  closeVehicleDialog(): void {
    this.showVehicleDialog = false;
    this.editingVehicle = null;
  }

  saveVehicle(): void {
    if (this.editingVehicle) {
      this.dispatchService.updateVehicle(this.editingVehicle.id, this.vehicleForm).subscribe({
        next: () => {
          this.successMessage = 'Pojazd został zaktualizowany';
          this.closeVehicleDialog();
          this.loadVehicles();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Błąd podczas aktualizacji pojazdu';
        }
      });
    } else {
      this.dispatchService.createVehicle(this.vehicleForm).subscribe({
        next: () => {
          this.successMessage = 'Pojazd został dodany';
          this.closeVehicleDialog();
          this.loadVehicles();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Błąd podczas dodawania pojazdu';
        }
      });
    }
  }

  deleteVehicle(vehicle: VehicleResponse): void {
    if (confirm(`Czy na pewno chcesz usunąć pojazd ${vehicle.registrationNumber}?`)) {
      this.dispatchService.deleteVehicle(vehicle.id).subscribe({
        next: () => {
          this.successMessage = 'Pojazd został usunięty';
          this.loadVehicles();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Błąd podczas usuwania pojazdu';
        }
      });
    }
  }

  // ========== GRAFIKI PRACY ==========

  openScheduleDialog(schedule?: DriverScheduleResponse): void {
    this.editingSchedule = schedule || null;

    // Reset dni pracy
    Object.keys(this.selectedWorkDays).forEach(key => {
      this.selectedWorkDays[key] = false;
    });

    if (schedule) {
      this.scheduleForm = {
        driverId: schedule.driverId,
        workDays: schedule.workDays,
        workStartTime: schedule.workStartTime,
        workEndTime: schedule.workEndTime,
        active: schedule.active
      };
      schedule.workDays.forEach(day => {
        this.selectedWorkDays[day] = true;
      });
    } else {
      this.scheduleForm = {
        driverId: this.drivers.length > 0 ? this.drivers[0].id : 0,
        workDays: [],
        workStartTime: '08:00',
        workEndTime: '16:00',
        active: true
      };
    }
    this.showScheduleDialog = true;
  }

  closeScheduleDialog(): void {
    this.showScheduleDialog = false;
    this.editingSchedule = null;
  }

  saveSchedule(): void {
    // Zbierz wybrane dni pracy
    this.scheduleForm.workDays = Object.keys(this.selectedWorkDays)
      .filter(key => this.selectedWorkDays[key]);

    if (this.editingSchedule) {
      this.dispatchService.updateSchedule(this.editingSchedule.id, this.scheduleForm).subscribe({
        next: () => {
          this.successMessage = 'Grafik został zaktualizowany';
          this.closeScheduleDialog();
          this.loadSchedules();
          this.loadDrivers();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Błąd podczas aktualizacji grafiku';
        }
      });
    } else {
      this.dispatchService.createSchedule(this.scheduleForm).subscribe({
        next: () => {
          this.successMessage = 'Grafik został utworzony';
          this.closeScheduleDialog();
          this.loadSchedules();
          this.loadDrivers();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Błąd podczas tworzenia grafiku';
        }
      });
    }
  }

  deleteSchedule(schedule: DriverScheduleResponse): void {
    if (confirm(`Czy na pewno chcesz usunąć grafik dla ${schedule.driverEmail}?`)) {
      this.dispatchService.deleteSchedule(schedule.id).subscribe({
        next: () => {
          this.successMessage = 'Grafik został usunięty';
          this.loadSchedules();
          this.loadDrivers();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Błąd podczas usuwania grafiku';
        }
      });
    }
  }

  getDayLabels(days: string[]): string {
    const dayMap: { [key: string]: string } = {
      'MONDAY': 'Pn',
      'TUESDAY': 'Wt',
      'WEDNESDAY': 'Śr',
      'THURSDAY': 'Cz',
      'FRIDAY': 'Pt',
      'SATURDAY': 'So',
      'SUNDAY': 'Nd'
    };
    return days.map(d => dayMap[d] || d).join(', ');
  }

  // ========== PLANOWANIE TRAS ==========

  openRoutePlanDialog(): void {
    if (this.selectedOrders.size === 0) {
      this.errorMessage = 'Wybierz przynajmniej jedno zlecenie do zaplanowania';
      return;
    }

    // Ustaw domyślną datę na jutro
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    this.routePlanForm = {
      routeDate: tomorrow.toISOString().split('T')[0],
      driverId: 0,
      vehicleId: 0
    };
    this.availableDriversForDate = [];
    this.availableVehiclesForDate = [];
    this.showRoutePlanDialog = true;

    // Załaduj dostępnych dla daty
    this.loadAvailableResourcesForDate();
  }

  closeRoutePlanDialog(): void {
    this.showRoutePlanDialog = false;
  }

  onRouteDateChange(): void {
    // Resetuj wybór kierowcy i pojazdu
    this.routePlanForm.driverId = 0;
    this.routePlanForm.vehicleId = 0;
    // Załaduj dostępnych dla nowej daty
    this.loadAvailableResourcesForDate();
  }

  loadAvailableResourcesForDate(): void {
    if (!this.routePlanForm.routeDate) return;

    this.dispatchService.getAvailableDriversForDate(this.routePlanForm.routeDate).subscribe({
      next: (drivers: DriverResponse[]) => {
        this.availableDriversForDate = drivers;
        if (drivers.length > 0) {
          this.routePlanForm.driverId = drivers[0].id;
        }
      },
      error: (err: any) => console.error('Error loading available drivers:', err)
    });

    this.dispatchService.getAvailableVehiclesForDate(this.routePlanForm.routeDate).subscribe({
      next: (vehicles: VehicleResponse[]) => {
        this.availableVehiclesForDate = vehicles;
        if (vehicles.length > 0) {
          this.routePlanForm.vehicleId = vehicles[0].id;
        }
      },
      error: (err: any) => console.error('Error loading available vehicles:', err)
    });
  }

  planRoute(): void {
    if (!this.routePlanForm.routeDate) {
      this.errorMessage = 'Wybierz datę trasy';
      return;
    }
    if (!this.routePlanForm.driverId) {
      this.errorMessage = 'Wybierz kierowcę';
      return;
    }
    if (!this.routePlanForm.vehicleId) {
      this.errorMessage = 'Wybierz pojazd';
      return;
    }

    const request = {
      routeDate: this.routePlanForm.routeDate,
      driverId: this.routePlanForm.driverId,
      vehicleId: this.routePlanForm.vehicleId,
      orderIds: Array.from(this.selectedOrders)
    };

    this.dispatchService.planRoute(request).subscribe({
      next: (route: RouteResponse) => {
        this.plannedRoutes = [route];
        this.showRoutePlanDialog = false;
        this.showRouteResult = true;
        this.successMessage = `Zaplanowano trasę #${route.id} na ${route.routeDate}`;
        this.selectedOrders.clear();
        this.loadOrders();
        this.loadRoutes();
        setTimeout(() => this.successMessage = '', 5000);
      },
      error: (err: any) => {
        this.errorMessage = err.error?.message || 'Błąd podczas planowania trasy';
      }
    });
  }

  closeRouteResult(): void {
    this.showRouteResult = false;
    this.plannedRoutes = [];
  }

  cancelRoute(route: RouteResponse): void {
    if (confirm(`Czy na pewno chcesz anulować trasę #${route.id}? Zlecenia zostaną przywrócone do planowania.`)) {
      this.dispatchService.cancelRoute(route.id).subscribe({
        next: () => {
          this.successMessage = `Trasa #${route.id} została anulowana. Zlecenia przywrócono do planowania.`;
          this.loadRoutes();
          this.loadOrders();
          setTimeout(() => this.successMessage = '', 5000);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Błąd podczas anulowania trasy';
        }
      });
    }
  }

  formatTime(minutes: number): string {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hours}h ${mins}min`;
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

  getVehicleTypeDisplay(type: string): string {
    const map: { [key: string]: string } = {
      'SMALL_VAN': 'Mały van',
      'MEDIUM_TRUCK': 'Średnia ciężarówka',
      'LARGE_TRUCK': 'Duża ciężarówka',
      'SEMI_TRUCK': 'Naczepa'
    };
    return map[type] || type;
  }

  getVehicleTypeMaxWeight(type: string): number {
    return this.vehicleTypeMaxWeights[type] || 1500;
  }

  onVehicleTypeChange(): void {
    // Automatycznie ustaw maxWeight na podstawie typu
    this.vehicleForm.maxWeight = this.getVehicleTypeMaxWeight(this.vehicleForm.type);
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

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
