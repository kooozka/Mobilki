import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, LoginRequest } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  loginData: LoginRequest = {
    email: '',
    password: ''
  };

  errorMessage = '';
  successMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.login(this.loginData).subscribe({
      next: (response) => {
        this.successMessage = response.message;
        setTimeout(() => {
          this.router.navigate(['/home']);
        }, 500);
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Logowanie nie powiodło się. Spróbuj ponownie.';
      }
    });
  }
}
