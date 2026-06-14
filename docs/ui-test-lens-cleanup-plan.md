# UI Test Lens cleanup plan

## Rekomendowana nazwa projektu

Rekomendowana nazwa robocza i docelowa: `ui-test-lens`.

Nazwa produktowa/czytelna: `UI Test Lens`.

Opis: UI Test Lens to visual observability/debug layer for UI/browser automation tests. Projekt ma dostarczac wspolny overlay runtime, HUD, highlight elementow, event-bus/logger, eksport logow oraz adaptery dla Selenium, React-safe actions, Selenide i potencjalnie innych narzedzi UI automation.

Uzasadnienie:

- nazwa jest bardziej informatywna niz ogolne `testtools`,
- wskazuje UI automation, ale nie ogranicza projektu tylko do Selenium,
- pasuje do warstwy observability/debug, a nie tylko do pojedynczych helperow,
- zostawia miejsce na adaptery Selenium, Selenide, React-safe, Allure, TeamCity, SLF4J i loggery projektowe.

## Rekomendowane artifactId

Docelowy uklad multi-module:

- `ui-test-lens-core`
- `ui-test-lens-overlay`
- `ui-test-lens-selenium`
- `ui-test-lens-react`
- `ui-test-lens-selenide`
- `ui-test-lens-examples`

Moduly przyszle/opcjonalne:

- `ui-test-lens-api-overlay`
- `ui-test-lens-restassured`
- `ui-test-lens-assertions`
- `ui-test-lens-allure`
- `ui-test-lens-teamcity`

Splity Maven zostaly wykonane etapowo. Aktualne moduly:

- parent: `ui-test-lens-parent`,
- neutralny core: `ui-test-lens-core`,
- overlay/runtime: `ui-test-lens-overlay`,
- modul Selenium/fasady/akcji: `ui-test-lens-selenium`,
- modul React helpers: `ui-test-lens-react`,
- all-in-one compatibility artifact: `ui-test-lens`,
- compile-check examples: `ui-test-lens-examples`.

## Wybrany groupId

Dla publikacji publicznej:

```text
io.github.mmaciekk111
```

Pelne koordynaty przykladowego modulu:

```text
io.github.mmaciekk111:ui-test-lens-selenium
```

Dla projektu lokalnego/prywatnego:

```text
pl.mmaciekk111
```

Alternatywa firmowa dla wewnetrznego Nexus/Artifactory:

```text
pl.<organization>.uitestlens
```

GroupId w POM zostal ustawiony na publiczne koordynaty GitHub namespace.
Aktualna wersja projektu to `0.1.0-SNAPSHOT`. Projekt jest nadal pre-1.0, API moze zmieniac sie miedzy wydaniami 0.x, a Maven Central release nie jest jeszcze skonfigurowany. Lokalne uzycie odbywa sie przez `mvn install`.

Aktualne glowne Maven coordinates dla modulu Selenium/overlay:

```text
io.github.mmaciekk111:ui-test-lens:0.1.0-SNAPSHOT
```

Aktualne coordinates modulu core:

```text
io.github.mmaciekk111:ui-test-lens-core:0.1.0-SNAPSHOT
```

Aktualne coordinates modulu overlay:

```text
io.github.mmaciekk111:ui-test-lens-overlay:0.1.0-SNAPSHOT
```

## Rekomendowany base package

Dla publikacji publicznej:

```text
io.github.mmaciekk111.uitestlens
```

Dla projektu lokalnego/prywatnego:

```text
pl.mmaciekk111.uitestlens
```

Rekomendacja na teraz: nie robic pelnego rename pakietow razem z porzadkowaniem layoutu. Obecne pakiety nalezy traktowac jako stan techniczny do migracji, a rename wykonac pozniej jako osobny, mechaniczny commit.

## Docelowy namespace runtime w przegladarce

Docelowy global:

```javascript
window.__uiTestLens
```

Zasady migracji:

- obecne namespace runtime Selenium zachowac przejsciowo, dopoki kod Java i zasoby JS nie zostana wydzielone,
- nowy runtime powinien eksportowac jeden stabilny obiekt `window.__uiTestLens`,
- stare globale Selenium mozna pozniej mapowac jako kompatybilne aliasy tylko na okres przejsciowy,
- kod Java powinien docelowo wolac publiczne funkcje runtime, a nie skladac duze fragmenty JS inline.

