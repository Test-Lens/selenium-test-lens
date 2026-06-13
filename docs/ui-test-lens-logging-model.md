# UI Test Lens logging model

## Cel

Model logowania UI Test Lens jest minimalnym fundamentem pod przyszły event-bus. Ma umożliwić emitowanie neutralnych zdarzeń z testów UI/browser automation bez wiązania core z konkretnym loggerem, frameworkiem raportowym albo narzędziem CI.

Docelowo zdarzeń będą mogły słuchać:

- HUD,
- overlay/highlight,
- historia in-memory,
- console sink,
- adapter SLF4J,
- istniejące loggery projektowe przez custom sink,
- eksportery JSON/TXT/HTML,
- adaptery Allure i TeamCity.

## Dlaczego nie `LogWraper`

`LogWraper` był prywatną zależnością projektu downstream. Główna biblioteka `ui-test-lens` nie powinna wymuszać prywatnego loggera ani pakietów spoza publicznego POM-a. Zamiast tego użytkownik może podpiąć własny logger przez `ConsumerLogSink`.

## Dlaczego nie SLF4J w core

Na tym etapie core nie dostaje zależności SLF4J. To utrzymuje główny artifact lekki i neutralny. Adapter SLF4J powinien powstać później jako osobny sink albo moduł, gdy granice `core`, `overlay` i `selenium` będą stabilniejsze.

## Modele

### `UiTestLensLogger`

Centralny logger/event-bus. Przyjmuje wiele sinków i emituje `UiTestLensLogEntry`. Jeśli sink rzuci wyjątek, logger go izoluje i nie przerywa testu.

Domyślny `UiTestLensLogger.noop()` nic nie robi. `ConsoleLogSink` nie jest włączony automatycznie.

### `UiTestLensLogEntry`

Niemutowalny wpis loga oparty o Java 17 record. Zawiera timestamp, poziom, typ zdarzenia, status, message, step, action, target, metadata i opcjonalny throwable.

### `UiTestLensLogSink`

Minimalny kontrakt sinka:

```java
void accept(UiTestLensLogEntry entry);
```

Sinki nie powinny pozwolić, żeby błąd logowania przerwał test. Dodatkowo `UiTestLensLogger` izoluje wyjątki sinków.

### `TargetDescriptor`

Neutralny opis celu akcji. Nie zależy od Selenium. Może opisywać selector, label, tagName, text i metadata.

### `InMemoryLogSink`

Przechowuje wpisy w pamięci i zwraca niemutowalną kopię przez `entries()`. Może służyć jako fundament eksportu JSON/TXT/HTML.

### `ConsoleLogSink`

Prosty sink na konsolę. `ERROR` idzie do `System.err`, pozostałe poziomy do `System.out`. Jest opt-in.

### `ConsumerLogSink`

Adapter do istniejącego loggera użytkownika bez zależności compile-time.

```java
UiTestLensLogger logger = UiTestLensLogger.builder()
        .sink(new ConsumerLogSink(entry -> existingProjectLogger.info(entry.message())))
        .build();
```

## Przykład historii in-memory

```java
InMemoryLogSink history = new InMemoryLogSink();

UiTestLensLogger logger = UiTestLensLogger.builder()
        .sink(history)
        .build();

logger.info("Opened checkout page");

List<UiTestLensLogEntry> entries = history.entries();
```

## Tymczasowy bridge `OverlayLogger`

Obecny `OverlayLogger` jest internal bridge używany przez istniejące klasy, takie jak `OverlayWait` i `Guards`. Deleguje do `UiTestLensLogger`, ale nie zastępuje jeszcze publicznego API fasady `JsOverlayDebug`.

W kolejnych etapach `OverlayLogger` może zostać zastąpiony bezpośrednim użyciem `UiTestLensLogger` albo `UiTestLensEventBus`, kiedy zostanie ustalony finalny podział modułów.

## Stage: Logger integration with existing overlay flow

Minimalna integracja podpina event-bus do obecnego przepływu bez zmiany package names, runtime namespace ani publicznych metod `JsOverlayDebug`.

Eventy emitują teraz:

