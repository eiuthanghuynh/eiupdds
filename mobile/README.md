# EIU Plant Disease Detection System (EIU PDDS) — Mobile Client

Ứng dụng di động chẩn đoán bệnh cây trồng dành cho nông dân, phát triển bằng **React Native** và **Expo Dev Client**.

## 🛠️ Công Nghệ Sử Dụng
* **Framework:** React Native (Expo SDK 57, React 19)
* **Routing:** Expo Router (File-based navigation)
* **Styling:** NativeWind (TailwindCSS v4), Gluestack UI
* **Icons & Animation:** Lucide Icons, React Native Reanimated
* **Hardware & Media:** `expo-camera`, `expo-image-picker`, `react-native-svg` (dành cho Bounding Box)

---

## 🚀 Hướng Dẫn Cài Đặt & Khởi Chạy

### 1. Cài đặt Dependencies
```bash
cd mobile
npm install
```

### 2. Thiết lập Biến Môi Trường (API Backend)
Sao chép file `.env.example` thành `.env`:
```bash
cp .env.example .env
```
Cấu hình địa chỉ IP Backend Spring Boot:
* **Máy ảo Android (Emulator):** `EXPO_PUBLIC_API_URL=http://10.0.2.2:8080/api/v1`
* **Máy ảo iOS (Simulator):** `EXPO_PUBLIC_API_URL=http://localhost:8080/api/v1`
* **Điện thoại thật (Physical Device qua Wi-Fi LAN):** `EXPO_PUBLIC_API_URL=http://<IP_LAN_MAY_TINH>:8080/api/v1`

### 3. Khởi chạy Ứng dụng
```bash
# Khởi chạy Metro Bundler
npx expo start

# Hoặc chạy trực tiếp trên Android Dev Client
npm run android

# Kiểm tra lỗi Typescript
npx tsc --noEmit
```

---

## 📁 Cấu Trúc Thư Mục
* `src/app/`: Màn hình điều hướng (Camera, History, Profile/Settings, Home)
* `src/components/`: Reusable components & UI layout
* `src/context/`: ThemeContext (Dark mode), LanguageContext (Đa ngôn ngữ)
* `src/services/` & `src/api/`: Service kết nối REST API Core Backend
* `assets/`: Hình ảnh, icons, splash screen