## Docelowy katalog resources

Docelowy katalog runtime:

```text
src/main/resources/uitestlens/runtime/
```

Proponowany uklad:

```text
src/main/resources/uitestlens/runtime/
  ui-test-lens-runtime.js
  overlay-root.js
  hud.js
  highlight.js
  event-bus.js
  logger.js
  wait-hud.js
  api-overlay.js
```

W etapie przejsciowym mozna zachowac kompatybilne sciezki zasobow Selenium, jesli istnieja downstream consumers. Docelowo nowe zasoby powinny byc ladowane z `uitestlens/runtime/`.

## Runtime namespace migration

Wykonano kompatybilny etap migracji runtime:

- primary browser namespace: `window.__uiTestLens`,
- stan wait/network/overlay jest inicjalizowany pod `window.__uiTestLens.state`,
- moduly runtime moga byc aliasowane pod `window.__uiTestLens.modules`,
- stare globale `window.__selenium...` zostaja jako legacy compatibility aliases,
- preferowany resource root to `uitestlens/runtime/`,
- stare sciezki `selenium/...` zostaja jako fallback w loaderach.

Nie wykonano jeszcze pelnego przeniesienia wszystkich inline JS do plikow resources.

## API overlay JavaScript extraction

Pierwszy fragment runtime JS zostal przeniesiony do realnego resource file:

```text
src/main/resources/uitestlens/runtime/api-overlay.js
```

Loader `ApiOverlayJs` preferuje teraz:

```text
uitestlens/runtime/api-overlay.js
```

i zachowuje legacy fallback:

```text
selenium/api-overlay.js
```

API overlay rejestruje sie pod primary namespace `window.__uiTestLens.modules.apiOverlay`.
Dla kompatybilnosci pozostaje alias `window.__seleniumApiModal`, poniewaz obecne klasy Java nadal korzystaja z tego globalnego mostka.

## Wait HUD JavaScript extraction

Drugi fragment runtime JS zostal przeniesiony do realnego resource file:

```text
src/main/resources/uitestlens/runtime/wait-hud.js
```

Loader `WaitHudJs` preferuje teraz:

```text
uitestlens/runtime/wait-hud.js
```

i zachowuje legacy fallback:

```text
selenium/wait/WaitHud.js
```

Wait HUD rejestruje sie pod primary namespace `window.__uiTestLens.modules.waitHud`.
Dla kompatybilnosci pozostaja aliasy:

- `window.__seleniumWaitHud`,
- `window.__seleniumLastWaitMessage`,
- `window.__seleniumLastWaitElapsedMs`.

## Highlight JavaScript extraction

Trzeci fragment runtime JS zostal przeniesiony do realnego resource file:

```text
src/main/resources/uitestlens/runtime/highlight.js
```

Loader `HighlightJs` preferuje teraz:

```text
uitestlens/runtime/highlight.js
```

i zachowuje legacy fallback:

```text
selenium/highlight.js
```

Highlight runtime rejestruje sie pod primary namespace `window.__uiTestLens.modules.highlight`.
Warstwa JS nadal uzywa kompatybilnego overlay root:

- primary state: `window.__uiTestLens.state.overlay.root`,
- legacy alias: `window.__seleniumOverlayRoot`.

## Type hint JavaScript extraction

Kolejny fragment runtime JS zostal przeniesiony do realnego resource file:

```text
src/main/resources/uitestlens/runtime/type-hint.js
```

Loader `TypeHintJs` preferuje teraz:

```text
uitestlens/runtime/type-hint.js
```

i zachowuje legacy fallback:

```text
selenium/type-hint.js
```

Type hint runtime rejestruje sie pod primary namespace `window.__uiTestLens.modules.typeHint`.
Warstwa JS nadal uzywa kompatybilnego overlay root:

- primary state: `window.__uiTestLens.state.overlay.root`,
- legacy alias: `window.__seleniumOverlayRoot`.

Semantyka `typeWithHint` pozostaje bez zmian: dymek w overlay moze nadal prezentowac wpisywana wartosc jako `SET: ...`.
Pelne maskowanie wartosci inputow zostaje na pozniejszy etap.

