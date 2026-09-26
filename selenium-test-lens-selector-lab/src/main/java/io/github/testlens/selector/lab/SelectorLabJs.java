package io.github.testlens.selector.lab;

import io.github.testlens.utils.JsResources;

final class SelectorLabJs {
    static final String INIT=JsResources.load("uitestlens/runtime/selector-lab.js");
    static final String BRIDGE="var lab=window.__uiTestLens&&window.__uiTestLens.modules&&window.__uiTestLens.modules.selectorLab;";
    private SelectorLabJs(){}
}
