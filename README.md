# Aplikacja z rejestracją i logowaniem użytkowników

Podstawowa aplikacja full-stack z Spring Boot (backend) i Angular (frontend), umożliwiająca rejestrację i logowanie użytkowników z wykorzystaniem JWT i Spring Security.

## Technologie

### Backend
- Spring Boot 3.5.7
- Spring Security
- JWT (JSON Web Tokens)
- H2 Database (in-memory)
- JPA/Hibernate
- Lombok

### Frontend
- Angular 19
- TypeScript
- RxJS
- FormsModule

## Struktura projektu

```
.
├── backend/          # Aplikacja Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/example/demo/
│   │   │   │       ├── config/          # Konfiguracja Security
│   │   │   │       ├── controller/      # REST API endpoints
│   │   │   │       ├── dto/             # Data Transfer Objects
│   │   │   │       ├── model/           # Encje JPA
│   │   │   │       ├── repository/      # Repozytoria JPA
│   │   │   │       ├── security/        # JWT i UserDetailsService
│   │   │   │       └── service/         # Logika biznesowa
│   │   │   └── resources/
│   │   │       └── application.properties
│   └── pom.xml
│
└── frontend/        # Aplikacja Angular
    ├── src/
    │   ├── app/
    │   │   ├── components/     # Komponenty UI
    │   │   ├── guards/         # Route guards
    │   │   ├── interceptors/   # HTTP interceptors
    │   │   └── services/       # Serwisy Angular
    └── package.json
```

## Jak uruchomić aplikację

### Backend (Spring Boot)

1. Przejdź do folderu backend:
```powershell
cd backend
```

2. Uruchom aplikację:
```powershell
./mvnw spring-boot:run
```

Lub na Windows:
```powershell
.\mvnw.cmd spring-boot:run
```

Backend będzie dostępny pod adresem: `http://localhost:8080`

#### Dostęp do konsoli H2
Możesz przeglądać bazę danych H2 pod adresem: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (puste)

### Frontend (Angular)

1. Przejdź do folderu frontend:
```powershell
cd frontend
```

2. Zainstaluj zależności (tylko za pierwszym razem):
```powershell
npm install
```

3. Uruchom aplikację:
```powershell
npm start
```

Lub:
```powershell
ng serve
```

Frontend będzie dostępny pod adresem: `http://localhost:4200`

## API Endpoints

### Autentykacja

#### Rejestracja
```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123"
}
```

#### Logowanie
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

Odpowiedź zawiera token JWT:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "testuser",
  "email": "test@example.com",
  "message": "Login successful"
}
```

## Funkcjonalności

### Backend
- ✅ Rejestracja użytkowników z walidacją
- ✅ Logowanie użytkowników
- ✅ Generowanie tokenów JWT
- ✅ Zabezpieczenie endpointów z wykorzystaniem Spring Security
- ✅ Haszowanie haseł (BCrypt)
- ✅ Walidacja unikalności username i email
- ✅ CORS skonfigurowany dla frontnendu

### Frontend
- ✅ Formularz logowania
- ✅ Formularz rejestracji
- ✅ Chroniona strona główna (Home)
- ✅ Automatyczne dodawanie tokena JWT do requestów (HTTP Interceptor)
- ✅ Route Guard chroniący dostęp do stron dla zalogowanych
- ✅ Wylogowanie użytkownika
- ✅ Responsywny design

## Konfiguracja

### Backend - application.properties
```properties
# Port serwera
server.port=8080

# H2 Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true

# JWT
jwt.secret=mySecretKeyForJWTTokenGenerationAndValidation123456789
jwt.expiration=86400000  # 24 godziny w milisekundach
```

### Frontend - API URL
Adres backend API jest skonfigurowany w `auth.service.ts`:
```typescript
private apiUrl = 'http://localhost:8080/api/auth';
```

## Bezpieczeństwo

- Hasła są haszowane przy użyciu BCrypt
- JWT token ważny przez 24 godziny
- CORS skonfigurowany tylko dla localhost:4200
- Chronione endpointy wymagają ważnego tokena JWT
- Walidacja danych wejściowych na poziomie backendu

## Dalszy rozwój

Możliwe ulepszenia:
- Dodanie ról użytkowników (ADMIN, USER)
- Odświeżanie tokenów (refresh tokens)
- Resetowanie hasła
- Potwierdzenie email
- Profil użytkownika
- Zmiana hasła
- Przejście na prawdziwą bazę danych (PostgreSQL, MySQL)
- Testy jednostkowe i integracyjne
- Docker containerization

## Licencja

Ten projekt jest stworzony do celów edukacyjnych.
