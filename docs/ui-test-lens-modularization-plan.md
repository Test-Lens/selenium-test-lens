# Plan modularyzacji `ui-test-lens`

## 1. Obecny projekt

Projekt jest biblioteką pomocniczą dla testów Selenium. Główna idea to wykonywanie JavaScriptu na testowanej stronie przez `JavascriptExecutor`, aby dodać warstwę debug UI: shadow-root overlay, HUD z informacją o teście i kroku, ramki/highlighty elementów, dymki przy wpisywaniu, wizualizację waitów, modal dla wywołań API oraz helpery do bardziej odpornych akcji w aplikacjach SPA/React.

Obecnie projekt jest pojedynczym modułem Maven/IDEA o niestandardowej strukturze: pliki `.java` leżą bezpośrednio w katalogu repozytorium i podkatalogach `actions`, `api`, `core`, `hud`, `react`, `scroll`, `utils`. Nie ma standardowego układu `src/main/java` ani `src/main/resources`.

Główne funkcjonalności:

- inicjalizacja i czyszczenie shadow-root overlaya,
- HUD testu, pipeline, aktualnego kroku i logów,
- highlight elementów, rodziców, przodków i najbliższych selektorów,
- smart click/type z obsługą popupów i overlayów blokujących,
- wait helpers dla `document.readyState`, network idle, React root, stabilnego DOM i widoczności komponentów,
- wait HUD/indicator,
- popup detection i close heuristics,
- scroll z animowaną strzałką,
- overlay assertions i soft assertions,
- target resolver dla kliknięć i uploadu plików,
- API modal z podglądem request/response i highlightem ścieżek JSON,
- React-safe retry wrappers i helper dla `react-select`,
- guardy wykrywające typowe strony błędów.

Główne punkty wejścia dla użytkownika biblioteki:

- `JsOverlayDebug` - obecna fasada dla większości operacji,
- `OverlayConfig` - konfiguracja overlaya/HUD/highlightów,
- `OverlayWait` - wrapper dla `WebDriverWait` z wait HUD i logowaniem,
- `ReactSafeExecutor` - retry na locatorach w React/SPA,
- `OverlayContentAssertions` - integracja overlay assertions z zewnętrznym `ContentIssueCollector`,
- `ApiOverlayPanel` i `ApiCallActions` - API modal i wrapper wywołań API.

Klasy/metody wyglądające na publiczne API:

- `JsOverlayDebug`: `initHud`, `setStep`, `hudLog`, `highlightClick`, `highlightElement`, `highlightParent`, `highlightAncestor`, `highlightClosest`, `typeWithHint`, `clearAndType`, `smartTypeWithHint*`, `smartClickWithOverlayHandler`, `smartClickReactSafe`, `clearDebugArtifacts`, `waitFor*`, `showWaitIndicator`, `hideWaitIndicator`, `detectPopup`, `closePopupIfPresent`, `scrollToElementWithArrow`, `assert*`, `assertGroup*`, `resolve*`, `smartClickResolved`, `smartUploadFile`, `showApiCall`, `apiCallWithModal`, `apiHighlight*`, `reactSafe`.
- `OverlayConfig.builder()`.
- `OverlayWait.until(...)`.
- `ReactSafeExecutor.doWithRetry`, `click`, `clearAndType`, `getText`, `getAttribute`, `isDisplayed`, `select`.
- `ApiOverlayPanel.showRequest`, `setPending`, `setResponse`, `setError`, `hide`, `highlight*`, `filterToPaths`.
- `Guards.checkpoint`, `assertOk`.

Elementy wyglądające na implementację wewnętrzną:

- `OverlayRootManager`, `HudPanel`, `HighlightActions`, `TypingActions`, `SmartClickActions`, `SmartInputActions`, `BlockingOverlayHelper`, `PopupDetector`, `PageWaits`, `ScrollActions`, `TargetResolverActions`, `AssertActions`,
- `ApiOverlayJs`, `JsResources`,
- `ApiOverlayContext`, `ApiOverlayPlan`, `ApiOverlayRule` w obecnej formie `ThreadLocal`,
- puste `ScriptExecutor`,
- zaszyte stringi JavaScript w klasach akcji i core.

