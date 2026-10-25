#!/bin/bash
# 会话韧性消融验证：每轮 登录拿新会话 → 并发打 5 个首页接口 → 15ms 后删除该会话 key
# 预期（修复后）：全部响应无 500；日志出现 "Session key vanished" WARN；无 "Session was invalidated" ERROR
cd "C:\Users\Administrator\Desktop\smart class\scripts"
LOG=runtime-run.log
REDIS="docker exec redbook-redis redis-cli -a redis123 -n 1 --no-auth-warning"
endpoints=(daily-articles/today daily-words/today course/recommend ai-avatars/page announcements/page)

for cycle in 1 2 3 4 5 6 7 8; do
  uuid=$(curl -s http://localhost:12345/api/captcha | sed 's/.*"uuid":"\([^"]*\)".*/\1/')
  ccode=$($REDIS GET "captcha:$uuid")
  curl -s -c /tmp/ck_$cycle.txt -o /dev/null -X POST http://localhost:12345/api/user/login \
    -H "Content-Type: application/json" \
    -d "{\"userAccount\":\"demo_admin\",\"userPassword\":\"12345678\",\"captchaUuid\":\"$uuid\",\"captchaCode\":\"$ccode\"}"
  kuuid=$(grep SESSION /tmp/ck_$cycle.txt | awk '{print $7}' | base64 -d)
  key="spring:session:sessions:$kuuid"
  exists=$($REDIS EXISTS "$key")
  for i in 1 2; do
    for p in "${endpoints[@]}"; do
      curl -s -o /dev/null -w "%{http_code}\n" -b /tmp/ck_$cycle.txt "http://localhost:12345/api/$p" &
    done
  done > /tmp/codes_$cycle.txt &
  burstpid=$!
  sleep 0.015
  $REDIS DEL "$key" >/dev/null
  wait $burstpid
  codes=$(sort /tmp/codes_$cycle.txt | tr '\n' ' ')
  echo "cycle $cycle keyexisted=$exists codes: $codes"
done
echo ====LOG-CHECK====
echo -n "swallow-warn count: "; grep -c "Session key vanished during request processing" $LOG
echo -n "invalidated-error count: "; grep -c "Session was invalidated" $LOG
