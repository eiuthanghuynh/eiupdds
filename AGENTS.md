# AGENTS.MD — DỰ ÁN EIU Plant Disease Detection System (EIU PDDS)

> **Dành cho mọi AI Coding Agent:**  
> Đọc kỹ file này để nắm bắt toàn bộ ngữ cảnh hệ thống, kiến trúc luồng dữ liệu, phân vai thành viên và các nguyên tắc phát triển trước khi bắt tay vào sinh mã nguồn.

---

## 1. Tổng Quan Dự Án & Chiến Lược 3 Phase
* **Tên chính thức dự án:** **EIU Plant Disease Detection System** (Tên viết tắt: **EIU PDDS**).
* **Quy ước tên gọi (BẮT BUỘC):** **TUYỆT ĐỐI KHÔNG** sử dụng các tên gọi cũ như "CropPulse" hay "PhytoGuard". Mọi package, repository, API, tài liệu, commit, comment và giao diện bắt buộc dùng định danh **EIU Plant Disease Detection System** hoặc **EIU PDDS**.
* **Tính chất kỹ thuật:** Đồ án tốt nghiệp Kỹ thuật Phần mềm (Software Engineering) tập trung vào phát triển hệ thống phân tán tích hợp thị giác máy tính ứng dụng (Applied Computer Vision).
* **Lộ trình tiến hóa 3 giai đoạn (Agile/Iterative Roadmap):**
  * **Phase 1 (Giai đoạn hiện tại - 10 tuần):** Xây dựng hệ thống End-to-End MVP hoàn chỉnh. Sử dụng Cloud Inference API có sẵn (Roboflow Serverless); tập trung vào luồng dữ liệu khép kín, backend proxy, quản lý lịch sử, cấu trúc API và trải nghiệm Mobile Client.
  * **Phase 2 (Tương lai):** Nghiên cứu nhúng mô hình AI trực tiếp vào thiết bị di động (On-device / Edge AI với TFLite hoặc ONNX) để tối ưu độ trễ và loại bỏ phụ thuộc API bên ngoài.
  * **Phase 3 (Tương lai):** Hiện thực hóa kiến trúc Offline-First chuyên sâu (Local DB, Sync Engine, Outbox Pattern, Exponential Backoff, bản đồ dịch tễ).

---

## 2. Phạm Vi & Ràng Buộc Kỹ Thuật Phase 1 (Core Constraints)
Agent khi tiếp nhận công việc **bắt buộc tuân thủ nghiêm ngặt** các giới hạn sau để tránh vỡ tiến độ:
1. **Không nhúng AI on-device trong Phase 1:** Không cài runtime nén mô hình (TFLite/ONNX) lên thiết bị di động lúc này.
2. **Không làm Offline-First phức tạp:** Ứng dụng yêu cầu có kết nối mạng (Internet Required) để thực hiện chẩn đoán. Chưa làm cơ chế đồng bộ ngoại tuyến phức tạp (Sync Engine / Outbox Pattern).
3. **Không huấn luyện mô hình từ đầu (Zero Custom Training):** Sử dụng Model Object Detection pre-trained chuẩn trên Roboflow: `plant-leaf-diseases/10` (mAP@50: 70.7%, Precision: 71.0%, Recall: 65.1%, 30 classes lá và bệnh) hoặc qua Workflow `plant-disease-yolo-onfen`.
4. **Luồng dữ liệu khép kín qua Backend Proxy (Bắt buộc):**
   * Mobile chụp ảnh $\rightarrow$ gửi Multipart lên Core Backend.
   * Core Backend gọi Roboflow Serverless API (Header: `Authorization: Bearer <ROBOFLOW_API_KEY>`).
   * Core Backend lưu ảnh vào Storage (MinIO/Cloudinary), lưu lịch sử vào PostgreSQL.
   * Core Backend trả về: Tọa độ Bounding Box, nhãn bệnh, độ tin cậy và phác đồ điều trị.
   * Mobile render Bounding Box đè lên ảnh chụp và hiển thị kết quả.

---

## 3. Cấu Trúc Monorepo Chuẩn Enterprise