## 2. Mapa obecnej struktury kodu

```text
.
  pom.xml
  JsOverlayDebug.java
  OverlayConfig.java
  OverlayWait.java
  OverlayContentAssertions.java
  actions/
  api/
  core/
  hud/
  react/
  scroll/
  utils/
```

Odpowiedzialność katalogów:

- root package `io.github.mmaciekk111.uitestlens`: fasada (`JsOverlayDebug`), konfiguracja (`OverlayConfig`), wait wrapper (`OverlayWait`), adapter content assertions.
- `actions`: akcje Selenium + overlay, czyli highlight, typing, smart click/input, scroll, target resolver i overlay assertions.
- `core`: techniczny runtime overlaya, page waits, popup/overlay heuristics, guards.
- `hud`: panel HUD i pozycje HUD.
- `api`: API modal, wrapper wywołań API, ThreadLocal context/rules/plan, loader JS modala.
- `react`: React-safe retry i react-select helper.
- `scroll`: enumy wyrównania scrolla.
- `utils`: loader zasobów JS.

Najważniejsze zależności:

- prawie wszystkie pakiety zależą od Selenium (`WebDriver`, `WebElement`, `JavascriptExecutor`, `By`, `WebDriverWait`),
- `JsOverlayDebug` zależy od wszystkich domen: `actions`, `api`, `core`, `hud`, `react`, `scroll`,
- `actions` zależy od `core.OverlayRootManager`, `OverlayConfig`, czasem `hud.HudPanel`,
- `core.BlockingOverlayHelper` i `core.PopupDetector` zależą od `actions.HighlightActions`, czyli `core` nie jest czyste,
- `react.ReactSafeExecutor` zależy od `JsOverlayDebug`, więc moduł React jest sprzężony z fasadą overlay,
- `api.ApiCallActions` zależy od RestAssured (`io.restassured.response.Response`), którego nie ma w `pom.xml`,
- `OverlayWait` i `Guards` zależą od `utils.logs.LogWraper`, którego nie ma w `pom.xml`,
- `OverlayWait` zależy od `utils.time.TimeStamp`, którego nie ma w `pom.xml`,
- `OverlayContentAssertions` zależy od `tests.fe.utils.contentassertions.ContentIssueCollector` i `utils.datetime.LocalDateTimeUtils`, których nie ma w `pom.xml`.

Miejsca, gdzie warstwy są wymieszane:

- `JsOverlayDebug` jest fasadą, ale zawiera także prywatne helpery Selenium/JS, wait HUD, logowanie, React find helpers, assertions i API modal.
- `HighlightActions.highlightClick` nie tylko rysuje highlight, ale też wykonuje klik i fallbacki kliku.
- `core` zawiera logikę niskopoziomową, ale zależy od akcji highlightów.
- `HudPanel`, `HighlightActions`, `AssertActions`, `PageWaits`, `PopupDetector`, `BlockingOverlayHelper`, `ScrollActions` zawierają długie sklejane stringi JS zamiast zasobów/runtime API.
- `ApiCallActions` miesza UI modala, generics, RestAssured i obsługę wyjątków.
- `OverlayWait` miesza wait abstraction, HUD, zewnętrzny logger i timestamp provider.

## 3. Główne domeny/funkcje

