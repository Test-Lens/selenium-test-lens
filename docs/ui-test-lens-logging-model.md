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

## Następne kroki

- rozszerzyć eventy action/assertion z istniejących klas,
- dodać sink HUD,
- dodać eksport JSON/TXT/HTML,
- dodać adapter SLF4J,
- dodać adaptery Allure i TeamCity,
- dopiero później przepiąć runtime/HUD/highlight na centralny event-bus.
