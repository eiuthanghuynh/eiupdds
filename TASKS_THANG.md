# BACKEND & CLOUD/AI SPRINT BACKLOG — THẮNG

> **Chỉ dẫn cho AI Agent:**
> Khi người dùng nhắc đến cụm từ **"Tôi là Thắng"**, **"Thắng đây"**, hoặc khai báo vai trò là **Thắng**:
> 1. Tự động nhận diện vai trò: **Backend Lead & AI Cloud Engineer** (Spring Boot, PostgreSQL, Docker, Roboflow Proxy, Storage).
> 2. Đọc file này để xác định Sprint hiện tại và các task chưa hoàn thành (`- [ ]`).
> 3. Tuyệt đối không can thiệp vào logic giao diện Mobile hay Web Admin trừ khi được yêu cầu API Contract.
> 4. Khi implement xong mỗi task, tự động cập nhật trạng thái thành `- [x]`.

---

## Thông Tin Thành Viên & Tech Stack
* **Thành viên:** Thắng
* **Vai trò:** Backend Developer & Data/AI Engineer
* **Công nghệ cốt lõi:** Java 17+, Spring Boot 3.x, Spring Data JPA, PostgreSQL, Docker / Docker Compose, MinIO / Cloudinary, RESTful API (OpenAPI 3 / Swagger).
* **Workspace dự kiến:** `server/` hoặc `backend/`

---

## Sprint 1: Khởi Tạo Nền Tảng & Đặc Tả API Contract (Tuần 1 - 2)
**Mục tiêu Sprint:** Dựng khung dịch vụ Spring Boot, Database và kết nối thành công với Roboflow API.

- [x] **Task 1.1: Khởi tạo Spring Boot Project & Docker Compose** (4h)
  - Khởi tạo project Spring Boot 3.x (Dependencies: Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok, Validation).
  - Viết file `docker-compose.yml` khởi chạy PostgreSQL 16, Redis 7, MinIO/Silo và `backend` Spring Boot service.
  - Viết multi-stage `Dockerfile` (`eclipse-temurin:21-jdk-alpine` builder, `eclipse-temurin:21-jre-alpine` runtime, non-root user `pdds:pdds`, JVM container ergonomics).
  - *Acceptance Criteria:* Chạy `docker compose -f infra/docker-compose.yml up -d --build` khởi chạy toàn bộ hệ thống khép kín gồm PostgreSQL, Redis, MinIO (bucket auto-init), và Spring Boot Backend với healthcheck tự động.


- [x] **Task 1.2: Thiết kế Database Schema & Entity** (6h)
  - Tạo 8 JPA Entities chuẩn 3NF: `User`, `Plant`, `Disease`, `PlantDisease`, `Treatment`, `Diagnosis`, `BoundingBox`, `DiagnosisFeedback`.
  - Cấu hình quan hệ One-to-Many (`Diagnosis` - `BoundingBox`), Many-to-Many qua junction (`Plant` - `Disease`), và Human-in-the-loop (`DiagnosisFeedback`).
  - Viết Flyway migration script `V1__init_schema.sql` với UUID PK, Foreign Key indexes, JSONB GIN index, và geospatial index.
  - Viết suite kiểm thử tự động `DatabaseFoundationTests` xác minh persistence, audit timestamps, soft delete tombstones, và optimistic locking.
  - *Acceptance Criteria:* Pass 100% kiểm thử JPA và Flyway migration script chuẩn PostgreSQL 16.

- [x] **Task 1.3: Tích hợp Thư viện Roboflow Inference API** (6h)
  - Pre-trained Model chính thức: `plant-leaf-diseases/10` (mAP@50: 70.7%, Precision: 71.0%, Recall: 65.1%, 30 classes).
  - Direct Model Endpoint: `POST https://detect.roboflow.com/plant-leaf-diseases/10?api_key=${ROBOFLOW_API_KEY}`.
  - Workflow ID tích hợp: `plant-disease-yolo-onfen`, Workspace: `plant-disease-detection-mztfb`.
  - Client script: `ai-pipeline/scripts/roboflow_workflow_client.py` (hỗ trợ `Authorization: Bearer`, exponential backoff retry, decode ảnh base64 ra đĩa an toàn).
  - Smoke test: `ai-pipeline/scripts/test_workflow_smoke.py`.
  - *Acceptance Criteria:* Chạy `python ai-pipeline/scripts/test_workflow_smoke.py` với `ROBOFLOW_API_KEY` pass 100%.

