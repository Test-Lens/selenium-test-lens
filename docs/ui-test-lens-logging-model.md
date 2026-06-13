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

## Następne kroki

- emitować zdarzenia wait/action/assertion z istniejących klas,
- dodać sink HUD,
- dodać eksport JSON/TXT/HTML,
- dodać adapter SLF4J,
- dodać adaptery Allure i TeamCity,
- dopiero później przepiąć runtime/HUD/highlight na centralny event-bus.
