# TURBO - API Contracts: Module 1 (Auth & Security)

> This document is a reference for the frontend and backend teams.
> It describes the happy paths and unhappy paths for each endpoint, with exact JSON examples for requests and responses.

---

## Table of Contents

- [Endpoints Summary](#endpoints-summary)
- [Input Validation Patterns](#input-validation-patterns)
- [Error Formats](#error-formats)
- [Error Code Catalog](#error-code-catalog)
- [POST /api/auth/register](#post-apiauthregister)
- [POST /api/auth/verify-otp](#post-apiauthverify-otp)
- [POST /api/auth/resend-otp](#post-apiauthresend-otp)
- [POST /api/auth/login](#post-apiauthlogin)

---

## Endpoints Summary

| Method | Endpoint | Request DTO | Success Response DTO | Description |
|--------|----------|-------------|---------------------|-------------|
| POST | `/api/auth/register` | `RegisterRequest` | `RegisterResponse` | Register a new user and send an OTP by email |
| POST | `/api/auth/verify-otp` | `VerifyOtpRequest` | `AuthResponse` | Verify the OTP code and return a JWT token |
| POST | `/api/auth/resend-otp` | `ResendOtpRequest` | `MessageResponse` | Send a new OTP code to the user's email |
| POST | `/api/auth/login` | `LoginRequest` | `AuthResponse` | Log in with email and password, return a JWT token |

---

## Input Validation Patterns

Both the **frontend** and the **backend** must validate the input fields. The frontend validates first to give fast feedback to the user. The backend validates again for security (never trust the client).

### Why validate on both sides?

```
User types input
      |
      v
[FRONTEND validates]  -->  Shows error immediately (good UX, no waiting)
      |
      v
[Sends request to API]
      |
      v
[BACKEND validates]   -->  Returns VALIDATION-001 if something is wrong (security)
```

> If someone uses Postman, curl, or any tool to skip the frontend, the backend still catches bad input.

### Field Validation Rules

#### `email`

| Property | Value |
|----------|-------|
| Required | Yes |
| Type | String |
| Regex | `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$` |
| Max length | 255 characters |
| Example valid | `john.doe@example.com` |
| Example invalid | `john@`, `@example.com`, `john doe@mail.com`, `""` |
| Backend annotation | `@NotBlank` + `@Email` |
| Error message | `"Email is required"` / `"Invalid email format"` |

**Frontend tip**: Use `<input type="email">` in HTML for basic browser validation, and also check with the regex before sending the request.

#### `password`

| Property | Value |
|----------|-------|
| Required | Yes |
| Type | String |
| Min length | 6 characters |
| Max length | 100 characters (recommended) |
| Regex | `^.{6,100}$` |
| Example valid | `secure123`, `MyP@ssw0rd!` |
| Example invalid | `123`, `ab`, `""` |
| Backend annotation | `@NotBlank` + `@Size(min = 6)` |
| Error message | `"Password is required"` / `"Password must be at least 6 characters"` |

**Frontend tip**: Show a character counter or a message like "Minimum 6 characters" below the input field.

#### `firstName`

| Property | Value |
|----------|-------|
| Required | Yes |
| Type | String |
| Min length | 1 character |
| Max length | 50 characters (recommended) |
| Regex | `^[a-zA-ZÀ-ÿ' -]{1,50}$` |
| Allowed chars | Letters, accents, apostrophe, hyphen, space |
| Example valid | `John`, `María`, `Jean-Pierre`, `O'Brien` |
| Example invalid | `""`, `John123`, `<script>` |
| Backend annotation | `@NotBlank` |
| Error message | `"First name is required"` |

#### `lastName`

| Property | Value |
|----------|-------|
| Required | Yes |
| Type | String |
| Min length | 1 character |
| Max length | 50 characters (recommended) |
| Regex | `^[a-zA-ZÀ-ÿ' -]{1,50}$` |
| Allowed chars | Letters, accents, apostrophe, hyphen, space |
| Example valid | `Doe`, `García`, `O'Connor` |
| Example invalid | `""`, `Doe123` |
| Backend annotation | `@NotBlank` |
| Error message | `"Last name is required"` |

#### `dateOfBirth`

| Property | Value |
|----------|-------|
| Required | Yes |
| Type | LocalDate (ISO-8601 format) |
| Format | `YYYY-MM-DD` |
| Rule | Must be a date in the past |
| Example valid | `1995-06-15`, `2000-01-01` |
| Example invalid | `""`, `2030-01-01`, `not-a-date` |
| Backend annotation | `@NotNull` + `@Past` |
| Error message | `"Date of birth is required"` / `"Date of birth must be in the past"` |

**Frontend tip**: Use `<input type="date">` with a `max` attribute set to today's date to prevent future dates.

#### `phoneNumber`

| Property | Value |
|----------|-------|
| Required | No (optional) |
| Type | String |
| Regex | `^\+?[0-9\s()-]{7,20}$` |
| Allowed chars | Digits, spaces, `+`, `-`, `(`, `)` |
| Example valid | `+1 604 555 0001`, `6045550001`, `(604) 555-0001` |
| Example invalid | `phone`, `abc123`, `+1` |
| Backend annotation | None (optional field) |
| Error message | `"Invalid phone number format"` (frontend only) |

**Frontend tip**: If the user types something, validate the format. If the field is empty, that is OK.

#### `role`

| Property | Value |
|----------|-------|
| Required | Yes |
| Type | String |
| Valid values | `DRIVER`, `CAR_OWNER` (case-insensitive: `driver`, `car_owner`, `Driver` also work) |
| Regex | `^(?i)(DRIVER|CAR_OWNER)$` |
| Example valid | `DRIVER`, `CAR_OWNER`, `driver`, `Car_Owner` |
| Example invalid | `""`, `ADMIN`, `BLAH`, `passenger` |
| Backend annotation | `@NotBlank` + business logic validates the value |
| Error message | `"Role is required (DRIVER or CAR_OWNER)"` / `"Invalid role. Must be DRIVER or CAR_OWNER"` |

**Frontend tip**: Use a dropdown or radio buttons instead of a text input. This avoids invalid roles completely.

#### `otp`

| Property | Value |
|----------|-------|
| Required | Yes |
| Type | String |
| Length | Exactly 6 digits |
| Regex | `^\d{6}$` |
| Example valid | `123456`, `000001`, `999999` |
| Example invalid | `""`, `12345`, `1234567`, `abcdef`, `12 34 56` |
| Backend annotation | `@NotBlank` |
| Error message | `"OTP code is required"` |

**Frontend tip**: Use `<input maxlength="6" pattern="[0-9]{6}">` and only allow numbers. You can split into 6 individual boxes for better UX.

### Validation Summary Table

This table shows all fields, the regex, and where the validation happens:

| Field | Regex Pattern | Frontend | Backend | Required |
|-------|--------------|----------|---------|----------|
| `email` | `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$` | Yes | Yes (`@Email` + `@NotBlank`) | Yes |
| `password` | `^.{6,100}$` | Yes | Yes (`@Size(min=6)` + `@NotBlank`) | Yes |
| `firstName` | `^[a-zA-ZÀ-ÿ' -]{1,50}$` | Yes | Yes (`@NotBlank`) | Yes |
| `lastName` | `^[a-zA-ZÀ-ÿ' -]{1,50}$` | Yes | Yes (`@NotBlank`) | Yes |
| `dateOfBirth` | `YYYY-MM-DD` (ISO-8601, must be past) | Yes | Yes (`@NotNull` + `@Past`) | Yes |
| `phoneNumber` | `^\+?[0-9\s()-]{7,20}$` | Yes (if filled) | No (optional) | No |
| `role` | `^(?i)(DRIVER\|CAR_OWNER)$` | Yes (use dropdown) | Yes (`@NotBlank` + business logic) | Yes |
| `otp` | `^\d{6}$` | Yes | Yes (`@NotBlank`) | Yes |

### Frontend Validation Example (JavaScript)

```javascript
const patterns = {
  email:     /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
  password:  /^.{6,100}$/,
  firstName: /^[a-zA-ZÀ-ÿ' -]{1,50}$/,
  lastName:  /^[a-zA-ZÀ-ÿ' -]{1,50}$/,
  phone:     /^\+?[0-9\s()-]{7,20}$/,
  role:      /^(DRIVER|CAR_OWNER)$/i,
  otp:       /^\d{6}$/
};

function validate(field, value) {
  if (!value || value.trim() === '') return `${field} is required`;
  if (!patterns[field].test(value)) return `Invalid ${field} format`;
  return null; // no error
}
```

---

## Error Formats

There are three types of error responses. Each one has a different structure.

### Business Error (`ErrorResponse`)

This happens when there is a logic error. For example: the email already exists, the user is not found, etc.

```json
{
  "code": "AUTH-001",
  "message": "Email already registered",
  "status": 409,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

### Validation Error (`ValidationErrorResponse`)

This happens when the request fields do not pass the validations. For example: empty fields, bad email format, short password, etc.

```json
{
  "code": "VALIDATION-001",
  "error": "Validation Failed",
  "status": 400,
  "timestamp": "2026-03-12T11:30:00.123",
  "details": {
    "email": "must not be blank",
    "password": "size must be at least 6 characters"
  }
}
```

### System Error

This happens when there is an unexpected error on the server. For example: null pointer, database connection lost, etc.

```json
{
  "code": "SYS-001",
  "message": "An unexpected error occurred",
  "status": 500,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

---

## Error Code Catalog

### Auth Errors

| Code | Message | HTTP Status | When does it happen? |
|------|---------|-------------|----------------------|
| `AUTH-001` | Email already registered | `409 Conflict` | Register: the email already exists in the database |
| `AUTH-002` | User not found | `404 Not Found` | Verify OTP / Resend OTP / Login: the email does not exist in the database |
| `AUTH-003` | Email is already verified | `409 Conflict` | Verify OTP / Resend OTP: the email was already verified before |
| `AUTH-004` | Invalid verification code | `400 Bad Request` | Verify OTP: the OTP code is wrong or the token is null |
| `AUTH-005` | Verification code has expired. Please request a new one. | `400 Bad Request` | Verify OTP: the OTP code expired (more than 10 minutes old) |
| `AUTH-006` | Please verify your email before logging in. | `403 Forbidden` | Login: the user did not verify their email yet |
| `AUTH-007` | Invalid role. Must be DRIVER or CAR_OWNER | `400 Bad Request` | Register: the role is not DRIVER or CAR_OWNER |
| `AUTH-008` | User must be at least 19 years old | `400 Bad Request` | Register: the user's date of birth indicates they are under 19 |

### Email Errors

| Code | Message | HTTP Status | When does it happen? |
|------|---------|-------------|----------------------|
| `EMAIL-001` | Failed to send verification email | `500 Internal Server Error` | Register / Resend OTP: the SMTP email server failed |

### Validation Errors

| Code | Message | HTTP Status | When does it happen? |
|------|---------|-------------|----------------------|
| `VALIDATION-001` | Validation Failed | `400 Bad Request` | Any endpoint: required fields are empty, bad format, etc. |

### System Errors

| Code | Message | HTTP Status | When does it happen? |
|------|---------|-------------|----------------------|
| `SYS-001` | An unexpected error occurred | `500 Internal Server Error` | Any unexpected server error |

---

## POST /api/auth/register

This endpoint registers a new user (DRIVER or CAR_OWNER). It sends a 6-digit OTP code to the user's email.

### Request

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "secure123",
  "phoneNumber": "+1 604 555 0001",
  "dateOfBirth": "1995-06-15",
  "role": "DRIVER"
}
```

| Field | Type | Required | Pattern | Validations |
|-------|------|----------|---------|-------------|
| `firstName` | String | Yes | `^[a-zA-ZÀ-ÿ' -]{1,50}$` | Cannot be empty, only letters |
| `lastName` | String | Yes | `^[a-zA-ZÀ-ÿ' -]{1,50}$` | Cannot be empty, only letters |
| `email` | String | Yes | `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$` | Cannot be empty, must be a valid email |
| `password` | String | Yes | `^.{6,100}$` | Cannot be empty, minimum 6 characters |
| `phoneNumber` | String | No | `^\+?[0-9\s()-]{7,20}$` | Optional, digits and phone chars only |
| `dateOfBirth` | LocalDate | Yes | `YYYY-MM-DD` | Cannot be null, must be a date in the past |
| `role` | String | Yes | `^(?i)(DRIVER\|CAR_OWNER)$` | Cannot be empty, must be `DRIVER` or `CAR_OWNER` (not case-sensitive) |

### Happy Path

- **Condition**: All fields are valid, the email is not registered, the role is valid
- **Status**: `200 OK`
- **Response**:

```json
{
  "message": "Registration successful. Please check your email for the verification code.",
  "email": "john.doe@example.com",
  "requiresVerification": true
}
```

- **Side effect**: The server sends an email with a 6-digit OTP code. This code expires in 10 minutes.

### Unhappy Paths

#### Email already registered

- **Condition**: The email already exists in the database
- **Status**: `409 Conflict`
- **Response**:

```json
{
  "code": "AUTH-001",
  "message": "Email already registered",
  "status": 409,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

#### Invalid role

- **Condition**: The `role` field is not `DRIVER` or `CAR_OWNER`. For example: `"BLAH"`, `"ADMIN"`, or `""`
- **Status**: `400 Bad Request`
- **Response**:

```json
{
  "code": "AUTH-007",
  "message": "Invalid role. Must be DRIVER or CAR_OWNER",
  "status": 400,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

#### Empty or invalid fields

- **Condition**: A required field is empty, the email format is bad, or the password has less than 6 characters
- **Status**: `400 Bad Request`
- **Response** (example: empty email and short password):

```json
{
  "code": "VALIDATION-001",
  "error": "Validation Failed",
  "status": 400,
  "timestamp": "2026-03-12T11:30:00.123",
  "details": {
    "email": "must not be blank",
    "password": "size must be at least 6 characters"
  }
}
```

#### Email sending failed

- **Condition**: The SMTP server failed when trying to send the OTP email
- **Status**: `500 Internal Server Error`
- **Response**:

```json
{
  "code": "EMAIL-001",
  "message": "Failed to send verification email",
  "status": 500,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

---

## POST /api/auth/verify-otp

This endpoint verifies the OTP code that was sent to the user's email. If the code is correct, it marks the email as verified and returns a JWT token.

### Request

```json
{
  "email": "john.doe@example.com",
  "otp": "123456"
}
```

| Field | Type | Required | Pattern | Validations |
|-------|------|----------|---------|-------------|
| `email` | String | Yes | `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$` | Cannot be empty, must be a valid email |
| `otp` | String | Yes | `^\d{6}$` | Cannot be empty, exactly 6 digits |

### Happy Path

- **Condition**: The email exists, it is not verified yet, the OTP code is correct and not expired
- **Status**: `200 OK`
- **Response**:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInJvbGUiOiJEUklWRVIiLCJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSJ9.xxx",
  "userId": 1,
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "DRIVER"
}
```

- **Side effect**: The field `emailVerified` is set to `true`. The fields `verificationToken` and `tokenExpiresAt` are cleared.

### Unhappy Paths

#### User not found

- **Condition**: The email does not exist in the database
- **Status**: `404 Not Found`
- **Response**:

```json
{
  "code": "AUTH-002",
  "message": "User not found",
  "status": 404,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

#### Email already verified

- **Condition**: The user already has `emailVerified = true`
- **Status**: `409 Conflict`
- **Response**:

```json
{
  "code": "AUTH-003",
  "message": "Email is already verified",
  "status": 409,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

#### Wrong OTP code

- **Condition**: The OTP code does not match the one stored in the database, or the token is null
- **Status**: `400 Bad Request`
- **Response**:

```json
{
  "code": "AUTH-004",
  "message": "Invalid verification code",
  "status": 400,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

#### Expired OTP code

- **Condition**: The OTP code was created more than 10 minutes ago
- **Status**: `400 Bad Request`
- **Response**:

```json
{
  "code": "AUTH-005",
  "message": "Verification code has expired. Please request a new one.",
  "status": 400,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

#### Empty or invalid fields

- **Condition**: The email or OTP field is empty
- **Status**: `400 Bad Request`
- **Response**:

```json
{
  "code": "VALIDATION-001",
  "error": "Validation Failed",
  "status": 400,
  "timestamp": "2026-03-12T11:30:00.123",
  "details": {
    "otp": "OTP code is required"
  }
}
```

---

## POST /api/auth/resend-otp

This endpoint creates a new OTP code and sends it to the user's email. The old code is no longer valid.

### Request

```json
{
  "email": "john.doe@example.com"
}
```

| Field | Type | Required | Pattern | Validations |
|-------|------|----------|---------|-------------|
| `email` | String | Yes | `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$` | Cannot be empty, must be a valid email |

### Happy Path

- **Condition**: The email exists in the database and it is not verified yet
- **Status**: `200 OK`
- **Response**:

```json
{
  "message": "A new verification code has been sent to your email."
}
```

- **Side effect**: A new 6-digit OTP is created. The fields `verificationToken` and `tokenExpiresAt` are updated. An email is sent with the new code.

### Unhappy Paths

#### User not found

- **Condition**: The email does not exist in the database
- **Status**: `404 Not Found`
- **Response**:

```json
{
  "code": "AUTH-002",
  "message": "User not found",
  "status": 404,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

#### Email already verified

- **Condition**: The user already has `emailVerified = true`
- **Status**: `409 Conflict`
- **Response**:

```json
{
  "code": "AUTH-003",
  "message": "Email is already verified",
  "status": 409,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

#### Empty or invalid fields

- **Condition**: The email field is empty or has a bad format
- **Status**: `400 Bad Request`
- **Response**:

```json
{
  "code": "VALIDATION-001",
  "error": "Validation Failed",
  "status": 400,
  "timestamp": "2026-03-12T11:30:00.123",
  "details": {
    "email": "must not be blank"
  }
}
```

#### Email sending failed

- **Condition**: The SMTP server failed when trying to send the new OTP email
- **Status**: `500 Internal Server Error`
- **Response**:

```json
{
  "code": "EMAIL-001",
  "message": "Failed to send verification email",
  "status": 500,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

---

## POST /api/auth/login

This endpoint authenticates the user with email and password. The email must be verified first.

### Request

```json
{
  "email": "john.doe@example.com",
  "password": "secure123"
}
```

| Field | Type | Required | Pattern | Validations |
|-------|------|----------|---------|-------------|
| `email` | String | Yes | `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$` | Cannot be empty, must be a valid email |
| `password` | String | Yes | `^.{1,}$` | Cannot be empty |

### Happy Path

- **Condition**: The credentials are correct and the email is verified
- **Status**: `200 OK`
- **Response**:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInJvbGUiOiJEUklWRVIiLCJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSJ9.xxx",
  "userId": 1,
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "DRIVER"
}
```

### Unhappy Paths

#### Wrong credentials

- **Condition**: The password is wrong, or the email does not exist
- **Status**: `401 Unauthorized`
- **Response**: Spring Security handles this error automatically (BadCredentialsException)

```json
{
  "code": "SYS-001",
  "message": "An unexpected error occurred",
  "status": 500,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

> **Note for frontend**: If login fails with 401 or 500, show a generic message like "Invalid email or password" for security reasons.

#### Email not verified

- **Condition**: The user exists but `emailVerified = false`
- **Status**: `403 Forbidden`
- **Response**:

```json
{
  "code": "AUTH-006",
  "message": "Please verify your email before logging in.",
  "status": 403,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

> **Note for frontend**: If you get AUTH-006, redirect the user to the OTP verification screen.

#### Empty or invalid fields

- **Condition**: The email or password field is empty
- **Status**: `400 Bad Request`
- **Response**:

```json
{
  "code": "VALIDATION-001",
  "error": "Validation Failed",
  "status": 400,
  "timestamp": "2026-03-12T11:30:00.123",
  "details": {
    "email": "must not be blank"
  }
}
```

---

## General Notes for the Frontend

### JWT Token

- The JWT token is returned in the `verify-otp` and `login` responses
- You must send it in the header `Authorization: Bearer <token>` for all authenticated requests
- The token payload contains: `userId`, `email`, `role`
- The token expires after 24 hours (this is configurable on the backend)

### Authentication Flow

```
1. POST /register  -->  200 (requiresVerification: true)
       |
2. User checks their email (MailHog in dev: http://localhost:8025)
       |
3. POST /verify-otp  -->  200 (token + user data)
       |
   [If the OTP expires: POST /resend-otp --> new code]
       |
4. POST /login  -->  200 (token + user data)
```

### Available Roles

| Role | Description |
|------|-------------|
| `DRIVER` | Driver - can rent vehicles |
| `CAR_OWNER` | Car Owner - can list vehicles for rent |
| `ADMIN` | Administrator - only created internally (cannot register) |

### How to Handle Errors in the Frontend

- **`VALIDATION-001`**: Show the errors for each field using the `details` object
- **`AUTH-001`**: "This email is already registered. Try logging in."
- **`AUTH-003`**: "Your email is already verified. You can log in."
- **`AUTH-004`**: "Invalid code. Please check and try again."
- **`AUTH-005`**: "Code expired. Click 'Resend' to get a new one."
- **`AUTH-006`**: Redirect the user to the OTP verification screen
- **`AUTH-007`**: This is a development error (it should not appear in production)
- **`AUTH-008`**: "You must be at least 19 years old to register."
- **`EMAIL-001`**: "We couldn't send the email. Please try again later."
- **`SYS-001`**: "Something went wrong. Please try again later."

---

## How to Define API Contracts (Step by Step)

Use this guide when you need to define the API contract for a new module. Follow these steps in order. The goal is to have a complete document like this one before writing any code.

### Step 1: List the endpoints

- Write down every endpoint that the module needs
- For each endpoint, define: HTTP method, URL path, and a short description
- Example:

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/vehicles/register` | Register a new vehicle |
| GET | `/api/vehicles/{id}` | Get vehicle details |
| PUT | `/api/vehicles/{id}` | Update vehicle information |

### Step 2: Define the request JSON for each endpoint

- Write the exact JSON body that the frontend will send
- List every field with its type (String, Long, Boolean, etc.)
- Mark which fields are required and which are optional
- Example:

```json
{
  "make": "Toyota",
  "model": "Corolla",
  "year": 2022,
  "licensePlate": "ABC-1234"
}
```

### Step 3: Define the validation patterns for each field

- For each field, write a regex pattern that validates the input
- Define: min length, max length, allowed characters, format
- Write examples of valid and invalid values
- These patterns will be used by **both** the frontend and the backend
- Example:

| Field | Pattern | Valid | Invalid |
|-------|---------|-------|---------|
| `licensePlate` | `^[A-Z]{2,3}-\d{4}$` | `ABC-1234` | `abc1234`, `A-1` |
| `year` | `^\d{4}$` (between 1900 and current year) | `2022` | `22`, `abcd` |

### Step 4: Define the success response (happy path)

- Write the exact JSON that the backend will return when everything is OK
- Define the HTTP status code (usually `200 OK` or `201 Created`)
- Describe any side effects (email sent, record saved, file uploaded, etc.)
- Example:

```
Status: 200 OK
```
```json
{
  "vehicleId": 1,
  "make": "Toyota",
  "model": "Corolla",
  "status": "PENDING_DOCUMENTS"
}
```

### Step 5: Define every unhappy path

- Think about everything that can go wrong and list each case
- For each unhappy path, define:
  - **What is the condition?** (what went wrong)
  - **What HTTP status code?** (400, 401, 403, 404, 409, 500)
  - **What error code?** (MODULE-001, MODULE-002, etc.)
  - **What is the exact JSON response?**
- Common unhappy paths to always consider:
  - Required fields are empty or have a bad format → `VALIDATION-001` (400)
  - The resource already exists → conflict error (409)
  - The resource was not found → not found error (404)
  - The user does not have permission → forbidden error (403)
  - The user is not logged in → unauthorized error (401)
  - Something unexpected broke on the server → system error (500)
- Example:

```
Condition: The license plate already exists in the database
Status: 409 Conflict
```
```json
{
  "code": "VEHICLE-001",
  "message": "A vehicle with this license plate already exists",
  "status": 409,
  "timestamp": "2026-03-12T11:30:00.123"
}
```

### Step 6: Define the error codes for the module

- Create a table with all the error codes for this module
- Use the format: `MODULE-XXX` (example: `VEHICLE-001`, `BOOKING-001`)
- For each code, write the message, HTTP status, and when it happens
- Example:

| Code | Message | HTTP Status | When? |
|------|---------|-------------|-------|
| `VEHICLE-001` | License plate already exists | 409 | Register: plate is duplicated |
| `VEHICLE-002` | Vehicle not found | 404 | Get/Update: vehicle ID does not exist |
| `VEHICLE-003` | Vehicle is not active | 400 | Booking: vehicle has no approved documents |

### Step 7: Define the frontend error messages

- For each error code, write the message that the frontend should show to the user
- The backend message is technical. The frontend message should be friendly.
- Example:

| Backend code | Frontend message |
|-------------|-----------------|
| `VEHICLE-001` | "This license plate is already registered." |
| `VEHICLE-002` | "Vehicle not found. It may have been removed." |
| `VALIDATION-001` | Show each field error from the `details` object |

### Step 8: Draw the user flow

- Draw the steps that the user follows from start to finish
- This helps the frontend know when to call each endpoint
- Example:

```
1. Owner fills the vehicle form
       |
2. POST /api/vehicles/register  -->  200 (vehicleId)
       |
3. Owner uploads 3 documents (Registration, Insurance, Inspection)
       |
4. POST /api/documents/upload  -->  200 (for each document)
       |
5. Admin reviews documents in the admin panel
       |
6. Vehicle status changes to ACTIVE when all 3 documents are approved
```

### Step 9: Review with the team

- Share the document with the frontend and backend developers
- Check that everyone agrees on the field names, types, and error codes
- Make changes if needed before writing any code
- Once approved, both teams can start working in parallel:
  - **Frontend**: Build the forms and mock the API responses
  - **Backend**: Implement the endpoints and return the exact JSON defined here

---

## Module List

TURBO is built in 10 modules. Each module is a complete feature from database to frontend. We finish one module before starting the next one.

| # | Module | Branch | Status |
|---|--------|--------|--------|
| 1 | Auth & Security | `module-1/auth-security` | Done |
| 2 | User & Entity Model | `module-2/user-entity-model` | Pending |
| 3 | Vehicle Management | `module-3/vehicle-management` | Pending |
| 4 | Document Management | `module-4/document-management` | Pending |
| 5 | Booking System | `module-5/booking-system` | Pending |
| 6 | Payments & Earnings | `module-6/payments-earnings` | Pending |
| 7 | Reviews | `module-7/reviews` | Pending |
| 8 | Admin Panel | `module-8/admin-panel` | Pending |
| 9 | Public Pages & Layout | `module-9/public-pages-layout` | Pending |
| 10 | Infrastructure | `module-10/infrastructure` | Pending |

### Module 1 - Auth & Security (Done)

- **What it does**: User registration, login, OTP email verification, JWT tokens, role-based access
- **Backend endpoints**: `POST /api/auth/register`, `/verify-otp`, `/resend-otp`, `/login`
- **Frontend pages**: LoginPage, SignUpPage, VerifyOtpPage
- **Error codes**: AUTH-001 to AUTH-007, EMAIL-001
- **API contract**: This document

### Module 2 - User & Entity Model

- **What it does**: Define the database model for all entities and enums used across the platform
- **Entities**: User (base), Driver, CarOwner, Admin
- **Enums**: UserRole, BookingStatus, DocumentStatus, PaymentStatus, ReviewType
- **Repositories**: UserRepository, DriverRepository, CarOwnerRepository, AdminRepository
- **Seed data**: 3 test users (admin, driver, owner) created on startup
- **Note**: This module has no new endpoints. It builds the data foundation for modules 3-8.

### Module 3 - Vehicle Management

- **What it does**: Car owners can create, edit, and delete vehicles. Drivers can browse available vehicles.
- **Entities**: Vehicle, AvailabilitySlot
- **Backend endpoints**:
  - `POST /api/owner/vehicles` - Create a vehicle
  - `GET /api/owner/vehicles` - List owner's vehicles
  - `PUT /api/owner/vehicles/{id}` - Update a vehicle
  - `DELETE /api/owner/vehicles/{id}` - Delete a vehicle
  - `GET /api/vehicles` - Browse available vehicles (public or driver)
  - `GET /api/vehicles/{id}` - View vehicle details
- **Frontend pages**: OwnerVehicles, SelectCarPage
- **Key rule**: A vehicle needs 3 approved documents (Registration, Insurance, Inspection) to become ACTIVE
- **Error code prefix**: `VEHICLE-XXX`

### Module 4 - Document Management

- **What it does**: Upload, download, and manage documents for vehicles and users. Admin reviews and approves/rejects.
- **Entities**: Document
- **Backend endpoints**:
  - `POST /api/documents/upload` - Upload a document (JPG, PNG, PDF, max 5MB)
  - `GET /api/documents/{id}` - Download/preview a document
  - `GET /api/documents/vehicle/{vehicleId}` - List documents for a vehicle
- **Frontend pages**: DocumentsPage
- **Key rule**: Old documents are never deleted. New uploads create new records (audit trail). Use `getLatestDocsPerType()` to check current status.
- **Error code prefix**: `DOC-XXX`

### Module 5 - Booking System

- **What it does**: Drivers create bookings to rent vehicles. Owners confirm or reject. Both can cancel.
- **Entities**: Booking
- **Backend endpoints**:
  - `POST /api/driver/bookings` - Create a booking
  - `GET /api/driver/bookings` - List driver's bookings
  - `GET /api/owner/bookings` - List owner's vehicle bookings
  - `PUT /api/owner/bookings/{id}/confirm` - Owner confirms
  - `PUT /api/owner/bookings/{id}/reject` - Owner rejects
  - `PUT /api/bookings/{id}/cancel` - Either party cancels
- **Frontend pages**: DriverDashboard, OwnerBookings
- **Status flow**: `PENDING` → `CONFIRMED` → `ACTIVE` → `COMPLETED` / `CANCELLED`
- **Error code prefix**: `BOOKING-XXX`

### Module 6 - Payments & Earnings

- **What it does**: Link payments to bookings. Split money between platform and owner. Show earnings dashboard.
- **Entities**: Payment
- **Backend endpoints**:
  - `POST /api/payments` - Create a payment for a booking
  - `GET /api/owner/earnings` - Owner earnings summary
  - `GET /api/owner/transactions` - Owner transaction history
- **Frontend pages**: OwnerDashboard, OwnerEarnings
- **Payment split**: Platform fee + owner payout + security deposit
- **Status flow**: `PENDING` → `COMPLETED` / `REFUNDED` / `FAILED`
- **Error code prefix**: `PAYMENT-XXX`

### Module 7 - Reviews

- **What it does**: After a completed booking, both driver and owner can leave a star rating (1-5) with a comment.
- **Entities**: Review
- **Backend endpoints**:
  - `POST /api/reviews` - Create a review
  - `GET /api/reviews/user/{userId}` - List reviews for a user
  - `GET /api/reviews/vehicle/{vehicleId}` - List reviews for a vehicle
- **Frontend pages**: OwnerReviews, StarRating component
- **Key rule**: Only allowed after booking status is `COMPLETED`
- **Error code prefix**: `REVIEW-XXX`

### Module 8 - Admin Panel

- **What it does**: Admin can review documents (with zoom modal), approve/reject them, and manage users.
- **Backend endpoints**:
  - `GET /api/admin/documents/pending` - List pending documents
  - `PUT /api/admin/documents/{id}/approve` - Approve a document
  - `PUT /api/admin/documents/{id}/reject` - Reject a document
  - `GET /api/admin/users` - List all users
- **Frontend pages**: AdminDashboard, AdminUsers
- **Error code prefix**: `ADMIN-XXX`

### Module 9 - Public Pages & Layout

- **What it does**: Build the public-facing pages and the shared layout components (navbar, footer, sidebar).
- **Frontend pages**: HomePage, PricingPage, AboutPage
- **Components**: Navbar (dynamic by role), Footer, Sidebar (dashboard navigation)
- **Router**: 27 routes with role-based access and 404 fallback
- **Note**: This module is frontend-only. No new backend endpoints.

### Module 10 - Infrastructure

- **What it does**: Containerize the entire app with Docker Compose for deployment.
- **Containers**: MySQL, Spring Boot backend, Nginx reverse proxy, MailHog
- **Files**: `docker-compose.yml`, backend `Dockerfile`, frontend `Dockerfile`, `nginx.conf`
- **Key features**:
  - Multi-stage builds (smaller images)
  - Nginx routes `/api/*` to backend, serves static files with cache
  - MailHog captures emails in dev (Web UI on port 8025)
  - Persistent volumes for MySQL data and file uploads
  - Health checks for service dependencies
