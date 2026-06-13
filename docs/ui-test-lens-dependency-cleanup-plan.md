# UI Test Lens dependency cleanup plan

## Environment blocker: Maven PKIX

`mvn -q -DskipTests compile` nie dochodzi do kompilacji Java. Maven zatrzymuje się na rozwiązywaniu standardowego pluginu:

- artifact: `org.apache.maven.plugins:maven-resources-plugin:3.4.0`
- repository: `https://repo.maven.apache.org/maven2`
- błąd: `certificate_unknown` / `PKIX path building failed`

`mvn -o -DskipTests compile` również nie dochodzi do kompilacji Java, ponieważ plugin nie jest dostępny w lokalnym cache:

- `Cannot access central ... in offline mode`
- `maven-resources-plugin:jar:3.4.0 has not been downloaded from it before`

Wniosek: obecny blocker jest środowiskowy, związany z truststore/proxy/certyfikatami JDK/Maven. Nie jest to jeszcze błąd kodu ani wynik przeniesienia layoutu. Po naprawie dostępu Maven trzeba ponownie uruchomić compile, bo dopiero wtedy ujawnią się realne błędy Java compile.

## A. Aktualne zależności produkcyjne

| Dependency/import | Gdzie występuje | Problem | Decyzja na etap 2 | Docelowa decyzja |
| --- | --- | --- | --- | --- |
| `org.seleniumhq.selenium:selenium-java` | większość klas w `src/main/java/utils/jsExecHelper/**` | Główna zależność obecnego adaptera Selenium. | Zostaje w POM. Wersja ujednolicona przez `${selenium.version}`. | Przenieść do `ui-test-lens-selenium`; `core` i `overlay runtime` nie powinny zależeć od Selenium bezpośrednio. |
| `org.projectlombok:lombok` / `lombok.Getter` | `JsOverlayDebug` | Użycie minimalne: `@Getter` dla `driver` i `config`. | Zostaje `provided`, żeby nie zmieniać kodu. | Zdecydować później: usunąć Lombok z publicznego API albo zostawić jako compile-only/provided. |
| `io.restassured.response.Response` | `api/ApiCallActions.java` | RestAssured nie jest w POM i nie powinien być główną zależnością core. | Nie dodawać do głównego POM. Znany blocker kompilacji po naprawie PKIX. | Wydzielić do `ui-test-lens-restassured` albo zastąpić neutralnym modelem response. |
| `utils.logs.LogWraper` | `OverlayWait.java`, `core/Guards.java` | Prywatna/projektowa zależność, niedostępna w publicznym POM. | Nie dodawać do POM. Znany blocker kompilacji po naprawie PKIX. | Zastąpić `UiTestLensLogger`/sinkami albo przenieść do prywatnego adaptera. |
| `utils.time.TimeStamp` | `OverlayWait.java` | Prywatny timestamp helper, niedostępny w publicznym POM. | Nie dodawać do POM. Znany blocker kompilacji po naprawie PKIX. | Zastąpić `Clock` + `DateTimeFormatter` albo event timestampem w loggerze. |
| `tests.fe.utils.contentassertions.ContentIssueCollector` | `OverlayContentAssertions.java` | Testowo-projektowa zależność, nieakceptowalna w publicznym core. | Nie dodawać do POM. Znany blocker kompilacji po naprawie PKIX. | Przenieść do examples/private adapter lub wyłączyć z pierwszego publicznego artifactu. |
| `utils.datetime.LocalDateTimeUtils` | `OverlayContentAssertions.java` | Prywatny date-time helper, nieakceptowalny w publicznym core. | Nie dodawać do POM. Znany blocker kompilacji po naprawie PKIX. | Zastąpić `Clock`/`ZoneId` w adapterze albo usunąć z publicznej biblioteki. |

## B. Brakujące lub prywatne zależności

### `utils.logs.LogWraper`

- Występuje w:
  - `src/main/java/utils/jsExecHelper/OverlayWait.java`
  - `src/main/java/utils/jsExecHelper/core/Guards.java`
- Blokuje compile: tak, po usunięciu blokera PKIX.
- Publicznie akceptowalne w bibliotece: nie jako dependency głównego artifactu.
- Decyzja: zostawić tymczasowo jako opisany blocker; nie dodawać do POM.
- Docelowo:
  - `OverlayWait` powinien raportować przez `UiTestLensLogger` lub `UiTestLensEventBus`,
  - adapter projektowego `LogWraper` powinien trafić do prywatnego modułu/integracji,
  - publiczne sinki: `InMemoryLogSink`, `HudLogSink`, `Slf4jLogSink`, `ConsumerLogSink`.

### `utils.time.TimeStamp`

- Występuje w:
  - `src/main/java/utils/jsExecHelper/OverlayWait.java`
