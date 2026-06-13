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
| `org.seleniumhq.selenium:selenium-java` | większość klas w `src/main/java/io/github/mmaciekk111/uitestlens/**` | Główna zależność obecnego adaptera Selenium. | Zostaje w POM. Wersja ujednolicona przez `${selenium.version}`. | Przenieść do `ui-test-lens-selenium`; `core` i `overlay runtime` nie powinny zależeć od Selenium bezpośrednio. |
| `org.projectlombok:lombok` / `lombok.Getter` | `JsOverlayDebug` | Użycie minimalne: `@Getter` dla `driver` i `config`. | Zostaje `provided`, żeby nie zmieniać kodu. | Zdecydować później: usunąć Lombok z publicznego API albo zostawić jako compile-only/provided. |
| `io.restassured.response.Response` | `api/ApiCallActions.java` | RestAssured nie jest w POM i nie powinien być główną zależnością core. | Nie dodawać do głównego POM. Znany blocker kompilacji po naprawie PKIX. | Wydzielić do `ui-test-lens-restassured` albo zastąpić neutralnym modelem response. |
| `utils.logs.LogWraper` | `OverlayWait.java`, `core/Guards.java` | Prywatna/projektowa zależność, niedostępna w publicznym POM. | Nie dodawać do POM. Znany blocker kompilacji po naprawie PKIX. | Zastąpić `UiTestLensLogger`/sinkami albo przenieść do prywatnego adaptera. |
| `utils.time.TimeStamp` | `OverlayWait.java` | Prywatny timestamp helper, niedostępny w publicznym POM. | Nie dodawać do POM. Znany blocker kompilacji po naprawie PKIX. | Zastąpić `Clock` + `DateTimeFormatter` albo event timestampem w loggerze. |
| `tests.fe.utils.contentassertions.ContentIssueCollector` | `OverlayContentAssertions.java` | Testowo-projektowa zależność, nieakceptowalna w publicznym core. | Nie dodawać do POM. Znany blocker kompilacji po naprawie PKIX. | Przenieść do examples/private adapter lub wyłączyć z pierwszego publicznego artifactu. |
| `utils.datetime.LocalDateTimeUtils` | `OverlayContentAssertions.java` | Prywatny date-time helper, nieakceptowalny w publicznym core. | Nie dodawać do POM. Znany blocker kompilacji po naprawie PKIX. | Zastąpić `Clock`/`ZoneId` w adapterze albo usunąć z publicznej biblioteki. |

## B. Brakujące lub prywatne zależności

### `utils.logs.LogWraper`

- Występuje w:
  - `src/main/java/io/github/mmaciekk111/uitestlens/OverlayWait.java`
  - `src/main/java/io/github/mmaciekk111/uitestlens/core/Guards.java`
- Blokuje compile: tak, po usunięciu blokera PKIX.
- Publicznie akceptowalne w bibliotece: nie jako dependency głównego artifactu.
- Decyzja: zostawić tymczasowo jako opisany blocker; nie dodawać do POM.
- Docelowo:
  - `OverlayWait` powinien raportować przez `UiTestLensLogger` lub `UiTestLensEventBus`,
  - adapter projektowego `LogWraper` powinien trafić do prywatnego modułu/integracji,
  - publiczne sinki: `InMemoryLogSink`, `HudLogSink`, `Slf4jLogSink`, `ConsumerLogSink`.

### `utils.time.TimeStamp`

- Występuje w:
  - `src/main/java/io/github/mmaciekk111/uitestlens/OverlayWait.java`
- Blokuje compile: tak, po usunięciu blokera PKIX.
- Publicznie akceptowalne w bibliotece: nie.
- Decyzja: zostawić tymczasowo jako opisany blocker.
- Docelowo:
  - użyć `Clock`,
  - formatowanie przez `DateTimeFormatter`,
  - timestamp trzymać w `UiTestLensLogEntry`.

### `tests.fe.utils.contentassertions.ContentIssueCollector`

- Występuje w:
  - `src/main/java/io/github/mmaciekk111/uitestlens/OverlayContentAssertions.java`
- Blokuje compile: tak, po usunięciu blokera PKIX.
- Publicznie akceptowalne w bibliotece: nie, bo wskazuje na konkretny projekt testowy.
- Decyzja: nie dodawać do POM.
- Docelowo:
  - przenieść do `ui-test-lens-examples`,
  - albo do prywatnego adaptera,
  - albo wyłączyć z pierwszej wersji publicznego artifactu.

