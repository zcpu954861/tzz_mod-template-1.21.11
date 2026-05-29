package com.zcpu.tzzmod.webadmin;

final class WebAdminLogicChainViewerScripts {
    private WebAdminLogicChainViewerScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""

                """)
                .append("""
                async function renderLogicChainsPage(options={}){
                """)
                .append("""
                  if(!options.silent)setView(loading('正在加载逻辑链...'));
                """)
                .append("""
                  let chains;try{chains=await api('/api/webadmin/logic-chains')}catch(err){if(options.silent){toast('逻辑链列表实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                """)
                .append("""
                  appState.logicChains=chains||[];
                """)
                .append("""
                  renderLogicChainList('',options);
                """)
                .append("""
                }
                """)
                .append("""
                function renderLogicChainList(focusId,options={}){
                """)
                .append("""
                  waEnsureState();
                """)
                .append("""
                  const chains=appState.logicChains||[], filtered=logicChainFilter(chains), page=waPageItems('logicChains',filtered,10);
                """)
                .append("""
                  const saved=chains.filter(c=>c.saved).length, warning=chains.filter(c=>String(c.doctorStatus||'').toUpperCase()!=='OK').length;
                """)
                .append("""
                  if(setView(`<section class="wa-page" data-logic-chain-list-page="true" data-logic-chain-list-metadata-first="true" data-logic-chain-one-entry-per-component="true" data-logic-chain-list-no-duplicate-component-channels="true">
                """)
                .append("""
                    ${waPageHead('逻辑链','跨频道逻辑链查看器与受控编辑入口。视图由 SignalBridge 现有关系和 WebAdmin metadata 推导，不改变运行时。',`${canEditLogicChainMetadata()?waButton('新建逻辑链','logic-chain','onclick="startNewLogicChainMetadataCreate()" data-logic-chain-new-entry="true" data-logic-chain-create-root-channel="true" data-logic-chain-disconnected-draft-new-chain="true"','primary'):waButton('新建逻辑链','logic-chain','disabled data-logic-chain-edit-locked-disabled="role"','ghost')}${waButton('刷新','refresh','onclick="renderLogicChainsPage()"','ghost')}`)}
                """)
                .append("""
                    <section class="wa-card-grid wa-metrics-4">
                """)
                .append("""
                      ${waMetric('链路候选',chains.length,'保存视图与自动发现频道','channel-total')}
                """)
                .append("""
                      ${waMetric('已保存视图',saved,'WebAdmin-only 视图 metadata','action-template','ok')}
                """)
                .append("""
                      ${waMetric('涉及频道',uniqueValues(chains.flatMap(c=>Array.isArray(c.includedChannels)&&c.includedChannels.length?c.includedChannels:[c.rootChannel]).filter(Boolean)).length,'component includedChannels 去重','active-channel')}
                """)
                .append("""
                      ${waMetric('需关注',warning,'Doctor 非 OK 或断链','warning-issue',warning?'warning':'')}
                """)
                .append("""
                    </section>
                """)
                .append("""
                    <section class="wa-table-card">
                """)
                .append("""
                      <div class="wa-filter-bar">
                """)
                .append("""
                        <label class="filter-field search-control"><span>搜索</span><input class="input" id="logic-chain-search" placeholder="搜索链路名称 / root / channel..." value="${esc(appState.logicChainFilters.search)}"></label>
                """)
                .append("""
                        <label class="filter-field"><span>状态</span>${waSelect('logic-chain-status',['ALL','OK','WARNING','ERROR','UNKNOWN'],appState.logicChainFilters.status,optionLabel)}</label>
                """)
                .append("""
                        <label class="filter-field"><span>来源</span>${waSelect('logic-chain-saved',['ALL','SAVED','AUTO'],appState.logicChainFilters.saved,v=>({ALL:'全部',SAVED:'已保存',AUTO:'自动发现'}[v]||v))}</label>
                """)
                .append("""
                        ${waButton('刷新','refresh','onclick="renderLogicChainsPage()"','ghost')}
                """)
                .append("""
                      </div>
                """)
                .append("""
                      ${page.items.length===0?empty(chains.length===0?'当前暂无可解析逻辑链。':'没有匹配当前筛选条件的逻辑链。'):logicChainTable(page.items)}
                """)
                .append("""
                      ${waPagination('logicChains',page)}
                """)
                .append("""
                    </section>
                """)
                .append("""
                  </section>`,options))bindLogicChainFilters(focusId);
                """)
                .append("""
                }
                """)
                .append("""
                function bindLogicChainFilters(focusId){const update=(event)=>{appState.logicChainFilters.search=document.getElementById('logic-chain-search')?.value||'';appState.logicChainFilters.status=document.getElementById('logic-chain-status')?.value||'ALL';appState.logicChainFilters.saved=document.getElementById('logic-chain-saved')?.value||'ALL';appState.uiPages.logicChains=1;renderLogicChainList(event?.target?.id||'');};['logic-chain-search','logic-chain-status','logic-chain-saved'].forEach(id=>document.getElementById(id)?.addEventListener(id==='logic-chain-search'?'input':'change',update));if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}}
                """)
                .append("""
                function logicChainTable(items){const rows=logicChainPrimaryRows(items);return `<div class="wa-table-scroll" data-logic-chain-sub-chain-hierarchy="true" data-logic-chain-sub-chain-hierarchy-mode="collapsed-to-detail" data-no-flat-all-chains-list="true" data-logic-chain-list-prefers-metadata-root-entries="true" data-logic-chain-component-entry-list="true" data-third-level-sub-chain="collapsed-to-detail" data-multiple-upstream-reference="guarded" data-cycle-hierarchy-guard="true" data-logic-chain-child-toggle-collapses="true" data-nested-child-chain-collapse="true" data-expanded-state-keyed-by-chain-id="true" data-self-cycle-not-child-chain="true"><table class="wa-table" data-logic-chain-list-table="true"><thead><tr><th>逻辑链入口</th><th>默认焦点频道</th><th>包含频道</th><th>Join / Timer / Listener</th><th>动作</th><th>最近状态</th><th>来源</th><th>操作</th></tr></thead><tbody>${rows.map(item=>logicChainTableRow(item)).join('')}</tbody></table></div>`;}
                """)
                .append("""
                function logicChainPrimaryRows(items){const byId={};(items||[]).forEach(item=>{byId[item.id]=item;});return (items||[]).filter(item=>item.saved||item.visibleInTopLevel!==false||!byId[item.parentChainId]).sort((a,b)=>Number(b.saved)-Number(a.saved)||String(a.displayName||a.rootChannel||'').localeCompare(String(b.displayName||b.rootChannel||'')));}
                """)
                .append("""
                function logicChainHierarchyRows(items){return logicChainPrimaryRows(items);}
                """)
                .append("""
                function logicChainTableRow(item){const target=logicChainHash(item.id), channels=Array.isArray(item.includedChannels)?item.includedChannels.filter(Boolean):[], focus=item.defaultFocusChannel||item.rootChannel||channels[0]||'', title=item.displayName||focus||item.id, sourceKey=String(item.source||'').toLowerCase(), source=sourceKey==='auto_component'?'自动发现 component':(sourceKey==='template_apply'?'模板应用':'已保存 metadata'), sourceMarker=sourceKey==='auto_component'?'data-logic-chain-source-auto-component="true"':'data-logic-chain-source-metadata="true"', channelCount=channels.length||item.channelCount||0, listenerCount=item.listenerCount??item.consumerCount??0, channelPreview=channels.length?`${channels.slice(0,3).map(esc).join(' / ')}${channels.length>3?' ...':' · 详情页内切换焦点'}`:'详情页内切换焦点';return `<tr class="wa-clickable-row logic-chain-row" data-logic-chain-primary-entry="true" data-logic-chain-component-entry="${esc(item.componentId||item.id||'')}" data-logic-chain-default-focus-channel="${esc(focus||'')}" ${navDataAttr(target,`查看逻辑链 ${title}`)}><td><span class="device-name logic-chain-row-title"><span class="device-icon">${icon(item.metadata?.effectiveIconKey||'logic-chain')}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">${esc(item.metadata?.note||item.rootRef||item.id||'')}</span></span></span></td><td><strong>${esc(focus||'-')}</strong><span class="device-subtitle">${esc(logicChainRootTypeLabel(item.rootType))}: ${esc(item.rootRef||'-')}</span></td><td data-logic-chain-included-channel-count="true">${esc(channelCount)}<span class="device-subtitle">${channelPreview}</span></td><td>${esc(item.signalJoinCount??'--')} / ${esc(item.timerCount??'--')} / ${esc(listenerCount)}</td><td>${esc(item.actionCount||0)}</td><td>${pill(item.doctorStatus||'UNKNOWN')}<span class="device-subtitle">${fmtTime(item.lastTriggeredAt)}</span></td><td><span ${sourceMarker}>${textPill(source,item.saved?'ok':'info')}</span></td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看逻辑链 ${title}`)}>查看</button>${focus?`<button class="wa-btn ghost" ${navDataAttr(`${target}?focusChannel=${encodeURIComponent(focus)}`,`以 ${focus} 为焦点查看`)}>焦点</button>`:''}</div></td></tr>`;}
                """)
                .append("""
                function toggleLogicChainRow(id){appState.logicChainFilters.expanded=appState.logicChainFilters.expanded||{};const key=String(id||''), scrollY=window.scrollY||document.documentElement.scrollTop||0;if(appState.logicChainFilters.expanded[key]===false)delete appState.logicChainFilters.expanded[key];else appState.logicChainFilters.expanded[key]=false;renderLogicChainList('');requestAnimationFrame(()=>window.scrollTo({top:scrollY,left:0,behavior:'auto'}));}
                """)
                .append("""
                async function renderLogicChainLegacyChannelRoute(hash,options={}){
                """)
                .append("""
                  const params=parseHashParams(hash), channel=String(params.channel||params.focusChannel||params.focus||'').trim();
                """)
                .append("""
                  if(!channel){return renderLogicChainsPage(options);}
                """)
                .append("""
                  if(!options.silent)setView(loading('正在定位逻辑链组件...'));
                """)
                .append("""
                  let chains;try{chains=await api('/api/webadmin/logic-chains')}catch(err){if(options.silent){toast('旧频道入口实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                """)
                .append("""
                  appState.logicChains=chains||[];
                """)
                .append("""
                  const match=(chains||[]).find(item=>Array.isArray(item.includedChannels)&&item.includedChannels.includes(channel))||(chains||[]).find(item=>String(item.rootChannel||'')===channel||String(item.defaultFocusChannel||'')===channel);
                """)
                .append("""
                  if(match?.id){const target=`#/logic-chains/${encodeURIComponent(match.id)}?focusChannel=${encodeURIComponent(channel)}`;if(currentRouteHash()===target)return renderLogicChainDetail(`${encodeURIComponent(match.id)}?focusChannel=${encodeURIComponent(channel)}`,options);navigateTo(target);return;}
                """)
                .append("""
                  return renderLogicChainResolve(logicChainResolveHash('channel',channel),options);
                """)
                .append("""
                }
                """)
                .append("""
                async function renderLogicChainResolve(hash,options={}){
                """)
                .append("""
                  const params=parseHashParams(hash), rootType=params.rootType||'channel', rootRef=params.rootRef||'', includeDisabled=params.includeDisabled||'true', maxDepth=params.maxDepth||'3';
                """)
                .append("""
                  if(!options.silent)setView(loading('正在解析逻辑链...'));
                """)
                .append("""
                  let graph;try{graph=await api(`/api/webadmin/logic-chains/resolve?rootType=${encodeURIComponent(rootType)}&rootRef=${encodeURIComponent(rootRef)}&includeDisabled=${encodeURIComponent(includeDisabled)}&maxDepth=${encodeURIComponent(maxDepth)}`)}catch(err){if(options.silent){toast('逻辑链实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                """)
                .append("""
                  renderLogicChainViewer(graph,{fallback:'#/logic-chains',returnTo:isValidReturnHash(params.returnTo)?params.returnTo:'',temporary:true,focusChannel:rootType==='channel'?rootRef:'',chainId:''},options);
                """)
                .append("""
                }
                """)
                .append("""
                async function renderLogicChainDetail(rawId,options={}){
                """)
                .append("""
                  const routeInfo=detailRoute(rawId,'#/logic-chains'), chainId=routeInfo.id||'';
                """)
                .append("""
                  const params=parseHashParams(rawId), focusChannel=params.focusChannel||params.focus||'';
                """)
                .append("""
                  routeInfo.chainId=chainId;
                """)
                .append("""
                  routeInfo.focusChannel=focusChannel;
                """)
                .append("""
                  if(!options.silent)setView(loading('正在加载逻辑链详情...'));
                """)
                .append("""
                  const focusQuery=focusChannel?`?focusChannel=${encodeURIComponent(focusChannel)}`:'';
                """)
                .append("""
                  let graph;try{graph=await api(`/api/webadmin/logic-chains/${encodeURIComponent(chainId)}${focusQuery}`)}catch(err){if(options.silent){toast('逻辑链详情实时刷新失败，已保留当前页面。');return;}setView(`<section class="wa-page"><div class="back-row">${backButton(routeInfo,'返回逻辑链')}</div>${waPageHead('逻辑链不可用','该逻辑链 metadata 不存在或 root 无法解析。',waButton('返回列表','logic-chain',navigationAttr('#/logic-chains'),'ghost'))}${errorBlock(err.message)}</section>`);return;}
                """)
                .append("""
                  renderLogicChainViewer(graph,routeInfo,options);
                """)
                .append("""
                }
                """)
                .append("""
                function renderLogicChainViewer(graph,routeInfo={},options={}){
                """)
                .append("""
                  appState.currentLogicChainGraph=graph||{};
                """)
                .append("""
                  appState.logicChainCanvas.routeInfo={fallback:routeInfo.fallback||'#/logic-chains',returnTo:isValidReturnHash(routeInfo.returnTo)?routeInfo.returnTo:'',chainId:routeInfo.chainId||graph?.metadata?.id||'',focusChannel:routeInfo.focusChannel||graph?.stats?.focusChannel||graph?.metadata?.rootChannel||graph?.root?.channel||'',temporary:!!routeInfo.temporary};
                """)
                .append("""
                  const renderedGraph=logicChainRenderedGraphWithDraftOverlay(graph), canvasNodes=logicChainNodeMap(renderedGraph), detailGraph=logicChainGraphWithNewDraftDetails(renderedGraph), nodes=logicChainNodeMap(detailGraph), metadata=graph?.metadata||{}, stats=graph?.stats||{}, root=graph?.root||{};
                """)
                .append("""
                  const graphKey=metadata.id||`${metadata.rootType||root.refType||'channel'}:${metadata.rootRef||metadata.rootChannel||root.channel||root.id||''}`;
                """)
                .append("""
                  if(appState.logicChainCanvas.graphKey!==graphKey){appState.logicChainCanvas.graphKey=graphKey;appState.logicChainCanvas.panX=0;appState.logicChainCanvas.panY=0;appState.logicChainCanvas.zoom=1;appState.logicChainCanvas.collapsedChannels={};appState.logicChainCanvas.selectedNodeId=root.id||Object.keys(nodes)[0]||'';appState.logicChainCanvas.highlightNodeId='';appState.logicChainCanvas.selectionPinned=false;appState.logicChainCanvas.focusNodeId='';appState.logicChainCanvas.detailOpen=true;}
                """)
                .append("""
                  if(appState.logicChainCanvas.detailOpen!==false&&(!appState.logicChainCanvas.selectedNodeId||!nodes[appState.logicChainCanvas.selectedNodeId]))appState.logicChainCanvas.selectedNodeId=root.id||Object.keys(nodes)[0]||'';
                """)
                .append("""
                  const title=metadata.effectiveDisplayName||metadata.displayName||root.label||metadata.rootChannel||'逻辑链';
                """)
                .append("""
                  const warningHtml=(graph?.warnings||[]).length?`<div class="logic-chain-warnings">${(graph.warnings||[]).slice(0,6).map(w=>`<span>${esc(w)}</span>`).join('')}</div>`:'';
                """)
                .append("""
                  const editor=appState.logicChainEditor||{}, editorMode=!!editor.active;
                """)
                .append("""
                  const actions=[logicChainMetadataAction(graph),logicChainEditorAction(graph),waButton('刷新','refresh',htmlHandler('route({silent:true})'),'ghost'),waButton('自动布局','logic-chain','onclick="resetLogicChainLayout()"','ghost')].filter(Boolean);
                """)
                .append("""
                  const rendered=setView(`<section class="wa-page logic-chain-page" data-logic-chain-viewer-page="true" data-logic-chain-enhanced-runtime-graph="true" data-logic-chain-detail-focus-channel-selector="true" data-logic-chain-focus-switch-updates-route-state="true" data-logic-chain-old-channel-route-compatible="true" data-logic-chain-editor-mvp="${editorMode?'true':'available'}" data-logic-chain-readonly-graph="true" data-logic-chain-readonly-graph-mode="${editorMode?'draft-overlay':'true'}" data-logic-chain-draft-overlay="${editorMode?'true':'false'}" data-logic-chain-rendered-graph-overlay="${editorMode?'true':'false'}" data-no-condition-engine-editing="true" data-no-runtime-node-creation="true" data-no-runtime-node-creation-mode="${editorMode?'config-only-new-node':'true'}" data-logic-chain-no-runtime-mutation="true" data-logic-chain-save-writes-underlying-config="${editorMode?'true':'available'}">
                """)
                .append("""
                    ${detailHeader({back:backButton(appState.logicChainCanvas.routeInfo,'返回逻辑链'),kicker:'WebAdmin / 跨频道逻辑链',iconName:metadata.effectiveIconKey||'logic-chain',title,subtitle:'思维导图模式：只读视图由现有 SignalBridge 关系推导跨频道逻辑树，不修改运行时。',copyValue:metadata.id||metadata.rootRef||metadata.rootChannel,helpTopic:'logic-chain.editor-draft',badges:[`<span class="pill info">根节点: ${esc(logicChainRootTypeLabel(metadata.rootType||root.refType))}</span>`,`<span class="pill">${esc(metadata.rootChannel||root.channel||'未解析频道')}</span>`,pill(logicChainGraphDoctorStatus(graph))],actions})}
                """)
                .append("""
                    ${warningHtml}
                """)
                .append("""
                    ${logicChainComponentFocusCard(graph)}
                """)
                .append("""
                    <section class="logic-chain-layout">
                """)
                .append("""
                      <aside class="logic-chain-legend" data-logic-chain-legend="true">${logicChainLegend(graph)}</aside>
                """)
                .append("""
                      <main class="logic-chain-canvas-shell" data-channel-logic-chain-viewer="true" data-logic-chain-mind-map-tree="true" data-curved-connectors="true" data-no-table-like-fixed-lane-layout="true" data-root-channel-consumer-action-downstream-tree="true" data-channel-separates-consumers="true" data-same-channel-consumers-parallel="true" data-action-order-local-only="true" data-downstream-channel-child-subtree="true" data-no-cross-channel-consumer-mixing="true" data-no-cross-channel-long-line-mixing="true">
                """)
                .append("""
                        ${logicChainCanvasToolbar()}
                """)
                .append("""
                        ${logicChainCanvas(renderedGraph,canvasNodes)}
                """)
                .append("""
                      </main>
                """)
                .append("""
                      <aside class="logic-chain-right" data-logic-chain-node-detail="true" data-logic-chain-node-detail-panel="true">${logicChainSelectedNodePanel(detailGraph,nodes)}</aside>
                """)
                .append("""
                    </section>
                """)
                .append("""
                    <section class="logic-chain-footer">${logicChainStats(stats)}${logicChainVersionNote()}</section>
                """)
                .append("""
                  </section>`,options);
                """)
                .append("""
                  if(rendered)renderIcons(appView());
                """)
                .append("""
                }
                """)
                .append("""
                function logicChainGraphDoctorStatus(graph){const nodes=graph?.nodes||[];if(nodes.some(n=>String(n.doctorStatus||n.status||n.metadata?.runtimeStatus||'').toUpperCase()==='ERROR'))return 'ERROR';if((graph?.warnings||[]).length||nodes.some(n=>['WARNING','BLOCKED','MISSING'].includes(String(n.doctorStatus||n.status||n.metadata?.runtimeStatus||'').toUpperCase())))return 'WARNING';return 'OK';}
                """)
                .append("""
                function logicChainFocusChannelOptions(graph){const values=[];const add=value=>{const text=String(value||'').trim();if(text&&!values.includes(text))values.push(text);};const metadata=graph?.metadata||{}, stats=graph?.stats||{}, summary=stats.componentSummary||{};add(stats.focusChannel);add(metadata.rootChannel);add(metadata.rootRef);add(graph?.root?.channel);[...(Array.isArray(stats.includedChannels)?stats.includedChannels:[]),...(Array.isArray(summary.includedChannels)?summary.includedChannels:[])].forEach(add);(graph?.nodes||[]).forEach(node=>{const type=String(node?.type||'').toLowerCase();if(type==='channel'||type==='downstream_channel'||String(node?.id||'').startsWith('channel:'))add(node.channel||node.refId||String(node.id||'').replace(/^channel:/,''));});return values;}
                """)
                .append("""
                function logicChainComponentFocusCard(graph){const stats=graph?.stats||{}, summary=stats.componentSummary||{}, focus=stats.focusChannel||summary.focusChannel||graph?.metadata?.rootChannel||graph?.root?.channel||'', truncated=stats.componentTruncated||summary.truncated, reason=stats.componentTruncationReason||summary.truncationReason||'', options=logicChainFocusChannelOptions(graph), selector=options.length>1?`<label class="filter-field logic-chain-focus-select" data-logic-chain-focus-channel-selector="true"><span>焦点频道</span><select class="select" id="logic-chain-focus-channel" onchange="switchLogicChainFocusChannel(this.value)">${options.map(ch=>`<option value="${esc(ch)}" ${ch===focus?'selected':''}>${esc(ch)}</option>`).join('')}</select></label>`:`<span class="pill info" data-logic-chain-focus-channel-selector="true">焦点频道：${esc(focus||'未解析')}</span>`;return `<section class="logic-chain-component-focus" data-logic-chain-component-aware-mode="true" data-logic-chain-focus-channel="${esc(focus||'')}" data-logic-chain-component-summary="true" data-logic-chain-join-all-input-channels-visible="${truncated?'partial':'true'}"><div><strong>当前焦点频道：${esc(focus||'未解析')}</strong><span>本图为关联组件视图；root channel 只负责默认入口，详情页内可切换焦点频道。</span></div><div class="logic-chain-component-metrics"><span>${esc(summary.channelCount??stats.componentChannelCount??0)} 个频道</span><span>${esc(summary.signalJoinCount??stats.signalJoinCount??0)} 个 Join</span><span>${esc(summary.timerCount??stats.timerCount??0)} 个 Timer</span><span>${esc(summary.consumerCount??stats.consumerCount??0)} 个消费者</span></div><div class="inline-actions">${selector}<button class="wa-btn ghost" type="button" onclick="setLogicChainViewMode('COMPONENT')" data-logic-chain-expand-related="true">展开相关</button><span class="device-subtitle" data-logic-chain-collapsed-related-marker="true">${truncated?esc(reason||'因图规模限制未完全展示，部分弱关联已折叠。'):'弱关联默认按安全限制折叠，避免整服图谱连成一片。'}</span></div></section>`;}
                """)
                .append("""
                function switchLogicChainFocusChannel(channel){const focus=String(channel||'').trim();if(!focus){toast('焦点频道不能为空。');return;}const routeInfo=appState.logicChainCanvas.routeInfo||{}, chainId=routeInfo.chainId||appState.currentLogicChainGraph?.metadata?.id||'';appState.logicChainCanvas.focusChannel=focus;if(routeInfo.temporary||!chainId){navigateTo(logicChainResolveHash('channel',focus));return;}const target=`#/logic-chains/${encodeURIComponent(chainId)}?focusChannel=${encodeURIComponent(focus)}`;if(currentRouteHash()===target)route({silent:true});else navigateTo(target);}
                """)
                .append("""
                function logicChainViewModeLabel(value){return {COMPONENT:'组件视图',BOTH:'双向',DOWNSTREAM:'下游',UPSTREAM:'上游',RELATED:'相关节点'}[String(value||'COMPONENT').toUpperCase()]||String(value||'组件视图');}
                """)
                .append("""
                function logicChainNodeTypeFilterLabel(value){return {ALL:'全部节点',SIGNAL:'信号 / 频道',JOIN:'信号汇合',TIMER:'计时器',STATE:'状态',GATE:'条件门控',ACTION:'动作'}[String(value||'ALL').toUpperCase()]||String(value||'全部节点');}
                """)
                .append("""
                function logicChainSelectOptions(options,value,labeler){let list=[...(options||[])];if((String(value||'').toUpperCase()==='COMPONENT'||list.includes('BOTH'))&&!list.includes('COMPONENT'))list=['COMPONENT',...list];return list.map(o=>`<option value="${esc(o)}" ${String(o)===String(value)?'selected':''}>${esc(labeler(o))}</option>`).join('');}
                """)
                .append("""
                function logicChainLegend(graph){const stats=graph?.stats||{}, canvas=appState.logicChainCanvas||{};return `<h2>图例 / 筛选</h2><div class="logic-chain-legend-list"><span class="logic-chain-chip producer">${icon('signal-device')}触发源</span><span class="logic-chain-chip channel">${icon('active-channel')}频道</span><span class="logic-chain-chip consumer">${icon('consumer-listener')}消费者</span><span class="logic-chain-chip join">${icon('signal-join')}信号汇合</span><span class="logic-chain-chip timer">${icon('timer')}计时器</span><span class="logic-chain-chip state">${icon('state-action')}状态</span><span class="logic-chain-chip gate">${icon('runtime-gate')}条件门控</span><span class="logic-chain-chip action">${icon('action-binding')}动作</span><span class="logic-chain-chip reference">引用卡</span><span class="logic-chain-chip disabled">停用节点</span><span class="logic-chain-chip warning">断链 / 异常</span></div><div class="logic-chain-path-legend" data-logic-chain-path-color-legend="true" data-logic-chain-default-edge-opacity="0.86"><span class="group-signal">主路径</span><span class="group-consumer">消费者</span><span class="group-execution">执行</span><span class="group-downstream">下游</span><span class="group-join" data-logic-chain-join-primary-input-edge="true">Join 主输入实线</span><span class="group-join dashed" data-logic-chain-join-related-input-edge="true">Join 其他输入虚线</span><span class="group-gate">门控</span><span class="group-timer">Timer</span><span class="group-state">状态写入</span><span class="group-reference dashed" data-logic-chain-reference-edge="true">引用灰色虚线</span></div><div class="logic-chain-filter-grid"><label class="filter-field" data-logic-chain-view-mode-filter="true"><span>视图模式</span><select class="select" id="logic-chain-view-mode" onchange="setLogicChainViewMode(this.value)">${logicChainSelectOptions(['BOTH','DOWNSTREAM','UPSTREAM','RELATED'],canvas.viewMode||'BOTH',logicChainViewModeLabel)}</select></label><label class="filter-field" data-logic-chain-node-type-filter="true"><span>节点类型</span><select class="select" id="logic-chain-node-type-filter" onchange="setLogicChainNodeTypeFilter(this.value)">${logicChainSelectOptions(['ALL','SIGNAL','JOIN','TIMER','STATE','GATE','ACTION'],canvas.nodeTypeFilter||'ALL',logicChainNodeTypeFilterLabel)}</select></label></div><p class="muted">GraphModel V2 使用 Join 专用布局：输入和上游在左侧，Join 居中，输出频道和下游在右侧。</p><p class="muted">同一 output channel 只保留一个主节点；引用卡只用于定位，不继续展开下游。</p><p class="muted">主要连线默认清晰可见；点击或悬停节点时仅降低非关联线透明度。</p><div class="identity-grid"><span class="k">最大深度</span><span class="v">${esc(stats.maxDepth||3)}</span><span class="k">图规模限制</span><span class="v">${esc(stats.maxGraphNodes||'-')} nodes / ${esc(stats.maxGraphEdges||'-')} edges</span><span class="k">主 / 引用</span><span class="v">${esc(stats.primaryNodeCount||0)} / ${esc(stats.referenceNodeCount||0)}</span><span class="k">边合并</span><span class="v">${esc(stats.edgeMergeCount||0)}</span><span class="k">截断</span><span class="v" data-logic-chain-graph-truncation-marker="true">${stats.nodesTruncated||stats.edgesTruncated?'图规模已截断，请缩小范围或使用筛选。':'未截断'}</span></div>`;}
                """)
                .append("""
                function logicChainValidationToastSummary(result,fallback='保存失败，草稿已保留。'){const errors=Array.isArray(result?.validationErrors)?result.validationErrors:[],prefix=String(fallback||'').startsWith('草稿校验')?'草稿校验失败':'保存失败';if(errors.length){const first=errors[0]||{},message=first.message||result?.message||fallback;return `${prefix}：${message}${errors.length>1?`（共 ${errors.length} 个问题）`:''}`;}return result?.message||fallback;}
                """)
                .append("""
                function logicChainValidationErrorMeta(error){const rows=[];if(error?.severity)rows.push(['级别',error.severity]);if(error?.nodeId)rows.push(['相关节点',error.nodeId]);if(error?.channelId)rows.push(['相关频道',error.channelId]);if(error?.edgeId)rows.push(['相关连线',error.edgeId]);if(error?.code)rows.push(['错误代码',error.code]);return rows.map(([k,v])=>`<span class="logic-chain-validation-kv"><b>${esc(k)}</b>${esc(v)}</span>`).join('');}
                """)
                .append("""
                function logicChainValidationErrorsHtml(errors,lockLost=false){const list=(Array.isArray(errors)?errors:[]).filter(Boolean);if(!list.length)return '';const note=lockLost?'草稿已保留，但编辑锁已失效，需要重新进入编辑模式。':'编辑锁和草稿会保留，修正后可继续保存。';return `<section class="logic-chain-validation-panel" data-logic-chain-validation-list="true" data-logic-chain-validation-detail-list="true" data-logic-chain-structured-validation-errors="true" data-logic-chain-validation-channel-id="true" data-logic-chain-validation-severity="true" data-logic-chain-validation-fix-hint="true" data-logic-chain-save-error-reason-visible="true" data-logic-chain-save-failure-keeps-edit-session="true" data-logic-chain-save-failure-keeps-lock="${lockLost?'false':'true'}" data-logic-chain-second-save-after-validation-fail="true"><header><strong>保存前需要修正 ${list.length} 个问题</strong><span>${esc(note)}</span></header><ul>${list.map(error=>`<li data-logic-chain-validation-error-code="${esc(error.code||'unknown')}"><strong>${esc(error.message||'保存失败')}</strong><div>${logicChainValidationErrorMeta(error)}</div><p>${esc(error.fixHint||'按提示修正草稿后重新保存。')}</p></li>`).join('')}</ul></section>`;}
                """)
                .append("""
                function logicChainEditorLockFailureCode(code){return ['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(String(code||''));}
                """)
                .append("""
                function logicChainEditorResultLosesCurrentLock(result){const e=appState.logicChainEditor,data=result?.data||{};if(!e||!logicChainEditorLockFailureCode(result?.code))return false;if(data.editorLockLost===true)return true;const expectedTargetId=`${e.lockTargetType||'logic_chain_editor'}:${e.lockTargetId||`${e.rootType||'channel'}:${e.rootRef||''}`}`;return String(result?.targetType||'')==='EDIT_LOCK'&&String(result?.targetId||'')===expectedTargetId;}
                """)
                .append("""
                function logicChainApplyEditorLockFailure(result){const e=appState.logicChainEditor;if(!e||!logicChainEditorResultLosesCurrentLock(result))return false;e.lockLost=true;e.lockId='';e.lock={};stopLogicChainEditorLockHeartbeat();return true;}
                """)
                .append("""
                function logicChainEditorUiState(){const view=appView(),active=document.activeElement;return {scrollTop:view?view.scrollTop:0,scrollLeft:view?view.scrollLeft:0,activeId:active&&active.id?active.id:'',saveButton:!!active?.dataset?.logicChainSaveValidation};}
                """)
                .append("""
                function restoreLogicChainEditorUiState(state){requestAnimationFrame(()=>{const view=appView();if(view&&state){view.scrollTop=state.scrollTop||0;view.scrollLeft=state.scrollLeft||0;}const active=state?.activeId?document.getElementById(state.activeId):(state?.saveButton?document.querySelector('[data-logic-chain-save-validation="true"]'):null);if(active&&typeof active.focus==='function')active.focus({preventScroll:true});});}
                """)
                .append("""
                function rerenderLogicChainEditorPreservingUi(){const state=logicChainEditorUiState();renderLogicChainViewer(appState.currentLogicChainGraph,appState.logicChainCanvas.routeInfo||{fallback:'#/logic-chains'}, {silent:true});restoreLogicChainEditorUiState(state);}
                """)
                .append("""
                function logicChainBuildTree(graph,nodes){const segments=graph?.segments||[], segmentByChannel={};segments.forEach(seg=>{if(seg?.channel&&!segmentByChannel[seg.channel])segmentByChannel[seg.channel]=seg;});const edgeIndexes=logicChainEdgeIndexes(graph);const focus=logicChainFocusNode(graph,nodes)||graph?.root||Object.values(nodes||{})[0];if(!focus)return null;return logicChainTreeFromNode(focus,graph,nodes,segmentByChannel,edgeIndexes,new Set(),new Set(),0);}
                """).toString();
    }
}