- `OverlayWait`:
  - `WAIT` / `STARTED` na początku `until(...)`,
  - `WAIT` / `PASSED` po udanym wait,
  - `WAIT` / `FAILED` po timeout,
  - `ERROR` / `FAILED` po innym błędzie.
- `Guards`:
  - `GENERAL` / `PASSED` dla udanego `checkpoint(...)`,
  - `ERROR` / `FAILED` dla wykrytej strony błędu, bez zmiany tekstu rzucanego `AssertionError`.
- `JsOverlayDebug`:
  - `STEP` dla `setStep(...)`,
  - `HUD` dla `hudLog(...)`,
  - `WAIT` dla `waitHudStart(...)`, `waitHudStop(...)`, `showWaitIndicator(...)` i `hideWaitIndicator(...)`.

Domyślne zachowanie pozostaje `noop`. Użytkownik musi jawnie przekazać logger/sinki, żeby zbierać zdarzenia.

```java
InMemoryLogSink memorySink = new InMemoryLogSink();

UiTestLensLogger logger = UiTestLensLogger.builder()
        .sink(memorySink)
        .sink(new ConsoleLogSink())
        .build();

OverlayLogger overlayLogger = OverlayLogger.from(logger);
Guards guards = new Guards(driver, overlayLogger);

JsOverlayDebug overlay = new JsOverlayDebug(
        driver,
        config,
        apiPanel,
        apiCalls,
        guards,
        overlayLogger
);

overlay.setStep("Open checkout page");
overlay.hudLog("info", "Checkout opened", "2026-06-13 10:00:00.000 CEST");

List<UiTestLensLogEntry> entries = memorySink.entries();
```

Istniejący logger projektowy można podpiąć bez zależności compile-time:

```java
UiTestLensLogger logger = UiTestLensLogger.builder()
        .sink(new ConsumerLogSink(entry -> existingProjectLogger.info(entry.message())))
        .build();
```

Na później zostają:

- HUD jako pełny sink, zamiast równoległego emitowania z fasady,
- highlight jako sink/event consumer,
- adaptery Allure i TeamCity,
- pełne eventy akcji click/type/scroll,
- eksport JSON/TXT/HTML,
- docelowy podział modułów i ewentualne bezpośrednie użycie `UiTestLensLogger` zamiast bridge `OverlayLogger`.

## Stage: Action and highlight event instrumentation

Akcje Selenium/overlay zaczęły emitować eventy przez ten sam `OverlayLogger`, który jest bridge do `UiTestLensLogger`. Stare konstruktory klas akcji zostały zachowane; nowe overloady przyjmują `OverlayLogger`, a `JsOverlayDebug` przekazuje do nich własny logger.

Podpięte klasy:

- `HighlightActions`
  - emituje `HIGHLIGHT/STARTED` i `HIGHLIGHT/PASSED` dla `highlightClick`, `highlightParent`, `highlightClosest`,
  - emituje `ACTION/PASSED` dla kliknięcia wykonanego przez `highlightClick`,
  - przy ostatnim fallbacku kliknięcia emituje `ERROR/FAILED`.
- `TypingActions`
  - emituje `ACTION/STARTED`, `ACTION/PASSED`, `ERROR/FAILED` dla `typeWithHint` i `clearAndType`,
  - eventy nie zapisują pełnej wpisywanej wartości; metadata zawiera tylko `valueLength`.
- `SmartClickActions`
  - emituje `ACTION/STARTED`, `ACTION/PASSED`, `ERROR/FAILED` dla `clickWithOverlayHandling`,
  - metadata zawiera `fallback`, `fallbackType` i `popupHandled`, jeśli ścieżka weszła w fallback.
- `SmartInputActions`
  - emituje `ACTION/STARTED`, `ACTION/PASSED`, `ERROR/FAILED` dla `smartTypeWithHint`,
  - eventy nie zapisują pełnej wartości inputu; metadata zawiera tylko `valueLength`.
- `ScrollActions`
  - emituje `ACTION/STARTED`, `ACTION/PASSED`, `ERROR/FAILED` dla `scrollToElementWithArrow`,
  - metadata zawiera `durationMs`, `elementEdge`, `viewportEdge`, `withArrow`.
