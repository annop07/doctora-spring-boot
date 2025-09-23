# fix.md - การเปลี่ยนแปลงโปรเจค Doctoralia

## สถานะ Branch
- **Branch ปัจจุบัน**: `joecsss` 
- **Branch ที่ Merge เข้ามา**: 
  - `feature/Doctor-Management`
  - `feature/register-login`

## การเปลี่ยนแปลงใหญ่ & ฟีเจอร์ใหม่

### 1. ระบบ Authentication & Security ✅

#### คอมโพเนนต์ใหม่:
- **ระบบ JWT Authentication**
  - `JwtUtils.java` - สร้างและตรวจสอบ JWT token
  - `JwtAuthenticationFilter.java` - Filter สำหรับประมวลผล JWT token
  - Token หมดอายุ: 24 ชั่วโมง (86400000ms)

```java
// ตัวอย่าง JWT Utils
@Component
public class JwtUtils {
    public String generateJwtToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
```

- **Security Configuration**
  - `SecurityConfig.java` - ตั้งค่า Spring Security แบบครบถ้วน
  - Endpoint สาธารณะสำหรับลงทะเบียน/เข้าสู่ระบบ
  - Endpoint ที่ป้องกันด้วยการควบคุมแบบ role-based
  - การกำหนดค่า CORS สำหรับ cross-origin requests

