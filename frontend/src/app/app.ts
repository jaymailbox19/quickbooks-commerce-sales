import { Component } from '@angular/core';
import { Dashboard } from './dashboard/dashboard';
import { LoginComponent } from './login/login';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [Dashboard, LoginComponent, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}