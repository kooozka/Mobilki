import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, RegisterRequest } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent implements OnInit {
  registerData: RegisterRequest = {
    email: '',
    password: '',
    role: ''
  };

  roles: string[] = [];
  errorMessage = '';
  successMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.getRoles().subscribe({
      next: (roles) => {
        this.roles = roles;
      },
      error: (error) => {
        console.error('Error loading roles:', error);
      }
    });
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.register(this.registerData).subscribe({
      next: (response) => {
        this.successMessage = response.message;
        setTimeout(() => {
          this.router.navigate(['/home']);
        }, 500);
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Registration failed. Please try again.';
      }
    });
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