### `utils.datetime.LocalDateTimeUtils`

- Występuje w:
  - `src/main/java/io/github/mmaciekk111/uitestlens/OverlayContentAssertions.java`
- Blokuje compile: tak, po usunięciu blokera PKIX.
- Publicznie akceptowalne w bibliotece: nie.
- Decyzja: nie dodawać do POM.
- Docelowo:
  - zastąpić `Clock`/`ZoneId`,
  - albo pozostawić wyłącznie w prywatnym adapterze content assertions.

## C. RestAssured

RestAssured występuje w:

- `src/main/java/io/github/mmaciekk111/uitestlens/api/ApiCallActions.java`
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

## Stage 3: Isolate external dependency blockers

W etapie 3 odcięto główny artifact od znanych zależności zewnętrznych/prywatnych bez package rename, bez multi-module split i bez zmiany runtime namespace.

### RestAssured

Wykonano:

- usunięto import `io.restassured.response.Response` z `ApiCallActions`,
- usunięto metodę convenience `callWithModalRA(...)` z głównego kodu,
- usunięto specjalne sprawdzanie `result instanceof io.restassured.response.Response`,
- pozostawiono neutralne `callWithModal(...)`, które przyjmuje `Callable<T>` i `Function<T, String>`.

Rozwiązanie tymczasowe:

- neutralny `callWithModal(...)` ustawia status `200`, tak jak dotychczas robił fallback dla wyników innych niż RestAssured `Response`,
- użytkownik może nadal przekazać `responsePreview`, ale nie ma już typu zależnego od RestAssured w głównym artifactcie.

Docelowo:

- adapter RestAssured powinien trafić do `ui-test-lens-restassured`,
- główny moduł API overlay powinien dostać neutralny model request/response preview, jeśli będzie potrzebny dokładny status/header/body bez zależności od klienta HTTP.

### `LogWraper`

Wykonano:

- dodano mały neutralny kontrakt `io.github.mmaciekk111.uitestlens.core.OverlayLogger`,
- dodano package-private noop implementację `NoopOverlayLogger`,
- `OverlayWait` używa teraz `OverlayLogger`,
- `Guards` używa teraz `OverlayLogger`,
- usunięto importy `utils.logs.LogWraper` z `src/main/java`.

Rozwiązanie tymczasowe:

- domyślny logger to `OverlayLogger.noop()`,
- nie dodano SLF4J ani projektowego loggera,
- nie zaimplementowano jeszcze pełnego event-busa.

Docelowo:

- `OverlayLogger` jest kandydatem na pomost do `UiTestLensLogger`,
- właściwy kierunek to `UiTestLensEventBus` + sinki: HUD, in-memory, console/consumer, SLF4J, TeamCity.

### `TimeStamp`

Wykonano:

- usunięto zależność od `utils.time.TimeStamp`,
- `OverlayWait` używa teraz `Clock.systemDefaultZone()` oraz `DateTimeFormatter`,
- dodano package-private konstruktor z `Clock`, żeby ułatwić przyszłe testy timestampów.

Rozwiązanie tymczasowe:

- timestamp HUD jest formatowany przez JDK jako `yyyy-MM-dd HH:mm:ss.SSS z`,
- semantyka waitów nie została zmieniona.

Docelowo:

- timestamp powinien być częścią neutralnego `UiTestLensLogEntry`,
- formatowanie powinno być odpowiedzialnością sinka/HUD/exportera.

### `OverlayContentAssertions`

Wykonano:

- `OverlayContentAssertions.java` usunięto z `src/main/java`,
- kod zachowano jako przykład referencyjny w `docs/examples/OverlayContentAssertions.java.example`,
- główny artifact nie importuje już `ContentIssueCollector` ani `LocalDateTimeUtils`.

Rozwiązanie tymczasowe:

- przykład nie jest kompilowany,
- prywatne zależności pozostają opisane jako private adapter/example.

Docelowo:

- jeśli adapter będzie potrzebny, powinien trafić do prywatnego modułu albo `ui-test-lens-examples`,
- publiczne assertions powinny emitować neutralne wyniki/failure events, które projekty downstream mogą mapować na własne collectory.

### Klasy nadal wymagające refaktoru

- `JsOverlayDebug` nadal jest dużą fasadą z wieloma odpowiedzialnościami.
- `ApiCallActions` nadal ma uproszczony model response i wymaga neutralnego modelu API overlay.
- `OverlayWait` ma już odcięty prywatny logger, ale nadal powinien docelowo emitować eventy zamiast pisać bezpośrednio do HUD/loggera.
- `Guards` ma neutralny logger, ale powinien docelowo emitować `guard.tripped`.
- Runtime JS nadal używa historycznych nazw `window.__selenium...`; namespace `window.__uiTestLens` jest odłożony na późniejszy etap.