```java
// ตัวอย่าง Security Config
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

#### Authentication Endpoints:
```
POST /api/auth/register - ลงทะเบียนผู้ใช้
POST /api/auth/login - เข้าสู่ระบบ
```

### 2. ระบบจัดการผู้ใช้ ✅

#### ฟีเจอร์ใหม่:
- **การลงทะเบียนและเข้าสู่ระบบ**
  - Authentication ผ่านอีเมล
  - เข้ารหัสรหัสผ่านด้วย BCrypt
  - ระบบ Role ของผู้ใช้ (PATIENT, DOCTOR, ADMIN)
  - ชื่อจริง/นามสกุล สามารถเป็น null ได้ (V7 migration)

```java
// ตัวอย่าง User Registration
@PostMapping("/register")
public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
    if (userService.existsByEmail(request.getEmail())) {
        return ResponseEntity.badRequest()
                .body(new MessageResponse("Error: Email is already in use!"));
    }
    
    User user = userService.registerUser(
            request.getEmail(),
            request.getPassword(),
            request.getFirstName(),
            request.getLastName(),
            request.getRole()
    );
    
    return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
}
```

- **การจัดการโปรไฟล์ผู้ใช้**
  - `GET /api/users/me` - ดูโปรไฟล์ผู้ใช้ปัจจุบัน
  - `PUT /api/users/me` - อัปเดตโปรไฟล์ผู้ใช้
  - `GET /api/users/all` - เฉพาะ Admin: ดูผู้ใช้ทั้งหมด

#### DTO ที่เพิ่มเข้ามา:
- `LoginRequest.java`
- `RegisterRequest.java` (อัปเดตให้รองรับ role)
- `UpdateProfileRequest.java`
- `MessageResponse.java`

### 3. ระบบจัดการหมอ ✅

#### Model ใหม่:
- **Doctor Entity** (`Doctor.java`)
  - เลขใบประกอบวิชาชีพ (ไม่ซ้ำ)
  - ประวัติ, จำนวนปีประสบการณ์
  - ค่าตรวจ (ค่าเริ่มต้น: 500.00)
  - หมายเลขห้อง
  - สถานะการใช้งาน
  - ความสัมพันธ์กับ User และ Specialty

```java
// ตัวอย่าง Doctor Entity
@Entity
@Table(name = "doctors")
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "license_number", unique = true, nullable = false)
    private String licenseNumber;
    
    @DecimalMin(value = "0.0")
    private BigDecimal consultationFee = BigDecimal.valueOf(500.00);
    
    // ... getters and setters
}
```

#### ฟีเจอร์ใหม่:
- **การจัดการโปรไฟล์หมอ**
  - `GET /api/doctors/me` - โปรไฟล์ของหมอเอง (สำหรับ DOCTOR role)
  - `PUT /api/doctors/me` - อัปเดตโปรไฟล์หมอ (สำหรับ DOCTOR role)

```java
// ตัวอย่าง Doctor Profile Management
@GetMapping("/me")
@PreAuthorize("hasRole('DOCTOR')")
public ResponseEntity<?> getMyProfile(HttpServletRequest request) {
    String jwt = parseJwt(request);
    Long userId = jwtUtils.getUserIdFromJwtToken(jwt);
    
    Optional<Doctor> doctorOpt = doctorService.findByUserId(userId);
    if (doctorOpt.isPresent()) {
        return ResponseEntity.ok(convertToDoctorProfileResponse(doctorOpt.get()));
    }
    return ResponseEntity.badRequest()
            .body(new MessageResponse("Doctor profile not found."));
}
```

- **API สาธารณะสำหรับหมอ**
  - `GET /api/doctors` - ค้นหาหมอพร้อม filter
  - `GET /api/doctors/{id}` - ดูหมอตาม ID (สาธารณะ)
  - `GET /api/doctors/specialty/{id}` - หมอตามแผนก
  - `GET /api/doctors/search?name=` - ค้นหาตามชื่อ
  - `GET /api/doctors/stats` - สถิติหมอ

```java
// ตัวอย่างการค้นหาหมอ
@GetMapping
public ResponseEntity<?> searchDoctors(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Long specialty,
        @RequestParam(required = false) BigDecimal minFee,
        @RequestParam(required = false) BigDecimal maxFee) {
    
    Page<Doctor> doctors = doctorService.searchDoctors(name, specialty, minFee, maxFee, page, size);
    
    Map<String, Object> response = new HashMap<>();
    response.put("doctors", doctors.getContent().stream().map(this::convertToDoctorResponse).toList());
    response.put("totalPages", doctors.getTotalPages());
    response.put("totalItems", doctors.getTotalElements());
    
    return ResponseEntity.ok(response);
}
```

- **การจัดการหมอสำหรับ Admin**
  - `POST /api/admin/doctors` - สร้างหมอใหม่ (เฉพาะ ADMIN)
  - `PUT /api/admin/doctors/{id}/status` - เปิด/ปิดสถานะหมอ

### 4. ระบบจัดการแผนก ✅

#### ฟีเจอร์ใหม่:
- **API สาธารณะสำหรับแผนก**
  - `GET /api/specialties` - ดูแผนกทั้งหมด
  - `GET /api/specialties/with-count` - แผนกพร้อมจำนวนหมอ
  - `GET /api/specialties/{id}` - ดูแผนกตาม ID
  - `GET /api/specialties/search?name=` - ค้นหาแผนก

```java
// ตัวอย่างการดูแผนกพร้อมจำนวนหมอ
@GetMapping("/with-count")
public ResponseEntity<?> getSpecialtiesWithDoctorCount() {
    List<SpecialtyWithDoctorCount> specialties = specialtyService.getSpecialtiesWithDoctorCount();
    
    List<Map<String, Object>> response = specialties.stream()
            .map(item -> {
                Map<String, Object> specialty = convertToSpecialtyResponse(item.getSpecialty());
                specialty.put("doctorCount", item.getDoctorCount());
                return specialty;
            })
            .toList();
    
    return ResponseEntity.ok(Map.of("specialties", response));
}
```

- **การจัดการแผนกสำหรับ Admin**
  - `POST /api/admin/specialties` - สร้างแผนก
  - `PUT /api/admin/specialties/{id}` - อัปเดตแผนก
  - `DELETE /api/admin/specialties/{id}` - ลบแผนก (ถ้าไม่มีหมอ)

### 5. ระบบ Admin Dashboard ✅

#### ฟีเจอร์ Admin ใหม่:
- `GET /api/admin/dashboard` - สถิติระบบ
- `GET /api/admin/specialties/with-count` - จัดการแผนกสำหรับ Admin
- การดำเนินการ CRUD แบบครบถ้วนสำหรับหมอและแผนก
- ความสามารถในการจัดการผู้ใช้

```java
// ตัวอย่าง Admin Dashboard
@GetMapping("/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getDashboard() {
    DoctorStats doctorStats = doctorService.getDoctorStats();
    
    Map<String, Object> dashboard = new HashMap<>();
    dashboard.put("totalDoctors", doctorStats.getTotalDoctors());
    dashboard.put("totalSpecialties", doctorStats.getTotalSpecialties());
    
    return ResponseEntity.ok(dashboard);
}
```

### 6. การอัปเดต Database Schema

#### ตารางใหม่:
1. **doctors** (V3 migration)
   - การจัดการโปรไฟล์หมอแบบครบถ้วน
   - Foreign keys ไปยัง users และ specialties
   - ข้อจำกัดตาม business logic

```sql
-- ตัวอย่าง Doctor Table
CREATE TABLE doctors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    specialty_id BIGINT NOT NULL REFERENCES specialties(id) ON DELETE RESTRICT,
    license_number VARCHAR(100) UNIQUE NOT NULL,
    bio TEXT,
    experience_years INTEGER DEFAULT 0,
    consultation_fee DECIMAL(10,2) DEFAULT 500.00,
    room_number VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