- `TargetResolverActions`
  - emituje diagnostyczne `ACTION/PASSED` lub `ERROR/FAILED` dla resolve targetów i selectorów,
  - `TargetDescriptor` jest nadal minimalny: selector, label albo `none()`.
- `JsOverlayDebug.smartUploadFile`
  - emituje `ACTION/STARTED`, `ACTION/PASSED`, `ERROR/FAILED`,
  - event nie zapisuje pełnej ścieżki pliku; metadata zawiera tylko `pathLength`.

`AssertActions` nie został w tym etapie przebudowany. Ma własny model `OverlayAssertionResult` i powinien dostać osobny etap dla pełnego modelu `ASSERTION`.

## Stage: Assertion and visual check event instrumentation

`AssertActions` został minimalnie podpięty do loggera/event-busa bez zmiany semantyki asercji i bez dodawania JUnit/TestNG/AssertJ. Stary konstruktor został zachowany, a nowy overload przyjmuje `OverlayLogger`. `JsOverlayDebug` przekazuje do `AssertActions` ten sam logger, którego używają pozostałe akcje.

Eventy emitują teraz wszystkie metody, które budują `OverlayAssertionResult`, w tym:

- text equals / contains,
- modified text equals / contains,
- attribute / CSS / color equals,
- class / visibility / enabled / selected checks,
- generic equals / notEquals / contains / notContains / true / false,
- null-element failure path.

Statusy:

- sukces: `ASSERTION` / `PASSED`,
- porażka: `ASSERTION` / `FAILED`,
- summary grupy z błędami: `ASSERTION` / `FAILED`,
- summary grupy bez błędów: `ASSERTION` / `PASSED`.

Metadata assertion eventów zawiera:

- `assertionName`,
- `expected`,
- `actual`,
- `label`,
- `badge`.

`expected` i `actual` są przycinane do 500 znaków. Nie zmienia to treści `OverlayAssertionResult`, komunikatów HUD ani wyjątków downstream; truncation dotyczy tylko event metadata.

`assertGroup(...)` i `assertGroupReactSafe(...)` emitują summary event po wykonaniu consumerów. Metadata summary zawiera `groupName`, `total`, `passed`, `failed`, `soft=true` i `reactSafe`.

`OverlayAssertionResult` pozostał w obecnym miejscu. Docelowo powinien zostać przeanalizowany jako kandydat do przyszłego `ui-test-lens-assertions` albo `ui-test-lens-overlay`.

Na później zostają:

- pełny moduł `ui-test-lens-assertions`,
- integracje JUnit/TestNG/AssertJ,
- private adapter `ContentIssueCollector`,
- eksport raportu assertion/checks,
- lepszy model `OverlayAssertionResult`,
- dokładniejsze oznaczanie, które assertion eventy fizycznie narysowały badge.

## Stage: Logging model unit tests

Dodano pierwsze testy jednostkowe dla modelu loggera/event-busa. Testy nie wymagają Selenium, WebDrivera, przeglądarki ani runtime JS.

Pokrycie obejmuje:

- `UiTestLensLogEntry`:
  - factory methods `info`, `warn`, `error`,
  - builder,
  - defensywne kopiowanie i niemutowalność metadata,
  - `toBuilder()` bez mutowania oryginału.
- `TargetDescriptor`:
  - `none`,
  - `selector`,
  - `label`,
  - `withMetadata` jako niemutujący helper.
- sinki:
  - `InMemoryLogSink`,
  - `ConsumerLogSink`,
  - `ConsoleLogSink`.
- `UiTestLensLogger`:
  - `noop`,
  - kolejność eventów,
  - wiele sinków,
  - izolowanie wyjątku jednego sinka,
  - `withSink`.
- `OverlayLogger`:
  - noop bridge,
  - delegowanie `info/warn/error`,
  - delegowanie typowanego `emit(UiTestLensLogEntry)`,
  - bezpieczne `from(null)`.

Te testy stabilizują neutralny model eventów przed dalszym refaktorem pakietów i modułów.

