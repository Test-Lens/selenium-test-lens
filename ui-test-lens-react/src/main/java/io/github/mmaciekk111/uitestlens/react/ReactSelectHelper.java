package io.github.mmaciekk111.uitestlens.react;

import org.openqa.selenium.*;

/**
 * Helper do obsługi komponentów React-Select przez JavaScript.
 */
public class ReactSelectHelper {

    private final WebDriver driver;

    public ReactSelectHelper(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Wylicza baseId (react-select-XX) na podstawie grupy (kontenera) i comboboxa.
     */
    public String resolveReactSelectBaseId(WebElement group, WebElement combo) {
        Object v = ((JavascriptExecutor) driver).executeScript(
                "var g=arguments[0], c=arguments[1];" +
                        "if(!g) return null;" +
                        "if(c){ var ctr=c.getAttribute('aria-controls');" +
                        "  if(ctr) return ctr.replace(/-listbox$/,''); }" +
                        "var lr=g.querySelector('[id^=\"react-select-\"][id$=\"-live-region\"]');" +
                        "if(lr && lr.id) return lr.id.replace(/-live-region$/,'');" +
                        "var ph=g.querySelector('[id^=\"react-select-\"][id$=\"-placeholder\"]');" +
                        "if(ph && ph.id) return ph.id.replace(/-placeholder$/,'');" +
                        "if(c){ var d=c.getAttribute('aria-describedby');" +
                        "  if(d && d.indexOf('react-select-')===0) return d.replace(/-placeholder$/,''); }" +
                        "return null;",
                group, combo
        );
        return v == null ? null : String.valueOf(v).trim();
    }

    /**
     * Kliknięcie opcji react-select przez JS (bez WebElement) — minimalizuje StaleElement.
     * Szuka elementów po id: baseId-option-*
     */
    public boolean jsClickReactSelectOptionContaining(String baseId, String needle) {
        Object r = ((JavascriptExecutor) driver).executeScript(
                "var base=arguments[0], needle=arguments[1];" +
                        "if(!base||!needle) return false;" +
                        "var sel=\"[id^='\"+base+\"-option-']\";" +
                        "var opts=document.querySelectorAll(sel);" +
                        "for(var i=0;i<opts.length;i++){" +
                        "  var o=opts[i];" +
                        "  if(!o) continue;" +
                        "  var visible = !!(o.offsetParent) && (getComputedStyle(o).visibility!=='hidden');" +
                        "  if(!visible) continue;" +
                        "  var t=(o.textContent||o.innerText||'').trim();" +
                        "  if(t.indexOf(needle)!==-1){" +
                        "    try{ o.scrollIntoView({block:'nearest'});}catch(e){}" +
                        "    o.click();" +
                        "    return true;" +
                        "  }" +
                        "}" +
                        "return false;",
                baseId, needle
        );
        return r instanceof Boolean && (Boolean) r;
    }

    public void pickByLabel(ReactOverlaySupport overlay,
                           String labelContains,
                           String valueToType,
                           String hiddenName,
                           long timeoutMs,
                           String xpathPrefix) {
        final String needle = valueToType == null ? "" : valueToType.trim();
        if (needle.isEmpty()) throw new IllegalArgumentException("valueToType must not be blank");

        By groupBy = By.xpath(xpathPrefix + "//div[@role='group' and not(ancestor-or-self::*[@hidden]) and .//label[contains(normalize-space(.), " + xpathLiteral(labelContains) + ")]]");
        By comboBy = By.xpath(xpathPrefix + "//div[@role='group' and not(ancestor-or-self::*[@hidden]) and .//label[contains(normalize-space(.), " + xpathLiteral(labelContains) + ")]]//input[@role='combobox']");
        By hiddenBy = By.xpath(xpathPrefix + "//div[@role='group' and not(ancestor-or-self::*[@hidden]) and .//label[contains(normalize-space(.), " + xpathLiteral(labelContains) + ")]]//input[@type='hidden' and @name=" + xpathLiteral(hiddenName) + "]");

        long end = System.currentTimeMillis() + timeoutMs;

        // 1) wpisz do comboboxa + pobudź listę
        overlay.reactSafe().doWithRetry(comboBy, "TYPE_COMBO: " + labelContains, combo -> {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center',inline:'nearest'});" +
                            "try{arguments[0].focus();}catch(e){}",
                    combo
            );
            combo.click();
            combo.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            combo.sendKeys(Keys.BACK_SPACE);
            combo.sendKeys(needle);
            return true;
        });

        overlay.reactSafe().doWithRetry(comboBy, "ARROWDOWN: " + labelContains, combo -> {
            combo.sendKeys(Keys.ARROW_DOWN);
            return true;
        });

        // 2) resolve baseId
        final String[] baseIdHolder = new String[1];
        overlay.reactSafe().doWithRetry(comboBy, "BASEID: " + labelContains, combo -> {
            WebElement group = driver.findElement(groupBy);
            String baseId = resolveReactSelectBaseId(group, combo);
            if (baseId == null || baseId.isBlank()) throw new NoSuchElementException("baseId not ready");
            baseIdHolder[0] = baseId;
            return true;
        });

        // 3) wybierz opcję przez JS
        boolean clicked = false;
        while (System.currentTimeMillis() < end) {
            if (jsClickReactSelectOptionContaining(baseIdHolder[0], needle)) {
                clicked = true;
                break;
            }
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        }
        if (!clicked) throw new NoSuchElementException("No option containing: " + needle + " for " + labelContains);

        // 4) potwierdź po hidden
        overlay.reactSafe().doWithRetry(hiddenBy, "CONFIRM: " + labelContains, h -> {
            long confirmEnd = System.currentTimeMillis() + 8000;
            while (System.currentTimeMillis() < confirmEnd) {
                String v = h.getAttribute("value");
                if (v != null && v.trim().contains(needle)) return true;
                try { Thread.sleep(120); } catch (InterruptedException ignored) {}
            }
            throw new NoSuchElementException("Hidden not updated for " + labelContains + ", current=" + h.getAttribute("value"));
        });
    }

    public String textContent(WebElement el) {
        if (el == null) return "";
        Object r = ((JavascriptExecutor) driver).executeScript("return (arguments[0].textContent || arguments[0].innerText || '').trim();", el);
        return r == null ? "" : String.valueOf(r).trim();
    }

    public static String xpathLiteral(String s) {
        if (s == null) return "''";
        if (!s.contains("'")) return "'" + s + "'";
        if (!s.contains("\"")) return "\"" + s + "\"";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    public static String cssEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }
}
