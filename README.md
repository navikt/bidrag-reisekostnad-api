# bidrag-reisekostnad-api
Løsning for innhenting av samtykke til opprettelse av sak for fordeling av reisekostnader mellom foreldre

### Lokal kjøring
Ved lokal kjøring brukes Spring-boot-instansen BidragReisekostnadApiLocalTestapplikasjon. Denne er satt opp med token-supports test-token, og bruker 
h2 som database. Spring profil lokal må brukes ved lokal kjøring. Det er satt opp wiremock av bidrag-person-endepunktene /motpartBarnRelasjon og /informasjon.

Flyt for testing av endepunktene brukerinformasjon
    1. Generere test-token for issuer tokenx og legge dette til nettleser cookie som Swagger kan bruke: 
        http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost
    2. Trykk execute for brukerinformasjon-endepunktet

Flyt for tedsting av forespoersel/ny-endepunktet fra lokal Swagger:
    1. Generere test-token for issuer tokenx og legge dette til nettleser cookie som Swagger kan bruke:
        http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost
    2. Se test/java/resources/mapping/bidrag-person-*****.json for testpersoner som figurerer i eksisterende bidrag-person-mocker.
    3. Bruk personidentene fra steg 2 til å opprette ny forespørse.

#### Ressurser
 - Swagger-ui: http://localhost:8080/swagger-ui/index.html
 - Sette cookie med tokenx som issuer for Swagger: http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost
 - h2-konsoll: http://localhost:8080/h2-console/login.jsp (passord blankt)