- Java Selenium integration: `JsOverlayDebug`, `OverlayWait`, `ReactSafeExecutor`, klasy `actions`, `PageWaits`, `PopupDetector`, `BlockingOverlayHelper`.
- JavaScript injection/runtime: `OverlayRootManager`, `JsResources`, `ApiOverlayJs`, zaszyte `executeScript`/`executeAsyncScript` w większości klas.
- Overlay/HUD rendering: `OverlayRootManager`, `HudPanel`, `OverlayConfig`, część `JsOverlayDebug`.
- Element highlighting: `HighlightActions`, metody `highlight*` w `JsOverlayDebug`, badge assertions w `AssertActions`.
- Action/debug logging: `HudPanel.appendLog`, `JsOverlayDebug.hudLog`, `OverlayWait` + `LogWraper`.
- Wait/debug visualization: `OverlayWait`, `PageWaits`, `JsOverlayDebug.ensureWaitHudInjected`, `waitHudStart`, `waitHudStop`, `showWaitIndicator`, `hideWaitIndicator`.
- Assertion/debug helpers: `AssertActions`, `JsOverlayDebug.AssertionSummary`, `JsOverlayDebug.SoftAssertions`, `OverlayContentAssertions`.
- Configuration: `OverlayConfig`, `HudPosition`, częściowo system properties w `Guards`.
- API/debug modal: `ApiOverlayPanel`, `ApiCallActions`, `ApiOverlayPlan`, `ApiOverlayRule`, `ApiOverlayContext`, `ApiOverlayJs`.
- React/SPA helpers: `ReactSafeExecutor`, `ReactSelectHelper`, React wait methods w `PageWaits` i `JsOverlayDebug`.
- Popup/overlay handling: `PopupDetector`, `BlockingOverlayHelper`, część `SmartClickActions` i `SmartInputActions`.
- Shared utilities: `JsResources`, enumy scrolla, potencjalnie przyszły `ScriptExecutor`.
- Test/demo/example code: brak wydzielonego katalogu przykładów, ale `OverlayContentAssertions` wygląda jak kod specyficzny dla testów właściciela projektu.

## 4. Propozycja podziału na moduły Maven

### `ui-test-lens-core`

Odpowiedzialność:

- wspólna konfiguracja, modele, minimalne kontrakty i loader zasobów,
- brak bezpośredniej zależności od Selenium, jeśli to możliwe.

Klasy/pliki:

- `OverlayConfig` po odseparowaniu od `HudPosition` albo z przeniesieniem prostych enumów,
- `HudPosition`, `ScrollElementEdge`, `ScrollViewportEdge`,
- `JsResources`,
- przyszłe modele: `OverlayLogLevel`, `OverlayTheme`, `WaitHudConfig`, `ApiOverlayConfig`, `OverlayAssertionResult`.

Zależności:

- JDK only.

Czego nie zawiera:

- `WebDriver`, `WebElement`, `JavascriptExecutor`,
- RestAssured,
- zewnętrznych loggerów właściciela projektu,
- dużych stringów JS specyficznych dla Selenium.

Status:

- publiczna biblioteka bazowa.

### `ui-test-lens-js-runtime`

Odpowiedzialność:

- zasoby JavaScript/CSS w `src/main/resources`,
- wersjonowany runtime dla overlay root, HUD, highlight, wait HUD, API modal,
- docelowo jeden stabilny JS namespace, np. `window.__uiTestLens`.

Klasy/pliki:

- `ApiOverlayJs`,
- nowy `OverlayRuntimeScripts`,
- zasoby `selenium/api-overlay.js`, `selenium/wait/WaitHud.js`, `selenium/overlay-root.js`, `selenium/highlight.js`, `selenium/hud.js`, `selenium/assertions.js`.

Zależności:

- `ui-test-lens-core`.

Czego nie zawiera:

- Selenium,
- RestAssured,
- logiki testowej Java.

Status:

- publiczny lub internal. Jeśli runtime JS ma być używany też poza Selenium, warto utrzymać publiczny moduł.

### `ui-test-lens-selenium`

Odpowiedzialność:

- integracja z Selenium,
- główna fasada dla użytkownika Selenium,
- adapter wykonywania skryptów, overlay root, podstawowe akcje i cleanup.

Klasy/pliki:

- docelowa fasada po odchudzeniu `JsOverlayDebug`,
- `OverlayRootManager`,
- `TargetResolverActions`,
- `TypingActions`,
- `SmartClickActions`,
- `SmartInputActions`,
- `BlockingOverlayHelper`,
- `PopupDetector`,
- `PageWaits`,
- `ScrollActions`,
- `ReactSafeExecutor` tylko jeśli React pozostaje częścią bazowego Selenium modułu.

Zależności:

- `ui-test-lens-core`,
- `ui-test-lens-js-runtime`,
- `org.seleniumhq.selenium:selenium-java`.

Czego nie zawiera:

- RestAssured,
- `ContentIssueCollector`,
- prywatnego `LogWraper`,
- API modala, jeśli zostanie wydzielony.

Status:

- główna publiczna biblioteka.

### `ui-test-lens-overlay`

Odpowiedzialność:

- operacje wizualne: HUD, highlight, assertion badges, wait indicator.

Klasy/pliki:

- `HudPanel`,
- `HighlightActions` po rozdzieleniu na `highlight` i `click`,
- overlayowa część `AssertActions`,
- wait HUD methods z `JsOverlayDebug`,
- JS zasoby wizualne, jeśli nie trafią do `js-runtime`.

Zależności:

- `ui-test-lens-core`,
- `ui-test-lens-js-runtime`,
- Selenium adapter albo abstrakcja `ScriptExecutor`.

Czego nie zawiera:

- smart click heuristics,
- RestAssured,
- React-select helper.

Status:

- publiczny, jeśli overlay ma być opcjonalny; internal, jeśli zawsze instalowany przez `ui-test-lens-selenium`.

### `ui-test-lens-wait-hud`

Odpowiedzialność:

- wait helpers i ich wizualizacja.

Klasy/pliki:

- `OverlayWait`,
- `PageWaits`,
- wait HUD fragmenty `JsOverlayDebug`,
- zasób `selenium/wait/WaitHud.js`.

Zależności:

- `ui-test-lens-core`,
- `ui-test-lens-overlay` albo `ui-test-lens-js-runtime`,
- Selenium.

Czego nie zawiera:

- prywatnego `LogWraper`; zamiast tego neutralny interfejs np. `OverlayLogger`,
- `utils.time.TimeStamp`; zamiast tego `Clock` lub `DateTimeFormatter`.

Status:

- publiczny moduł opcjonalny.

### `ui-test-lens-assertions`

Odpowiedzialność:

- overlay assertions i soft assertion summary.

Klasy/pliki:

- `AssertActions`,
- `JsOverlayDebug.AssertionSummary` i `SoftAssertions` jako osobne klasy,
- wynik `OverlayAssertionResult`.

Zależności:

- `ui-test-lens-core`,
- opcjonalnie `ui-test-lens-overlay`,
- Selenium dla asercji na `WebElement`/`By`.

Czego nie zawiera:

- integracji `OverlayContentAssertions` z `ContentIssueCollector`,
- framework-specific assertions typu JUnit/TestNG, chyba że jako adapter.

Status:

- publiczny moduł opcjonalny.

### `ui-test-lens-api-overlay`

Odpowiedzialność:

- modal API niezależny od konkretnego klienta HTTP.

Klasy/pliki:

- `ApiOverlayPanel`,
- `ApiOverlayJs`,
- `ApiOverlayPlan`,
- `ApiOverlayRule`,
- `ApiOverlayContext` po refaktorze away from static ThreadLocal API lub jako internal.

Zależności:

- `ui-test-lens-core`,
- `ui-test-lens-js-runtime`,
- Selenium.

Czego nie zawiera:

- RestAssured specific `Response`,
- wykonywania HTTP.

Status:

- publiczny moduł opcjonalny.

### `ui-test-lens-restassured`

Odpowiedzialność:

- adapter RestAssured do API modala.

Klasy/pliki:

- `ApiCallActions.callWithModalRA`,
- ewentualny `RestAssuredApiOverlay`.

Zależności:

- `ui-test-lens-api-overlay`,
- `io.rest-assured:rest-assured`.

Czego nie zawiera:

- głównego Selenium overlaya,
- ogólnego API panelu.

Status:

- publiczny moduł opcjonalny.

### `ui-test-lens-react`

Odpowiedzialność:

- React/SPA retry i komponentowe helpery.

Klasy/pliki:

- `ReactSafeExecutor`,
- `ReactSelectHelper`,
- React wait methods z `PageWaits` lub adapter wokół nich.

Zależności:

- `ui-test-lens-selenium`,
- opcjonalnie `ui-test-lens-overlay`.

Czego nie zawiera:

- core overlay rendering,
- API modal,
- globalnej fasady `JsOverlayDebug` jako wymaganej zależności w konstruktorze; lepiej użyć małego interfejsu typu `StepReporter`/`ElementHighlighter`.

