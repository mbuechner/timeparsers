# timeparsers

Freitext rein, Datum raus. Ein kleines Labor zum Vergleich historischer und sprachbasierter Zeitparser.

## Was ist das?

Dieses Projekt ist eine Spring-Boot-Webanwendung, die verschiedene Parser gegeneinander antreten lässt und deren Ergebnisse direkt vergleichbar macht. Nämlich:

- DDB Timeparser
- DDB Timeparser New
- DDB Julian Day Mode
- Europeana Date Normaliser
- HeidelTime
- Duckling

Ziel: Datumsangaben wie _"14. Juli 1789"_ oder _"ab 1858"_ in maschinenlesbare Formen zu überführen.

## Tech-Stack

- Java 21
- Spring Boot 4
- Thymeleaf
- OkHttp
- HeidelTime + TreeTagger
- Docker Multi-Stage Build

## Schnellstart lokal

Voraussetzungen:

- Java 21
- Maven oder Maven Wrapper

Build:

```bash
./mvnw clean package -U
```

Start:

```bash
./mvnw spring-boot:run
```

Danach läuft die App standardmäßig auf:

- http://localhost:8080

## Docker

Image bauen:

```bash
docker build -t timeparsers .
```

Container starten:

```bash
docker run --rm -p 8080:8080 \
  -e DUCKLING_URL=http://host.docker.internal:8000/ \
  timeparsers
```

## API

### Request

```http
GET /parse?value=14. Juli 1789
```

### Beispiel mit curl

```bash
curl "http://localhost:8080/parse?value=14.%20Juli%201789"
```

### Antwortidee

Die Antwort enthält den Originalwert und die Ergebnisse der einzelnen Parser in einem gemeinsamen JSON-Objekt.

## Konfiguration

Wichtige Umgebungsvariable:

- `DUCKLING_URL` – URL zu einer erreichbaren Duckling-Instanz

Standard-Port:

- `8080`