- [x] **Task 1.4: Đặc tả API Contract qua Swagger / OpenAPI** (4h)
  - Tích hợp `springdoc-openapi-starter-webmvc-ui` với đầy đủ OpenAPI 3.0 Schemas, Models và Tags.
  - Viết Flyway migration `V2__seed_master_data.sql` và `MasterDataSeeder` nạp danh mục 4 cây trồng, 5 bệnh hại và phác đồ điều trị.
  - Xây dựng hệ thống DTOs bất biến (`record`): `BoundingBoxDto` (tọa độ kép Pixel & Top-Left Normalized [0, 1]), `DiagnosisAnalyzeResponse`, `DiseaseSummaryDto`, `DiseaseDetailDto`, `PlantDto`, `TreatmentDto`.
  - Triển khai Controllers & Services:
    - `POST /api/v1/diagnoses/analyze` (nhận multipart file ảnh, trả về tọa độ chuẩn hóa, bệnh chiếm ưu thế theo Max Confidence và phác đồ điều trị).
    - `GET /api/v1/diseases` & `GET /api/v1/diseases/{code}` (danh mục bệnh và phác đồ chi tiết).
    - `GET /api/v1/plants` & `GET /api/v1/plants/{code}` (danh mục cây trồng được hỗ trợ).
  - Viết bộ kiểm thử tự động `ApiContractTests` với MockMvc đạt 100% pass (10/10 tests toàn dự án).
  - *Acceptance Criteria:* Huy có thể truy cập `http://localhost:8080/swagger-ui.html` để lấy mẫu JSON và phát triển Mobile Client độc lập.

---

## Sprint 2: Core MVP — Luồng Chẩn Đoán & Proxy AI (Tuần 3 - 4)
**Mục tiêu Sprint:** Hoàn thiện pipeline nhận diện khép kín từ Mobile gửi lên qua Backend proxy, tái cấu trúc SOLID toàn diện và tối ưu hóa khả năng mở rộng / bảo vệ quota AI.

- [x] **Task 2.1: Hoàn thiện Endpoint Chẩn Đoán Cốt Lõi & Tái Cấu Trúc SOLID** (8h)
  - Viết controller và service cho `POST /api/v1/diagnoses/analyze`.
  - Validate kích thước file ảnh, định dạng (JPEG, PNG, WebP) và kiểm tra magic bytes không phụ thuộc extension.
  - Forward ảnh sang Roboflow API, xử lý timeout và exponential backoff retry.
  - Tái cấu trúc theo chuẩn SOLID:
    - Thu nhỏ `DiagnosisServiceImpl` thành Clean Facade (< 120 LOC).
    - Tách 5 Domain Services chuyên biệt: `CoordinateTransformer` (SRP), `PathologyAnalysisService` (SRP), `TreatmentRecommendationService` (SRP), `DiagnosisPersistenceService` (SRP), `DiagnosisIdempotencyService` (SRP).
    - Áp dụng Parameter Object `DiagnosisAnalyzeRequest` cho Controller và Service.
  - *Acceptance Criteria:* Nhận ảnh từ request multipart, trả về JSON chuẩn hóa: `{ diagnosisId, dominantDisease, confidence, boundingBoxes: [{ x, y, width, height, xMinNormalized... }] }`.

- [x] **Task 2.2: Tích hợp Cloudinary/MinIO Storage Service** (6h)
  - Viết `StorageService` interface trừu tượng hóa tầng lưu trữ ảnh (DIP).
  - Triển khai `MinioStorageServiceImpl` kết nối AWS S3 SDK v2 với MinIO / Pigsty Silo S3 server trong Docker Compose.
  - Khởi tạo bucket `eiu-pdds-plants` tự động kèm chính sách public read policy.
  - Sinh URL công khai cho ảnh chẩn đoán và hỗ trợ dọn dẹp file khi cần thiết.
  - *Acceptance Criteria:* Đẩy file ảnh gốc lên MinIO S3 thành công, sinh URL ảnh truy cập được ngay lập tức; vượt qua 100% `MinioStorageServiceTests`.

