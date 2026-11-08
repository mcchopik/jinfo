# Тестирование Worker через curl

## Переменные окружения
WORKER_URL="https://mcprofiles.mcchopik.workers.dev"
USERNAME="TestPlayer"
OFFLINE_PUBLIC_KEY="<base64-encoded-ed25519-public-key>"
TIMESTAMP=$(date +%s000)
SIGNATURE="<base64-encoded-signature-of-username:timestamp>"

# 1. Авторизация лицензионного аккаунта
# serverId должен начинаться с "jinfo-" и быть минимум 32 hex символа
SERVER_ID="jinfo-$(openssl rand -hex 16)"
curl -X POST "$WORKER_URL/auth" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"serverId\":\"$SERVER_ID\"}"

# Ответ: {"token":"uuid.expiry.hmac","uuid":"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"}

# 2. Авторизация offline-аккаунта
# Сначала сгенерируйте ключи Ed25519 на клиенте, затем:
curl -X POST "$WORKER_URL/auth/offline" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"publicKey\":\"$OFFLINE_PUBLIC_KEY\",\"timestamp\":$TIMESTAMP,\"signature\":\"$SIGNATURE\"}"

# 3. Сохранение профиля (нужен токен из шага 1 или 2)
TOKEN="<token-from-auth>"
curl -X PUT "$WORKER_URL/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"data":"Привет, я {youtube} @mychannel и {twitch} mychannel"}'

# 4. Получение своего профиля
curl -X GET "$WORKER_URL/profile/<your-uuid>" \
  -H "Authorization: Bearer $TOKEN"

# 5. Пакетный запрос профилей (до 50 UUID)
curl -X POST "$WORKER_URL/profiles/batch" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"uuids":["uuid1","uuid2","uuid3"]}'

# 6. Удаление профиля
curl -X DELETE "$WORKER_URL/profile" \
  -H "Authorization: Bearer $TOKEN"

# 7. Жалоба на профиль
curl -X POST "$WORKER_URL/report" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"profileUuid":"target-uuid"}'

# Проверка ошибок:
# 400 - неверный запрос
# 401 - неверный/истекший токен
# 403 - доступ запрещён
# 404 - не найдено
# 429 - слишком часто (rate limit)
# 500 - ошибка сервера
# 502 - ошибка Mojang session server