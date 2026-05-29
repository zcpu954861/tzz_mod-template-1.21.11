package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.webadmin.WebAdminFrontendAssets;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WebAdminPerformanceBaselineGuardTest {
    private static final int PHASE6_APP_JS_BEFORE_BYTES = 1_843_648;
    private static final String PHASE6_APP_JS_BEFORE_SHA256 = "057e7e370d555036aff6d542b3ae4361f82d734b8fa95cf429d4d7ac7425beb3";
    private static final int PHASE6_APP_JS_WARNING_LIMIT_BYTES = 1_880_521;

    private static final Map<String, Map<String, String>> PHASE6_DOM_BASELINES = phase6DomBaselines();

    private WebAdminPerformanceBaselineGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        CodeQualityGuardSupport.GuardReport report = new CodeQualityGuardSupport.GuardReport("9.1.1 WebAdmin performance baseline guard");
        run(report);
        report.printAndFail();
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws Exception {
        String appJs = WebAdminFrontendAssets.appJs();
        String appCss = WebAdminFrontendAssets.appCss();
        int appJsBytes = appJs.getBytes(StandardCharsets.UTF_8).length;
        int appCssBytes = appCss.getBytes(StandardCharsets.UTF_8).length;
        report.metric("performance.baseline.app_js.bytes", appJsBytes);
        report.metric("performance.baseline.app_css.bytes", appCssBytes);
        report.metric("performance.phase", "phase6-logic-chain-performance-baseline");
        report.metric("performance.app_js.before.bytes", PHASE6_APP_JS_BEFORE_BYTES);
        report.metric("performance.app_js.before.sha256", PHASE6_APP_JS_BEFORE_SHA256);
        report.metric("performance.app_js.after.bytes", appJsBytes);
        report.metric("performance.app_js.after.sha256", sha256Hex(appJs));
        report.metric("performance.app_js.delta.bytes", appJsBytes - PHASE6_APP_JS_BEFORE_BYTES);
        if (appJsBytes > PHASE6_APP_JS_WARNING_LIMIT_BYTES) {
            report.warning("Phase 6 app.js bytes exceeded current + 2% warning limit: actual="
                    + appJsBytes + " limit=" + PHASE6_APP_JS_WARNING_LIMIT_BYTES);
        }
        report.requireContains(appJs, "data-logic-chain-render-perf-markers", "Phase 6 performance marker registry");
        report.requireContains(appJs, "function logicChainRelatedNodeIndex", "Phase 6 related node precomputed map marker");
        report.requireContains(appJs, "function logicChainMinimapKey", "Phase 6 minimap memo key marker");

        Path output = CodeQualityGuardSupport.projectRoot().resolve("build/tmp/webadmin-app.js");
        Files.createDirectories(output.getParent());
        Files.writeString(output, appJs, StandardCharsets.UTF_8);
        String node = CodeQualityGuardSupport.findNodeExecutable();
        String parseScript = "const fs=require('fs');const vm=require('vm');const p=process.argv[1];"
                + "const code=fs.readFileSync(p,'utf8');const start=process.hrtime.bigint();"
                + "new vm.Script(code);const ms=Number(process.hrtime.bigint()-start)/1e6;"
                + "console.log(ms.toFixed(3));";
        try {
            CodeQualityGuardSupport.CommandResult parse = CodeQualityGuardSupport.runCommand(Duration.ofSeconds(30), node, "-e", parseScript, output.toString());
            if (parse.exitCode == 0) {
                report.metric("performance.baseline.vm_script_parse_ms", parse.output);
            } else {
                report.warning("Node vm.Script parse timing failed; syntax is covered by node --check. Output: " + parse.output);
            }
        } catch (AssertionError error) {
                report.warning("Node vm.Script parse timing timed out; syntax is covered by node --check. " + error.getMessage());
        }
        runLogicChainSyntheticBaseline(report, node, output);

        String performanceDoc = CodeQualityGuardSupport.read("docs/PERFORMANCE_HOTSPOTS_9_1_1.md");
        String currentContext = CodeQualityGuardSupport.read("docs/CODEBASE_HEALTH_GUARD_BASELINE_9_1_1_CURRENT_CONTEXT.md");
        String guardPlan = CodeQualityGuardSupport.read("docs/CODE_QUALITY_GUARD_PLAN_9_1_1.md");
        report.requireContains(performanceDoc, "DOM equivalence baseline", "Performance doc DOM equivalence baseline");
        report.requireContains(performanceDoc, "Phase 6 implemented", "Performance doc Phase 6 implementation note");
        report.requireContains(currentContext, "Phase 6 Logic Chain Performance Baseline Context", "Phase 6 current context section");
        report.requireContains(currentContext, "node --check", "Phase 1 current context node syntax guard");
        report.requireContains(guardPlan, "phase6-logic-chain-performance-baseline", "Phase 6 guard scope marker");
    }

    private static void runLogicChainSyntheticBaseline(CodeQualityGuardSupport.GuardReport report, String node, Path appJs) throws Exception {
        Path smoke = CodeQualityGuardSupport.projectRoot().resolve("build/tmp/webadmin-phase6-logic-chain-perf-smoke.js");
        Files.writeString(smoke, phase6SmokeHarness(), StandardCharsets.UTF_8);
        CodeQualityGuardSupport.CommandResult result = CodeQualityGuardSupport.runCommand(Duration.ofSeconds(45),
                node, smoke.toString(), appJs.toString());
        report.metric("performance.phase6.synthetic.exit", result.exitCode);
        if (result.exitCode != 0) {
            report.fail("Phase 6 Logic Chain synthetic render baseline failed: " + result.output);
            return;
        }
        Map<String, String> metrics = parseMetrics(result.output);
        for (Map.Entry<String, String> entry : metrics.entrySet()) {
            report.metric("performance.phase6." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Map<String, String>> scenario : PHASE6_DOM_BASELINES.entrySet()) {
            for (Map.Entry<String, String> expected : scenario.getValue().entrySet()) {
                requireMetric(report, metrics, "scenario." + scenario.getKey() + "." + expected.getKey(), expected.getValue());
            }
        }
        warnTiming(report, metrics, "initial", 50.0d);
        warnTiming(report, metrics, "selected", 16.0d);
        warnTiming(report, metrics, "hover", 16.0d);
        warnTiming(report, metrics, "edit", 50.0d);
        warnTiming(report, metrics, "draft", 50.0d);
        warnTiming(report, metrics, "unsaved", 50.0d);
        warnTiming(report, metrics, "vbd", 50.0d);
    }

    private static void requireMetric(CodeQualityGuardSupport.GuardReport report, Map<String, String> metrics,
                                      String key, String expected) {
        String actual = metrics.get(key);
        report.require(expected.equals(actual), "Phase 6 DOM equivalence metric changed for " + key
                + ": expected=" + expected + " actual=" + actual);
    }

    private static void warnTiming(CodeQualityGuardSupport.GuardReport report, Map<String, String> metrics,
                                   String scenario, double softLimitMs) {
        String raw = metrics.get("timing." + scenario + ".ms");
        if (raw == null || raw.isBlank()) {
            report.fail("Phase 6 synthetic " + scenario + " timing metric missing");
            return;
        }
        try {
            double value = Double.parseDouble(raw);
            if (value > softLimitMs) {
                report.warning("Phase 6 synthetic " + scenario + " render exceeded soft timing target: "
                        + value + "ms > " + softLimitMs + "ms");
            }
        } catch (NumberFormatException ignored) {
            report.warning("Phase 6 synthetic " + scenario + " timing was not numeric: " + raw);
        }
    }

    private static Map<String, String> parseMetrics(String output) {
        Map<String, String> metrics = new LinkedHashMap<>();
        for (String line : output.split("\\R")) {
            int index = line.indexOf('=');
            if (index <= 0) {
                continue;
            }
            metrics.put(line.substring(0, index).trim(), line.substring(index + 1).trim());
        }
        return metrics;
    }

    private static Map<String, Map<String, String>> phase6DomBaselines() {
        Map<String, Map<String, String>> baselines = new LinkedHashMap<>();
        putScenario(baselines, "initial", "5", "4", "3", "3", "0", "0", "6", "0", "false",
                "17f9c3d0091d0b6d");
        putScenario(baselines, "selected", "5", "4", "3", "3", "5", "4", "6", "0", "false",
                "f0379248a5b961d2");
        putScenario(baselines, "hover", "5", "4", "3", "3", "7", "2", "6", "0", "false",
                "0a17ab3b1d5376c6");
        putScenario(baselines, "edit", "5", "4", "3", "3", "0", "0", "6", "0", "false",
                "c1fd5bcc6e014cab");
        putScenario(baselines, "draft", "7", "5", "4", "4", "0", "0", "6", "8", "false",
                "839ed60a0b4b0250");
        putScenario(baselines, "unsaved", "7", "5", "4", "4", "0", "0", "6", "9", "false",
                "c1495383aac0995c");
        putScenario(baselines, "vbd", "6", "4", "4", "4", "3", "7", "6", "9", "true",
                "e2e15cb55f58bf43");
        putExtraScenario(baselines, "vbd",
                "vbdStableIdentity", "1",
                "vbdNoDuplicate", "1",
                "vbdTargetChannelOnly", "1",
                "vbdSourceCard", "1",
                "vbdSourceNodeIds", "vbd:one",
                "vbdTriggerKeys", "right_click",
                "vbdSelectedTrigger", "right_click",
                "vbdDraftSourceNodeId", "vbd:one");
        putExtraScenario(baselines, "pendingDelete",
                "pendingDeleteCard", "9",
                "pendingDeleteBadge", "4",
                "pendingDeleteDiff", "5",
                "savePayloadPendingDeleteLeak", "false");
        putExtraScenario(baselines, "vbdFallback",
                "vbdSourceCard", "1",
                "vbdSourceNodeIds", "vbd:one",
                "vbdTriggerKeys", "right_click",
                "vbdSelectedTrigger", "right_click",
                "vbdDraftSourceNodeId", "");
        putExtraScenario(baselines, "vbdSourcePriority",
                "vbdSourceCard", "1",
                "vbdSourceNodeIds", "vbd:one",
                "vbdTriggerKeys", "right_click",
                "vbdSelectedTrigger", "right_click",
                "vbdDraftSourceNodeId", "vbd:one");
        putExtraScenario(baselines, "minimapCap", "minimapSegments", "24");
        return baselines;
    }

    private static void putScenario(Map<String, Map<String, String>> baselines, String name,
                                    String nodeCount, String edgeCount, String markerEnd, String arrowOwner,
                                    String related, String dimmed, String minimapSegments, String diffCount,
                                    String vbdOverlay, String domHash) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("nodeCount", nodeCount);
        values.put("edgeCount", edgeCount);
        values.put("markerEnd", markerEnd);
        values.put("arrowOwner", arrowOwner);
        values.put("related", related);
        values.put("dimmed", dimmed);
        values.put("minimapSegments", minimapSegments);
        values.put("diffCount", diffCount);
        values.put("vbdOverlay", vbdOverlay);
        values.put("hash.dom", domHash);
        baselines.put(name, values);
    }

    private static void putExtraScenario(Map<String, Map<String, String>> baselines, String name, String... pairs) {
        Map<String, String> values = baselines.computeIfAbsent(name, ignored -> new LinkedHashMap<>());
        for (int index = 0; index + 1 < pairs.length; index += 2) {
            values.put(pairs[index], pairs[index + 1]);
        }
    }

    private static String phase6SmokeHarness() {
        return """
                const fs=require('fs');
                const vm=require('vm');
                const crypto=require('crypto');
                const code=fs.readFileSync(process.argv[2],'utf8');
                function makeEl(){return {innerHTML:'',dataset:{},className:'',style:{},children:[],scrollTop:0,scrollLeft:0,addEventListener(){},removeEventListener(){},querySelector(){return null;},querySelectorAll(){return [];},closest(){return null;},getAttribute(){return null;},setAttribute(){},focus(){},classList:{add(){},remove(){},contains(){return false;},toggle(){}}};}
                const view=makeEl();
                const document={body:makeEl(),documentElement:makeEl(),addEventListener(){},removeEventListener(){},querySelector(){return null;},querySelectorAll(){return [];},getElementById(id){return id==='app-view'?view:null;},createElement(){return makeEl();}};
                const context={console,document,window:null,globalThis:null,navigator:{onLine:true},location:{hash:'#/logic-chains'},localStorage:{getItem(){return null;},setItem(){},removeItem(){}},addEventListener(){},removeEventListener(){},setTimeout,clearTimeout,setInterval,clearInterval,requestAnimationFrame:(cb)=>{cb();return 1;},cancelAnimationFrame(){},performance:{now:()=>Date.now()},URL,URLSearchParams,TextEncoder,TextDecoder,fetch:async()=>({ok:true,json:async()=>({})})};
                context.window=context;
                context.globalThis=context;
                vm.createContext(context);
                vm.runInContext(code+`
                ;globalThis.__phase6Run=function(graph,scenario){
                  appState.me={username:'Owner',role:'OWNER'};
                  appState.currentLogicChainGraph=graph;
                  appState.logicChainCanvas={zoom:1,panX:0,panY:0,selectedNodeId:'',focusNodeId:'',hoverNodeId:'',detailOpen:true,graphKey:'CHANNEL:root',collapsedChannels:{},viewMode:'BOTH',nodeTypeFilter:'ALL',routeInfo:{fallback:'#/logic-chains'}};
                  appState.logicChainEditor=null;
                  if(scenario==='selected'){appState.logicChainCanvas.selectedNodeId='listener:one';appState.logicChainCanvas.focusNodeId='listener:one';appState.logicChainCanvas.selectionPinned=true;}
                  if(scenario==='hover'){appState.logicChainCanvas.hoverNodeId='channel:root';}
                  if(scenario==='edit'){appState.logicChainEditor={active:true,routeHash:'#/logic-chains',rootType:'CHANNEL',rootRef:'root',includeDisabled:true,maxDepth:3,lockId:'lock',lockLost:false,baseGraphFingerprint:'base-fp',nodes:[],edges:[],draftChannels:[],existingNodeEdits:[],actionEdits:[],nodeDeletes:[],actionDeletes:[],actionReorders:[],dirty:false,errors:[],connectionMode:'',saving:false};}
                  if(scenario==='draft'||scenario==='unsaved'){
                    appState.logicChainEditor={active:true,routeHash:'#/logic-chains',rootType:'CHANNEL',rootRef:'root',includeDisabled:true,maxDepth:3,lockId:'lock',lockLost:false,baseGraphFingerprint:'base-fp',nodes:[{id:'draft:join',type:'signal_join',displayName:'Draft Join',label:'Draft Join',column:'C2',slot:1,placed:true,enabled:true,mode:'ALL',threshold:2,scopeMode:'GLOBAL',resetPolicy:'RESET_AFTER_EMIT',timeoutTicks:0,cooldownTicks:0}],edges:[{from:'channel:root',to:'draft:join',type:'join_input',label:'draft input',metadata:{draft:true,newEdge:true}}],draftChannels:[{channel:'draft.out',displayName:'Draft Out',cardDraft:true,metadataDraft:true}],existingNodeEdits:[],actionEdits:[],nodeDeletes:[],actionDeletes:[],actionReorders:[],dirty:true,errors:[],connectionMode:'',saving:false,diffExpanded:scenario==='unsaved'};
                  }
                  if(scenario==='pendingDelete'){
                    appState.logicChainCanvas.selectedNodeId='draft:action:draft:listener:listener:default:0';appState.logicChainCanvas.focusNodeId='draft:action:draft:listener:listener:default:0';appState.logicChainCanvas.selectionPinned=true;
                    appState.logicChainEditor={active:true,routeHash:'#/logic-chains',rootType:'CHANNEL',rootRef:'root',includeDisabled:true,maxDepth:3,lockId:'lock',lockLost:false,baseGraphFingerprint:'base-fp',nodes:[{id:'draft:listener',type:'signal_listener',displayName:'Draft Listener',label:'Draft Listener',column:'C2',slot:1,placed:true,enabled:true,signalListener:{actions:[{type:'message',value:'hello',enabled:true,_pendingDelete:true}]}},{id:'draft:timer',type:'timer',displayName:'Draft Timer',label:'Draft Timer',column:'C2',slot:2,placed:true,enabled:true,timer:{onStartActions:[{type:'message',value:'start',enabled:true,_pendingDelete:true}],onTickActions:[{type:'message',value:'tick',enabled:true,_pendingDelete:true}],onCompleteActions:[{type:'message',value:'done',enabled:true,_pendingDelete:true}],onCancelActions:[{type:'message',value:'cancel',enabled:true,_pendingDelete:true}]}}],edges:[{from:'channel:root',to:'draft:listener',type:'consumes',label:'draft consumes',metadata:{draft:true,newEdge:true}},{from:'draft:timer',to:'channel:root',type:'timer_outputs_channel',label:'timer output',metadata:{draft:true,newEdge:true}}],draftChannels:[],existingNodeEdits:[],actionEdits:[],nodeDeletes:[],actionDeletes:[],actionReorders:[],dirty:true,errors:[],connectionMode:'',saving:false,diffExpanded:true};
                  }
                  if(scenario==='vbd'||scenario==='vbdFallback'||scenario==='vbdSourcePriority'){
                    const native={deviceId:'vbd-one',originalJson:'{}',values:{interactionEnabled:true,interactChannel:'draft.out',containerChangeEnabled:true,containerChangeChannel:'container.out'}};
                    const d={kind:'virtual_block_device',targetId:'vbd-one',deviceId:'vbd-one',displayName:'VBD One',confirmed:true,original:{},virtualBlockDevice:{selectedTriggerType:'right_click',nativeTriggerDraft:native,itemSubmitRequirements:[{displayName:'Item One',count:1,consumeCount:1}]}};
                    if(scenario!=='vbdFallback')d.sourceNodeId='vbd:one';
                    appState.logicChainCanvas.selectedNodeId=scenario==='vbdSourcePriority'?'vbd:two':'vbd:one';appState.logicChainCanvas.selectionPinned=true;
                    appState.logicChainEditor={active:true,routeHash:'#/logic-chains',rootType:'CHANNEL',rootRef:'root',includeDisabled:true,maxDepth:3,lockId:'lock',lockLost:false,baseGraphFingerprint:'base-fp',nodes:[],edges:[],draftChannels:[{channel:'draft.out',displayName:'Draft Out',metadataDraft:true}],existingNodeEdits:[d],existingEdit:d,actionEdits:[],nodeDeletes:[],actionDeletes:[],actionReorders:[],dirty:true,errors:[],connectionMode:'',saving:false,diffExpanded:true};
                  }
                  renderLogicChainViewer(graph,{fallback:'#/logic-chains'},{silent:true});
                  const html=document.getElementById('app-view').innerHTML;
                  let payloadText='';
                  try{payloadText=appState.logicChainEditor?.active?JSON.stringify(logicChainEditorSaveBody()):'';}catch(_err){payloadText='PAYLOAD_ERROR';}
                  const vbdDraft=(appState.logicChainEditor?.existingNodeEdits||[]).find(d=>String(d?.kind||'').toLowerCase()==='virtual_block_device')||null;
                  let vbdTriggerKeys='',vbdSelectedTrigger='',vbdDraftSourceNodeId='';
                  if(vbdDraft){const draft=logicChainVbdStoredNativeTriggerDraft(vbdDraft);vbdSelectedTrigger=String(vbdDraft?.virtualBlockDevice?.selectedTriggerType||logicChainExistingVbdSelectedTrigger(vbdDraft)||'');vbdDraftSourceNodeId=String(vbdDraft.sourceNodeId||'');try{vbdTriggerKeys=logicChainVbdOutputRowsForSelectedTrigger(vbdDraft,draft).map(row=>String(row.triggerKey||row.type||'')).join(',');}catch(_ignored){}}
                  return {html,savePayloadPendingDeleteLeak:payloadText.includes('_pendingDelete')?'true':'false',vbdTriggerKeys,vbdSelectedTrigger,vbdDraftSourceNodeId};
                };`,context,{filename:'webadmin-app.js'});
                const graph={id:'synthetic',componentId:'synthetic',displayName:'Synthetic Chain',root:{id:'producer:root',type:'producer',label:'Root',channel:'root'},metadata:{rootType:'CHANNEL',rootRef:'root'},stats:{},segments:[{channel:'root',downstreamChannels:['draft.out','container.out']},{channel:'draft.out',downstreamChannels:[]},{channel:'container.out',downstreamChannels:[]},{channel:'extra.1',downstreamChannels:[]},{channel:'extra.2',downstreamChannels:[]},{channel:'extra.3',downstreamChannels:[]}],nodes:[{id:'producer:root',type:'producer',label:'Root',channel:'root',enabled:true,metadata:{nodeKind:'primary'}},{id:'channel:root',type:'channel',label:'Root Channel',channel:'root',enabled:true,metadata:{nodeKind:'primary'}},{id:'listener:one',type:'consumer',refType:'signal_listener',label:'Listener',channel:'root',enabled:true,metadata:{}},{id:'action:one',type:'action',label:'Action',enabled:true,metadata:{}},{id:'vbd:one',type:'producer',refType:'virtual_block_device',refId:'vbd-one',label:'VBD One',channel:'root',enabled:true,metadata:{deviceId:'vbd-one',kind:'virtual_block_device',nodeKind:'primary'}}],edges:[{from:'producer:root',to:'channel:root',type:'emits',label:'emits'},{from:'channel:root',to:'listener:one',type:'consumes',label:'consumes'},{from:'listener:one',to:'action:one',type:'executes',label:'executes'},{from:'vbd:one',to:'channel:root',type:'vbd_outputs_channel',label:'vbd output',pathGroupId:'draft'}]};
                function graphForScenario(scenario){const copy=JSON.parse(JSON.stringify(graph));if(scenario==='vbdSourcePriority'){copy.nodes.push({id:'vbd:two',type:'producer',refType:'virtual_block_device',refId:'vbd-one',label:'VBD Two',channel:'root',enabled:true,metadata:{deviceId:'vbd-one',kind:'virtual_block_device',nodeKind:'primary'}});copy.edges.push({from:'vbd:two',to:'channel:root',type:'vbd_outputs_channel',label:'vbd selected output',pathGroupId:'draft'});}if(scenario==='minimapCap')copy.segments=Array.from({length:30},(_,i)=>({channel:`cap.${i}`,downstreamChannels:Array.from({length:i%4},(__,j)=>`cap.${i}.${j}`)}));return copy;}
                function sha(s){return crypto.createHash('sha256').update(s).digest('hex').slice(0,16);}
                function count(s,re){return (s.match(re)||[]).length;}
                function sig(html){
                  function attr(tag,name){const m=tag.match(new RegExp(name+'="([^"]*)"'));return m?m[1]:'';}
                  const pathTags=[...html.matchAll(/<path class="logic-chain-edge[^"]*"[^>]*>/g)].map(m=>m[0]);
                  const paths=pathTags.map(tag=>attr(tag,'d')+'|marker='+(attr(tag,'marker-end')||'0')+'|owner='+(tag.includes('data-logic-chain-target-arrow-owner="true"')?'1':'0'));
                  const edgeAttrs=pathTags.map(tag=>`${attr(tag,'class')}|group=${attr(tag,'data-logic-chain-edge-path-group')}|visual=${attr(tag,'data-logic-chain-edge-visual-style')}|shape=${attr(tag,'data-logic-chain-route-shape')}|marker=${attr(tag,'marker-end')||''}`);
                  const nodes=[...html.matchAll(/<div class="logic-chain-tree-node[^"]*"[^>]*data-logic-chain-node-id="([^"]*)"[^>]*style="left:([^;]+);top:([^;]+);width:([^;]+);height:([^;"]+)/g)].map(m=>`${m[1]}@${m[2]},${m[3]},${m[4]},${m[5]}`);
                  const cardTags=[...html.matchAll(/<div role="button"[^>]*class="logic-chain-node-card[^"]*"[^>]*>/g)].map(m=>m[0]);
                  const cards=cardTags.map(tag=>`${attr(tag,'data-logic-chain-node-id')}:${attr(tag,'class').replace(/\\s+/g,' ').trim()}`);
                  const cardAttrs=cardTags.map(tag=>`${attr(tag,'data-logic-chain-node-id')}|type=${attr(tag,'data-node-type')}|action=${attr(tag,'data-logic-chain-node-action')}|primary=${attr(tag,'data-logic-chain-primary-node-id')}|pending=${tag.includes('data-logic-chain-pending-delete-card="true"')?'1':'0'}|vbdSource=${tag.includes('data-logic-chain-vbd-trigger-source-card-draft="true"')?'1':'0'}`);
                  const vbdSourceNodeIds=cardTags.filter(tag=>tag.includes('data-logic-chain-vbd-trigger-source-card-draft="true"')).map(tag=>attr(tag,'data-logic-chain-node-id')).join(',');
                  const mini=(html.match(/<div class="logic-chain-minimap"[^>]*>([\\s\\S]*?)<\\/div><\\/div>/)||['',''])[1];
                  const panel=(html.match(/<aside class="logic-chain-right"[^>]*>([\\s\\S]*?)<\\/aside>/)||['',''])[1];
                  const diff=(html.match(/<section class="logic-chain-draft-diff-banner[\\s\\S]*?<\\/section>/)||[''])[0];
                  return {nodeCount:nodes.length,edgeCount:paths.length,markerEnd:count(html,/marker-end=/g),arrowOwner:count(html,/data-logic-chain-target-arrow-owner="true"/g),related:count(html,/ related/g),dimmed:count(html,/ dimmed/g),minimapSegments:count(mini,/logic-chain-minimap-segment/g),diffCount:count(html,/data-logic-chain-draft-diff/g),vbdOverlay:html.includes('data-logic-chain-vbd-trigger-graph-render-before-save="true"'),pendingDeleteCard:count(html,/data-logic-chain-pending-delete-card="true"/g),pendingDeleteBadge:count(html,/data-logic-chain-pending-delete-badge="true"/g),pendingDeleteDiff:count(html,/data-logic-chain-draft-action-pending-delete-diff="true"/g),vbdStableIdentity:count(html,/data-logic-chain-vbd-trigger-stable-identity="true"/g),vbdNoDuplicate:count(html,/data-logic-chain-vbd-trigger-no-duplicate-card="true"/g),vbdTargetChannelOnly:count(html,/data-logic-chain-vbd-trigger-target-channel-only="true"/g),vbdSourceCard:count(html,/data-logic-chain-vbd-trigger-source-card-draft="true"/g),vbdSourceNodeIds,hash:{dom:sha([paths.join('\\n'),edgeAttrs.join('\\n'),nodes.join('\\n'),cards.join('\\n'),cardAttrs.join('\\n'),mini,panel,diff].join('\\n---\\n'))}};
                }
                for(const scenario of ['initial','selected','hover','edit','draft','unsaved','vbd','pendingDelete','vbdFallback','vbdSourcePriority','minimapCap']){
                  const start=process.hrtime.bigint();
                  const result=context.__phase6Run(graphForScenario(scenario),scenario);
                  const html=result.html||String(result||'');
                  const ms=Number(process.hrtime.bigint()-start)/1e6;
                  const s=sig(html);
                  const originalScenario=new Set(['initial','selected','hover','edit','draft','unsaved','vbd']).has(scenario);
                  const baseKeys=new Set(['nodeCount','edgeCount','markerEnd','arrowOwner','related','dimmed','minimapSegments','diffCount','vbdOverlay']);
                  const extraKeys={pendingDelete:['pendingDeleteCard','pendingDeleteBadge','pendingDeleteDiff'],vbd:['vbdStableIdentity','vbdNoDuplicate','vbdTargetChannelOnly','vbdSourceCard','vbdSourceNodeIds'],vbdFallback:['vbdSourceCard','vbdSourceNodeIds'],vbdSourcePriority:['vbdSourceCard','vbdSourceNodeIds'],minimapCap:['minimapSegments']};
                  const hashKeys=new Set(['dom']);
                  for(const [key,value] of Object.entries(s)){
                    if(key==='hash'){if(originalScenario)for(const [hashKey,hashValue] of Object.entries(value)){if(hashKeys.has(hashKey))console.log(`scenario.${scenario}.hash.${hashKey}=${hashValue}`);}}
                    else if((originalScenario&&baseKeys.has(key))||(extraKeys[scenario]||[]).includes(key))console.log(`scenario.${scenario}.${key}=${value}`);
                  }
                  if(scenario==='pendingDelete')console.log(`scenario.${scenario}.savePayloadPendingDeleteLeak=${result.savePayloadPendingDeleteLeak||'false'}`);
                  if(scenario==='vbd'||scenario==='vbdFallback'||scenario==='vbdSourcePriority'){
                    console.log(`scenario.${scenario}.vbdTriggerKeys=${result.vbdTriggerKeys||''}`);
                    console.log(`scenario.${scenario}.vbdSelectedTrigger=${result.vbdSelectedTrigger||''}`);
                    console.log(`scenario.${scenario}.vbdDraftSourceNodeId=${result.vbdDraftSourceNodeId||''}`);
                  }
                  console.log(`timing.${scenario}.ms=${ms.toFixed(3)}`);
                }
                """;
    }

    private static String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b & 0xff));
        }
        return hex.toString();
    }
}