- [x] **Task 2.3: Viết Unit Test & Integration Test Toàn Diện & Gia Cố Enterprise** (6h)
  - Thiết lập bộ kiểm thử toàn diện với `@SpringBootTest`, MockMvc và Mockito (60/60 tests PASS — 100%).
  - Mock kết quả Roboflow để test luồng chẩn đoán độc lập, zero-quota consumption.
  - **Anti-Corruption Layer (ACL):** Đóng gói toàn bộ mapping nhãn Roboflow vào `RoboflowInferenceProvider`, bảo vệ Domain Services độc lập với vendor.
  - **Idempotency Quota Guard:** Fingerprint ảnh bằng SHA-256 + Sliding Window TTL Cache trong `DiagnosisIdempotencyService`, phát lại kết quả trong < 5ms khi gặp ảnh trùng lặp.
  - **Khắc phục Technical Debt kích thước ảnh:** Triệt tiêu magic numbers 399x501, bổ sung `FileSecurityValidator.extractDimensions` (Source of Truth đọc header JPEG/PNG/WebP trong 0.05ms) và Fail-Fast với `ROBOFLOW_API_ERROR`.
  - *Acceptance Criteria:* Pass 100% 60 test cases trên toàn bộ module backend (`mvnw.cmd test`), không phát sinh bất kỳ hồi quy nào.

---

## Sprint 2.5: Nền Tảng Xác Thực & Phân Quyền Chuẩn Enterprise (AuthN & AuthZ Core)
**Mục tiêu Sprint:** Xây dựng hệ thống xác thực người dùng hoàn chỉnh, hỗ trợ đăng ký/đăng nhập cục bộ (LOCAL), đăng nhập Google OAuth2, cơ chế Dual-Token JWT an toàn với Refresh Token Rotation và ngăn chặn tấn công Replay Attack.

- [x] **Task 2.5.1: Thiết kế Federated Identity Pattern & Flyway Migration V3** (4h)
  - Cập nhật entity `User` (cho phép `passwordHash` nullable với tài khoản OAuth, bổ sung `avatarUrl`).
  - Tạo entity `UserIdentity` và enum `AuthProvider` (`LOCAL`, `GOOGLE`, `APPLE`) mapping với bảng `user_identities`.
  - Viết script Flyway migration `V3__create_user_identities_and_adjust_users.sql` kèm ràng buộc duy nhất `(provider, provider_user_id)` và indexes.
  - *Acceptance Criteria:* Khởi tạo cấu trúc bảng sạch sẽ, hỗ trợ một user liên kết nhiều provider danh tính.

- [x] **Task 2.5.2: Triển khai Kiến Trúc Dual-Token JWT & Redis Refresh Token Store** (6h)
  - `JwtTokenProvider`: Ký Access Token HMAC-SHA256 với claims (`sub`, `email`, `role`, `fullName`, `iss`, `aud`), hạn 15 phút.
  - Sinh Refresh Token ID an toàn với `SecureRandom` 256 bits, hạn 7 ngày.
  - `RefreshTokenStore` (DIP): Triển khai `RedisRefreshTokenStore` (`@Primary`) với atomic `getAndDelete` và `InMemoryRefreshTokenStore` (`@Profile("test")`).
  - **Token Reuse Detection:** Lưu vết token đã xoay vòng (tombstone), nếu phát hiện replay token cũ sẽ lập tức hủy toàn bộ phiên của người dùng (`revokeAllForUser`) và bắn lỗi bảo mật `TOKEN_REUSE_DETECTED`.
  - *Acceptance Criteria:* Kiểm thử xoay vòng token mượt mà, token cũ bị chặn tức thì.

- [x] **Task 2.5.3: Tích hợp Google OAuth2 Token Exchange & Identity Auto-linking** (4h)
  - Tích hợp thư viện `google-api-client` (với `google-http-client-gson`).
  - `GoogleTokenVerifier`: Kiểm tra tính hợp lệ của Google ID Token, xác thực chữ ký số từ Google public keys và trích xuất `sub`, `email`, `name`, `pictureUrl`.
  - Auto-linking: Nếu email đã tồn tại dưới dạng `LOCAL`, tự động liên kết danh tính `GOOGLE` với user mà không tạo user rác.
  - *Acceptance Criteria:* Xác thực Google ID Token thành công, cấp phát token hệ thống EIU PDDS.

- [x] **Task 2.5.4: Cấu hình Spring Security 6 Stateless Filter Chain & RBAC** (4h)
  - Tích hợp `BCryptPasswordEncoder(12)`.
  - Triển khai `JwtAuthenticationFilter` phân giải Bearer token và nạp `UserPrincipal` vào `SecurityContext`.
  - Xử lý lỗi bảo mật tập trung: `JwtAuthenticationEntryPoint` (401 Unauthorized JSON) và `CustomAccessDeniedHandler` (403 Forbidden JSON).
  - RBAC: Thiết lập quyền truy cập cho `ROLE_USER`, `ROLE_EXPERT`, `ROLE_ADMIN` trên các endpoint chẩn đoán và quản lý danh mục.
  - *Acceptance Criteria:* Mọi request không hợp lệ đều trả về mã lỗi JSON chuẩn của hệ thống, không render trang lỗi mặc định của Spring.

