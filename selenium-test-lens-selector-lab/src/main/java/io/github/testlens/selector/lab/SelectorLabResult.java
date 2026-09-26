package io.github.testlens.selector.lab;

import java.util.List;

/** Terminal result of one explicit caller-thread Lab session. */
public record SelectorLabResult(SelectorLabState state,List<String>issues,boolean scriptTimeoutRestored,int acceptedCommands,int rejectedCommands,
                                io.github.testlens.selector.engine.PolicyDraft.PendingPolicyChange pendingPolicyChange){
    public SelectorLabResult{issues=List.copyOf(issues);}
    public SelectorLabResult(SelectorLabState state,List<String>issues,boolean scriptTimeoutRestored,int acceptedCommands,int rejectedCommands){this(state,issues,scriptTimeoutRestored,acceptedCommands,rejectedCommands,null);}
}
