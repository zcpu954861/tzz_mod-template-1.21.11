package com.zcpu.tzzmod.webadmin;

final class WebAdminFrontendSnapshotScripts {
    private WebAdminFrontendSnapshotScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                  const storeNotice=data.storeDegraded?`<div class="error-state">${esc(data.storeMessage||'状态变量存储读取异常。')}</div>`:'';
                  const createButton=canEditStateVariableDefinitions()?waButton('新建状态变量','plus','onclick="startStateVariableCreate()" data-state-variable-definition-edit="true"','primary'):waButton('新建状态变量','plus','disabled data-state-variable-definition-edit="true"','primary');
                  const rendered=setView(`<section class="wa-page" data-state-variable-page="true" data-state-variable-definition-edit="true" data-state-variable-no-raw-json-primary="true" data-state-variable-silent-refresh-preserves-filters="true">
                    ${waPageHead('状态变量','受控查看 / 创建 / 编辑 GLOBAL 与 PLAYER 状态变量定义；保存仍走 WebAdmin 写链路。',`${createButton}${waButton('刷新','refresh','onclick="renderStateVariablesPage()" data-state-variable-refresh="true"','ghost')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('变量总数',summary.totalCount??variables.length,'当前世界状态变量','state-variable')}
                      ${waMetric('全局变量',summary.globalCount??0,'全局作用域','state-variable-global','info')}
                      ${waMetric('玩家变量',summary.playerCount??0,'玩家作用域，按目标 ID 区分','state-variable-player','warning')}
                      ${waMetric('整数变量',summary.integerCount??0,'整数类型','pulse-duration')}
                      ${waMetric('布尔变量',summary.booleanCount??0,'布尔类型','enabled','ok')}
                    </section>
                    ${storeNotice}
                    <section class="wa-table-card" data-state-variable-list="true" data-state-variable-list-loader="true">
                      <div class="wa-filter-bar">
                        <label class="filter-field search-control"><span>搜索变量键 / 当前值</span><input class="input" id="state-variable-search" placeholder="搜索变量键、当前值、显示路径..." value="${esc(appState.stateVariableFilters.search)}" data-state-variable-search-key-target="true"></label>
                        <label class="filter-field"><span>作用域</span>${waSelect('state-variable-scope',['ALL','GLOBAL','PLAYER'],appState.stateVariableFilters.scope,stateVariableOptionLabel)}</label>
                        <label class="filter-field"><span>类型</span>${waSelect('state-variable-type',['ALL','BOOLEAN','INTEGER','STRING'],appState.stateVariableFilters.type,stateVariableOptionLabel)}</label>
                        <label class="filter-field search-control"><span>目标 ID</span><input class="input" id="state-variable-target" placeholder="搜索玩家目标 ID..." value="${esc(appState.stateVariableFilters.target)}" data-state-variable-search-key-target="true"></label>
                        ${waButton('刷新','refresh','onclick="renderStateVariablesPage()"','ghost')}
                      </div>
                      ${page.items.length===0?empty(variables.length===0?'当前暂无状态变量。状态变量会由受控状态动作写入后出现在这里。':'没有匹配当前筛选条件的状态变量。'):stateVariableTable(page.items)}
                      ${waPagination('stateVariables',page)}
                    </section>
                  </section>`,options);
                  if(rendered)bindStateVariableFilters(focusId);
                }
                function stateVariableTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>变量</th><th>作用域</th><th>目标</th><th>类型</th><th>当前值</th><th>版本</th><th>更新时间</th><th>操作</th></tr></thead><tbody>${items.map(v=>{const target=stateVariableHash(v.id), title=v.displayName||v.key||v.id, value=stateVariableValueText(v), editAttrs=canEditStateVariableDefinitions()?htmlHandler(`event.stopPropagation();startStateVariableEdit(${jsString(v.id||'')})`):'disabled';return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看状态变量 ${title}`)} data-state-variable-row-click-detail="true"><td><span class="device-name"><span class="device-icon">${icon('state-variable')}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">${esc(v.displayPath||v.id)}</span></span></span></td><td>${textPill(v.scopeLabel||v.scope,stateVariableScopeTone(v.scope))}</td><td class="truncate" title="${esc(v.targetId||'')}">${esc(v.targetLabel||v.targetId||'--')}</td><td>${textPill(v.typeLabel||v.type,'info')}</td><td class="truncate" title="${esc(value)}" data-state-variable-value-truncated="true">${esc(v.valuePreview||value||'--')}</td><td>${esc(v.version??'--')}</td><td>${fmtTime(v.updatedAt)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看状态变量 ${title}`)}>详情</button><button class="wa-btn ghost" ${editAttrs} data-state-variable-definition-edit="true">编辑</button></div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function bindStateVariableFilters(focusId){
                  const update=(event)=>{appState.stateVariableFilters.search=document.getElementById('state-variable-search')?.value||'';appState.stateVariableFilters.scope=document.getElementById('state-variable-scope')?.value||'ALL';appState.stateVariableFilters.type=document.getElementById('state-variable-type')?.value||'ALL';appState.stateVariableFilters.target=document.getElementById('state-variable-target')?.value||'';appState.uiPages.stateVariables=1;renderStateVariablesPage({silent:true});restoreFocusEnd(event?.target?.id||'');};
                  ['state-variable-search','state-variable-target'].forEach(id=>document.getElementById(id)?.addEventListener('input',update));
                  ['state-variable-scope','state-variable-type'].forEach(id=>document.getElementById(id)?.addEventListener('change',update));
                  restoreFocusEnd(focusId);
                }
                async function renderStateVariableDetail(rawId,options={}){
                  const routeInfo=detailRoute(rawId,'#/state-variables'), id=routeInfo.id||'';
                  if(!options.silent)setView(loading('正在加载状态变量详情...'));
                  let detail;try{detail=await api(`/api/webadmin/state-variables/${encodeURIComponent(id)}`)}catch(err){if(options.silent){toast('状态变量详情实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.currentStateVariableDetail=detail;
                  const valueText=String(detail.valueText??detail.value??''), condition=detail.conditionSuggestion||{};
                  const editButton=canEditStateVariableDefinitions()?waButton('编辑定义','settings',`onclick="startStateVariableEdit(${jsString(detail.id||id)})" data-state-variable-definition-edit="true"`,'primary'):waButton('编辑定义','settings','disabled data-state-variable-definition-edit="true"','ghost');
                  setView(`<section class="wa-page" data-state-variable-detail-loader="true" data-state-variable-definition-edit="true" data-state-variable-no-raw-json-primary="true">
                    ${detailHeader({back:backButton(routeInfo,'返回状态变量'),kicker:'状态变量详情',iconName:'state-variable',title:detail.displayName||detail.key||detail.id,subtitle:detail.displayPath||detail.id,copyValue:detail.key,badges:[textPill(detail.scopeLabel||detail.scope,stateVariableScopeTone(detail.scope)),textPill(detail.typeLabel||detail.type,'info'),`<span class="pill">v${esc(detail.version)}</span>`],actions:[editButton,waButton('刷新','refresh',`onclick="renderStateVariableDetail(${jsString(encodeURIComponent(id))})"`,'ghost')]})}
                    <section class="wa-card-grid wa-metrics-4">
                      ${waMetric('作用域',detail.scopeLabel||detail.scope,'变量作用域',String(detail.scope||'').toUpperCase()==='PLAYER'?'state-variable-player':'state-variable-global')}
                      ${waMetric('目标',detail.targetLabel||'全局','目标 ID','current-user')}
                      ${waMetric('类型',detail.typeLabel||detail.type,'变量类型','pulse-duration')}
                      ${waMetric('版本',detail.version??'--',`指纹 ${detail.fingerprintShort||'--'}`,'history')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <h2>当前值</h2>
                        <pre class="wa-code-block" data-state-variable-value-full="true">${esc(valueText||'')}</pre>
                        <div class="list-stack">
                          <div class="kv-row"><span class="muted">变量键</span><strong>${esc(detail.key)}</strong></div>
                          <div class="kv-row"><span class="muted">目标 ID</span><strong>${esc(detail.targetId||'global')}</strong></div>
                          <div class="kv-row"><span class="muted">指纹</span><strong>${esc(detail.fingerprint||'--')}</strong></div>
                          <div class="kv-row"><span class="muted">更新时间</span><strong>${fmtTime(detail.updatedAt)}</strong></div>
                          <div class="kv-row"><span class="muted">更新者</span><strong>${esc(detail.updatedBy||'未记录')}</strong></div>
                          <div class="kv-row"><span class="muted">创建时间</span><strong>${esc(detail.createdAt||'当前版本未记录')}</strong></div>
                          <div class="kv-row"><span class="muted">存储位置</span><strong>${esc(detail.storagePathSummary||'world/tzz/webadmin/state_variables.json')}</strong></div>
                        </div>
                      </div>
                      <aside class="wa-right-rail">
                        <article class="wa-panel"><h2>复制字段</h2><div class="wa-quick-grid">${waButton('复制变量键','copy',`onclick="copyTextToClipboard(${jsString(detail.key)})"`,'ghost')}${waButton('复制目标 ID','copy',`onclick="copyTextToClipboard(${jsString(detail.targetId||'')})"`,'ghost')}${waButton('复制路径','copy',`onclick="copyTextToClipboard(${jsString(detail.displayPath||'')})"`,'ghost')}</div></article>
                        <article class="wa-panel"><h2>条件配置提示</h2><div class="list-stack"><div class="kv-row"><span class="muted">条件类型</span><strong>${esc(condition.type||'')}</strong></div><div class="kv-row"><span class="muted">作用域</span><strong>${esc(condition.scope||detail.scope)}</strong></div><div class="kv-row"><span class="muted">目标模式</span><strong>${esc(condition.targetMode||'')}</strong></div><div class="kv-row"><span class="muted">目标 ID</span><strong>${esc(condition.targetId||'')}</strong></div></div></article>
                        <article class="wa-panel"><h2>编辑边界</h2><p class="wa-disabled-note">9.1 只支持受控创建 / 编辑已有字段，不支持删除、批量导入导出、GAME / TEAM scope 或 scoreboard migration runtime。</p><div class="wa-quick-grid">${editButton}</div></article>
                      </aside>
                    </section>
                  </section>`,options);
                }
                """)
.append("""
                function waDash(value){return isBlank(value)?'--':value;}
                function waCount(items,predicate){return (items||[]).filter(predicate).length;}
                function sumNumeric(items,keys){let found=false,total=0;(items||[]).forEach(item=>{for(const key of keys){const value=key.split('.').reduce((acc,part)=>acc&&acc[part],item);if(!isBlank(value)){const n=Number(value);if(Number.isFinite(n)){total+=n;found=true;}break;}}});return found?total:'--';}
                function firstKnown(item,keys){for(const key of keys){const value=key.split('.').reduce((acc,part)=>acc&&acc[part],item);if(!isBlank(value))return value;}return '';}
                function uniqueNonBlank(values){return [...new Set((values||[]).filter(v=>!isBlank(v)).map(v=>String(v)))].sort((a,b)=>a.localeCompare(b));}
                function countBy(items,selector){
                  const counts={};
                  (Array.isArray(items)?items:[]).forEach(item=>{
                    let key;
                    if(typeof selector==='function')key=selector(item);
                    else if(!isBlank(selector))key=String(selector).split('.').reduce((acc,part)=>acc==null?undefined:acc[part],item);
                    else key=item;
                    const normalized=isBlank(key)?'UNKNOWN':String(key);
                    counts[normalized]=(counts[normalized]||0)+1;
                  });
                  return counts;
                }
                function parseHashParams(hash){
                  const raw=String(hash||((typeof window!=='undefined'&&window.location)?window.location.hash:'')||'');
                  const index=raw.indexOf('?');
                  if(index<0&&!raw.startsWith('?'))return {};
                  const query=raw.startsWith('?')?raw.slice(1):raw.slice(index+1);
                  try{
                    const result={};
                    new URLSearchParams(query).forEach((value,key)=>{result[key]=value;});
                    return result;
                  }catch(_){
                    return {};
                  }
                }
                function endpointEnabledText(value){return value?'启用':'停用';}
                function deviceTypeIcon(type){return {SIGNAL_EMITTER:'signal-device',SIGNAL_RECEIVER:'signal-receiver',ACTION_RELAY:'action-relay',VIRTUAL_BLOCK_DEVICE:'virtual-block-device'}[String(type||'').toUpperCase()]||'device';}
                function deviceTypeTone(type){return {SIGNAL_EMITTER:'',SIGNAL_RECEIVER:'',ACTION_RELAY:'warning',VIRTUAL_BLOCK_DEVICE:'ok'}[String(type||'').toUpperCase()]||'';}
                function distributionItems(items,mapper,labeler,totalOverride){
                  const total=Number(totalOverride??items.length), counts=countBy(items,mapper);
                  return Object.entries(counts).sort((a,b)=>b[1]-a[1]).map(([key,value])=>({label:labeler?labeler(key):key,value,total,kind:key==='ERROR'||key==='FAILED'||key==='DISABLED'?'error':(key==='WARNING'?'warning':(key==='OK'||key==='ENABLED'?'ok':''))}));
                }

                async function renderSignalListenerDetail(rawId,options={}){
                  const routeInfo=detailRoute(rawId,'#/listeners'), listenerId=routeInfo.id||'';
                  if(!options.silent)setView(loading('正在加载监听器详情...'));
                  const result=await loadSignalListenerDetail(listenerId);
                  if(!result.ok){
                    if(options.silent){toast('监听器详情实时刷新失败，已保留当前页面。');return;}
                    setView(`<section class="wa-page"><div class="back-row">${backButton(routeInfo,'返回信号监听器')}</div>${waPageHead('监听器详情不可用','当前没有稳定 listener id 或频道详情数据可定位该监听器。',`${waButton('编辑监听器','settings','disabled','ghost')}${waButton('创建动作','plus','disabled','primary')}`)}${empty(result.message||'请返回列表，通过已有频道详情确认当前监听器。')}</section>`);
                    return;
                  }
                  const data=result.data, listener=data.listener||{}, detail=data.detail||{}, channel=listener.channel||data.channel||detail.channel||'', title=listener.name||listener.id||listenerId||'未命名监听器';
                  const [history,config,actionList]=await Promise.all([channel?settle(`/api/signals/history?channel=${encodeURIComponent(channel)}&limit=8`):Promise.resolve({ok:true,data:[]}),listener.id?settle(`/api/webadmin/signal-listener-basic-config/${encodeURIComponent(listener.id)}`):Promise.resolve({ok:false}),listener.id?settle(`/api/webadmin/signal-listeners/${encodeURIComponent(listener.id)}/actions`):Promise.resolve({ok:false})]);
                  appState.currentSignalListenerDetail={listener,channel,detail};
                  const actionInfo=actionList.ok?(actionList.data||{}):{listenerId:listener.id||listenerId,actions:listener.actions||[],actionCount:listener.actionCount||0,lockStatus:null,expectedFingerprint:''};
                  const actions=actionInfo.actions||listener.actions||[], recent=history.ok?(history.data||[]):[], cfg=config.ok?(config.data||{}):{};
                  const editAction=signalListenerBasicConfigEditAction(listener,channel,cfg,'primary');
                  const deleteAction=listener.id&&canDeleteSignalListener()?waButton('删除监听器','channel-error',htmlHandler(`openSignalListenerDeleteModal(${jsString(listener.id)},${jsString(channel)})`),'danger'):waButton('删除监听器','channel-error','disabled','danger');
                  const listenerStatus=listener.enabled!==false?'启用':'停用', actionCount=actionInfo.actionCount??listener.actionCount??actions.length, cooldown=listener.cooldownTicks??cfg.cooldownTicks;
                  const advancedRows=[
                    ['listener.id',listener.id||listenerId],
                    ['listener.name',title],
                    ['listener.channel',channel],
                    ['listener.enabled',listenerStatus],
                    ['listener.cooldownTicks',cooldown],
                    ['listener.actionCount',actionCount],
                    ['listener.lastTriggeredAt',formatDateTime(listener.lastTriggeredAt)],
                    ['config.fingerprint',cfg.fingerprint||cfg.expectedFingerprint]
                  ];
                  const rendered=setView(`<section class="wa-page wa-detail-shell" data-detail-kind="listener">
                    ${detailHeader({back:backButton(routeInfo,'返回信号监听器'),kicker:'信号监听器 / 监听器详情',iconName:'listener-receiver',title:title,subtitle:listener.description||`监听频道：${channel||'未绑定'}`,copyValue:listener.id||listenerId,badges:[`<span class="pill info">ID: ${esc(listener.id||listenerId||'无稳定 ID')}</span>`,pill(listener.enabled!==false?'OK':'WARNING')],actions:[listener.id?waButton('查看逻辑链','action-binding',navigationAttr(logicChainResolveHash('listener',listener.id)),'primary'):'',waButton('测试触发','play','disabled','ghost'),waButton('复制监听器','copy','disabled','ghost'),waButton('更多','more','disabled','ghost')].filter(Boolean)})}
                    ${detailTabs(['基本信息','动作列表','最近事件','触发统计','递归检查'])}
                    <section class="wa-detail-first-row">
                      ${detailCard('基本信息',detailInfoGrid([
                        ['监听器名称',title],
                        ['ID',listener.id||listenerId||'无稳定 ID'],
                        ['监听频道',safeHtml(channel?channelButton(channel):'<span class="muted">未绑定</span>')],
                        ['状态',safeHtml(pill(listener.enabled!==false?'OK':'WARNING')+' '+esc(listenerStatus))],
                        ['冷却时间',formatTicks(cooldown)||'0 tick'],
                        ['描述',listener.description||cfg.note||'暂无描述'],
                        ['创建/修改时间',cfg.updatedAt?`${formatDateTime(cfg.updatedAt)} · ${cfg.updatedBy||'未知用户'}`:'暂无']
                      ]),editAction)}
                      ${detailCard('状态与统计',`${detailStatGrid([
                        {label:'动作数量',value:actionCount,sub:'详情页摘要',icon:'action-total'},
                        {label:'今日触发次数',value:listener.triggerCountToday ?? 0,sub:'今日',icon:'today-trigger'},
                        {label:'最后触发',value:formatDateTime(listener.lastTriggeredAt),sub:'最近一次',icon:'recent-event'},
                        {label:'总触发次数',value:listener.totalTriggerCount ?? listener.triggerCountTotal ?? 0,sub:'累计',icon:'history'}
                      ])}<h3 class="wa-detail-subhead">消费者关系 / 关联对象</h3>${detailConsumerGrid([
                        {label:'来源频道',value:labelChannel(channel),icon:'active-channel',target:channel?signalHash(channel):''},
                        {label:'动作列表',value:actionCount,icon:'action-total'},
                        {label:'频道消费者',value:`${(detail.listeners||[]).length} / ${(detail.receivers||[]).length}`,icon:'consumer-listener',target:channel?signalHash(channel):''},
                        {label:'Doctor',value:labelStatus(listener.doctorStatus||detail.doctorStatus||'UNKNOWN'),icon:'doctor-overview',target:'#/doctor'}
                      ])}`)}
                    </section>
                    ${detailFixedLayout([
                      detailCard(`动作列表（共 ${actions.length} 个）`,signalListenerActionSummaryCard(actionInfo),'','detail-card-stretchable'),
                      detailCard('最近事件',listenerRecentEvents(recent,channel))
                    ],[
                      detailCard('冷却与条件 gate',`${detailInfoGrid([['启用状态',listenerStatus],['冷却时间',formatTicks(cooldown)||'0 tick'],['最后触发',safeHtml(fmtTime(listener.lastTriggeredAt))],['绑定频道',safeHtml(channel?channelButton(channel):'<span class="muted">未绑定</span>')]])}${conditionGateRecentStatusCard(cfg.recentConditionGate)}`,'','detail-card-stretchable'),
                      detailCard('操作工具',`<div class="wa-quick-grid">${editAction}${listener.id?waButton('查看逻辑链','action-binding',navigationAttr(logicChainResolveHash('listener',listener.id)),'ghost'):''}${canEditSignalListenerActions()?waButton('添加动作','plus',lockHeldByOther(actionInfo.lockStatus)?'disabled data-signal-listener-action-lock-disabled="true"':htmlHandler(`openSignalListenerActionAddModal(${jsString(actionInfo.listenerId||listener.id)},false)`),'primary'):waButton('添加动作','plus','disabled','ghost')}${deleteAction}${waButton('导出监听器','download','disabled','ghost')}</div><p class="wa-disabled-note">虚拟监听器删除使用危险确认窗口；动作列表编辑使用独立窗口，不提供 matcher / itemSubmit 或新条件组编辑入口。</p>`)
                    ],[
                      advancedDetailCard('listeners',listener.id||listenerId,advancedRows,[
                      {title:'监听器配置',rows:advancedRowsFromObject({listener,cfg},'listener')},
                      {title:'冷却状态与调试信息',rows:advancedRowsFromObject({lastTriggeredAt:listener.lastTriggeredAt,cooldownTicks:cooldown,doctorStatus:listener.doctorStatus||detail.doctorStatus,recent},'runtime')},
                      {title:'关联频道详情',rows:advancedRowsFromObject({channel,channelDetail:{stats:detail.stats,doctorIssues:detail.doctorIssues,listeners:detail.listeners,receivers:detail.receivers}},'channel')}
                    ])
                    ])}
                  </section>`,options);
                  if(rendered)renderIcons(appView());
                }
                async function loadSignalListenerDetail(listenerId){
                  if(isBlank(listenerId))return {ok:false,message:'缺少监听器 ID。'};
                  const cached=(appState.listeners||[]).find(l=>listenerMatches(l,listenerId));
                  if(cached){
                    const channel=cached.channel||cached.sourceChannel||'';
                    const detail=channel?await settle(`/api/signals/channels/${encodeURIComponent(channel)}`):{ok:false};
                    return {ok:true,data:{listener:cached,channel,detail:detail.ok?detail.data:{}}};
                  }
                  let channels;try{channels=await api('/api/signals/channels');}catch(err){return {ok:false,message:err.message};}
                  const candidates=(channels||[]).filter(c=>Number(c.listenerCount||0)>0);
                  const details=await Promise.allSettled(candidates.map(c=>api(`/api/signals/channels/${encodeURIComponent(c.channel||'')}`)));
                  for(let index=0;index<details.length;index++){
                    const result=details[index];if(result.status!=='fulfilled')continue;
                    const detail=result.value||{}, channel=detail.channel||candidates[index]?.channel||'';
                    const found=(detail.listeners||[]).find(l=>listenerMatches(l,listenerId));
                    if(found)return {ok:true,data:{listener:{...found,channel:found.channel||channel,sourceChannel:channel,sourceDisplayName:detail.metadata?.effectiveDisplayName||channel},channel,detail}};
                  }
                  return {ok:false,message:'未能在现有频道详情数据中找到该监听器。'};
                }
                function listenerMatches(listener,id){const target=String(id||'');return [listener?.id,listener?.name,listener?.ref,listener?.listenerRef].filter(v=>!isBlank(v)).some(v=>String(v)===target);}
                function listenerInfoRows(listener,channel,cfg){const rows=[['监听器 ID',listener.id||'--'],['显示名称',listener.name||'--'],['频道',channel||'--'],['启用',listener.enabled!==false?'启用':'停用'],['冷却时间',formatTicks(listener.cooldownTicks??cfg.cooldownTicks)||'0 tick'],['来源频道',listener.sourceDisplayName||listener.sourceChannel||channel||'--'],['配置 fingerprint',cfg.fingerprint||cfg.expectedFingerprint||'--']];return rows.map(([k,v])=>`<div class="kv-row"><span class="muted">${esc(k)}</span><strong>${esc(v)}</strong></div>`).join('');}
                function listenerActionList(actions){if(!actions||actions.length===0)return empty('当前监听器没有可展示的动作。');return `<div class="wa-table-scroll" data-table-row-stretch="false"><table class="wa-table"><thead><tr><th>动作</th><th>类型</th><th>摘要</th><th>状态</th></tr></thead><tbody>${actions.map(a=>`<tr><td>${a.id?actionButton(a.id,a.name||a.id):esc(a.name||a.id||'未命名动作')}</td><td>${textPill(labelActionType(a.type),'info')}</td><td class="truncate" title="${esc(cleanActionSummary(a.summary||''))}">${esc(cleanActionSummary(a.summary||'--'))}</td><td>${pill(a.doctorStatus||'UNKNOWN')}</td></tr>`).join('')}</tbody></table></div>`;}
                function signalListenerBasicConfigEditAction(listener,channel,cfg,kind='primary'){
                  const listenerId=listener?.id||listener?.listenerId||'', lock=cfg?.lockStatus||null, canEdit=canEditSignalListenerBasicConfig(), currentLock=lock&&lock.locked&&lock.heldByCurrentUser;
                  if(!listenerId||!canEdit)return waButton('编辑基本信息','settings','disabled','ghost');
                  if(lockHeldByOther(lock)){
                    const msg=lockMessage(lock,'虚拟监听器基础配置');
                    return `${waButton('编辑基本信息','settings',`disabled title="${esc(msg)}" data-signal-listener-basic-lock-disabled="true"`,'ghost is-locked')}<span class="wa-lock-badge" data-signal-listener-basic-lock-badge="true">${esc(msg)}</span>`;
                  }
                  const button=waButton('编辑基本信息','settings',htmlHandler(`startSignalListenerBasicConfigEdit(${jsString(listenerId)},${jsString(channel)})`),kind);
                  return currentLock?`${button}<span class="wa-lock-badge" data-signal-listener-basic-lock-current="true">正在编辑 · 锁到期：${esc(formatDateTime(lock.expiresAt))}</span>`:button;
                }
                function listenerRecentEventKey(h){return [h?.id,h?.time,h?.channel,h?.sourceType,h?.sourceId,h?.result,h?.description].filter(v=>!isBlank(v)).join('|');}
                function listenerRecentEvents(items,channel){
                  const seen=new Set(), source=Array.isArray(items)?items:[];
                  const recent=[];
                  for(const h of [...source].reverse()){
                    const key=listenerRecentEventKey(h);
                    if(key&&seen.has(key))continue;
                    if(key)seen.add(key);
                    recent.push(h);
                    if(recent.length>=3)break;
                  }
                  if(recent.length===0)return empty('暂无最近事件');
                  return `<div class="list-stack" data-signal-listener-recent-events-max3="true" data-signal-listener-recent-events-deduped="true">${recent.map(h=>`<div class="event-row"><strong>${esc(labelChannel(h.channel||channel))}</strong><span class="meta">${fmtTime(h.time)} · ${esc(labelSourceType(h.sourceType))} · ${esc(labelStatus(h.result))}</span><span>${esc(h.description||h.sourceName||'暂无详情')}</span></div>`).join('')}</div>`;
                }
                async function renderListeners(options={}){
                  if(!options.silent)setView(loading('正在加载信号监听器...'));
                  let channels=[];
                  try{channels=await api('/api/signals/channels');}
                  catch(err){if(options.silent){toast('信号监听器实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  const listenerChannels=(channels||[]).filter(c=>Number(c.listenerCount||0)>0).slice(0,50);
                  const details=await Promise.allSettled(listenerChannels.map(c=>api(`/api/signals/channels/${encodeURIComponent(c.channel||'')}`)));
                  const listeners=[];
                  details.forEach((result,index)=>{
                    if(result.status!=='fulfilled')return;
                    const detail=result.value||{}, channel=detail.channel||listenerChannels[index]?.channel||'';
                    (detail.listeners||[]).forEach(item=>listeners.push({...item,channel:item.channel||channel,sourceChannel:channel,sourceDisplayName:detail.metadata?.effectiveDisplayName||detail.channel||channel}));
                  });
                  appState.listenerSourceChannels=channels||[];
                  appState.listeners=listeners;
                  appState.listenerLoadLimited=(channels||[]).filter(c=>Number(c.listenerCount||0)>0).length>listenerChannels.length;
                  renderListenerList('',options);
                }
                function renderListenerList(focusId,options={}){
                  waEnsureState();
                  const listeners=appState.listeners||[], channels=appState.listenerSourceChannels||[], filtered=filterListeners(listeners), page=waPageItems('listeners',filtered,10);
                  const enabled=waCount(listeners,l=>l.enabled!==false), bound=uniqueNonBlank(listeners.map(l=>l.channel)).length, today=sumNumeric(channels,['triggerCountToday']);
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('信号监听器','后台虚拟监听器用于监听 channel 并执行 actions，不等同于 signal_receiver 方块。',`${canCreateSignalListener()?waButton('新建监听器','plus',htmlHandler('openSignalListenerCreateModal()'),'primary'):waButton('新建监听器','plus','disabled','primary')}${waButton('批量导入','upload','disabled','ghost')}${waButton('导出配置','download','disabled','ghost')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('监听器总数',listeners.length,appState.listenerLoadLimited?'仅加载前 50 个有监听器频道':'来自频道详情 API','consumer-listener')}
                      ${waMetric('启用监听器',enabled,'enabled=true','enabled','ok')}
                      ${waMetric('停用监听器',listeners.length-enabled,'enabled=false','receiver-disabled',listeners.length-enabled?'warning':'')}
                      ${waMetric('绑定频道总数',bound,'去重频道数量','active-channel')}
                      ${waMetric('今日触发次数',today,'来自频道统计','today-trigger')}
                    </section>
                    <section class="wa-table-card">
                      <div class="wa-filter-bar">
                        <label class="filter-field search-control"><span>搜索</span><input class="input" id="listener-search" placeholder="搜索监听器名称 / ID / 频道..." value="${esc(appState.listenerFilters.search)}"></label>
                        <label class="filter-field"><span>状态</span>${waSelect('listener-enabled',['ALL','ENABLED','DISABLED'],appState.listenerFilters.enabled,optionLabel)}</label>
                        <label class="filter-field"><span>频道</span>${waSelect('listener-channel',['ALL',...uniqueNonBlank(listeners.map(l=>l.channel))],appState.listenerFilters.channel,v=>v==='ALL'?'全部频道':v)}</label>
                        ${waButton('刷新','refresh','onclick="renderListeners()"','ghost')}
                      </div>
                      ${page.items.length===0?empty(listeners.length===0?'当前没有可从频道详情 API 读取到的信号监听器。':'没有匹配当前筛选条件的监听器。'):listenerTable(page.items)}
                      ${waPagination('listeners',page)}
                    </section>
                  </section>`,options);
                  if(rendered)bindListenerFilters(focusId);
                }
                function listenerTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>监听器名称 / ID</th><th>频道</th><th>状态</th><th>冷却时间</th><th>动作数量</th><th>最后触发</th><th>操作</th></tr></thead><tbody>${items.map(l=>{const title=l.name||l.id||'未命名监听器', channel=l.channel||l.sourceChannel||'', channelTarget=isBlank(channel)?'':signalHash(channel), detailTarget=isBlank(l.id)?'':listenerHash(l.id), deleteAttrs=l.id&&canDeleteSignalListener()?htmlHandler(`event.stopPropagation();openSignalListenerDeleteModal(${jsString(l.id)},${jsString(channel)})`):'disabled';return `<tr class="${detailTarget?'wa-clickable-row':''}" ${detailTarget?navDataAttr(detailTarget,`查看监听器 ${title}`):'aria-disabled="true"'}><td><span class="device-name"><span class="device-icon">${icon('consumer-listener')}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(l.id))}</span></span></span></td><td class="truncate" title="${esc(channel)}">${channelCell(channel)}</td><td>${pill(l.enabled!==false?'OK':'WARNING')} ${esc(endpointEnabledText(l.enabled!==false))}</td><td>${esc(formatTicks(l.cooldownTicks)||'0 tick')}</td><td>${esc(l.actionCount ?? '--')}</td><td>${fmtTime(l.lastTriggeredAt)}</td><td><div class="wa-action-cell">${detailTarget?`<button class="wa-btn ghost" ${navDataAttr(detailTarget,`查看监听器 ${title}`)}>详情</button>`:`<button class="wa-btn ghost" disabled>详情</button>`}${channelTarget?`<button class="wa-btn ghost" ${navDataAttr(channelTarget,`查看频道 ${channel}`)}>频道</button>`:`<button class="wa-btn ghost" disabled>频道</button>`}<button class="wa-btn danger" ${deleteAttrs}>删除</button></div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function filterListeners(items){
                  const f=appState.listenerFilters||{};
                  return (items||[]).filter(l=>{const hay=[l.id,l.name,l.channel,l.sourceDisplayName].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.enabled==='ENABLED'&&l.enabled===false)return false;if(f.enabled==='DISABLED'&&l.enabled!==false)return false;if(f.channel&&f.channel!=='ALL'&&String(l.channel||'')!==f.channel)return false;return true;});
                }
                function bindListenerFilters(focusId){
                  const update=(event)=>{appState.listenerFilters.search=document.getElementById('listener-search')?.value||'';appState.listenerFilters.enabled=document.getElementById('listener-enabled')?.value||'ALL';appState.listenerFilters.channel=document.getElementById('listener-channel')?.value||'ALL';appState.uiPages.listeners=1;renderListenerList(event?.target?.id||'');};
                  ['listener-search','listener-enabled','listener-channel'].forEach(id=>document.getElementById(id)?.addEventListener(id==='listener-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                async function renderDevices(options={}){
                  if(!options.silent)setView(loading('正在加载信号设备...'));
                  let devices;try{devices=await api('/api/devices')}catch(err){if(options.silent){toast('信号设备实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.devices=devices||[];
                  renderDeviceList('',options);
                }
                function renderDeviceList(focusId,options={}){
                  waEnsureState();
                  const devices=appState.devices||[], filtered=filterDevices(devices), page=waPageItems('devices',filtered,10), worlds=uniqueNonBlank(devices.map(d=>d.world));
                  const enabled=waCount(devices,d=>d.enabled), today=sumNumeric(devices,['triggerCountToday','todayTriggerCount']), recent=devices.filter(d=>!isBlank(d.lastTriggeredAt)).sort((a,b)=>String(b.lastTriggeredAt||'').localeCompare(String(a.lastTriggeredAt||'')))[0];
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('信号设备','统一查看 signal_emitter、signal_receiver、action_relay 与 virtual_block_device。',`${waButton('添加设备','plus','disabled','primary')}${waButton('批量导入','upload','disabled','ghost')}${waButton('导出设备','download','disabled','danger')}`)}
                    <section class="wa-card-grid">
                      ${waMetric('设备总数',devices.length,'来自 /api/devices','device-overview')}
                      ${waMetric('启用中',enabled,'enabled=true','enabled','ok')}
                      ${waMetric('禁用中',devices.length-enabled,'enabled=false','receiver-disabled',devices.length-enabled?'warning':'')}
                      ${waMetric('今日触发次数',today,'API 未提供时显示 --','today-trigger')}
                      ${waMetric('最近触发设备',recent?.displayName||'--',recent?formatRelativeTime(recent.lastTriggeredAt):'暂无','recent-event')}
                      ${waMetric('未绑定频道',waCount(devices,d=>isBlank(d.channel)),'channel 为空','channel-orphan','warning')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="device-search" placeholder="搜索设备名称 / ID / channel / 坐标..." value="${esc(appState.deviceFilters.search)}"></label>
                          <label class="filter-field"><span>类型</span>${waSelect('device-type',['ALL','SIGNAL_EMITTER','SIGNAL_RECEIVER','ACTION_RELAY','VIRTUAL_BLOCK_DEVICE','UNKNOWN'],appState.deviceFilters.type,optionLabel)}</label>
                          <label class="filter-field"><span>状态</span>${waSelect('device-enabled',['ALL','ENABLED','DISABLED'],appState.deviceFilters.enabled,optionLabel)}</label>
                          <label class="filter-field"><span>Doctor</span>${waSelect('device-doctor',['ALL','OK','INFO','WARNING','ERROR','UNKNOWN'],appState.deviceFilters.doctor,optionLabel)}</label>
                          <label class="filter-field"><span>世界</span>${waSelect('device-world',['ALL',...worlds],appState.deviceFilters.world,v=>v==='ALL'?'全部世界':v)}</label>
                          ${waButton('刷新','refresh','onclick="renderDevices()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(devices.length===0?'当前暂无设备数据。':'没有匹配当前筛选条件的设备。'):deviceTable(page.items)}
                        ${waPagination('devices',page)}
                      </div>
                      ${deviceRightRail(devices)}
                    </section>
                  </section>`,options);
                  if(rendered)bindDeviceFilters(focusId);
                }
                """)
.append("""
                function ensurePopoverRoot(){let root=document.getElementById('wa-popover-root');if(!root){root=document.createElement('div');root.id='wa-popover-root';document.body.appendChild(root);}return root;}
                function cssEscape(value){return (typeof CSS!=='undefined'&&CSS.escape)?CSS.escape(String(value)):String(value).replace(/["\\\\]/g,'\\\\$&');}
                function deviceForMoreMenu(deviceId){return (appState.devices||[]).find(d=>String(d.id||'')===String(deviceId))||((appState.currentDeviceDetail&&String(appState.currentDeviceDetail.id||'')===String(deviceId))?appState.currentDeviceDetail:null);}
                function toggleDeviceMoreMenu(deviceId,event){if(event){event.preventDefault();event.stopPropagation();}if(appState.openDeviceMoreMenuId===deviceId){closeDeviceMoreMenu(false);return;}openDeviceMorePopover(deviceId,event?.currentTarget||document.querySelector(`[data-device-more-trigger="${cssEscape(deviceId)}"]`));}
                function closeDeviceMoreMenu(updateButton=true){appState.openDeviceMoreMenuId='';appState.deviceMorePopover=null;const root=document.getElementById('wa-popover-root');if(root)root.innerHTML='';document.querySelectorAll('[data-device-more-trigger]').forEach(btn=>btn.setAttribute('aria-expanded','false'));}
                function openDeviceMorePopover(deviceId,anchor){const device=deviceForMoreMenu(deviceId);if(!device||!anchor)return;appState.openDeviceMoreMenuId=String(deviceId);appState.deviceMorePopover={deviceId:String(deviceId)};document.querySelectorAll('[data-device-more-trigger]').forEach(btn=>btn.setAttribute('aria-expanded',String(btn===anchor)));const root=ensurePopoverRoot();root.innerHTML=`<div id="wa-device-more-popover" class="wa-menu-pop wa-device-more-popover" data-floating-popover="device-more" data-table-popover-portal="true" role="menu" onclick="event.stopPropagation()">${deviceMoreMenuItems(device)}</div>`;positionDeviceMorePopover(anchor);}
                function positionDeviceMorePopover(anchor){const pop=document.getElementById('wa-device-more-popover');if(!pop||!anchor)return;const rect=anchor.getBoundingClientRect(), margin=8, width=Math.min(280,Math.max(240,window.innerWidth-margin*2));pop.style.width=`${width}px`;pop.style.left='0px';pop.style.top='0px';const height=pop.offsetHeight||240;let left=Math.min(window.innerWidth-width-margin,Math.max(margin,rect.right-width));let top=rect.bottom+6;if(top+height>window.innerHeight-margin)top=Math.max(margin,rect.top-height-6);pop.style.left=`${left}px`;pop.style.top=`${top}px`;}
                function deviceMoreMenuItems(d){
                  const id=String(d.id||''), target=deviceHash(d);
                  const actionLock=actionRelayLockForDevice(d), lockedByOther=actionRelayLockHeldByOther(actionLock);
                  const actionRelayItem=isActionRelay(d)?(lockedByOther?`<button class="wa-menu-item" type="button" data-action-relay-more-menu-entry="true" onclick='event.stopPropagation();closeDeviceMoreMenu(false);openActionRelayActionsReadonlyModal(${jsString(id)})'>${icon('action-relay')}<span>只读查看 Action 列表</span></button><div class="wa-menu-note">${esc(actionRelayLockMessage(actionLock))}</div>`:`<button class="wa-menu-item" type="button" data-action-relay-more-menu-entry="true" onclick='event.stopPropagation();closeDeviceMoreMenu(false);openActionRelayActionsModal(${jsString(id)})'>${icon('action-relay')}<span>${canEditActionRelayActions()?'编辑 Action 列表':'查看 Action 列表'}</span></button>`):'';
                  const deleteItem=isVirtualBlockDevice(d)?(canDeleteVirtualBlockDevice()?`<button class="wa-menu-item danger" type="button" onclick='event.stopPropagation();closeDeviceMoreMenu(false);openVirtualBlockDeviceDeleteModal(${jsString(id)})'>${icon('channel-error')}<span>删除 / 解绑 VBD</span></button>`:`<div class="wa-menu-note">需要 EDITOR / OWNER 权限才能删除 / 解绑 VBD。</div>`):`<div class="wa-menu-note" data-physical-device-delete-disabled="true">物理设备不提供 WebUI 删除；需要删除时请在游戏内破坏方块。</div>`;
                  const canEditConfig=canEditDeviceConfig(d), configLockMessage=deviceConfigLockMessage(d);
                  const editConfigItem=canEditConfig&&!configLockMessage?`<button class="wa-menu-item" type="button" data-device-config-more-menu-entry="true" onclick='event.stopPropagation();closeDeviceMoreMenu(false);startDeviceConfigEdit(${jsString(id)})'>${icon('settings')}<span>编辑设备配置</span></button>`:`<button class="wa-menu-item" type="button" data-device-config-more-menu-entry="true" data-device-config-lock-disabled="${configLockMessage?'true':'false'}" disabled>${icon('settings')}<span>编辑设备配置</span></button><div class="wa-menu-note">${esc(configLockMessage||'需要 EDITOR / OWNER 权限才能编辑设备配置。')}</div>`;
                  return `<button class="wa-menu-item" type="button" onclick='event.stopPropagation();closeDeviceMoreMenu(false);location.hash=${jsString(target)}'>${icon('eye')}<span>查看详情</span></button>${editConfigItem}${actionRelayItem}${d.channel?`<button class="wa-menu-item" type="button" onclick='event.stopPropagation();closeDeviceMoreMenu(false);location.hash=${jsString(signalHash(d.channel))}'>${icon('active-channel')}<span>打开频道</span></button>`:''}<button class="wa-menu-item" type="button" onclick='event.stopPropagation();copyTextToClipboard(${jsString(id)});closeDeviceMoreMenu(false)'>${icon('copy')}<span>复制 ID</span></button>${deleteItem}`;
                }
                function deviceMoreMenu(d){const id=String(d.id||''), open=appState.openDeviceMoreMenuId===id;return `<div class="wa-menu-wrap" data-device-more-menu="${esc(id)}" onclick="event.stopPropagation()"><button class="wa-icon-btn" type="button" title="更多" aria-label="更多" data-device-more-trigger="${esc(id)}" aria-expanded="${open?'true':'false'}" onclick='toggleDeviceMoreMenu(${jsString(id)},event)'>${icon('more')}</button></div>`;}
                function deviceTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>设备名称 / ID</th><th>类型</th><th>频道</th><th>位置</th><th>状态</th><th>最近触发</th><th>Doctor</th><th>操作</th></tr></thead><tbody>${items.map(d=>{const target=deviceHash(d), title=d.displayName||d.id;return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看设备 ${title}`)}><td><span class="device-name"><span class="device-icon">${icon(deviceTypeIcon(d.type))}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(d.id))}</span></span></span></td><td>${textPill(labelType(d.type),deviceTypeTone(d.type)||'info')}</td><td class="truncate" title="${esc(d.channel||'')}">${channelCell(d.channel)}</td><td class="truncate" title="${esc((d.world||'')+' '+posText(d.pos))}">${esc(d.world||'-')} / ${esc(posText(d.pos))}</td><td>${pill(d.enabled?'OK':'WARNING')} ${esc(labelBool(d.enabled))}</td><td>${fmtTime(d.lastTriggeredAt)}</td><td>${pill(d.doctorStatus)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看设备 ${title}`)}>查看</button>${deviceMoreMenu(d)}</div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function filterDevices(items){const f=appState.deviceFilters||{};return (items||[]).filter(d=>{const hay=[d.id,d.displayName,d.channel,d.world,posText(d.pos),d.type,d.doctorStatus].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.type!=='ALL'&&String(d.type||'UNKNOWN').toUpperCase()!==f.type)return false;if(f.enabled==='ENABLED'&&!d.enabled)return false;if(f.enabled==='DISABLED'&&d.enabled)return false;if(f.doctor!=='ALL'&&String(d.doctorStatus||'UNKNOWN').toUpperCase()!==f.doctor)return false;if(f.world!=='ALL'&&d.world!==f.world)return false;return true;});}
                function bindDeviceFilters(focusId){
                  const update=(event)=>{appState.deviceFilters.search=document.getElementById('device-search')?.value||'';appState.deviceFilters.type=document.getElementById('device-type')?.value||'ALL';appState.deviceFilters.enabled=document.getElementById('device-enabled')?.value||'ALL';appState.deviceFilters.doctor=document.getElementById('device-doctor')?.value||'ALL';appState.deviceFilters.world=document.getElementById('device-world')?.value||'ALL';appState.uiPages.devices=1;renderDeviceList(event?.target?.id||'');};
                  ['device-search','device-type','device-enabled','device-doctor','device-world'].forEach(id=>document.getElementById(id)?.addEventListener(id==='device-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                function deviceRightRail(devices){
                  const enabled=waCount(devices,d=>d.enabled), total=devices.length;
                  return `<aside class="wa-right-rail">
                    <article class="wa-panel"><h2>设备类型分布</h2>${progressList(distributionItems(devices,d=>String(d.type||'UNKNOWN').toUpperCase(),labelType,total))}</article>
                    <article class="wa-panel"><h2>状态分布</h2>${progressList([{label:'启用中',value:enabled,total,kind:'ok'},{label:'禁用中',value:total-enabled,total,kind:'warning'}])}</article>
                    <article class="wa-panel"><h2>快速筛选</h2><div class="wa-quick-grid">${['SIGNAL_EMITTER','SIGNAL_RECEIVER','ACTION_RELAY','VIRTUAL_BLOCK_DEVICE'].map(type=>`<button class="wa-btn ghost" onclick="appState.deviceFilters.type='${type}';appState.uiPages.devices=1;renderDeviceList()">${esc(labelType(type))}</button>`).join('')}<button class="wa-btn ghost" onclick="appState.deviceFilters={search:'',type:'ALL',enabled:'ALL',doctor:'ALL',world:'ALL'};appState.uiPages.devices=1;renderDeviceList()">重置筛选</button></div></article>
                    <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('批量启用','enabled','disabled','ghost')}${waButton('批量禁用','receiver-disabled','disabled','ghost')}${waButton('测试设备','signal-device','disabled','ghost')}${waButton('清空统计','channel-error','disabled','danger')}</div><p class="wa-disabled-note">写操作需要后端权限、CSRF、审计和 edit lock 支持，本轮保持禁用。</p></article>
                  </aside>`;
                }
                function isVirtualBlockDevice(d){return String(d.type||'').toUpperCase()==='VIRTUAL_BLOCK_DEVICE'||String(d.subType||'').toLowerCase()==='virtual_block_device';}
                async function renderVirtualBlockDevices(options={}){
                  if(!options.silent)setView(loading('正在加载虚拟方块设备...'));
                  let devices;try{devices=await api('/api/devices')}catch(err){if(options.silent){toast('虚拟方块设备实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.virtualBlockDevices=(devices||[]).filter(isVirtualBlockDevice);
                  renderVirtualBlockList('',options);
                }
                function virtualDetail(d){const cache=(appState.virtualBlockDetailCache||{})[String(d?.id||'')];return cache&&cache.status==='ok'?cache.detail:null;}
                function virtualConfig(d){return virtualDetail(d)?.configSummary||d.configSummary||{};}
                function virtualBlockId(d){return firstKnown(virtualConfig(d),['blockId','block','minecraftBlockId','boundBlockId']);}
                function virtualTriggerType(d){const cfg=virtualConfig(d);if(!cfg||Object.keys(cfg).length===0)return 'unknown';if(cfg.containerEnabled)return 'container';if(cfg.interactionEnabled)return 'interact';if(cfg.conditionEnabled)return 'blockstate';return firstKnown(cfg,['mode','triggerType'])||'unknown';}
                function labelTriggerType(value){const v=String(value||'unknown').toLowerCase();return {redstone_rising:'红石上升沿',redstone_falling:'红石下降沿',redstone_both:'红石双沿',redstone:'红石触发',blockstate:'方块状态',interact:'玩家交互',container:'容器事件',unknown:'未知'}[v]||value;}
                function virtualConditionText(d){const cfg=virtualConfig(d), parts=[];if(cfg.conditionEnabled)parts.push('条件启用');if(Number(cfg.itemConditionCount||0)>0)parts.push(`物品条件 ${cfg.itemConditionCount}`);if(cfg.interactionItem?.enabled)parts.push('交互物品匹配');if(Number(cfg.itemSubmitRequirementCount||0)>0)parts.push(`提交要求 ${cfg.itemSubmitRequirementCount}`);return parts.length?parts.join(' / '):'--';}
                async function refreshVisibleVirtualBlockDetails(items,options={}){
                  waEnsureState();if(document.hidden)return;
                  const force=!!options.force, now=Date.now(), jobs=[];
                  (items||[]).filter(d=>isVirtualBlockDevice(d)&&!isBlank(d.id)).slice(0,10).forEach(d=>{
                    const id=String(d.id), cache=appState.virtualBlockDetailCache[id];
                    if(cache&&cache.status==='loading')return;
                    if(cache&&cache.status==='ok'&&!force)return;
                    if(cache&&cache.status==='error'&&!force&&now-Number(cache.at||0)<5000)return;
                    appState.virtualBlockDetailCache[id]=cache&&cache.status==='ok'?{...cache,refreshing:true}:{status:'loading',at:now};
                    jobs.push((async()=>{try{const detail=await api(`/api/devices/${encodeURIComponent(id)}`);appState.virtualBlockDetailCache[id]={status:'ok',detail,at:Date.now(),refreshing:false};return true;}catch(err){const latest=appState.virtualBlockDetailCache[id];if(latest&&latest.status==='ok'){appState.virtualBlockDetailCache[id]={...latest,refreshing:false,errorAt:Date.now(),message:err.message||'详情加载失败'};return false;}appState.virtualBlockDetailCache[id]={status:'error',message:err.message||'详情加载失败',at:Date.now()};return true;}})());
                  });
                  if(jobs.length===0)return;
                  const results=await Promise.all(jobs);
                  if(results.some(Boolean)&&['#/virtual-block-devices','#/block-devices'].includes(currentRouteHash()))renderVirtualBlockList('',{silent:true,skipDetailRefresh:true});
                }
                function renderVirtualBlockList(focusId,options={}){
                  waEnsureState();
                  const devices=appState.virtualBlockDevices||[], filtered=filterVirtualBlocks(devices), page=waPageItems('virtualBlockDevices',filtered,10), worlds=uniqueNonBlank(devices.map(d=>d.world));
                  const enabled=waCount(devices,d=>d.enabled), today=sumNumeric(devices,['triggerCountToday','todayTriggerCount']);
                  const conditions=devices.some(d=>!isBlank(virtualConditionText(d))&&virtualConditionText(d)!=='--')?waCount(devices,d=>virtualConditionText(d)!=='--'):'--';
                  const interact=devices.some(d=>!isBlank(virtualConfig(d).interactionEnabled))?waCount(devices,d=>!!virtualConfig(d).interactionEnabled):'--';
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('虚拟方块设备','绑定已有 Minecraft 方块作为虚拟事件源；方块材质不使用 WebAdmin 图标伪造。',`${canStartObjectSelection()?waButton('新建虚拟方块设备','plus',htmlHandler('openCreateVirtualBlockDeviceModal()'),'primary'):waButton('新建虚拟方块设备','plus','disabled','primary')}${waButton('批量导入','upload','disabled','ghost')}${waButton('导出配置','download','disabled','danger')}`)}
                    <section class="wa-card-grid">
                      ${waMetric('虚拟设备总数',devices.length,'type=virtual_block_device','virtual-block-device')}
                      ${waMetric('启用中',enabled,'enabled=true','enabled','ok')}
                      ${waMetric('禁用中',devices.length-enabled,'enabled=false','receiver-disabled',devices.length-enabled?'warning':'')}
                      ${waMetric('今日触发次数',today,'API 未提供时显示 --','today-trigger')}
                      ${waMetric('已配置条件',conditions,'来自可见详情缓存','condition-group')}
                      ${waMetric('交互触发设备',interact,'来自可见详情缓存','pulse-duration')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="virtual-search" placeholder="搜索设备名称 / ID / 方块 / channel..." value="${esc(appState.virtualBlockFilters.search)}"></label>
                          <label class="filter-field"><span>状态</span>${waSelect('virtual-enabled',['ALL','ENABLED','DISABLED'],appState.virtualBlockFilters.enabled,optionLabel)}</label>
                          <label class="filter-field"><span>触发类型</span>${waSelect('virtual-trigger',['ALL','redstone','blockstate','interact','container'],appState.virtualBlockFilters.trigger,v=>v==='ALL'?'全部触发类型':labelTriggerType(v))}</label>
                          <label class="filter-field"><span>世界</span>${waSelect('virtual-world',['ALL',...worlds],appState.virtualBlockFilters.world,v=>v==='ALL'?'全部世界':v)}</label>
                          ${waButton('刷新','refresh','onclick="renderVirtualBlockDevices()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(devices.length===0?'当前暂无虚拟方块设备。':'没有匹配当前筛选条件的虚拟方块设备。'):virtualBlockTable(page.items)}
                        ${waPagination('virtualBlockDevices',page)}
                      </div>
                      ${virtualBlockRightRail(devices)}
                    </section>
                  </section>`,options);
                  if(rendered){bindVirtualBlockFilters(focusId);if(!options.skipDetailRefresh)refreshVisibleVirtualBlockDetails(page.items,{force:!!options.silent});}
                }
                function virtualBlockTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>设备名称 / ID</th><th>绑定方块</th><th>位置</th><th>触发类型</th><th>通道</th><th>状态</th><th>条件配置</th><th>最近触发</th><th>操作</th></tr></thead><tbody>${items.map(d=>{const target=deviceHash(d), title=d.displayName||d.id, block=virtualBlockId(d), trigger=virtualTriggerType(d), deleteAttrs=canDeleteVirtualBlockDevice()?htmlHandler(`event.stopPropagation();openVirtualBlockDeviceDeleteModal(${jsString(d.id)})`):'disabled';return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看虚拟方块设备 ${title}`)}><td><span class="device-name"><span class="device-icon">${icon('virtual-block-device')}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(d.id))}</span></span></span></td><td><span>${esc(block||'--')}</span><span class="device-subtitle">原版材质未在列表 API 中提供时仅显示文本</span></td><td class="truncate" title="${esc((d.world||'')+' '+posText(d.pos))}">${esc(d.world||'-')} / ${esc(posText(d.pos))}</td><td>${textPill(labelTriggerType(trigger),trigger==='container'?'warning':(trigger==='interact'?'ok':'info'))}</td><td class="truncate" title="${esc(d.channel||'')}">${channelCell(d.channel)}</td><td>${pill(d.enabled?'OK':'WARNING')} ${esc(labelBool(d.enabled))}</td><td>${esc(virtualConditionText(d))}</td><td>${fmtTime(d.lastTriggeredAt)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看虚拟方块设备 ${title}`)}>查看</button><button class="wa-btn danger" ${deleteAttrs}>解绑</button></div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function filterVirtualBlocks(items){
                  const f=appState.virtualBlockFilters||{};
                  return (items||[]).filter(d=>{const cfg=virtualConfig(d), trigger=virtualTriggerType(d), hay=[d.id,d.displayName,d.channel,d.world,posText(d.pos),virtualBlockId(d),trigger,virtualConditionText(d)].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.enabled==='ENABLED'&&!d.enabled)return false;if(f.enabled==='DISABLED'&&d.enabled)return false;if(f.trigger!=='ALL'&&String(trigger).toLowerCase()!==String(f.trigger).toLowerCase()&&!(f.trigger==='redstone'&&String(trigger).startsWith('redstone')))return false;if(f.world!=='ALL'&&d.world!==f.world)return false;if(f.doctor!=='ALL'&&String(d.doctorStatus||'UNKNOWN').toUpperCase()!==f.doctor)return false;return true;});
                }
                function bindVirtualBlockFilters(focusId){
                  const update=(event)=>{appState.virtualBlockFilters.search=document.getElementById('virtual-search')?.value||'';appState.virtualBlockFilters.enabled=document.getElementById('virtual-enabled')?.value||'ALL';appState.virtualBlockFilters.trigger=document.getElementById('virtual-trigger')?.value||'ALL';appState.virtualBlockFilters.world=document.getElementById('virtual-world')?.value||'ALL';appState.uiPages.virtualBlockDevices=1;renderVirtualBlockList(event?.target?.id||'');};
                  ['virtual-search','virtual-enabled','virtual-trigger','virtual-world'].forEach(id=>document.getElementById(id)?.addEventListener(id==='virtual-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                function virtualBlockRightRail(devices){
                  const total=devices.length, triggerItems=distributionItems(devices,d=>String(virtualTriggerType(d)||'unknown').toLowerCase(),labelTriggerType,total);
                  return `<aside class="wa-right-rail">
                    <article class="wa-panel"><h2>触发类型分布</h2>${progressList(triggerItems)}</article>
                    <article class="wa-panel"><h2>快速筛选</h2><div class="wa-rail-filter"><label><span>所在世界</span>${waSelect('virtual-rail-world',['ALL',...uniqueNonBlank(devices.map(d=>d.world))],appState.virtualBlockFilters.world,v=>v==='ALL'?'全部世界':v)}</label><label><span>触发类型</span>${waSelect('virtual-rail-trigger',['ALL','redstone','blockstate','interact','container'],appState.virtualBlockFilters.trigger,v=>v==='ALL'?'全部触发类型':labelTriggerType(v))}</label><div class="wa-button-row"><button class="wa-btn primary" onclick="appState.virtualBlockFilters.world=document.getElementById('virtual-rail-world').value;appState.virtualBlockFilters.trigger=document.getElementById('virtual-rail-trigger').value;appState.uiPages.virtualBlockDevices=1;renderVirtualBlockList()">应用筛选</button><button class="wa-btn ghost" onclick="appState.virtualBlockFilters={search:'',enabled:'ALL',trigger:'ALL',world:'ALL',doctor:'ALL'};appState.uiPages.virtualBlockDevices=1;renderVirtualBlockList()">重置筛选</button></div></div></article>
                    <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('批量启用','enabled','disabled','ghost')}${waButton('批量禁用','receiver-disabled','disabled','ghost')}${waButton('测试触发','pulse-duration','disabled','ghost')}${waButton('清空触发记录','channel-error','disabled','danger')}</div><p class="wa-disabled-note">绑定、条件编辑、matcher、itemSubmit 和测试写操作需要后端写入支持，本轮不启用。</p></article>
                  </aside>`;
                }

                """)
.append("""
                function snapshotKindLabel(kind){const v=String(kind||'auto').toLowerCase();return {manual:'手动保存',auto:'自动快照',pre_rollback:'回滚前保护'}[v]||'自动快照';}
                function snapshotModuleLabel(module){const v=String(module||'').trim().toLowerCase();return {'snapshot':'快照','webadmin':'WebAdmin','device':'设备','logic chain':'逻辑链','template':'模板','timer':'计时器','signalbridge':'Signal 频道','signal bridge':'Signal 频道','signal listener':'信号监听器','signal join':'信号汇合','actionrelay':'动作继电器','action relay':'动作继电器','virtualblockdevice':'虚拟方块设备','virtual block device':'虚拟方块设备','condition':'条件','condition group':'条件组','state variable':'状态变量','region':'区域','region controller':'区域控制器'}[v]||module||'-';}
                function snapshotResourceLabel(resource){const v=String(resource||'').trim().toLowerCase();return {channel_metadata:'频道显示信息',condition_group:'条件组',condition_runtime_gate:'条件运行时 Gate',device_metadata:'设备显示信息',logic_chain_metadata:'逻辑链显示信息',region_controller:'区域控制器',signal_device:'信号设备',signal_join:'信号汇合',signal_listener:'信号监听器',state_variable:'状态变量',template:'模板',timer:'计时器'}[v]||resource||'-';}
                function snapshotOperationLabel(operation){const v=String(operation||'').trim().toUpperCase();return {ROLLBACK_SNAPSHOT:'回滚快照',CREATE_SNAPSHOT:'创建保存点',DELETE_SNAPSHOT:'删除保存点',EDIT_DEVICE_METADATA:'编辑设备显示信息',EDIT_DEVICE_BASIC_CONFIG:'编辑设备基础配置',EDIT_DEVICE_EXTENDED_CONFIG:'编辑设备扩展配置',EDIT_ACTION_RELAY_ACTIONS:'编辑动作继电器动作',EDIT_ITEM_MATCHER:'编辑物品匹配模板',EDIT_LOGIC_CHAIN:'编辑逻辑链',EDIT_LOGIC_CHAIN_METADATA:'编辑逻辑链显示信息',APPLY_TEMPLATE:'应用模板',IMPORT_TEMPLATE:'导入模板',DELETE_VIRTUAL_BLOCK_DEVICE:'删除虚拟方块设备',EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS:'编辑 VBD 原生触发',EDIT_REGION:'编辑区域控制器',EDIT_TIMER:'编辑计时器',EDIT_SIGNAL_JOIN:'编辑信号汇合',EDIT_SIGNAL_LISTENER_BASIC_CONFIG:'编辑监听器基础配置',EDIT_SIGNAL_LISTENER_ACTIONS:'编辑监听器动作',EDIT_CONDITION_GROUP:'编辑条件组',EDIT_STATE_VARIABLE:'编辑状态变量定义',EDIT_CHANNEL_METADATA:'编辑频道显示信息'}[v]||operation||'-';}
                function snapshotKindClass(kind){return String(kind||'auto').toLowerCase().replace(/_/g,'-').replace(/[^a-z0-9-]/g,'-');}
                function snapshotDiffChanged(summary){const s=summary||{};return Number(s.created||0)+Number(s.updated||0)+Number(s.deleted||0);}
                function snapshotChangeText(summary){const s=summary||{}, changed=snapshotDiffChanged(s);return changed<=0?'无资源变化':`新增 ${Number(s.created||0)} / 更新 ${Number(s.updated||0)} / 删除 ${Number(s.deleted||0)}`;}
                function snapshotDate(value){return isBlank(value)?'-':formatDateTime(value);}
                function snapshotLocalDateTimeValue(value){if(isBlank(value))return '';const d=new Date(value);if(Number.isNaN(d.getTime()))return '';const pad=n=>String(n).padStart(2,'0');return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;}
                function snapshotFilterIsoFromLocal(value){if(isBlank(value))return '';const d=new Date(value);if(Number.isNaN(d.getTime()))return '';const pad=(n,w=2)=>String(n).padStart(w,'0');return `${d.getUTCFullYear()}-${pad(d.getUTCMonth()+1)}-${pad(d.getUTCDate())}T${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())}:${pad(d.getUTCSeconds())}.${pad(d.getUTCMilliseconds(),3)}Z`;}
                function snapshotCanCreateManual(){return (appState.capabilities?.operations||[]).some(op=>op.operation==='CREATE_SNAPSHOT'&&op.allowed);}
                function snapshotCanRollback(){return (appState.capabilities?.operations||[]).some(op=>op.operation==='ROLLBACK_SNAPSHOT'&&op.allowed);}
                function snapshotFilterQuery(){const f=appState.snapshotFilters||{};const params=new URLSearchParams();['search','kind','module','resource','user','from','to'].forEach(key=>{const value=f[key]||'';if(value&&value!=='ALL')params.set(key,value);});const q=params.toString();return q?`?${q}`:'';}
                function snapshotFilterStateKey(){const f=appState.snapshotFilters||{};return ['search','kind','module','resource','user','from','to'].map(key=>`${key}:${f[key]||''}`).join('|');}
                function snapshotRecordSearchHaystack(record){const trigger=record?.trigger||{};return [record?.snapshotId,record?.title,record?.note,record?.createdBy,snapshotKindLabel(record?.kind),snapshotModuleLabel(trigger.module),snapshotOperationLabel(trigger.operation),trigger.targetType,trigger.targetId].join(' ').toLowerCase();}
                function snapshotClientFilterRecords(records){const search=String(appState.snapshotFilters?.search||'').trim().toLowerCase();if(!search)return records;return (records||[]).filter(record=>snapshotRecordSearchHaystack(record).includes(search));}
                function applySnapshotSearchPreview(){const records=snapshotClientFilterRecords(Array.isArray(appState.snapshotTimeline?.records)?appState.snapshotTimeline.records:[]), selectedId=String(appState.snapshotDetail?.record?.snapshotId||records[0]?.snapshotId||''), graph=document.querySelector('[data-snapshot-timeline-graph]');if(graph)graph.innerHTML=renderSnapshotTimelineGraph(records,selectedId);}
                function setSnapshotFilter(field,value,composing=false){appState.snapshotFilters={...(appState.snapshotFilters||{}),[field]:value};if(field==='search'){if(composing)return;applySnapshotSearchPreview();clearTimeout(appState.snapshotSearchTimer);appState.snapshotSearchTimer=setTimeout(()=>renderSnapshotTimelinePage({silent:true}),260);return;}renderSnapshotTimelinePage({silent:true});}
                function setSnapshotSearchComposing(composing,input){appState.snapshotSearchComposing=!!composing;if(input)input.dataset.snapshotComposing=composing?'true':'false';if(!composing&&input)setSnapshotFilter('search',input.value,false);}
                function resetSnapshotFilters(){appState.snapshotFilters={search:'',kind:'ALL',module:'ALL',resource:'ALL',user:'ALL',from:'',to:''};renderSnapshotTimelinePage({silent:true});}
                function openSnapshotTimelineNode(snapshotId){if(isBlank(snapshotId))return;const state=captureViewState();history.pushState(null,'',`#/snapshots/${encodeURIComponent(snapshotId)}`);renderSnapshotTimelinePage({silent:true,preserveViewState:state});}
                async function renderSnapshotTimelinePage(options={}){
                  const expectedHash=options.expectedHash||currentRouteHash();
                  if(!options.silent)setView(loading('正在加载配置快照时间轴...'),{expectedHash});
                  const filterStateKey=snapshotFilterStateKey(), queryString=snapshotFilterQuery();
                  const list=await api(`/api/webadmin/snapshots${queryString}`);
                  if(filterStateKey!==snapshotFilterStateKey())return;
                  appState.snapshotTimeline=list;
                  const records=snapshotClientFilterRecords(Array.isArray(list.records)?list.records:[]);
                  const hash=currentRouteHash(), routeSelectedId=hash.startsWith('#/snapshots/')?routeDetailId(hash,'#/snapshots/'):'';
                  const snapshotFilteredRecordIds=new Set(records.map(record=>String(record.snapshotId||'')));
                  const previousSelectedId=String(appState.snapshotDetail?.record?.snapshotId||'');
                  const selectedId=routeSelectedId||(snapshotFilteredRecordIds.has(previousSelectedId)?previousSelectedId:(records[0]?.snapshotId||'')); // data-snapshot-selection-within-filtered-graph
                  const selectedHiddenByFilter=!!routeSelectedId&&!snapshotFilteredRecordIds.has(routeSelectedId);
                  let detail=null;
                  if(selectedId){
                    detail=await api(`/api/webadmin/snapshots/${encodeURIComponent(selectedId)}`).catch(()=>null);
                    appState.snapshotDetail=detail;
                  }else appState.snapshotDetail=null;
                  const f=appState.snapshotFilters||{}, stats=list.stats||{}, filters=list.filters||{};
                  const manualButton=snapshotCanCreateManual()?waButton('手动保存点','snapshot',htmlHandler('showSnapshotManualModal()'),'primary','data-snapshot-manual-entry="true"'):waButton('只读快照','snapshot','disabled','ghost');
                  const header=waPageHead('配置时间轴','手动保存点、自动快照、diff 与安全 dry-run 回滚。',`${manualButton}${waButton('刷新','refresh',htmlHandler('renderSnapshotTimelinePage()'),'ghost')}`);
                  const degradedWarning=list.degraded?`<div class="readonly-note danger" data-snapshot-degraded-warning="true" data-snapshot-help-topic="snapshot.rollback">配置时间轴当前处于降级状态：${esc(list.message||'manifest 或快照存储不可读取。请先检查服务端日志和 tzz/webadmin/snapshots。')}</div>`:'';
                  const hiddenWarning=selectedHiddenByFilter?`<div class="readonly-note warning" data-snapshot-selected-hidden-by-filter="true">当前详情保存点被筛选条件隐藏，右侧仍保留只读详情。${waButton('清除筛选','filter',htmlHandler('resetSnapshotFilters()'),'ghost')}</div>`:'';
                  const moduleOptions=['ALL',...(filters.modules||[])].map(v=>`<option value="${esc(v)}" ${String(f.module||'ALL')===String(v)?'selected':''}>${esc(v==='ALL'?'全部模块':snapshotModuleLabel(v))}</option>`).join('');
                  const resourceOptions=['ALL',...(filters.resourceTypes||[])].map(v=>`<option value="${esc(v)}" ${String(f.resource||'ALL')===String(v)?'selected':''}>${esc(v==='ALL'?'全部变化资源':snapshotResourceLabel(v))}</option>`).join('');
                  const userOptions=['ALL',...(filters.users||[])].map(v=>`<option value="${esc(v)}" ${String(f.user||'ALL')===String(v)?'selected':''}>${esc(v==='ALL'?'全部用户':v)}</option>`).join('');
                  const html=`<section class="wa-page snapshot-timeline-page" data-snapshot-timeline-page="true" data-snapshot-timeline-not-table="true" data-snapshot-node-kind-manual="marker" data-snapshot-node-kind-auto="marker" data-snapshot-node-kind-pre-rollback="marker" data-snapshot-operation-diff-item="marker" data-snapshot-previous-diff-item="marker">
                    ${header}
                    ${degradedWarning}
                    ${hiddenWarning}
                    <div class="wa-card-grid wa-metrics-4 snapshot-metrics">
                      ${snapshotMetric('全部保存点',stats.total||0,'snapshot')}
                      ${snapshotMetric('手动',stats.manual||0,'snapshot','manual')}
                      ${snapshotMetric('自动',stats.auto||0,'clock','auto')}
                      ${snapshotMetric('回滚保护',stats.preRollback||0,'warning-issue','pre_rollback')}
                    </div>
                    <div class="snapshot-filter-bar" data-snapshot-filter-bar="true">
                      <label class="filter-field search-control"><span>搜索</span><input id="snapshot-filter-search" class="input" value="${esc(f.search||'')}" placeholder="标题 / 备注 / 目标 ID" oncompositionstart="setSnapshotSearchComposing(true,this)" oncompositionend="setSnapshotSearchComposing(false,this)" oninput="setSnapshotFilter('search',this.value,this.dataset.snapshotComposing==='true'||appState.snapshotSearchComposing)" data-snapshot-filter-search="true" data-snapshot-filter-search-debounced="true" data-snapshot-filter-ime-safe="true"></label>
                      <label class="filter-field"><span>类型</span><select class="select" onchange="setSnapshotFilter('kind',this.value)"><option value="ALL" ${!f.kind||f.kind==='ALL'?'selected':''}>全部</option><option value="manual" ${f.kind==='manual'?'selected':''}>手动</option><option value="auto" ${f.kind==='auto'?'selected':''}>自动</option><option value="pre_rollback" ${f.kind==='pre_rollback'?'selected':''}>回滚前</option></select></label>
                      <label class="filter-field"><span>模块</span><select class="select" onchange="setSnapshotFilter('module',this.value)">${moduleOptions}</select></label>
                      <label class="filter-field"><span>资源</span><select class="select" onchange="setSnapshotFilter('resource',this.value)" data-snapshot-filter-resource="true">${resourceOptions}</select></label>
                      <label class="filter-field"><span>用户</span><select class="select" onchange="setSnapshotFilter('user',this.value)">${userOptions}</select></label>
                      <label class="filter-field"><span>开始时间</span><input class="input" type="datetime-local" value="${esc(snapshotLocalDateTimeValue(f.from||''))}" onchange="setSnapshotFilter('from',snapshotFilterIsoFromLocal(this.value))"></label>
                      <label class="filter-field"><span>结束时间</span><input class="input" type="datetime-local" value="${esc(snapshotLocalDateTimeValue(f.to||''))}" onchange="setSnapshotFilter('to',snapshotFilterIsoFromLocal(this.value))"></label>
                    </div>
                    <div class="snapshot-timeline-layout">
                      <section class="snapshot-timeline-graph" data-snapshot-timeline-graph="true" data-snapshot-timeline-card-scroll-limit="10">${renderSnapshotTimelineGraph(records,selectedId)}</section>
                      <aside class="snapshot-detail-rail" data-snapshot-detail-rail="true">${renderSnapshotDetailRail(detail,selectedId,list)}</aside>
                    </div>
                  </section>`;
                  setView(html,{silent:options.silent,expectedHash});
                  if(options.preserveViewState)restoreViewState(options.preserveViewState);
                  renderIcons(appView());
                }
                function snapshotMetric(label,value,iconName,kind=''){return `<article class="wa-metric snapshot-metric ${esc(kind)}"><div class="wa-metric-top"><span class="wa-metric-label">${esc(label)}</span><span class="wa-icon-bubble">${icon(iconName)}</span></div><div class="wa-metric-value">${esc(value)}</div></article>`;}
                function renderSnapshotTimelineGraph(records,selectedId){
                  if(!records.length)return empty('暂无配置快照。点击“手动保存点”可以创建第一个保存节点。');
                  return `<div class="snapshot-graph-stream">${records.map((record,index)=>renderSnapshotNode(record,selectedId,index)).join('')}</div>`;
                }
                function renderSnapshotNode(record,selectedId,index){const kind=snapshotKindClass(record.kind), active=record.snapshotId===selectedId, trigger=record.trigger||{}, recordKind=String(record.kind||'').toLowerCase(), diff=record.diffSummary||{}, opDiff=record.operationDiff?.summary||{}, nodeDiff=(recordKind==='auto'||recordKind==='pre_rollback')&&snapshotDiffChanged(opDiff)>0?opDiff:diff;return `<button type="button" class="snapshot-node ${esc(kind)} ${active?'active':''}" data-snapshot-node-kind-${esc(kind)}="true" data-snapshot-node-id="${esc(record.snapshotId)}" data-snapshot-node-select="true">
                  <span class="snapshot-rail"><i></i><b>${index===0?'': ''}</b></span>
                  <span class="snapshot-node-body">
                    <span class="snapshot-node-main"><strong>${esc(record.title||record.snapshotId)}</strong><em>${esc(snapshotKindLabel(record.kind))}</em></span>
                    <span class="snapshot-node-meta">${esc(snapshotDate(record.createdAt))} · ${esc(record.createdBy||'system')} · ${esc(snapshotModuleLabel(trigger.module||'WebAdmin'))}</span>
                    <span class="snapshot-node-diff">${esc(snapshotChangeText(nodeDiff))}</span>
                  </span>
                </button>`;}
                """)
.append("""
                function renderSnapshotDetailRail(detail,selectedId,list){
                  if(!selectedId)return `<div class="wa-panel">${empty('请选择一个快照节点查看详情。')}</div>`;
                  if(!detail||detail.notFound)return `<div class="wa-panel">${errorBlock(detail?.message||'快照详情加载失败。')}</div>`;
                  const record=detail.record||{}, trigger=record.trigger||{}, diff=detail.diff||{summary:{},entries:[]}, operationDiff=detail.operationDiff||record.operationDiff||{summary:{},entries:[]}, resources=Array.isArray(detail.resources)?detail.resources:[], canRollback=snapshotCanRollback(), recordKind=String(record.kind||'').toLowerCase(), isAuto=recordKind==='auto', isPreRollback=recordKind==='pre_rollback';
                  const rollback=canRollback?waButton('回滚到此保存点','snapshot',htmlHandler(`openSnapshotRollbackDryRun(${jsString(record.snapshotId)})`),'danger','data-snapshot-rollback-entry="true"'):waButton('OWNER 可回滚','snapshot','disabled','ghost');
                  const autoOperationNote=isAuto?`<span class="snapshot-before-write-marker" data-snapshot-before-write-explained="true"></span>`:'';
                  const detailWarning=detail.degraded?`<div class="readonly-note danger" data-snapshot-bad-package-warning="true" data-snapshot-degraded-warning="true">该保存点详情读取受限：${esc(detail.message||'快照包或上一保存点数据不可读取。')}</div>`:'';
                  const operationDiffBlock=(isAuto||isPreRollback)?`<h3>本次操作变化</h3><div data-snapshot-operation-diff="true" data-snapshot-operation-timer-updated="true" data-snapshot-rollback-operation-diff="true">${renderSnapshotDiff(operationDiff,isPreRollback?'本次回滚未检测到资源级变化。':'本次写入未检测到资源级变化。','operation')}</div>`:'';
                  const previousDiffBlock=(isAuto||isPreRollback)?`<details class="raw-config snapshot-previous-diff-advanced"><summary>${isPreRollback?'保护点创建时相对上一保存点的差异':'保护点自身与上一保存点的变化'}</summary>${renderSnapshotDiff(diff,'无资源级变化。','previous')}</details>`:`<h3>与上一保存点的变化</h3>${renderSnapshotDiff(diff,'无资源级变化。','previous')}`;
                  return `<div class="wa-panel snapshot-detail-card" data-snapshot-detail-diff="true">
                    <h2>${esc(record.title||record.snapshotId)}</h2>
                    <div class="identity-grid">
                      ${row('类型',textPill(snapshotKindLabel(record.kind),record.kind==='manual'?'ok':record.kind==='pre_rollback'?'warning':'info'))}
                      ${row('创建时间',esc(snapshotDate(record.createdAt)))}
                      ${row('创建者',esc(record.createdBy||'system'))}
                      ${row('触发模块',esc(snapshotModuleLabel(trigger.module||'-')))}
                      ${row('触发操作',esc(snapshotOperationLabel(trigger.operation||'-')))}
                      ${row('目标',esc([trigger.targetType,trigger.targetId].filter(Boolean).join(' / ')||'-'))}
                      ${row('Package 指纹',`<code>${esc(record.packageFingerprint||'-')}</code>`)}
                    </div>
                    <div class="snapshot-note">${esc(record.note||'无备注')}</div>
                    ${detailWarning}
                    <div class="snapshot-detail-actions">${rollback}</div>
                    ${autoOperationNote}
                    ${operationDiffBlock}
                    ${previousDiffBlock}
                    <details class="raw-config" data-snapshot-json-preview="true"><summary>资源 JSON 高级预览</summary><pre class="wa-code-block">${esc(JSON.stringify(resources.slice(0,40),null,2))}</pre></details>
                  </div>`;
                }
                """)
.append("""
                function renderSnapshotDiff(diff,emptyText='无资源级变化。',source='previous'){const entries=(diff.entries||[]).filter(e=>e.changeType!=='unchanged'), summary=diff.summary||{};if(!entries.length)return `<div class="snapshot-diff-empty">${esc(emptyText)}</div>`;return `<div class="snapshot-diff-summary">${textPill(`新增 ${summary.created||0}`,'ok')}${textPill(`更新 ${summary.updated||0}`,'info')}${textPill(`删除 ${summary.deleted||0}`,'warning')}</div><div class="snapshot-diff-list" data-snapshot-diff-entry-event-delegation="true">${entries.slice(0,80).map((e,index)=>`<button type="button" class="snapshot-diff-row ${esc(e.changeType)}" data-snapshot-diff-entry="true" data-snapshot-diff-clickable="true" data-snapshot-diff-button-type="button" data-snapshot-diff-source="${esc(source)}" data-snapshot-diff-index="${esc(index)}" data-snapshot-${source==='operation'?'operation':'previous'}-diff-item="true" data-snapshot-updated-resource="${esc(e.resourceType)}"><strong>${esc(snapshotDiffChangeLabel(e.changeType))}</strong><span>${esc(snapshotResourceLabel(e.resourceType))} / ${esc(e.displayName||e.resourceId)}</span><small>${esc(e.sourceStore||'')}</small></button>`).join('')}</div>`;}
                function snapshotDiffChangeLabel(type){return {created:'新增',updated:'更新',deleted:'删除',create:'新增',update:'更新',delete:'删除'}[String(type||'')]||'变化';}
                function snapshotRollbackOperationLabel(operation){return {create:'新增',update:'覆盖 / 更新',delete:'删除',created:'新增',updated:'覆盖 / 更新',deleted:'删除'}[String(operation||'')]||snapshotDiffChangeLabel(operation);}
                function snapshotDiffSourceLabel(source){return source==='operation'?'本次操作变化':'与上一保存点的变化';}
                function snapshotCurrentDiffEntries(source){const detail=appState.snapshotDetail||{}, diff=source==='operation'?(detail.operationDiff||detail.record?.operationDiff||{}):(detail.diff||{});return (diff.entries||[]).filter(e=>e.changeType!=='unchanged');}
                function handleSnapshotTimelineNodeClick(event){const target=event.target&&event.target.closest?event.target.closest('[data-snapshot-node-select]'):null;if(!target)return false;event.preventDefault();event.stopPropagation();openSnapshotTimelineNode(target.dataset.snapshotNodeId||'');return true;}
                function handleSnapshotDiffDelegatedClick(event){const target=event.target&&event.target.closest?event.target.closest('[data-snapshot-diff-entry]'):null;if(!target)return false;event.preventDefault();event.stopPropagation();const source=target.dataset.snapshotDiffSource==='operation'?'operation':'previous', index=Number(target.dataset.snapshotDiffIndex||-1), entry=snapshotCurrentDiffEntries(source)[index];if(!entry)return true;showSnapshotDiffDetailModal(entry,source);return true;}
                function snapshotDiffDisplayValue(value){const raw=String(value??'').trim(), unquoted=(raw.startsWith('"')&&raw.endsWith('"'))?raw.slice(1,-1):raw;const parsed=parseTime(unquoted);if(parsed&&/^\\d{4}-\\d{2}-\\d{2}T/.test(unquoted))return formatDateTime(unquoted);return raw;}
                function snapshotDiffValue(value){return isBlank(value)?'<span class="muted">空</span>':`<code>${esc(snapshotDiffDisplayValue(value))}</code>`;}
                function snapshotDiffJsonBlock(label,value,marker){if(isBlank(value))return '';return `<details class="raw-config" ${marker||''}><summary>${esc(label)}</summary><pre class="wa-code-block">${esc(value)}</pre></details>`;}
                function snapshotDiffFieldRows(entry){const fields=Array.isArray(entry.fieldDiffs)?entry.fieldDiffs:[];if(!fields.length)return `<div class="snapshot-diff-empty">当前为资源级 diff，未检测到可展开的浅层字段差异；可查看下方 JSON 摘要和资源指纹。</div>`;return `<div class="snapshot-field-diff-list" data-snapshot-diff-detail-field-diff="true"><div class="snapshot-field-diff-head"><strong>字段</strong><strong>旧值</strong><strong>新值</strong></div>${fields.map(field=>`<div class="snapshot-field-diff-row"><strong>${esc(field.field||'$')}</strong><span>${snapshotDiffValue(field.beforeValue)}</span><span>${snapshotDiffValue(field.afterValue)}</span></div>`).join('')}${Number(entry.omittedFieldDiffs||0)>0?`<div class="readonly-note">还有 ${Number(entry.omittedFieldDiffs||0)} 项字段变化未显示。</div>`:''}</div>`;}
                """)
.append("""
                function showSnapshotDiffDetailModal(entry,source){const type=snapshotDiffChangeLabel(entry.changeType), title=`${type} ${snapshotResourceLabel(entry.resourceType)||'资源'}`;const body=`<div class="snapshot-diff-detail-modal" data-snapshot-diff-detail-modal="true" data-snapshot-diff-detail-readonly="true" data-snapshot-diff-detail-no-save="true" data-snapshot-diff-detail-resource-metadata="true" data-snapshot-diff-detail-updated-summary="true" data-snapshot-diff-detail-created-summary="true" data-snapshot-diff-detail-deleted-summary="true" data-snapshot-diff-entry-event-delegation="true"><div class="identity-grid">${row('来源',esc(snapshotDiffSourceLabel(source)))}${row('变化类型',esc(type))}${row('资源类型',esc(snapshotResourceLabel(entry.resourceType||'-')))}${row('资源 ID',`<code>${esc(entry.resourceId||'-')}</code>`)}${row('显示名称',esc(entry.displayName||'-'))}${row('Source store',esc(entry.sourceStore||'-'))}${row('变更前指纹',`<code>${esc(entry.beforeFingerprint||'-')}</code>`)}${row('变更后指纹',`<code>${esc(entry.afterFingerprint||'-')}</code>`)}</div>${entry.changeType==='updated'?`<h3>字段变化</h3>${snapshotDiffFieldRows(entry)}`:''}${entry.changeType==='created'?`<h3>新增后的资源摘要</h3><p class="readonly-note">${esc(entry.afterSummary||'无摘要')}</p>`:''}${entry.changeType==='deleted'?`<h3>删除前的资源摘要</h3><p class="readonly-note">${esc(entry.beforeSummary||'无摘要')}</p>`:''}${entry.changeType==='updated'?`<h3>变更前后摘要</h3><p class="readonly-note">旧：${esc(entry.beforeSummary||'-')}<br>新：${esc(entry.afterSummary||'-')}</p>`:''}${snapshotDiffJsonBlock('变更前 JSON 预览',entry.beforeJsonPreview,'data-snapshot-diff-detail-before-json="true"')}${snapshotDiffJsonBlock('变更后 JSON 预览',entry.afterJsonPreview,'data-snapshot-diff-detail-after-json="true"')}</div>`;openWebAdminModal(title,body,`<button class="wa-btn ghost" type="button" onclick="dismissWebAdminModal()" data-snapshot-diff-detail-close="true">关闭</button>`,{className:'wa-config-modal snapshot-diff-detail-shell',dirtyCheck:()=>false});}
                function showSnapshotManualModal(){appState.snapshotManualDraft={title:'',note:'',tags:'',saving:false,errors:[]};markModalInitialSnapshot('snapshot_manual',appState.snapshotManualDraft);renderSnapshotManualModal();}
                function renderSnapshotManualModal(){const d=appState.snapshotManualDraft;if(!d)return;const errors=(d.errors||[]).length?`<ul class="validation-list">${d.errors.map(e=>`<li>${esc(e.message||e)}</li>`).join('')}</ul>`:'';openWebAdminModal('创建手动保存点',`<form class="edit-form" data-snapshot-manual-modal="true" onsubmit="event.preventDefault();saveSnapshotManual()"><label>名称<input id="snapshot-title" class="input" maxlength="80" value="${esc(d.title)}" oninput="syncSnapshotManualDraft()" required></label><label>备注<textarea id="snapshot-note" class="input wa-action-textarea" maxlength="1000" oninput="syncSnapshotManualDraft()">${esc(d.note)}</textarea></label><label>标签<input id="snapshot-tags" class="input" maxlength="256" value="${esc(d.tags)}" oninput="syncSnapshotManualDraft()" placeholder="用逗号分隔"></label><p class="readonly-note">保存的是 WebAdmin 配置快照，不包含运行时历史、在线玩家背包或世界实体。</p>${errors}</form>`,`<button class="wa-btn ghost" type="button" onclick="dismissWebAdminModal()">取消</button><button class="wa-btn primary" type="button" onclick="saveSnapshotManual()" ${d.saving?'disabled':''}>${d.saving?'保存中...':'创建保存点'}</button>`,{className:'wa-config-modal',dirtyCheck:()=>modalDraftDirty('snapshot_manual',appState.snapshotManualDraft)});}
                function syncSnapshotManualDraft(){const d=appState.snapshotManualDraft;if(!d)return;d.title=document.getElementById('snapshot-title')?.value??d.title;d.note=document.getElementById('snapshot-note')?.value??d.note;d.tags=document.getElementById('snapshot-tags')?.value??d.tags;}
                async function saveSnapshotManual(){const d=appState.snapshotManualDraft;if(!d)return;syncSnapshotManualDraft();d.saving=true;d.errors=[];renderSnapshotManualModal();try{const result=await api('/api/webadmin/snapshots/manual',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({title:d.title,note:d.note,tags:String(d.tags||'').split(',').map(s=>s.trim()).filter(Boolean)})});if(result.success){appState.snapshotManualDraft=null;await dismissWebAdminModal();toast(result.message||'手动保存点已创建。');location.hash=`#/snapshots/${encodeURIComponent(result.data?.snapshot?.snapshotId||'')}`;await renderSnapshotTimelinePage({silent:true});return;}d.saving=false;d.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];renderSnapshotManualModal();}catch(err){d.saving=false;d.errors=[{message:err.message||'保存失败'}];renderSnapshotManualModal();}}
                async function openSnapshotRollbackDryRun(snapshotId){try{const list=appState.snapshotTimeline||await api('/api/webadmin/snapshots');const result=await api(`/api/webadmin/snapshots/${encodeURIComponent(snapshotId)}/rollback/dry-run`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({expectedFingerprint:list.manifestFingerprint||''})});if(!result.success){toast(result.message||'回滚 dry-run 失败。');return;}appState.snapshotRollback={snapshotId,manifestFingerprint:list.manifestFingerprint||'',plan:result.data?.plan||{},lockId:'',applying:false,errors:[]};showSnapshotRollbackDryRunModal();}catch(err){toast(err.message||'回滚 dry-run 失败。');}}
                function showSnapshotRollbackDryRunModal(){const d=appState.snapshotRollback;if(!d)return;const plan=d.plan||{}, ops=plan.operations||[], blockers=plan.blockers||[], warnings=plan.warnings||[];openWebAdminModal('回滚 dry-run 预览',`<div class="snapshot-rollback-modal" data-snapshot-rollback-dry-run-modal="true"><p>将回滚到 <code>${esc(d.snapshotId)}</code>。请先确认将写入、覆盖或删除的配置文件。</p>${blockers.length?`<div class="validation-list">${blockers.map(b=>`<div>${esc(b)}</div>`).join('')}</div>`:''}${warnings.length?`<div class="readonly-note">${warnings.map(esc).join('<br>')}</div>`:''}<div class="snapshot-rollback-ops">${ops.length?ops.map(op=>`<div class="snapshot-rollback-op ${esc(op.operation)}"><strong>${esc(snapshotRollbackOperationLabel(op.operation))}</strong><span>${esc(op.pathKey)}</span><small>${esc(op.beforeFingerprint||'-')} → ${esc(op.afterFingerprint||'-')}</small></div>`).join(''):'无文件级写入变化。'}</div></div>`,`<button class="wa-btn ghost" type="button" onclick="dismissWebAdminModal()">取消</button><button class="wa-btn danger" type="button" onclick="showSnapshotRollbackConfirmModal()" ${blockers.length?'disabled':''}>继续确认</button>`,{className:'snapshot-rollback-modal-shell'});}
                function showSnapshotRollbackConfirmModal(){const d=appState.snapshotRollback;if(!d)return;openWebAdminModal('确认执行回滚',`<div class="confirm-stack" data-snapshot-rollback-confirm-modal="true"><p>回滚会覆盖当前 allowlist 中的 WebAdmin 配置文件。系统会先创建 pre_rollback 保护点。</p><p>目标快照：<code>${esc(d.snapshotId)}</code></p><p>Dry-run 指纹：<code>${esc(d.plan?.dryRunFingerprint||'')}</code></p>${(d.errors||[]).length?`<ul class="validation-list">${d.errors.map(e=>`<li>${esc(e.message||e)}</li>`).join('')}</ul>`:''}</div>`,`<button class="wa-btn ghost" type="button" onclick="showSnapshotRollbackDryRunModal()">返回预览</button><button class="wa-btn danger" type="button" onclick="applySnapshotRollback()" ${d.applying?'disabled':''}>${d.applying?'回滚中...':'确认回滚'}</button>`,{className:'snapshot-rollback-modal-shell'});}
                async function applySnapshotRollback(){const d=appState.snapshotRollback;if(!d)return;d.applying=true;d.errors=[];showSnapshotRollbackConfirmModal();try{const lock=await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'snapshot_rollback',targetId:'timeline'})});if(!lock.success){d.applying=false;d.errors=[{message:lock.message||'无法获取回滚编辑锁'}];showSnapshotRollbackConfirmModal();return;}d.lockId=lock.data?.lock?.lockId||'';const result=await api(`/api/webadmin/snapshots/${encodeURIComponent(d.snapshotId)}/rollback/apply`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({expectedFingerprint:d.manifestFingerprint||'',dryRunFingerprint:d.plan?.dryRunFingerprint||'',lockId:d.lockId,confirmed:true})});if(result.success){appState.snapshotRollback=null;await dismissWebAdminModal();toast(result.message||'配置已回滚。');await renderSnapshotTimelinePage({silent:true});return;}d.applying=false;d.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'回滚失败'}];showSnapshotRollbackConfirmModal();}catch(err){d.applying=false;d.errors=[{message:err.message||'回滚失败'}];showSnapshotRollbackConfirmModal();}}

                async function renderTemplatesPage(options={}){
                  if(!options.silent)setView(loading('正在加载模板中心...'));
                  let data;try{data=await api('/api/webadmin/templates')}catch(err){if(options.silent){toast('模板中心实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.templateCenter=data||{templates:[]};
                  appState.templates=Array.isArray(data?.templates)?data.templates:[];
                  renderTemplateList('',options);
                }
                function renderTemplateList(focusId,options={}){
                  waEnsureState();
                  const data=appState.templateCenter||{}, templates=appState.templates||[], filtered=filterTemplates(templates), page=waPageItems('templates',filtered,10), builtIn=templates.filter(t=>String(t.source)==='built_in').length, user=templates.filter(t=>String(t.source)==='user').length, placeholders=templates.filter(t=>Number(t.placeholderCount||0)>0).length, categories=uniqueNonBlank(templates.map(t=>t.category));
                  const storeNote=data.storeDegraded?`<div class="error-state">用户模板库不可写：${esc(data.storeMessage||'templates.json 读取失败')}</div>`:'';
                  const importButton=canImportTemplate()?waButton('导入 JSON','upload',htmlHandler('openTemplateImportJsonModal()')+' data-template-import-json-modal="true"','primary'):waButton('导入 JSON','upload','disabled data-template-import-json-modal="true"','primary');
                  const rendered=setView(`<section class="wa-page template-center-page" data-template-list-route="true" data-template-no-browser-dialogs="true">
                    ${waPageHead('模板中心','从内置模板或用户导入模板创建真实 Signal / Join / Timer / Listener 配置。',`${importButton}${waButton('刷新','refresh',htmlHandler('renderTemplatesPage()'),'ghost')}`)}
                    ${storeNote}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('模板总数',templates.length,'内置 + 用户模板','template-package')}
                      ${waMetric('内置模板',builtIn,'随 WebAdmin 提供','signal-join','ok')}
                      ${waMetric('用户模板',user,'world-scoped templates.json','archive')}
                      ${waMetric('含占位引用',placeholders,'未映射时阻止 apply','warning-issue',placeholders?'warning':'')}
                      ${waMetric('模板库指纹',shortId(data.storeFingerprint||data.expectedFingerprint||'--'),'expectedFingerprint','session')}
                    </section>
                    <section class="wa-two-column template-center-layout">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="template-center-search" placeholder="搜索模板名称 / ID / 分类 / 描述..." value="${esc(appState.templateCenterFilters.search)}"></label>
                          <label class="filter-field"><span>来源</span>${waSelect('template-center-source',['ALL','built_in','user'],appState.templateCenterFilters.source,templateSourceLabel)}</label>
                          <label class="filter-field"><span>分类</span>${waSelect('template-center-category',['ALL',...categories],appState.templateCenterFilters.category,v=>v==='ALL'?'全部分类':v)}</label>
                          <label class="filter-field"><span>占位引用</span>${waSelect('template-center-placeholder',['ALL','HAS','NONE'],appState.templateCenterFilters.placeholder,templatePlaceholderLabel)}</label>
                          ${waButton('刷新','refresh',htmlHandler('renderTemplatesPage()'),'ghost')}
                        </div>
                        ${page.items.length===0?empty(templates.length?'没有匹配当前筛选条件的模板。':'当前暂无模板。'):templateTable(page.items)}
                        ${waPagination('templates',page)}
                      </div>
                      ${templateCenterRightRail(templates,filtered,data)}
                    </section>
                  </section>`,options);
                  if(rendered)bindTemplateCenterFilters(focusId);
                }
                function filterTemplates(items){
                  const f=appState.templateCenterFilters||{};
                  return (items||[]).filter(t=>{const hay=[t.templateId,t.displayName,t.description,t.category,t.source,(t.notes||[]).join(' '),(t.warnings||[]).join(' ')].join(' ').toLowerCase();if(f.search&&!hay.includes(String(f.search).toLowerCase()))return false;if(f.source&&f.source!=='ALL'&&String(t.source)!==f.source)return false;if(f.category&&f.category!=='ALL'&&String(t.category)!==f.category)return false;if(f.placeholder==='HAS'&&Number(t.placeholderCount||0)<=0)return false;if(f.placeholder==='NONE'&&Number(t.placeholderCount||0)>0)return false;return true;}).sort((a,b)=>String(a.category||'').localeCompare(String(b.category||''))||String(a.displayName||a.templateId).localeCompare(String(b.displayName||b.templateId)));
                }
                function bindTemplateCenterFilters(focusId){
                  const update=(event)=>{appState.templateCenterFilters.search=document.getElementById('template-center-search')?.value||'';appState.templateCenterFilters.source=document.getElementById('template-center-source')?.value||'ALL';appState.templateCenterFilters.category=document.getElementById('template-center-category')?.value||'ALL';appState.templateCenterFilters.placeholder=document.getElementById('template-center-placeholder')?.value||'ALL';appState.uiPages.templates=1;renderTemplateList(event?.target?.id||'');};
                  ['template-center-search','template-center-source','template-center-category','template-center-placeholder'].forEach(id=>document.getElementById(id)?.addEventListener(id==='template-center-search'?'input':'change',update));
                  restoreFocusEnd(focusId);
                }
                function templateTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table template-table"><thead><tr><th>模板名称 / ID</th><th>分类</th><th>来源</th><th>资源</th><th>占位引用</th><th>更新时间</th><th>操作</th></tr></thead><tbody>${items.map(t=>{const target=templateDetailHash(t.source,t.templateId), applyAttrs=canApplyTemplate()?htmlHandler(`openTemplateApplyWizard(${jsString(t.source)},${jsString(t.templateId)})`):'disabled';return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看模板 ${t.displayName||t.templateId}`)}><td><span class="device-name"><span class="device-icon">${icon(t.iconKey||'template-package')}</span><span><strong>${esc(t.displayName||t.templateId)}</strong><span class="device-subtitle">ID: ${esc(t.templateId||'--')}</span></span></span></td><td>${textPill(t.category||'未分类','info')}</td><td>${textPill(templateSourceLabel(t.source),t.source==='built_in'?'ok':'info')}</td><td><span>${esc(t.resourceCount||0)} 项</span><span class="device-subtitle">${esc(templateResourceCountText(t))}</span></td><td>${Number(t.placeholderCount||0)>0?textPill(`${t.placeholderCount} 个`,'warning'):textPill('无','ok')}</td><td>${fmtTime(t.updatedAt||t.createdAt||'')}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看模板 ${t.displayName||t.templateId}`)}>详情</button><button class="wa-btn primary" ${applyAttrs} data-template-apply-wizard="true">应用</button><button class="wa-btn ghost" type="button" ${htmlHandler(`event.stopPropagation();exportTemplateJson(${jsString(t.source)},${jsString(t.templateId)})`)} data-template-export-json-action="true">${icon('download')}<span>导出</span></button></div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function templateCenterRightRail(items,filtered,data){
                  const total=Math.max(1,filtered.length||items.length||1);
                  return `<aside class="wa-right-rail">
                    <article class="wa-panel"><h2>分类分布</h2>${progressList(distributionItems(filtered,t=>String(t.category||'未分类'),v=>v,total))}</article>
                    <article class="wa-panel"><h2>应用策略</h2><div class="list-stack"><div class="kv-row"><span class="muted">冲突策略</span><strong>同 ID 阻断</strong></div><div class="kv-row"><span class="muted">写入目标</span><strong>真实配置</strong></div><div class="kv-row"><span class="muted">导入行为</span><strong>只保存用户模板</strong></div><div class="kv-row"><span class="muted">世界实体</span><strong>外部引用占位</strong></div></div></article>
                    <article class="wa-panel"><h2>模板库状态</h2><div class="list-stack"><div class="kv-row"><span class="muted">存储文件</span><strong>${esc(data.userTemplateStore||'templates.json')}</strong></div><div class="kv-row"><span class="muted">按世界隔离</span><strong>${esc(data.worldScoped?'是':'否')}</strong></div><div class="kv-row"><span class="muted">状态</span><strong>${data.storeDegraded?pill('ERROR'):pill('OK')}</strong></div></div></article>
                    <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${canImportTemplate()?waButton('导入 JSON','upload',htmlHandler('openTemplateImportJsonModal()'),'primary'):waButton('导入 JSON','upload','disabled','primary')}${waButton('导出选中模板','download','disabled','ghost')}${waButton('从逻辑链导出','logic-chain','disabled','ghost')}${waButton('模板市场','archive','disabled','ghost')}</div><p class="wa-disabled-note">当前支持模板 JSON 导入/导出；从现有逻辑链组件反向导出本阶段暂缓。</p></article>
                  </aside>`;
                }
                async function renderTemplateDetailPage(arg,options={}){
                  if(!options.silent)setView(loading('正在加载模板详情...'));
                  const route=templateRouteParts(arg);
                  let detail;try{detail=await api(`/api/webadmin/templates/detail?source=${encodeURIComponent(route.source)}&id=${encodeURIComponent(route.templateId)}`);}catch(err){if(options.silent){toast('模板详情实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  if(detail?.notFound||detail?.permissionDenied||detail?.code==='template_source_invalid'||detail?.code==='template_schema_invalid'){const message=templateDetailErrorMessage(detail);setView(`<section class="wa-page" data-template-detail-error-code="${esc(detail.code||'template_error')}">${waPageHead('模板详情',message,backButton(route,'返回模板中心'))}${errorBlock(detail.message||message)}</section>`,options);return;}
                  appState.currentTemplateDetail=detail;
                  const summary=detail||{}, template=detail.template||{}, resources=detail.resources||{}, source=summary.source||route.source, templateId=summary.templateId||route.templateId, applyAttrs=canApplyTemplate()?htmlHandler(`openTemplateApplyWizard(${jsString(source)},${jsString(templateId)})`):'disabled';
                  const left=[
                    detailCard('模板概览',detailInfoGrid([
                      ['显示名称',summary.displayName||template.displayName],
                      ['模板 ID',templateId],
                      ['分类',summary.category||template.category],
                      ['来源',templateSourceLabel(source)],
                      ['资源数量',summary.resourceCount||0],
                      ['占位引用',summary.placeholderCount||0],
                      ['指纹',shortId(summary.fingerprint||summary.expectedFingerprint||'')]
                    ])),
                    detailCard('包含资源',templateResourceSummaryGrid(summary,resources)),
                    detailCard('占位引用 / 暂缓资源',templatePlaceholderSection(resources,detail)),
                    detailCard('说明 / 警告',templateNotesList([...(template.metadata?.notes||summary.notes||[]),...(template.metadata?.warnings||summary.warnings||[])], '暂无额外说明。'))
                  ];
                  const right=[
                    detailCard('操作',`<div class="wa-quick-grid">${waButton('应用模板','play',applyAttrs+' data-template-apply-wizard="true"','primary')}${waButton('导出 JSON','download',htmlHandler(`exportTemplateJson(${jsString(source)},${jsString(templateId)})`)+' data-template-export-json-action="true"','ghost')}${waButton('复制 JSON','copy',htmlHandler(`copyTextToClipboard(${jsString(detail.json||'')})`),'ghost')}${waButton('从组件导出','logic-chain','disabled','ghost')}</div><p class="wa-disabled-note">导入不会自动应用；应用必须先执行预览，再持有编辑锁写入真实配置。</p>`),
                    detailCard('JSON 预览',`<pre class="wa-code-block template-json-preview" data-template-detail-right-json-preview="true">${esc(detail.json||'')}</pre>`,`${waButton('复制 JSON','copy',htmlHandler(`copyTextToClipboard(${jsString(detail.json||'')})`)+' data-template-json-copy-in-right-panel="true"','ghost')}${waButton('下载 JSON','download',htmlHandler(`downloadTemplateJson(${jsString(templateId+'.json')},${jsString(detail.json||'')})`)+' data-template-json-download-in-right-panel="true"','ghost')}`,'template-json-right-card'),
                    detailCard('兼容性',templateNotesList(template.metadata?.compatibility||detail.compatibility||[], '暂无兼容性说明。'))
                  ];
                  setView(`<section class="wa-page wa-detail-shell template-detail-shell" data-template-detail-route="true" data-template-no-browser-dialogs="true" data-template-built-in-detail-route="${source==='built_in'?'true':'false'}" data-template-detail-source-built-in="${source==='built_in'?'true':'false'}" data-template-detail-export-apply-visible="true" data-template-detail-two-column-stretch="true" data-template-detail-responsive-stack="true">${detailHeader({back:backButton(route,'返回模板中心'),kicker:'模板中心 / 模板详情',iconName:summary.iconKey||template.iconKey||'template-package',title:summary.displayName||template.displayName||templateId,subtitle:`${templateSourceLabel(source)} · ${summary.category||template.category||'未分类'}`,copyValue:templateId,badges:[textPill(templateSourceLabel(source),source==='built_in'?'ok':'info'),Number(summary.placeholderCount||0)>0?textPill('含 placeholder','warning'):textPill('无 placeholder','ok')],actions:[waButton('应用模板','play',applyAttrs,'primary'),waButton('导出 JSON','download',htmlHandler(`exportTemplateJson(${jsString(source)},${jsString(templateId)})`)+' data-template-export-json-action="true"','ghost')]})}${detailFixedLayout(left,right)}</section>`,options);
                }
                function templateResourceSummaryGrid(summary,resources){
                  const items=[['频道',summary.channelCount||resources.channels?.length||0,'active-channel'],['信号汇合',summary.signalJoinCount||resources.signalJoins?.length||0,'signal-join'],['计时器',summary.timerCount||resources.timers?.length||0,'timer'],['信号监听器',summary.signalListenerCount||resources.signalListeners?.length||0,'consumer-listener'],['动作',summary.actionCount||resources.actions?.length||0,'action'],['状态变量',summary.stateVariableCount||resources.stateVariables?.length||0,'state-variable'],['条件组',summary.conditionGroupCount||resources.conditionGroups?.length||0,'condition-group']];
                  return `<div class="template-resource-grid">${items.map(([label,count,iconName])=>`<div class="template-resource-chip">${icon(iconName)}<span>${esc(label)}</span><strong>${esc(count)}</strong></div>`).join('')}</div>${templateResourceList(resources)}`;
                }
                function templateResourceList(resources){
                  const rows=[];
                  const add=(type,list)=>{(list||[]).forEach(item=>rows.push({type,id:item.id||item.channel||item.definition?.id||item.listener?.id||'',name:item.displayName||item.definition?.displayName||item.listener?.name||''}));};
                  add('channel',resources.channels);add('signalJoin',resources.signalJoins);add('timer',resources.timers);add('signalListener',resources.signalListeners);
                  if(rows.length===0)return empty('该模板没有可展示资源。');
                  return `<div class="wa-table-scroll template-resource-table"><table class="wa-table"><thead><tr><th>类型</th><th>ID</th><th>名称</th></tr></thead><tbody>${rows.map(r=>`<tr><td>${textPill(templateResourceTypeLabel(r.type),'info')}</td><td><code>${esc(r.id||'--')}</code></td><td>${esc(r.name||'--')}</td></tr>`).join('')}</tbody></table></div>`;
                }
                function templatePlaceholderSection(resources,detail){
                  const placeholders=resources.placeholders||[], stateVariables=resources.stateVariables||[], conditionGroups=resources.conditionGroups||[];
                  const deferred=[];if(stateVariables.length)deferred.push(`状态变量定义：${stateVariables.length} 项，本阶段暂缓。`);if(conditionGroups.length)deferred.push(`条件组：${conditionGroups.length} 项，本阶段暂缓。`);if(detail.componentExportSupported===false)deferred.push(detail.componentExportDeferredReason||'从现有组件导出本阶段暂缓。');
                  const ph=placeholders.length?`<div class="list-stack">${placeholders.map(p=>`<div class="kv-row"><span class="muted">${esc(templateResourceTypeLabel(p.type||'placeholder'))}</span><strong>${esc(p.displayName||p.id)}</strong><span class="device-subtitle">${esc(p.description||p.id)}</span></div>`).join('')}</div>`:empty('该模板没有外部世界实体占位引用。');
                  return `${ph}${deferred.length?`<ul class="validation-list">${deferred.map(x=>`<li>${esc(x)}</li>`).join('')}</ul>`:''}`;
                }
                function templateResourceTypeLabel(value){return {channel:'频道',signalJoin:'信号汇合',signal_join:'信号汇合',timer:'计时器',signalListener:'信号监听器',signal_listener:'信号监听器',action:'动作',actions:'动作',stateVariable:'状态变量',state_variable:'状态变量',conditionGroup:'条件组',condition_group:'条件组',placeholder:'占位引用',resource:'资源'}[String(value||'')]||String(value||'');}
                function templateNotesList(items,emptyText){const clean=(items||[]).filter(v=>!isBlank(v));return clean.length?`<div class="list-stack">${clean.map(v=>`<div class="event-row"><strong>${esc(v)}</strong></div>`).join('')}</div>`:empty(emptyText);}
                function templateDetailHash(source,templateId){return `#/templates/${encodeURIComponent(`${normalizeTemplateSource(source)}:${String(templateId||'')}`)}`;}
                function templateRouteParts(arg){const text=String(arg||''), index=text.indexOf('?'), encoded=index>=0?text.substring(0,index):text, query=index>=0?text.substring(index+1):'', params=new URLSearchParams(query);let raw=encoded;try{raw=decodeURIComponent(encoded);}catch(_){ }const idx=raw.indexOf(':');const route=idx<0?{source:'',templateId:raw}:{source:normalizeTemplateSource(raw.substring(0,idx)),templateId:raw.substring(idx+1)};route.fallback='#/templates';const returnTo=params.get('returnTo')||'';route.returnTo=isValidReturnHash(returnTo)?returnTo:'';return route;}
                function normalizeTemplateSource(source){return String(source||'').trim()==='built_in'?'built_in':'user';}
                function templateDetailErrorMessage(detail){const code=String(detail?.code||'');if(code==='template_permission_denied')return '没有权限查看该模板。';if(code==='template_source_invalid')return '模板来源无效。';if(code==='template_schema_invalid')return '模板数据无效，请检查模板库。';if(code==='template_not_found'||detail?.notFound)return '模板不存在或已移除。';return detail?.message||'模板不可用。';}
                function normalizeTemplatePrefix(raw){return String(raw||'').trim().toLowerCase().replace(/\\s+/g,'-').replace(/[^a-z0-9_.:-]/g,'').replace(/[.:-]+$/g,'').substring(0,48);}
                function templateSourceLabel(value){return {ALL:'全部来源',built_in:'内置',user:'用户模板',imported:'导入模板',exported_component:'导出组件'}[String(value||'')]||value||'未知';}
                function templatePlaceholderLabel(value){return {ALL:'全部',HAS:'含 placeholder',NONE:'无 placeholder'}[String(value||'')]||value;}
                function templateResourceCountText(t){return `频道 ${t.channelCount||0} / 信号汇合 ${t.signalJoinCount||0} / 计时器 ${t.timerCount||0} / 监听器 ${t.signalListenerCount||0}`;}
                async function exportTemplateJson(source,templateId){
                  try{const result=await api(`/api/webadmin/templates/export?source=${encodeURIComponent(normalizeTemplateSource(source))}&id=${encodeURIComponent(templateId)}`);const json=result.json||'';if(downloadTemplateJson(result.downloadFileName||`${templateId}.json`,json))toast('模板 JSON 已准备下载。');}catch(err){toast(err.message||'导出模板失败');}
                }
                function downloadTemplateJson(fileName,json){
                  try{const blob=new Blob([String(json||'')],{type:'application/json;charset=utf-8'}), url=URL.createObjectURL(blob), a=document.createElement('a');a.href=url;a.download=String(fileName||'template.json').replace(/[^a-zA-Z0-9_.-]/g,'_');document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);return true;}catch(_){toast('当前浏览器不允许直接下载，请使用复制 JSON。');return false;}
                }
                function openTemplateImportJsonModal(){
                  appState.templateImport={packageJson:'',importedTemplateId:'',importedDisplayName:'',expectedFingerprint:appState.templateCenter?.expectedFingerprint||appState.templateCenter?.storeFingerprint||'',lockId:'',lock:null,errors:[],preview:null,previewSnapshot:'',saving:false};
                  showTemplateImportModal();
                  acquireTemplateImportLock();
                }
                function showTemplateImportModal(){
                  const d=appState.templateImport;if(!d)return;markModalInitialSnapshot('template_import',d);
                  const errs=templateErrorsHtml(d.errors), preview=d.preview?templateImportPreviewHtml(d.preview):'';
                  const lockLine=d.lockId?`<div class="readonly-note">正在编辑用户模板库，锁到期：${esc(formatDateTime(d.lock?.expiresAt))}</div>`:'<div class="readonly-note warning">正在获取模板库编辑锁；未持锁前不能保存。</div>';
                  const body=`<form class="edit-form template-import-form" data-template-import-json-modal="true" data-template-no-browser-dialogs="true" onsubmit="event.preventDefault();saveTemplateImport()">${lockLine}<label>模板 JSON<textarea id="template-import-json" class="input wa-action-textarea template-json-input" placeholder="粘贴 tzz_template_v1 JSON" oninput="syncTemplateImportDraft()">${esc(d.packageJson||'')}</textarea></label><label>用户模板 ID（可选覆盖）<input id="template-import-id" class="input" maxlength="96" value="${esc(d.importedTemplateId||'')}" oninput="syncTemplateImportDraft()"></label><label>显示名称（可选覆盖）<input id="template-import-name" class="input" maxlength="80" value="${esc(d.importedDisplayName||'')}" oninput="syncTemplateImportDraft()"></label><p class="readonly-note">导入只保存为用户模板，不会自动应用、不触发 signal、不执行 action。</p>${errs}${preview}</form>`;
                  const canSaveImport=d.lockId&&!d.saving&&d.preview&&d.previewSnapshot===templateImportDraftSnapshot(d);
                  const footer=`<button class="wa-btn ghost" type="button" onclick="closeWebAdminModal()">${icon('close')}<span>关闭</span></button><button class="wa-btn ghost" type="button" onclick="previewTemplateImport()">${icon('eye')}<span>预览</span></button><button class="wa-btn primary" type="button" ${canSaveImport?'':'disabled'} onclick="saveTemplateImport()">${icon('check-pass')}<span>${d.saving?'保存中...':'保存为用户模板'}</span></button>`;
                  openWebAdminModal('导入模板 JSON',body,footer,{className:'wa-config-modal template-import-modal',syncBeforeClose:()=>syncTemplateImportDraft(),dirtyCheck:()=>modalDraftDirty('template_import',appState.templateImport),onClose:async()=>{await releaseTemplateImportLock(true);appState.templateImport=null;await dismissWebAdminModal();}});
                }
                function syncTemplateImportDraft(){const d=appState.templateImport;if(!d)return;d.packageJson=document.getElementById('template-import-json')?.value??d.packageJson;d.importedTemplateId=document.getElementById('template-import-id')?.value??d.importedTemplateId;d.importedDisplayName=document.getElementById('template-import-name')?.value??d.importedDisplayName;if(d.preview&&d.previewSnapshot!==templateImportDraftSnapshot(d)){d.preview=null;d.previewSnapshot='';}}
                async function acquireTemplateImportLock(){const d=appState.templateImport;if(!d)return;try{const result=await acquireWebAdminEditLock('template_store','user-template-store');if(!result.success){d.errors=[{message:result.message||'无法获取模板库编辑锁'}];showTemplateImportModal();return;}d.lock=result.data?.lock||{};d.lockId=d.lock.lockId||'';scheduleTemplateImportLockHeartbeat();showTemplateImportModal();}catch(err){d.errors=[{message:err.message||'无法获取模板库编辑锁'}];showTemplateImportModal();}}
                async function previewTemplateImport(){const d=appState.templateImport;if(!d)return;syncTemplateImportDraft();d.errors=[];showTemplateImportModal();try{const result=await api('/api/webadmin/templates/import-preview',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(templateImportPayload(d))});if(result.success){d.preview=result.data||{};d.previewSnapshot=templateImportDraftSnapshot(d);toast(result.message||'模板 JSON 预览通过。');}else d.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'导入预览失败'}];showTemplateImportModal();}catch(err){d.errors=[{message:err.message||'导入预览失败'}];showTemplateImportModal();}}
                async function saveTemplateImport(){const d=appState.templateImport;if(!d)return;syncTemplateImportDraft();if(!d.lockId){d.errors=[{message:'模板库编辑锁未就绪。'}];showTemplateImportModal();return;}if(!d.preview||d.previewSnapshot!==templateImportDraftSnapshot(d)){d.errors=[{message:'请先预览当前 JSON，确认无误后再保存。'}];showTemplateImportModal();return;}d.saving=true;d.errors=[];showTemplateImportModal();try{const result=await api('/api/webadmin/templates/import',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(templateImportPayload(d))});if(result.success){stopTemplateImportLockHeartbeat();appState.templateImport=null;await dismissWebAdminModal();toast(result.message||'模板已导入。');await renderTemplatesPage({silent:true});return;}d.saving=false;d.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'导入失败'}];if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code)){d.lockId='';stopTemplateImportLockHeartbeat();}showTemplateImportModal();}catch(err){d.saving=false;d.errors=[{message:err.message||'导入失败'}];showTemplateImportModal();}}
                function templateImportPayload(d){return {packageJson:d.packageJson||'',importedTemplateId:d.importedTemplateId||'',importedDisplayName:d.importedDisplayName||'',expectedFingerprint:d.expectedFingerprint||appState.templateCenter?.expectedFingerprint||'',lockId:d.lockId||''};}
                function templateImportDraftSnapshot(d){return JSON.stringify({packageJson:d.packageJson||'',importedTemplateId:d.importedTemplateId||'',importedDisplayName:d.importedDisplayName||''});}
                function templateImportPreviewHtml(preview){const t=preview.template||{};return `<section class="template-dry-run-preview" data-template-dry-run-preview="true"><h3>导入预览</h3><div class="identity-grid"><div class="k">模板</div><div class="v">${esc(t.displayName||t.templateId||'--')}</div><div class="k">资源</div><div class="v">${esc(t.resourceCount||0)} 项</div><div class="k">行为</div><div class="v">只保存用户模板，不应用配置</div></div></section>`;}
                function openTemplateApplyWizard(source,templateId){
                  appState.templateApply={source:normalizeTemplateSource(source),templateId,detail:null,prefix:'',displayNamePrefix:'',rootChannel:'',placeholderMappings:{},expectedFingerprint:'',lockId:'',lock:null,lockTargetId:'',preview:null,previewSnapshot:'',errors:[],saving:false,loading:true};
                  showTemplateApplyWizard();
                  loadTemplateApplyDetail();
                }
                async function loadTemplateApplyDetail(){const d=appState.templateApply;if(!d)return;try{const detail=await api(`/api/webadmin/templates/detail?source=${encodeURIComponent(d.source)}&id=${encodeURIComponent(d.templateId)}`);d.detail=detail;d.loading=false;d.prefix=normalizeTemplatePrefix(d.templateId.replace(/[^a-z0-9]+/gi,'-'))||'template';d.displayNamePrefix=detail?.displayName?`${detail.displayName} - `:'';d.initialSnapshot=modalSnapshot('template_apply',d);showTemplateApplyWizard();}catch(err){d.loading=false;d.errors=[{message:err.message||'模板详情加载失败'}];showTemplateApplyWizard();}}
                function showTemplateApplyWizard(){
                  const d=appState.templateApply;if(!d)return;markModalInitialSnapshot('template_apply',d);
                  const detail=d.detail||{}, placeholders=detail.placeholders||detail.resources?.placeholders||[], errs=templateErrorsHtml(d.errors), preview=d.preview?templateApplyPreviewHtml(d.preview):'';
                  const lockLine=d.lockId?`<div class="readonly-note">应用锁已获取，锁到期：${esc(formatDateTime(d.lock?.expiresAt))}</div>`:'<div class="readonly-note">先填写参数并执行预览；确认应用前会按前缀获取模板应用锁。</div>';
                  const body=d.loading?loading('正在加载模板...'):`<form class="edit-form template-apply-form" data-template-apply-wizard="true" data-template-placeholder-mapping="true" data-template-no-browser-dialogs="true" onsubmit="event.preventDefault();requestTemplateDryRun()">${lockLine}<label>命名空间前缀<input id="template-apply-prefix" class="input" maxlength="48" value="${esc(d.prefix||'')}" oninput="syncTemplateApplyDraft()"><span class="muted">用于生成频道 / 信号汇合 / 计时器 / 信号监听器 ID；同前缀重复应用会冲突。</span></label><label>显示名前缀<input id="template-apply-display-prefix" class="input" maxlength="32" value="${esc(d.displayNamePrefix||'')}" oninput="syncTemplateApplyDraft()"></label><label>根频道（可选）<input id="template-apply-root-channel" class="input" maxlength="160" value="${esc(d.rootChannel||'')}" oninput="syncTemplateApplyDraft()" placeholder="留空则按前缀创建模板频道"></label>${templatePlaceholderInputs(placeholders,d)}<p class="readonly-note">预览会列出将创建资源、冲突、缺失引用和暂缓资源；应用会重新计算并写入真实配置库。</p>${errs}${preview}</form>`;
                  const canApply=d.preview&&d.preview.data&&d.preview.data.ok&&d.expectedFingerprint&&d.previewSnapshot===templateApplyDraftSnapshot(d)&&!d.saving;
                  const footer=`<button class="wa-btn ghost" type="button" onclick="closeWebAdminModal()">${icon('close')}<span>关闭</span></button><button class="wa-btn ghost" type="button" ${d.loading?'disabled':''} onclick="requestTemplateDryRun()" data-template-dry-run-preview="true">${icon('eye')}<span>预览</span></button><button class="wa-btn primary" type="button" ${canApply?'':'disabled'} onclick="applyTemplateDryRun()">${icon('check-pass')}<span>${d.saving?'应用中...':'确认应用'}</span></button>`;
                  openWebAdminModal('应用模板',body,footer,{className:'wa-config-modal template-apply-modal',syncBeforeClose:()=>syncTemplateApplyDraft(),dirtyCheck:()=>modalDraftDirty('template_apply',appState.templateApply),onClose:async()=>{await releaseTemplateApplyLock(true);appState.templateApply=null;await dismissWebAdminModal();}});
                }
                function templatePlaceholderInputs(placeholders,d){if(!placeholders.length)return '<div class="readonly-note">该模板没有外部占位引用。</div>';return `<section class="template-placeholder-list">${placeholders.map(p=>`<label>${esc(p.displayName||p.id)}<input class="input" data-template-placeholder-id="${esc(p.id)}" value="${esc(d.placeholderMappings?.[p.id]||'')}" oninput="syncTemplateApplyDraft()" placeholder="${esc(templateResourceTypeLabel(p.type||'placeholder'))} ID"><span class="muted">${esc(p.description||'请选择或填写已有资源 ID。')}</span></label>`).join('')}</section>`;}
                function syncTemplateApplyDraft(){const d=appState.templateApply;if(!d)return;const oldTarget=templateApplyLockTargetId(d);d.prefix=document.getElementById('template-apply-prefix')?.value??d.prefix;d.displayNamePrefix=document.getElementById('template-apply-display-prefix')?.value??d.displayNamePrefix;d.rootChannel=document.getElementById('template-apply-root-channel')?.value??d.rootChannel;const mappings={};document.querySelectorAll('[data-template-placeholder-id]').forEach(input=>{mappings[input.dataset.templatePlaceholderId]=input.value||'';});d.placeholderMappings=mappings;const nextTarget=templateApplyLockTargetId(d);if(d.preview&&d.previewSnapshot!==templateApplyDraftSnapshot(d)){d.expectedFingerprint='';d.preview=null;d.previewSnapshot='';}if(d.lockId&&oldTarget!==nextTarget){releaseTemplateApplyLock(true);d.lockId='';d.lock=null;d.lockTargetId='';d.expectedFingerprint='';d.preview=null;d.previewSnapshot='';}}
                async function requestTemplateDryRun(){const d=appState.templateApply;if(!d)return;syncTemplateApplyDraft();d.errors=[];d.expectedFingerprint='';d.preview=null;d.previewSnapshot='';showTemplateApplyWizard();try{const result=await api('/api/webadmin/templates/apply-preview',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(templateApplyPayload(d,false))});d.preview=result;d.expectedFingerprint=result.data?.expectedFingerprint||result.data?.planFingerprint||'';d.previewSnapshot=templateApplyDraftSnapshot(d);toast(result.message||'模板应用预览完成。');showTemplateApplyWizard();}catch(err){d.errors=[{message:err.message||'模板应用预览失败'}];showTemplateApplyWizard();}}
                async function ensureTemplateApplyLock(){const d=appState.templateApply;if(!d)return false;syncTemplateApplyDraft();const targetId=templateApplyLockTargetId(d);if(d.lockId&&d.lockTargetId===targetId)return true;if(d.lockId)await releaseTemplateApplyLock(true);try{const result=await acquireWebAdminEditLock('template_apply',targetId);if(!result.success){d.errors=[{message:result.message||'无法获取模板应用锁'}];showTemplateApplyWizard();return false;}d.lock=result.data?.lock||{};d.lockId=d.lock.lockId||'';d.lockTargetId=targetId;scheduleTemplateApplyLockHeartbeat();showTemplateApplyWizard();return true;}catch(err){d.errors=[{message:err.message||'无法获取模板应用锁'}];showTemplateApplyWizard();return false;}}
                async function applyTemplateDryRun(){const d=appState.templateApply;if(!d)return;syncTemplateApplyDraft();if(!d.preview||!d.preview.data?.ok||d.previewSnapshot!==templateApplyDraftSnapshot(d)){d.errors=[{message:'请先通过当前参数的预览，并修正冲突或缺失引用。'}];showTemplateApplyWizard();return;}if(!await ensureTemplateApplyLock())return;d.saving=true;d.errors=[];showTemplateApplyWizard();try{const result=await api('/api/webadmin/templates/apply',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(templateApplyPayload(d,true))});if(result.success){stopTemplateApplyLockHeartbeat();const target=result.data?.routeTarget||'#/logic-chains';appState.templateApply=null;await dismissWebAdminModal();toast(result.message||'模板已应用。');location.hash=target;return;}d.saving=false;d.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'模板应用失败'}];if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code)){d.lockId='';stopTemplateApplyLockHeartbeat();}showTemplateApplyWizard();}catch(err){d.saving=false;d.errors=[{message:err.message||'模板应用失败'}];showTemplateApplyWizard();}}
                function templateApplyPayload(d,confirmed=false){return {source:d.source,templateId:d.templateId,prefix:normalizeTemplatePrefix(d.prefix),displayNamePrefix:d.displayNamePrefix||'',rootChannel:normalizeChannelName(d.rootChannel),placeholderMappings:d.placeholderMappings||{},expectedFingerprint:d.expectedFingerprint||'',lockId:d.lockId||'',confirmed:!!confirmed};}
                function templateApplyLockTargetId(d){return `${normalizeTemplateSource(d?.source)}:${String(d?.templateId||'')}:${normalizeTemplatePrefix(d?.prefix)}`;}
                function templateApplyDraftSnapshot(d){return JSON.stringify({source:normalizeTemplateSource(d?.source),templateId:String(d?.templateId||''),prefix:normalizeTemplatePrefix(d?.prefix),displayNamePrefix:d?.displayNamePrefix||'',rootChannel:normalizeChannelName(d?.rootChannel),placeholderMappings:d?.placeholderMappings||{}});}
                function templateApplyPreviewHtml(result){const data=result.data||{}, errors=data.validationErrors||result.validationErrors||[];return `<section class="template-dry-run-preview" data-template-dry-run-preview="true"><h3>应用预览</h3><div class="template-preview-grid">${templatePreviewMetric('将创建频道',data.createChannels?.length||0,'active-channel')}${templatePreviewMetric('逻辑链入口',data.createLogicChains?.length||0,'logic-chain')}${templatePreviewMetric('将创建信号汇合',data.createSignalJoins?.length||0,'signal-join')}${templatePreviewMetric('将创建计时器',data.createTimers?.length||0,'timer')}${templatePreviewMetric('将创建信号监听器',data.createSignalListeners?.length||0,'consumer-listener')}${templatePreviewMetric('动作数',data.createActions||0,'action')}${templatePreviewMetric('冲突',data.conflicts?.length||0,'warning-issue')}</div>${templatePreviewList('ID 映射',templateIdMapRows(data.idMap))}${templatePreviewList('逻辑链入口',data.createLogicChains)}${templatePreviewIssueList('冲突',data.conflicts)}${templatePreviewIssueList('缺失引用',data.missingPlaceholders)}${templatePreviewIssueList('暂缓资源',data.deferredResources)}${templatePreviewList('警告',data.warnings)}${errors&&errors.length?templateErrorsHtml(errors):''}</section>`;}
                function templatePreviewMetric(label,value,iconName){return `<div class="template-preview-metric">${icon(iconName)}<span>${esc(label)}</span><strong>${esc(value)}</strong></div>`;}
                function templateIdMapRows(idMap){const rows=[];Object.keys(idMap||{}).forEach(group=>Object.keys(idMap[group]||{}).forEach(id=>rows.push(`${group}: ${id} -> ${idMap[group][id]}`)));return rows;}
                function templatePreviewList(title,items){return items&&items.length?`<div class="template-preview-list"><strong>${esc(title)}</strong>${items.slice(0,18).map(item=>`<span>${esc(templatePreviewItemText(item))}</span>`).join('')}</div>`:'';}
                function templatePreviewItemText(item){return typeof item==='string'?item:(item?.id||item?.channel||item?.name||item?.displayName||JSON.stringify(item));}
                function templatePreviewIssueList(title,items){return items&&items.length?`<div class="template-preview-list warning"><strong>${esc(title)}</strong>${items.map(item=>`<span>${esc(item.message||item.id||JSON.stringify(item))}</span>`).join('')}</div>`:'';}
                function templateErrorsHtml(errors){const list=(errors||[]).filter(Boolean);return list.length?`<ul class="validation-list">${list.map(e=>`<li>${esc(e.message||e)}</li>`).join('')}</ul>`:'';}
                function scheduleTemplateImportLockHeartbeat(){stopTemplateImportLockHeartbeat();appState.templateImportLockTimer=setTimeout(async()=>{await heartbeatTemplateImportLock();if(appState.templateImport?.lockId)scheduleTemplateImportLockHeartbeat();},20000);}
                function stopTemplateImportLockHeartbeat(){if(appState.templateImportLockTimer){clearTimeout(appState.templateImportLockTimer);appState.templateImportLockTimer=null;}}
                async function heartbeatTemplateImportLock(){const d=appState.templateImport;if(!d?.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'template_store',targetId:'user-template-store',lockId:d.lockId})});if(appState.templateImport!==d)return;if(result.success){d.lock=result.data?.lock||d.lock;return;}d.errors=[{message:result.message||'模板库编辑锁续期失败'}];d.lockId='';stopTemplateImportLockHeartbeat();showTemplateImportModal();}catch(err){if(appState.templateImport!==d)return;d.errors=[{message:err.message||'模板库编辑锁续期失败'}];d.lockId='';stopTemplateImportLockHeartbeat();}}
                async function releaseTemplateImportLock(silent=false){const d=appState.templateImport;if(!d?.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'template_store',targetId:'user-template-store',lockId:d.lockId})});}catch(err){if(!silent)toast(err.message||'模板库编辑锁释放失败，将等待自动过期。');}stopTemplateImportLockHeartbeat();}
                function scheduleTemplateApplyLockHeartbeat(){stopTemplateApplyLockHeartbeat();appState.templateApplyLockTimer=setTimeout(async()=>{await heartbeatTemplateApplyLock();if(appState.templateApply?.lockId)scheduleTemplateApplyLockHeartbeat();},20000);}
                function stopTemplateApplyLockHeartbeat(){if(appState.templateApplyLockTimer){clearTimeout(appState.templateApplyLockTimer);appState.templateApplyLockTimer=null;}}
                async function heartbeatTemplateApplyLock(){const d=appState.templateApply;if(!d?.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'template_apply',targetId:d.lockTargetId||templateApplyLockTargetId(d),lockId:d.lockId})});if(appState.templateApply!==d)return;if(result.success){d.lock=result.data?.lock||d.lock;return;}d.errors=[{message:result.message||'模板应用锁续期失败'}];d.lockId='';stopTemplateApplyLockHeartbeat();showTemplateApplyWizard();}catch(err){if(appState.templateApply!==d)return;d.errors=[{message:err.message||'模板应用锁续期失败'}];d.lockId='';stopTemplateApplyLockHeartbeat();}}
                async function releaseTemplateApplyLock(silent=false){const d=appState.templateApply;if(!d?.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'template_apply',targetId:d.lockTargetId||templateApplyLockTargetId(d),lockId:d.lockId})});}catch(err){if(!silent)toast(err.message||'模板应用锁释放失败，将等待自动过期。');}stopTemplateApplyLockHeartbeat();}

                """).toString();
    }
}
