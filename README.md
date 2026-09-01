# StudyPlan AI — Backend (Phase 1)

## Phase 1 scope
- Project skeleton (Maven, Spring Boot 3.3.4, Java 17)
- MongoDB connection (local instance, db: `studyplan_ai`)
- `User` model + repository
- JWT generation & validation
- Spring Security config — stateless, CORS-enabled for the Vite frontend
- `POST /api/auth/signup` and `POST /api/auth/login`

## Prerequisites
- Java 17+
- Maven 3.9+ (or use the included `./mvnw` if you add the wrapper)
- MongoDB running locally on `mongodb://localhost:27017`

## Run
```bash
cd studyplan-ai
mvn spring-boot:run
```
Server starts on `http://localhost:8080`.

## Test it
```bash
# Signup
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Mulayam","email":"mulayam@test.com","password":"secret123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"mulayam@test.com","password":"secret123"}'
```
Both should return: `{ "token": "...", "id": "...", "name": "...", "email": "..." }`

## Connect to frontend
1. Backend must be running on port 8080 (frontend's default `VITE_API_BASE_URL`).
2. In the frontend folder, run `npm install && npm run dev` — it starts on
   `http://localhost:5173`, which is already whitelisted in
   `application.properties` under `app.cors.allowed-origins`.
3. Try signup/login from the actual UI — token gets stored in
   `localStorage` under `studyplan_token` and attached to every future
   request automatically (see `src/api/axios.js`).

## Package structure
```
com.studyplan.ai
├── config       -> SecurityConfig, MongoConfig
├── controller   -> AuthController (Phase 1); SubjectController etc. later
├── dto          -> request/response shapes, never expose models directly
├── exception    -> ApiException + GlobalExceptionHandler
├── model        -> MongoDB documents (User, Streak)
├── repository   -> Spring Data Mongo repositories
├── security     -> JwtUtil, JwtAuthFilter, CurrentUser helper
└── service      -> AuthService (business logic, separate from controller)
```

## Next: Phase 2
Subject & Topic models, Gemini API integration (topic ordering + weighting),
`POST /api/subjects` with DEADLINE/PACE schedule generation.