- [x] **Task 2.5.5: REST API Controller & Bộ Kiểm Thử Tự Động Toàn Diện** (6h)
  - Viết `AuthController` với các endpoint:
    - `POST /api/v1/auth/register` (201 Created)
    - `POST /api/v1/auth/login` (200 OK)
    - `POST /api/v1/auth/google` (200 OK)
    - `POST /api/v1/auth/refresh` (200 OK)
    - `POST /api/v1/auth/logout` (200 OK)
    - `GET /api/v1/auth/me` (200 OK, Bearer token required)
  - Cập nhật Swagger UI / OpenAPI 3.0 với cấu hình `bearerAuth`.
  - Viết bộ test suite tự động 34 test cases: `JwtTokenProviderTest`, `AuthServiceTest`, `InMemoryRefreshTokenStoreTest`, `AuthIntegrationTests`.
  - *Acceptance Criteria:* Pass 100% toàn bộ 94/94 test cases trên toàn hệ thống server backend (`mvnw.cmd test`), không phát sinh bất kỳ lỗi hồi quy nào.

---

## Sprint 3: Lưu Trữ Lịch Sử, GPS & Danh Mục Phác Đồ (Tuần 5 - 6)
**Mục tiêu Sprint:** Quản lý dữ liệu bệnh, phác đồ điều trị và lưu vết lịch sử chẩn đoán.

- [x] **Task 3.1: API Lưu Vết Lịch Sử Chẩn Đoán Kèm GPS** (8h)
  - Liên kết danh tính người dùng (`User`) và tọa độ địa lý (`latitude`, `longitude`, `deviceInfo`) vào bản ghi `Diagnosis` khi gọi `POST /api/v1/diagnoses/analyze`.
  - Phân tách khóa bộ đệm `DiagnosisIdempotencyService` theo tổ hợp `(userId + ":" + imageSha256)` để bảo vệ quota AI mà vẫn đảm bảo tính toàn vẹn lịch sử riêng cho từng user.
  - Phân tách kiến trúc đọc/ghi (`/solid` CQRS-lite & ISP): Giữ `DiagnosisServiceImpl` (< 120 LOC) chỉ thực hiện Command; tạo `DiagnosisQueryService` và `DiagnosisQueryServiceImpl` chuyên trách truy vấn lịch sử và chi tiết.
  - Viết endpoint `GET /api/v1/diagnoses`: Phân trang `PageResponse<DiagnosisSummaryDto>`, hỗ trợ lọc động qua `DiagnosisSpecifications` (`plantCode`, `diseaseCode`, `status`, khoảng thời gian `fromDate`/`toDate`), tối ưu truy vấn nạp sẵn quan hệ tránh N+1 Query.
  - Viết endpoint `GET /api/v1/diagnoses/{id}`: Trả về đầy đủ `DiagnosisAnalyzeResponse`, tái tạo hệ tọa độ kép Pixel + Normalized qua `CoordinateTransformer`, danh sách bệnh đồng nhiễm `coOccurringDiseases`, và phác đồ điều trị `recommendedTreatments`.
  - Viết endpoint `DELETE /api/v1/diagnoses/{id}`: Xóa mềm bản ghi (`deleted_at = Instant.now()`), bảo toàn bia mộ Tombstone cho AI Active Learning và Sync Engine Phase 3.
  - Bảo mật Zero-Trust chống lỗ hổng IDOR/BOLA: Mọi endpoint trên `/api/v1/diagnoses/**` yêu cầu JWT Bearer Token và chỉ cho phép người dùng truy xuất/xóa dữ liệu của chính mình (trả về 404 Not Found nếu truy cập bản ghi của user khác).
  - Viết bộ kiểm thử tự động toàn diện: `DiagnosisQueryServiceImplTest` (6 unit tests), `DiagnosisIdempotencyServiceTest` (bổ sung test evict theo ID), và `DiagnosisHistoryIntegrationTest` (7 integration tests bao gồm lọc đa tiêu chí và vòng đời xóa mềm/cache).
  - *Acceptance Criteria:* Pass 100% toàn bộ 110/110 test cases trên toàn hệ thống server backend (`./mvnw test`).

