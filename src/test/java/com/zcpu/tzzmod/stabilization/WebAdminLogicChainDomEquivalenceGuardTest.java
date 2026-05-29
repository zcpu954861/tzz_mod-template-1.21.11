package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.webadmin.WebAdminFrontendAssets;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

final class WebAdminLogicChainDomEquivalenceGuardTest {
    private static final Map<String, String> BASELINES = phase3Baselines();

    private WebAdminLogicChainDomEquivalenceGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        CodeQualityGuardSupport.GuardReport report =
                new CodeQualityGuardSupport.GuardReport("9.1.2 Logic Chain DOM equivalence guard");
        run(report);
        report.printAndFail();
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws Exception {
        String appJs = WebAdminFrontendAssets.appJs();
        String appCss = WebAdminFrontendAssets.appCss();
        checkSourceMarkers(report, appJs, appCss);

        Path appJsPath = CodeQualityGuardSupport.projectRoot().resolve("build/tmp/webadmin-app.js");
        Path harness = CodeQualityGuardSupport.projectRoot()
                .resolve("build/tmp/webadmin-phase912-logic-chain-dom-equivalence.js");
        Path metricsPath = CodeQualityGuardSupport.projectRoot()
                .resolve("build/tmp/webadmin-phase912-logic-chain-dom-equivalence.metrics");
        Files.createDirectories(harness.getParent());
        Files.writeString(appJsPath, appJs, StandardCharsets.UTF_8);
        Files.writeString(harness, harnessJs(), StandardCharsets.UTF_8);

        String node = CodeQualityGuardSupport.findNodeExecutable();
        CodeQualityGuardSupport.CommandResult result = CodeQualityGuardSupport.runCommand(
                Duration.ofSeconds(90),
                node,
                harness.toString(),
                appJsPath.toString(),
                metricsPath.toString()
        );
        report.metric("phase912.logic_chain_dom_equivalence.exit", result.exitCode);
        if (result.exitCode != 0) {
            report.fail("Logic Chain DOM equivalence harness failed: " + result.output);
            return;
        }

        String metricsOutput = Files.exists(metricsPath)
                ? Files.readString(metricsPath, StandardCharsets.UTF_8)
                : "";
        Map<String, String> metrics = parseMetrics(metricsOutput);
        for (Map.Entry<String, String> entry : metrics.entrySet()) {
            report.metric("phase912.logic_chain_dom_equivalence." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> expected : BASELINES.entrySet()) {
            String actual = metrics.get(expected.getKey());
            report.require(expected.getValue().equals(actual),
                    "Logic Chain DOM equivalence baseline changed for " + expected.getKey()
                            + ": actual=" + actual + " expected=" + expected.getValue());
        }
        for (String key : new String[]{
                "equivalence.hover",
                "equivalence.selection",
                "equivalence.zoom",
                "scenario.vbd.vbdSourceCard",
                "scenario.vbd.vbdTriggerKeys",
                "scenario.minimap.minimapSegments"
        }) {
            report.require(metrics.containsKey(key), "Logic Chain DOM equivalence metric missing " + key);
        }
        report.require("true".equals(metrics.get("equivalence.hover")),
                "Logic Chain hover path must remain equivalent to canonical full render");
        report.require("true".equals(metrics.get("equivalence.selection")),
                "Logic Chain selection path must remain equivalent to canonical full render");
        report.require("true".equals(metrics.get("equivalence.zoom")),
                "Logic Chain zoom path must remain equivalent to canonical full render");
        report.require("1".equals(metrics.get("scenario.vbd.vbdSourceCard")),
                "VBD overlay must keep one source-card marker");
        report.require("right_click".equals(metrics.get("scenario.vbd.vbdTriggerKeys")),
                "VBD overlay must keep selected triggerKey snapshot");
        report.require("24".equals(metrics.get("scenario.minimap.minimapSegments")),
                "Logic Chain minimap must keep 24-segment cap");
    }

    private static void checkSourceMarkers(
            CodeQualityGuardSupport.GuardReport report,
            String appJs,
            String appCss
    ) throws Exception {
        String canvas = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminLogicChainCanvasScripts.java");
        String nodePanel = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminLogicChainNodePanelScripts.java");
        String viewer = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminLogicChainViewerScripts.java");
        report.requireContains(canvas, "data-logic-chain-edge-from", "Phase 3 edge from snapshot attr");
        report.requireContains(canvas, "data-logic-chain-edge-to", "Phase 3 edge to snapshot attr");
        report.requireContains(canvas, "data-logic-chain-edge-type", "Phase 3 edge type snapshot attr");
        report.requireContains(canvas, "traversalForward", "Phase 3 traversal index reuse marker");
        report.requireContains(nodePanel, "logicChainSelectedNodePanel(graph,nodes,edgeIndexes=null)",
                "Phase 3 selected panel edge-index signature");
        report.requireContains(viewer, "detailEdgeIndexes=logicChainEdgeIndexes(detailGraph)",
                "Phase 3 selected panel render-local edge index");
        report.requireContains(canvas, "function focusLogicChainNodeDetail(id){const next=String(id||'');"
                        + "appState.logicChainCanvas.selectedNodeId=next;"
                        + "appState.logicChainCanvas.focusNodeId=next;"
                        + "appState.logicChainCanvas.selectionPinned=!!next;"
                        + "appState.logicChainCanvas.detailOpen=true;"
                        + "if(appState.currentLogicChainGraph)renderLogicChainViewer(",
                "Phase 3 still defers selection local DOM update");
        report.requireContains(canvas, "function highlightRelatedEdges(id){const next=id||'';"
                        + "if(appState.logicChainCanvas.hoverNodeId===next)return;"
                        + "appState.logicChainCanvas.hoverNodeId=next;"
                        + "if(appState.currentLogicChainGraph)renderLogicChainViewer(",
                "Phase 3 still defers hover local DOM update");
        report.requireContains(canvas, "function setLogicChainZoom(delta){appState.logicChainCanvas.zoom=Math.max(.45,Math.min(1.8,Number(appState.logicChainCanvas.zoom||1)+Number(delta||0)));"
                        + "if(appState.currentLogicChainGraph)renderLogicChainViewer(",
                "Phase 3 still defers zoom local DOM update");
        report.require(!appCss.contains("data-logic-chain-edge-from")
                        && !appCss.contains("data-logic-chain-edge-to")
                        && !appCss.contains("data-logic-chain-edge-type"),
                "Phase 3 edge identity attrs must remain nonvisual");
        report.require(CodeQualityGuardSupport.count(appJs, "data-logic-chain-edge-from") == 1,
                "Phase 3 edge identity attrs must not become event-delegation behavior");
    }

    private static Map<String, String> parseMetrics(String output) {
        Map<String, String> metrics = new LinkedHashMap<>();
        for (String line : output.split("\\R")) {
            int equals = line.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            metrics.put(line.substring(0, equals), line.substring(equals + 1));
        }
        return metrics;
    }

    private static Map<String, String> phase3Baselines() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("scenario.initial.nodeHash", "6dbd91f761d3d5a8");
        values.put("scenario.initial.edgeHash", "2a065e3f4c1291ce");
        values.put("scenario.initial.classHash", "fd9b7879a0e9b64a");
        values.put("scenario.initial.panelHash", "c3c5b02f7d3f3154");
        values.put("scenario.initial.minimapHash", "791b46ef7c21fe4a");
        values.put("scenario.initial.domHash", "fe282969482e245f");
        values.put("scenario.hover.edgeHash", "480946fc7456da42");
        values.put("scenario.hover.classHash", "adf970e9e7262305");
        values.put("scenario.hover.related", "7");
        values.put("scenario.hover.dimmed", "3");
        values.put("scenario.hover.domHash", "26552ce8f86edc54");
        values.put("scenario.selection.classHash", "4cdaeffa84566ac2");
        values.put("scenario.selection.panelHash", "e581a9db584d07e6");
        values.put("scenario.selection.related", "5");
        values.put("scenario.selection.dimmed", "5");
        values.put("scenario.selection.domHash", "03b4ab670d3917e3");
        values.put("scenario.zoom.transformHash", "a52c8138b4cf0df5");
        values.put("scenario.zoom.domHash", "6294bf507dee0fe7");
        values.put("scenario.draft.nodeHash", "8620955a585710f1");
        values.put("scenario.draft.edgeHash", "13f57dc432a91ec1");
        values.put("scenario.draft.diffHash", "c84c24e85163c222");
        values.put("scenario.draft.panelHash", "0e332cab4c677c49");
        values.put("scenario.draft.domHash", "bcdfc2f966d169d9");
        values.put("scenario.unsaved.diffHash", "c4551a28a45d4b40");
        values.put("scenario.unsaved.payloadPendingDeleteLeak", "false");
        values.put("scenario.unsaved.domHash", "8fd8f0e6473e6a06");
        values.put("scenario.vbd.edgeHash", "67af61cd6b60af9b");
        values.put("scenario.vbd.classHash", "f873ac11e4279927");
        values.put("scenario.vbd.vbdHash", "cb564f7b2aa80113");
        values.put("scenario.vbd.vbdSourceCard", "1");
        values.put("scenario.vbd.vbdSourceNodeIds", "vbd:one");
        values.put("scenario.vbd.vbdTriggerKeys", "right_click");
        values.put("scenario.vbd.vbdDraftSourceNodeId", "vbd:one");
        values.put("scenario.vbd.domHash", "aff15b4f8d783b2a");
        values.put("scenario.minimap.minimapSegments", "24");
        values.put("scenario.minimap.minimapHash", "204794e1690ec684");
        values.put("scenario.minimap.domHash", "256efa86a30786a6");
        return values;
    }