## Scroll arrow JavaScript extraction

Kolejny fragment runtime JS zostal przeniesiony do realnego resource file:

```text
src/main/resources/uitestlens/runtime/scroll-arrow.js
```

Loader `ScrollArrowJs` preferuje teraz:

```text
uitestlens/runtime/scroll-arrow.js
```

i zachowuje legacy fallback:

```text
selenium/scroll-arrow.js
```

Scroll arrow runtime rejestruje sie pod primary namespace `window.__uiTestLens.modules.scrollArrow`.
Semantyka scrollowania, wyrownanie elementu wzgledem viewportu oraz enumy `ScrollElementEdge` / `ScrollViewportEdge` pozostaja bez zmian.

## HUD panel JavaScript extraction

Kolejny fragment runtime JS zostal przeniesiony do realnego resource file:

```text
src/main/resources/uitestlens/runtime/hud-panel.js
```

Loader `HudPanelJs` preferuje teraz:

```text
uitestlens/runtime/hud-panel.js
```

i zachowuje legacy fallback:

```text
selenium/hud-panel.js
```

HUD panel runtime rejestruje sie pod primary namespace `window.__uiTestLens.modules.hud`.
Zachowanie `init`, `setStep` oraz dodawania logow do HUD zostaje bez zmian.
Legacy DOM ID, takie jak `selenium-hud-panel`, `selenium-hud-step` i `selenium-hud-logs`, zostaly zachowane dla kompatybilnosci.

## Assertion badges JavaScript extraction

Kolejny fragment runtime JS zostal przeniesiony do realnego resource file:

```text
src/main/resources/uitestlens/runtime/assertion-badges.js
```

Loader `AssertionBadgesJs` preferuje teraz:

```text
uitestlens/runtime/assertion-badges.js
```

i zachowuje legacy fallback:

```text
selenium/assertion-badges.js
```

Assertion badge runtime rejestruje sie pod primary namespace `window.__uiTestLens.modules.assertionBadges`.
Legacy klasy i wlasciwosci DOM, takie jak `selenium-overlay-assert`, `selenium-assert-badge` i `target.__seleniumAssertContainer`, zostaly zachowane dla kompatybilnosci.
Semantyka assertion pass/fail oraz model `OverlayAssertionResult` pozostaja bez zmian.

Glowne znane runtime resources obejmuja teraz:

- API overlay,
- Wait HUD,
- Highlight,
- Type hint,
- Scroll arrow,
- HUD panel,
- Assertion badges.

Szczegolowy audyt pozostalego inline JavaScript, namespace i fallbackow znajduje sie w:

```text
docs/ui-test-lens-runtime-js-audit.md
```

Legacy aliasy `window.__selenium...` pozostaja celowo utrzymywane do czasu osobnego etapu migracji kompatybilnosciowej.

## Overlay root runtime state cleanup

`OverlayRootManager` traktuje teraz `window.__uiTestLens.state.overlay.root` jako primary reference dla overlay root.
Legacy `window.__seleniumOverlayRoot` jest nadal synchronizowany jako alias kompatybilnosciowy.

Cleanup zachowuje publiczne API `OverlayRootManager` i nie zmienia zachowania HUD/highlight/assertion badges/type hint/scroll arrow.
Dodano testy skryptow root managera bez uruchamiania Selenium ani przegladarki.

Osobny `overlay-root.js` pozostaje opcja na pozniejszy etap tylko wtedy, gdy bootstrap root zacznie rosnac albo bedzie potrzebny poza Selenium.

## PageWaits runtime state cleanup

`PageWaits` uzywa teraz primary runtime state pod `window.__uiTestLens.state`:

- `window.__uiTestLens.state.network.activeRequests` jest primary licznikiem aktywnych requestow,
- `window.__uiTestLens.state.network.trackerInstalled` jest primary flaga instalacji network trackera,
- `window.__uiTestLens.state.wait.lastMessage` jest primary stanem ostatniego komunikatu wait,
- `window.__uiTestLens.state.dom` jest inicjalizowany jako primary namespace dla przyszlych helperow DOM.

Legacy globale pozostaja synchronizowane kompatybilnosciowo:

- `window.__seleniumActiveRequests`,
- `window.__seleniumNetworkTrackerInstalled`,
- `window.__seleniumLastWaitMessage`.

Semantyka waitow, timeouty, tracking `fetch` / `XMLHttpRequest` oraz algorytm DOM stable nie zostaly zmienione.
DOM stable nadal trzyma `MutationObserver` i znaczniki mutacji lokalnie na obserwowanym elemencie root, bo przeniesienie tego stanu do globalnego obiektu nie jest konieczne do obecnego cleanupu.
Pelna ekstrakcja JS z `PageWaits` do resource zostaje na pozniejszy etap.

## Target resolver cleanup

`TargetResolverActions` zostal uporzadkowany bez zmiany algorytmu resolve:

- skrypt resolve click target jest wydzielony do `clickTargetResolverScript()`,
- skrypt resolve file input target jest wydzielony do `fileInputResolverScript()`,
- publiczne metody, fallbacki i wyniki pozostaja bez zmian,
- resolver nie zapisuje runtime state, wiec nie wprowadzono nowych kluczy `window.__uiTestLens.state`,
- nie ma tez potrzeby utrzymywania legacy aliasow `window.__selenium...` w tym obszarze.

Skrypty pozostaja inline w Javie jako page-query snippets, poniewaz sa nadal scisle zwiazane z argumentami Selenium `WebElement`.
Przeniesienie do resource powinno nastapic dopiero po udokumentowaniu semantyki target resolvera, zasad escapowania selektorow i zachowania przy niejednoznacznych dopasowaniach.

## Popup and blocking overlay cleanup

`PopupDetector` i `BlockingOverlayHelper` zostaly uporzadkowane bez zmiany heurystyk:

- skrypty popup detection, global close button, overlay at viewport center i close button inside overlay sa nazwanymi helperami w `PopupDetector`,
- skrypty global overlay close button, blocking overlay for target i close button inside overlay sa nazwanymi helperami w `BlockingOverlayHelper`,
- listy selektorow, keywordy tekstowe, kolejnosc fallbackow i sleep po dismiss pozostaja bez zmian,
- nie dodano runtime state ani nowych globali `window.__uiTestLens` / `window.__selenium...`.

Heurystyki pozostaja inline w Javie jako Selenium helper snippets.
Pelne wydzielenie do resource lub osobnego modulu powinno nastapic dopiero po skonsolidowaniu `PopupDetector` i `BlockingOverlayHelper` oraz opisaniu polityki selektorow/keywordow.

## Module split planning

Powstal precyzyjny plan pierwszego multi-module splitu:

```text
docs/ui-test-lens-module-split-plan.md
```

Roadmapa kolejnych epikow reliability/diagnostics jest w:

```text
docs/ui-test-lens-playwright-inspired-roadmap.md
```

Pierwsze minimalne splity zostaly wykonane: root POM jest parentem, neutralne logging/export oraz `BrowserScriptExecutor` sa w `ui-test-lens-core`, runtime resources i overlay bridge classes sa w `ui-test-lens-overlay`, fasada/akcje Selenium oraz `SeleniumBrowserScriptExecutor` sa w `ui-test-lens-selenium`, a React-safe helpers sa w `ui-test-lens-react`.

Dalszy plan dokumentuje docelowy uklad:

- `ui-test-lens-parent`,
- `ui-test-lens-overlay`,
- `ui-test-lens-selenium`,
- `ui-test-lens-react`,
- `ui-test-lens-examples`.

`ui-test-lens-core` juz istnieje i musi pozostac Selenium-free. `ui-test-lens-overlay` istnieje i nie zalezy juz bezposrednio od Selenium; jego primary API uzywa `BrowserScriptExecutor`. WebDriver-compatible construction jest po stronie `ui-test-lens-selenium` przez `SeleniumOverlayFactory`. `ui-test-lens-selenium` istnieje jako glowny modul Selenium i nie zalezy juz od `ui-test-lens-react`. React-safe helpers sa dostepne po stronie `ui-test-lens-react` przez `ReactSupport`.

## Blocking overlay policy

Pierwszy punkt roadmapy Playwright-inspired reliability zostal zaimplementowany po stronie `ui-test-lens-selenium`. Uzytkownik moze zdefiniowac znane overlaye/popupy przez `OverlayPolicy`, np. cookie banner, newsletter modal albo session expired modal.