Status:

- publiczny moduł opcjonalny.

### `ui-test-lens-content-assertions-adapter`

Odpowiedzialność:

- integracja z prywatnym `ContentIssueCollector`, jeśli jest potrzebna w organizacji.

Klasy/pliki:

- `OverlayContentAssertions`.

Zależności:

- `ui-test-lens-assertions`,
- zależność właściciela projektu zawierająca `ContentIssueCollector` i `LocalDateTimeUtils`.

Czego nie zawiera:

- publicznej, ogólnej biblioteki Maven, dopóki zależności są prywatne.

Status:

- raczej internal albo przykład firmowy, nie publiczna biblioteka.

### `ui-test-lens-examples`

Odpowiedzialność:

- przykłady użycia i demo testy.

Klasy/pliki:

- przyszłe przykłady JUnit/TestNG,
- przykłady Selenium + overlay, API modal, wait HUD, React-safe.

Zależności:

- moduły publiczne,
- test framework,
- WebDriver manager opcjonalnie.

Czego nie zawiera:

- kodu produkcyjnego biblioteki.

Status:

- moduł przykładów/testów, bez publikacji jako runtime dependency.

## 5. Propozycja API publicznego

Docelowo użytkownik powinien móc zacząć od jednej fasady i buildera. Szkic:

```java
OverlayConfig config = OverlayConfig.builder()
    .enabled(true)
    .hud(HudConfig.builder()
        .visible(true)
        .position(HudPosition.BOTTOM_RIGHT)
        .offset(10, 10)
        .maxWidthPx(320)
        .build())
    .highlight(HighlightConfig.builder()
        .color("#ffeb3b")
        .duration(Duration.ofMillis(1500))
        .build())
    .waitHud(WaitHudConfig.builder()
        .enabled(true)
        .build())
    .debugMode(DebugMode.VISUAL)
    .build();

UiTestLens tools = UiTestLens.selenium(driver, config);

tools.hud().init("Checkout test", "pipeline-123");
tools.hud().step("Open cart");
tools.hud().log(OverlayLogLevel.INFO, "Cart opened");

tools.highlight().element(cartButton, "CART");
tools.actions().click(cartButton, "OPEN_CART");
tools.actions().type(emailInput, "user@example.com", "EMAIL");

tools.waits().pageReady();
tools.waits().networkIdle(Duration.ofMillis(500), Duration.ofSeconds(10));
tools.waits().withHud("Waiting for submit", d -> d.findElement(submitBy).isDisplayed());

tools.assertions().textEquals(title, "Checkout", "Page title");
tools.assertions().group("Checkout form", softly -> {
    softly.visible(emailInput, true, "email visible");
    softly.enabled(submitButton, true, "submit enabled");
});

tools.apiOverlay().showRequest("Create order", "POST", "/orders", payloadPreview);
tools.apiOverlay().setResponse(requestId, 201, 420, headersPreview, bodyPreview);

tools.cleanup().clearOverlay();
tools.close();
```

Wariant z modułem React:

```java
ReactTools react = tools.react();

react.safe().click(By.cssSelector("[data-testid='save']"), "SAVE");
react.select().pickByLabel("Country", "Poland");
react.waits().componentVisible(By.id("root"), By.cssSelector(".checkout"));
```

Wariant bez globalnej fasady:

```java
Overlay overlay = SeleniumOverlay.create(driver, config);
WaitHud waits = WaitHud.create(driver, overlay);
ElementHighlighter highlighter = overlay.highlighter();
```

API powinno ukrywać klasy `*Actions`, `OverlayRootManager`, `HudPanel` i zasoby JS. Te klasy mogą pozostać publiczne technicznie tylko tam, gdzie rzeczywiście są rozszerzalnym kontraktem, ale domyślnie powinny być internal/package-private.

## 6. Problemy techniczne i dług architektoniczny

