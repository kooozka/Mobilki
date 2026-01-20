import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import {
  AdminService,
  UserTO,
  UserRole,
  CreateUserRequest,
  UpdateUserRequest,
  VehicleType
} from '../../services/admin.service';

@Component({
  selector: 'app-admin-dashboard',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {
  userEmail: string = '';

  users: UserTO[] = [];
  showUserDialog = false;
  editingUser: UserTO | null = null;
  userForm: any = {
    email: '',
    password: '',
    role: UserRole.CLIENT,
    licenseTypes: []
  };

  // Available vehicle types for license selection
  availableVehicleTypes: { name: string; displayName: string; maxWeight: number }[] = [
    { name: 'SMALL_VAN', displayName: 'Mały van', maxWeight: 1500 },
    { name: 'MEDIUM_TRUCK', displayName: 'Średnia ciężarówka', maxWeight: 5000 },
    { name: 'LARGE_TRUCK', displayName: 'Duża ciężarówka', maxWeight: 12000 },
    { name: 'SEMI_TRUCK', displayName: 'Naczepa', maxWeight: 25000 }
  ];

  vehicleTypeDisplayMap: { [key: string]: string } = {
    'SMALL_VAN': 'Mały van',
    'MEDIUM_TRUCK': 'Średnia ciężarówka',
    'LARGE_TRUCK': 'Duża ciężarówka',
    'SEMI_TRUCK': 'Naczepa'
  };

  userRoleDisplayMap: { [key: string]: string } = {
    'CLIENT': 'Klient',
    'DISPATCH_MANAGER': 'Kierownik Spedycji',
    'DRIVER': 'Kierowca',
    'ADMIN': 'Administrator Systemu'
  };

  selectedLicenseTypes: { [key: string]: boolean } = {};

  errorMessage = '';
  successMessage = '';

  userRoles = Object.values(UserRole);

  constructor(
    private authService: AuthService,
    private adminService: AdminService,
    private router: Router
  ) {
    const user = this.authService.getCurrentUser();
    this.userEmail = user?.email || '';
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.adminService.getAllUsers().subscribe({
      next: (users) => this.users = users,
      error: (err) => {
        console.error('Error loading users:', err);
        this.errorMessage = 'Błąd podczas ładowania użytkowników';
      }
    });
  }

  openCreateUserDialog(): void {
    this.editingUser = null;
    this.userForm = {
      email: '',
      password: '',
      role: UserRole.CLIENT,
      licenseTypes: []
    };
    this.selectedLicenseTypes = {};
    this.showUserDialog = true;
  }

  openEditUserDialog(user: UserTO): void {
    this.editingUser = user;
    this.userForm = {
      email: user.email,
      password: '', // Don't prefill password
      suspended: user.suspended
    };
    this.selectedLicenseTypes = {};
    if (user.licenseTypes) {
      user.licenseTypes.forEach(vt => {
        const displayName = this.vehicleTypeDisplayMap[vt] || vt;
        this.selectedLicenseTypes[displayName] = true;
      });
    }
    this.showUserDialog = true;
  }

  closeUserDialog(): void {
    this.showUserDialog = false;
    this.editingUser = null;
    this.errorMessage = '';
    this.successMessage = '';
  }

  saveUser(): void {
    this.errorMessage = '';
    this.successMessage = '';

    // Build license types from selected
    const licenseTypes: VehicleType[] = [];
    Object.keys(this.selectedLicenseTypes).forEach(key => {
      if (this.selectedLicenseTypes[key]) {
        const vt = this.availableVehicleTypes.find(v => v.displayName === key);
        if (vt) licenseTypes.push(vt.name as VehicleType);
      }
    });

    if (this.editingUser) {
      // Update
      const updateRequest: UpdateUserRequest = {
        email: this.userForm.email,
        password: this.userForm.password || undefined,
        suspended: this.userForm.suspended,
        licenseTypes: this.editingUser.role === UserRole.DRIVER ? licenseTypes : undefined
      };
      this.adminService.updateUser(this.editingUser.id, updateRequest).subscribe({
        next: () => {
          this.successMessage = 'Użytkownik zaktualizowany';
          this.loadUsers();
          this.closeUserDialog();
        },
        error: (err) => {
          console.error('Error updating user:', err);
          this.errorMessage = 'Błąd podczas aktualizacji użytkownika';
        }
      });
    } else {
      // Create
      const createRequest: CreateUserRequest = {
        email: this.userForm.email!,
        password: this.userForm.password!,
        role: this.userForm.role!,
        licenseTypes: licenseTypes
      };
      this.adminService.createUser(createRequest).subscribe({
        next: () => {
          this.successMessage = 'Użytkownik utworzony';
          this.loadUsers();
          this.closeUserDialog();
        },
        error: (err) => {
          console.error('Error creating user:', err);
          this.errorMessage = 'Błąd podczas tworzenia użytkownika';
        }
      });
    }
  }

  toggleSuspendUser(user: UserTO): void {
    if (user.suspended) {
      this.adminService.unsuspendUser(user.id).subscribe({
        next: () => {
          this.successMessage = 'Użytkownik odblokowany';
          this.loadUsers();
        },
        error: (err) => {
          console.error('Error unsuspending user:', err);
          this.errorMessage = 'Błąd podczas odblokowywania użytkownika';
        }
      });
    } else {
      this.adminService.suspendUser(user.id).subscribe({
        next: () => {
          this.successMessage = 'Użytkownik zablokowany';
          this.loadUsers();
        },
        error: (err) => {
          console.error('Error suspending user:', err);
          this.errorMessage = 'Błąd podczas blokowania użytkownika';
        }
      });
    }
  }

  getRoleDisplay(role: UserRole): string {
    return this.userRoleDisplayMap[role] || role;
  }

  getLicenseTypesDisplay(licenseTypes?: VehicleType[]): string {
    if (!licenseTypes || licenseTypes.length === 0) return 'Brak';
    return licenseTypes.map(vt => this.vehicleTypeDisplayMap[vt] || vt).join(', ');
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