Polityka rozroznia overlaye opcjonalne i fatalne. `OverlayAction.fail(...)` przerywa obsluge jako blad, a smart click uruchamia polityke przed kliknieciem i jeszcze raz po Selenium click interception, zanim przejdzie do starych heurystyk `BlockingOverlayHelper`.

`OverlayConfig` pozostaje Selenium-free w module overlay; konfiguracja polityki jest ustawiana na fasadzie Selenium przez `JsOverlayDebug.setOverlayPolicy(...)`. Ten sam executor jest uzywany przez bazowe actionability checks i powinien zostac pozniej wykorzystany przez React-aware readiness.

## Actionability checks

Drugi punkt roadmapy Playwright-inspired reliability zostal zaimplementowany po stronie `ui-test-lens-selenium`. Dodano `ActionabilityChecker`, `ActionabilityOptions`, `ActionabilityReport`, enumy statusow i powodow porazki oraz male Selenium-only skrypty do `getBoundingClientRect()`, `scrollIntoView(...)` i `document.elementFromPoint(...)`.

Pierwszy zakres checkow obejmuje: attached, visible, enabled, stable bounding box, scroll into viewport, click point receiving/not covered oraz konfiguracje `OverlayPolicyExecutor`. `SmartClickActions` uruchamia checker jako best-effort diagnostyke przed dotychczasowym click flow, bez usuwania legacy fallbackow.

## React-aware actionability checks

Trzeci punkt roadmapy Playwright-inspired reliability zostal zaimplementowany po stronie `ui-test-lens-react`. Dodano `ReactActionabilityChecker`, `ReactActionabilityOptions`, `ReactActionabilityReport`, enumy readiness check/failure oraz male React-only skrypty diagnostyczne.

Warstwa React najpierw uzywa bazowego `JsOverlayDebug.checkActionability(...)`, a potem sprawdza sygnaly typowe dla aplikacji React: `aria-disabled`, `aria-busy`, `data-loading`, `data-pending`, `data-state`, progressbar, spinner/loading indicators, skeleton loaders, focus-lock, dialog/modal oraz custom busy/blocking locatory. `ReactSupport.checkActionability(...)` jest publicznym entrypointem. `ui-test-lens-selenium` nadal nie zalezy od React.

## Retryable UI locator API

Czwarty punkt roadmapy Playwright-inspired reliability zostal zaimplementowany po stronie `ui-test-lens-selenium`. Dodano `UiLocator`, `UiLocatorOptions`, `UiLocatorResolver`, `UiLocatorDescription`, `UiLocatorResult`, status/failure enumy oraz `UiLocatorException`.

Locator trzyma `By` i opis zamiast dlugo przechowywac `WebElement`. Akcje `click`, `fill`, `clear`, `pressEnter`, `textContent`, `isVisible`, `isEnabled`, `resolve` i `checkActionability` resolve'uja swiezy element tuz przed uzyciem. `click()` deleguje do istniejacego smart click i overlay policy flow, a akcje retry'uja transient stale/intercept/not-interactable problemy do skonfigurowanego limitu. Publiczne wejscia to `JsOverlayDebug.locator(...)` i `JsOverlayDebug.getByTestId(...)`.

Pelne web-first assertions oraz bogatsze factory typu `getByRole`, `getByLabel`, `getByText` zostaja na kolejny etap.

## Retryable web assertions

Piaty punkt roadmapy Playwright-inspired reliability zostal zaimplementowany po stronie `ui-test-lens-selenium`. Dodano `UiExpect`, `UiAssertionOptions`, `UiAssertionResult`, status/failure enumy, `UiAssertionError` oraz `UiAssertionReporter`.

Retryable assertions bazuja na `UiLocator`, czyli trzymaja `By` i resolve'uja swiezy element przy probach asercji zamiast polegac na dlugowiecznym `WebElement`. Pierwszy zakres obejmuje `toBeVisible`, `toBeHidden`, `toBeEnabled`, `toBeDisabled`, `toHaveText`, `toContainText`, `toHaveValue` i `toContainValue`.

