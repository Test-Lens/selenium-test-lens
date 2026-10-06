package io.github.testlens.application.tooling.codegen;

import java.util.Comparator;
import java.util.List;

/** Generated source and traceable ApplicationModel element projections. @since 0.5.0 */
public record GeneratedPageObject(String pageId, String generatedClassName, String extensionClassName,
                                  String generatedSource, String extensionSource,
                                  List<ElementBinding> elementBindings, List<String> warnings) {
    public GeneratedPageObject {
        elementBindings = elementBindings.stream().sorted(Comparator.comparing(ElementBinding::elementId)).toList();
        warnings = warnings.stream().distinct().sorted().toList();
    }
    /** Concise alias used by tooling consumers. */
    public List<ElementBinding> bindings() { return elementBindings; }
    public record ElementBinding(String elementId, String fieldName, String candidateId, String selectorQuality) { }
}
