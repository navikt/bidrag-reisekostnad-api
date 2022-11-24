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

