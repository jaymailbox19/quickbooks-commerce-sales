import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  username = '';
  password = '';

  errorMessage = signal('');
  successMessage = signal('');

  constructor(private authService: AuthService,private router: Router) {}

  login(): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.successMessage.set('Login successful!');
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.errorMessage.set('Invalid username or password.');
      }
    });
  }
}