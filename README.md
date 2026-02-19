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
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean install # -DskipTests
```
### Oppsett med lokal database-instans og WireMock API-simulering

Rask oppsett uten å være avhengig av eksterne tjenester.

Ved lokal kjøring brukes Spring-boot-instansen 
[BidragReisekostnadApiLokalTestapplikasjon](src/test/java/no/nav/bidrag/reisekostnad/BidragReisekostnadApiLokalTestapplikasjon.java).
For lokal kjøring må Spring-profil settes til enten <b>lokal-h2</b> eller 
<b>lokal-postgres</b> avhengig av hvilken database-teknologi én ønsker å 
teste med. Til <b>lokal-postgres</b>-profilen kreves det en lokal Postgres-instans.

#### Lokal H2 instans

Eksempel på kommando for å starte applikasjonen lokalt med H2-database:
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test-compile exec:java \
-Dexec.mainClass="no.nav.bidrag.reisekostnad.BidragReisekostnadApiLokalTestapplikasjon" \
-Dexec.classpathScope=test \
-Dspring.profiles.active=lokal-h2
```

H2-databasen er satt opp in-memory og kan enklest nås på 
http://localhost:8080/h2-console/login.jsp med "jdbc:h2:mem:default" JDBC 
URL og blankt passord.

#### Lokal Postgres instans

Installasjon av Postgres via Homebrew på MacOS:
```bash
brew install postgresql
```
Oppstart av Postgres-tjenesten:
```bash
brew services start postgresql
```

#### Oppsett av test-token og API-simulering

[BidragReisekostnadApiLokalTestapplikasjon](src/test/java/no/nav/bidrag/reisekostnad/BidragReisekostnadApiLokalTestapplikasjon.java)
er satt opp til å bruke et test-token generert av [token-support](https://github.com/navikt/token-support), og
benytter wiremock til `/bidrag-person/motpartbarnrelasjon` og 
`/bidrag-person/informasjon`, dvs. for å simulere endepunktene til 
bidrag-person appen.

##### Teste brukerinformasjon endepunktet i lokal Swagger

1. Generer et test-token for _tokenx_ issuer og _aud-localhost_ audience for å 
sette tokenet i en cookie som Swagger kan bruke for autentisering:

    > Generer token for Gråtass bruker
    >
    > http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost&subject=12345678910

    > Generer token for Råtass bruker
    >
    > http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost&subject=55555678910

    > Generer token for Streng
    >
    > http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost&subject=11111122222

2. Autentiser som én av ovenfor valgte brukerne, se etter Authorize-knappen 
øverst til høyre i Swagger UI og lim inn det genererte tokenet:
    > http://localhost:8080/swagger-ui/index.html

3. Test ut endepunktet for _brukerinformasjon_ i Swagger.

##### Teste forespoersel/ny endepunktet i lokal Swagger

Følg steg 1 og 2 i forrige seksjon for å generere token og autentisere i 
Swagger UI. Se etter "fellesBarn" og "ident" til innlogget testperson 
i test/java/resources/mapping/bidrag-person-relasjon-*****.json. Test ut 
endepunktet for _forespørsel/ny_ i Swagger.

Tips:
Problem ved lokal kjøring fra Intellij:
_Finner ikke sti til wiremock-mappings under classpath:/mappings_.
Løsning:
Verifiser at test/resources har type 'Test Resources' i 'project structure'.

## Oppsett av lokalt utviklingsmiljø mot sky

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