package io.github.testlens.selector.lab;

import io.github.testlens.selector.engine.ObservationEvidence;
import io.github.testlens.selector.engine.PolicyWorkspaceSnapshot;
import io.github.testlens.selector.engine.SelectorPolicy;
import io.github.testlens.selector.engine.SelectorStabilityEngine;
import io.github.testlens.selector.engine.SelectorSubject;
import io.github.testlens.selector.engine.StabilityAssessment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sanitized Java-side projection of the current merged policy workspace. */
final class SelectorLabPolicyView {
    private SelectorLabPolicyView() { }

    static Map<String, Object> project(PolicyWorkspaceSnapshot workspace, SelectorSubject subject,
                                       ObservationEvidence evidence) {
        StabilityAssessment assessment = new SelectorStabilityEngine().analyze(subject, evidence, workspace.compiled());
        StabilityAssessment.PolicyEvaluation evaluation = assessment.policyEvaluation();
        Map<String, PolicyWorkspaceSnapshot.OriginRule> byId = new java.util.TreeMap<>();
        workspace.originRules().forEach(value -> byId.putIfAbsent(value.rule().ruleId(), value));
        List<Map<String, Object>> matching = new ArrayList<>();
        for (PolicyWorkspaceSnapshot.OriginRule value : workspace.originRules()) {
            SelectorPolicy.Rule rule = value.rule();
            if (!rule.scope().matches(subject) || !rule.matcher().matches(subject)) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ruleId", rule.ruleId());
            row.put("origin", value.origin().name());
            row.put("decision", rule.decision().name());
            row.put("scope", scope(rule.scope()));
            row.put("scopeChoice", scopeChoice(rule.scope()));
            row.put("matcher", rule.matcher().kind() == SelectorPolicy.MatcherKind.EXACT_VALUE_DIGEST ? "EXACT" : "STRUCTURAL_PATTERN");
            row.put("priority", rule.priority());
            row.put("selected", rule.ruleId().equals(evaluation.selectedRuleId()));
            row.put("shadowed", evaluation.matchedRuleIds().contains(rule.ruleId()) && !rule.ruleId().equals(evaluation.selectedRuleId()));
            matching.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("effective", evaluation.conflict() ? "CONFLICT" : evaluation.selectedDecision() == null ? "NONE" : evaluation.selectedDecision().name());
        result.put("selectedRuleId", evaluation.selectedRuleId());
        PolicyWorkspaceSnapshot.OriginRule selected = evaluation.selectedRuleId() == null
                ? null
                : byId.get(evaluation.selectedRuleId());
        result.put("selectedOrigin", selected == null ? null : selected.origin().name());
        result.put("policyConflict", evaluation.conflict());
        result.put("evidenceConflict", assessment.effectiveDisposition() == StabilityAssessment.EffectiveDisposition.POLICY_EVIDENCE_CONFLICT);
        result.put("matchingRules", matching);
        result.put("shadowedCount", matching.stream().filter(row -> Boolean.TRUE.equals(row.get("shadowed"))).count());
        result.put("crossOriginMoveMessage", "Moving a rule between policy files requires separate remove/add operations and is not atomic in V1.");
        return result;
    }

    static List<Map<String, Object>> scopes(SelectorLabRequest request) {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(scopeOption("DECLARATION", "This declaration", request.declarationRef() != null,
                "No correlated declaration is available"));
        result.add(scopeOption("FILE", "This file", request.logicalPath() != null,
                "No logical source path is available"));
        result.add(scopeOption("MODULE", "This module", request.modulePath() != null,
                "No module identity is available"));
        result.add(scopeOption("PROJECT", "Project", true,
                "Project scope requires explicit selection"));
        if (request.declarationRef() == null) {
            boolean narrow = request.usageClass() != null || request.usageMethod() != null || request.contextFingerprint() != null;
            result.add(scopeOption("RUNTIME_NARROW", runtimeLabel(request), narrow,
                    "No reliable persistent scope is available for this runtime-only target"));
        }
        return result;
    }

    static String defaultScope(SelectorLabRequest request) {
        if (request.declarationRef() != null) return "DECLARATION";
        if (request.usageClass() != null || request.usageMethod() != null || request.contextFingerprint() != null)
            return "RUNTIME_NARROW";
        return null;
    }

    private static Map<String, Object> scopeOption(String value, String label, boolean enabled, String unavailableReason) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", value);
        result.put("label", label);
        result.put("enabled", enabled);
        result.put("unavailableReason", enabled ? null : unavailableReason);
        result.put("explicit", "PROJECT".equals(value));
        return result;
    }

    private static String runtimeLabel(SelectorLabRequest request) {
        List<String> parts = new ArrayList<>();
        if (request.usageClass() != null) parts.add("class " + request.usageClass());
        if (request.usageMethod() != null) parts.add("method " + request.usageMethod());
        if (request.contextFingerprint() != null) parts.add("known context");
        return "Runtime scope: " + String.join(" / ", parts);
    }

    private static String scope(SelectorPolicy.Scope scope) {
        return switch (scopeChoice(scope)) {
            case "DECLARATION" -> "This declaration";
            case "FILE" -> "This file";
            case "MODULE" -> "This module";
            case "RUNTIME_NARROW" -> "Runtime usage/context";
            default -> "Project";
        };
    }

    private static String scopeChoice(SelectorPolicy.Scope scope) {
        if (scope.declarationRef() != null) return "DECLARATION";
        if (scope.logicalPath() != null) return "FILE";
        if (scope.modulePath() != null) return "MODULE";
        if (scope.usageClass() != null || scope.usageMethod() != null || scope.contextFingerprint() != null)
            return "RUNTIME_NARROW";
        return "PROJECT";
    }
}
