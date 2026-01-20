import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { HomeComponent } from './components/home/home.component';
import { ClientDashboardComponent } from './components/client-dashboard/client-dashboard.component';
import { CreateOrderComponent } from './components/create-order/create-order.component';
import { OrderHistoryComponent } from './components/order-history/order-history.component';
import { OrderDetailsComponent } from './components/order-details/order-details.component';
import { DispatchManagerDashboardComponent } from './components/dispatch-manager-dashboard/dispatch-manager-dashboard.component';
import { DriverDashboardComponent } from './components/driver-dashboard/driver-dashboard.component';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard.component';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'home', component: HomeComponent, canActivate: [authGuard] },
  {
    path: 'client/dashboard',
    component: ClientDashboardComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CLIENT'] }
  },
  {
    path: 'client/create-order',
    component: CreateOrderComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CLIENT'] }
  },
  {
    path: 'client/order-history',
    component: OrderHistoryComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CLIENT'] }
  },
  {
    path: 'client/order-details/:id',
    component: OrderDetailsComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CLIENT'] }
  },
  {
    path: 'dispatch-manager/dashboard',
    component: DispatchManagerDashboardComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['DISPATCH_MANAGER'] }
  },
  {
    path: 'driver/dashboard',
    component: DriverDashboardComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['DRIVER'] }
  },
  {
    path: 'admin/dashboard',
    component: AdminDashboardComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] }
  },
  { path: '**', redirectTo: '/login' }
];
