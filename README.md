# DCAT-AP-SE Processor

[![License: EUPL 1.2](https://img.shields.io/badge/License-European%20Union%20Public%20Licence%201.2-library?style=for-the-badge&&color=lightblue)](LICENSES/EUPL-1.2.txt)

[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/diggsweden/dcat-ap-se-processor/badge?style=for-the-badge)](https://scorecard.dev/viewer/?uri=github.com/diggsweden/dcat-ap-se-processor)

[![REUSE status](https://img.shields.io/reuse/compliance/github.com%2Fdiggsweden%2Fdcat-ap-se-processor?style=for-the-badge)](https://api.reuse.software/info/github.com/diggsweden/dcat-ap-se-processor)

[![publiccode.yml](https://img.shields.io/badge/publiccode.yml-library?style=for-the-badge&color=orange)](publiccode.yml)

DCAT-AP-SE Processor är ett verktyg för att automatisera framställningen av metadata enligt metadataspecifikationen [DCAT-AP-SE](https://docs.dataportal.se/dcat/sv/) , som används för att publicera information om datamängder och API:er på [Sveriges dataportal](https://www.dataportal.se/).
Verktyget tar en API-definition som indata och genererar en RDF-fil redo för skördning av dataportalen. Som indata stöds API-definitioner i formaten OpenAPI och RAML, samt separata metadatafiler på JSON-format för OAS2/RAML0.8 eller för API:er utan en formell definition. Detta verktyg är framtaget för att enkelt kunna integreras i en CI/CD-pipeline eller köras fristående.

## Innehållsförteckning

- [DCAT-AP-SE Processor](#dcat-ap-se-processor)
  - [Innehållsförteckning](#innehållsförteckning)
  - [Bakgrund](#bakgrund)
  - [Instruktioner för att komma igång snabbt](#instruktioner-för-att-komma-igång-snabbt)
  - [Användning](#användning)
  - [API-dokumentation](#api-dokumentation)
  - [Systemkomponenter](#systemkomponenter)
  - [Lägga till stöd för nya metadata i verktyget](#lägga-till-stöd-för-nya-metadata-i-verktyget)
  - [Arbetsprocess för att publicera api/er på dataportalen](#arbetsprocess-för-att-publicera-apier-på-dataportalen)
  - [Licens](#licens)
  - [Beroenden](#beroenden)
  - [Underhållare](#underhållare)
  - [Versioner](#versioner)
  - [Bidra](#bidra)

## Bakgrund

### Sveriges dataportal

[Sveriges dataportal](https://www.dataportal.se/) synliggör information om datamängder (d.v.s. metadata) där själva datamängderna och åtkomstpunkterna finns publicerade hos olika aktörer.
Detta sker genom att Sveriges dataportal automatiskt inhämtar, ”skördar”, informationen hos publicerande aktör.
En aktör kan vara både från offentlig och privat sektor samt från civilsamhället.

En förutsättning för att dataportalen ska kunna synliggöra bland annat information om datamängder och dess API:er,
är att aktörerna upprättar och publicerar informationen enligt en gemensam och standardiserad metadataspecifikation som är tillgänglig för skördning.
Den specifikationen heter [DCAT-AP-SE](https://docs.dataportal.se/dcat/sv/) och är speciellt framtagen för att passa Sveriges dataportal på grund av den är tillräckligt övergripande för
att passa inhämtning av metadata från olika typer av organisationer och datadomäner.
Det möjliggör en enhetlig beskrivning av datamängder för att förenkla insamling, sökning och presentation av data på Sveriges dataportal. Följande information innehåller mycket tekniska termer och begrepp. För att kunna tillgodogöra sig informationen bör därför någon med sådan typ av kompetens läsa instruktionen.

### Automatiserad process för framställning av metadata

I syfte att hjälpa producenter av metadata, som ska skördas till dataportalen, har detta verktyg tagits fram för att kunna införlivas i godtycklig CI/CD driven kedja eller köras separat.
Verktyget skapar en metadataspecifikation på RDF-format utifrån en API-definition alternativt separat metadatafil.
RDF är det språk som används för att uttrycka metadata om ting på webben. En central egenskap med RDF är att man använder webbadresser (URI:er) för att referera till ting i olika påståenden.

## Instruktioner för att komma igång snabbt

Bygg en container image från koden i detta repository.

När container startas finns ett webbaserat UI och ett REST API tillgängligt att använda efter behov.

### Docker

Kör med docker:

```text
docker build --no-cache -t "dcatprocessor" .
```

```text
docker run -it --rm -p 8080:8080 dcatprocessor:latest 
```

### Podman

Kör med Podman:

```text
podman build --no-cache -t "dcatprocessor" .
```

```text
podman run -it --rm -p 8080:8080 dcatprocessor:latest 
```

### Färdigbyggd image

Alternativt, finns det också en experimentell färdigbyggd image för att testa.

```text
docker run ghcr.io/diggsweden/dcat-ap-se-processor:latest
```

```text
podman run ghcr.io/diggsweden/dcat-ap-se-processor:latest
```

**ANVÄND INTE OVAN IMAGE I PRODUKTIONSYFTEN** - Den är endast avsedd för lokal test, och inga garantier lämnas på säkerhetsuppdateringar med mera.

## Användning

### Format på API-definitioner

Det finns stöd för formaten OpenAPI eller RAML.

- OpenAPI 3.x tillägg av metadata sker via extensions<br>
  Lägg till en extension x-dcat och underliggande metadata hanteras.
- RAML1.x tillägg av metadata sker via annotations<br>
  Definiera en annotationType och använd den sedan.
- OAS2.x och RAML0.8 tillägg av metadata sker via separat metadatafil på json format.

Verktyget kan användas på följande sätt.

### 1. Via UI

Starta docker container, öppna browser till <http://localhost:8080>

I gränssnittet finns en editor där det går att lägga till och redigera en/flera API-definitioner
innan de skickas in för generering. Det finns också en startmalls funktion — ett
formulär som hjälper till att skapa upp x-dcat-attribut i en API-definition.

När generering lyckas levereras resultatet som en RDF på sidan och en kvalitets rapport blir tillgänglig under fliken rapport. Om något går fel visas en felrapport.

### 2. Via anrop till REST gränssnitt

Anropa endpoints med valfritt verktyg. I utveckling har vi använt curl från git bash, t.ex:

```text
curl "http://localhost:8080/dcat-generation/files/?dir=<path-to-your-folder>"
```

Vid lyckat anrop kommer en XML med genererad RFD att returneras.

Jenkins pipeline exempel
[Jenkinsfile](docs/jenkinsfile).

### 3. Via CLI

Applikationen kan köras som kommandoradsverktyg för att konvertera en enskild fil eller en hel katalog.
När argument skickas med körs den i CLI-läge och ingen webbserver startas.

#### Flaggor

| Flagga | Värde | Beskrivning | Exempel |
|--------|-------|-------------|---------|
| `-f` | FIL | Konverterar en specifikationsfil (.raml, .yaml, .json) | `java -jar dcat-ap-se-processor-0.0.3-SNAPSHOT.jar -f api.yaml` |
| `-d` | KATALOG | Konverterar alla specifikationsfiler i en katalog | `java -jar dcat-ap-se-processor-0.0.3-SNAPSHOT.jar -d ./specs` |
| `-h` | - | Visa hjälpmenyn | `java -jar dcat-ap-se-processor-0.0.3-SNAPSHOT.jar -h` |

Endast en CLI-flagga kan användas per körning. Flaggorna `-f` och `-d` kräver ett värde i form av en fil- eller katalogsökväg, medan `-h` och `-help` används utan värde.

Ogiltiga argument, exempelvis okända flaggor, saknade värden eller flera CLI-flaggor, resulterar i exitkod 2.
Argument som börjar med `--`, t.ex. `--server.port=8081`, är Spring-inställningar och påverkar inte vilket läge applikationen startar i.

#### Användning

Bygg en java JAR fil.

```text
mvn clean package -DskipTests
```

Konvertera en specifikationsfil, resultatet blir en RDF samt en kvalitets rapport, båda ges som fil och visas via stdout:

```text
java -jar dcat-ap-se-processor-0.0.3-SNAPSHOT.jar -f FIL
```

Konvertera en katalog med specifikationsfiler, resultatet blir en RDF samt en kvalitets rapport, båda ges som fil och visas via stdout:

```text
java -jar dcat-ap-se-processor-0.0.3-SNAPSHOT.jar -d KATALOG
```

Visa hjälpmenyn med information om tillgängliga flaggor och exempel på användning:

```text
java -jar dcat-ap-se-processor-0.0.3-SNAPSHOT.jar -h
```

Hjälpmenyn kan även visas med -help. Båda alternativen avslutar programmet med exitkod 0.

Vid lyckad konvertering skrivs DCAT-datan till stdout och till filen `dcat.rdf` i aktuell katalog.
Om konverteringen misslyckas skrivs en felrapport till stderr och ingen fil skapas.

#### Exitkoder

| Kod | Betydelse |
|-----|-----------|
| 0 | DCAT-data genererad eller visad hjälpmeny |
| 1 | Oväntat internt fel |
| 2 | Felaktiga argument, t.ex. okänd flagga eller saknat värde |
| 3 | Indata kunde inte konverteras, felrapport skriven till stderr |

## API-dokumentation

Applikationen exponerar två endpoints för dokumentation och metadata om sitt API.

### Swagger UI

Interaktiv dokumentation för samtliga endpoints, inklusive parametrar,
svarsformat och möjlighet att anropa API:et direkt från webbläsaren.

```text
https://<host>/swagger-ui
```

Den underliggande OpenAPI-specifikationen finns på `/openapi.yaml`

### api-info

Metadata om API:et

```text
GET https://<host>/api-info
```

```json
{
  "apiName": "DCAT-AP-SE Processor API",
  "apiVersion": "0.1.0",
  "apiReleased": "2026-08-06",
  "apiDocumentation": "https://<host>/api-docs",
  "apiStatus": "beta"
}
```

`apiDocumentation` pekar som standard på den svarande instansens egen Swagger UI.
Hostas verktyget bakom en gateway eller med publik dokumentation på annan plats
kan länken sättas via `api-info.documentation` i `application.properties`.

## Systemkomponenter

Sekvensdiagram över flödet i verktyget. <br>
![img_1.png](docs/dcat-sekvens.png)

### PreprocessorController (REST API)

REST API:n för verktyget, följande två är de som finns att använda:

"/dcat-generation/files/" - Skickar man in directory (dir) som sedan skickas vidare till Managern för hantering. <br>

"/" - Är index sidan för Web-UI. Genom UI kan en eller flera filer skickas via endpointen "/dcat-generation/web/spec". Endpoint accepterar objektet SpecRequest vilket innehåller en array av ApiSource, se [openapi.yaml](src/main/resources/static/openapi.yaml) för schema beskrivning. Filerna skickas vidare till Managern.

Se kapitlet [API-dokumentation](#api-dokumentation) för att läsa mer om vilka endpoints som finns tillgängliga.

### Manager

Tar emot anrop från REST API't eller Cli och styr parsning, konvertering och uppskapande av RDF-data.

### ApiDefinitionParser

Använder [snakeYaml](https://bitbucket.org/asomov/snakeyaml) för att parsa RAML1.0 och OAS3 yaml strängen, som innehåller apidefinitionen, till JSONObject.

När det gäller OAS3 json format läses den strängen in som JSONObject direkt.

RAML0.8 och/eller OAS2 på json/yaml format stöder inte annotations/extensions, så där får användaren tillgång att lägga metadata i en separat fil på json format, som parsas till JSONObject.

### Converter

Använder MultiValuedMap (Apache Commons) och org.json.

För att konvertera mellan inläst metadata till element på DCAT-AP-SE format används konverteringsfiler (t.ex. TO_DCAT_OAS.json).<br>
I senare skeden kan man enkelt lägga till nya konverteringsfiler för andra format eller ändra till nyare versioner av befintliga format.

### RDFWorker

Använder RDF4J.

Tar emot en lista av Katalog objekt och skapar matchande RDF utifrån det.

## Lägga till stöd för nya metadata i verktyget

[Översikt över vad som finns och fungerar enligt DCAT-AP-SE spec](docs/DCATAPSE_completion.md)<br>
[Tillägg i Converter](docs/converter-tutorial.md)<br>
[Tillägg i RDFWorker](docs/rdfworker-tutorial.md)<br>
[specifikationsfil](src/main/resources/dcat-ap-se-301-bundle.json)

## Arbetsprocess för att publicera api/er på dataportalen

- [Skapa konto](https://docs.dataportal.se/accounts/) till de bakomliggande systemen för dataportal.se eller kontrollera [status](https://admin.dataportal.se/status/public) om er organisation redan finns upplagd. Upprätta sedan en skördningskälla, [Komma igång](https://docs.dataportal.se/registry/start/).<br>
- Inför metadata i apidefinitionen, eller skapa en separat metadatafil.<br>
- Använd verktyget för att generera en RDF fil.
- Skörda RDF filen, [Hantera organisationer och skördningskällor](https://docs.dataportal.se/registry/organisationer/#lagga-till-en-organisation).
- Verifiera att skördningen fungerar.
[Dataportalen docs](https://docs.dataportal.se/) har ingående information om hur skördningen fungerar samt hur en organisation sätter upp sin katalogkälla.

### Införa metadata

För att verktyget ska ges information att generera data behöver api definitionen uppdateras med metadata information.<br>
Detta repository innehåller [exempelfiler](src/main/resources/metadataExample) som visar hur metadata kan införas i apidefinition eller i separat metadatafil.<br>
[Attribut som stöds](docs/supported_attributes.md) finns listade med beskrivning.
Utgå från exempelfilerna och ta hjälp av [rekommendationer](https://docs.dataportal.se/dcat/docs/recommendations/) på dataportalen<br>

Det finns exempelfiler som visar hur obligatoriska, rekommenderade och valfria värden kan läggas in.<br>
När apidefinition/erna är uppdaterade med metadata görs de tillgängliga på en publikt nåbar folder, så att den separata pipeline som kör verktyget kan nå dem.

#### Om ni har ett API

Se exempel [enkel fil](src/main/resources/metadataExample/single), där Katalog elementet ligger i samma fil som resterande metadata.  
Exempel med obligatoriska och rekommenderade värden.<br>
[RAML](src/main/resources/metadataExample/single/full_example.raml)<br>
[OAS YAML](src/main/resources/metadataExample/single/full_example_oas.yaml)<br>
[OAS JSON](src/main/resources/metadataExample/single/full_example_oas.json)<br>

För API som inte har en definition (code-first) kan dataproducenten tillhandahålla separat metadata på json format<br>
[Separat JSON](src/main/resources/metadataExample/single/full_example.json)<br>

#### Om ni har flera API:er

Organisation med multipla API att producera RDF från använder en separat catalog.json fil för att hålla samman de ingående API:ernas metadata, se exempel under [multipla filer](src/main/resources/metadataExample/multiple).<br>
För att verktyget ska generera en sammanslagen RDF-fil krävs att organisationen skapar filer enligt följande:<br>
catalog.json - beskriver det övergripande katalog elementet och är samma för alla ingående apier.<br>
[catalog.json](src/main/resources/metadataExample/multiple/catalog.json) - Katalog elementet i separat fil på json format.<br>

Exempel på ingående API-definitioner innehållande metadata för DCAT-AP-SE<br>
[full_example.raml](src/main/resources/metadataExample/multiple/full_example.raml) - Api A på RAML format<br>
[full_example_oas.yaml](src/main/resources/metadataExample/multiple/full_example_oas.yaml) - Api B på OAS3 yaml format<br>
[full_example_oas.json](src/main/resources/metadataExample/multiple/full_example_oas.json) - Api C på OAS3 json format<br>
[full_example.json](src/main/resources/metadataExample/multiple/full_example.json), Api D, separat metadataspecifikation på json format<br>

## Licens

DCAT-AP-SE-Processor är licensierad under EUPL-1.2<br>
Se [licens](LICENSES/EUPL-1.2.txt) för mer information.

## Beroenden

Samtliga tredjepartsberoenden och deras licenser listas i [THIRD-PARTY.txt](docs/THIRD-PARTY.txt),
som genereras från `pom.xml`. För att uppdatera filen kör:

```text
mvn license:add-third-party
```

## Underhållare

Mjukvaran utvecklas av [DIGG](https://github.com/diggsweden) och Arbetsförmedlingen.

## Versioner

Aktuell version är 0.9.

Detta är en första version av verktyget. Arbetsförmedlingen och Bolagsverket kommer prova mjukvaran skarpt under hösten 2022. När mjukvaran fungerar för tillräckligt många offentliga organisationer kommer versionen uppdateras till 1.0.

## Bidra