- `JsOverlayDebug.java` ma około 59 KB i łączy wiele domen: fasada, wait HUD, React helpers, assertions, API modal, target resolver, cleanup. Przed publikacją warto rozbić go na mniejsze serwisy i zostawić jako cienką fasadę.
- `AssertActions.java`, `HighlightActions.java`, `PageWaits.java`, `PopupDetector.java`, `BlockingOverlayHelper.java`, `HudPanel.java` są duże i zawierają długie stringi JS.
- `HighlightActions.highlightClick` ma mylącą nazwę: rysuje highlight i wykonuje klik wraz z fallbackami.
- `core` zależy od `actions.HighlightActions`, więc warstwa core nie jest niezależna.
- `ReactSafeExecutor` zależy od `JsOverlayDebug`, zamiast od małych interfejsów.
- Statyczne `ThreadLocal` w `ApiOverlayContext`, `ApiOverlayPlan`, `ApiOverlayRule` utrudniają czytelny lifecycle, równoległość i testowanie.
- Brakuje abstrakcji dla wykonywania JS, np. `BrowserScriptExecutor`/`ScriptExecutor`; obecny `core.ScriptExecutor` jest pusty.
- API overlay JS został wydzielony do `src/main/resources/uitestlens/runtime/api-overlay.js`; loader zachowuje fallback `selenium/api-overlay.js`.
- Wait HUD i pozostałe fragmenty runtime nadal wymagają ekstrakcji z inline JavaScript albo legacy resource paths.
- POM deklaruje Selenium `4.39.0`, ale property `selenium.version` ma wartość `4.40.0` i nie jest używane.
- POM nie deklaruje RestAssured mimo importu w `ApiCallActions`.
- POM nie deklaruje prywatnych zależności: `utils.logs.LogWraper`, `utils.time.TimeStamp`, `utils.datetime.LocalDateTimeUtils`, `tests.fe.utils.contentassertions.ContentIssueCollector`.
- Lombok jest w POM-ie, ale widoczne użycie to tylko `@Getter` w `JsOverlayDebug`; można go usunąć albo ograniczyć.
- Część komentarzy/tekstu wygląda na problem z kodowaniem znaków w odczycie plików; przed publikacją warto ujednolicić UTF-8 i sprawdzić źródła w IDE/buildzie.
- Brakuje testów jednostkowych/integracyjnych i standardowego layoutu Maven.
- Brak jawnego rozdziału API stabilnego od eksperymentalnego.
- Długie inline JS utrudnia formatowanie, linting, testowanie i wersjonowanie runtime.
- `Thread.sleep` w helperach popup/input/wait może spowalniać testy i utrudniać deterministykę.
- Wiele `catch (Exception ignored)` ukrywa problemy integracyjne, szczególnie przy brakujących zasobach JS albo błędach strony.

## 7. Co trzeba wydzielić, przepisać albo zostawić

### A. Gotowe do przeniesienia prawie bez zmian

- `OverlayConfig` - dobry kandydat na core config, choć warto rozbić konfigurację HUD/highlight/wait/API na podkonfiguracje.
- `HudPosition`, `ScrollElementEdge`, `ScrollViewportEdge` - proste enumy.
- `JsResources` - nadaje się do core/runtime po doprecyzowaniu ścieżek i obsługi błędów.
- `AssertActions.OverlayAssertionResult` - warto przenieść jako osobny model.

### B. Warto przenieść, ale po lekkim refaktorze

- `HudPanel` - działa jako komponent HUD, ale JS powinien trafić do zasobu.
- `OverlayRootManager` - dobry element runtime, ale powinien używać abstrakcji wykonania JS.
- `PageWaits` - użyteczne waity, ale trzeba odseparować raportowanie HUD od samego waitowania.
- `ScrollActions` - sensowna domena, ale JS powinien być osobnym zasobem.
- `TargetResolverActions` - przydatne, ale `buildCssSelector` powinien poprawnie escapować CSS i nie obiecywać unikalności.
- `ReactSafeExecutor` - wartościowy, ale powinien zależeć od interfejsów `StepReporter`/`ElementHighlighter`, nie od `JsOverlayDebug`.

### C. Wymaga większego refaktoru przed publikacją