- [ ] **Task 3.2: CRUD Danh Mục Bệnh & Phác Đồ Điều Trị Cho Web Admin** (8h)
  - Xây dựng các endpoints:
    - `GET /api/v1/diseases`
    - `POST /api/v1/diseases`
    - `PUT /api/v1/diseases/{id}`
    - `DELETE /api/v1/diseases/{id}`
    - `GET /api/v1/diseases/{id}/treatments`
  - *Acceptance Criteria:* Hỗ trợ Web Admin (Huy) thêm, sửa, xóa danh mục bệnh và tài liệu phác đồ.

- [ ] **Task 3.3: Seed Data Mẫu Chuẩn Cho Demo** (4h)
  - Viết script SQL hoặc `DataInitializer` nạp sẵn 10-15 bệnh cây trồng phổ biến (lúa, cà chua, ngô, khoai tây) kèm phác đồ mẫu tiếng Việt.
  - *Acceptance Criteria:* Chạy lại ứng dụng tự động có sẵn dữ liệu mẫu trong DB.

---

## Sprint 4: Bảo Mật, Rate Limiting & Tối Ưu Triển Khai (Tuần 7 - 8)
**Mục tiêu Sprint:** Bảo vệ API, tránh lãng phí quota AI và đóng gói Docker chuẩn hóa.

- [ ] **Task 4.1: Xác Thực JWT & Phân Quyền (Spring Security)** (8h)
  - Cấu hình Spring Security với JWT token.
  - Phân quyền 2 vai trò: `USER` (Mobile app) và `ADMIN` (Web Admin).
  - *Acceptance Criteria:* Endpoints quản lý bệnh yêu cầu quyền ADMIN; endpoint chẩn đoán yêu cầu USER hoặc public token.

- [ ] **Task 4.2: Rate Limiting Chống Spam Upload Ảnh** (5h)
  - Áp dụng Bucket4j hoặc logic kiểm soát tần suất request theo IP/User (tối đa 10 request/phút).
  - *Acceptance Criteria:* Gửi request thứ 11 liên tiếp trả về HTTP 429 Too Many Requests.

- [ ] **Task 4.3: Dockerfile & CI/CD Pipeline (GitHub Actions)** (7h)
  - Viết Dockerfile đa tầng (Multi-stage build) tối ưu dung lượng image Spring Boot (< 200MB).
  - Viết `.github/workflows/backend-ci.yml` tự động build và chạy test khi có commit/PR vào nhánh `main`.
  - *Acceptance Criteria:* Pipeline GitHub Actions chạy pass toàn bộ test case.

---

## Sprint 5: Nghiệm Thu, Tài Liệu Đồ Án & Hỗ Trợ Demo (Tuần 9 - 10)
**Mục tiêu Sprint:** Kiểm thử toàn diện, viết thuyết minh đồ án và chuẩn bị môi trường demo.

- [ ] **Task 5.1: Deploy Server Lên Môi Trường Thực Tế (VPS / Cloud)** (6h)
  - Deploy Docker Compose (Spring Boot + PostgreSQL) lên VPS (DigitalOcean / Oracle Free / EC2).
  - Cấu hình Nginx reverse proxy và chứng chỉ SSL Let's Encrypt (HTTPS bắt buộc để Mobile gọi được).
  - *Acceptance Criteria:* Mobile gọi được API qua domain HTTPS công khai.

- [ ] **Task 5.2: Viết Tài Liệu Kỹ Thuật Đồ Án (Chương Kiến Trúc & CSDL)** (10h)
  - Vẽ sơ đồ kiến trúc phân tán (Component Diagram, Deployment Diagram).
  - Vẽ sơ đồ thực thể liên kết (ERD) và từ điển dữ liệu (Data Dictionary).
  - Thống kê benchmark độ trễ xử lý API chẩn đoán.
  - *Acceptance Criteria:* Hoàn thành nội dung Word nộp cho giảng viên hướng dẫn.

- [ ] **Task 5.3: Chuẩn Bị Kịch Bản Demo Phòng Thi** (4h)
  - Chuẩn bị sẵn tài khoản admin, dữ liệu chẩn đoán mẫu và kịch bản dự phòng khi mất kết nối mạng.
  - *Acceptance Criteria:* Diễn tập kịch bản demo trơn tru cùng Huy.
