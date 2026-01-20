import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export enum VehicleType {
  SMALL_VAN = "SMALL_VAN",
  SEMI_TRUCK = "SEMI_TRUCK",
  MEDIUM_TRUCK = "MEDIUM_TRUCK",
  LARGE_TRUCK = "LARGE_TRUCK"
}

export enum UserRole {
  CLIENT = "CLIENT",
  DISPATCH_MANAGER = "DISPATCH_MANAGER",
  DRIVER = "DRIVER",
  ADMIN = "ADMIN"
}

export interface UpdateUserRequest {
  email?: string;
  password?: string;
  suspended?: boolean;
  licenseTypes?: VehicleType[];
}

export interface CreateUserRequest {
  email: string;
  password: string;
  role: UserRole;
  licenseTypes?: VehicleType[];
}

export interface UserTO {
  id: number;
  email: string;
  role: UserRole;
  suspended: boolean;
  licenseTypes?: VehicleType[];
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = 'http://localhost:8080/api/admin';

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<UserTO[]> {
    return this.http.get<UserTO[]>(`${this.apiUrl}/users`);
  }

  getUserById(id: number): Observable<UserTO> {
    return this.http.get<UserTO>(`${this.apiUrl}/users/${id}`);
  }

  createUser(request: CreateUserRequest): Observable<UserTO> {
    return this.http.post<UserTO>(`${this.apiUrl}/users`, request);
  }

  updateUser(id: number, request: UpdateUserRequest): Observable<UserTO> {
    return this.http.put<UserTO>(`${this.apiUrl}/users/${id}`, request);
  }

  suspendUser(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/users/${id}/suspend`, {});
  }

  unsuspendUser(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/users/${id}/unsuspend`, {});
  }
}