`JsOverlayDebug.expect(By)`, `expect(By, String)`, `expect(UiLocator)` i `expect(UiLocator, UiAssertionOptions)` sa publicznym entrypointem. `UiLocator.expect()` jest wygodnym skrotem. Istniejace `AssertActions` pozostaja bez zmian dla dotychczasowych visual/grouped assertions.

## Business assertions

Szosty punkt roadmapy Playwright-inspired reliability zostal zaimplementowany po stronie `ui-test-lens-selenium`. Dodano `BusinessAssertions`, `BusinessAssertionOptions`, `BusinessAssertionResult`, `BusinessAssertionFailure`, `BusinessAssertionError`, status enum, wewnetrzny model checkow oraz reporter eventow.

Business assertions grupuja techniczne retryable `UiExpect` checks pod biznesowym subjectem, np. `Order summary`. Domyslnie `verify()` wykonuje wszystkie zarejestrowane checki, zbiera wiele failure i rzuca jeden czytelny `BusinessAssertionError`. `BusinessAssertionOptions.failFast(true)` albo `collectFailures(false)` konczy grupe po pierwszej porazce.

Biblioteka nie dodaje domenowych metod typu `shouldShowAmount`; takie DSL powinny powstawac w projekcie testowym albo adapterze examples. Relacja warstw: `UiExpect` = techniczne retryable web assertions, `BusinessAssertions` = czytelne grupy biznesowe, `AssertActions` = istniejaca visual/grouped assertion layer.

## Business step DSL

Siodmy punkt roadmapy Playwright-inspired reliability/diagnostics zostal zaimplementowany po stronie `ui-test-lens-selenium`. Dodano `UiStepScope`, `UiStepOptions`, `UiStepResult`, `UiStepStatus`, `UiStepFailure`, `UiStepError`, `UiStepReporter`, `UiStepContext` oraz wewnetrzny model `UiStep`.

`JsOverlayDebug.step(String, Runnable)` i `step(String, UiStepOptions, Runnable)` wykonuja nazwany krok, mierza start/end/duration, emituja `STEP_STARTED`, `STEP_PASSED`, `STEP_FAILED` albo `STEP_SKIPPED` i opcjonalnie aktualizuja HUD przez istniejace `setStep(...)` oraz `hudLog(...)`. `setStep(...)` pozostaje reczna zmiana etykiety HUD, a `step(...)` jest wykonawczym wrapperem z wynikiem i obsluga bledow.

Domyslnie `UiStepOptions.failFast(true)` opakowuje blad w `UiStepError`. Przy `failFast(false)` step zwraca `UiStepResult` ze statusem `FAILED`. Step dobrze owija `BusinessAssertions.verify()`: `BusinessAssertionError` staje sie cause/failure summary kroku. HTML trace, screenshots i video markers zostaja na kolejne etapy.

## Trace evidence model

Osmy punkt roadmapy diagnostics zostal zaimplementowany neutralnie w `ui-test-lens-core`. Dodano `UiTestLensSession`, `TraceMetadata`, `TraceEvent`, `TraceEventType`, `TraceStatus`, `TraceArtifact`, `TraceArtifactType`, `TraceFailure`, `TraceTimeline`, `TraceStep`, `TraceJsonExporter` oraz `TraceLogSink`.

Model zapisuje metadata sesji, timeline events, failures oraz referencje do artefaktow: screenshoty, video, HTML, JSON, logi tekstowe/browser/network, custom files i custom URLs. Artefakty sa tylko sciezkami albo URL-ami; biblioteka na tym etapie nie robi Selenium screenshot capture, nie nagrywa video i nie renderuje HTML trace.

Po stronie `ui-test-lens-selenium` `JsOverlayDebug` potrafi `startSession(...)`, `attachSession(...)`, zwrocic `session()`, dodac screenshot/video/custom artifact oraz dopisac zdarzenia step DSL do sesji. Pelne mapowanie akcji, asercji, locatorow i overlay policy na trace oraz statyczny HTML renderer zostaja na kolejne etapy.

## Preferowany styl nazw klas Java

Docelowy styl nazw publicznych:

- `UiTestLens`
- `UiTestLensConfig`
- `UiTestLensLogger`
- `UiTestLensEventBus`
- `UiTestLensLogEntry`
- `UiTestLensOverlay`
- `UiTestLensRuntime`
- `UiTestLensActions`