- `JsOverlayDebug` - wymaga rozbicia na fasadę i serwisy domenowe.
- `HighlightActions` - trzeba rozdzielić highlight od click/fallback click.
- `AssertActions` - warto oddzielić czystą logikę wyniku asercji od renderowania badge w overlay.
- `BlockingOverlayHelper` i `PopupDetector` - duplikują heurystyki close/accept i są sprzężone z highlightem.
- `OverlayWait` - wymaga odcięcia `LogWraper` i `TimeStamp`.
- `ApiCallActions` - wymaga wydzielenia RestAssured do adaptera.
- `ApiOverlayContext`, `ApiOverlayPlan`, `ApiOverlayRule` - statyczne ThreadLocal trzeba zastąpić jawnie przekazywanym kontekstem lub opakować jako internal.

### D. Powinno zostać jako demo/test/example

- `OverlayContentAssertions` - obecnie zależy od projektowych klas `ContentIssueCollector` i `LocalDateTimeUtils`; dobry kandydat na przykład adaptera.
- Przyszłe przykłady użycia `JsOverlayDebug`, `OverlayWait`, `ApiOverlayPanel`, `ReactSafeExecutor`.

### E. Prawdopodobnie nie powinno trafiać do publicznej biblioteki Maven

- bezpośrednie zależności od `utils.logs.LogWraper`, `utils.time.TimeStamp`, `utils.datetime.LocalDateTimeUtils`, `tests.fe.utils.contentassertions.ContentIssueCollector`,
- prywatne/teamowe integracje logowania i content collector,
- puste `ScriptExecutor` w obecnej formie,
- IDE metadata `.idea`, `.iml`,
- zaszyte, nieudokumentowane system properties `guard.*` jako jedyny sposób konfiguracji guardów.

## 8. Proponowana kolejność prac

1. Uporządkować layout Maven bez zmiany zachowania: `src/main/java`, `src/main/resources`, `src/test/java`; zachować pakiety na początku.
2. Udokumentować obecne API i oznaczyć eksperymentalne elementy, szczególnie `JsOverlayDebug`, API modal i React helpers.
3. Dodać brakujące zasoby JS do `src/main/resources` albo usunąć martwe referencje, jeśli zasoby są generowane gdzie indziej.
4. Wydzielić `core`: konfiguracja, enumy, modele wyników, loader zasobów, neutralny logger/interfejs raportowania.
5. Wprowadzić abstrakcję wykonywania JS, np. `ScriptExecutor`, i adapter Selenium dla `JavascriptExecutor`.
6. Przenieść overlay runtime/HUD/highlight do osobnej warstwy i zmienić długie inline JS na pliki `.js`.
7. Odchudzić `JsOverlayDebug` do cienkiej fasady delegującej do `hud()`, `highlight()`, `actions()`, `waits()`, `assertions()`, `apiOverlay()`, `react()`.
8. Wydzielić Selenium integration i upewnić się, że moduły bez Selenium nie importują `org.openqa.selenium.*`.
9. Wydzielić wait HUD i assertions jako moduły opcjonalne.
10. Wydzielić API overlay i adapter RestAssured do osobnych modułów.
11. Odseparować prywatne integracje (`LogWraper`, `ContentIssueCollector`) jako internal adapter albo example.
12. Dodać testy jednostkowe dla modeli/config/normalizacji oraz testy integracyjne Selenium na prostej stronie HTML.
13. Przygotować parent POM, dependency management, enforcer, source/javadoc jars i publikację lokalną `install`.
14. Opublikować `SNAPSHOT` lokalnie lub w firmowym repozytorium i zweryfikować użycie w realnym projekcie testowym.

## 9. Propozycja docelowej struktury repo