2. **availabilities** (V4 migration) - เตรียมไว้สำหรับระบบนัดหมายในอนาคต
3. **appointments** (V5 migration) - เตรียมไว้สำหรับระบบนัดหมายในอนาคต
4. **reviews** (V6 migration) - เตรียมไว้สำหรับระบบรีวิวในอนาคต

#### การแก้ไข Schema:
- **V7**: ทำให้ `first_name` และ `last_name` เป็น nullable ในตาราง users

### 7. สถาปัตยกรรม Service Layer

#### Service ใหม่:
- **DoctorService.java**
  - การดำเนินการ CRUD ของหมอ
  - การค้นหาและการกรอง
  - การจัดการสถานะ
  - การคำนวณสถิติ

```java
// ตัวอย่าง Doctor Service
@Service
@Transactional
public class DoctorService {
    public Page<Doctor> searchDoctors(String name, Long specialtyId, 
                                     BigDecimal minFee, BigDecimal maxFee,
                                     int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("user.firstName").ascending());
        return doctorRepository.findDoctorsWithFilters(name, specialtyId, minFee, maxFee, pageable);
    }
    
    public Doctor createDoctor(Long userId, Long specialtyId, String licenseNumber,
                              String bio, Integer experienceYears, 
                              BigDecimal consultationFee, String roomNumber) {
        // ตรวจสอบ user role
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.get().getRole().equals(UserRole.DOCTOR)) {
            throw new IllegalArgumentException("User must have DOCTOR role");
        }
        
        // สร้าง doctor profile
        Doctor doctor = new Doctor();
        doctor.setUser(userOpt.get());
        // ... set other fields
        
        return doctorRepository.save(doctor);
    }
}
```

### 8. Repository Layer

#### Repository ใหม่:
- **DoctorRepository.java**
  - Query ที่ซับซ้อนด้วย @Query annotations
  - การกรองตามสถานะ active
  - รองรับ Pagination
  - การค้นหาชื่อหมอผ่าน User relationships

```java
// ตัวอย่าง Doctor Repository
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    @Query("SELECT d FROM Doctor d JOIN d.user u JOIN d.specialty s WHERE " +
           "d.isActive = true AND " +
           "(:name IS NULL OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:specialtyId IS NULL OR s.id = :specialtyId) AND " +
           "(:minFee IS NULL OR d.consultationFee >= :minFee) AND " +
           "(:maxFee IS NULL OR d.consultationFee <= :maxFee)")
    Page<Doctor> findDoctorsWithFilters(@Param("name") String name,
                                       @Param("specialtyId") Long specialtyId,
                                       @Param("minFee") BigDecimal minFee,
                                       @Param("maxFee") BigDecimal maxFee,
                                       Pageable pageable);
}
```

### 9. Security & Access Control

#### การควบคุมแบบ Role-Based:
- **PUBLIC**: ลงทะเบียน, เข้าสู่ระบบ, เรียกดูหมอ/แผนก
- **AUTHENTICATED**: จัดการโปรไฟล์, นัดหมาย (ในอนาคต)
- **DOCTOR**: จัดการโปรไฟล์ตนเอง (`/api/doctors/me/**`)
- **ADMIN**: จัดการระบบแบบเต็มรูปแบบ (`/api/admin/**`)

```java
// ตัวอย่างการใช้ Role-based Authorization
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/admin/doctors")
public ResponseEntity<?> addDoctor(@Valid @RequestBody CreateDoctorRequest request) {
    // เฉพาะ ADMIN เท่านั้นที่เข้าถึงได้
}

@PreAuthorize("hasRole('DOCTOR')")
@GetMapping("/doctors/me")
public ResponseEntity<?> getMyProfile() {
    // เฉพาะ DOCTOR เท่านั้นที่เข้าถึงได้
}
```

