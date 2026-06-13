package io.github.mmaciekk111.uitestlens.core;

import io.github.mmaciekk111.uitestlens.utils.JsResources;

public final class TypeHintJs {

    public static final String INIT =
            UiTestLensRuntimeNames.ensureNamespaceScript() +
                    JsResources.readFirstExisting(
                            UiTestLensRuntimeNames.TYPE_HINT_RESOURCE,
                            UiTestLensRuntimeNames.LEGACY_TYPE_HINT_RESOURCE
                    ) +
                    bridgeScript();

    private TypeHintJs() {}

    public static String bridgeScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript() +
                "var typeHint = window.__uiTestLens.modules.typeHint;";
    }
}
