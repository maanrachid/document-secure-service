TOKEN=$(curl -s -X POST http://localhost:8080/dev/token \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-1","organisationId":"org-a","roles":["USER","ADMIN"]}' \
  | jq -r .token)


curl -s -X POST http://localhost:8080/api/dokuments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"Workflow document","description":"A document created during the workflow.","classification":"PUBLIC"}'


curl -s -X GET http://localhost:8080/api/dokuments \
  -H "Authorization: Bearer $TOKEN"


OPEN_ID=$(curl -s -X POST http://localhost:8080/api/arenden \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"Open arende","description":"An open arende."}' \
  | jq -r .id)


CLOSED_ID=$(curl -s -X POST http://localhost:8080/api/arenden \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"Initially closed arende","description":"This arende is closed immediately."}' \
  | jq -r .id)


curl -s -X PUT http://localhost:8080/api/arenden/$CLOSED_ID/close \
  -H "Authorization: Bearer $TOKEN"

curl -s -X GET "http://localhost:8080/api/arenden?status=OPEN" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X GET "http://localhost:8080/api/arenden?status=CLOSED" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X PUT http://localhost:8080/api/arenden/$OPEN_ID/close \
  -H "Authorization: Bearer $TOKEN"

curl -s -X GET "http://localhost:8080/api/arenden?status=CLOSED" \
  -H "Authorization: Bearer $TOKEN"