## สรุป API Endpoints

### Endpoints สาธารณะ (ไม่ต้อง Authentication):
```
POST /api/auth/register      - ลงทะเบียน
POST /api/auth/login         - เข้าสู่ระบบ
GET  /api/specialties/**     - ดูแผนกต่างๆ
GET  /api/doctors            - ค้นหาหมอ (สาธารณะ)
GET  /api/doctors/{id}       - ดูโปรไฟล์หมอ
GET  /api/doctors/search     - ค้นหาหมอตามชื่อ
GET  /api/doctors/specialty/**  - หมอตามแผนก
GET  /api/doctors/stats      - สถิติหมอ
```

### Endpoints ที่ต้อง Authentication:
```
GET  /api/users/me           - ดูโปรไฟล์ตนเอง
PUT  /api/users/me           - อัปเดตโปรไฟล์
GET  /api/doctors/me         - โปรไฟล์หมอ (เฉพาะ DOCTOR)
PUT  /api/doctors/me         - อัปเดตโปรไฟล์หมอ (เฉพาะ DOCTOR)
```

### Endpoints สำหรับ Admin (เฉพาะ ADMIN role):
```
POST /api/admin/doctors             - สร้างหมอใหม่
PUT  /api/admin/doctors/{id}/status - เปิด/ปิดสถานะหมอ
POST /api/admin/specialties         - สร้างแผนกใหม่
PUT  /api/admin/specialties/{id}    - อัปเดตแผนก
DELETE /api/admin/specialties/{id}  - ลบแผนก
GET  /api/admin/specialties/with-count  - แผนกพร้อมจำนวนหมอ
GET  /api/admin/dashboard           - แดชบอร์ดระบบ
GET  /api/users/all                 - ดูผู้ใช้ทั้งหมด
```

## คุณสมบัติทางเทคนิคที่สำคัญ

### 1. การค้นหาและกรองข้อมูลขั้นสูง
```java
// ตัวอย่างการใช้งาน API
GET /api/doctors?name=สมชาย&specialty=1&minFee=300&maxFee=800&page=0&size=10
```

### 2. การตรวจสอบข้อมูล
```java
@Valid @RequestBody RegisterRequest request
// Bean validation annotations
@Email(message = "Invalid email format")
@NotBlank(message = "Email is required")
```

### 3. การเพิ่มประสิทธิภาพฐานข้อมูล
```sql
-- Index สำหรับการค้นหา
CREATE INDEX idx_doctors_user_id ON doctors(user_id);
CREATE INDEX idx_doctors_specialty_id ON doctors(specialty_id);
CREATE INDEX idx_doctors_active ON doctors(is_active);
```

## สถานะ: ✅ ฟีเจอร์ที่เสร็จสมบูรณ์

Branch ที่ merge เข้ามาได้ทำการพัฒนาสำเร็จแล้ว:
- ✅ ระบบ authentication แบบครบถ้วน
- ✅ การลงทะเบียนและจัดการโปรไฟล์ผู้ใช้
- ✅ การจัดการโปรไฟล์หมอ
- ✅ การจัดการแผนก
- ✅ แดชบอร์ด Admin
- ✅ ความปลอดภัยแบบ role-based
- ✅ การค้นหาและกรองข้อมูลขั้นสูง
- ✅ API สาธารณะสำหรับการรวมเข้ากับ frontend

## ขั้นตอนต่อไปที่แนะนำ

1. **การรวมเข้ากับ Frontend**: API พร้อมใช้งานสำหรับ frontend
2. **ระบบนัดหมาย**: ตารางฐานข้อมูลมีอยู่แล้ว ให้ทำการนัดหมาย
3. **ระบบรีวิว**: ตารางฐานข้อมูลมีอยู่แล้ว ให้ทำรีวิวหมอ
4. **การยืนยันอีเมล**: เพิ่มการยืนยันอีเมลสำหรับการลงทะเบียน
5. **รีเซ็ตรหัสผ่าน**: ทำระบบลืมรหัสผ่าน
6. **อัปโหลดไฟล์**: เพิ่มอัปโหลดรูปโปรไฟล์
7. **ระบบแจ้งเตือน**: แจ้งเตือนการนัดหมาย
8. **การวิเคราะห์**: เพิ่มสถิติในแดชบอร์ด Admin