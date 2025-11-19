import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-home',
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  user: any;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.user = this.authService.getUser();
    console.log('Home component - user:', this.user);
    this.redirectBasedOnRole();
  }

  redirectBasedOnRole(): void {
    if (!this.user) {
      this.router.navigate(['/login']);
      return;
    }

    console.log('Redirecting based on role:', this.user.role);

    switch (this.user.role) {
      case 'CLIENT':
        this.router.navigate(['/client/dashboard']);
        break;
      case 'DISPATCH_MANAGER':
        this.router.navigate(['/dispatch-manager/dashboard']);
        break;
      case 'DRIVER':
        this.router.navigate(['/driver/dashboard']);
        break;
      case 'ADMIN':
        this.router.navigate(['/admin/dashboard']);
        break;
      default:
        console.warn('Unknown role:', this.user.role);
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getRoleDisplayName(role: string): string {
    const roleMap: { [key: string]: string } = {
      'CLIENT': 'Klient',
      'DISPATCH_MANAGER': 'Kierownik Spedycji',
      'DRIVER': 'Kierowca',
      'ADMIN': 'Administrator Systemu'
    };
    return roleMap[role] || role;
  }
}