## Stage 4: Minimal logging model

Dodano minimalny model logowania/event-busa w `io.github.mmaciekk111.uitestlens.core.logging`:

- `UiTestLensLogger`,
- `UiTestLensLogEntry`,
- `UiTestLensLogSink`,
- `UiTestLensLogLevel`,
- `UiTestLensEventType`,
- `UiTestLensStatus`,
- `TargetDescriptor`,
- `InMemoryLogSink`,
- `ConsoleLogSink`,
- `ConsumerLogSink`.

`OverlayLogger` nie jest już osobnym systemem logowania. Został uproszczony do internal bridge, który deleguje do `UiTestLensLogger`.

Decyzje:

- nie dodano SLF4J do core,
- nie dodano prywatnych loggerów projektowych,
- `ConsoleLogSink` jest opt-in i jako jedyny element core może pisać do `System.out`/`System.err`,
- HUD, Allure, TeamCity, eksportery i projektowe loggery powinny być później sinkami albo adapterami.

Szczegóły są w `docs/ui-test-lens-logging-model.md`.

## Stage 5: Wire logging model into overlay flow

Model logowania został minimalnie podpięty do istniejącego przepływu bez package rename, bez multi-module split i bez zmiany runtime namespace JS.

Wykonano:

- `OverlayLogger` dostał metodę `emit(UiTestLensLogEntry)` i pozostaje internal bridge do `UiTestLensLogger`,
- `OverlayWait` emituje eventy `WAIT`/`ERROR` dla startu, sukcesu, timeoutu i błędu waita,
- `Guards` emituje eventy dla `checkpoint(...)`: `GENERAL`/`PASSED` albo `ERROR`/`FAILED`,
- `JsOverlayDebug` emituje eventy `STEP`, `HUD` i `WAIT` równolegle do dotychczasowych aktualizacji HUD.

Decyzje odłożone:

- HUD nie jest jeszcze pełnym sinkiem,
- highlight/actions nie emitują jeszcze kompletnego modelu action events,
- adaptery SLF4J, Allure i TeamCity pozostają poza core,
- docelowy namespace `window.__uiTestLens` nadal jest odłożony na osobny etap.

## Stage 6: Instrument Selenium actions with logging events

Event-bus został podpięty do głównych akcji Selenium/overlay bez nowych zależności i bez zmiany publicznej fasady:

- `HighlightActions`,
- `TypingActions`,
- `SmartClickActions`,
- `SmartInputActions`,
- `ScrollActions`,
- `TargetResolverActions`,
- `JsOverlayDebug.smartUploadFile`.

Stare konstruktory klas akcji zostały zachowane. Nowe overloady przyjmują `OverlayLogger`, a `JsOverlayDebug` przekazuje ten sam logger do tworzonych akcji.

Celowo odłożono:

- pełny model `ASSERTION` w `AssertActions`,
- integrację przez `WebDriverListener`,
- adaptery Selenide/Allure/TeamCity,
- pełny opis targetu Selenium,
- eksportery logów.

## Stage 7: Instrument visual assertions with logging events

`AssertActions` został podpięty do `OverlayLogger` bez nowych zależności i bez zmiany publicznych metod asercji.

Wykonano:

- dodano overload konstruktora `AssertActions` z `OverlayLogger`,
- `JsOverlayDebug` przekazuje ten sam logger do `AssertActions`,
- centralne tworzenie `OverlayAssertionResult` emituje eventy `ASSERTION/PASSED` albo `ASSERTION/FAILED`,
- `assertGroup(...)` i `assertGroupReactSafe(...)` emitują summary eventy grupy,
- metadata `expected` i `actual` jest przycinana do 500 znaków tylko na potrzeby eventów.

Odłożono:

- osobny artifact `ui-test-lens-assertions`,
- integracje JUnit/TestNG/AssertJ,
- adapter `ContentIssueCollector`,
- eksport assertion report,
- przeniesienie lub rename `OverlayAssertionResult`.

## Current Maven coordinates

Aktualne single-module Maven coordinates po cleanupie groupId:

```text
io.github.mmaciekk111:ui-test-lens:1.0-SNAPSHOT
```

Java package pozostaje `io.github.mmaciekk111.uitestlens`.