- Blokuje compile: tak, po usunięciu blokera PKIX.
- Publicznie akceptowalne w bibliotece: nie.
- Decyzja: zostawić tymczasowo jako opisany blocker.
- Docelowo:
  - użyć `Clock`,
  - formatowanie przez `DateTimeFormatter`,
  - timestamp trzymać w `UiTestLensLogEntry`.

### `tests.fe.utils.contentassertions.ContentIssueCollector`

- Występuje w:
  - `src/main/java/utils/jsExecHelper/OverlayContentAssertions.java`
- Blokuje compile: tak, po usunięciu blokera PKIX.
- Publicznie akceptowalne w bibliotece: nie, bo wskazuje na konkretny projekt testowy.
- Decyzja: nie dodawać do POM.
- Docelowo:
  - przenieść do `ui-test-lens-examples`,
  - albo do prywatnego adaptera,
  - albo wyłączyć z pierwszej wersji publicznego artifactu.

### `utils.datetime.LocalDateTimeUtils`

- Występuje w:
  - `src/main/java/utils/jsExecHelper/OverlayContentAssertions.java`
- Blokuje compile: tak, po usunięciu blokera PKIX.
- Publicznie akceptowalne w bibliotece: nie.
- Decyzja: nie dodawać do POM.
- Docelowo:
  - zastąpić `Clock`/`ZoneId`,
  - albo pozostawić wyłącznie w prywatnym adapterze content assertions.

## C. RestAssured

RestAssured występuje w:

- `src/main/java/utils/jsExecHelper/api/ApiCallActions.java`
  - `import io.restassured.response.Response`
  - `if (result instanceof io.restassured.response.Response r)`
  - `callWithModalRA(...)`

Obecny kod używa `Response` tylko do:

- odczytu `statusCode()`,
- odczytu body przez `asString()`,
- wygodnej metody `callWithModalRA(...)`.

To oznacza, że integracja może zostać zastąpiona neutralnym modelem, np.:

- `UiTestLensApiResponsePreview`,
- pola: `statusCode`, `headersPreview`, `bodyPreview`, `durationMs`,
- adapter RestAssured jako osobny moduł `ui-test-lens-restassured`.

Decyzja na etap 2:

- nie dodawać RestAssured do głównego POM,
- zostawić `ApiCallActions` jako znany blocker kompilacji po naprawie Maven PKIX,
- nie przepisywać klasy w tym etapie, bo to byłby refaktor API/integracji.

Docelowa decyzja:

- przenieść `callWithModalRA` do `ui-test-lens-restassured`,
- w `ui-test-lens-api-overlay` zostawić neutralny `Callable<T>` i `Function<T, String>` albo jawny model preview.

## D. `LogWraper` / `TimeStamp`

`LogWraper` jest używany w:

- `OverlayWait` jako sink dla statusów waitów: success/warn/error,
- `Guards` do logowania wykrytych stron błędów.

`TimeStamp` jest używany w:

- `OverlayWait` do timestampu wpisu HUD log.

Rekomendowany kierunek:

- `UiTestLensLogger` jako publiczny kontrakt logowania,
- `UiTestLensEventBus` jako mechanizm emitowania zdarzeń (`wait.started`, `wait.finished`, `guard.tripped`, `hud.log`),
- `UiTestLensLogEntry` jako neutralny model wpisu logu,
- `Clock` w konfiguracji do testowalnych timestampów,
- `DateTimeFormatter` jako formatowanie na potrzeby HUD/exportu,
- sinki:
  - `InMemoryLogSink`,
  - `HudLogSink`,
  - `Slf4jLogSink`,
  - `ConsumerLogSink`.

Nie implementować tego w etapie 2. Najpierw trzeba ustalić minimalny model event/log i granicę między `core`, `overlay`, `selenium`.

## E. `ContentIssueCollector` / `LocalDateTimeUtils`

`OverlayContentAssertions` nie powinno być częścią publicznego core, bo:

- zależy od pakietu `tests.fe...`, czyli od konkretnego projektu testowego,
- wymusza prywatny model content issue,
- miesza overlay assertions z raportowaniem specyficznym dla jednego środowiska,
- używa prywatnego helpera czasu.

Rekomendacja:

- w pierwszej publicznej wersji nie włączać tej klasy do głównego artifactu,
- przenieść do `ui-test-lens-examples` jako przykład adaptera,
- albo do prywatnego modułu adaptera poza publiczną publikacją,
- w `ui-test-lens-assertions` zostawić neutralny model `AssertionSummary`/`OverlayAssertionResult` i punkt rozszerzeń dla consumerów failure events.

## Następny etap techniczny

Po naprawie Maven PKIX należy ponownie uruchomić:

```powershell
mvn -q -DskipTests compile
```

Spodziewane kategorie błędów kompilacji:

- brak RestAssured w `ApiCallActions`,
- brak `LogWraper` i `TimeStamp`,
- brak `ContentIssueCollector` i `LocalDateTimeUtils`,
- potencjalne braki zasobów JS dopiero runtime, nie compile-time,
- ewentualne problemy składni, jeśli build nie użyje Java 17.

