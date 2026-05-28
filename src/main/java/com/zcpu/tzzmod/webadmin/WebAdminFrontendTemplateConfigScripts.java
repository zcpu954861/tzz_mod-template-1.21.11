package com.zcpu.tzzmod.webadmin;

final class WebAdminFrontendTemplateConfigScripts {
    private WebAdminFrontendTemplateConfigScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                async function renderActionTemplatesPage(options={}){
                  if(!options.silent)setView(loading('正在加载动作模板...'));
                  let actions;try{actions=await api('/api/actions')}catch(err){if(options.silent){toast('动作模板实时刷新失败，已保留当前页面。');return;}actions=[];}
                  appState.actions=Array.isArray(actions)?actions:[];
                  appState.actionTemplates=deriveActionTemplates(appState.actions);
                  renderActionTemplateList('',options);
                }
                function deriveActionTemplates(actions){
                  const source=Array.isArray(actions)?actions:[];
                  return source.map((a,index)=>{const type=String(a.type||'UNKNOWN').toUpperCase(), title=a.name||`${labelActionType(type)} 模板 ${index+1}`;return {id:`template:${a.id||index}`,name:title,type,actionCount:Math.max(1,Number(a.referencedByCount||a.actionCount||1)),description:cleanActionSummary(a.summary||`${labelActionType(type)} 动作的只读模板候选。`),favorite:Number(a.executionCount||a.executionCountToday||0)>0,status:actionAvailable(a)?'ENABLED':'DISABLED',createdAt:a.createdAt||'',updatedAt:a.updatedAt||a.lastExecutedAt||'',usageCount:Number(a.executionCount||a.executionCountToday||0),ownerType:a.ownerType||'ACTION',sourceActionId:a.id||'',channel:a.channel||'',doctorStatus:a.doctorStatus||'UNKNOWN'};});
                }
                function renderActionTemplateList(focusId,options={}){
                  waEnsureState();
                  const templates=appState.actionTemplates||[], filtered=filterActionTemplates(templates), page=waPageItems('actionTemplates',filtered,10);
                  const enabled=templates.filter(t=>t.status==='ENABLED').length, favorites=templates.filter(t=>t.favorite).length, today=sumNumeric(templates,['usageCount']);
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('动作模板','从现有 Action 只读数据派生模板候选；不创建、不编辑、不删除真实模板。',`${waButton('添加模板','plus','disabled','primary')}${waButton('导入模板','upload','disabled','ghost')}${waButton('导出配置','download','disabled','ghost')}${waButton('批量操作','more','disabled','danger')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('模板总数',templates.length,'来自 /api/actions 只读派生','action-template')}
                      ${waMetric('启用中',enabled,'Doctor OK / 可用动作','enabled','ok')}
                      ${waMetric('禁用中',templates.length-enabled,'需要关注或不可用','receiver-disabled',templates.length-enabled?'warning':'')}
                      ${waMetric('今日使用次数',today,'API 未提供时显示 --','today-trigger')}
                      ${waMetric('收藏模板',favorites,'当前按使用记录标记','active-channel')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="template-search" placeholder="搜索模板名称 / ID / 类型 / 描述..." value="${esc(appState.templateFilters.search)}"></label>
                          <label class="filter-field"><span>类型</span>${waSelect('template-type',['ALL',...uniqueNonBlank(templates.map(t=>t.type))],appState.templateFilters.type,templateOptionLabel)}</label>
                          <label class="filter-field"><span>状态</span>${waSelect('template-status',['ALL','ENABLED','DISABLED'],appState.templateFilters.status,templateOptionLabel)}</label>
                          <label class="filter-field"><span>收藏</span>${waSelect('template-favorite',['ALL','FAVORITE','NORMAL'],appState.templateFilters.favorite,templateOptionLabel)}</label>
                          ${waButton('刷新','refresh','onclick="renderActionTemplatesPage()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(templates.length===0?'当前没有可从现有 Action 派生的模板候选。':'没有匹配当前筛选条件的动作模板。'):actionTemplateTable(page.items)}
                        ${waPagination('actionTemplates',page)}
                      </div>
                      ${actionTemplateRightRail(templates,filtered)}
                    </section>
                  </section>`,options);
                  if(rendered)bindActionTemplateFilters(focusId);
                }
                function actionTemplateTable(items){return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>模板名称 / ID</th><th>类型</th><th>动作数量</th><th>描述</th><th>收藏</th><th>状态</th><th>创建时间</th><th>最后更新</th><th>操作</th></tr></thead><tbody>${items.map(t=>`<tr><td><span class="device-name"><span class="device-icon">${icon('action-template')}</span><span><strong>${esc(t.name)}</strong><span class="device-subtitle">ID: ${esc(shortId(t.id))}</span></span></span></td><td>${textPill(labelActionType(t.type),actionTypeTone(t.type))}</td><td>${esc(t.actionCount)}</td><td class="truncate" title="${esc(t.description||'')}">${esc(t.description||'--')}</td><td>${textPill(t.favorite?'已收藏':'未收藏',t.favorite?'ok':'info')}</td><td>${textPill(t.status==='ENABLED'?'启用':'禁用',t.status==='ENABLED'?'ok':'warning')} ${pill(t.doctorStatus||'UNKNOWN')}</td><td>${fmtTime(t.createdAt)}</td><td>${fmtTime(t.updatedAt)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" disabled>使用</button>${t.sourceActionId?`<button class="wa-btn ghost" ${navDataAttr(actionHash(t.sourceActionId),`查看来源动作 ${t.name}`)}>来源</button>`:`<button class="wa-btn ghost" disabled>来源</button>`}${waIconButton('编辑不可用','settings','disabled')}</div></td></tr>`).join('')}</tbody></table></div>`;}
                function filterActionTemplates(items){const f=appState.templateFilters||{};const filtered=(items||[]).filter(t=>{const hay=[t.id,t.name,t.type,t.description,t.ownerType,t.channel].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.type&&f.type!=='ALL'&&String(t.type)!==f.type)return false;if(f.status==='ENABLED'&&t.status!=='ENABLED')return false;if(f.status==='DISABLED'&&t.status!=='DISABLED')return false;if(f.favorite==='FAVORITE'&&!t.favorite)return false;if(f.favorite==='NORMAL'&&t.favorite)return false;return true;});return filtered.sort((a,b)=>{if(f.sort==='TYPE')return String(a.type).localeCompare(String(b.type))||String(a.name).localeCompare(String(b.name));if(f.sort==='UPDATED')return String(b.updatedAt||'').localeCompare(String(a.updatedAt||''));return String(a.name).localeCompare(String(b.name));});}
                function bindActionTemplateFilters(focusId){const update=(event)=>{appState.templateFilters.search=document.getElementById('template-search')?.value||'';appState.templateFilters.type=document.getElementById('template-type')?.value||'ALL';appState.templateFilters.status=document.getElementById('template-status')?.value||'ALL';appState.templateFilters.favorite=document.getElementById('template-favorite')?.value||'ALL';appState.uiPages.actionTemplates=1;renderActionTemplateList(event?.target?.id||'');};['template-search','template-type','template-status','template-favorite'].forEach(id=>document.getElementById(id)?.addEventListener(id==='template-search'?'input':'change',update));restoreFocusEnd(focusId);}
                """)
.append("""
                function actionTemplateRightRail(items,filtered){const total=Math.max(1,items.length);return `<aside class="wa-right-rail"><article class="wa-panel"><h2>类型分布</h2>${progressList(distributionItems(filtered,t=>String(t.type||'UNKNOWN'),labelActionType,Math.max(1,filtered.length)))}</article><article class="wa-panel"><h2>使用频率</h2>${progressList([{label:'已有使用记录',value:items.filter(t=>Number(t.usageCount||0)>0).length,total,kind:'ok'},{label:'暂无使用记录',value:items.filter(t=>Number(t.usageCount||0)<=0).length,total,kind:'info'},{label:'需关注',value:items.filter(t=>t.status!=='ENABLED').length,total,kind:'warning'}])}</article><article class="wa-panel"><h2>快速筛选</h2><div class="wa-rail-filter"><label><span>模板类型</span>${waSelect('template-rail-type',['ALL',...uniqueNonBlank(items.map(t=>t.type))],appState.templateFilters.type,templateOptionLabel)}</label><div class="wa-button-row"><button class="wa-btn primary" onclick="appState.templateFilters.type=document.getElementById('template-rail-type').value;appState.uiPages.actionTemplates=1;renderActionTemplateList()">应用筛选</button><button class="wa-btn ghost" onclick="appState.templateFilters={search:'',type:'ALL',status:'ALL',favorite:'ALL',sort:'NAME'};appState.uiPages.actionTemplates=1;renderActionTemplateList()">重置筛选</button></div></div></article><article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('添加模板','plus','disabled','primary')}${waButton('使用模板','play','disabled','ghost')}${waButton('导入模板','upload','disabled','ghost')}${waButton('导出配置','download','disabled','ghost')}</div><p class="wa-disabled-note">模板创建、使用、导入、导出和批量操作尚无完整后端写入链路，本轮保持禁用且不发送写请求。</p></article></aside>`;}
                function templateOptionLabel(value){return {ALL:'全部',ENABLED:'启用',DISABLED:'禁用',FAVORITE:'收藏',NORMAL:'未收藏'}[String(value||'')]||labelActionType(value);}
                async function renderActionsPage(options={}){
                  if(!options.silent)setView(loading('正在加载动作列表...'));
                  let actions;try{actions=await api('/api/actions')}catch(err){if(options.silent){toast('动作列表实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.actions=actions||[];
                  renderActionList('',options);
                }
                function actionAvailable(a){return String(a.doctorStatus||'UNKNOWN').toUpperCase()==='OK';}
                function renderActionList(focusId,options={}){
                  waEnsureState();
                  const actions=appState.actions||[], filtered=filterActions(actions), page=waPageItems('actions',filtered,10), ownerTypes=uniqueNonBlank(actions.map(a=>String(a.ownerType||'UNKNOWN').toUpperCase()));
                  const ok=waCount(actions,actionAvailable), failed=waCount(actions,a=>String(a.lastResult||'').toUpperCase()==='FAILED'), today=sumNumeric(actions,['executionCountToday','todayExecutionCount']);
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('动作列表','ActionEngine 动作只读展示；不包含动作模板或动作编辑器。',`${waButton('添加动作','plus','disabled','primary')}${waButton('批量导入','upload','disabled','ghost')}${waButton('导出配置','download','disabled','danger')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('动作总数',actions.length,'来自 /api/actions','action-total')}
                      ${waMetric('可用动作',ok,'Doctor OK','enabled','ok')}
                      ${waMetric('需关注',actions.length-ok,'Doctor 非 OK','warning-issue',actions.length-ok?'warning':'')}
                      ${waMetric('今日执行次数',today,'API 未提供时显示 --','today-trigger')}
                      ${waMetric('失败次数',failed,'最近结果 FAILED','critical-issue',failed?'error':'')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="action-search" placeholder="搜索动作名称 / ID / 类型 / owner / channel..." value="${esc(appState.actionFilters.search)}"></label>
                          <label class="filter-field"><span>类型</span>${waSelect('action-type',['ALL','COMMAND','MESSAGE','SOUND','SIGNAL','STATE_VARIABLE','TIMER_START','TIMER_CANCEL','UNKNOWN'],appState.actionFilters.type,actionOptionLabel)}</label>
                          <label class="filter-field"><span>归属</span>${waSelect('action-owner',['ALL',...ownerTypes],appState.actionFilters.owner,actionOptionLabel)}</label>
                          <label class="filter-field"><span>结果</span>${waSelect('action-result',['ALL','SUCCESS','FAILED','UNKNOWN'],appState.actionFilters.result,actionOptionLabel)}</label>
                          <label class="filter-field"><span>排序</span>${waSelect('action-sort',['NAME','TYPE','OWNER','RECENT'],appState.actionFilters.sort,actionOptionLabel)}</label>
                          ${waButton('刷新','refresh','onclick="renderActionsPage()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(actions.length===0?'当前暂无动作数据。':'没有匹配当前筛选条件的动作。'):actionTable(page.items)}
                        ${waPagination('actions',page)}
                      </div>
                      ${actionRightRail(actions)}
                    </section>
                  </section>`,options);
                  if(rendered)bindActionFilters(focusId);
                }
                function actionTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>动作名称 / ID</th><th>类型</th><th>描述</th><th>状态</th><th>标签</th><th>最近执行</th><th>操作</th></tr></thead><tbody>${items.map(a=>{const target=actionHash(a.id), title=a.name||a.id;return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看动作 ${title}`)}><td><span class="device-name"><span class="device-icon">${icon(actionIcon(a.type))}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(a.id))}</span></span></span></td><td>${textPill(labelActionType(a.type),actionTypeTone(a.type))}</td><td class="truncate" title="${esc(cleanActionSummary(a.summary||''))}">${esc(cleanActionSummary(a.summary||'--'))}</td><td>${pill(a.doctorStatus||'UNKNOWN')} ${esc(actionAvailable(a)?'可用':'需关注')}</td><td><span class="pill info">${esc(labelOwnerType(a.ownerType))}</span> ${isBlank(a.channel)?'<span class="muted">无频道</span>':channelCell(a.channel)}</td><td>${fmtTime(a.lastExecutedAt)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看动作 ${title}`)}>查看</button><button class="wa-btn ghost" disabled>编辑</button>${waIconButton('更多','more','disabled')}</div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function actionIcon(type){return {COMMAND:'settings',MESSAGE:'history',SOUND:'pulse-duration',SIGNAL:'signalbridge-main',STATE_VARIABLE:'state-variable',TIMER_START:'timer-start',TIMER_CANCEL:'timer-cancel',UNKNOWN:'action'}[String(type||'UNKNOWN').toUpperCase()]||'action';}
                function actionTypeTone(type){return {COMMAND:'ok',MESSAGE:'info',SOUND:'warning',SIGNAL:'',STATE_VARIABLE:'info',TIMER_START:'info',TIMER_CANCEL:'warning'}[String(type||'').toUpperCase()]||'info';}
                function bindActionFilters(focusId){
                  const update=(event)=>{appState.actionFilters.search=document.getElementById('action-search')?.value||'';appState.actionFilters.type=document.getElementById('action-type')?.value||'ALL';appState.actionFilters.owner=document.getElementById('action-owner')?.value||'ALL';appState.actionFilters.result=document.getElementById('action-result')?.value||'ALL';appState.actionFilters.sort=document.getElementById('action-sort')?.value||'NAME';appState.uiPages.actions=1;renderActionList(event?.target?.id||'');};
                  ['action-search','action-type','action-owner','action-result','action-sort'].forEach(id=>document.getElementById(id)?.addEventListener(id==='action-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                function actionRightRail(actions){
                  const total=actions.length, ok=waCount(actions,actionAvailable);
                  return `<aside class="wa-right-rail">
                    <article class="wa-panel"><h2>动作类型分布</h2>${progressList(distributionItems(actions,a=>String(a.type||'UNKNOWN').toUpperCase(),labelActionType,total))}</article>
                    <article class="wa-panel"><h2>状态分布</h2>${progressList([{label:'可用动作',value:ok,total,kind:'ok'},{label:'需关注',value:total-ok,total,kind:'warning'}])}</article>
                    <article class="wa-panel"><h2>快速筛选</h2><div class="wa-rail-filter"><label><span>类型</span>${waSelect('action-rail-type',['ALL','COMMAND','MESSAGE','SOUND','SIGNAL','STATE_VARIABLE','UNKNOWN'],appState.actionFilters.type,actionOptionLabel)}</label><label><span>标签搜索</span><input class="input" id="action-rail-search" placeholder="搜索 owner / channel..." value="${esc(appState.actionFilters.search)}"></label><div class="wa-button-row"><button class="wa-btn primary" onclick="appState.actionFilters.type=document.getElementById('action-rail-type').value;appState.actionFilters.search=document.getElementById('action-rail-search').value;appState.uiPages.actions=1;renderActionList()">应用筛选</button><button class="wa-btn ghost" onclick="appState.actionFilters={search:'',type:'ALL',owner:'ALL',result:'ALL',doctor:'ALL',sort:'NAME'};appState.uiPages.actions=1;renderActionList()">重置筛选</button></div></div></article>
                    <article class="wa-panel"><h2>动作类型说明</h2><div class="list-stack"><div class="kv-row"><span class="muted">command</span><strong>执行服务器命令</strong></div><div class="kv-row"><span class="muted">message</span><strong>发送消息</strong></div><div class="kv-row"><span class="muted">sound</span><strong>播放音效</strong></div><div class="kv-row"><span class="muted">signal</span><strong>发送下游 signal</strong></div><div class="kv-row"><span class="muted">state_variable</span><strong>写入状态变量</strong></div><div class="kv-row"><span class="muted">timer_start / timer_cancel</span><strong>启动或取消 Scheduler Timer</strong></div><div class="kv-row"><span class="muted">unknown</span><strong>聚合或不可识别动作</strong></div></div></article>
                  </aside>`;
                }
                function filterHistoryItems(items){
                  const f=appState.historyFilters||{}, now=Date.now();
                  const filtered=(Array.isArray(items)?items:[]).filter(h=>{
                    const hay=[h.channel,h.sourceType,h.sourceName,h.sourceId,h.result,h.world,h.playerName,h.description].join(' ').toLowerCase();
                    if(f.search&&!hay.includes(String(f.search).toLowerCase()))return false;
                    if(f.channel&&f.channel!=='ALL'&&h.channel!==f.channel)return false;
                    if(f.sourceType&&f.sourceType!=='ALL'&&String(h.sourceType||'UNKNOWN').toUpperCase()!==f.sourceType)return false;
                    if(f.result&&f.result!=='ALL'&&String(h.result||'UNKNOWN').toUpperCase()!==f.result)return false;
                    if(f.range&&f.range!=='ALL'){
                      const t=parseTime(h.time);if(!t)return false;
                      const age=now-t.getTime();
                      if(f.range==='M10'&&age>10*60*1000)return false;
                      if(f.range==='H1'&&age>60*60*1000)return false;
                      if(f.range==='H24'&&age>24*60*60*1000)return false;
                    }
                    return true;
                  });
                  return filtered.sort((a,b)=>{
                    const at=parseTime(a.time)?.getTime()||0, bt=parseTime(b.time)?.getTime()||0;
                    if(f.sort==='OLDEST')return at-bt;
                    return bt-at;
                  });
                }
                function historyOptionLabel(id,value){
                  const v=String(value||'');
                  if(v==='ALL')return '全部';
                  if(id==='history-source')return labelSourceType(v);
                  if(id==='history-result')return {SUCCESS:'成功',FAILED:'失败',UNKNOWN:'未知'}[v]||labelStatus(v);
                  if(id==='history-range')return {M10:'最近 10 分钟',H1:'最近 1 小时',H24:'最近 24 小时'}[v]||v;
                  return v;
                }
                async function renderHistoryPage(queryTail='',options={}){
                  const params=parseHashParams(queryTail);if(params.channel)appState.historyFilters.channel=params.channel;
                  if(!options.silent)setView(loading('正在加载事件历史...'));
                  let history;try{history=await api('/api/signals/history?limit=500')}catch(err){if(options.silent){toast('事件历史实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.historyItems=history||[];
                  renderHistoryListPage('',options);
                }
                function renderHistoryListPage(focusId,options={}){
                  waEnsureState();
                  const items=appState.historyItems||[], filtered=filterHistoryItems(items), page=waPageItems('history',filtered,10), channels=uniqueNonBlank(items.map(h=>h.channel)), sourceTypes=uniqueNonBlank(items.map(h=>h.sourceType));
                  const todayStart=new Date();todayStart.setHours(0,0,0,0);const today=items.filter(h=>{const t=parseTime(h.time);return t&&t.getTime()>=todayStart.getTime();}).length, success=waCount(items,h=>String(h.result||'').toUpperCase()==='SUCCESS'), failed=waCount(items,h=>String(h.result||'').toUpperCase()==='FAILED');
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('事件历史','统一查看当前已有 Signal history；Live 后端未完整接入时保持不可用。',waButton('刷新','refresh','onclick="renderHistoryPage()"','ghost'))}
                    <div class="wa-tabs wa-tabs-scroll"><button class="wa-tab active">事件列表</button><button class="wa-tab" disabled>实时流 Live</button></div>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="history-search" placeholder="搜索频道 / 来源 / 对象 / 结果..." value="${esc(appState.historyFilters.search)}"></label>
                          <label class="filter-field"><span>频道</span>${waSelect('history-channel',['ALL',...channels],appState.historyFilters.channel,v=>v==='ALL'?'全部频道':v)}</label>
                          <label class="filter-field"><span>来源</span>${waSelect('history-source',['ALL',...sourceTypes],appState.historyFilters.sourceType,historyOptionLabel.bind(null,'history-source'))}</label>
                          <label class="filter-field"><span>结果</span>${waSelect('history-result',['ALL','SUCCESS','FAILED','UNKNOWN'],appState.historyFilters.result,historyOptionLabel.bind(null,'history-result'))}</label>
                          <label class="filter-field"><span>时间</span>${waSelect('history-range',['ALL','M10','H1','H24'],appState.historyFilters.range,historyOptionLabel.bind(null,'history-range'))}</label>
                          ${waButton('刷新','refresh','onclick="renderHistoryPage()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(items.length===0?'暂无事件历史。':'没有匹配当前筛选条件的事件。'):historyTable(page.items)}
                        ${waPagination('history',page)}
                      </div>
                      ${historyRightRail(items,filtered,today,success,failed)}
                    </section>
                  </section>`,options);
                  if(rendered)bindHistoryFilters(focusId);
                }
                function historyTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>时间</th><th>频道</th><th>来源类型</th><th>来源对象</th><th>动作数</th><th>结果</th><th>操作</th></tr></thead><tbody>${items.map(h=>`<tr><td>${fmtTime(h.time)}</td><td class="truncate" title="${esc(h.channel||'')}">${channelCell(h.channel)}</td><td>${textPill(labelSourceType(h.sourceType),'info')}</td><td><span class="wa-source-object"><strong>${esc(h.sourceName||h.sourceId||'--')}</strong><span class="device-subtitle">${esc([h.world,posText(h.pos),h.playerName].filter(v=>!isBlank(v)&&v!=='-').join(' / ')||h.description||'--')}</span></span></td><td>--</td><td>${pill(h.result||'UNKNOWN')}</td><td><div class="wa-action-cell">${historyAction(h)}${waIconButton('详情不可用','more','disabled')}</div></td></tr>`).join('')}</tbody></table></div>`;
                }
                function bindHistoryFilters(focusId){
                  const update=(event)=>{appState.historyFilters.search=document.getElementById('history-search')?.value||'';appState.historyFilters.channel=document.getElementById('history-channel')?.value||'ALL';appState.historyFilters.sourceType=document.getElementById('history-source')?.value||'ALL';appState.historyFilters.result=document.getElementById('history-result')?.value||'ALL';appState.historyFilters.range=document.getElementById('history-range')?.value||'ALL';appState.uiPages.history=1;renderHistoryListPage(event?.target?.id||'');};
                  ['history-search','history-channel','history-source','history-result','history-range'].forEach(id=>document.getElementById(id)?.addEventListener(id==='history-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                function historyRightRail(items,filtered,today,success,failed){
                  const total=items.length;
                  return `<aside class="wa-right-rail">
                    <article class="wa-panel"><h2>今日事件统计</h2><div class="summary-grid">${waMetric('今日事件数',today,'本地日期','recent-event')}${waMetric('总触发次数',total,'当前内存历史','today-trigger')}${waMetric('成功',success,'result=SUCCESS','check-pass','ok')}${waMetric('失败',failed,'result=FAILED','critical-issue',failed?'error':'')}</div></article>
                    <article class="wa-panel"><h2>来源类型</h2>${progressList(distributionItems(filtered,h=>String(h.sourceType||'UNKNOWN').toUpperCase(),labelSourceType,Math.max(1,filtered.length)))}</article>
                    <article class="wa-panel"><h2>操作</h2><div class="wa-quick-grid">${waButton('实时流 Live','signalbridge-main','disabled','ghost')}${waButton('导出事件记录','download','disabled','ghost')}${waButton('清空历史记录','channel-error','disabled','danger')}</div><p class="wa-disabled-note">导出、清空和完整实时流没有后端写入或流式能力，本轮保持禁用。</p></article>
                  </aside>`;
                }
                """)
.append("""
                async function renderConfigPage(options={}){
                  if(!options.silent)setView(loading('正在加载配置管理...'));
                  const [settings,status,capabilities]=await Promise.all([settle('/api/webadmin/settings'),settle('/api/status'),settle('/api/webadmin/write/capabilities')]);
                  if(!settings.ok){if(options.silent){toast('配置管理实时刷新失败，已保留当前页面。');return;}setView(errorBlock(settings.error.message));return;}
                  appState.configData={settings:settings.data||{},status:status.ok?status.data:{},capabilities:capabilities.ok?capabilities.data:{}};
                  renderConfigList('',options);
                }
                function renderConfigList(focusId,options={}){
                  waEnsureState();
                  const data=appState.configData||{}, settings=data.settings||{}, storage=settings.storage||{}, service=settings.service||{}, rows=filterConfigRows(configRows(data));
                  const page=waPageItems('config',rows,10), warning=rows.filter(r=>r.status!=='OK').length;
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('配置管理','只读查看 WebAdmin 配置文件、存储状态与写入边界；不实现发布、回滚或版本系统。',`${waButton('新建草稿','plus','disabled','primary')}${waButton('导入配置包','upload','disabled','ghost')}${waButton('导出当前配置','download','disabled','ghost')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('当前配置版本',storage.configExists?'当前文件':'未检测到','来自 /api/webadmin/settings','settings')}
                      ${waMetric('草稿数量','--','无配置版本系统','archive')}
                      ${waMetric('备份数量','--','无备份列表 API','archive')}
                      ${waMetric('未发布变更','--','无发布流程 API','warning-issue',warning?'warning':'')}
                      ${waMetric('最近发布时间','--','当前后端未提供','clock')}
                      ${waMetric('配置警告',warning,'路径隐藏或文件缺失','warning-issue',warning?'warning':'')}
                    </section>
                    <div class="wa-tabs wa-tabs-scroll"><button class="wa-tab active">配置版本列表</button><button class="wa-tab" disabled>草稿</button><button class="wa-tab" disabled>发布记录</button><button class="wa-tab" disabled>备份记录</button><button class="wa-tab" disabled>导入记录</button><button class="wa-tab" disabled>变更对比 Diff</button><button class="wa-tab" disabled>审计日志</button></div>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="config-search" placeholder="搜索配置项 / 路径 / 类型..." value="${esc(appState.configFilters.search)}"></label>
                          <label class="filter-field"><span>类型</span>${waSelect('config-type',['ALL','SERVICE','STORAGE','SECURITY','USERS','AUDIT'],appState.configFilters.type,configOptionLabel)}</label>
                          <label class="filter-field"><span>状态</span>${waSelect('config-status',['ALL','OK','WARNING','DISABLED'],appState.configFilters.status,configOptionLabel)}</label>
                          ${waButton('刷新','refresh','onclick="renderConfigPage()"','ghost')}
                        </div>
                        ${page.items.length===0?empty('没有匹配当前筛选条件的配置项。'):configTable(page.items)}
                        ${waPagination('config',page)}
                      </div>
                      ${configRightRail(data,rows,service)}
                    </section>
                  </section>`,options);
                  if(rendered)bindConfigFilters(focusId);
                }
                function configRows(data){
                  const settings=data.settings||{}, storage=settings.storage||{}, service=settings.service||{}, security=settings.security||{}, audit=settings.audit||{}, system=settings.system||{};
                  const hidden=storage.restricted?'受限信息已隐藏':'';
                  return [
                    {name:'WebAdmin 服务配置',type:'SERVICE',status:service.running?'OK':'WARNING',path:`${service.host||'-'}:${service.port||'-'}`,desc:`访问模式：${labelAccessMode(service.accessMode)}`},
                    {name:'当前配置文件',type:'STORAGE',status:storage.configExists?'OK':'WARNING',path:hidden||storage.configPath||'--',desc:`存储作用域：${storage.scope||'WORLD_SAVE'}`},
                    {name:'用户配置文件',type:'USERS',status:storage.usersExists?'OK':'WARNING',path:hidden||storage.usersPath||'--',desc:'WebAdmin 用户与角色存储文件'},
                    {name:'审计日志文件',type:'AUDIT',status:audit.enabled?(storage.auditLogExists?'OK':'WARNING'):'DISABLED',path:hidden||storage.auditLogPath||'--',desc:`审计日志：${audit.enabled?'启用':'关闭'}`},
                    {name:'认证与 Session',type:'SECURITY',status:'OK',path:security.sessionCookieName||'--',desc:`${security.authMode||'USERNAME_PASSWORD'} / ${formatMinutes(security.sessionTtlMinutes)}`},
                    {name:'运行环境摘要',type:'SERVICE',status:'OK',path:system.worldName||'--',desc:`${system.minecraftVersion||'--'} / ${system.serverType||'--'}`}
                  ];
                }
                function filterConfigRows(rows){const f=appState.configFilters;return (rows||[]).filter(r=>{const hay=[r.name,r.type,r.status,r.path,r.desc].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.type!=='ALL'&&r.type!==f.type)return false;if(f.status!=='ALL'&&r.status!==f.status)return false;return true;});}
                function bindConfigFilters(focusId){
                  const update=(event)=>{appState.configFilters.search=document.getElementById('config-search')?.value||'';appState.configFilters.type=document.getElementById('config-type')?.value||'ALL';appState.configFilters.status=document.getElementById('config-status')?.value||'ALL';appState.uiPages.config=1;renderConfigList(event?.target?.id||'');};
                  ['config-search','config-type','config-status'].forEach(id=>document.getElementById(id)?.addEventListener(id==='config-search'?'input':'change',update));
                  restoreFocusEnd(focusId);
                }
                function configTable(items){return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>配置项</th><th>类型</th><th>状态</th><th>位置 / 标识</th><th>说明</th><th>操作</th></tr></thead><tbody>${items.map(r=>`<tr><td><span class="device-name"><span class="device-icon">${icon(configIcon(r.type))}</span><span><strong>${esc(r.name)}</strong><span class="device-subtitle">${esc(r.type)}</span></span></span></td><td>${textPill(configOptionLabel(r.type),'info')}</td><td>${pill(r.status)}</td><td class="truncate" title="${esc(r.path)}">${esc(r.path)}</td><td class="truncate" title="${esc(r.desc)}">${esc(r.desc)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" disabled>Diff</button>${waIconButton('更多不可用','more','disabled')}</div></td></tr>`).join('')}</tbody></table></div>`;}
                function configRightRail(data,rows,service){const settings=data.settings||{}, storage=settings.storage||{};return `<aside class="wa-right-rail">
                  <article class="wa-panel"><h2>配置状态</h2><div class="list-stack">${rows.map(r=>`<div class="kv-row"><span class="muted">${esc(r.name)}</span><strong>${pill(r.status)}</strong></div>`).join('')}</div></article>
                  <article class="wa-panel"><h2>最近发布记录</h2>${empty('当前没有配置发布记录 API。')}</article>
                  <article class="wa-panel"><h2>最近备份</h2>${empty('当前没有备份列表 API。')}</article>
                  <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('创建备份','archive','disabled','ghost')}${waButton('查看 Diff','eye','disabled','ghost')}${waButton('发布','enabled','disabled','primary')}${waButton('回滚到此版本','warning-issue','disabled','danger')}</div><p class="wa-disabled-note">发布、回滚、导入覆盖和删除没有完整后端安全能力，本轮保持禁用。</p></article>
                  <article class="wa-panel"><h2>存储边界</h2><div class="list-stack"><div class="kv-row"><span class="muted">作用域</span><strong>${esc(storage.scope||'WORLD_SAVE')}</strong></div><div class="kv-row"><span class="muted">按世界隔离</span><strong>${esc(storage.worldScoped?'是':'否')}</strong></div><div class="kv-row"><span class="muted">当前服务</span><strong>${esc(service.url||'--')}</strong></div></div></article>
                </aside>`;}
                function configOptionLabel(value){return {ALL:'全部',SERVICE:'服务',STORAGE:'存储',SECURITY:'安全',USERS:'用户',AUDIT:'审计',OK:'正常',WARNING:'需关注',DISABLED:'禁用'}[String(value||'')]||value;}
                function configIcon(type){return {SERVICE:'settings',STORAGE:'archive',SECURITY:'doctor-ok',USERS:'user-total',AUDIT:'history'}[String(type||'')]||'settings';}
                function restoreFocusEnd(focusId){if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}}
                async function renderUsersPage(options={}){
                  if(!options.silent)setView(loading('正在加载用户与权限...'));
                  let data;try{data=await api('/api/webadmin/users')}catch(err){if(options.silent){toast('用户与权限实时刷新失败，已保留当前页面。');return;}setView(`<section class="wa-page">${waPageHead('用户与权限','查看 WebAdmin 用户、角色、权限与 session 状态。',`${waButton('添加用户','plus','disabled','primary')}${waButton('角色管理','user','disabled','ghost')}${waButton('权限策略管理','critical-issue','disabled','danger')}`)}${err.status===403?errorBlock('权限不足：只有所有者可以查看用户与权限。'):errorBlock(err.message)}</section>`);return;}
                  appState.usersData=data||{summary:{},users:[],roles:[]};
                  renderUserList('',options);
                }
                function renderUserList(focusId,options={}){
                  waEnsureState();
                  const data=appState.usersData||{summary:{},users:[],roles:[]}, users=data.users||[], summary=data.summary||{}, filtered=filterUsersStep3(users), page=waPageItems('users',filtered,10);
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('用户与权限','查看 WebAdmin 用户、角色、权限与登录状态；不启用用户写操作。',`${waButton('添加用户','plus','disabled','primary')}${waButton('角色管理','user','disabled','ghost')}${waButton('权限策略管理','critical-issue','disabled','danger')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('用户总数',summary.totalCount ?? users.length,'来自 /api/webadmin/users','user-total')}
                      ${waMetric('在线用户',summary.onlineCount ?? users.filter(u=>u.online).length,'当前 session','current-user','ok')}
                      ${waMetric('角色数量',(data.roles||[]).length || 4,'OWNER / EDITOR / TESTER / VIEWER','current-role')}
                      ${waMetric('今日登录次数','--','后端未提供登录统计','today-trigger')}
                      ${waMetric('今日操作次数','--','审计聚合未提供','action-total')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="user-search" placeholder="搜索用户名 / UID / 角色..." value="${esc(appState.userFilters.search)}"></label>
                          <label class="filter-field"><span>角色</span>${waSelect('user-role',['ALL','OWNER','EDITOR','TESTER','VIEWER'],appState.userFilters.role,userOptionLabel)}</label>
                          <label class="filter-field"><span>状态</span>${waSelect('user-enabled',['ALL','ENABLED','DISABLED'],appState.userFilters.enabled,userOptionLabel)}</label>
                          <label class="filter-field"><span>在线</span>${waSelect('user-online',['ALL','ONLINE','OFFLINE'],appState.userFilters.online,userOptionLabel)}</label>
                          ${waButton('刷新','refresh','onclick="renderUsersPage()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(users.length?'没有匹配当前筛选条件的用户。':'暂无 WebAdmin 用户。'):userTableStep3(page.items)}
                        ${waPagination('users',page)}
                      </div>
                      ${userRightRail(data,users)}
                    </section>
                  </section>`,options);
                  if(rendered)bindUserFiltersStep3(focusId);
                }
                function filterUsersStep3(users){const f=appState.userFilters;return (users||[]).filter(u=>{const hay=[u.username,u.displayName,u.role,u.roleDisplayName,u.createdBy].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.role!=='ALL'&&String(u.role||'').toUpperCase()!==f.role)return false;if(f.enabled==='ENABLED'&&!u.enabled)return false;if(f.enabled==='DISABLED'&&u.enabled)return false;if(f.online==='ONLINE'&&!u.online)return false;if(f.online==='OFFLINE'&&u.online)return false;return true;});}
                function bindUserFiltersStep3(focusId){const update=(event)=>{appState.userFilters.search=document.getElementById('user-search')?.value||'';appState.userFilters.role=document.getElementById('user-role')?.value||'ALL';appState.userFilters.enabled=document.getElementById('user-enabled')?.value||'ALL';appState.userFilters.online=document.getElementById('user-online')?.value||'ALL';appState.uiPages.users=1;renderUserList(event?.target?.id||'');};['user-search','user-role','user-enabled','user-online'].forEach(id=>document.getElementById(id)?.addEventListener(id==='user-search'?'input':'change',update));restoreFocusEnd(focusId);}
                function userTableStep3(users){return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>用户名 / UID</th><th>角色</th><th>状态</th><th>在线状态</th><th>最后登录</th><th>今日操作</th><th>操作</th></tr></thead><tbody>${users.map(u=>`<tr><td><span class="device-name"><span class="device-icon">${icon('user')}</span><span><strong>${esc(u.displayName||u.username)}</strong><span class="device-subtitle">UID: ${esc(u.username)}</span></span></span></td><td>${textPill(u.roleDisplayName||labelRoleFull(u.role),'info')}</td><td>${textPill(labelEnabledState(u.enabled),u.enabled?'ok':'warning')}</td><td>${textPill(labelOnline(u.online),u.online?'ok':'info')} <span class="muted">${esc(Number(u.sessionCount||0))} session</span></td><td>${fmtTime(u.lastLoginAt)}</td><td>--</td><td><div class="wa-action-cell"><button class="wa-btn ghost" disabled>查看</button><button class="wa-btn ghost" disabled>编辑</button>${waIconButton('更多不可用','more','disabled')}</div></td></tr>`).join('')}</tbody></table></div>`;}
                function userRightRail(data,users){const roles=data.roles||[];return `<aside class="wa-right-rail">
                  <article class="wa-panel"><h2>角色分布</h2>${progressList(roles.map(r=>({label:r.displayName||labelRoleFull(r.role),value:r.count,total:Math.max(1,users.length),kind:'info'})))}</article>
                  <article class="wa-panel"><h2>状态分布</h2>${progressList([{label:'启用中',value:users.filter(u=>u.enabled).length,total:Math.max(1,users.length),kind:'ok'},{label:'禁用中',value:users.filter(u=>!u.enabled).length,total:Math.max(1,users.length),kind:'error'},{label:'在线',value:users.filter(u=>u.online).length,total:Math.max(1,users.length),kind:'info'}])}</article>
                  <article class="wa-panel"><h2>权限概览</h2><div class="list-stack"><div class="kv-row"><span class="muted">权限策略</span><strong>当前命令 / 服务端配置管理</strong></div><div class="kv-row"><span class="muted">密码策略</span><strong>PBKDF2 哈希</strong></div><div class="kv-row"><span class="muted">明文密码</span><strong>不展示</strong></div></div></article>
                  <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('重置密码','key','disabled','ghost')}${waButton('禁用用户','receiver-disabled','disabled','ghost')}${waButton('踢出会话','logout','disabled','ghost')}${waButton('删除用户','critical-issue','disabled','danger')}</div><p class="wa-disabled-note">用户写操作没有完整 WebAdmin HTTP 后端和审计边界，本轮保持禁用。</p></article>
                </aside>`;}
                async function renderSettingsPage(options={}){
                  if(!options.silent)setView(loading('正在加载系统设置...'));
                  const [settings,status,capabilities]=await Promise.all([settle('/api/webadmin/settings'),settle('/api/status'),settle('/api/webadmin/write/capabilities')]);
                  if(!settings.ok){if(options.silent){toast('系统设置实时刷新失败，已保留当前页面。');return;}setView(errorBlock(settings.error.message));return;}
                  const data=settings.data||{}, service=data.service||{}, storage=data.storage||{}, security=data.security||{}, audit=data.audit||{}, system=data.system||{}, visibility=data.visibility||{}, runtime=status.ok?status.data:{};
                  const sessionCount=runtime.webAdmin?.sessionCount ?? system.sessionCount ?? '--';
                  const accessMode=service.accessMode||security.accessMode;
                  setView(`<section class="wa-page wa-settings-shell" data-settings-layout="true" data-responsive-layout="true">
                    ${settingsHeader(`${waButton('保存设置','settings','disabled','primary')}${waButton('重置为默认','archive','disabled','ghost')}`)}
                    <div class="wa-tabs wa-tabs-scroll wa-settings-tabs" data-settings-tabs="true"><button class="wa-tab active">通用设置</button><button class="wa-tab" disabled>安全设置</button><button class="wa-tab" disabled>性能设置</button><button class="wa-tab" disabled>界面设置</button><button class="wa-tab" disabled>通知设置</button><button class="wa-tab" disabled>备份与维护</button><button class="wa-tab" disabled>关于</button></div>
                    <section class="wa-settings-layout">
                      <div class="wa-settings-main">
                        <article class="wa-panel wa-settings-card"><h2>平台信息（只读）</h2>${settingsInfoGrid([
                          {icon:'settings',label:'平台名称',value:'TZZ Mod WebAdmin'},
                          {icon:'archive',label:'平台版本',value:system.modVersion||'unknown'},
                          {icon:'device',label:'Minecraft 版本',value:system.minecraftVersion||'--'},
                          {icon:'server-online',label:'服务器类型',value:labelServerType(system.serverType)},
                          {icon:'region',label:'当前世界 / 存档',value:system.worldName||'--'},
                          {icon:'doctor-overview',label:'访问模式',value:labelAccessMode(accessMode)}
                        ])}</article>
                        <article class="wa-panel wa-settings-card"><h2>服务信息（只读）</h2>${settingsInfoGrid([
                          {icon:'enabled',label:'服务状态',value:settingsServiceStatus(service)},
                          {icon:'active-channel',label:'服务地址',value:service.url||'--'},
                          {icon:'signalbridge-main',label:'监听地址',value:`${service.host||'127.0.0.1'}:${service.port||'18080'}`},
                          {icon:'current-user',label:'当前用户',value:service.currentUser||appState.me?.username||'--'},
                          {icon:'current-role',label:'当前角色',value:labelRoleFull(service.currentRole||appState.me?.role)},
                          {icon:'session',label:'Session 数量',value:sessionCount}
                        ])}</article>
                        <article class="wa-panel wa-settings-card"><h2>功能开关（只读）</h2><div class="wa-settings-switch-grid" data-settings-switch-grid="true">${settingReadonlyToggle('启用 SignalBridge','信号系统由当前服务端模块提供',true)}${settingReadonlyToggle('启用事件记录','记录 signal/action 等已有历史',true)}${settingReadonlyToggle('启用审计日志','WebAdmin 写操作审计',!!audit.enabled || !!security.auditEnabled)}${settingReadonlyToggle('启用 Web API','当前 WebAdmin API 正在服务',true)}${settingReadonlyToggle('启用自动修复','Doctor 自动修复未开放',false)}${settingReadonlyToggle('启用设置写入','系统设置写入未开放',false)}</div></article>
                        <article class="wa-panel wa-settings-card"><h2>运行环境信息（只读）</h2><div class="wa-settings-env-grid">${settingsEnvItem('Java 版本',runtime.javaVersion||runtime.jvm?.version||system.javaVersion||'--','settings')}${settingsEnvItem('CPU 使用率',`${settingsPercent(runtime.cpu?.usagePercent ?? runtime.system?.cpuUsagePercent ?? runtime.cpuUsagePercent,0)}%`,'pulse-duration')}${settingsEnvItem('内存使用率',`${settingsPercent(runtime.memory?.usagePercent ?? runtime.jvm?.memoryUsagePercent ?? runtime.memoryUsagePercent,0)}%`,'response-time')}${settingsEnvItem('磁盘使用率',`${settingsPercent(runtime.disk?.usagePercent ?? storage.diskUsagePercent,0)}%`,'archive')}${settingsEnvItem('TPS',runtime.server?.tps ?? runtime.tps ?? system.tps ?? '--','today-trigger')}${settingsEnvItem('访问模式',labelAccessMode(accessMode),'doctor-overview')}</div></article>
                        <div class="wa-settings-readonly-note">${icon('info')}<span>系统设置当前主要为只读信息。保存、重置、重新加载配置等全局操作缺少完整后端安全能力，本轮保持禁用且不发送写请求。</span><button class="link-button" disabled>了解系统设置</button></div>
                      </div>
                      ${settingsRightRail(data,runtime,capabilities.ok?capabilities.data:{})}
                    </section>
                  </section>`,options);
                }
                function settingsServiceStatus(service){
                  const running=service&&service.running;
                  const kind=running===true?'OK':running===false?'WARNING':'UNKNOWN';
                  const text=running===true?'运行中':running===false?'未运行':'未知';
                  return safeHtml(pill(kind)+' '+esc(text));
                }
                function settingsHeader(actions=''){
                  const actionHtml=[pageHelpLink('getting-started.overview',currentRouteHash()),actions].filter(Boolean).join('');
                  return `<header class="wa-settings-header"><div class="wa-settings-title"><span class="wa-settings-icon">${icon('settings')}</span><div><div class="wa-detail-kicker">系统管理 / 系统设置</div><h1>系统设置</h1><p>查看 WebAdmin 服务状态、运行环境、安全边界和功能开关。</p></div></div><div class="wa-settings-notice"><strong>只读设置</strong><span>当前仅展示已有服务信息，未实现的写操作保持不可用。</span></div>${actionHtml?`<div class="wa-actions">${actionHtml}</div>`:''}</header>`;
                }
                function settingsInfoGrid(items){
                  return `<div class="wa-settings-info-grid">${(items||[]).map(item=>`<div class="wa-settings-info-item"><span class="wa-settings-mini-icon">${icon(item.icon||'settings')}</span><span><small>${esc(item.label)}</small><strong>${detailValue(item.value)}</strong></span></div>`).join('')}</div>`;
                }
                function settingReadonlyToggle(label,desc,enabled){return `<div class="setting-row"><span><strong>${esc(label)}</strong><small>${esc(desc)}</small></span><span class="wa-switch ${enabled?'is-on':'is-off'}" aria-hidden="true"><i></i></span><em>${esc(enabled?'启用':'禁用')}</em></div>`;}
                function settingsEnvItem(label,value,iconName){return `<div class="wa-settings-env-item"><span>${icon(iconName||'info')}</span><small>${esc(label)}</small><strong>${esc(value||'--')}</strong></div>`;}
                function settingsPercent(value,fallback=0){const n=Number(value);if(!Number.isFinite(n))return fallback;const pct=n>0&&n<=1?n*100:n;return Math.max(0,Math.min(100,Math.round(pct)));}
                function settingsProgressItem(label,value,display,kind=''){const pct=settingsPercent(value,0);return `<div class="wa-progress-item"><div><span>${esc(label)}</span><strong>${esc(display||`${pct}%`)}</strong></div><div class="wa-progress-track"><div class="wa-progress-bar ${esc(kind)}" style="width:${pct}%"></div></div></div>`;}
                function settingsRailLine(label,value,iconName='info'){return `<div class="wa-settings-rail-line"><span class="wa-settings-mini-icon">${icon(iconName)}</span><span><small>${esc(label)}</small><strong>${detailValue(value)}</strong></span></div>`;}
                function settingsRailAction(label,iconName,danger=false){return `<button class="wa-btn ${danger?'danger':'ghost'}" disabled>${icon(iconName)}<span>${esc(label)}</span></button>`;}
                function settingsRightRail(data,status,capabilities){
                  const storage=data.storage||{}, security=data.security||{}, service=data.service||{}, system=data.system||{};
                  const cpu=settingsPercent(status.cpu?.usagePercent ?? status.system?.cpuUsagePercent ?? status.cpuUsagePercent,0);
                  const mem=settingsPercent(status.memory?.usagePercent ?? status.jvm?.memoryUsagePercent ?? status.memoryUsagePercent,0);
                  const disk=settingsPercent(status.disk?.usagePercent ?? storage.diskUsagePercent,0);
                  const tps=status.server?.tps ?? status.tps ?? system.tps ?? '--';
                  const sessionCount=status.webAdmin?.sessionCount ?? system.sessionCount ?? '--';
                  return `<aside class="wa-settings-rail" data-settings-status-rail="true">
                    <article class="wa-panel wa-settings-card"><h2>系统状态</h2><div class="wa-settings-status-list">${settingsRailLine('平台状态',settingsServiceStatus(service),'server-online')}${settingsRailLine('已运行时间',status.uptime||status.server?.uptime||'--','history')}${settingsRailLine('Java 版本',status.javaVersion||status.jvm?.version||system.javaVersion||'--','settings')}${settingsProgressItem('CPU 使用率',cpu,`${cpu}%`,'ok')}${settingsProgressItem('内存使用率',mem,`${mem}%`,'ok')}${settingsProgressItem('磁盘使用率',disk,`${disk}%`,'info')}${settingsRailLine('活动连接数',sessionCount,'session')}${settingsRailLine('TPS',tps,'pulse-duration')}</div></article>
                    <article class="wa-panel wa-settings-card"><h2>环境摘要</h2><div class="wa-settings-status-list">${settingsRailLine('存储作用域',storage.scope||'WORLD_SAVE','archive')}${settingsRailLine('按世界隔离',storage.worldScoped?'是':'否','region')}${settingsRailLine('敏感路径',storage.restricted||security.sensitiveStorageHidden?'已隐藏':'可见','eye')}${settingsRailLine('旧全局文件',storage.legacyGlobalFilesDetected?'检测到':'未检测到','warning-issue')}</div></article>
                    <article class="wa-panel wa-settings-card"><h2>快速操作</h2><div class="wa-settings-action-list">${settingsRailAction('查看系统日志','eye')}${settingsRailAction('查看审计日志','history')}${settingsRailAction('检查更新','refresh')}${settingsRailAction('导出系统信息','download')}${settingsRailAction('重新加载配置','critical-issue',true)}</div><p class="wa-disabled-note">保存、重置、导出、检查更新和重新加载配置没有完整后端安全能力，本轮保持禁用。</p></article>
                  </aside>`;}
                async function renderRegionsPage(options={}){
                  if(!options.silent)setView(loading('正在加载区域列表...'));
                  let regions;try{regions=await api('/api/regions?limit=500')}catch(err){if(options.silent){toast('区域列表实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.regions=regions||[];
                  renderRegionList('',options);
                }
                function renderRegionList(focusId,options={}){
                  waEnsureState();
                  const regions=appState.regions||[], worlds=uniqueNonBlank(regions.map(r=>r.world)), filtered=filterRegionsStep3(regions), page=waPageItems('regions',filtered,10);
                  const enabled=regions.filter(r=>r.enabled).length, warnings=regions.filter(r=>['WARNING','ERROR'].includes(String(r.doctorStatus||'').toUpperCase())).length;
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('区域列表','查看已登记区域、坐标边界和当前只读运行状态；不是 RegionController 编辑器。',`${waButton('添加区域','plus','disabled','primary')}${waButton('导入区域配置','upload','disabled','ghost')}${waButton('导出区域配置','download','disabled','ghost')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('区域总数',regions.length,'来自 /api/regions','region')}
                      ${waMetric('启用中',enabled,'enabled=true','enabled','ok')}
                      ${waMetric('禁用中',regions.length-enabled,'enabled=false','receiver-disabled',regions.length-enabled?'warning':'')}
                      ${waMetric('今日进入事件','--','当前 API 未提供','today-trigger')}
                      ${waMetric('今日离开事件','--','当前 API 未提供','history')}
                      ${waMetric('有问题区域',warnings,'Doctor 非 OK','warning-issue',warnings?'warning':'')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="region-search" placeholder="搜索区域名称 / ID / world / channel..." value="${esc(appState.regionFilters.search)}"></label>
                          <label class="filter-field"><span>世界</span>${waSelect('region-world',['ALL',...worlds],appState.regionFilters.world,v=>v==='ALL'?'全部世界':v)}</label>
                          <label class="filter-field"><span>状态</span>${waSelect('region-enabled',['ALL','ENABLED','DISABLED'],appState.regionFilters.enabled,regionOptionLabel)}</label>
                          <label class="filter-field"><span>Doctor</span>${waSelect('region-doctor',['ALL','OK','INFO','WARNING','ERROR','UNKNOWN'],appState.regionFilters.doctor,regionOptionLabel)}</label>
                """).toString();
    }
}