Zasada: `Ui`, nie `UI`, w nazwach klas Java. Daje to zgodnosc z konwencja JavaBeans i czytelne nazwy metod/pol, np. `getUiTestLensConfig()`.

## Nazwy odrzucone

- `JsTestTools` - zbyt ogolne i historyczne; nie opisuje observability/debug layer.
- `js-test-tools` - zbyt techniczne i zbyt zwiazane z JavaScriptem jako implementacja.
- `browser-test-lens` - dobre technicznie, ale nowe wymaganie produktowe preferuje szersze `UI Test Lens`.
- `test-lens` - zbyt szerokie; nie wskazuje UI/browser automation.
- `selenium-test-tools` - zbyt waskie; projekt ma miec adaptery nie tylko dla Selenium.
- `selenium-overlay-debug` - opisuje tylko fragment obecnych funkcji.
- `overlay-test-tools` - zbyt waskie, bo projekt obejmuje tez akcje, event-bus, logi, eksport i adaptery.

## Mapowanie starego nazewnictwa na nowe

| Obecnie/historycznie | Docelowo | Uwagi |
| --- | --- | --- |
| nazwa projektu historyczna | `ui-test-lens` | Nazwa repo/root project/parent POM. |
| produkt | `UI Test Lens` | Nazwa w README, opisach i dokumentacji. |
| obecny single-module artifact | `ui-test-lens` albo parent + moduly | Decyzja w osobnym commicie POM cleanup. |
| obecny root package | `io.github.mmaciekk111.uitestlens` | Dla publikacji publicznej. |
| obecny root package | `pl.mmaciekk111.uitestlens` | Dla projektu prywatnego/lokalnego. |
| `JsOverlayDebug` | `UiTestLens` albo `UiTestLensSelenium` | Rename dopiero po odchudzeniu fasady. |
| `OverlayConfig` | `UiTestLensConfig` | Rename razem z modularyzacja konfiguracji. |
| `OverlayWait` | `UiTestLensWaits` albo `ObservedUiWait` | Rename po wydzieleniu wait HUD. |
| `HudPanel` | `UiTestLensHud` | Docelowo internal lub publiczny komponent overlay. |
| `ApiOverlayPanel` | `UiTestLensApiOverlay` | Modul opcjonalny. |
| `ReactSafeExecutor` | `UiTestLensReactActions` albo `ReactSafeActions` | Modul `ui-test-lens-react`. |

## Czego nie rename'owac w pierwszym kroku

W pierwszym kroku nie rename'owac:

- deklaracji pakietow Java,
- importow Java,
- klas publicznych takich jak `JsOverlayDebug`, `OverlayConfig`, `OverlayWait`,
- metod publicznych,
- obecnych globali runtime JS,
- obecnych sciezek zasobow JS, jesli kod ich nadal oczekuje,
- docelowych modulow Maven, dopoki projekt jest jeszcze jednym modulem.

Powod: te zmiany dotykaja publicznego API, zasobow runtime i potencjalnych testow downstream. Bezpieczniej najpierw ustabilizowac layout Maven i dokumentacje, a dopiero potem zrobic mechaniczne rename w izolowanych commitach.

## Plan bezpiecznej migracji nazewnictwa w commitach

1. Commit 1: standardowy layout Maven.
   - Kod w `src/main/java`.
   - Katalogi `src/main/resources`, `src/test/java`, `src/test/resources`.
   - Bez zmiany pakietow i zachowania.

2. Commit 2: naming docs.
   - Uzgodnic `ui-test-lens` jako nazwe projektu.
   - Uzgodnic publiczny albo prywatny base package.
   - Uzgodnic namespace `window.__uiTestLens`.

3. Commit 3: POM cleanup.
   - Uporzadkowac `artifactId`, `name`, `description`.
   - Uzyc jednej wersji Selenium z property.
   - Udokumentowac lub wydzielic brakujace zaleznosci prywatne i RestAssured.

4. Commit 4: resources cleanup.
   - Przeniesc JS runtime do `src/main/resources/uitestlens/runtime/`.
   - Wprowadzic loader runtime.
   - Zachowac przejsciowe aliasy/sciezki tylko jesli sa potrzebne.

