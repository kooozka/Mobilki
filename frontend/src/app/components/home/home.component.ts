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
