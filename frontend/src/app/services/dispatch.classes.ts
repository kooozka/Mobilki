// TypeScript class for AutoPlanResponse
export class AutoPlanResponse {
  planningId: number;
  status: AutoPlanningStatus;
  planningDate: string; // LocalDate as ISO string
  routes: AutoPlanRoute[];

  constructor(planningId: number, status: AutoPlanningStatus, planningDate: string, routes: AutoPlanRoute[]) {
    this.planningId = planningId;
    this.status = status;
    this.planningDate = planningDate;
    this.routes = routes;
  }
}

// Enum for AutoPlanningStatus
export enum AutoPlanningStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED'
}

// TypeScript class for AutoPlanningEvent
export class AutoPlanningEvent {
  planningId: number;
  status: AutoPlanningStatus;
  planningDate: string; // LocalDate as ISO string

  constructor(planningId: number, status: AutoPlanningStatus, planningDate: string) {
    this.planningId = planningId;
    this.status = status;
    this.planningDate = planningDate;
  }
}

// TypeScript class for AutoPlanRoute
export class AutoPlanRoute {
  driverId: number;
  driverEmail: string;
  vehicleId: number;
  vehicleRegistration: string;
  routeDate: string; // LocalDate as ISO string
  orders: OrderResponse[];
  totalDistance: number;
  estimatedTimeMinutes: number;

  constructor(driverId: number, driverEmail: string,
              vehicleId: number, vehicleRegistration: string,
              routeDate: string, orders: OrderResponse[],
              totalDistance: number, estimatedTimeMinutes: number) {
    this.driverId = driverId;
    this.driverEmail = driverEmail;
    this.vehicleId = vehicleId;
    this.vehicleRegistration = vehicleRegistration;
    this.routeDate = routeDate;
    this.orders = orders;
    this.totalDistance = totalDistance;
    this.estimatedTimeMinutes = estimatedTimeMinutes;
  }
}

// Enum for RouteStatus (if needed elsewhere)
export enum RouteStatus {
  PLANNED = 'PLANNED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

// TypeScript class for OrderResponse
export class OrderResponse {
  id: number;
  title: string;
  clientEmail: string;
  driverEmail: string;
  pickupLocation: string;
  pickupAddress: string;
  pickupDate: string; // LocalDate as ISO string
  deliveryLocation: string;
  deliveryAddress: string;
  deliveryDeadline: string; // LocalDate as ISO string
  vehicleType: VehicleType;
  cargoWeight: number;
  description: string;
  price: number;
  status: OrderStatus;
  createdAt: string; // LocalDateTime as ISO string
  updatedAt: string; // LocalDateTime as ISO string
  confirmedAt: string; // LocalDateTime as ISO string
  cancelledAt: string; // LocalDateTime as ISO string
  cancellationReason: string;

  constructor(id: number, title: string, clientEmail: string, driverEmail: string, pickupLocation: string, pickupAddress: string, pickupDate: string, deliveryLocation: string, deliveryAddress: string, deliveryDeadline: string, vehicleType: VehicleType, cargoWeight: number, description: string, price: number, status: OrderStatus, createdAt: string, updatedAt: string, confirmedAt: string, cancelledAt: string, cancellationReason: string) {
    this.id = id;
    this.title = title;
    this.clientEmail = clientEmail;
    this.driverEmail = driverEmail;
    this.pickupLocation = pickupLocation;
    this.pickupAddress = pickupAddress;
    this.pickupDate = pickupDate;
    this.deliveryLocation = deliveryLocation;
    this.deliveryAddress = deliveryAddress;
    this.deliveryDeadline = deliveryDeadline;
    this.vehicleType = vehicleType;
    this.cargoWeight = cargoWeight;
    this.description = description;
    this.price = price;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.confirmedAt = confirmedAt;
    this.cancelledAt = cancelledAt;
    this.cancellationReason = cancellationReason;
  }
}

// Enum for OrderStatus
export enum OrderStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  ASSIGNED = 'ASSIGNED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

// Enum for VehicleType
export enum VehicleType {
  SMALL_VAN = 'SMALL_VAN',
  MEDIUM_TRUCK = 'MEDIUM_TRUCK',
  LARGE_TRUCK = 'LARGE_TRUCK',
  SEMI_TRUCK = 'SEMI_TRUCK'
}