```text
PROJECT_PlantDiseaseDetection/
├── server/                    # Backend Spring Boot 3.x (Java 21) - Phụ trách: Thắng
│   ├── src/main/java/com/eiu/pdds/
│   │   ├── controller/        # REST Endpoints (/api/v1/diagnoses)
│   │   ├── service/           # Business logic & interfaces
│   │   ├── repository/        # Spring Data JPA
│   │   ├── entity/            # PostgreSQL entities
│   │   ├── dto/               # Request / Response DTOs
│   │   ├── integration/       # RoboflowClient, StorageService
│   │   └── config/            # Swagger, Security, WebClient
│   └── pom.xml / Dockerfile
├── mobile/                    # Mobile App React Native (Expo Dev Client) - Phụ trách: Huy
│   ├── src/
│   │   ├── screens/           # Camera, Result, History
│   │   ├── components/        # BoundingBox Canvas / SVG overlay
│   │   ├── navigation/        # React Navigation
│   │   └── services/          # ApiClient
├── web/                       # Web Dashboard Next.js 14+ (App Router) - Phụ trách: Huy
│   ├── app/                   # Dashboard, Quản lý bệnh, Quản lý phác đồ
│   └── components/            # UI components (shadcn/ui / Tailwind)
├── ai-pipeline/               # Module Computer Vision & Roboflow SDK - Phụ trách: Thắng
│   ├── scripts/               # roboflow_workflow_client.py, test_workflow_smoke.py
│   └── datasets/              # Ảnh mẫu kiểm thử
├── infra/                     # Cấu hình hạ tầng
│   ├── docker-compose.yml     # PostgreSQL 15+, MinIO
│   └── init-scripts/          # seed_diseases.sql
├── .env                       # Chứa bí mật (ROBOFLOW_API_KEY...) - CẤM COMMIT
├── .gitignore
├── TASKS_THANG.md             # Backlog 5 Sprint của Thắng
├── TASKS_HUY.md               # Backlog 5 Sprint của Huy
└── AGENTS.md                  # Tài liệu ngữ cảnh hệ thống cho AI Agent
```

---

## 4. Cơ Chế Nhận Diện Vai Trò Thành Viên (Persona Detection)

Khi người dùng bắt đầu câu lệnh, Agent cần kiểm tra ngay lập tức cụm từ định danh:

### Khi người dùng là Thắng:
* **Từ khóa nhận diện:** `"Tôi là Thắng"`, `"Thắng đây"`, `"Vai trò: Thắng"`
* **Phạm vi trách nhiệm:** Backend (Spring Boot), Database (PostgreSQL), Docker/Infra, Cloud AI Proxy (Roboflow), Storage (MinIO/Cloudinary).
* **Nguồn công việc:** Đọc file [TASKS_THANG.md](file:///e:/PROJECT_PlantDiseaseDetection/TASKS_THANG.md).
* **Hành vi bắt buộc:** Tập trung vào các thư mục `server/`, `ai-pipeline/`, `infra/`. Sau khi hoàn thành một task, tự động đánh dấu `- [x]` vào [TASKS_THANG.md](file:///e:/PROJECT_PlantDiseaseDetection/TASKS_THANG.md).

### Khi người dùng là Huy:
* **Từ khóa nhận diện:** `"Tôi là Huy"`, `"Huy đây"`, `"Vai trò: Huy"`
* **Phạm vi trách nhiệm:** Mobile Client (React Native + Expo Dev Client), Thuật toán vẽ Bounding Box (SVG/Canvas), Web Admin (Next.js 14+), Trải nghiệm người dùng (UI/UX).
* **Nguồn công việc:** Đọc file [TASKS_HUY.md](file:///e:/PROJECT_PlantDiseaseDetection/TASKS_HUY.md).
* **Hành vi bắt buộc:** Tập trung vào các thư mục `mobile/`, `web/`. Sau khi hoàn thành một task, tự động đánh dấu `- [x]` vào [TASKS_HUY.md](file:///e:/PROJECT_PlantDiseaseDetection/TASKS_HUY.md).

---

## 5. Quy Tắc & Rào Cản Kỹ Thuật (Guardrails for Agents)
1. **Bảo mật bí mật:** Tuyệt đối không hardcode API key hoặc commit file `.env`. Đọc `ROBOFLOW_API_KEY` từ biến môi trường.
2. **Không can thiệp chéo mã nguồn:** Agent không được tự ý sửa đổi code của thành viên khác trừ khi đó là hợp đồng giao tiếp chung (API Contract / Swagger DTO).
3. **Phân bổ Sprint rõ ràng:** Mỗi task phải gắn liền với Sprint cụ thể (từ Sprint 1 đến 5). Không nhảy cóc sang tính năng của Sprint sau khi Sprint trước chưa hoàn thành Definition of Done.
4. **Tối ưu phản hồi (ADHD / Action-first):** Bắt đầu phản hồi bằng hành động/lệnh cụ thể có thể chạy được ngay; chia nhỏ bước việc rõ ràng; không nói vòng vo hoặc tóm tắt thừa thãi.
5. **Chuẩn hóa danh xưng & thương hiệu (CẤM NGOẠI LỆ):** Tên hệ thống là **EIU Plant Disease Detection System** (**EIU PDDS**). Tuyệt đối cấm sử dụng các danh xưng cũ "CropPulse", "PhytoGuard" trong mã nguồn, tên package Java (`com.eiu.pdds`), tài liệu, commit hay UI.