```text
ui-test-lens/
  pom.xml
  docs/
    ui-test-lens-modularization-plan.md
  ui-test-lens-core/
    pom.xml
    src/main/java/io/github/mmaciekk111/uitestlens/core/
    src/test/java/
  ui-test-lens-js-runtime/
    pom.xml
    src/main/java/io/github/mmaciekk111/uitestlens/runtime/
    src/main/resources/uitestlens/runtime/
      overlay-root.js
      hud.js
      highlight.js
      wait-hud.js
      api-overlay.js
  ui-test-lens-selenium/
    pom.xml
    src/main/java/io/github/mmaciekk111/uitestlens/selenium/
  ui-test-lens-overlay/
    pom.xml
    src/main/java/io/github/mmaciekk111/uitestlens/overlay/
  ui-test-lens-wait-hud/
    pom.xml
    src/main/java/io/github/mmaciekk111/uitestlens/wait/
  ui-test-lens-assertions/
    pom.xml
    src/main/java/io/github/mmaciekk111/uitestlens/assertions/
  ui-test-lens-api-overlay/
    pom.xml
    src/main/java/io/github/mmaciekk111/uitestlens/api/
  ui-test-lens-restassured/
    pom.xml
    src/main/java/io/github/mmaciekk111/uitestlens/restassured/
  ui-test-lens-react/
    pom.xml
    src/main/java/io/github/mmaciekk111/uitestlens/react/
  ui-test-lens-examples/
    pom.xml
    src/test/java/
    src/test/resources/
      pages/
```

Zasoby JS:

- powinny leżeć w `src/main/resources/uitestlens/runtime/...`,
- powinny mieć stabilne nazwy i wersję runtime zgodną z wersją artefaktu Maven,
- Java powinna ładować je przez `ClassLoader` z jednego miejsca,
- warto rozważyć składanie runtime z małych plików w buildzie, ale publikować gotowe zasoby,
- namespace w przegladarce jest wprowadzany jako `window.__uiTestLens = { version, modules, state }`,
- stare `window.__selenium...` globale pozostaja przejsciowo jako compatibility aliases,
- API overlay jest pierwszym wydzielonym runtime resource: `src/main/resources/uitestlens/runtime/api-overlay.js`,
- legacy path `selenium/api-overlay.js` pozostaje fallbackiem loadera,
- nowe resource paths `uitestlens/runtime/...` sa preferowane, a stare `selenium/...` moga zostac fallbackiem do czasu pelnej ekstrakcji runtime,
- klasy Java nie powinny znać szczegółów DOM/CSS poza wywołaniem publicznych funkcji runtime JS.

## 10. Pytania decyzyjne dla właściciela projektu

- Czy biblioteki mają być tylko dla Selenium, czy docelowo też dla Playwright/Cypress/innych driverów?
- Czy overlay/HUD ma być niezależny od Selenium przez abstrakcję wykonywania skryptów?
- Czy runtime JavaScript ma być wersjonowany jako osobny kontrakt i czy dopuszczamy breaking changes?
- Base package został ustawiony na `io.github.mmaciekk111.uitestlens`, a aktualne single-module Maven coordinates to `io.github.mmaciekk111:ui-test-lens:1.0-SNAPSHOT`.
- Czy API ma być stabilne od pierwszej publikacji, czy oznaczamy je jako eksperymentalne?
- Jaki minimalny Java version: 11, 17 czy nowszy?
- Jaki minimalny Selenium version i czy używać `selenium-java`, czy węższych artefaktów?
- Czy konfiguracja ma być tylko fluent builderem, czy także properties/system properties?
- Czy prywatne `LogWraper` i `ContentIssueCollector` mają dostać publiczne adaptery, czy pozostać poza biblioteką?
- Czy RestAssured ma być wspierany jako oficjalny adapter, czy tylko przykład?
- Czy wait HUD i API modal mają być domyślnie włączone, czy opt-in?
- Czy highlight ma kiedykolwiek wykonywać click, czy highlight i action powinny być jawnie rozdzielone?
- Jak obsługiwać błędy JS: ignorować, logować, czy opcjonalnie fail-fast?
- Czy publikacja ma być lokalna, do firmowego Nexus/Artifactory, czy publiczna Maven Central?
- Czy projekt ma utrzymywać kompatybilność binarną między wersjami?
- Jakie testy integracyjne są wymagane przed publikacją: Chrome only, Firefox, headless, CI?
- Czy API overlay ma obsługiwać duże payloady przez truncation, pretty print, masking danych wrażliwych?
- Czy konfiguracja kolorów/tematów ma wspierać dark/light/custom theme?
- Czy obsługujemy równoległe testy w jednym JVM przez jawny kontekst zamiast statycznych `ThreadLocal`?
