# lunch-recommendation
점심 메뉴 추천 서비스: 사용자의 위치와 선호도를 기반으로 다양한 점심 메뉴를 추천하는 Spring Boot 애플리케이션

## 프론트엔드 설정

### Google Maps API 키 설정

프론트엔드에서 Google Maps를 사용하기 위해 API 키가 필요합니다. 다음 중 하나의 방법으로 설정할 수 있습니다:

1. **로컬 환경변수 설정 (권장)**
   ```bash
   # Windows PowerShell
   $env:GOOGLE_MAPS_API_KEY="your_api_key_here"
   
   # Windows CMD
   set GOOGLE_MAPS_API_KEY=your_api_key_here
   
   # Linux/Mac
   export GOOGLE_MAPS_API_KEY=your_api_key_here
   ```

2. **.env 파일 생성**
   `frontend` 폴더에 `.env` 파일을 생성하고 다음 내용을 추가하세요:
   ```
   VITE_GOOGLE_MAPS_API_KEY=your_api_key_here
   ```

### 실행 방법

```bash
cd frontend
npm install
npm run dev
```
