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

Dla obecnego jednego modulu przejsciowego rekomendacja: nie zmieniac artifactId razem z przenosinami layoutu Maven. Zmiana na przejsciowe `ui-test-lens` albo od razu parent/module coordinates powinna byc osobnym commitem po uporzadkowaniu zaleznosci POM.

## Rekomendowany groupId

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

Obecny groupId w POM nalezy traktowac jako historyczny/roboczy, nie jako docelowe koordynaty biblioteki.

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
   - Uporzadkowac `groupId`, `artifactId`, `name`, `description`.
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
   - Mechanicznie przeniesc obecny kod do wybranego base package.
   - Nie zmieniac logiki.
   - Zweryfikowac importy i kompilacje.

7. Commit 7: facade/API rename.
   - Zmienic publiczne nazwy na styl `UiTestLens*`.
   - Jesli trzeba, zostawic deprecated aliases na okres przejsciowy.

8. Commit 8: multi-module split.
   - Wydzielic `core`, `overlay`, `selenium`, `react`, `selenide`, `examples`.
   - Moduly opcjonalne wydzielac dopiero po ustabilizowaniu core event/log/runtime API.

## Decyzja dla aktualnego etapu

Aktualny etap powinien pozostac bezpieczny:

- dokumentacja wskazuje `ui-test-lens` jako nazwe docelowa,
- standardowy layout Maven zostaje,
- nie ma globalnego rename pakietow,
- nie ma zmiany runtime JS,
- nie ma rozbijania modulu,
- nie ma refaktoru klas i API.