5. Commit 5: runtime namespace.
   - Wprowadzic `window.__uiTestLens`.
   - Ograniczyc stare globale do aliasow kompatybilnosciowych albo usunac po decyzji.

6. Commit 6: package rename.
   - Wykonane: kod Java przeniesiono mechanicznie do `io.github.mmaciekk111.uitestlens`.
   - Nie zmieniono logiki.
   - Importy, testy i kompilacja pozostaja do weryfikacji w commicie rename.

7. Commit 7: Maven coordinates cleanup.
   - Wykonane: `groupId` ustawiono na `io.github.mmaciekk111`.
   - Aktualne coordinates: `io.github.mmaciekk111:ui-test-lens:0.1.0-SNAPSHOT`.

8. Commit 8: facade/API rename.
   - Zmienic publiczne nazwy na styl `UiTestLens*`.
   - Jesli trzeba, zostawic deprecated aliases na okres przejsciowy.

9. Commit 9: multi-module split.
   - Wydzielic `core`, `overlay`, `selenium`, `react`, `selenide`, `examples`.
   - Moduly opcjonalne wydzielac dopiero po ustabilizowaniu core event/log/runtime API.

## Decyzja dla pierwszego etapu cleanupu

Aktualny etap powinien pozostac bezpieczny:

- dokumentacja wskazuje `ui-test-lens` jako nazwe docelowa,
- standardowy layout Maven zostaje,
- package rename zostal wykonany pozniej w osobnym commicie,
- nie ma zmiany runtime JS,
- nie ma rozbijania modulu,
- nie ma refaktoru klas i API.

## Etap 2: POM cleanup i zależności

Aktualny single-module POM pozostaje projektem przejściowym przed multi-module split.

Wykonane zmiany POM:

- `artifactId` ustawiony na `ui-test-lens`,
- `groupId` ustawiony na `io.github.mmaciekk111`,
- dodane `name`: `UI Test Lens`,
- dodane `description`: `Visual observability and debug layer for UI/browser automation tests.`,
- dodane `project.reporting.outputEncoding=UTF-8`,
- Selenium ujednolicone przez `${selenium.version}`,
- Lombok ujednolicony przez `${lombok.version}`,
- kompilator ustawiony na `maven.compiler.release=17`, bo obecny kod używa składni nowszej niż Java 11 (`record`, pattern matching `instanceof`, switch arrows).

Decyzje odłożone:

- package rename wykonano pozniej mechanicznie do `io.github.mmaciekk111.uitestlens`,
- brak multi-module split,
- brak zmiany namespace runtime JS,
- brak refaktoru `JsOverlayDebug`.

Zależności blokujące czystą publikację:

- RestAssured w `ApiCallActions` nie powinien trafić do głównego artifactu; docelowo osobny adapter `ui-test-lens-restassured`,
- `LogWraper` i `TimeStamp` są prywatnymi zależnościami i powinny zostać zastąpione przez `UiTestLensLogger`, `UiTestLensEventBus`, `Clock` i sinki logów,
- `ContentIssueCollector` i `LocalDateTimeUtils` w `OverlayContentAssertions` powinny trafić do examples albo prywatnego adaptera, nie do publicznego core.

Szczegóły decyzji są w `docs/ui-test-lens-dependency-cleanup-plan.md`.

## Etap 3: izolacja zewnętrznych blockerów

Wykonane zmiany:

- RestAssured został usunięty z głównego kodu `ApiCallActions`; przyszły adapter powinien trafić do `ui-test-lens-restassured`,
- prywatny `LogWraper` został zastąpiony neutralnym `OverlayLogger` z noop implementacją,
- prywatny `TimeStamp` został zastąpiony przez JDK `Clock` i `DateTimeFormatter`,
- `OverlayContentAssertions` przeniesiono z `src/main/java` do `docs/examples/OverlayContentAssertions.java.example`, bo zależy od prywatnego `ContentIssueCollector` i `LocalDateTimeUtils`.

Decyzje odłożone:

- docelowa nazwa `OverlayLogger` jako `UiTestLensLogger`,
- pełny event-bus,
- adapter RestAssured,
- adapter prywatnego content collectora,
- namespace runtime `window.__uiTestLens`.