    private static String harnessJs() {
        return """
                const fs=require('fs');
                const vm=require('vm');
                const crypto=require('crypto');
                const code=fs.readFileSync(process.argv[2],'utf8');
                const outputPath=process.argv[3];
                const rows=[];
                function emit(line){rows.push(line);}
                function makeEl(){return {innerHTML:'',dataset:{},className:'',style:{},children:[],scrollTop:0,scrollLeft:0,addEventListener(){},removeEventListener(){},querySelector(){return null;},querySelectorAll(){return [];},closest(){return null;},getAttribute(){return null;},setAttribute(){},focus(){},classList:{add(){},remove(){},contains(){return false;},toggle(){}}};}
                const view=makeEl();
                const document={body:makeEl(),documentElement:makeEl(),activeElement:null,addEventListener(){},removeEventListener(){},querySelector(){return null;},querySelectorAll(){return [];},getElementById(id){return id==='app-view'?view:null;},createElement(){return makeEl();}};
                const context={console,document,window:null,globalThis:null,navigator:{onLine:true},location:{hash:'#/logic-chains'},localStorage:{getItem(){return null;},setItem(){},removeItem(){}},addEventListener(){},removeEventListener(){},setTimeout,clearTimeout,setInterval,clearInterval,requestAnimationFrame:(cb)=>{cb();return 1;},cancelAnimationFrame(){},performance:{now:()=>Date.now()},URL,URLSearchParams,TextEncoder,TextDecoder,fetch:async()=>({ok:true,json:async()=>({})})};
                context.window=context;
                context.globalThis=context;
                vm.createContext(context);
                vm.runInContext(code+`
                ;globalThis.__phase912DomRun=function(scenario,mode){
                  const graph=phase912DomGraph(scenario);
                  appState.me={username:'Owner',role:'OWNER'};
                  appState.currentLogicChainGraph=graph;
                  appState.logicChainCanvas={zoom:1,panX:0,panY:0,selectedNodeId:'',focusNodeId:'',hoverNodeId:'',detailOpen:true,graphKey:'',collapsedChannels:{},viewMode:'BOTH',nodeTypeFilter:'ALL',routeInfo:{fallback:'#/logic-chains'}};
                  appState.logicChainEditor=null;
                  renderLogicChainViewer(graph,{fallback:'#/logic-chains'},{silent:true});
                  if(scenario==='hover'){
                    if(mode==='interaction')highlightRelatedEdges('channel:root');
                    else{appState.logicChainCanvas.hoverNodeId='channel:root';renderLogicChainViewer(graph,{fallback:'#/logic-chains'},{silent:true});}
                  }
                  if(scenario==='selection'){
                    if(mode==='interaction')focusLogicChainNodeDetail('listener:one');
                    else{appState.logicChainCanvas.selectedNodeId='listener:one';appState.logicChainCanvas.focusNodeId='listener:one';appState.logicChainCanvas.selectionPinned=true;appState.logicChainCanvas.detailOpen=true;renderLogicChainViewer(graph,{fallback:'#/logic-chains'},{silent:true});}
                  }
                  if(scenario==='zoom'){
                    if(mode==='interaction')setLogicChainZoom(0.2);
                    else{appState.logicChainCanvas.zoom=1.2;renderLogicChainViewer(graph,{fallback:'#/logic-chains'},{silent:true});}
                  }
                  if(scenario==='draft'||scenario==='unsaved'){
                    appState.logicChainEditor={active:true,routeHash:'#/logic-chains',rootType:'CHANNEL',rootRef:'root',includeDisabled:true,maxDepth:3,lockId:'lock',lockLost:false,baseGraphFingerprint:'base-fp',nodes:[{id:'draft:listener',type:'signal_listener',displayName:'Draft Listener',label:'Draft Listener',column:'C2',slot:1,placed:true,enabled:true,signalListener:{actions:[{type:'message',value:'hello',enabled:true,_pendingDelete:scenario==='unsaved'}]}}],edges:[{from:'channel:root',to:'draft:listener',type:'consumes',label:'draft consumes',metadata:{draft:true,newEdge:true}}],draftChannels:[{channel:'draft.out',displayName:'Draft Out',metadataDraft:true}],existingNodeEdits:[],actionEdits:[],nodeDeletes:scenario==='unsaved'?[{nodeId:'listener:one',displayName:'Listener'}]:[],actionDeletes:[],actionReorders:[],dirty:scenario==='unsaved',errors:[],connectionMode:'',saving:false,diffExpanded:scenario==='unsaved'};
                    renderLogicChainViewer(graph,{fallback:'#/logic-chains'},{silent:true});
                  }
                  if(scenario==='vbd'){
                    const native={deviceId:'vbd-one',originalJson:'{}',values:{interactionEnabled:true,interactChannel:'draft.out',containerChangeEnabled:true,containerChangeChannel:'container.out'}};
                    const d={kind:'virtual_block_device',targetId:'vbd-one',deviceId:'vbd-one',displayName:'VBD One',confirmed:true,sourceNodeId:'vbd:one',original:{},virtualBlockDevice:{selectedTriggerType:'right_click',nativeTriggerDraft:native,itemSubmitRequirements:[{displayName:'Item One',count:1,consumeCount:1}]}};
                    appState.logicChainCanvas.selectedNodeId='vbd:two';
                    appState.logicChainCanvas.focusNodeId='vbd:two';
                    appState.logicChainCanvas.selectionPinned=true;
                    appState.logicChainEditor={active:true,routeHash:'#/logic-chains',rootType:'CHANNEL',rootRef:'root',includeDisabled:true,maxDepth:3,lockId:'lock',lockLost:false,baseGraphFingerprint:'base-fp',nodes:[],edges:[],draftChannels:[{channel:'draft.out',displayName:'Draft Out',metadataDraft:true},{channel:'container.out',displayName:'Container Out',metadataDraft:true}],existingNodeEdits:[d],existingEdit:d,actionEdits:[],nodeDeletes:[],actionDeletes:[],actionReorders:[],dirty:true,errors:[],connectionMode:'',saving:false,diffExpanded:true};
                    renderLogicChainViewer(graph,{fallback:'#/logic-chains'},{silent:true});
                  }
                  if(scenario==='minimap'){
                    renderLogicChainViewer(graph,{fallback:'#/logic-chains'},{silent:true});
                  }
                  const html=document.getElementById('app-view').innerHTML||'';
                  let payload='';
                  try{payload=appState.logicChainEditor?.active?JSON.stringify(logicChainEditorSaveBody()):'';}catch(_err){payload='PAYLOAD_ERROR';}
                  const vbdDraft=(appState.logicChainEditor?.existingNodeEdits||[]).find(d=>String(d?.kind||'').toLowerCase()==='virtual_block_device')||null;
                  let vbdTriggerKeys='',vbdSelectedTrigger='',vbdDraftSourceNodeId='';
                  if(vbdDraft){const draft=logicChainVbdStoredNativeTriggerDraft(vbdDraft);vbdSelectedTrigger=String(vbdDraft?.virtualBlockDevice?.selectedTriggerType||logicChainExistingVbdSelectedTrigger(vbdDraft)||'');vbdDraftSourceNodeId=String(vbdDraft.sourceNodeId||'');try{vbdTriggerKeys=logicChainVbdOutputRowsForSelectedTrigger(vbdDraft,draft).map(row=>String(row.triggerKey||row.type||'')).join(',');}catch(_ignored){}}
                  return {html,payloadPendingDeleteLeak:payload.includes('_pendingDelete')?'true':'false',vbdTriggerKeys,vbdSelectedTrigger,vbdDraftSourceNodeId};
                };
                function phase912DomGraph(scenario){
                  const graph={id:'phase3-dom',componentId:'phase3-dom',displayName:'Phase 3 DOM',root:{id:'producer:root',type:'producer',label:'Root',channel:'root'},metadata:{rootType:'CHANNEL',rootRef:'root'},stats:{},segments:[{channel:'root',downstreamChannels:['draft.out','container.out']},{channel:'draft.out',downstreamChannels:[]},{channel:'container.out',downstreamChannels:[]}],nodes:[{id:'producer:root',type:'producer',label:'Root',channel:'root',enabled:true,metadata:{nodeKind:'primary'}},{id:'channel:root',type:'channel',label:'Root Channel',channel:'root',enabled:true,metadata:{nodeKind:'primary'}},{id:'listener:one',type:'consumer',refType:'signal_listener',label:'Listener',channel:'root',enabled:true,metadata:{ownerId:'listener-one'}},{id:'action:one',type:'action',label:'Action',enabled:true,metadata:{ownerType:'listener',ownerId:'listener-one',actionIndex:0,bucket:'default'}},{id:'vbd:one',type:'producer',refType:'virtual_block_device',refId:'vbd-one',label:'VBD One',channel:'root',enabled:true,metadata:{deviceId:'vbd-one',kind:'virtual_block_device',nodeKind:'primary'}},{id:'vbd:two',type:'producer',refType:'virtual_block_device',refId:'vbd-one',label:'VBD Two',channel:'root',enabled:true,metadata:{deviceId:'vbd-one',kind:'virtual_block_device',nodeKind:'reference',isReferenceCard:true,primaryNodeId:'vbd:one'}}],edges:[{from:'producer:root',to:'channel:root',type:'emits',label:'emits'},{from:'channel:root',to:'listener:one',type:'consumes',label:'consumes'},{from:'listener:one',to:'action:one',type:'executes',label:'executes'},{from:'vbd:one',to:'channel:root',type:'vbd_outputs_channel',label:'vbd output',pathGroupId:'draft'}]};
                  if(scenario==='minimap')graph.segments=Array.from({length:30},(_,i)=>({channel:'cap.'+i,downstreamChannels:Array.from({length:i%4},(__,j)=>'cap.'+i+'.'+j)}));
                  return graph;
                }`,context,{filename:'webadmin-app.js'});
                function sha(value){return crypto.createHash('sha256').update(String(value||'')).digest('hex').slice(0,16);}
                function attr(tag,name){const m=String(tag||'').match(new RegExp(name+'="([^"]*)"'));return m?m[1]:'';}
                function count(value,re){return (String(value||'').match(re)||[]).length;}
                function snapshot(result){
                  const html=result.html||'';
                  const pathTags=[...html.matchAll(/<path class="logic-chain-edge[^"]*"[^>]*>/g)].map(m=>m[0]);
                  const edges=pathTags.map(tag=>[
                    attr(tag,'data-logic-chain-edge-from'),
                    attr(tag,'data-logic-chain-edge-to'),
                    attr(tag,'data-logic-chain-edge-type'),
                    attr(tag,'data-logic-chain-edge-path-group'),
                    attr(tag,'data-logic-chain-edge-visual-style'),
                    attr(tag,'data-logic-chain-route-shape'),
                    attr(tag,'data-logic-chain-vbd-trigger-key'),
                    attr(tag,'data-logic-chain-vbd-trigger-type'),
                    attr(tag,'data-logic-chain-vbd-stable-trigger-identity'),
                    attr(tag,'class').replace(/\\s+/g,' ').trim(),
                    attr(tag,'d'),
                    attr(tag,'marker-end')||'',
                    tag.includes('data-logic-chain-target-arrow-owner="true"')?'owner':''
                  ].join('|'));
                  const treeTags=[...html.matchAll(/<div class="logic-chain-tree-node[^"]*"[^>]*data-logic-chain-node-id="([^"]*)"[^>]*style="left:([^;]+);top:([^;]+);width:([^;]+);height:([^;"]+)/g)].map(m=>`${m[1]}@${m[2]},${m[3]},${m[4]},${m[5]}`);
                  const cardTags=[...html.matchAll(/<div role="button"[^>]*class="logic-chain-node-card[^"]*"[^>]*>/g)].map(m=>m[0]);
                  const cards=cardTags.map(tag=>[
                    attr(tag,'data-logic-chain-node-id'),
                    attr(tag,'data-node-type'),
                    attr(tag,'data-logic-chain-node-action'),
                    attr(tag,'data-logic-chain-primary-node-id'),
                    attr(tag,'class').replace(/\\s+/g,' ').trim(),
                    tag.includes('data-logic-chain-pending-delete-card="true"')?'pending':'',
                    tag.includes('data-logic-chain-vbd-trigger-source-card-draft="true"')?'vbdSource':''
                  ].join('|'));
                  const panel=(html.match(/<aside class="logic-chain-right"[^>]*>([\\s\\S]*?)<\\/aside>/)||['',''])[1];
                  const diff=(html.match(/<section class="logic-chain-draft-diff-banner[\\s\\S]*?<\\/section>/)||[''])[0];
                  const mini=(html.match(/<div class="logic-chain-minimap"[^>]*>([\\s\\S]*?)<\\/div><\\/div>/)||['',''])[1];
                  const surface=(html.match(/<div class="logic-chain-surface"[^>]*style="([^"]*)"/)||['',''])[1];
                  const vbdSourceIds=cardTags.filter(tag=>tag.includes('data-logic-chain-vbd-trigger-source-card-draft="true"')).map(tag=>attr(tag,'data-logic-chain-node-id')).join(',');
                  const vbdEdges=pathTags.filter(tag=>attr(tag,'data-logic-chain-vbd-trigger-key')).map(tag=>`${attr(tag,'data-logic-chain-edge-from')}->${attr(tag,'data-logic-chain-edge-to')}#${attr(tag,'data-logic-chain-vbd-trigger-key')}`);
                  return {
                    nodeCount:treeTags.length,
                    edgeCount:edges.length,
                    markerEnd:count(html,/marker-end=/g),
                    arrowOwner:count(html,/data-logic-chain-target-arrow-owner="true"/g),
                    related:count(html,/ related/g),
                    dimmed:count(html,/ dimmed/g),
                    minimapSegments:count(mini,/logic-chain-minimap-segment/g),
                    diffCount:count(html,/data-logic-chain-draft-diff/g),
                    vbdSourceCard:count(html,/data-logic-chain-vbd-trigger-source-card-draft="true"/g),
                    vbdSourceNodeIds:vbdSourceIds,
                    vbdTriggerKeys:result.vbdTriggerKeys||'',
                    vbdDraftSourceNodeId:result.vbdDraftSourceNodeId||'',
                    payloadPendingDeleteLeak:result.payloadPendingDeleteLeak||'false',
                    nodeHash:sha(treeTags.join('\\n')),
                    edgeHash:sha(edges.join('\\n')),
                    classHash:sha(cards.join('\\n')),
                    panelHash:sha(panel),
                    diffHash:sha(diff),
                    minimapHash:sha(mini),
                    transformHash:sha(surface),
                    vbdHash:sha([vbdSourceIds,vbdEdges.join('\\n'),result.vbdTriggerKeys||'',result.vbdDraftSourceNodeId||''].join('\\n')),
                    domHash:sha([treeTags.join('\\n'),edges.join('\\n'),cards.join('\\n'),panel,diff,mini,surface].join('\\n---\\n'))
                  };
                }
                function emitScenario(name,snap){
                  for(const key of ['nodeCount','edgeCount','markerEnd','arrowOwner','related','dimmed','minimapSegments','diffCount','vbdSourceCard','vbdSourceNodeIds','vbdTriggerKeys','vbdDraftSourceNodeId','payloadPendingDeleteLeak','nodeHash','edgeHash','classHash','panelHash','diffHash','minimapHash','transformHash','vbdHash','domHash']){
                    emit(`scenario.${name}.${key}=${snap[key]}`);
                  }
                }
                for(const name of ['initial','draft','unsaved','vbd','minimap']){
                  emitScenario(name,snapshot(context.__phase912DomRun(name,'canonical')));
                }
                for(const name of ['hover','selection','zoom']){
                  const canonical=snapshot(context.__phase912DomRun(name,'canonical'));
                  const interaction=snapshot(context.__phase912DomRun(name,'interaction'));
                  emit(`equivalence.${name}=${canonical.domHash===interaction.domHash}`);
                  emit(`equivalence.${name}.canonical=${canonical.domHash}`);
                  emit(`equivalence.${name}.interaction=${interaction.domHash}`);
                  emitScenario(name,interaction);
                }
                fs.writeFileSync(outputPath,rows.join('\\n'),'utf8');
                console.log(`rows=${rows.length}`);
                process.exit(0);
                """;
    }
}
