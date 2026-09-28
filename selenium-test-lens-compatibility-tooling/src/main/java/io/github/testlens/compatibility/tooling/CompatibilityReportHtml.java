package io.github.testlens.compatibility.tooling;

import io.github.testlens.compatibility.engine.CompatibilityComparisonReport;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

/** Standalone, no-network HTML rendering of the canonical comparison model. */
public final class CompatibilityReportHtml {
    public static final long MAX_OUTPUT_BYTES=64L*1024*1024;
    private final long maximumOutputBytes;

    public CompatibilityReportHtml(){this(MAX_OUTPUT_BYTES);}
    CompatibilityReportHtml(long maximumOutputBytes){if(maximumOutputBytes<1||maximumOutputBytes>MAX_OUTPUT_BYTES)throw new IllegalArgumentException("invalid report output bound");this.maximumOutputBytes=maximumOutputBytes;}

    public byte[] write(CompatibilityComparisonReport report){
        Objects.requireNonNull(report);
        try{return BoundedByteRendering.render(maximumOutputBytes,out->render(report,out));}
        catch(IOException failure){if(limitExceeded(failure))throw new CompatibilityReportJson.ReportFormatException("OUTPUT_LIMIT_EXCEEDED",failure);throw new CompatibilityReportJson.ReportFormatException("HTML report cannot be serialized",failure);}
    }

    public Path writeDefault(CompatibilityComparisonReport report,Path projectRoot)throws IOException{Path destination=CompatibilityReportJson.safeDestination(projectRoot,Path.of("target","test-lens","compatibility","reports","compatibility-v1.html"),"compatibility-v1.html");CompatibilityReportJson.atomic(destination,write(report));return destination;}

    private static void render(CompatibilityComparisonReport r,OutputStream output)throws IOException{
        Writer w=new BufferedWriter(new OutputStreamWriter(output,StandardCharsets.UTF_8),8192);
        w.write("<!doctype html><html lang=\"en\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Test Lens compatibility report</title><style>body{font:14px system-ui;margin:2rem;color:#18202b}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ccd3dc;padding:.45rem;text-align:left;vertical-align:top}select{margin:0 .7rem .8rem 0}.warn{font-weight:700}code{overflow-wrap:anywhere}</style><body>");
        w.write("<h1>Compatibility comparison</h1><p><b>Axis:</b> ");escape(w,r.intent().axis().name());w.write(" · ");escape(w,r.intent().baselineRole());w.write(" vs ");escape(w,r.intent().variantRole());w.write("</p><p class=\"warn\">Coverage applies only to supplied matched invocations; this is not a whole-application guarantee.</p>");
        w.write("<h2>Coverage</h2><p>Matched: ");w.write(Integer.toString(r.coverage().matched()));w.write("; regressions: ");w.write(Integer.toString(r.coverage().regressions()));w.write("; behavioral deltas: ");w.write(Integer.toString(r.coverage().behavioralDeltas()));w.write("; ambiguous: ");w.write(Integer.toString(r.coverage().ambiguous()));w.write(".</p>");
        w.write("<label>Outcome <select id=\"outcome\"><option value=\"\">all</option></select></label><label>Comparability <select id=\"comparability\"><option value=\"\">all</option></select></label><label>Severity <select id=\"severity\"><option value=\"\">all</option></select></label><label>Category <select id=\"category\"><option value=\"\">all</option></select></label><label>Causal state <select id=\"causal\"><option value=\"\">all</option></select></label>");
        w.write("<h2>Test comparisons</h2><table><thead><tr><th>Test</th><th>Results</th><th>Outcome</th><th>Comparability</th><th>Behavior/evidence</th><th>Recommendations</th><th>Code change</th></tr></thead><tbody>");
        for(var x:r.testComparisons()){
            w.write("<tr data-outcome=\"");escape(w,x.outcome().name());w.write("\" data-comparability=\"");escape(w,x.comparability().status().name());w.write("\"><td><code>");escape(w,x.testIdentityRef());w.write("</code></td><td>");escape(w,x.baseline().terminalStatus().name());w.write(" → ");escape(w,x.variant().terminalStatus().name());w.write("<br>attempts ");w.write(Integer.toString(x.baseline().attemptCount()));w.write(" → ");w.write(Integer.toString(x.variant().attemptCount()));w.write("</td><td>");escape(w,x.outcome().name());w.write("</td><td>");escape(w,x.comparability().status().name());w.write("<br>");escape(w,String.join(", ",x.comparability().reasonCodes()));w.write("</td><td>");
            for(var group:x.behaviorDiff().groups()){escape(w,group.group());w.write(": ");escape(w,group.state().name());w.write("<br>");}
            w.write("</td><td>");escape(w,x.recommendationCodes().toString());w.write("<br>");escape(w,x.verificationScope().toString());w.write("</td><td>");escape(w,x.codeChangeRequired().name());w.write("</td></tr>");
        }
        w.write("</tbody></table><h2>Findings</h2><table><thead><tr><th>Code</th><th>Category</th><th>Severity</th><th>Causal state/confidence</th><th>Evidence</th></tr></thead><tbody>");
        for(var f:r.findings()){w.write("<tr data-severity=\"");escape(w,f.severity().name());w.write("\" data-category=\"");escape(w,f.category().name());w.write("\" data-causal=\"");escape(w,f.causalState().name());w.write("\"><td>");escape(w,f.code());w.write("</td><td>");escape(w,f.category().name());w.write("</td><td>");escape(w,f.severity().name());w.write("</td><td>");escape(w,f.causalState().name());w.write(" / ");escape(w,f.confidence().name());w.write("</td><td>");escape(w,f.evidence().toString());w.write("</td></tr>");}
        w.write("</tbody></table><h2>Limitations</h2><p>");escape(w,r.outputLimitations().toString());w.write("</p><script>(()=>{const ids=['outcome','comparability','severity','category','causal'];for(const id of ids){const key=id;const rows=[...document.querySelectorAll('tr[data-'+key+']')];const sel=document.getElementById(id);for(const v of [...new Set(rows.map(r=>r.dataset[key]))].sort()){const o=document.createElement('option');o.value=v;o.textContent=v;sel.append(o)}sel.addEventListener('change',()=>{for(const row of rows)row.hidden=!!sel.value&&row.dataset[key]!==sel.value})}})()</script></body></html>\n");
        w.flush();
    }

    private static void escape(Writer writer,String value)throws IOException{if(value==null)return;for(int i=0;i<value.length();i++){char c=value.charAt(i);switch(c){case '&'->writer.write("&amp;");case '<'->writer.write("&lt;");case '>'->writer.write("&gt;");case '\"'->writer.write("&quot;");case '\''->writer.write("&#39;");default->writer.write(c);}}}
    static String e(String value){if(value==null)return"";return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    private static boolean limitExceeded(Throwable failure){for(Throwable current=failure;current!=null;current=current.getCause())if(current instanceof BoundedByteRendering.LimitExceeded)return true;return false;}
}
