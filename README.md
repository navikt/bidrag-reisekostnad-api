# bidrag-reisekostnad-api
Løsning for innhenting av samtykke til opprettelse av sak for fordeling av reisekostnader mellom foreldre

### Lokal kjøring
Ved lokal kjøring brukes Spring-boot-instansen BidragReisekostnadApiLocalTestapplikasjon. Denne er satt opp med token-supports test-token, og benytter 
wiremock for bidrag-person-endepunktene /motpartBarnRelasjon og /informasjon. For lokal kjøring må Spring-profil settes til enten <b>lokal-h2</b>,
 eller <b>lokal-postgres</b> avhengig av hvilken database-teknologi en ønsker å teste med. 

NB! <b>lokal-postgres</b>-profilen krever at en Postgres-instans er tilgjengelig lokalt. 

Flyt for testing av endepunktene brukerinformasjon

     1. Generere test-token for issuer tokenx og legge dette til nettleser cookie som Swagger kan bruke: 
        http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost
     2. Trykk execute for brukerinformasjon-endepunktet

Flyt for testing av endepunktene brukerinformasjon med Råtass som innlogget bruker

     1. Generere test-token for issuer tokenx og legge dette til nettleser cookie som Swagger kan bruke: 
        http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost&subject=55555678910
     2. Trykk execute for brukerinformasjon-endepunktet

Flyt for tedsting av forespoersel/ny-endepunktet fra lokal Swagger:
     
     1. Generere test-token for issuer tokenx og legge dette til nettleser cookie som Swagger kan bruke:
        http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost
     2. Se test/java/resources/mapping/bidrag-person-*****.json for testpersoner som figurerer i eksisterende bidrag-person-mocker.
     3. Bruk personidentene fra steg 2 til å opprette ny forespørse.

##### Tips og trix

* Postgres via Homebrew på MacOS:

   installere: 
    >brew install postgresql

    starte:
    >brew services start postgresql
* Ved lokal kjøring fra Intellij: Finner ikke sti til wiremock-mappings under classpath:/mappings
    >Verifiser at test/resources har type 'Test Resources' i 'project strukture'
  
#### Ressurser
 - Swagger-ui: http://localhost:8080/swagger-ui/index.html
 - Sette cookie med tokenx som issuer for Swagger: http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost
 - h2-konsoll: http://localhost:8080/h2-console/login.jsp (passord blankt)

#### Kjøre lokalt mot sky
For å kunne kjøre lokalt mot sky må du gjøre følgende

Åpne terminal på root mappen til `bidrag-reisekostnad-api`
Logg inn til GCP og konfigurer kubectl til å gå mot kluster `dev-gcp`
```bash
gcloud auth login --update-adc

# Sett cluster til dev-gcp
kubectx dev-gcp
# Sett namespace til bidrag
kubens bidrag 

# -- Eller hvis du ikke har kubectx/kubens installert 
# (da må -n=bidrag legges til etter exec i neste kommando)
kubectl config use dev-gcp
```
Deretter kjør følgende kommando for å importere secrets. Viktig at filen som opprettes ikke committes til git

```bash
kubectl exec --tty deployment/bidrag-reisekostnad-api printenv | grep -E 'AZURE_APP_CLIENT_ID|AZURE_APP_CLIENT_SECRET|TOKEN_X|BIDRAG_PERSON_URL|BIDRAG_DOKUMENT_URL|SCOPE|AZURE_OPENID_CONFIG_TOKEN_ENDPOINT|AZURE_APP_TENANT_ID|AZURE_APP_WELL_KNOWN_URL' > src/main/resources/application-lokal-sky-secrets.properties
```

Deretter holder det med å kjøre [BidragReisekostnadApiLokalSky](src/test/java/no/nav/bidrag/reisekostnad/BidragReisekostnadApiLokalSky.java)
Dette vil starte opp applikasjonen lokalt med `H2` database. 

Api kall kan testet ved å først hente token fra https://bidrag-reisekostnad.dev.nav.no/api/dev/session - `reisekostnad_api_token`. 
Du må logge inn med en fødselsnummer hentet fra https://dolly.nais-dev-fss.adeo.no -> Test Norge

Deretter kan tokenet brukes til å logge inn på swagger-ui http://localhost:8080/swagger-ui/index.html og teste ut ulike api kall