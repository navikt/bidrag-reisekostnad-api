# Bidrag Reisekostnad

## bidrag-reisekostnad-api

Backend for å [fordele reisekostnader ved samvær med barn](https://www.nav.no/fordele-reisekostnader).

## Oppsett av lokalt utviklingsmiljø

Etter å ha lastet ned repoet kan én velge å kopiere [maven-settings.
xml](.m2/maven-settings.xml) over til `~/.m2/` for å få tilgang til de 
private maven-repositoriene som inneholder nødvendige avhengigheter for 
prosjektet. Det kreves GitHub konto tilknyttet navikt organisasjonen og et 
navikt autorisert Personal Access Token (PAT) med read:packages scope. Denne 
settings filen ble tidligere brukt ved CI, dvs. ved bygg på GitHub, men 
workflowene er omstrukturert til å bruke bidrag-workflow for å sammenstille 
med andre prosjekter til teamet.

Én kan sjekke om prosjektet får tak i nødvendige avhengigheter og bygger ved å 
kjøre følgende kommando i terminalen i root-mappen til prosjektet:
> JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean install -DskipTests

Ved lokal kjøring brukes Spring-boot-instansen 
[BidragReisekostnadApiLokalTestapplikasjon](src/test/java/no/nav/bidrag/reisekostnad/BidragReisekostnadApiLokalTestapplikasjon.java).
For lokal kjøring må Spring-profil settes til enten <b>lokal-h2</b> eller 
<b>lokal-postgres</b> avhengig av hvilken database-teknologi én ønsker å 
teste med. Til <b>lokal-postgres</b>-profilen kreves det en lokal Postgres-instans.

Eksempel på kommando for å starte applikasjonen lokalt med H2-database:
> JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test-compile exec:java \
-Dexec.mainClass="no.nav.bidrag.reisekostnad.BidragReisekostnadApiLokalTestapplikasjon" \
-Dexec.classpathScope=test \
-Dspring.profiles.active=lokal-h2

H2-databasen er satt opp in memory og kan enklest nås på 
http://localhost:8080/h2-console/login.jsp med "jdbc:h2:mem:default" JDBC 
URL og blankt passord.

BidragReisekostnadApiLokalTestapplikasjon er satt opp med
token-supports test-token, og benytter wiremock for
bidrag-person-endepunktene /motpartBarnRelasjon og /informasjon.

### Flyt for testing av endepunktene brukerinformasjon

     1. Generer test-token for issuer tokenx og legg dette til nettleser cookie som Swagger kan bruke: 
        http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost
     2. Trykk execute for brukerinformasjon-endepunktet

### Flyt for testing av endepunktene brukerinformasjon med Råtass som innlogget bruker

     1. Generere test-token for issuer tokenx og legge dette til nettleser cookie som Swagger kan bruke:
        http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost&subject=55555678910
     2. Trykk execute for brukerinformasjon-endepunktet

### Flyt for testing av forespoersel/ny-endepunktet fra lokal Swagger:

     1. Generere test-token for issuer tokenx og legge dette til nettleser cookie som Swagger kan bruke:
        http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost
     2. Se test/java/resources/mapping/bidrag-person-*****.json for testpersoner som figurerer i eksisterende bidrag-person-mocker.
     3. Bruk personidentene fra steg 2 til å opprette ny forespørse.

#### Ta i bruk lokal Postgres-instans

* Installere Postgres via Homebrew på MacOS:

  > brew install postgresql

* Starte Postgres-tjenesten:
  > brew services start postgresql

Problem ved lokal kjøring fra Intellij:
_Finner ikke sti til wiremock-mappings under classpath:/mappings_.
Løsning:
Verifiser at test/resources har type 'Test Resources' i 'project structure'.

#### Ressurser

- Swagger-ui: http://localhost:8080/swagger-ui/index.html
- Sette cookie med tokenx som issuer for
  Swagger: http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost

## Kjøre lokalt mot sky

Kjør følgende kommandoer fra terminalvinduet i root mappen til
`bidrag-reisekostnad-api` prosjektet:

```bash
# Logg inn i GCP
gcloud auth login --update-adc

# Still inn kubectl cluster til dev-gcp
kubectl config use-context dev-gcp

# Sett namespace til bidrag
kubectl config set-context --current --namespace=bidrag
```

```bash
# Eksporter variabler til src/main/resources/application-lokal-sky-secrets.properties slik at appen kan autentisere i dev-gcp.
# Filen application-lokal-sky-secrets.properties skal aldri committes til Git og skal slettes etter bruk.
# Filen application-lokal-sky-secrets.properties er lagt til i .gitignore for å unngå at den committes ved en feil.
export $(kubectl exec -n bidrag deployment/bidrag-reisekostnad-api -- printenv | grep -E 'AZURE_APP_CLIENT_ID|AZURE_APP_CLIENT_SECRET|TOKEN_X|BIDRAG_PERSON_URL|BIDRAG_DOKUMENT_URL|SCOPE|AZURE_OPENID_CONFIG_TOKEN_ENDPOINT|AZURE_APP_TENANT_ID|AZURE_APP_WELL_KNOWN_URL')
```

Kjør [BidragReisekostnadApiLokalSky](src/test/java/no/nav/bidrag/reisekostnad/BidragReisekostnadApiLokalSky.java).
Dette vil starte opp applikasjonen lokalt med `H2` database.

Api kall kan testes ved å først hente `reisekostnad_api_token` token fra
[https://bidrag-reisekostnad.ekstern.dev.nav.no/api/dev/session](https://bidrag-reisekostnad.ekstern.dev.nav.no/api/dev/session).
Testbruker hentes fra [Dolly](https://dolly.ekstern.dev.nav.no/).

Deretter kan tokenet brukes til å logge inn på swagger-ui
http://localhost:8080/swagger-ui/index.html og teste ut ulike api kall.