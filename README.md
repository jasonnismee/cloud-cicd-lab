# Cloud CI/CD Lab — Spring Boot, Docker, GitHub Actions, AWS EC2

Dự án portfolio DevOps Intern: một Spring Boot REST API lưu dữ liệu vào PostgreSQL, được đóng gói bằng Docker và tự động test, publish, deploy từ GitHub lên AWS EC2 thông qua OIDC + AWS Systems Manager (SSM).

**Repository:** https://github.com/jasonnismee/cloud-cicd-lab  
**Trạng thái xác nhận 31/10/2026:** 3 job GitHub Actions `Test and Build`, `Publish to GHCR`, `Deploy to EC2 via SSM` đã chạy xanh; ứng dụng và DB đã chạy trên EC2, endpoint health trả `UP`, HTTP public qua Nginx hoạt động.

## 1. Kiến trúc

```text
Developer push to main
        |
        v
GitHub Actions
  1. Test and Build: Maven verify + Docker build
  2. Publish to GHCR: latest & sha-<commit>
  3. Deploy: GitHub OIDC -> AWS STS/IAM -> SSM SendCommand
                                                  |
                                                  v
                                       EC2 Ubuntu + SSM Agent
                                                  |
                                                  v
                                          deploy/deploy.sh
                                                  |
                                                  v
                                     Docker Compose: app + db

Internet client -> EC2 Security Group port 80 -> Nginx -> 127.0.0.1:8080
                                                          |
                                                          v
                                                     Spring Boot
                                                          |
                                                          v
                                                   PostgreSQL (volume)
```

## 2. Công nghệ

- Java 21, Spring Boot 4.1.2, Maven, Spring Data JPA, Actuator.
- PostgreSQL 17 Alpine, Docker multi-stage, Docker Compose.
- GitHub Actions, GitHub Container Registry (GHCR), image tags `latest` và `sha-<commit>`.
- AWS EC2 Ubuntu, EC2 Security Group, IAM roles, GitHub OIDC, AWS STS, Systems Manager Run Command, SSM Agent.
- Nginx Reverse Proxy (HTTP port 80).

## 3. Cấu trúc các file chính

| File | Vai trò |
|---|---|
| `Dockerfile` | Build JAR bằng Maven ở stage 1, chạy ứng dụng bằng JRE 21 ở stage 2 |
| `compose.yaml` | Chạy app + DB ở local, build image từ source |
| `deploy/compose.yaml` | Trên EC2 pull image `APP_IMAGE` đã publish và chạy app + DB |
| `deploy/deploy.sh` | Pull, `compose up`, đợi endpoint health `UP` |
| `deploy/nginx.conf` | Proxy cổng 80 vào app chỉ bind loopback:8080 |
| `.github/workflows/ci.yml` | Một workflow gồm 3 job test → publish → deploy |
| `src/main/resources/application.properties` | Datasource qua biến môi trường, Actuator, Hibernate JPA |

## 4. Chạy local bằng Docker Engine trong WSL2 Ubuntu

Máy phát triển thực tế: **Windows 10 + WSL2 Ubuntu**, đã cài Docker Engine và Docker Compose trực tiếp trong Ubuntu; **không yêu cầu Docker Desktop**.

```bash
cd ~/projects/cloud-cicd-lab
cp .env.example .env
# Đổi DB_PASSWORD trong .env; không commit mật khẩu
docker compose up --build -d
docker compose ps
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1:8080/api/tasks
```

Kiểm thử ghi dữ liệu:

```bash
curl -X POST http://127.0.0.1:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Deploy Spring Boot with Docker"}'
docker compose restart
# Chờ ứng dụng khởi động xong, rồi kiểm tra dữ liệu còn tồn tại
curl http://127.0.0.1:8080/api/tasks
```

Lưu ý: `docker compose down -v` xóa cả volume database, **không dùng khi cần giữ dữ liệu**.

## 5. Pipeline CI/CD

Workflow `.github/workflows/ci.yml` kích hoạt khi `push`/`pull_request` vào `main`.

- **test:** checkout code, Java 21, `mvn -B -ntp verify`, `docker build`. Chạy cho PR và push.
- **publish:** `needs: test`, chỉ chạy khi `push`, login GHCR bằng `GITHUB_TOKEN`, build/push `latest` và `sha-${{ github.sha }}`.
- **deploy:** `needs: publish`, `id-token: write` để dùng GitHub OIDC, assume `CloudCICDLabGitHubRole`, gửi SSM command đến `EC2_INSTANCE_ID`. SSM Agent thực thi `deploy.sh` với image SHA. Workflow theo dõi kết quả và thất bại nếu lệnh SSM thất bại.

**GitHub Secrets được workflow dùng:** `AWS_REGION`, `EC2_INSTANCE_ID`. Không lưu AWS access key tĩnh. Các secrets SSH từng dùng cho kế hoạch SSH deploy không còn cần thiết cho workflow OIDC/SSM hiện hành.

## 6. Chạy trên EC2

EC2 cần Docker Engine + Docker Compose plugin, Nginx, SSM Agent đang online với instance role có policy `AmazonSSMManagedInstanceCore`. File `/home/ubuntu/cloud-cicd-lab/deploy/.env` cần `DB_PASSWORD` và `APP_IMAGE` nếu chạy Compose thủ công.

```bash
cd /home/ubuntu/cloud-cicd-lab/deploy
sudo docker compose --env-file .env ps
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1/actuator/health
sudo docker inspect deploy-app-1 --format '{{.Config.Image}}'
```

Luồng request: client → Nginx HTTP/80 → `127.0.0.1:8080` → Spring Boot → `db:5432` (internal Compose). Không mở công khai cổng 8080/5432.

## 7. Kết quả đã kiểm chứng

- Local Docker Compose build, PostgreSQL healthy, Spring Boot health `UP`.
- POST task tạo `id=1`, GET đọc được và task vẫn tồn tại sau `docker compose restart`.
- GHCR publish thành công; application + DB chạy trên EC2, HTTP health qua Nginx hoạt động.
- CI/CD có ba job xanh, OIDC + SSM deploy thành công.

## 8. Giới hạn và hướng phát triển

Đây là **lab, chưa phải hệ thống production-ready**. Chưa cấu hình HTTPS và authentication; Hibernate `ddl-auto=update` vẫn dùng cho demo. Chưa có test rollback tự động, DB backup/restore, canary/blue-green, Kubernetes, Terraform, quan sát nâng cao, hoặc mô hình HA. Không lưu dữ liệu thật/nhạy cảm trong demo public.

Các hạng mục nâng cấp tùy chọn: TLS + domain, API authentication, Flyway migrations, database backup, integration tests, image scanning, GitHub environment approval, digest pinning, automated rollback.

## 9. Ứng tuyển & trình bày

Điểm cần nêu trong CV: đã xây dựng CI/CD gồm test/build/publish/deploy theo thứ tự; deploy Docker image lên EC2 qua AWS OIDC/SSM (không SSH từ GitHub runner, không access key tĩnh); kết nối container app–PostgreSQL và Nginx proxy, kiểm chứng persistence/health. Không mô tả những tính năng chưa triển khai như đã hoàn thành.
