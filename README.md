# Secure Dokument Service

Secure Dokument Service är en Spring Boot-baserad REST-tjänst för säker dokument- och ärendehantering i en multi-tenant-miljö. Applikationen använder JWT-baserad autentisering, rollbaserad åtkomstkontroll och strikt organisationsisolering för all dataåtkomst.

## Funktioner

- JWT-baserad autentisering via Spring Security OAuth2 Resource Server
- Multi-tenant-isolering med `organisation_id` från token
- Rollbaserad auktorisering med Spring Security authorities
- CRUD-flöden för dokument
- Ärendehantering med statusfiltrering
- Dev-profil med H2 och lokal JWT-generering
- Prod-profil med MySQL och extern OIDC issuer
- Actuator health endpoints för container- och plattformsdrift

## Teknikstack

| Del | Teknik |
|-----|--------|
| Språk | Java 21 |
| Byggverktyg | Maven 3.9+ |
| Ramverk | Spring Boot 4 |
| Säkerhet | Spring Security, OAuth2 Resource Server, JWT |
| Databas | H2 (dev), MySQL (prod) |
| Persistens | Spring Data JPA |
| Validering | Jakarta Validation |
| Drift | Dockerfile + OpenShift-manifest |

## Projektstruktur

```text
secure-document-service/
|- deployment.yaml
|- document-service/
|  |- Dockerfile
|  |- pom.xml
|  `- src/
|     |- main/java/se/exempel/sds/
|     |  |- controller/
|     |  |- domain/
|     |  |- dto/
|     |  |- exception/
|     |  |- repository/
|     |  |- security/
|     |  `- service/
|     `- main/resources/
`- README.md
```

## Kom igång lokalt

Kör kommandon från `document-service`.

### Krav

| Verktyg | Version |
|---------|---------|
| Java | 21 |
| Maven | 3.9+ |

### Starta applikationen

```bash
cd document-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Applikationen startar då med:

- H2 in-memory-databas
- H2-konsol aktiverad på `http://localhost:8080/h2-console`
- lokal dev-endpoint för JWT-generering på `POST /dev/token`

H2-inställningar:

- JDBC URL: `jdbc:h2:mem:sdsdev`
- Användare: `sa`
- Lösenord: tomt

### Kör tester

```bash
cd document-service
mvn test
```

### IntelliJ IDEA

Sätt VM option till:

```text
-Dspring.profiles.active=dev
```

## Profiler och konfiguration

### `dev`

Dev-profilen använder H2 och en lokal signeringshemlighet för att generera testtoken.

### `prod`

Prod-profilen använder MySQL och kräver följande miljövariabler:

| Variabel | Beskrivning |
|----------|-------------|
| `DB_URL` | JDBC-url till MySQL |
| `DB_USERNAME` | Databasanvändare |
| `DB_PASSWORD` | Databaslösenord |
| `OIDC_ISSUER_URI` | OIDC issuer för JWT-validering |

## Säkerhetsmodell

Applikationen bygger sin identitet från JWT-claims:

| Claim | Typ | Användning |
|-------|-----|------------|
| `sub` | String | användar-ID |
| `organisation_id` | String | tenant-/organisations-ID |
| `roles` | List\<String\> | mappas till Spring authorities |

### Viktiga regler

- `organisation_id` måste finnas i token, annars misslyckas autentisering
- roller mappas till authorities med prefixet `ROLE_`
- organisations-ID hämtas alltid från autentiserad principal, aldrig från request body
- data filtreras i repository-lagret för att upprätthålla tenant-isolering vid källan

## API-översikt

Alla `/api/**`-endpoints kräver Bearer-token.

### Dev endpoint

| Metod | Path | Beskrivning |
|-------|------|-------------|
| `POST` | `/dev/token` | Genererar JWT för lokal utveckling när `dev`-profilen är aktiv |

### Dokument

| Metod | Path | Beskrivning |
|-------|------|-------------|
| `GET` | `/api/dokuments` | Lista dokument för aktuell organisation |
| `GET` | `/api/dokuments/{id}` | Hämta ett dokument via ID |
| `GET` | `/api/dokuments/by-arende/{arendeId}` | Lista dokument kopplade till ett ärende |
| `POST` | `/api/dokuments` | Skapa dokument |
| `PUT` | `/api/dokuments/{id}` | Uppdatera dokument |
| `DELETE` | `/api/dokuments/{id}` | Radera dokument |

### Ärenden

| Metod | Path | Beskrivning |
|-------|------|-------------|
| `GET` | `/api/arenden` | Lista organisationens ärenden |
| `GET` | `/api/arenden?status=OPEN` | Filtrera öppna ärenden |
| `GET` | `/api/arenden?status=CLOSED` | Filtrera stängda ärenden |
| `GET` | `/api/arenden/{id}` | Hämta ett ärende via ID |
| `POST` | `/api/arenden` | Skapa ärende |
| `PUT` | `/api/arenden/{id}/close` | Stäng ett ärende |

## Exempel: lokal testkörning

### 1. Generera token

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/dev/token \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-1","organisationId":"org-a","roles":["USER","ADMIN"]}' \
  | jq -r .token)
```

### 2. Skapa dokument

```bash
curl -s -X POST http://localhost:8080/api/dokuments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","description":"x","classification":"PUBLIC"}'
```

### 3. Lista dokument

```bash
curl -s http://localhost:8080/api/dokuments \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Filtrera ärenden på status

```bash
curl -s "http://localhost:8080/api/arenden?status=OPEN" \
  -H "Authorization: Bearer $TOKEN"
```

## Felhantering

Applikationen använder central felhantering för att ge konsekventa HTTP-svar.

| Situation | Status | Exempel |
|-----------|--------|---------|
| saknad/ogiltig autentisering | `401 Unauthorized` | token saknas eller kan inte valideras |
| otillåten åtkomst | `403 Forbidden` | användaren saknar rätt roll |
| resurs saknas eller tillhör annan organisation | `404 Not Found` | dokument-ID finns inte eller är i annan tenant |
| ogiltigt request-innehåll | `400 Bad Request` | valideringsfel |
| ogiltig affärsregel / konflikt | `409 Conflict` | försök att stänga redan stängt ärende |

Notera att vissa svar är medvetet generiska för att undvika informationsläckage mellan organisationer.

## Deployment

Projektet innehåller:

- `document-service/Dockerfile` för containerbygge
- `deployment.yaml` för OpenShift/Kubernetes-liknande deployment

Deployment-manifestet använder:

- stateless repliker
- health probes via `/actuator/health/liveness` och `/actuator/health/readiness`
- secrets för databas och OIDC-konfiguration
- persistent volume för uppladdningskatalog

## Designprinciper

- Controllers hanterar HTTP och delegerar vidare
- Service-lagret innehåller affärslogik
- Repository-lagret ansvarar för dataåtkomst och tenant-filtrering
- Säkerhetskontext byggs från JWT och injiceras som `OrganisationPrincipal`

## Licens

Projektet distribueras under licensen i `LICENSE`.
