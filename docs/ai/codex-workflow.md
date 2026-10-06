# Konfiguracja Codexa dla Selenium Test Lens

Status: zestaw przygotowany bez dostępu do repo Lensa. Walidacja składni nie zastępuje próby w zainstalowanym kliencie Codexa.

## Wdrożenie

Przed zastosowaniem patcha sprawdź git status oraz istniejące AGENTS.md, .codex/config.toml, .codex/agents i .agents/skills. Patch dodaje pliki, dlatego kolizje wymagają scalenia, a nie nadpisania. Zachowaj istniejące reguły projektu. W razie kolizji przenieś treść nowych plików ręcznie, porównując ją z obecną konfiguracją. Nie zmieniaj globalnych ustawień użytkownika.

Zweryfikuj aktualną wersję klienta, obsługę własnych agentów TOML i klucza max_concurrent_threads_per_session. Sprawdź rozpoznawanie pięciu agentów i pięciu skilli; jeśli konfiguracja jest odrzucana, dopasuj składnię do dokumentacji tej wersji. Przy braku narzędzi delegacji stosuj role sekwencyjnie i zgłoś ten fakt.

Modele są dziedziczone z rodzica, aby nie wymuszać modelu niedostępnego na koncie. Po sprawdzeniu dostępu można dopisać model i model_reasoning_effort osobno w plikach agentów. Sam AGENTS.md nie przełącza modeli. Orkiestratorem pozostaje główna sesja; limit to trzy równolegle aktywne wątki delegowane, zależnie od semantyki klienta.

## Próba działania

Zleć niewielką, rzeczywistą zmianę w Lensie. Explorer ma ustalić kontrakt i polecenia walidacji. Orkiestrator przydziela rozłączne pliki implementerowi i test engineerowi. Po integracji reviewer sprawdza całość; docs engineer aktualizuje przykład, jeśli zmieniło się publiczne API. Potwierdź realne uruchomienie agentów na podstawie widocznych zdarzeń klienta, a nie samej deklaracji w odpowiedzi. Zapisz czas, wyniki testów, liczbę poprawek po review oraz zużycie, jeśli klient je udostępnia. Porównaj z podobnym zadaniem wykonanym bez delegacji.

## Prompt wdrożeniowy

Wdróż do tego repo konfigurację z lens-codex-kit.patch. Najpierw przeczytaj istniejące instrukcje i sprawdź status Git. Scal AGENTS.md i .codex/config.toml; zachowaj obecne reguły, modele, MCP i ustawienia uprawnień. Dopasuj składnię konfiguracji do mojego klienta Codexa. Sprawdź rzeczywiste moduły Lensa, wersje i polecenia CI; uzupełnij instrukcje tylko na podstawie kodu. Nie implementuj przy okazji auth bridge ani innej funkcji produktu. Zweryfikuj wykrywanie agentów i skilli, a jeśli nie możesz uruchomić klienta, wskaż dokładnie niewykonane sprawdzenie. Przeprowadź próbę delegacji read-only: explorer mapuje eksport stanu uwierzytelnienia, reviewer niezależnie ocenia jego kontrakt. Zintegruj wyniki. Nie pushuj ani nie publikuj.

## Źródła konfiguracji

- https://learn.chatgpt.com/docs/agent-configuration/subagents
- https://learn.chatgpt.com/docs/build-skills

Sprawdzone 2026-10-06. Zestaw nie dodaje zależności ani kodu do biblioteki Lens i nie instaluje skilli globalnie.
