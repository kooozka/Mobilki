import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dispatch-manager-dashboard',
  imports: [CommonModule],
  templateUrl: './dispatch-manager-dashboard.component.html',
  styleUrl: './dispatch-manager-dashboard.component.css'
})
export class DispatchManagerDashboardComponent {
  userEmail: string = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    const user = this.authService.getCurrentUser();
    this.userEmail = user?.email || '';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