Na później zostają:

- Selenium integration tests,
- overlay runtime tests,
- exporter tests,
- HUD/highlight sink tests,
- testy przyszłych adapterów Selenide/Allure/TeamCity.

## Stage: Log exporters

Dodano pierwsze eksportery logów z `InMemoryLogSink`. Eksportery nie wymagają Selenium, WebDrivera, przeglądarki ani runtime JS.

Dostępne formaty:

- plain text przez `PlainTextLogExporter`,
- JSON przez `JsonLogExporter`,
- prosty samodzielny HTML przez `HtmlLogExporter`.

JSON i HTML są generowane bez zewnętrznych bibliotek, bez Jacksona, Gsona i template engine. Eksportery używają tylko standardowego JDK.

Przykład:

```java
InMemoryLogSink memorySink = new InMemoryLogSink();

UiTestLensLogger logger = UiTestLensLogger.builder()
        .sink(memorySink)
        .build();

// test flow...

String text = memorySink.exportAsText();
String json = memorySink.exportAsJson();
String html = memorySink.exportAsHtml();
```

`LogExportOptions` pozwala kontrolować:

- dołączanie metadata,
- dołączanie throwable,
- pretty print,
- maksymalną długość pól tekstowych.

Na później zostają:

- zapis do pliku,
- attachmenty do Allure/TeamCity,
- streaming logów,
- filtrowanie po typie eventu,
- maskowanie danych wrażliwych na poziomie eksportu,
- gotowe raporty HTML ze screenshotami.

Na później zostają:

- Selenium `WebDriverListener`,
- Selenide adapter,
- pełniejszy target descriptor oparty o bezpieczne atrybuty elementu,
- eksport JSON/TXT/HTML,
- HUD/highlight jako sinki,
- pełny assertion model,
- spójne eventy dla wszystkich react-safe find helpers.

## Następne kroki

- rozszerzyć eventy action/assertion z istniejących klas,
- dodać sink HUD,
- dodać eksport JSON/TXT/HTML,
- dodać adapter SLF4J,
- dodać adaptery Allure i TeamCity,
- dopiero później przepiąć runtime/HUD/highlight na centralny event-bus.
## Stage: Java package rename

Model loggera/event-busa zostal mechanicznie przeniesiony do pakietu
`io.github.mmaciekk111.uitestlens.core.logging`.

Zmiana dotyczy tylko deklaracji pakietow, importow i sciezek plikow Java.
Nie zmieniono nazw klas, publicznych metod, runtime namespace JS ani modelu eventow.

## Current artifact coordinates

Logger/event-bus jest czescia aktualnego single-module artifactu:

```text
io.github.mmaciekk111:ui-test-lens:1.0-SNAPSHOT
```

Pakiet Java loggera pozostaje `io.github.mmaciekk111.uitestlens.core.logging`.

## Runtime namespace migration

Runtime JS inicjalizuje teraz primary namespace `window.__uiTestLens` z sekcjami `modules` i `state`.
Eventy Java pozostaja bez zmian, ale wait/network/overlay state moze byc odczytywany z `window.__uiTestLens.state` przez przyszle HUD/highlight sinki.

Stare `window.__selenium...` globale pozostaja jako legacy compatibility aliases. Pelne przepisanie HUD/highlight na publiczne funkcje runtime JS zostaje na pozniejszy etap.

## Stage: Assertion badges runtime extraction

Visual assertion badges sa teraz renderowane przez runtime resource:

```text
src/main/resources/uitestlens/runtime/assertion-badges.js
```

Primary browser API to `window.__uiTestLens.modules.assertionBadges`.
Loader zachowuje fallback `selenium/assertion-badges.js`.

Ta zmiana nie zmienia eventow `ASSERTION`, semantyki pass/fail ani modelu `OverlayAssertionResult`.
Java nadal emituje eventy z `AssertActions`, a runtime JS odpowiada tylko za wizualny badge/check marker.

Glowne znane runtime resources obejmuja teraz:

- API overlay,
- Wait HUD,
- Highlight,
- Type hint,
- Scroll arrow,
- HUD panel,
- Assertion badges.
