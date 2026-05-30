package com.zcpu.tzzmod.webadmin;

final class WebAdminFrontendActionTimerScripts {
    private WebAdminFrontendActionTimerScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                function labelTimerMode(value){return {DELAY:'延迟执行',COUNTDOWN:'倒计时',REPEAT:'重复执行'}[String(value||'').toUpperCase()]||value||'未知模式';}
                function timerModeIcon(value){const mode=String(value||'').toUpperCase();if(mode==='DELAY')return 'delay';if(mode==='COUNTDOWN')return 'countdown';if(mode==='REPEAT')return 'repeat';return 'timer';}
                function labelTimerScope(value){return {GLOBAL:'全局',PLAYER:'玩家'}[String(value||'').toUpperCase()]||value||'未知作用域';}
                function labelTimerStartPolicy(value){return {RESTART:'重新开始',IGNORE_IF_RUNNING:'运行中则忽略',FAIL_IF_RUNNING:'运行中则失败'}[String(value||'').toUpperCase()]||value||'使用 Timer 定义';}
                function ticksToSeconds(ticks){const n=Number(ticks||0);return Number.isFinite(n)?(n/20).toFixed(n%20===0?0:2):'0';}
                function timerDurationSummary(t){const mode=String(t.mode||'DELAY').toUpperCase(), duration=Number(t.durationTicks||0), interval=Number(t.intervalTicks||0), runs=Number(t.maxRuns||0);if(mode==='REPEAT')return `间隔 ${interval} ticks / ${runs===0?'无限重复':`最多 ${runs} 次`}`;if(mode==='DELAY')return `总时长 ${duration} ticks（${ticksToSeconds(duration)} 秒）`;return `总时长 ${duration} ticks（${ticksToSeconds(duration)} 秒） / 间隔 ${interval} ticks`;}
                async function renderTimersPage(options={}){
                  if(!options.silent)setView(loading('正在加载计时器...'));
                  let data;try{data=await api('/api/webadmin/timers')}catch(err){if(options.silent){toast('计时器实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.timers=data?.timers||[];
                  renderTimerList('',options,data||{});
                }
                function renderTimerList(focusId,options={},meta={}){
                  waEnsureState();
                  if(meta&&Object.keys(meta).length)appState.timerListMeta=meta;
                  meta=(meta&&Object.keys(meta).length)?meta:(appState.timerListMeta||{});
                  const timers=appState.timers||[], filtered=filterTimers(timers), page=waPageItems('timers',filtered,10), active=timers.reduce((sum,t)=>sum+Number(t.activeInstanceCount||t.status?.activeInstanceCount||0),0), enabled=timers.filter(t=>t.enabled!==false).length, recent=timers.filter(t=>!isBlank(t.lastResult)||!isBlank(t.lastFailureReason)||!isBlank(t.status?.lastResult)||!isBlank(t.status?.lastFailureReason)).length;
                  const create=canEditTimer()?waButton('新建计时器','plus','onclick="startTimerCreate()"','primary'):waButton('新建计时器','plus','disabled','ghost');
                  const clear=timerFiltersActive()?waButton('清除筛选','close','onclick="clearTimerFilters()" data-timer-empty-filter-reset="true"','ghost'):'';
                  if(setView(`<section class="wa-page timer-page" data-timer-page="true" data-timer-unified-layout="true" data-timer-silent-refresh-preserves-draft="true" data-timer-refresh-preserves-filters="true">
                    ${waPageHead('调度器 / 计时器','配置延迟、倒计时和重复执行。配置持久化，运行中实例仅保存在内存。',`${create}${waButton('刷新','refresh','onclick="renderTimersPage({silent:true})"','ghost')}`)}
                    ${meta.storeDegraded?`<div class="readonly-note danger">${esc(meta.storeMessage||'Timer store 当前不可写。')}</div>`:''}
                    <section class="wa-card-grid wa-metrics-4" data-timer-stats-cards="true">
                      ${waMetric('Timer 总数',timers.length,`启用 ${enabled} / 停用 ${timers.length-enabled}`,'timer')}
                      ${waMetric('启用数量',enabled,'可被 action 或手动操作启动','enabled','ok')}
                      ${waMetric('运行中实例',active,'仅内存态，服务器停止会清空','timer-start',active?'warning':'')}
                      ${waMetric('最近事件',recent,'最近完成、失败或运行结果','recent-event')}
                    </section>
                    <article class="readonly-note timer-storage-note" data-timer-secondary-storage-info="true">存储：${esc(meta.storeFile||'timers.json')} · world-scoped · 运行态仅内存保存。</article>
                    <section class="wa-table-card" data-timer-list="true" data-timer-compact-filter-toolbar="true">
                      <div class="wa-filter-bar timer-filter-toolbar" data-timer-filter-responsive-wrap="true" data-timer-no-giant-full-width-stacked-filters="true">
                        <label class="filter-field search-control"><span>搜索</span><input class="input" id="timer-search" placeholder="搜索名称、ID、频道..." value="${esc(appState.timerFilters.search||'')}"></label>
                        <label class="filter-field"><span>状态</span>${waSelect('timer-filter-enabled',['ALL','ENABLED','DISABLED'],appState.timerFilters.enabled||'ALL',timerFilterLabel)}</label>
                        <label class="filter-field"><span>模式</span>${waSelect('timer-filter-mode',['ALL','DELAY','COUNTDOWN','REPEAT'],appState.timerFilters.mode||'ALL',timerFilterLabel)}</label>
                        <label class="filter-field"><span>作用域</span>${waSelect('timer-filter-scope',['ALL','GLOBAL','PLAYER'],appState.timerFilters.scope||'ALL',timerFilterLabel)}</label>
                        ${clear}${waButton('刷新','refresh','onclick="renderTimersPage({silent:true})"','ghost')}
                      </div>
                      ${page.items.length?timerTable(page.items):timerEmptyState(timers.length,filtered.length)}
                      ${waPagination('timers',page)}
                    </section>
                  </section>`,options))bindTimerFilters(focusId);
                }
                function timerFilterLabel(value){return {ALL:'全部',ENABLED:'启用',DISABLED:'停用',GLOBAL:'全局',PLAYER:'玩家',DELAY:'延迟执行',COUNTDOWN:'倒计时',REPEAT:'重复执行'}[String(value||'')]||value;}
                function timerFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span>${waSelect(id,options,value,timerFilterLabel)}</label>`;}
                function timerFiltersActive(){const f=appState.timerFilters||{};return !!(f.search||f.enabled&&f.enabled!=='ALL'||f.mode&&f.mode!=='ALL'||f.scope&&f.scope!=='ALL');}
                function clearTimerFilters(){appState.timerFilters={search:'',enabled:'ALL',mode:'ALL',scope:'ALL'};appState.uiPages.timers=1;renderTimerList('',{silent:true});}
                function bindTimerFilters(focusId){const update=(event)=>{appState.timerFilters.search=document.getElementById('timer-search')?.value||'';appState.timerFilters.enabled=document.getElementById('timer-filter-enabled')?.value||'ALL';appState.timerFilters.mode=document.getElementById('timer-filter-mode')?.value||'ALL';appState.timerFilters.scope=document.getElementById('timer-filter-scope')?.value||'ALL';appState.uiPages.timers=1;renderTimerList(event?.target?.id||'',{silent:true});};['timer-search','timer-filter-enabled','timer-filter-mode','timer-filter-scope'].forEach(id=>document.getElementById(id)?.addEventListener(id==='timer-search'?'input':'change',update));restoreFocusEnd(focusId);}
                function filterTimers(items){const f=appState.timerFilters||{};return (items||[]).filter(t=>{const text=[t.id,t.displayName,t.note,t.outputChannel].join(' ').toLowerCase();if(f.search&&!text.includes(String(f.search).toLowerCase()))return false;if(f.enabled==='ENABLED'&&t.enabled===false)return false;if(f.enabled==='DISABLED'&&t.enabled!==false)return false;if(f.mode&&f.mode!=='ALL'&&String(t.mode||'')!==f.mode)return false;if(f.scope&&f.scope!=='ALL'&&String(t.scopeMode||'')!==f.scope)return false;return true;}).sort((a,b)=>String(a.displayName||a.id).localeCompare(String(b.displayName||b.id)));}
                function timerEmptyState(total,filteredCount){const filtered=Number(total||0)>0&&Number(filteredCount||0)===0;return `<div class="timer-empty-state" data-timer-mature-empty-state="true"><strong>${filtered?'没有匹配当前筛选条件的 Timer':'暂无 Timer 配置'}</strong><p>${filtered?'调整搜索、状态、模式或作用域后重试。':'可以创建延迟、倒计时或重复计时器，用于延迟执行动作、按间隔触发动作，或在完成时输出 signal。'}</p><div class="inline-actions">${filtered?waButton('清除筛选','close','onclick="clearTimerFilters()" data-timer-empty-filter-reset="true"','ghost'):(canEditTimer()?waButton('新建计时器','plus','onclick="startTimerCreate()" data-timer-empty-create-action="true"','primary'):waButton('需要 EDITOR / OWNER','plus','disabled data-timer-empty-create-action="true"','ghost'))}${waButton('刷新','refresh','onclick="renderTimersPage({silent:true})"','ghost')}</div></div>`;}
                function timerTable(items){return `<div class="wa-table-scroll" data-timer-modern-table="true"><table class="wa-table"><thead><tr><th>计时器</th><th>状态</th><th>模式 / 作用域</th><th>时间线</th><th>动作 / 输出</th><th>运行态</th><th>最近结果</th><th>操作</th></tr></thead><tbody>${items.map(t=>{const target=timerHash(t.id), title=t.displayName||t.id, actionCount=(t.onStartActions?.length||0)+(t.onTickActions?.length||0)+(t.onCompleteActions?.length||0)+(t.onCancelActions?.length||0), active=Number(t.activeInstanceCount||t.status?.activeInstanceCount||0), result=t.lastResult||t.lastFailureReason||t.status?.lastResult||t.status?.lastFailureReason||'暂无';return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看计时器 ${title}`)} data-timer-list-row-card="true" data-timer-row-click-detail="true"><td><span class="device-name"><span class="device-icon">${icon(timerModeIcon(t.mode))}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">${esc(t.id)}</span>${t.note?`<span class="device-subtitle">${esc(t.note)}</span>`:''}</span></span></td><td>${textPill(t.enabled===false?'停用':'启用',t.enabled===false?'warning':'ok')}</td><td>${textPill(labelTimerMode(t.mode),'info')} <span class="muted">${esc(labelTimerScope(t.scopeMode))}</span></td><td class="truncate" title="${esc(timerDurationSummary(t))}">${esc(timerDurationSummary(t))}</td><td>${actionCount?`<span class="pill info">${esc(actionCount)} 个动作</span>`:'<span class="muted">无直接动作</span>'} ${t.outputChannel?channelButton(t.outputChannel):'<span class="muted">无输出频道</span>'}</td><td>${active?textPill(`${active} 运行中`,'warning'):'<span class="muted">未运行</span>'}</td><td class="truncate" title="${esc(result)}">${esc(result)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看计时器 ${title}`)}>详情</button>${canEditTimer()?`<button class="wa-btn ghost" onclick="event.stopPropagation();startTimerManual(${jsString(t.id)},'start')">${icon('timer-start')}<span>启动</span></button>`:''}</div></td></tr>`;}).join('')}</tbody></table></div>`;}
                async function renderTimerDetail(rawId,options={}){
                  const routeInfo=detailRoute(rawId,'#/timers'), id=routeInfo.id||'';
                  if(!id){setView(`<section class="wa-page">${backButton(routeInfo,'返回计时器')}${errorBlock('Timer ID 不能为空。')}</section>`);return;}
                  if(!options.silent)setView(loading('正在加载计时器详情...'));
                  let detail;try{detail=await api(`/api/webadmin/timers/${encodeURIComponent(id)}`)}catch(err){if(options.silent){toast('计时器详情实时刷新失败，已保留当前页面。');return;}setView(`<section class="wa-page">${backButton(routeInfo,'返回计时器')}${errorBlock(err.message)}</section>`);return;}
                  const status=detail.status||{}, lock=detail.lockStatus||{}, locked=lockHeldByOther(lock), actions=[];
                  if(canEditTimer()){actions.push(waButton('编辑','settings',locked?'disabled':htmlHandler(`startTimerEdit(${jsString(detail.id)})`),'primary'));actions.push(waButton('启动','timer-start',locked?'disabled':htmlHandler(`startTimerManual(${jsString(detail.id)},'start')`),'ghost'));actions.push(waButton('取消','timer-cancel',locked?'disabled':htmlHandler(`startTimerManual(${jsString(detail.id)},'cancel')`),'ghost'));actions.push(waButton('重置运行态','refresh',locked?'disabled':htmlHandler(`startTimerManual(${jsString(detail.id)},'reset')`),'danger'));actions.push(waButton('删除','critical-issue',locked?'disabled':htmlHandler(`startTimerDelete(${jsString(detail.id)})`),'danger'));}
                  setView(`<section class="wa-page wa-detail-shell timer-detail-page" data-timer-detail="true">${detailHeader({back:backButton(routeInfo,'返回计时器'),kicker:'调度器 / 计时器详情',iconName:timerModeIcon(detail.mode),title:detail.displayName||detail.id,subtitle:detail.note||`${labelTimerMode(detail.mode)} · ${labelTimerScope(detail.scopeMode)} · ${detail.enabled===false?'停用':'启用'}`,copyValue:detail.id,badges:[textPill(detail.enabled===false?'停用':'启用',detail.enabled===false?'warning':'ok'),textPill(labelTimerMode(detail.mode),'info'),textPill(labelTimerScope(detail.scopeMode),'info')],actions})}${detailTabs(['配置摘要','运行状态','动作入口','写入安全','完整详情'])}<section class="wa-detail-first-row">${detailCard('关键配置',detailInfoGrid([['ID',detail.id],['启用',detail.enabled===false?'否':'是'],['模式',labelTimerMode(detail.mode)],['作用域',labelTimerScope(detail.scopeMode)],['总时长',`${detail.durationTicks||0} ticks / ${ticksToSeconds(detail.durationTicks)} 秒`],['间隔',`${detail.intervalTicks||0} ticks / ${ticksToSeconds(detail.intervalTicks)} 秒`],['重复次数',Number(detail.maxRuns||0)===0?'无限直到取消':detail.maxRuns],['启动策略',labelTimerStartPolicy(detail.startPolicy)],['输出频道',detail.outputChannel?safeHtml(channelButton(detail.outputChannel)):'无']]))}${detailCard('运行状态',timerStatusPanel(status),'','detail-card-stretchable')}</section>${detailFixedLayout([timerDetailActionCard('启动动作','data-timer-on-start-actions',detail.onStartActions||[]),timerDetailActionCard('Tick 动作','data-timer-on-tick-actions',detail.onTickActions||[]),timerDetailActionCard('完成动作','data-timer-on-complete-actions',detail.onCompleteActions||[]),timerDetailActionCard('取消动作','data-timer-on-cancel-actions',detail.onCancelActions||[])],[detailCard('写入安全',`${locked?`<div class="readonly-note">${esc(lockMessage(lock,'Timer 配置'))}</div>`:'<div class="readonly-note">写操作走 permission / CSRF / same-origin / edit lock / expectedFingerprint / audit / realtime。</div>'}`),detailCard('存储与运行态','<p class="muted">配置保存到 world-scoped <code>tzz/webadmin/timers.json</code>；active runtime state 仅内存保存，服务器停止后清空。</p>')],[advancedDetailCard('timers',detail.id,[['fingerprint',detail.expectedFingerprint],['version',detail.version],['updatedAt',detail.updatedAt],['updatedBy',detail.updatedBy]],[{title:'status',rows:advancedRowsFromObject(status,'status')},{title:'validation',rows:advancedRowsFromObject({validationErrors:detail.validationErrors},'validation')}])])}</section>`,options);
                }
                function timerStatusPanel(status){const instances=status?.instances||[];const rows=instances.slice(0,8).map(s=>`<div class="event-row"><strong>${esc(s.scopeKey||'global')}</strong><span>剩余=${esc(s.remainingTicks)} · 下次=${esc(s.nextFireInTicks??s.remainingTicks)} · run=${esc(s.runCount||0)} · 剩余次数=${esc(s.remainingRuns??'')}</span><span class="muted">${esc(s.lastResult||'RUNNING')} ${s.lastFailureReason?`· ${esc(s.lastFailureReason)}`:''}</span></div>`).join('');return `<div data-timer-status-panel="true">${detailStatGrid([{label:'运行中实例',value:status?.activeInstanceCount||0,sub:'仅统计运行中实例',icon:'timer-start'},{label:'最近结果',value:status?.lastResult||'暂无',sub:status?.lastFailureReason||'',icon:'check-pass'},{label:'运行态持久化',value:'否',sub:'服务器停止会清空',icon:'settings'}])}${rows?`<div class="list-stack">${rows}</div>`:empty('当前没有运行中实例。')}</div>`;}
                """)
.append("""
                function timerDetailActionCard(title,marker,actions){return detailCard(title,`<div ${esc(marker)}="true">${timerActionSummaryList(actions)}</div>`);}
                function timerActionSummaryList(actions){return (actions||[]).length?`<div class="list-stack">${actions.map((a,i)=>`<div class="event-row"><strong>#${i+1} ${esc(labelActionType(a.type))}</strong><span>${esc(cleanActionSummary(a.summary||a.timerActionSummary||a.stateActionSummary||a.value||'尚未配置'))}</span><span class="muted">${a.enabled===false?'禁用':'启用'} · cooldown ${esc(a.cooldownTicks||0)}</span></div>`).join('')}</div>`:empty('尚未配置 action。');}
                function timerDraftFromDetail(detail={},mode='edit'){return {mode,id:detail.id||'',displayName:detail.displayName||'',note:detail.note||'',enabled:detail.enabled!==false,modeValue:detail.mode||'DELAY',scopeMode:detail.scopeMode||'GLOBAL',durationTicks:Number(detail.durationTicks??20),intervalTicks:Number(detail.intervalTicks??20),maxRuns:Number(detail.maxRuns??1),startPolicy:detail.startPolicy||'RESTART',outputChannel:detail.outputChannel||'',onStartActions:(detail.onStartActions||[]).map(normalizeActionRelayDraftAction),onTickActions:(detail.onTickActions||[]).map(normalizeActionRelayDraftAction),onCompleteActions:(detail.onCompleteActions||[]).map(normalizeActionRelayDraftAction),onCancelActions:(detail.onCancelActions||[]).map(normalizeActionRelayDraftAction),conditionGateOptions:detail.conditionGateOptions||{},expectedFingerprint:detail.expectedFingerprint||detail.fingerprint||'',lockId:'',lock:null,lockTargetId:'',errors:[],saving:false};}
                function timerActionConditionTargetTypes(){return ['TIMER_ON_START_ACTION','TIMER_ON_TICK_ACTION','TIMER_ON_COMPLETE_ACTION','TIMER_ON_CANCEL_ACTION'];}
                async function loadTimerActionConditionGateOptions(timerId){return await loadRuntimeConditionGateOptions(timerActionConditionTargetTypes(),timerId||'',{parentTargetType:'TIMER',parentTargetId:timerId||''});}
                async function startTimerCreate(){const [conditionGateOptions]=await Promise.all([loadTimerActionConditionGateOptions(''),loadTimerOptions(),loadSignalChannelOptions().catch(()=>[])]);appState.timerEdit=timerDraftFromDetail({id:'',displayName:'',mode:'DELAY',scopeMode:'GLOBAL',durationTicks:20,intervalTicks:20,maxRuns:1,startPolicy:'RESTART',onCompleteActions:[],conditionGateOptions},'create');showTimerEditModal();}
                async function startTimerEdit(id){if(!canEditTimer())return;try{const [detail]=await Promise.all([api(`/api/webadmin/timers/${encodeURIComponent(id)}`),loadTimerOptions(),loadSignalChannelOptions().catch(()=>[])]);detail.conditionGateOptions=await loadTimerActionConditionGateOptions(detail.id);const result=await acquireTimerLock(detail.id);if(!result.success){toast(result.message||'无法获取编辑锁');await renderTimerDetail(encodeURIComponent(id),{silent:true});return;}const draft=timerDraftFromDetail(detail,'edit');draft.lockId=result.data?.lock?.lockId||'';draft.lock=result.data?.lock||null;draft.lockTargetId=detail.id;appState.timerEdit=draft;scheduleTimerLockHeartbeat();showTimerEditModal();}catch(err){toast(err.message||'无法打开 Timer 编辑器');}}
                function timerValidationErrorsHtml(errors){return (errors||[]).length?`<ul class="validation-list" data-timer-validation-preserves-input="true">${errors.map(e=>`<li>${esc(e.field?`${e.field}：`: '')}${esc(e.message||'保存失败')}</li>`).join('')}</ul>`:'';}
                function timerModeConfig(mode){const m=String(mode||'DELAY').toUpperCase();return {showDuration:m!=='REPEAT',showInterval:m==='COUNTDOWN'||m==='REPEAT',showMaxRuns:m==='REPEAT',showTick:m!=='DELAY',showComplete:true};}
                function timerPayloadFromDraft(d){const body={id:d.id,displayName:d.displayName,note:d.note,enabled:d.enabled,mode:d.modeValue,scopeMode:d.scopeMode,durationTicks:d.durationTicks,intervalTicks:d.intervalTicks,maxRuns:d.maxRuns,startPolicy:d.startPolicy,outputChannel:d.outputChannel,onStartActions:(d.onStartActions||[]).map(actionDraftPayload),onTickActions:(d.onTickActions||[]).map(actionDraftPayload),onCompleteActions:(d.onCompleteActions||[]).map(actionDraftPayload),onCancelActions:(d.onCancelActions||[]).map(actionDraftPayload),expectedFingerprint:d.expectedFingerprint,lockId:d.lockId};const mode=String(d.modeValue||'DELAY').toUpperCase();if(mode==='DELAY'){body.intervalTicks=0;body.maxRuns=1;body.onTickActions=[];}else if(mode==='COUNTDOWN'){body.maxRuns=1;}else if(mode==='REPEAT'){body.durationTicks=0;}return body;}
                function showTimerEditModal(){const d=appState.timerEdit;if(!d)return;appState.timerActionBucket='';markModalInitialSnapshot('timer_config',d);const cfg=timerModeConfig(d.modeValue), durationField=cfg.showDuration?`<label data-timer-mode-duration-field="true"><span>总时长 ticks</span><input id="timer-duration" class="input" data-timer-duration-ticks="true" type="number" min="0" step="1" value="${esc(d.durationTicks)}" oninput="syncTimerDraft()"><span class="muted">${esc(ticksToSeconds(d.durationTicks))} 秒；20 ticks = 1 秒。</span></label>`:'<input id="timer-duration" type="hidden" value="0" data-timer-mode-repeat-hides-duration="true">', intervalField=cfg.showInterval?`<label data-timer-mode-interval-field="true"><span>触发间隔 ticks</span><input id="timer-interval" class="input" data-timer-interval-ticks="true" type="number" min="1" step="1" value="${esc(d.intervalTicks)}" oninput="syncTimerDraft()"></label>`:'<input id="timer-interval" type="hidden" value="0" data-timer-mode-delay-hides-interval="true">', maxRunsField=cfg.showMaxRuns?`<label data-timer-mode-max-runs-field="true"><span>最大重复次数</span><input id="timer-max-runs" class="input" data-timer-max-runs="true" type="number" min="0" step="1" value="${esc(d.maxRuns)}" oninput="syncTimerDraft()"><span class="muted">REPEAT 中 0 表示无限重复直到取消。</span></label>`:'<input id="timer-max-runs" type="hidden" value="1" data-timer-mode-countdown-hides-max-runs="true" data-timer-mode-delay-hides-max-runs="true">', tickBucket=cfg.showTick?timerActionBucketSummaryEditor(d,'onTickActions','Tick 动作','data-timer-on-tick-actions="true"'):'<div data-timer-mode-delay-hides-on-tick="true" class="readonly-note">延迟执行模式不使用 Tick 动作。</div>';const errs=timerValidationErrorsHtml(d.errors), lockLine=d.lockId?`<div class="readonly-note">正在编辑 Timer，编辑锁到期：${esc(formatDateTime(d.lock?.expiresAt))}</div>`:'<div class="readonly-note">新建时会在保存前按 ID 获取编辑锁。</div>';openWebAdminModal(d.mode==='create'?'新建计时器':'编辑计时器',`<form class="edit-form timer-form wa-unified-config-form" data-timer-editor="true" data-timer-no-raw-json="true" data-timer-validation-preserves-input="true" data-timer-unified-animated-modal="true" data-timer-modal-uses-webadmin-modal="true" onsubmit="event.preventDefault();saveTimerEdit()"><section class="wa-edit-section">${lockLine}<label><span>Timer ID</span><input id="timer-id" class="input" maxlength="96" value="${esc(d.id)}" ${d.mode==='edit'?'disabled':''} oninput="syncTimerDraft()"><span class="muted">技术字段：id。</span></label><label><span>显示名称</span><input id="timer-name" class="input" maxlength="64" value="${esc(d.displayName)}" oninput="syncTimerDraft()"></label><label><span>说明</span><textarea id="timer-note" class="input wa-action-textarea" maxlength="512" oninput="syncTimerDraft()">${esc(d.note)}</textarea></label><label class="switch-row"><span>启用</span><input id="timer-enabled" type="checkbox" ${d.enabled?'checked':''} onchange="syncTimerDraft()"></label></section><section class="wa-edit-section"><header><h3>时间规则</h3></header><label><span>模式</span><select id="timer-mode" class="select" data-timer-mode-selector="true" onchange="syncTimerDraft();withPreservedModalScroll(()=>showTimerEditModal())">${['DELAY','COUNTDOWN','REPEAT'].map(v=>`<option value="${v}" ${d.modeValue===v?'selected':''}>${esc(labelTimerMode(v))}</option>`).join('')}</select></label><label><span>作用域</span><select id="timer-scope" class="select" data-timer-scope-selector="true" onchange="syncTimerDraft()">${['GLOBAL','PLAYER'].map(v=>`<option value="${v}" ${d.scopeMode===v?'selected':''}>${esc(labelTimerScope(v))}</option>`).join('')}</select></label>${durationField}${intervalField}${maxRunsField}<label><span>启动策略</span><select id="timer-start-policy" class="select" data-timer-start-policy="true" onchange="syncTimerDraft()">${['RESTART','IGNORE_IF_RUNNING','FAIL_IF_RUNNING'].map(v=>`<option value="${v}" ${d.startPolicy===v?'selected':''}>${esc(labelTimerStartPolicy(v))}</option>`).join('')}</select></label><label><span>完成输出频道</span>${timerOutputChannelField(d.outputChannel)}<span class="muted">可选兼容输出；完成动作仍是直接动作入口。</span></label></section><section class="wa-edit-section" data-timer-action-summary-cards="true"><header><h3>动作入口</h3><span class="pill info">摘要卡片 + 弹窗</span></header><div class="wa-config-stack">${timerActionBucketSummaryEditor(d,'onStartActions','启动动作','data-timer-on-start-actions="true"')}${tickBucket}${timerActionBucketSummaryEditor(d,'onCompleteActions','完成动作','data-timer-on-complete-actions="true"')}${timerActionBucketSummaryEditor(d,'onCancelActions','取消动作','data-timer-on-cancel-actions="true"')}</div></section>${errs}<p class="readonly-note" data-timer-no-raw-json="true">不提供 raw JSON；保存失败会保留当前输入和滚动位置。</p></form>`,editModalFooter(d.saving),{className:'wa-config-modal',syncBeforeClose:()=>syncTimerDraft(),dirtyCheck:()=>modalDraftDirty('timer_config',appState.timerEdit),onClose:async()=>{await cancelTimerEdit(true);await dismissWebAdminModal();}});}
                function timerActionBucketLabel(bucket){return {onStartActions:'启动动作',onTickActions:'Tick 动作',onCompleteActions:'完成动作',onCancelActions:'取消动作'}[bucket]||bucket;}
                function timerActionBucketSummaryEditor(d,bucket,title,marker){const list=d[bucket]||[], types=uniqueValues(list.map(a=>labelActionType(a.type))).join(' / ')||'未配置', keyHandler=`if(event.key==='Enter'||event.key===' '){event.preventDefault();openTimerActionBucketModal(${jsString(bucket)})}`;return `<article class="wa-native-trigger-compact-card timer-action-summary-card" ${marker||''} data-timer-action-summary-card="true" role="button" tabindex="0" onclick='openTimerActionBucketModal(${jsString(bucket)})' onkeydown="${esc(keyHandler)}"><div class="wa-native-trigger-compact-head"><strong>${esc(title)}</strong><span class="pill info">${esc(list.length)} 个</span></div><div class="wa-native-trigger-compact-line"><small>${esc(types)}</small><button class="wa-btn ghost" type="button" onclick='event.stopPropagation();openTimerActionBucketModal(${jsString(bucket)})'>${icon('settings')}<span>编辑动作</span></button></div><p class="muted">${esc(list.length?cleanActionSummary(list[0].summary||list[0].timerActionSummary||list[0].stateActionSummary||list[0].value||'已配置动作'):'动作明细在弹窗中管理，不在主表单展开。')}</p></article>`;}
                function openTimerActionBucketModal(bucket){syncTimerDraft();const d=appState.timerEdit;if(d)d.mainEditorUiState=captureModalScrollState();appState.timerActionBucket=bucket;showTimerActionBucketModal(bucket);}
                function returnTimerActionBucketToEditor(){syncTimerDraft();const state=appState.timerEdit?.mainEditorUiState;showTimerEditModal();restoreModalScrollState(state);}
                function showTimerActionBucketModal(bucket){const d=appState.timerEdit;if(!d)return;appState.timerActionBucket=bucket;const title=timerActionBucketLabel(bucket), errs=timerValidationErrorsHtml(d.errors);openWebAdminModal(`编辑${title}`,`<form class="edit-form timer-form wa-unified-config-form" data-timer-actions-managed-in-modal="true" data-timer-scroll-preserved="true" data-timer-unified-animated-modal="true" data-timer-modal-uses-webadmin-modal="true" onsubmit="event.preventDefault();saveTimerEdit()"><section class="wa-edit-section"><header><h3>${esc(title)}</h3><button class="wa-btn ghost" type="button" onclick="returnTimerActionBucketToEditor()">${icon('settings')}<span>返回计时器表单</span></button></header><p class="muted">动作按顺序执行；单条 gate false 时只跳过当前 action。</p></section>${timerActionListEditor(d,bucket,title,bucket==='onStartActions'?'data-timer-on-start-actions="true"':bucket==='onTickActions'?'data-timer-on-tick-actions="true"':bucket==='onCompleteActions'?'data-timer-on-complete-actions="true"':'data-timer-on-cancel-actions="true"')}${errs}</form>`,`${waButton('返回表单','settings','onclick="returnTimerActionBucketToEditor()" data-timer-scroll-preserved="true"','ghost')}<button class="wa-btn primary" type="button" data-timer-action-submit="true" ${d.saving?'disabled':''}>${icon('check-pass')}<span>${d.saving?'保存中...':'保存'}</span></button>`,{className:'wa-config-modal',syncBeforeClose:()=>syncTimerDraft(),dirtyCheck:()=>modalDraftDirty('timer_config',appState.timerEdit),onClose:async()=>{await cancelTimerEdit(true);await dismissWebAdminModal();}});}
                function rerenderTimerEditor(){const bucket=appState.timerActionBucket;if(bucket)withPreservedModalScroll(()=>showTimerActionBucketModal(bucket));else withPreservedModalScroll(()=>showTimerEditModal());}
                function timerActionListEditor(d,bucket,title,marker){const list=d[bucket]||[];return `<section class="wa-edit-section" ${marker||''}><header><h3>${esc(title)}</h3><button class="wa-btn ghost" type="button" onclick='addTimerAction(${jsString(bucket)})'>${icon('plus')}<span>添加动作</span></button></header>${list.length?list.map((a,i)=>timerActionRow(bucket,a,i)).join(''):empty(`${title} 暂无动作。`)}<p class="muted">动作明细通过统一弹窗管理，不在列表或详情页展开复杂表单。</p></section>`;}
                function timerActionBucketTargetType(bucket){return {onStartActions:'TIMER_ON_START_ACTION',onTickActions:'TIMER_ON_TICK_ACTION',onCompleteActions:'TIMER_ON_COMPLETE_ACTION',onCancelActions:'TIMER_ON_CANCEL_ACTION'}[bucket]||'TIMER_ON_COMPLETE_ACTION';}
                function timerActionBucketTargetId(timerId,bucket,index){const b={onStartActions:'start',onTickActions:'tick',onCompleteActions:'complete',onCancelActions:'cancel'}[bucket]||'complete';return `timer_${b}:${timerId||'draft'}:action:${Number(index||0)}`;}
                function timerActionRow(bucket,action,index){const prefix=`timer-${bucket}-${index}`, type=String(action.type||'message').toLowerCase(), d=appState.timerEdit||{}, ownerId=actionOwnerId('timer',bucket), owner=`data-timer-${bucket.replace(/[^a-zA-Z0-9_-]/g,'-')}-action-condition-gate-picker`, gatePicker=actionConditionGatePicker(d,action,index,timerActionBucketTargetType(bucket),action.actionConditionGateTargetId||timerActionBucketTargetId(d.id,bucket,index),owner,'syncTimerDraft()');return `<article class="wa-action-row"><header><div><strong>#${index+1} ${esc(labelActionType(type))}</strong><small>${esc(action.summary||action.timerActionSummary||action.stateActionSummary||action.value||'尚未配置')}</small></div><select id="${prefix}-type" class="select" data-action-owner-capability-filter="true" onchange='changeTimerActionType(${jsString(bucket)},${index})'>${actionTypeOptions(type,ownerId)}</select></header><div class="wa-action-editor-grid">${timerActionValueEditor(prefix,action,bucket)}${gatePicker}<label class="switch-row">启用<input id="${prefix}-enabled" type="checkbox" ${action.enabled!==false?'checked':''} onchange="syncTimerDraft()"></label><label>冷却 tick<input id="${prefix}-cooldown" class="input" type="number" min="0" step="1" value="${esc(action.cooldownTicks||0)}" oninput="syncTimerDraft()"></label></div><div class="inline-actions"><button class="wa-btn ghost" type="button" ${index===0?'disabled':''} onclick='moveTimerAction(${jsString(bucket)},${index},-1)'>上移</button><button class="wa-btn ghost" type="button" ${index>=(appState.timerEdit?.[bucket]||[]).length-1?'disabled':''} onclick='moveTimerAction(${jsString(bucket)},${index},1)'>下移</button><button class="wa-btn danger" type="button" onclick='removeTimerAction(${jsString(bucket)},${index})'>删除</button></div></article>`;}
                function timerActionValueEditor(prefix,action,bucket=''){return renderTypedActionValueEditor(prefix,action,{oninput:'syncTimerDraft()',onchange:'syncTimerDraft();rerenderTimerEditor()',ownerMarker:`data-timer-typed-action-fields="true" data-action-owner-id="${esc(actionOwnerId('timer',bucket))}"`});}
                function syncTimerDraft(){const d=appState.timerEdit;if(!d)return;const get=id=>document.getElementById(id);d.id=get('timer-id')?.value??d.id;d.displayName=get('timer-name')?.value??d.displayName;d.note=get('timer-note')?.value??d.note;if(get('timer-enabled'))d.enabled=!!get('timer-enabled')?.checked;d.modeValue=get('timer-mode')?.value??d.modeValue;d.scopeMode=get('timer-scope')?.value??d.scopeMode;d.durationTicks=Number(get('timer-duration')?.value??d.durationTicks);d.intervalTicks=Number(get('timer-interval')?.value??d.intervalTicks);d.maxRuns=Number(get('timer-max-runs')?.value??d.maxRuns);d.startPolicy=get('timer-start-policy')?.value??d.startPolicy;d.outputChannel=get('timer-output-channel')?.value??d.outputChannel;['onStartActions','onTickActions','onCompleteActions','onCancelActions'].forEach(bucket=>{d[bucket]=(d[bucket]||[]).map((a,index)=>{const prefix=`timer-${bucket}-${index}`, type=String(get(`${prefix}-type`)?.value||a.type||'message').toLowerCase(), timerType=type==='timer_start'||type==='timer_cancel', stateType=type==='state_variable', gateId=`action-condition-data-timer-${bucket.replace(/[^a-zA-Z0-9_-]/g,'-')}-action-condition-gate-picker-${index}`, next={...a,type,value:(timerType||stateType)?'':(get(`${prefix}-value`)?.value??a.value??''),enabled:get(`${prefix}-enabled`)?!!get(`${prefix}-enabled`)?.checked:a.enabled!==false,cooldownTicks:Number(get(`${prefix}-cooldown`)?.value??a.cooldownTicks??0),requiresOp:type==='command'&&!!a.requiresOp,notifyOps:type==='command'&&!!a.notifyOps,conditionGroupId:get(gateId)?.value??a.conditionGroupId??''};if(stateType)syncStateActionDraftFromForm(prefix,next);if(timerType)syncTimerActionDraftFromForm(prefix,next);return next;});});}
                function addTimerAction(bucket){syncTimerDraft();const d=appState.timerEdit;if(!d)return;d[bucket]=d[bucket]||[];d[bucket].push({type:'message',value:'',enabled:true,cooldownTicks:0});rerenderTimerEditor();}
                function removeTimerAction(bucket,index){syncTimerDraft();const d=appState.timerEdit;if(!d)return;d[bucket].splice(index,1);rerenderTimerEditor();}
                function moveTimerAction(bucket,index,delta){syncTimerDraft();const d=appState.timerEdit;if(!d)return;const list=d[bucket]||[], next=index+delta;if(next<0||next>=list.length)return;const [item]=list.splice(index,1);list.splice(next,0,item);rerenderTimerEditor();}
                function changeTimerActionType(bucket,index){syncTimerDraft();const d=appState.timerEdit;if(!d)return;const a=d[bucket]?.[index];if(!a)return;a.type=String(document.getElementById(`timer-${bucket}-${index}-type`)?.value||'message').toLowerCase();a.value=a.type==='sound'?'minecraft:entity.experience_orb.pickup':'';if(a.type==='state_variable')Object.assign(a,stateActionPayload(a));if(a.type==='timer_start'||a.type==='timer_cancel')Object.assign(a,timerActionPayload(a));rerenderTimerEditor();}
                function timerOutputChannelField(value){return `<div class="channel-combo timer-channel-combo" data-timer-output-channel-combobox="true"><div class="channel-combo-control"><input id="timer-output-channel" class="input" maxlength="128" value="${esc(value||'')}" placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="false" aria-controls="timer-output-channel-menu" oninput="syncTimerDraft()" onkeydown="handleTimerChannelKey(event)"><button class="channel-combo-toggle" type="button" aria-label="显示已有频道" onclick="toggleTimerChannelOptions()">${icon('chevron-down')}</button></div><div id="timer-output-channel-menu" class="channel-combo-menu" role="listbox">${timerChannelOptions(value)}</div></div>`;}
                function timerChannelOptions(value){const options=filteredChannelOptions(appState.channelOptions||[],String(value||'')).slice(0,8);return options.length?options.map(c=>`<button type="button" class="channel-combo-option" onmousedown="event.preventDefault()" onclick='selectTimerChannel(${jsString(c.channel||'')})'><strong>${esc(c.channel||'')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join(''):'<div class="channel-combo-empty">可直接输入新的频道名</div>';}
                function closeTimerChannelOptions(){let closed=false;document.querySelectorAll('#wa-modal-root .timer-channel-combo.open').forEach(combo=>{closed=true;combo.classList.remove('open');const input=combo.querySelector('input[role="combobox"]');if(input)input.setAttribute('aria-expanded','false');});return closed;}
                async function toggleTimerChannelOptions(){const combo=document.getElementById('timer-output-channel')?.closest('.timer-channel-combo'), wasOpen=!!combo?.classList.contains('open');if(!appState.channelOptions&&!appState.channelOptionsError)await loadSignalChannelOptions().catch(()=>[]);closeAllCustomComboboxes();if(wasOpen)return;if(combo){combo.classList.add('open');const input=document.getElementById('timer-output-channel');if(input){input.setAttribute('aria-expanded','true');input.focus();}const menu=document.getElementById('timer-output-channel-menu');if(menu)menu.innerHTML=timerChannelOptions(input?.value||'');}}
                function selectTimerChannel(channel){const input=document.getElementById('timer-output-channel');if(input)input.value=channel||'';closeTimerChannelOptions();syncTimerDraft();withPreservedModalScroll(()=>showTimerEditModal());}
                function handleTimerChannelKey(event){if(event.key==='Escape'){closeTimerChannelOptions();event.preventDefault();return;}const menu=document.getElementById('timer-output-channel-menu');if(menu)menu.innerHTML=timerChannelOptions(event.target?.value||'');}
                function normalizeTimerId(id){return String(id||'').trim().toLowerCase().replace(/\\s+/g,'-').replace(/[^a-z0-9_.:-]/g,'').substring(0,96);}
                async function acquireTimerLock(id){return await acquireWebAdminEditLock('timer_config',id);}
                async function ensureTimerDraftLock(d){if(!d)return false;d.id=normalizeTimerId(d.id);if(d.lockId&&d.lockTargetId===d.id)return true;if(d.lockId)await releaseTimerLock(d,true);if(!d.id){d.errors=[{field:'id',message:'Timer ID 不能为空。'}];return false;}const result=await acquireTimerLock(d.id);if(result.success){d.lockId=result.data?.lock?.lockId||'';d.lock=result.data?.lock||null;d.lockTargetId=d.id;scheduleTimerLockHeartbeat();return true;}d.errors=[{message:result.message||'Timer 编辑锁获取失败'}];return false;}
                async function saveTimerEdit(){syncTimerDraft();const d=appState.timerEdit;if(!d)return;d.saving=true;d.errors=[];rerenderTimerEditor();if(!await ensureTimerDraftLock(d)){d.saving=false;rerenderTimerEditor();return;}const body=timerPayloadFromDraft(d);try{const result=await api(d.mode==='create'?'/api/webadmin/timers':`/api/webadmin/timers/${encodeURIComponent(d.id)}`,{method:d.mode==='create'?'POST':'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(body)});if(result.success){const target=result.data?.routeTarget||timerHash(result.data?.timer?.id||d.id);appState.timerEdit=null;stopTimerLockHeartbeat();await dismissWebAdminModal();toast(result.message||'Timer 已保存。');location.hash=target;return;}d.saving=false;d.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopTimerLockHeartbeat();rerenderTimerEditor();}catch(err){d.saving=false;d.errors=[{message:err.message||'保存失败'}];rerenderTimerEditor();}}
                """)
.append("""
                async function cancelTimerEdit(silent){const d=appState.timerEdit;if(d&&d.lockId)await releaseTimerLock(d,silent);appState.timerEdit=null;stopTimerLockHeartbeat();}
                function maybeReleaseTimerEditForRoute(hash,options={}){const d=appState.timerEdit;if(!d||options.silent)return;const h=String(hash||'');if(h==='#/timers'&&d.mode==='create')return;if(h.startsWith('#/timers/')){const info=detailRoute(h.substring('#/timers/'.length),'#/timers');if(info.id===(d.lockTargetId||d.id))return;}cancelTimerEdit(true);}
                async function releaseTimerLock(d,silent){if(!d||!d.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'timer_config',targetId:d.lockTargetId||d.id,lockId:d.lockId})});}catch(err){if(!silent)toast(err.message||'Timer 编辑锁释放失败，将等待自动过期。');}}
                function scheduleTimerLockHeartbeat(){stopTimerLockHeartbeat();appState.timerLockTimer=setTimeout(async()=>{await heartbeatTimerLock();if(appState.timerEdit?.lockId)scheduleTimerLockHeartbeat();},30000);}
                function stopTimerLockHeartbeat(){if(appState.timerLockTimer){clearTimeout(appState.timerLockTimer);appState.timerLockTimer=null;}}
                async function heartbeatTimerLock(){const d=appState.timerEdit;if(!d||!d.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'timer_config',targetId:d.lockTargetId||d.id,lockId:d.lockId})});if(result.success){d.lock=result.data?.lock||d.lock;return;}d.errors=[{message:result.message||'Timer 编辑锁续期失败'}];d.lockId='';stopTimerLockHeartbeat();showTimerEditModal();}catch(err){d.errors=[{message:err.message||'Timer 编辑锁续期失败'}];d.lockId='';stopTimerLockHeartbeat();showTimerEditModal();}}
                async function startTimerManual(id,op){if(!canEditTimer())return;try{const detail=await api(`/api/webadmin/timers/${encodeURIComponent(id)}`);const result=await acquireTimerLock(detail.id);if(!result.success){toast(result.message||'无法获取编辑锁');return;}appState.timerEdit={mode:op,id:detail.id,displayName:detail.displayName||detail.id,scopeMode:detail.scopeMode||'GLOBAL',expectedFingerprint:detail.expectedFingerprint||detail.fingerprint||'',lockId:result.data?.lock?.lockId||'',lock:result.data?.lock||null,lockTargetId:detail.id,targetMode:detail.scopeMode==='PLAYER'?'explicit_target':'global',targetId:'',startPolicyOverride:'',scopeKey:'',errors:[],saving:false};scheduleTimerLockHeartbeat();showTimerManualModal(op);}catch(err){toast(err.message||'无法执行 Timer 操作');}}
                function showTimerManualModal(op){const d=appState.timerEdit;if(!d)return;const title=op==='start'?'手动启动 Timer':(op==='cancel'?'手动取消 Timer':'重置 Timer 运行态'), errs=(d.errors||[]).length?`<ul class="validation-list">${d.errors.map(e=>`<li>${esc(e.message||'操作失败')}</li>`).join('')}</ul>`:'', playerScope=String(d.scopeMode||'GLOBAL').toUpperCase()==='PLAYER';const targetFields=op==='reset'?`<label>scopeKey<input id="timer-manual-scope-key" class="input" value="${esc(d.scopeKey||'')}" placeholder="留空表示重置该 Timer 全部实例" oninput="syncTimerManualDraft()"></label>`:(playerScope?`<input id="timer-manual-target-mode" type="hidden" value="explicit_target"><label>指定玩家<input id="timer-manual-target-id" class="input" value="${esc(d.targetId||'')}" placeholder="PLAYER scope 需要填写玩家 UUID 或名称" oninput="syncTimerManualDraft()"></label><div class="readonly-note">WebAdmin 手动操作没有 ActionEngine 触发玩家上下文；PLAYER 作用域请使用“指定玩家”。</div>`:`<input id="timer-manual-target-mode" type="hidden" value="global"><input id="timer-manual-target-id" type="hidden" value=""><div class="readonly-note">该 Timer 为 GLOBAL 作用域，手动操作会作用于全局运行实例。</div>`)+`${op==='start'?`<label>启动策略覆盖<select id="timer-manual-start-policy" class="select" onchange="syncTimerManualDraft()"><option value="">使用 Timer 定义</option>${['RESTART','IGNORE_IF_RUNNING','FAIL_IF_RUNNING'].map(v=>`<option value="${v}" ${d.startPolicyOverride===v?'selected':''}>${esc(labelTimerStartPolicy(v))}</option>`).join('')}</select></label>`:''}`;openWebAdminModal(title,`<form class="edit-form" data-timer-manual-form="true" data-timer-manual-op="${esc(op)}" data-timer-unified-animated-modal="true" data-timer-modal-uses-webadmin-modal="true" onsubmit="event.preventDefault();submitTimerManualForm(this)"><div class="readonly-note ${op==='reset'?'danger':''}" data-timer-manual-${op}="true" data-timer-manual-start="${op==='start'}" data-timer-manual-cancel="${op==='cancel'}" data-timer-manual-reset="${op==='reset'}"><strong>${esc(d.displayName||d.id)}</strong><span>写入链路会校验 expectedFingerprint 和 edit lock。</span></div>${targetFields}${errs}</form>`,timerManualModalFooter(op,d.saving),{className:'wa-config-modal',dirtyCheck:()=>false,onClose:async()=>{await cancelTimerEdit(true);await dismissWebAdminModal();}});}
                function timerManualModalFooter(op,saving=false){const danger=op==='reset', label=op==='start'?'启动':(op==='cancel'?'取消 Timer':'确认重置');return `<button class="wa-btn ghost" type="button" onclick="closeWebAdminModal()">${icon('close')}<span>取消</span></button><button class="wa-btn ${danger?'danger':'primary'}" type="button" data-timer-manual-submit="true" data-timer-manual-op="${esc(op)}" ${saving?'disabled':''}>${icon(danger?'critical-issue':'check-pass')}<span>${saving?'处理中...':esc(label)}</span></button>`;}
                function submitTimerManualForm(form){const op=String(form?.dataset?.timerManualOp||appState.timerEdit?.mode||'');if(!['start','cancel','reset'].includes(op)){toast('Timer 操作类型无效。');return;}runTimerManual(op);}
                function syncTimerManualDraft(){const d=appState.timerEdit;if(!d)return;d.targetMode=document.getElementById('timer-manual-target-mode')?.value??d.targetMode??'global';d.targetId=document.getElementById('timer-manual-target-id')?.value??d.targetId??'';d.startPolicyOverride=document.getElementById('timer-manual-start-policy')?.value??d.startPolicyOverride??'';d.scopeKey=document.getElementById('timer-manual-scope-key')?.value??d.scopeKey??'';}
                async function runTimerManual(op){syncTimerManualDraft();const d=appState.timerEdit;if(!d)return;d.saving=true;d.errors=[];showTimerManualModal(op);try{const body={expectedFingerprint:d.expectedFingerprint,lockId:d.lockId,targetMode:d.targetMode,targetId:d.targetId,startPolicyOverride:d.startPolicyOverride,scopeKey:d.scopeKey,confirmed:op==='reset'};const result=await api(`/api/webadmin/timers/${encodeURIComponent(d.id)}/${op}`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(body)});if(result.success){const id=d.id;appState.timerEdit=null;stopTimerLockHeartbeat();await dismissWebAdminModal();toast(result.message||'Timer 操作已完成。');await renderTimerDetail(encodeURIComponent(id),{silent:true});return;}d.saving=false;d.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'操作失败'}];showTimerManualModal(op);}catch(err){d.saving=false;d.errors=[{message:err.message||'操作失败'}];showTimerManualModal(op);}}
                async function startTimerDelete(id){if(!canEditTimer())return;try{const detail=await api(`/api/webadmin/timers/${encodeURIComponent(id)}`);const result=await acquireTimerLock(detail.id);if(!result.success){toast(result.message||'无法获取编辑锁');return;}appState.timerEdit={mode:'delete',id:detail.id,displayName:detail.displayName||detail.id,expectedFingerprint:detail.expectedFingerprint||detail.fingerprint||'',lockId:result.data?.lock?.lockId||'',lock:result.data?.lock||null,lockTargetId:detail.id,errors:[],saving:false};scheduleTimerLockHeartbeat();showTimerDeleteModal();}catch(err){toast(err.message||'无法删除 Timer');}}
                function showTimerDeleteModal(){const d=appState.timerEdit;if(!d)return;const errs=(d.errors||[]).length?`<ul class="validation-list">${d.errors.map(e=>`<li>${esc(e.message||'删除失败')}</li>`).join('')}</ul>`:'';openWebAdminModal('删除计时器',`<form class="edit-form" onsubmit="event.preventDefault();deleteTimer()"><div class="readonly-note danger" data-danger-confirm-modal="true"><strong>将删除 ${esc(d.displayName||d.id)}</strong><span>不需要输入完整 ID 或名称；删除会清理该 Timer 的运行中实例。</span></div>${errs}</form>`,dangerousModalFooter(d.saving,'确认删除'),{className:'wa-config-modal',dirtyCheck:()=>false,onClose:async()=>{await cancelTimerEdit(true);await dismissWebAdminModal();}});}
                async function deleteTimer(){const d=appState.timerEdit;if(!d)return;d.saving=true;d.errors=[];showTimerDeleteModal();try{const result=await api(`/api/webadmin/timers/${encodeURIComponent(d.id)}/delete`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({expectedFingerprint:d.expectedFingerprint,lockId:d.lockId,confirmed:true})});if(result.success){appState.timerEdit=null;stopTimerLockHeartbeat();await dismissWebAdminModal();toast(result.message||'Timer 已删除。');location.hash='#/timers';return;}d.saving=false;d.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'删除失败'}];showTimerDeleteModal();}catch(err){d.saving=false;d.errors=[{message:err.message||'删除失败'}];showTimerDeleteModal();}}
                """)
.append("""
                async function renderSignals(options={}){
                  if(!options.silent)setView(loading('正在加载 Signal 频道...'));
                  let channels;try{channels=await api('/api/signals/channels')}catch(err){if(options.silent){toast('Signal 频道实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  storeSignalChannelOptions(channels);
                  appState.signals=channels||[];
                  renderSignalList('',options);
                }
                function renderSignalList(focusId,options={}){
                  const channels=appState.signals||[], filtered=filterSignalChannels(channels);
                  const hasConsumers=channels.filter(c=>consumerCount(c)>0).length;
                  const recent=channels.filter(c=>!isBlank(c.lastTriggeredAt)).length;
                  const warning=channels.filter(c=>['WARNING','ERROR'].includes(String(c.doctorStatus||'').toUpperCase())).length;
                  if(setView(`
                    <div class="page-head"><div><h1>Signal 管理</h1><p>查看频道、消费者、最近触发与逻辑链入口</p></div><span class="pill info">只读模式</span></div>
                    <section class="card-grid">
                      ${metric('频道总数',channels.length,'','signal')}
                      ${metric('有消费者频道',hasConsumers,'','receiver')}
                      ${metric('无消费者频道',channels.length-hasConsumers,(channels.length-hasConsumers)>0?'warning':'','warning')}
                      ${metric('最近触发频道',recent,'','history')}
                      ${metric('最近 Signal 事件',channels.reduce((sum,c)=>sum+Number(c.triggerCountToday||0),0),'','history')}
                      ${metric('Doctor 警告',warning,warning>0?'warning':'','doctor')}
                    </section>
                    <div class="toolbar">
                      <input class="input" id="signal-search" placeholder="搜索频道名" value="${esc(appState.signalFilters.search)}">
                      ${signalFilterSelect('消费者','signal-consumer',['ALL','HAS_CONSUMER','NO_CONSUMER','HAS_LISTENER','HAS_RECEIVER','HAS_RELAY','HAS_JOIN'],appState.signalFilters.consumer)}
                      ${signalFilterSelect('状态','signal-status',['ALL','RECENT','NO_RECENT','WARNING'],appState.signalFilters.status)}
                      ${signalFilterSelect('排序','signal-sort',['RECENT','CHANNEL','CONSUMERS'],appState.signalFilters.sort)}
                    </div>
                    ${filtered.length===0?(channels.length===0?empty('当前暂无 Signal 频道数据。请在游戏内触发 signal 或配置 listener / receiver / action_relay 后刷新。'):empty('没有匹配当前筛选条件的频道。')):signalTable(filtered)}
                    <article class="panel-card" style="margin-top:16px"><h2>预设频道图标说明</h2><p class="muted">6.3 只读阶段按频道状态和类型显示预设 2D 图标，不提供图标编辑或上传。</p></article>
                  `,options))bindSignalFilters(focusId);
                }
                function signalFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span><select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(signalOptionLabel(o))}</option>`).join('')}</select></label>`}
                function signalOptionLabel(v){return {ALL:'全部',HAS_CONSUMER:'有消费者',NO_CONSUMER:'无消费者',HAS_LISTENER:'有监听器',HAS_RECEIVER:'有接收器',HAS_RELAY:'有动作继电器',HAS_JOIN:'有信号汇合',RECENT:'最近有事件',NO_RECENT:'暂无事件',WARNING:'有警告',CHANNEL:'频道名',CONSUMERS:'消费者数量'}[v]||labelSignalSort(v)||v;}
                function bindSignalFilters(focusId){
                  const update=(event)=>{appState.signalFilters.search=document.getElementById('signal-search').value;appState.signalFilters.consumer=document.getElementById('signal-consumer').value;appState.signalFilters.status=document.getElementById('signal-status').value;appState.signalFilters.sort=document.getElementById('signal-sort').value;renderSignalList(event.target.id);};
                  ['signal-search','signal-consumer','signal-status','signal-sort'].forEach(id=>document.getElementById(id).addEventListener(id==='signal-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterSignalChannels(items){
                  const f=appState.signalFilters;
                  const filtered=items.filter(c=>{
                    if(f.search && ![c.channel,c.displayName,c.note].join(' ').toLowerCase().includes(f.search.toLowerCase()))return false;
                    if(f.consumer==='HAS_CONSUMER'&&consumerCount(c)===0)return false;
                    if(f.consumer==='NO_CONSUMER'&&consumerCount(c)>0)return false;
                    if(f.consumer==='HAS_LISTENER'&&Number(c.listenerCount||0)===0)return false;
                    if(f.consumer==='HAS_RECEIVER'&&Number(c.receiverCount||0)===0)return false;
                    if(f.consumer==='HAS_RELAY'&&Number(c.actionRelayCount||0)===0)return false;
                    if(f.consumer==='HAS_JOIN'&&Number(c.signalJoinCount||0)===0)return false;
                    if(f.status==='RECENT'&&isBlank(c.lastTriggeredAt))return false;
                    if(f.status==='NO_RECENT'&&!isBlank(c.lastTriggeredAt))return false;
                    if(f.status==='WARNING'&&!['WARNING','ERROR'].includes(String(c.doctorStatus||'').toUpperCase()))return false;
                    return true;
                  });
                  return filtered.sort((a,b)=>{
                    if(f.sort==='CHANNEL')return String(a.channel||'').localeCompare(String(b.channel||''));
                    if(f.sort==='CONSUMERS')return consumerCount(b)-consumerCount(a);
                    return String(b.lastTriggeredAt||'').localeCompare(String(a.lastTriggeredAt||'')) || String(a.channel||'').localeCompare(String(b.channel||''));
                  });
                }
                function signalTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>频道</th><th>消费者摘要</th><th>监听器</th><th>接收器</th><th>动作继电器</th><th>信号汇合</th><th>最近触发</th><th>最近来源</th><th>诊断</th><th>操作</th></tr></thead><tbody>${items.map(c=>{const target=signalHash(c.channel);return `<tr ${navigationAttr(target,false)}><td><span class="device-name"><span class="device-icon">${icon(c.iconKey||'signal')}</span><span><strong>${esc(c.displayName||c.channel)}</strong><span class="device-subtitle">${esc(c.channel||'未命名频道')}</span>${!isBlank(c.note)?`<span class="device-subtitle">${esc(c.note)}</span>`:''}</span></span></td><td>${consumerSummary(c)}</td><td>${Number(c.listenerCount||0)}</td><td>${Number(c.receiverCount||0)}</td><td>${Number(c.actionRelayCount||0)}</td><td>${Number(c.signalJoinCount||0)}</td><td>${fmtTime(c.lastTriggeredAt)}</td><td>${esc(c.sourceCount?`${c.sourceCount} 个来源`:'暂无')}</td><td>${pill(c.doctorStatus)}</td><td><button class="text-button" ${navigationAttr(target)}>查看详情</button></td></tr>`;}).join('')}</tbody></table></div>`}
                function consumerSummary(c){const count=consumerCount(c);if(count===0)return '<span class="muted">暂无消费者</span>';return `<span class="pill info">${count} 个消费者</span>`}
                function channelListenerTable(listeners,detail){
                  const rows=(listeners||[]).slice(0,5);
                  if(rows.length===0)return empty('当前频道暂无监听器。');
                  const more=(listeners||[]).length>rows.length?`<p class="muted">当前只在主列表显示前 ${rows.length} 个监听器；完整 listener 字段可在“完整详情”中查看。</p>`:'';
                  return `${detailTableWrap(`<table class="wa-table wa-detail-table"><thead><tr><th>监听器名称 / ID</th><th>状态</th><th>冷却时间</th><th>动作数量</th><th>最后触发</th><th>操作</th></tr></thead><tbody>${rows.map(listener=>{
                    const id=listener.id||listener.name||'', cfg=listener.basicConfig||{}, enabled=listener.enabled ?? cfg.enabled, cooldown=listener.cooldownTicks ?? cfg.cooldownTicks, actionCount=listener.actionCount ?? (listener.actions||[]).length;
                    return `<tr><td><span class="device-name"><span class="device-icon">${icon('listener-receiver')}</span><span><strong>${esc(listener.name||id||'未命名监听器')}</strong><span class="device-subtitle">${esc(id||'无稳定 ID')}</span></span></span></td><td>${pill(enabled!==false?'OK':'WARNING')} ${esc(enabled!==false?'启用':'停用')}</td><td>${esc(formatTicks(cooldown)||'0 tick')}</td><td>${esc(actionCount)}</td><td>${fmtTime(listener.lastTriggeredAt)}</td><td><div class="wa-action-cell">${id?waButton('查看','eye',navigationAttr(listenerHash(id)),'ghost'):waButton('查看','eye','disabled','ghost')}${id&&canEditSignalListenerBasicConfig()?waIconButton('编辑基础配置','settings',htmlHandler(`startSignalListenerBasicConfigEdit(${jsString(id)},${jsString(detail.channel)})`)):waIconButton('编辑基础配置不可用','settings','disabled')}</div></td></tr>`;
                  }).join('')}</tbody></table>`)}${more}`;
                }
                function actionTypeDistributionPanel(actions){
                  const items=actions||[];
                  if(items.length===0)return empty('暂无动作类型分布。');
                  const counts=countBy(items,a=>String(a.type||'UNKNOWN').toUpperCase());
                  return `<div class="wa-distribution-grid">${Object.entries(counts).sort((a,b)=>b[1]-a[1]).map(([type,count])=>`<div class="wa-distribution-item"><span>${icon(actionIcon(type))}</span><strong>${esc(labelActionType(type))}</strong><small>${esc(count)} 个</small></div>`).join('')}</div>`;
                }
                function channelConsumerDetailPanel(detail){
                  const groups=[
                    ['关联接收器',detail.receivers||[],'consumer-receiver'],
                    ['动作继电器',detail.actionRelays||[],'consumer-relay'],
                    ['信号汇合',detail.signalJoins||[],'signal-join'],
                    ['信号设备 / 触发源',detail.sources||[],'signal-device'],
                    ['下游频道',detail.downstreamSignals||[],'active-channel']
                  ];
                  const html=groups.map(([label,items,iconName])=>{
                    const list=(items||[]).slice(0,3);
                    const body=list.length?list.map(item=>{
                      const text=typeof item==='string'?item:(item.name||item.id||item.channel||'未命名');
                      const target=typeof item==='string'?signalHash(item):(item.navigationTarget||item.target||'');
                      return `<span>${target?navigationButton(target,text):esc(text)}</span>`;
                    }).join(''):'<span class="muted">暂无</span>';
                    const more=(items||[]).length>list.length?`<small>另有 ${(items||[]).length-list.length} 项在完整详情中查看</small>`:'';
                    return `<div class="wa-compact-row"><strong>${icon(iconName)} ${esc(label)}</strong><span>${body}</span>${more}</div>`;
                  }).join('');
                  return `<div class="wa-compact-list">${html}</div>`;
                }
                async function renderSignalDetail(channel,options={}){
                  const routeInfo=detailRoute(channel,'#/signals'), decoded=routeInfo.id||'';
                  if(!options.silent)setView(loading('正在加载频道详情...'));
                  let detail;try{detail=await api(`/api/signals/channels/${encodeURIComponent(decoded)}`)}catch(err){if(options.silent){toast('频道详情实时刷新失败，已保留当前页面。');return;}setView(`<section class="wa-page"><div class="back-row">${backButton(routeInfo,'返回 Signal 管理')}</div>${waPageHead('详情暂不可用','当前只读接口尚未提供该频道详情或频道没有可读取数据。',waButton('返回列表','signalbridge-main',navigationAttr('#/signals'),'ghost'))}${err.status===404?empty('频道不存在或当前没有可读取数据。'):errorBlock(err.message)}</section>`);return;}
                  const metadataRes=await settle(`/api/webadmin/channel-metadata?channel=${encodeURIComponent(decoded)}`);
                  if(metadataRes.ok)detail.metadata=metadataRes.data;
                  const listenerConfigResults=await Promise.all((detail.listeners||[]).map(l=>{const ref=l.id||l.name||'';return ref?settle(`/api/webadmin/signal-listener-basic-config/${encodeURIComponent(ref)}`):Promise.resolve({ok:false});}));
                  (detail.listeners||[]).forEach((listener,index)=>{const result=listenerConfigResults[index];if(result&&result.ok)listener.basicConfig=result.data;});
                  const channelMeta=detail.metadata||{}, channelTitle=channelMeta.effectiveDisplayName||detail.channel;
                  const stats=detail.stats||{}, regionCount=Number(stats.regionControllerCount||stats.regionCount||0), totalConsumers=Number(stats.listenerCount||0)+Number(stats.receiverCount||0)+Number(stats.actionRelayCount||0)+Number(stats.signalJoinCount||0)+regionCount;
                  const allActions=[...(detail.actions||[]),...(detail.listeners||[]).flatMap(listener=>listener.actions||[])];
                  const status=(detail.doctorIssues||[]).some(i=>i.severity==='ERROR')?'ERROR':((detail.doctorIssues||[]).length?'WARNING':'OK');
                  const editAction=canEditChannelMetadata()?waButton('编辑基本信息','settings',htmlHandler(`startChannelMetadataEdit(${jsString(detail.channel)})`),'primary'):waButton('编辑基本信息','settings','disabled','ghost');
                  const advancedRows=[
                    ['channel.raw',detail.channel],
                    ['channel.displayName',channelTitle],
                    ['channel.type',labelChannelType(detail.type)],
                    ['channel.status',status],
                    ['stats.lastTriggeredAt',formatDateTime(stats.lastTriggeredAt)],
                    ['stats.totalConsumers',totalConsumers],
                        ['stats.listenerCount',Number(stats.listenerCount||0)],
                        ['stats.receiverCount',Number(stats.receiverCount||0)],
                        ['stats.signalJoinCount',Number(stats.signalJoinCount||0)]
                  ];
                  setView(`<section class="wa-page wa-detail-shell" data-detail-kind="signal">
                    ${detailHeader({back:backButton(routeInfo,'返回 Signal 管理'),kicker:'SignalBridge / 频道详情',iconName:channelMeta.iconKey||'active-channel',title:`频道详情：${channelTitle}`,subtitle:channelMeta.note||'频道说明未配置，当前展示运行态与消费者关系。',copyValue:detail.channel,badges:[`<span class="pill info">ID: ${esc(detail.channel)}</span>`,pill(status),`<span class="pill">${esc(labelChannelType(detail.type))}</span>`],actions:[waButton('查看逻辑链','action-binding',navigationAttr(logicChainResolveHash('channel',detail.channel)),'primary'),waButton('导出频道配置','download','disabled','ghost'),waButton('诊断','doctor-overview',navigationAttr('#/doctor'),'ghost'),waButton('更多','more','disabled','ghost')]})}
                    ${detailTabs(['基本信息','监听器列表','最近事件','触发统计','递归检查'])}
                    <section class="wa-detail-first-row">
                      ${detailCard('基本信息',detailInfoGrid([
                        ['频道名称 Channel',detail.channel],
                        ['显示名称',channelTitle],
                        ['状态',safeHtml(pill(status))],
                        ['创建时间',channelMeta.createdAt||'暂无'],
                        ['最后修改',channelMeta.updatedAt?`${formatDateTime(channelMeta.updatedAt)} · ${channelMeta.updatedBy||'未知用户'}`:'暂无'],
                        ['描述',channelMeta.note||'暂无描述']
                      ]),editAction)}
                      ${detailCard('状态与统计',`${detailStatGrid([
                        {label:'今日触发次数',value:stats.triggerCountToday ?? 0,sub:'today',icon:'today-trigger'},
                        {label:'最后触发时间',value:formatDateTime(stats.lastTriggeredAt),sub:'last triggered',icon:'recent-event'},
                        {label:'总触发次数',value:stats.totalTriggerCount ?? stats.triggerCountTotal ?? 0,sub:'total',icon:'history'},
                        {label:'监听器数量',value:Number(stats.listenerCount||0),sub:'listeners',icon:'consumer-listener'}
                      ])}<h3 class="wa-detail-subhead">消费者关系</h3>${detailConsumerGrid([
                        {label:'关联接收器',value:Number(stats.receiverCount||0),icon:'consumer-receiver',target:'#/receivers'},
                        {label:'关联动作继电器',value:Number(stats.actionRelayCount||0),icon:'consumer-relay',target:'#/devices'},
                        {label:'关联信号设备',value:Number(stats.sourceDeviceCount||stats.sourceCount||0),icon:'signal-device',target:'#/devices'},
                        {label:'关联区域控制器',value:regionCount,icon:'region-controller',target:'#/regions'},
                        {label:'信号汇合',value:Number(stats.signalJoinCount||0),icon:'signal-join',target:'#/signal-joins'}
                      ])}`)}
                    </section>
                    ${detailFixedLayout([
                      detailCard(`监听器列表（共 ${(detail.listeners||[]).length} 个）`,channelListenerTable(detail.listeners||[],detail),'','detail-card-stretchable'),
                      detailCard('消费者关系明细',channelConsumerDetailPanel(detail))
                    ],[
                      detailCard('最近事件',`${compactEventList(detail.recentHistory||[],'暂无最近 Signal 事件。')}<p class="muted"><button class="link-button" ${navigationAttr(historyHash(detail.channel),false)}>查看该频道历史</button></p>`),
                      detailCard('动作类型分布',actionTypeDistributionPanel(allActions)),
                      detailCard('频道诊断',`${doctorList(detail.doctorIssues||[],6)}<p class="muted"><button class="link-button" onclick="location.hash='#/doctor'">查看全局诊断</button></p>`)
                    ],[
                      advancedDetailCard('signals',detail.channel,advancedRows,[
                      {title:'metadata / raw key',rows:advancedRowsFromObject(channelMeta,'metadata')},
                      {title:'history summary / diagnostic summary',rows:advancedRowsFromObject({stats,doctorIssues:detail.doctorIssues,recentHistory:detail.recentHistory},'runtime')},
                      {title:'消费者与下游摘要',rows:advancedRowsFromObject({sources:detail.sources,listeners:detail.listeners,receivers:detail.receivers,actionRelays:detail.actionRelays,signalJoins:detail.signalJoins,actions:detail.actions,downstreamSignals:detail.downstreamSignals},'relations')}
                    ])
                    ])}
                  </section>`,options);
                }
                function logicChain(detail){
                  const sources=detail.sources||[], listeners=detail.listeners||[], receivers=detail.receivers||[], relays=detail.actionRelays||[], joins=detail.signalJoins||[], actions=detail.actions||[], downstream=detail.downstreamSignals||[];
                  return `<div class="wa-flow-chain">
                    <div class="wa-flow-node"><span class="wa-icon-bubble">${icon('signal-device')}</span><strong>触发源</strong>${endpointCompact(sources,'暂无可推断触发源')}</div>
                    <div class="wa-flow-arrow">→</div>
                    <div class="wa-flow-node"><span class="wa-icon-bubble">${icon('active-channel')}</span><strong>${esc((detail.metadata&&detail.metadata.effectiveDisplayName)||detail.channel)}</strong><span class="muted">${esc(detail.channel)}</span><small>${esc(labelChannelType(detail.type))}</small>${pill((detail.doctorIssues||[]).length?'WARNING':'OK')}</div>
                    <div class="wa-flow-arrow">→</div>
                    <div class="wa-flow-node"><span class="wa-icon-bubble">${icon('listener-receiver')}</span><strong>消费者</strong>${endpointCompact([...listeners,...receivers,...relays,...joins],'暂无消费者')}</div>
                    <div class="wa-flow-arrow">→</div>
                    <div class="wa-flow-node"><span class="wa-icon-bubble">${icon('action-overview')}</span><strong>动作 / 下游影响</strong>${actions.length?actions.slice(0,4).map(a=>`<span>${actionButton(a.id,labelActionType(a.type))}：${esc(cleanActionSummary(a.summary||a.name||'-'))}</span>`).join(''):(downstream.length?downstream.map(c=>`<span>下游频道：${esc(c)}</span>`).join(''):'<span class="muted">暂无可用动作详情</span>')}</div>
                  </div>`;
                }
                function endpointCompact(items,emptyText){if(!items||items.length===0)return `<span class="muted">${esc(emptyText)}</span>`;return items.slice(0,4).map(e=>`<span>${navigationButton(e.navigationTarget,e.name||e.id)} <span class="muted">(${esc(labelEndpointType(e.type))})</span></span>`).join('');}
                """)
.append("""
                function signalListenerFieldId(ref){return String(ref||'').replace(/[^a-zA-Z0-9_-]/g,'_');}
                function signalListenerBasicConfigCard(e,detail){
                  const ref=e.id||e.name||'', draft=appState.signalListenerBasicConfigEdit, isEditing=draft&&draft.listenerRef===ref;
                  const cfg=e.basicConfig||{}, lock=cfg.lockStatus||{}, canEdit=canEditSignalListenerBasicConfig(), lockedByOther=!!lock.locked&&!lock.heldByCurrentUser;
                  const enabled=cfg.listenerRef?cfg.enabled:e.enabled, channel=cfg.listenerRef?cfg.channel:e.channel, cooldown=cfg.listenerRef?cfg.cooldownTicks:e.cooldownTicks, actionCount=cfg.listenerRef?cfg.actionCount:e.actionCount;
                  const lockHint=lockedByOther?`<div class="readonly-note">${esc(lock.holderUsername||'其他用户')} 正在编辑，锁到期：${esc(formatDateTime(lock.expiresAt))}</div>`:'';
                  const action=isEditing?`<button class="secondary" type="button" ${htmlHandler(`showSignalListenerBasicConfigEditModal(${jsString(ref)},${jsString(detail.channel)})`)}>继续编辑</button>`:(canEdit&&!lockedByOther?`<button class="secondary" type="button" ${htmlHandler(`startSignalListenerBasicConfigEdit(${jsString(ref)},${jsString(detail.channel)})`)}>编辑基础配置</button>`:(canEdit?lockHint:'<span class="muted">需要 EDITOR 或 OWNER 权限才能编辑。</span>'));
                  return `<div class="readonly-note"><div class="kv-row"><span class="muted">基础配置</span><strong>${enabled?'启用':'禁用'} / ${esc(channel||detail.channel||'未设置')} / 冷却 ${esc(cooldown ?? 0)} tick</strong></div><div class="kv-row"><span class="muted">动作列表</span><strong>${esc(actionCount ?? 0)} 个，可在监听器详情管理</strong></div>${action}</div>`;
                }
                function listenerChannelComboOptionsHtml(ref,draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft)), current=normalizeChannelName(draft.channel).toLowerCase(), active=Math.max(0,Number(draft.channelComboIndex||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接保存为新频道</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" ${htmlHandler(`selectSignalListenerBasicConfigChannel(${jsString(ref)},${jsString(c.channel||'')})`)}><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function renderSignalListenerConfigChannelCombo(ref,draft){
                  const id=signalListenerFieldId(ref), open=draft.channelComboOpen?' open':'';
                  return `<div id="listener-channel-combo-${id}" class="channel-combo listener-channel-combo${open}"><div class="channel-combo-control"><input id="listener-channel-${id}" class="input" maxlength="128" value="${esc(draft.channel||'')}" placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${draft.channelComboOpen?'true':'false'}" aria-controls="listener-channel-menu-${id}" ${htmlEvent('onfocus',`openSignalListenerBasicConfigChannelMenu(${jsString(ref)})`)} ${htmlEvent('oninput',`updateSignalListenerBasicConfigDraftFromForm(${jsString(ref)},true)`)} ${htmlEvent('onkeydown',`handleSignalListenerBasicConfigChannelKey(event,${jsString(ref)})`)}><button class="channel-combo-toggle" type="button" ${htmlHandler(`toggleSignalListenerBasicConfigChannelMenu(${jsString(ref)})`)} aria-label="显示已有频道">${icon('chevron-down')}</button></div><div id="listener-channel-menu-${id}" class="channel-combo-menu" role="listbox">${listenerChannelComboOptionsHtml(ref,draft)}</div></div>`;
                }
                function signalListenerBasicConfigForm(e,detail,draft){
                  const id=signalListenerFieldId(draft.listenerRef), errs=draft.errors?.length?`<ul class="validation-list">${draft.errors.map(err=>`<li>${esc(err.field?`${err.field}：`: '')}${esc(err.message||'保存失败')}</li>`).join('')}</ul>`:'';
                  const conflict=draft.conflict?`<div class="readonly-note">${esc(draft.errors?.[0]?.message||'监听器基础配置已发生冲突，请刷新后再编辑。')} <button class="link-button" ${htmlHandler(`reloadSignalListenerBasicConfigAfterConflict(${jsString(draft.listenerRef)},${jsString(draft.routeChannel||detail.channel)})`)}>刷新当前信息</button></div>`:'';
                  return `<form class="edit-form" ${htmlEvent('onsubmit',`event.preventDefault();saveSignalListenerBasicConfig(${jsString(draft.listenerRef)},${jsString(draft.routeChannel||detail.channel)})`)}><label>启用状态<select id="listener-enabled-${id}" class="select"><option value="true" ${draft.enabled?'selected':''}>启用</option><option value="false" ${!draft.enabled?'selected':''}>禁用</option></select></label><label>监听频道${renderSignalListenerConfigChannelCombo(draft.listenerRef,draft)}<span id="listener-channel-hint-${id}" class="muted">${channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError)}</span></label><label>冷却时间（ticks）<input id="listener-cooldown-${id}" class="input" type="number" min="0" max="72000" step="1" value="${esc(draft.cooldownTicks ?? 0)}"></label>${runtimeConditionGatePicker(draft,'conditionGroupId','SIGNAL_LISTENER',draft.listenerId,!draft.lockId||draft.saving,`updateSignalListenerBasicConfigDraftFromForm(${jsString(draft.listenerRef)},false)`)}<p class="readonly-note">正在编辑虚拟监听器基础配置；此窗口不修改动作列表。锁到期：${fmtTime(draft.lock?.expiresAt)}</p>${errs}${conflict}<div class="form-actions"><button class="primary" type="submit" ${draft.saving?'disabled':''}>${draft.saving?'保存中...':'保存'}</button><button class="secondary" type="button" ${htmlHandler('closeWebAdminModal()')}>取消</button></div></form>`;
                }
                function showSignalListenerBasicConfigEditModal(ref,routeChannel){
                  const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;
                  markModalInitialSnapshot('signal_listener_basic_config',draft);
                  openWebAdminModal('编辑监听器基础配置',signalListenerBasicConfigForm({id:ref}, {channel:routeChannel||draft.routeChannel||draft.channel||''}, draft),editModalFooter(draft.saving),{onClose:()=>cancelSignalListenerBasicConfigEdit(ref,routeChannel||draft.routeChannel||draft.channel||''),syncBeforeClose:()=>syncModalDraftBeforeClose('signal_listener_basic_config',ref),dirtyCheck:()=>modalDraftDirty('signal_listener_basic_config',appState.signalListenerBasicConfigEdit)});
                }
                function updateSignalListenerBasicConfigDraftFromForm(ref,openMenu=false){
                  const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;
                  const id=signalListenerFieldId(ref);
                  draft.enabled=(document.getElementById(`listener-enabled-${id}`)?.value||'false')==='true';
                  draft.channel=document.getElementById(`listener-channel-${id}`)?.value||'';
                  draft.cooldownTicks=document.getElementById(`listener-cooldown-${id}`)?.value||'0';
                  draft.conditionGroupId=document.getElementById('runtime-condition-conditionGroupId')?.value||'';
                  if(openMenu){draft.channelComboOpen=true;draft.channelComboIndex=0;setChannelComboQuery(draft,draft.channel);}
                  const hint=document.getElementById(`listener-channel-hint-${id}`);
                  if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);
                  syncSignalListenerBasicConfigChannelCombo(ref);
                }
                function syncSignalListenerBasicConfigChannelCombo(ref){
                  const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;
                  const id=signalListenerFieldId(ref), combo=document.getElementById(`listener-channel-combo-${id}`), menu=document.getElementById(`listener-channel-menu-${id}`), input=document.getElementById(`listener-channel-${id}`);
                  if(combo)combo.classList.toggle('open',!!draft.channelComboOpen);
                  if(input)input.setAttribute('aria-expanded',draft.channelComboOpen?'true':'false');
                  if(menu)menu.innerHTML=listenerChannelComboOptionsHtml(ref,draft);
                }
                function openSignalListenerBasicConfigChannelMenu(ref){const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;updateSignalListenerBasicConfigDraftFromForm(ref,false);closeAllCustomComboboxes();draft.channelComboOpen=true;resetChannelComboQuery(draft);syncSignalListenerBasicConfigChannelCombo(ref);}
                function toggleSignalListenerBasicConfigChannelMenu(ref){const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;updateSignalListenerBasicConfigDraftFromForm(ref,false);const wasOpen=!!draft.channelComboOpen;if(wasOpen){draft.channelComboOpen=false;syncSignalListenerBasicConfigChannelCombo(ref);return;}closeAllCustomComboboxes();draft.channelComboOpen=true;resetChannelComboQuery(draft);syncSignalListenerBasicConfigChannelCombo(ref);document.getElementById(`listener-channel-${signalListenerFieldId(ref)}`)?.focus();}
                function selectSignalListenerBasicConfigChannel(ref,channel){const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;draft.channel=channel||'';draft.channelComboOpen=false;draft.channelComboIndex=0;resetChannelComboQuery(draft);const id=signalListenerFieldId(ref), input=document.getElementById(`listener-channel-${id}`), hint=document.getElementById(`listener-channel-hint-${id}`);if(input)input.value=draft.channel;if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncSignalListenerBasicConfigChannelCombo(ref);}
                function handleSignalListenerBasicConfigChannelKey(event,ref){
                  const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft));
                  if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.channelComboOpen=false;syncSignalListenerBasicConfigChannelCombo(ref);return;}
                  if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex||0)+1:Number(draft.channelComboIndex||0)-1;draft.channelComboIndex=Math.min(max,Math.max(0,next));syncSignalListenerBasicConfigChannelCombo(ref);return;}
                  if(event.key==='Enter'&&draft.channelComboOpen&&options.length>0){event.preventDefault();selectSignalListenerBasicConfigChannel(ref,options[Math.min(options.length-1,Number(draft.channelComboIndex||0))].channel);return;}
                }
                function maybeReleaseSignalListenerBasicConfigEditForRoute(hash){const draft=appState.signalListenerBasicConfigEdit;if(!draft)return;const h=String(hash||'');if(h.startsWith('#/signals/')){const info=detailRoute(h.substring('#/signals/'.length),'#/signals');if(info.id===(draft.routeChannel||draft.channel))return;}if(h.startsWith('#/listeners/')){const info=detailRoute(h.substring('#/listeners/'.length),'#/listeners');if(info.id===draft.listenerRef||info.id===draft.listenerId)return;}if(h.startsWith('#/signal-listeners/')){const info=detailRoute(h.substring('#/signal-listeners/'.length),'#/signal-listeners');if(info.id===draft.listenerRef||info.id===draft.listenerId)return;}releaseSignalListenerBasicConfigLock(draft,true);appState.signalListenerBasicConfigEdit=null;stopSignalListenerBasicConfigLockHeartbeat();}
                async function startSignalListenerBasicConfigEdit(ref,routeChannel){
                  if(!canEditSignalListenerBasicConfig())return;
                  try{
                    const cfg=await api(`/api/webadmin/signal-listener-basic-config/${encodeURIComponent(ref)}`);
                    const result=await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_listener_basic_config',targetId:cfg.listenerId||cfg.listenerRef||ref})});
                    if(!result.success){toast(result.message||'无法获取编辑锁');await renderSignalDetail(encodeURIComponent(routeChannel||cfg.channel||''),{silent:true});return;}
                    const lock=result.data?.lock||{}, channelOptions=await loadSignalChannelOptions(), listenerId=cfg.listenerId||cfg.listenerRef||ref, conditionGateOptions=await loadRuntimeConditionGateOptions(['SIGNAL_LISTENER'],listenerId);
                    appState.signalListenerBasicConfigEdit={listenerRef:cfg.listenerRef||ref,listenerId,displayName:cfg.displayName||ref,enabled:!!cfg.enabled,channel:cfg.channel||'',cooldownTicks:cfg.cooldownTicks ?? 0,conditionGroupId:cfg.conditionGroupId||'',actionCount:cfg.actionCount||0,actionSummaries:cfg.actionSummaries||[],expectedFingerprint:cfg.expectedFingerprint||'',routeChannel:routeChannel||cfg.channel||'',lockId:lock.lockId||'',lock,channelOptions,channelOptionsError:appState.channelOptionsError,conditionGateOptions,channelComboOpen:false,channelComboIndex:0,channelComboQuery:'',channelComboSearchActive:false,errors:[],saving:false,conflict:null};
                    markModalInitialSnapshot('signal_listener_basic_config',appState.signalListenerBasicConfigEdit);
                    scheduleSignalListenerBasicConfigLockHeartbeat();
                    await route({silent:true});
                    showSignalListenerBasicConfigEditModal(cfg.listenerRef||ref,routeChannel||cfg.channel||'');
                  }catch(err){toast(err.message||'无法获取编辑锁');}
                }
                async function cancelSignalListenerBasicConfigEdit(ref,routeChannel){const draft=appState.signalListenerBasicConfigEdit;if(draft&&draft.listenerRef===ref){await releaseSignalListenerBasicConfigLock(draft,false);appState.signalListenerBasicConfigEdit=null;stopSignalListenerBasicConfigLockHeartbeat();}await dismissWebAdminModal();await route({silent:true});}
                async function reloadSignalListenerBasicConfigAfterConflict(ref,routeChannel){const draft=appState.signalListenerBasicConfigEdit;if(draft&&draft.listenerRef===ref)await releaseSignalListenerBasicConfigLock(draft,true);appState.signalListenerBasicConfigEdit=null;stopSignalListenerBasicConfigLockHeartbeat();await renderSignalDetail(encodeURIComponent(routeChannel||''),{silent:true});}
                function scheduleSignalListenerBasicConfigLockHeartbeat(){stopSignalListenerBasicConfigLockHeartbeat();appState.signalListenerBasicConfigLockTimer=setTimeout(async()=>{await heartbeatSignalListenerBasicConfigLock();if(appState.signalListenerBasicConfigEdit)scheduleSignalListenerBasicConfigLockHeartbeat();},30000);}
                function stopSignalListenerBasicConfigLockHeartbeat(){if(appState.signalListenerBasicConfigLockTimer){clearTimeout(appState.signalListenerBasicConfigLockTimer);appState.signalListenerBasicConfigLockTimer=null;}}
                async function heartbeatSignalListenerBasicConfigLock(){const draft=appState.signalListenerBasicConfigEdit;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_listener_basic_config',targetId:draft.listenerId,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;return;}draft.errors=[{message:result.message||'编辑锁续期失败'}];stopSignalListenerBasicConfigLockHeartbeat();await renderSignalDetail(encodeURIComponent(draft.routeChannel||draft.channel||''),{silent:true});}catch(err){draft.errors=[{message:err.message||'编辑锁续期失败'}];stopSignalListenerBasicConfigLockHeartbeat();}}
                async function releaseSignalListenerBasicConfigLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_listener_basic_config',targetId:draft.listenerId,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'编辑锁释放失败，将等待自动过期。');}}
                async function saveSignalListenerBasicConfig(ref,routeChannel){
                  const draft=appState.signalListenerBasicConfigEdit||{listenerRef:ref,routeChannel};
                  updateSignalListenerBasicConfigDraftFromForm(ref,false);
                  draft.saving=true;draft.errors=[];draft.conflict=null;appState.signalListenerBasicConfigEdit=draft;route({silent:true});
                  try{
                    const result=await api(`/api/webadmin/signal-listener-basic-config/${encodeURIComponent(ref)}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({enabled:draft.enabled,channel:draft.channel,cooldownTicks:Number(draft.cooldownTicks),conditionGroupId:draft.conditionGroupId||'',expectedFingerprint:draft.expectedFingerprint,lockId:draft.lockId})});
                    if(result.success){markChannelOptionsDirty({type:'signal_listener_config_changed'});appState.signalListenerBasicConfigEdit=null;stopSignalListenerBasicConfigLockHeartbeat();await dismissWebAdminModal();toast(result.changed?(result.message||'虚拟监听器基础配置已保存。'):'没有变更。');await route({silent:true});return;}
                    draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.conflict||null;appState.signalListenerBasicConfigEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopSignalListenerBasicConfigLockHeartbeat();toast(result.message||'保存失败');await route({silent:true});showSignalListenerBasicConfigEditModal(ref,routeChannel||draft.routeChannel||draft.channel||'');
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.signalListenerBasicConfigEdit=draft;toast(err.message||'保存失败');await route({silent:true});showSignalListenerBasicConfigEditModal(ref,routeChannel||draft.routeChannel||draft.channel||'');}
                }
                function signalListenerActionSummaryCard(actionInfo){
                  const info=actionInfo||{}, listenerId=info.listenerId||'', actions=info.actions||[], lock=info.lockStatus||null, canEdit=canEditSignalListenerActions();
                  const locked=canEdit&&lockHeldByOther(lock), currentLock=canEdit&&lock&&lock.locked&&lock.heldByCurrentUser;
                  const typeSummary=actions.length?uniqueValues(actions.map(a=>labelActionType(a.type))).join(' / '):'未配置';
                  const lockMsg=locked?lockMessage(lock,'虚拟监听器动作列表'):'';
                  const lockAttrs=locked?`disabled title="${esc(lockMsg)}" data-signal-listener-action-lock-disabled="true"`:'';
                  const manageLabel=canEdit?'管理动作':'查看动作';
                  const manageAttrs=htmlHandler(`openSignalListenerActionListModal(${jsString(listenerId)})`);
                  const addAttrs=locked?lockAttrs:htmlHandler(`openSignalListenerActionAddModal(${jsString(listenerId)},true)`);
                  const lockBadge=locked?`<span class="wa-lock-badge" data-signal-listener-action-lock-badge="true">${esc(lockMsg)}</span>`:(currentLock?`<span class="wa-lock-badge" data-signal-listener-action-lock-current="true">正在编辑 · 锁到期：${esc(formatDateTime(lock.expiresAt))}</span>`:'');
                  return `<div class="signal-listener-action-summary" data-signal-listener-action-summary-card="true" data-signal-listener-action-preview-hidden="true"><div class="wa-detail-stat-grid">${detailStatGrid([{label:'动作数量',value:actions.length,sub:'虚拟监听器动作',icon:'action-binding',kind:actions.length?'ok':'warning'},{label:'类型摘要',value:typeSummary,sub:'在动作列表中查看明细',icon:'action-total'},{label:'单条条件',value:actionConditionGateCount(actions),sub:actionConditionGateSummary(actions),icon:'check-pass',kind:actionConditionGateCount(actions)?'ok':'info'}])}</div><p class="muted" data-action-condition-gate-summary="true">单条条件：${esc(actionConditionGateSummary(actions))}。动作明细不在详情页展开；点击“${manageLabel}”查看当前列表，写操作会受编辑锁保护。</p><div class="inline-actions">${waButton(manageLabel,'action-relay',manageAttrs,locked?'ghost is-locked':'primary')}${canEdit?waButton('添加动作','plus',addAttrs,locked?'ghost is-locked':'ghost'):''}${lockBadge}</div></div>`;
                }
                async function acquireSignalListenerActionsLock(listenerId){
                  return await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_listener_actions',targetId:listenerId})});
                }
                function maybeReleaseSignalListenerActionsEditForRoute(hash){
                  const state=appState.signalListenerActionsEdit,d=state?.draft;if(!d)return;
                  const h=String(hash||'');
                  if(h.startsWith('#/listeners/')){const info=detailRoute(h.substring('#/listeners/'.length),'#/listeners');if(info.id===d.listenerId)return;}
                  if(h.startsWith('#/signal-listeners/')){const info=detailRoute(h.substring('#/signal-listeners/'.length),'#/signal-listeners');if(info.id===d.listenerId)return;}
                  cancelSignalListenerActionsEdit(d.listenerId,true);
                }
                async function cancelSignalListenerActionsEdit(listenerId,silent){
                  const d=appState.signalListenerActionsEdit?.draft;
                  if(d&&d.listenerId===listenerId&&d.lockId){try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_listener_actions',targetId:d.listenerId,lockId:d.lockId})});}catch(err){if(!silent)toast(err.message||'编辑锁释放失败，将等待自动过期。');}}
                  appState.signalListenerActionsEdit=null;stopSignalListenerActionsLockHeartbeat();
                }
                function scheduleSignalListenerActionsLockHeartbeat(){stopSignalListenerActionsLockHeartbeat();appState.signalListenerActionsLockTimer=setTimeout(async()=>{await heartbeatSignalListenerActionsLock();if(appState.signalListenerActionsEdit?.draft?.lockId)scheduleSignalListenerActionsLockHeartbeat();},30000);}
                function stopSignalListenerActionsLockHeartbeat(){if(appState.signalListenerActionsLockTimer){clearTimeout(appState.signalListenerActionsLockTimer);appState.signalListenerActionsLockTimer=null;}}
                async function heartbeatSignalListenerActionsLock(){const d=appState.signalListenerActionsEdit?.draft;if(!d||!d.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_listener_actions',targetId:d.listenerId,lockId:d.lockId})});if(result.success){d.lock=result.data?.lock||d.lock;return;}d.errors=[{message:result.message||'编辑锁续期失败'}];d.lockId='';stopSignalListenerActionsLockHeartbeat();}catch(err){d.errors=[{message:err.message||'编辑锁续期失败'}];d.lockId='';stopSignalListenerActionsLockHeartbeat();}}
                async function openSignalListenerActionListModal(listenerId,uiState=null){
                  try{
                    const detail=await api(`/api/webadmin/signal-listeners/${encodeURIComponent(listenerId)}/actions`);
                    let lock=null, lockId='';
                    if(canEditSignalListenerActions()){
                      const result=await acquireSignalListenerActionsLock(detail.listenerId||listenerId);
                      if(result.success){lock=result.data?.lock||{};lockId=lock.lockId||'';detail.lockStatus=lock;scheduleSignalListenerActionsLockHeartbeat();}
                      else{toast(result.message||'无法获取编辑锁，动作列表将以只读状态打开。');const conflict=result.conflict||{};detail.lockStatus=result.data?.lock||detail.lockStatus||(conflict.targetId?{locked:true,heldByCurrentUser:false,holderUsername:conflict.holderUsername||'其他用户',holderRole:conflict.holderRole||'',expiresAt:conflict.expiresAt||''}:null);}
                    }
                    appState.signalListenerActionsEdit={mode:'actionList',detail,draft:{listenerId:detail.listenerId||listenerId,expectedFingerprint:detail.expectedFingerprint||'',lockId,lock,errors:[],saving:false,pendingDeleteIndex:null}};
                    showSignalListenerActionListModal(uiState);
                  }catch(err){toast(err.message||'动作列表加载失败');}
                }
                function signalListenerActionLockStatusFromResult(detail,result){
                  const conflict=result?.conflict||{};
                  return result?.data?.lock||detail?.lockStatus||(conflict.targetId?{locked:true,heldByCurrentUser:false,holderUsername:conflict.holderUsername||'其他用户',holderRole:conflict.holderRole||'',expiresAt:conflict.expiresAt||''}:null);
                }
                function openReadonlySignalListenerActionListFromLock(detail,listenerId,result,message){
                  const info={...(detail||{})};
                  info.lockStatus=signalListenerActionLockStatusFromResult(info,result);
                  appState.signalListenerActionsEdit={mode:'actionList',detail:info,draft:{listenerId:info.listenerId||listenerId,expectedFingerprint:info.expectedFingerprint||'',lockId:'',lock:null,errors:[{message:result?.message||message||'当前动作列表已被锁定，只能查看。'}],saving:false,pendingDeleteIndex:null}};
                  showSignalListenerActionListModal();
                  toast(message||result?.message||'当前动作列表已被锁定，只能查看。');
                }
                function showSignalListenerActionListModal(uiState=null){
                  const state=appState.signalListenerActionsEdit,d=state?.draft,detail=state?.detail;if(!d||!detail)return;
                  const actions=detail.actions||[], canEdit=canEditSignalListenerActions(), locked=canEdit&&lockHeldByOther(detail.lockStatus);
                  const errs=(d.errors||[]).length?`<ul class="validation-list">${d.errors.map(e=>`<li>${esc(e.message||'操作失败')}</li>`).join('')}</ul>`:'';
                  const rows=actions.length?actions.map((action,index)=>signalListenerActionListRow(detail,action,index,d)).join(''):empty('暂无动作');
                  const lockLine=locked?`<div class="readonly-note danger" data-signal-listener-action-list-lock="true">${esc(lockMessage(detail.lockStatus,'虚拟监听器动作列表'))}</div>`:(d.lockId?`<div class="readonly-note" data-signal-listener-action-list-current-lock="true">正在管理动作列表，编辑锁到期：${esc(formatDateTime(d.lock?.expiresAt))}</div>`:'');
                  const controls=canEdit?`<div class="inline-actions">${waButton('添加动作','plus',locked?'disabled data-signal-listener-action-lock-disabled="true"':htmlHandler(`openSignalListenerActionAddModal(${jsString(detail.listenerId)},true)`),'primary')}${waButton('清空动作','critical-issue',locked||actions.length===0?'disabled':htmlHandler(`openSignalListenerActionClearModal(${jsString(detail.listenerId)},true)`),'danger')}</div>`:'';
                  openWebAdminModal('虚拟监听器动作管理',`<div class="edit-form signal-listener-action-list-modal" data-signal-listener-action-list-modal="true" data-signal-listener-actions-managed-in-modal="true" data-signal-listener-action-list-acquires-lock="true" data-signal-listener-action-delete-preserves-scroll="true">${lockLine}${errs}<div class="wa-action-list-editor signal-listener-managed-action-list">${rows}</div>${controls}<p class="muted">动作列表按顺序执行；单条编辑、单条删除和清空都会走编辑锁、expectedFingerprint、audit 与 realtime，不改变 SignalBridge 运行时语义。</p></div>`,waButton('关闭','close','onclick="closeWebAdminModal()"','ghost'),{className:'wa-action-relay-modal',onClose:async()=>{await cancelSignalListenerActionsEdit(detail.listenerId,true);await dismissWebAdminModal();}});
                  restoreSignalListenerActionModalUiState(uiState||d.listUiState);
                }
                function signalListenerActionListRow(detail,action,index,draft){const pending=draft.pendingDeleteIndex===index?signalListenerActionDeleteConfirm(detail,action,index):'', locked=lockHeldByOther(detail.lockStatus), gateText=action.conditionGroupId?`已配置：${action.conditionGroupId}`:'未配置';return `<article class="wa-action-row signal-listener-action-row"><header><div><strong>#${index+1} ${esc(labelActionType(action.type))}</strong><small>${esc(cleanActionSummary(action.summary||action.value||'尚未配置'))}</small></div><span class="pill ${action.enabled?'ok':'warning'}">${action.enabled?'启用':'禁用'}</span></header><div class="identity-grid"><span class="k">类型</span><span class="v">${esc(labelActionType(action.type))} <span class="muted">${esc(action.type||'')}</span></span><span class="k">摘要</span><span class="v">${esc(cleanActionSummary(action.summary||action.value||''))}</span><span class="k">单条条件</span><span class="v" data-action-condition-gate-summary="true">${esc(gateText)}</span><span class="k">冷却</span><span class="v">${esc(action.cooldownTicks||0)} ticks</span></div>${pending}<div class="inline-actions">${canEditSignalListenerActions()?`${waButton('编辑','settings',locked?'disabled':htmlHandler(`openSignalListenerActionEditModal(${jsString(detail.listenerId)},${index},true)`),'ghost')} ${waButton('删除','critical-issue',locked?'disabled':htmlHandler(`requestDeleteSignalListenerAction(${jsString(detail.listenerId)},${index})`),'danger')}`:''}</div></article>`;}
                """)
.append("""
                function signalListenerActionDeleteConfirm(detail,action,index){return `<div class="readonly-note danger" data-signal-listener-action-delete-confirm="true" data-danger-confirm-modal="true"><strong>确认删除 #${index+1} ${esc(labelActionType(action.type))}</strong><span>摘要：${esc(cleanActionSummary(action.summary||action.value||'尚未配置'))}。不需要输入 ID 或名称。</span><div class="inline-actions"><button class="wa-btn danger" type="button" onclick='confirmDeleteSignalListenerAction(${jsString(detail.listenerId)},${index})'>确认删除</button><button class="wa-btn ghost" type="button" onclick='cancelDeleteSignalListenerAction()'>取消</button></div></div>`;}
                function requestDeleteSignalListenerAction(listenerId,index){const state=appState.signalListenerActionsEdit;if(!state||state.mode!=='actionList')return;const ui=captureSignalListenerActionModalUiState();state.draft.pendingDeleteIndex=index;state.draft.listUiState=ui;showSignalListenerActionListModal(ui);}
                function cancelDeleteSignalListenerAction(){const state=appState.signalListenerActionsEdit;if(!state||state.mode!=='actionList')return;const ui=captureSignalListenerActionModalUiState();state.draft.pendingDeleteIndex=null;state.draft.listUiState=ui;showSignalListenerActionListModal(ui);}
                async function confirmDeleteSignalListenerAction(listenerId,index){
                  const state=appState.signalListenerActionsEdit,d=state?.draft;if(!d)return;
                  try{
                    const write=await api(`/api/webadmin/signal-listeners/${encodeURIComponent(listenerId)}/actions/${encodeURIComponent(String(index))}/delete`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({expectedFingerprint:d.expectedFingerprint||'',lockId:d.lockId||'',actionIndex:index,confirmed:true})});
                    const ui=captureSignalListenerActionModalUiState();
                    if(!write.success){toast(write.message||'删除动作失败');d.errors=write.validationErrors&&write.validationErrors.length?write.validationErrors:[{message:write.message||'删除动作失败'}];d.pendingDeleteIndex=index;d.listUiState=ui;showSignalListenerActionListModal(ui);return;}
                    toast(write.message||'动作已删除。');await openSignalListenerActionListModal(listenerId,ui);await renderSignalListenerDetail(encodeURIComponent(listenerId),{silent:true});
                  }catch(err){toast(err.message||'删除动作失败');}
                }
                function captureSignalListenerActionModalUiState(){const body=document.querySelector('#wa-modal-root .wa-modal-body'), active=document.activeElement;return {scrollTop:body?body.scrollTop:0,activeId:active&&active.id?active.id:'',selectionStart:active&&typeof active.selectionStart==='number'?active.selectionStart:null,selectionEnd:active&&typeof active.selectionEnd==='number'?active.selectionEnd:null};}
                function restoreSignalListenerActionModalUiState(state){requestAnimationFrame(()=>{const body=document.querySelector('#wa-modal-root .wa-modal-body');if(body&&state)body.scrollTop=state.scrollTop||0;const active=state&&state.activeId?document.getElementById(state.activeId):null;if(active&&typeof active.focus==='function'){active.focus();if(state.selectionStart!==null&&typeof active.setSelectionRange==='function')active.setSelectionRange(state.selectionStart,state.selectionEnd);}});}
                async function openSignalListenerActionAddModal(listenerId,returnToList=false){
                  if(!canEditSignalListenerActions())return;
                  try{
                    const returnListUiState=returnToList?captureSignalListenerActionModalUiState():null;
                    const [detail,channelOptions,conditionGateOptions]=await Promise.all([api(`/api/webadmin/signal-listeners/${encodeURIComponent(listenerId)}/actions`),loadSignalChannelOptions(),loadRuntimeConditionGateOptions(['SIGNAL_LISTENER_ACTION'],listenerId,{parentTargetType:'SIGNAL_LISTENER',parentTargetId:listenerId}),loadTimerOptions()]);
                    const result=await acquireSignalListenerActionsLock(detail.listenerId||listenerId);
                    if(!result.success){openReadonlySignalListenerActionListFromLock(detail,listenerId,result,'无法获取编辑锁，动作列表将以只读状态打开。');return;}
                    const lock=result.data?.lock||{};
                    appState.signalListenerActionsEdit={mode:'addAction',detail,draft:{listenerId:detail.listenerId||listenerId,actionIndex:(detail.actions||[]).length,actionConditionGateTargetId:`listener:${detail.listenerId||listenerId}:action:${(detail.actions||[]).length}`,returnToList,returnListUiState,type:'signal',value:'',enabled:true,requiresOp:false,cooldownTicks:0,notifyOps:false,conditionGroupId:'',conditionGateOptions,expectedFingerprint:detail.expectedFingerprint||'',lockId:lock.lockId||'',lock,errors:[],saving:false,channelOptions,channelOptionsError:appState.channelOptionsError,channelComboOpen:false,channelComboIndex:0,channelComboQuery:'',channelComboSearchActive:false,...stateActionPayload({})}};
                    scheduleSignalListenerActionsLockHeartbeat();showSignalListenerActionAddModal();
                  }catch(err){toast(err.message||'无法添加动作');}
                }
                async function openSignalListenerActionEditModal(listenerId,index,returnToList=false){
                  if(!canEditSignalListenerActions())return;
                  const returnListUiState=returnToList?captureSignalListenerActionModalUiState():null;
                  try{
                    const [detail,channelOptions,conditionGateOptions]=await Promise.all([api(`/api/webadmin/signal-listeners/${encodeURIComponent(listenerId)}/actions`),loadSignalChannelOptions(),loadRuntimeConditionGateOptions(['SIGNAL_LISTENER_ACTION'],listenerId,{parentTargetType:'SIGNAL_LISTENER',parentTargetId:listenerId}),loadTimerOptions()]);
                    const action=(detail.actions||[])[index];
                    if(!action){toast('要编辑的动作已不存在，请刷新后重试。');return;}
                    const result=await acquireSignalListenerActionsLock(detail.listenerId||listenerId);
                    if(!result.success){openReadonlySignalListenerActionListFromLock(detail,listenerId,result,'无法获取编辑锁，动作列表将以只读状态打开。');return;}
                    const lock=result.data?.lock||{};
                    appState.signalListenerActionsEdit={mode:'editAction',detail,draft:{listenerId:detail.listenerId||listenerId,actionIndex:index,actionConditionGateTargetId:action.actionConditionGateTargetId||`listener:${detail.listenerId||listenerId}:action:${index}`,returnToList,returnListUiState,type:String(action.type||'signal').toLowerCase(),value:action.value||'',enabled:action.enabled!==false,requiresOp:!!action.requiresOp,cooldownTicks:Number(action.cooldownTicks||0),notifyOps:!!action.notifyOps,conditionGroupId:action.conditionGroupId||'',conditionGateOptions,expectedFingerprint:detail.expectedFingerprint||'',lockId:lock.lockId||'',lock,errors:[],saving:false,channelOptions,channelOptionsError:appState.channelOptionsError,channelComboOpen:false,channelComboIndex:0,channelComboQuery:'',channelComboSearchActive:false,...stateActionPayload(action),...timerActionPayload({...action,type:String(action.type||'signal').toLowerCase()})}};
                    scheduleSignalListenerActionsLockHeartbeat();showSignalListenerActionAddModal();
                  }catch(err){toast(err.message||'无法编辑动作');}
                }
                function showSignalListenerActionAddModal(uiState=null){const d=appState.signalListenerActionsEdit?.draft;if(!d)return;const editing=appState.signalListenerActionsEdit?.mode==='editAction';markModalInitialSnapshot('signal_listener_action',d);openWebAdminModal(editing?`编辑虚拟监听器动作 #${Number(d.actionIndex)+1}`:'添加虚拟监听器动作',signalListenerActionAddForm(d),editModalFooter(d.saving),{className:'wa-config-modal',onClose:async()=>{const id=d.listenerId, back=!!d.returnToList, ui=d.returnListUiState;await cancelSignalListenerActionsEdit(id,true);await dismissWebAdminModal();if(back)await openSignalListenerActionListModal(id,ui);},syncBeforeClose:()=>syncSignalListenerActionDraft(),dirtyCheck:()=>modalDraftDirty('signal_listener_action',appState.signalListenerActionsEdit?.draft)});restoreSignalListenerActionModalUiState(uiState);}
                function signalListenerActionTypeOptions(value){return actionTypeOptions(value,'signal_listener');}
                function signalListenerActionValueEditor(d){const signalHtml=`<label class="wa-action-value-field" data-signal-listener-action-signal-only="true">信号频道${renderSignalListenerActionChannelCombo(d)}<span id="signal-listener-action-channel-hint" class="readonly-note">${channelHintHtml(d.value,d.channelOptions||appState.channelOptions||[],d.channelOptionsError||appState.channelOptionsError)}</span></label>`;return renderTypedActionValueEditor('signal-listener-action',d,{oninput:'syncSignalListenerActionDraft()',onchange:'syncSignalListenerActionDraft();showSignalListenerActionAddModal(captureSignalListenerActionModalUiState())',ownerMarker:'data-signal-listener-typed-action-fields="true"',typeMarkers:{command:'data-signal-listener-action-command-fields="true"',message:'data-signal-listener-action-message-field="true"',sound:'data-signal-listener-action-sound-field="true"',state_variable:'data-signal-listener-state-action-fields="true"'},signalHtml});}
                function signalListenerActionAddForm(d){const errs=(d.errors||[]).length?`<ul class="validation-list">${d.errors.map(e=>`<li>${esc(e.message||'输入未通过校验')}</li>`).join('')}</ul>`:'';const commandOnly=String(d.type||'').toLowerCase()==='command'?`<label class="switch-row"><span>需要 OP 权限</span><input id="signal-listener-action-requires-op" type="checkbox" ${d.requiresOp?'checked':''} onchange="syncSignalListenerActionDraft()"></label><label class="switch-row"><span>通知 OP</span><input id="signal-listener-action-notify-ops" type="checkbox" ${d.notifyOps?'checked':''} onchange="syncSignalListenerActionDraft()"></label>`:'';const lockLine=d.lockId?`<div class="readonly-note" data-signal-listener-action-current-lock="true">正在编辑虚拟监听器动作，编辑锁到期：${esc(formatDateTime(d.lock?.expiresAt))}</div>`:'';const editing=appState.signalListenerActionsEdit?.mode==='editAction';const gatePicker=actionConditionGatePicker(d,d,Number(d.actionIndex||0),'SIGNAL_LISTENER_ACTION',d.actionConditionGateTargetId||`listener:${d.listenerId}:action:${Number(d.actionIndex||0)}`,'data-signal-listener-action-condition-gate-picker','syncSignalListenerActionDraft()');return `<form class="edit-form" data-signal-listener-action-dynamic-fields="true" data-signal-listener-single-action-edit="true" data-signal-listener-action-edit-uses-dynamic-fields="true" data-signal-listener-action-edit-preserves-order="true" data-signal-listener-action-preserve-scroll="true" data-action-modal-validation-preserves-scroll="true" onsubmit="event.preventDefault();${editing?'saveSignalListenerActionEdit()':'saveSignalListenerActionAdd()'}">${lockLine}<label>动作类型<select id="signal-listener-action-type" class="select" onchange="changeSignalListenerActionType()">${signalListenerActionTypeOptions(d.type)}</select></label>${signalListenerActionValueEditor(d)}${gatePicker}<label>启用<select id="signal-listener-action-enabled" class="select" onchange="syncSignalListenerActionDraft()"><option value="true" ${d.enabled?'selected':''}>启用</option><option value="false" ${!d.enabled?'selected':''}>禁用</option></select></label><label>冷却时间（ticks）<input id="signal-listener-action-cooldown" class="input" type="number" min="0" step="1" value="${esc(d.cooldownTicks||0)}" oninput="syncSignalListenerActionDraft()"></label>${commandOnly}<p class="readonly-note" data-action-update-keeps-listener-base-fields="true">不会提供 raw JSON；单条条件组为空表示不单独判断，配置后只跳过当前 action，不改变监听器 enabled/channel/cooldown/name。</p>${errs}</form>`;}
                function syncSignalListenerActionDraft(openMenu=false){const d=appState.signalListenerActionsEdit?.draft;if(!d)return;d.type=document.getElementById('signal-listener-action-type')?.value||d.type||'signal';const type=String(d.type||'').toLowerCase(), stateType=type==='state_variable', timerType=type==='timer_start'||type==='timer_cancel';d.value=(stateType||timerType)?'':(document.getElementById('signal-listener-action-value')?.value ?? d.value ?? '');if(stateType)syncStateActionDraftFromForm('signal-listener-action',d);if(timerType)syncTimerActionDraftFromForm('signal-listener-action',d);d.conditionGroupId=document.getElementById(`action-condition-data-signal-listener-action-condition-gate-picker-${Number(d.actionIndex||0)}`)?.value||'';d.enabled=(document.getElementById('signal-listener-action-enabled')?.value ?? String(d.enabled))==='true';d.cooldownTicks=Number(document.getElementById('signal-listener-action-cooldown')?.value ?? d.cooldownTicks ?? 0);d.requiresOp=!stateType&&!timerType&&!!document.getElementById('signal-listener-action-requires-op')?.checked;d.notifyOps=!stateType&&!timerType&&!!document.getElementById('signal-listener-action-notify-ops')?.checked;if(openMenu&&!stateType&&!timerType){d.channelComboOpen=true;d.channelComboIndex=0;setChannelComboQuery(d,d.value);}}
                function changeSignalListenerActionType(){syncSignalListenerActionDraft();const d=appState.signalListenerActionsEdit?.draft;if(!d)return;const next=String(document.getElementById('signal-listener-action-type')?.value||'signal').toLowerCase();d.type=next;d.value=next==='sound'?'minecraft:entity.experience_orb.pickup':'';d.requiresOp=false;d.notifyOps=false;if(next==='state_variable')Object.assign(d,stateActionPayload(d));if(next==='timer_start'||next==='timer_cancel')Object.assign(d,timerActionPayload(d));showSignalListenerActionAddModal(captureSignalListenerActionModalUiState());}
                function renderSignalListenerActionChannelCombo(d){const open=d.channelComboOpen?' open':'';return `<div id="signal-listener-action-channel-combo" class="channel-combo signal-listener-action-channel-combo${open}"><div class="channel-combo-control"><input id="signal-listener-action-value" class="input" maxlength="128" value="${esc(d.value||'')}" placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${d.channelComboOpen?'true':'false'}" aria-controls="signal-listener-action-channel-menu" onfocus='openSignalListenerActionChannelMenu()' oninput='syncSignalListenerActionDraft(true)' onkeydown='handleSignalListenerActionChannelKey(event)'><button class="channel-combo-toggle" type="button" onclick='toggleSignalListenerActionChannelMenu()' aria-label="显示已有频道">${icon('chevron-down')}</button></div><div id="signal-listener-action-channel-menu" class="channel-combo-menu" role="listbox">${signalListenerActionChannelOptionsHtml(d)}</div></div>`;}
                function signalListenerActionChannelOptionsHtml(d){if(d.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';const options=filteredChannelOptions(d.channelOptions||appState.channelOptions||[],channelComboQuery(d)), current=normalizeChannelName(d.value).toLowerCase(), active=Math.max(0,Number(d.channelComboIndex||0));if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接保存为新频道</div>';return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" onclick='selectSignalListenerActionChannel(${jsString(c.channel||'')})'><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');}
                function openSignalListenerActionChannelMenu(){const d=appState.signalListenerActionsEdit?.draft;if(!d)return;syncSignalListenerActionDraft();closeAllCustomComboboxes();d.channelComboOpen=true;d.channelComboIndex=0;resetChannelComboQuery(d);syncSignalListenerActionChannelCombo();}
                function toggleSignalListenerActionChannelMenu(){const d=appState.signalListenerActionsEdit?.draft;if(!d)return;syncSignalListenerActionDraft();if(d.channelComboOpen){d.channelComboOpen=false;syncSignalListenerActionChannelCombo();return;}closeAllCustomComboboxes();d.channelComboOpen=true;resetChannelComboQuery(d);syncSignalListenerActionChannelCombo();document.getElementById('signal-listener-action-value')?.focus();}
                function selectSignalListenerActionChannel(channel){const d=appState.signalListenerActionsEdit?.draft;if(!d)return;d.value=channel||'';d.channelComboOpen=false;d.channelComboIndex=0;resetChannelComboQuery(d);const input=document.getElementById('signal-listener-action-value');if(input)input.value=d.value;const hint=document.getElementById('signal-listener-action-channel-hint');if(hint)hint.innerHTML=channelHintHtml(d.value,d.channelOptions||appState.channelOptions||[],d.channelOptionsError||appState.channelOptionsError);syncSignalListenerActionChannelCombo();}
                function handleSignalListenerActionChannelKey(event){const d=appState.signalListenerActionsEdit?.draft;if(!d)return;const options=filteredChannelOptions(d.channelOptions||appState.channelOptions||[],channelComboQuery(d));if(event.key==='Escape'){event.preventDefault();event.stopPropagation();d.channelComboOpen=false;syncSignalListenerActionChannelCombo();return;}if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();d.channelComboOpen=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(d.channelComboIndex||0)+1:Number(d.channelComboIndex||0)-1;d.channelComboIndex=Math.min(max,Math.max(0,next));syncSignalListenerActionChannelCombo();return;}if(event.key==='Enter'&&d.channelComboOpen&&options.length>0){event.preventDefault();selectSignalListenerActionChannel(options[Math.min(options.length-1,Number(d.channelComboIndex||0))].channel);}}
                function syncSignalListenerActionChannelCombo(){const d=appState.signalListenerActionsEdit?.draft;if(!d)return;const combo=document.getElementById('signal-listener-action-channel-combo'), menu=document.getElementById('signal-listener-action-channel-menu'), input=document.getElementById('signal-listener-action-value');if(combo)combo.classList.toggle('open',!!d.channelComboOpen);if(input)input.setAttribute('aria-expanded',d.channelComboOpen?'true':'false');if(menu)menu.innerHTML=signalListenerActionChannelOptionsHtml(d);}
                async function saveSignalListenerActionAdd(){syncSignalListenerActionDraft();const d=appState.signalListenerActionsEdit?.draft;if(!d)return;d.saving=true;d.errors=[];try{const result=await api(`/api/webadmin/signal-listeners/${encodeURIComponent(d.listenerId)}/actions`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({expectedFingerprint:d.expectedFingerprint,lockId:d.lockId,action:actionDraftPayload(d)})});if(result.success){const returnToList=!!d.returnToList, id=d.listenerId, ui=d.returnListUiState;appState.signalListenerActionsEdit=null;stopSignalListenerActionsLockHeartbeat();await dismissWebAdminModal();toast(result.message||'动作已添加。');if(returnToList)await openSignalListenerActionListModal(id,ui);await renderSignalListenerDetail(encodeURIComponent(id),{silent:true});return;}d.saving=false;d.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'添加失败'}];showSignalListenerActionAddModal(captureSignalListenerActionModalUiState());}catch(err){d.saving=false;d.errors=[{message:err.message||'添加失败'}];showSignalListenerActionAddModal(captureSignalListenerActionModalUiState());}}
                async function saveSignalListenerActionEdit(){syncSignalListenerActionDraft();const d=appState.signalListenerActionsEdit?.draft;if(!d)return;const index=Number(d.actionIndex);d.saving=true;d.errors=[];try{const result=await api(`/api/webadmin/signal-listeners/${encodeURIComponent(d.listenerId)}/actions/${encodeURIComponent(String(index))}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({expectedFingerprint:d.expectedFingerprint,lockId:d.lockId,actionIndex:index,action:actionDraftPayload(d)})});if(result.success){const returnToList=!!d.returnToList, id=d.listenerId, ui=d.returnListUiState;appState.signalListenerActionsEdit=null;stopSignalListenerActionsLockHeartbeat();await dismissWebAdminModal();toast(result.message||'动作已更新。');if(returnToList)await openSignalListenerActionListModal(id,ui);await renderSignalListenerDetail(encodeURIComponent(id),{silent:true});return;}d.saving=false;d.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];showSignalListenerActionAddModal(captureSignalListenerActionModalUiState());}catch(err){d.saving=false;d.errors=[{message:err.message||'保存失败'}];showSignalListenerActionAddModal(captureSignalListenerActionModalUiState());}}
                async function openSignalListenerActionClearModal(listenerId,returnToList=false){if(!canEditSignalListenerActions())return;try{const detail=await api(`/api/webadmin/signal-listeners/${encodeURIComponent(listenerId)}/actions`);const result=await acquireSignalListenerActionsLock(detail.listenerId||listenerId);if(!result.success){openReadonlySignalListenerActionListFromLock(detail,listenerId,result,'无法获取编辑锁，动作列表将以只读状态打开。');return;}const lock=result.data?.lock||{};appState.signalListenerActionsEdit={mode:'clearActions',detail,draft:{listenerId:detail.listenerId||listenerId,returnToList,confirmed:false,expectedFingerprint:detail.expectedFingerprint||'',lockId:lock.lockId||'',lock,errors:[],saving:false}};scheduleSignalListenerActionsLockHeartbeat();showSignalListenerActionClearModal();}catch(err){toast(err.message||'无法清空动作');}}
                function showSignalListenerActionClearModal(){const d=appState.signalListenerActionsEdit?.draft;if(!d)return;const errs=(d.errors||[]).length?`<ul class="validation-list">${d.errors.map(e=>`<li>${esc(e.message||'操作失败')}</li>`).join('')}</ul>`:'';const lockLine=d.lockId?`<div class="readonly-note" data-signal-listener-action-current-lock="true">正在编辑虚拟监听器动作，编辑锁到期：${esc(formatDateTime(d.lock?.expiresAt))}</div>`:'';openWebAdminModal('清空虚拟监听器动作',`<form class="edit-form" onsubmit="event.preventDefault();saveSignalListenerActionClear()">${lockLine}<div class="readonly-note danger" data-signal-listener-action-clear-confirm="true" data-danger-confirm-modal="true"><strong>将清空当前虚拟监听器的全部动作</strong><span>不需要输入 ID 或名称；点击“确认清空”后会清空动作列表，不影响监听器基础配置或其它系统数据。</span></div>${errs}</form>`,dangerousModalFooter(d.saving,'确认清空'),{className:'wa-config-modal',onClose:async()=>{await cancelSignalListenerActionsEdit(d.listenerId,true);await dismissWebAdminModal();},dirtyCheck:()=>false});}
                async function saveSignalListenerActionClear(){const d=appState.signalListenerActionsEdit?.draft;if(!d)return;d.saving=true;d.errors=[];try{const result=await api(`/api/webadmin/signal-listeners/${encodeURIComponent(d.listenerId)}/actions/clear`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({expectedFingerprint:d.expectedFingerprint,lockId:d.lockId,confirmed:true})});if(result.success){const returnToList=!!d.returnToList, id=d.listenerId;appState.signalListenerActionsEdit=null;stopSignalListenerActionsLockHeartbeat();await dismissWebAdminModal();toast(result.message||'动作已清空。');if(returnToList)await openSignalListenerActionListModal(id);await renderSignalListenerDetail(encodeURIComponent(id),{silent:true});return;}d.saving=false;d.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'清空失败'}];showSignalListenerActionClearModal();}catch(err){d.saving=false;d.errors=[{message:err.message||'清空失败'}];showSignalListenerActionClearModal();}}
                """)
.append("""
                function endpointGroups(detail){
                  const groups=[['监听器',detail.listeners||[]],['接收器',detail.receivers||[]],['动作继电器',detail.actionRelays||[]],['信号汇合',detail.signalJoins||[]]];
                  return `<div class="list-stack">${groups.map(([name,items])=>`<div class="endpoint-row"><strong>${esc(name)}：${items.length}</strong>${items.length?items.map(e=>endpointRow(e,detail)).join(''): '<span class="muted">暂无</span>'}</div>`).join('')}</div>`;
                }
                function endpointRow(e,detail){const base=`<span>${navigationButton(e.navigationTarget,e.name||e.id)} <span class="meta">${esc(labelEndpointType(e.type))} / ${esc(labelSubType(e.subType))} / ${e.enabled?'启用':'禁用'}${e.pos?` / ${esc(posText(e.pos))}`:''}</span></span>`;return String(e.type||'').toUpperCase()==='LISTENER'?`${base}${signalListenerBasicConfigCard(e,detail)}`:base;}
                function actionsPanel(detail){
                  const actions=detail.actions||[], downstream=detail.downstreamSignals||[];
                  if(actions.length===0&&downstream.length===0)return empty('暂无可用动作详情。');
                  return `<div class="list-stack">${actions.map(a=>`<div class="event-row"><strong>${actionButton(a.id,labelActionType(a.type))}</strong><span class="meta">${esc(a.ownerName||a.ownerId||'-')} · ${esc(labelEndpointType(a.ownerType))}</span><span>${esc(cleanActionSummary(a.summary||''))}</span></div>`).join('')}${downstream.map(c=>`<div class="event-row"><strong>下游频道</strong><span>${channelButton(c)}</span></div>`).join('')}</div>`;
                }
                function waEnsureState(){
                  appState.uiPages=appState.uiPages||{};
                  appState.listenerFilters=appState.listenerFilters||{search:'',enabled:'ALL',channel:'ALL'};
                  appState.virtualBlockFilters=appState.virtualBlockFilters||{search:'',enabled:'ALL',trigger:'ALL',world:'ALL',doctor:'ALL'};
                  appState.virtualBlockDetailCache=appState.virtualBlockDetailCache||{};
                  appState.deviceFilters=appState.deviceFilters||{search:'',type:'ALL',enabled:'ALL',doctor:'ALL',world:'ALL'};
                  appState.historyFilters=appState.historyFilters||{search:'',channel:'ALL',sourceType:'ALL',result:'ALL',range:'ALL',sort:'NEWEST'};
                  appState.actionFilters=appState.actionFilters||{search:'',type:'ALL',owner:'ALL',result:'ALL',doctor:'ALL',sort:'NAME'};
                  appState.templateFilters=appState.templateFilters||{search:'',type:'ALL',status:'ALL',favorite:'ALL',sort:'NAME'};
                  appState.templateCenterFilters=appState.templateCenterFilters||{search:'',source:'ALL',category:'ALL',placeholder:'ALL'};
                  appState.helpCenterFilters=appState.helpCenterFilters||{search:'',category:'ALL',kind:'ALL',mode:'basic',topicId:'',view:'docs',composing:false,topicListScrollTop:0};
                  appState.receiverFilters=appState.receiverFilters||{search:'',enabled:'ALL',output:'ALL',channel:'ALL'};
                  appState.receiverDetailCache=appState.receiverDetailCache||{};
                  appState.configFilters=appState.configFilters||{search:'',status:'ALL',type:'ALL'};
                  appState.userFilters=appState.userFilters||{search:'',role:'ALL',enabled:'ALL',online:'ALL'};
                  appState.regionFilters=appState.regionFilters||{search:'',world:'ALL',enabled:'ALL',doctor:'ALL',players:'ALL',sort:'NAME'};
                  appState.regionControllerFilters=appState.regionControllerFilters||{search:'',enabled:'ALL',target:'ALL',event:'ALL'};
                  appState.conditionFilters=appState.conditionFilters||{search:'',enabled:'ALL',suite:'ALL'};
                  appState.signalJoinFilters=appState.signalJoinFilters||{search:'',enabled:'ALL',mode:'ALL',scope:'ALL'};
                  appState.conditionDebuggerFilters=appState.conditionDebuggerFilters||{search:'',targetType:'ALL',result:'ALL',conditionGroupId:'',channel:''};
                  appState.advancedDetailOpen=appState.advancedDetailOpen||{};
                  if(appState.selectionCreateVirtualBlock===undefined)appState.selectionCreateVirtualBlock=null;
                  if(appState.virtualBlockDelete===undefined)appState.virtualBlockDelete=null;
                  if(appState.signalListenerCreate===undefined)appState.signalListenerCreate=null;
                  if(appState.signalListenerDelete===undefined)appState.signalListenerDelete=null;
                }
                function waMetric(label,value,sub='',iconName='dashboard',kind=''){
                  return `<article class="wa-metric ${esc(kind)}"><div class="wa-metric-top"><div class="wa-metric-label">${esc(label)}</div><span class="wa-icon-bubble ${esc(kind)} icon-bubble-${iconClassName(iconName)}">${icon(iconName)}</span></div><div class="wa-metric-value">${esc(value)}</div>${sub?`<div class="wa-metric-sub">${esc(sub)}</div>`:''}</article>`;
                }
                function waPageHead(title,desc,actions=''){
                  const helpTopic=helpTopicForPageTitle(title), help=helpTopic?pageHelpLink(helpTopic,currentRouteHash()):'';
                  const actionHtml=[helpReturnButton(),actions,help].filter(Boolean).join('');
                  return `<div class="wa-head"><div><h1>${esc(title)}</h1><p>${esc(desc)}</p></div>${actionHtml?`<div class="wa-actions">${actionHtml}</div>`:''}</div>`;
                }
                function waButton(label,iconName='',attrs='',kind='ghost'){
                  const safeAttrs=String(attrs||''), typeAttr=/\\btype\\s*=/.test(safeAttrs)?'':'type="button" ';
                  return `<button class="wa-btn ${esc(kind)}" ${typeAttr}${safeAttrs}>${iconName?icon(iconName):''}<span>${esc(label)}</span></button>`;
                }
                function waIconButton(label,iconName,attrs=''){
                  const safeAttrs=String(attrs||''), typeAttr=/\\btype\\s*=/.test(safeAttrs)?'':'type="button" ';
                  return `<button class="wa-icon-btn" ${typeAttr}aria-label="${esc(label)}" title="${esc(label)}" ${safeAttrs}>${icon(iconName)}</button>`;
                }
                function helpTopicForPageTitle(title){
                  const text=String(title||'');
                  if(text.includes('总览'))return 'getting-started.overview';
                  if(text.includes('SignalBridge'))return 'signalbridge.channel-basics';
                  if(text.includes('信号汇合'))return 'signal-join.basics';
                  if(text.includes('调度器')||text.includes('计时器'))return 'timer.delay';
                  if(text.includes('信号监听器'))return 'signalbridge.listener-flow';
                  if(text.includes('接收器'))return 'device-trigger.references';
                  if(text.includes('条件组'))return 'condition.group-basics';
                  if(text.includes('条件调试'))return 'debugger.doctor-replay';
                  if(text.includes('状态变量'))return 'state-variable.basics';
                  if(text.includes('逻辑链'))return 'logic-chain.viewer';
                  if(text.includes('模板中心')||text.includes('模板详情'))return 'templates.prefab';
                  if(text.includes('配置时间轴')||text.includes('保存点')||text.includes('Snapshot')||text.includes('Rollback'))return 'snapshot.rollback';
                  if(text.includes('Doctor')||text.includes('信号诊断'))return 'debugger.doctor-replay';
                  if(text.includes('区域控制器')||text.includes('区域列表')||text.includes('区域详情'))return 'region.controller';
                  if(text.includes('虚拟方块')||text.includes('信号设备')||text.includes('设备'))return 'device-trigger.references';
                  if(text.includes('动作列表')||text.includes('动作系统')||text.includes('动作模板'))return 'action.config-basics';
                  if(text.includes('事件历史'))return 'signalbridge.channel-basics';
                  if(text.includes('配置管理')||text.includes('系统设置'))return 'getting-started.overview';
                  return '';
                }
                function helpTopicHash(topicId='',returnTo=currentRouteHash()){
                  const params=new URLSearchParams();
                  if(!isBlank(topicId))params.set('topic',String(topicId));
                  if(isValidReturnHash(returnTo))params.set('returnTo',returnTo);
                  const qs=params.toString();
                  return `#/help${qs?'?'+qs:''}`;
                }
                function pageHelpLink(topicId,returnTo=currentRouteHash()){
                  return waButton('帮助','help-center',navDataAttr(helpTopicHash(topicId,returnTo),'打开帮助')+` data-page-help-link="true" data-page-help-topic="${esc(topicId)}" data-page-help-return-to="${esc(returnTo)}"`,'ghost');
                }
                function safeHtml(value){return {__waHtml:String(value||'')};}
                function detailValue(value){
                  if(value&&typeof value==='object'&&Object.prototype.hasOwnProperty.call(value,'__waHtml'))return value.__waHtml;
                  if(value===null||value===undefined||value==='')return '<span class="muted">暂无</span>';
                  return esc(value);
                }
                async function copyTextToClipboard(value){
                  try{
                    if(navigator.clipboard&&navigator.clipboard.writeText)await navigator.clipboard.writeText(String(value||''));
                    else throw new Error('clipboard unavailable');
                    toast('已复制。');
                  }catch(_){
                    toast('当前浏览器不允许直接复制，请手动选择文本复制。');
                  }
                }
                async function loadHelpCatalog(force=false){
                  if(!force&&appState.helpCatalog)return appState.helpCatalog;
                  appState.helpCatalog=await api('/api/webadmin/help');
                  return appState.helpCatalog;
                }
                function helpKindLabel(kind){return {topic:'模块',example:'示例',troubleshooting:'排错',glossary:'术语'}[String(kind||'topic')]||kind;}
                const HELP_CENTER_VIEW_ROUTE_MARKERS='#/help?view=examples #/help?view=troubleshooting #/help?view=glossary';
                function helpViewLabel(view){return {docs:'文档区',examples:'示例中心',troubleshooting:'排错中心',glossary:'术语表'}[String(view||'docs')]||'文档区';}
                function helpNormalizeView(view){const value=String(view||'docs');return ['docs','examples','troubleshooting','glossary'].includes(value)?value:'docs';}
                function helpKindForView(view){return {docs:'topic',examples:'example',troubleshooting:'troubleshooting',glossary:'glossary'}[helpNormalizeView(view)]||'topic';}
                function helpViewHash(view='docs',topicId='',returnTo=''){
                  const normalized=helpNormalizeView(view), params=new URLSearchParams();
                  if(normalized!=='docs')params.set('view',normalized);
                  if(normalized==='docs'&&!isBlank(topicId))params.set('topic',String(topicId));
                  if(isValidReturnHash(returnTo))params.set('returnTo',returnTo);
                  const qs=params.toString();
                  return `#/help${qs?'?'+qs:''}`;
                }
                function helpStorageGet(key){try{return sessionStorage.getItem(key);}catch(_){return null;}}
                function helpStorageSet(key,value){try{sessionStorage.setItem(key,value);return true;}catch(_){return false;}}
                function helpSafeReturnId(){return `help-${Date.now().toString(36)}-${Math.random().toString(36).slice(2,8)}`.replace(/[^a-zA-Z0-9_-]/g,'');}
                function helpReturnStorageKey(id){return `tzzHelpReturn:${String(id||'').replace(/[^a-zA-Z0-9_-]/g,'')}`;}
                function helpReadReturnContext(id){
                  const safe=String(id||'').replace(/[^a-zA-Z0-9_-]/g,'');
                  if(!safe)return null;
                  try{
                    const raw=helpStorageGet(helpReturnStorageKey(safe));
                    if(!raw)return null;
                    const ctx=JSON.parse(raw);
                    if(!ctx||Date.now()-Number(ctx.timestamp||0)>86400000)return null;
                    return ctx;
                  }catch(_){return null;}
                }
                function helpCaptureReturnContext(){
                  const f=appState.helpCenterFilters||{}, doc=document.querySelector('.help-document-scroll'), list=document.querySelector('.help-topic-list'), right=document.querySelector('.help-right-panel');
                  const context={
                    route:currentRouteHash(),
                    view:helpNormalizeView(f.view||'docs'),
                    topic:f.topicId||parseHashParams(currentRouteHash()).topic||'',
                    mode:f.mode==='professional'?'professional':'basic',
                    search:f.search||'',
                    category:f.category||'ALL',
                    type:f.kind||'ALL',
                    docScrollTop:doc?doc.scrollTop:0,
                    topicListScrollTop:list?list.scrollTop:Number(f.topicListScrollTop||0),
                    rightPanelScrollTop:right?right.scrollTop:Number(f.rightPanelScrollTop||0),
                    timestamp:Date.now()
                  };
                  return context;
                }
                function helpSaveReturnContext(){
                  const context=helpCaptureReturnContext(), id=helpSafeReturnId();
                  if(!helpStorageSet(helpReturnStorageKey(id),JSON.stringify(context)))context.fallbackRoute=helpViewHash(context.view,context.topic);
                  return id;
                }
                function helpTargetRouteWithReturn(targetRoute){
                  const id=helpSaveReturnContext();
                  return appendHashParams(targetRoute,{fromHelp:'1',helpReturn:id});
                }
                function helpReturnContextFromRoute(){
                  const params=parseHashParams(currentRouteHash()), id=String(params.helpReturn||'').replace(/[^a-zA-Z0-9_-]/g,'');
                  if(params.fromHelp!=='1'||!id)return null;
                  const ctx=helpReadReturnContext(id);
                  return ctx?{id,ctx}:null;
                }
                function helpReturnButton(){
                  const context=helpReturnContextFromRoute();
                  if(!context||routeBase(currentRouteHash())==='#/help'||routeBase(currentRouteHash())==='#/examples')return '';
                  return waButton('返回文档','chevron-left',`data-help-return-action="true" data-help-example-center-return-to-help-only-from-inline="true" data-help-return-id="${esc(context.id)}"`,'ghost');
                }
                function restoreHelpReturnContext(id){
                  const ctx=helpReadReturnContext(id);
                  if(!ctx){toast('返回文档上下文已过期。');location.hash='#/help';return;}
                  appState.helpCenterFilters={...(appState.helpCenterFilters||{}),search:ctx.search||'',category:ctx.category||'ALL',kind:ctx.type||'ALL',mode:ctx.mode==='professional'?'professional':'basic',topicId:ctx.topic||'',view:helpNormalizeView(ctx.view||'docs'),topicListScrollTop:Number(ctx.topicListScrollTop||0),documentScrollTop:Number(ctx.docScrollTop||0),rightPanelScrollTop:Number(ctx.rightPanelScrollTop||0),composing:false};
                  appState.pendingHelpReturnContext=ctx;
                  const target=isValidReturnHash(ctx.route)?ctx.route:helpViewHash(ctx.view||'docs',ctx.topic||'');
                  location.hash=target;
                }
                function applyPendingHelpReturnContext(){
                  const ctx=appState.pendingHelpReturnContext;if(!ctx)return;
                  requestAnimationFrame(()=>requestAnimationFrame(()=>{
                    const list=document.querySelector('.help-topic-list'), doc=document.querySelector('.help-document-scroll'), right=document.querySelector('.help-right-panel');
                    if(list)list.scrollTop=Number(ctx.topicListScrollTop||0);
                    if(doc)doc.scrollTop=Number(ctx.docScrollTop||0);
                    if(right)right.scrollTop=Number(ctx.rightPanelScrollTop||0);
                    appState.pendingHelpReturnContext=null;
                  }));
                }
                function helpCategoryTitle(catalog,category){const item=(catalog?.categories||[]).find(c=>String(c.id)===String(category));return item?.title||category||'未分类';}
                function helpAllItems(catalog){
                  const topics=(catalog?.topics||[]).map(item=>({...item,kind:'topic'}));
                  const examples=(catalog?.examples||[]).map(item=>({...item,kind:'example',title:item.title||item.id,summary:item.goal||item.whenToUse||''}));
                  const troubleshooting=(catalog?.troubleshooting||[]).map(item=>({...item,kind:'troubleshooting',title:item.title||item.symptom||item.id,summary:helpPhraseList(item.likelyCauses||[])}));
                  const glossary=(catalog?.glossary||[]).map(item=>({...item,kind:'glossary',title:item.term||item.title||item.id,summary:item.definition||''}));
                  return [...topics,...examples,...troubleshooting,...glossary];
                }
                function helpTopicById(catalog,id){return (catalog?.topics||[]).find(t=>String(t.id)===String(id))||null;}
                function helpExampleById(catalog,id){return (catalog?.examples||[]).find(t=>String(t.id)===String(id))||null;}
                function helpTroubleById(catalog,id){return (catalog?.troubleshooting||[]).find(t=>String(t.id)===String(id))||null;}
                function helpGlossaryById(catalog,id){return (catalog?.glossary||[]).find(t=>String(t.id)===String(id))||null;}
                function helpCategoryKeywordMap(view){
                  const common={
                    signal:['signal','channel','bridge','listener','receiver','join','频道','监听器','接收器'],
                    action:['action','ActionConfig','message','timer_start','state_variable','动作'],
                    condition:['condition','gate','ConditionGroup','debugger','条件'],
                    state:['state','StateVariable','状态变量'],
                    'logic-chain':['logic chain','logic-chain','editor','viewer','component','逻辑链','编辑器'],
                    template:['template','prefab','dry-run','apply','placeholder','模板'],
                    snapshot:['snapshot','rollback','pre_rollback','operationdiff','配置时间轴','保存点','回滚','本次操作变化'],
                    timer:['timer','scheduler','计时器','调度器'],
                    join:['join','barrier','aggregator','汇合'],
                    listener:['listener','SignalListener','监听器'],
                    device:['device','vbd','region','receiver','relay','设备'],
                    diagnostics:['doctor','debugger','history','replay','诊断','排错']
                  };
                  if(view==='examples')return {
                    join:['join','signal join','汇合'],timer:['timer','scheduler','timer_start'],listener:['listener','SignalListener','监听器'],state:['state','StateVariable','state_variable'],condition:['condition','ConditionGroup','gate'],template:['template','prefab','dry-run','apply'],snapshot:['snapshot','rollback','配置时间轴','保存点','回滚'],logicChain:['logic chain','logic-chain','editor'],signal:['signal','SignalBridge','channel']
                  };
                  if(view==='troubleshooting')return {
                    condition:['condition','ConditionGroup','gate','条件'],join:['join','汇合'],timer:['timer','计时器'],listener:['listener','SignalListener','监听器'],template:['template','prefab','apply','模板'],snapshot:['snapshot','rollback','配置时间轴','保存点','degraded','回滚'],editor:['editor','logic chain','lock','fingerprint','编辑器'],signal:['signal','SignalBridge','channel','consumer'],state:['state','StateVariable','状态变量']
                  };
                  if(view==='glossary')return {
                    signal:['signal','SignalBridge','channel','listener','receiver','join','频道'],action:['action','ActionEngine','ActionConfig','动作'],condition:['condition','ConditionGroup','gate','条件'],logicChain:['logic chain','logic-chain','component','focus','逻辑链'],template:['template','prefab','dry-run','placeholder','模板'],snapshot:['snapshot','rollback','pre_rollback','operationdiff','配置时间轴','回滚'],runtime:['runtime','scheduler','replay','gate'],editLock:['edit lock','fingerprint','lock','编辑锁'],device:['device','vbd','region','relay','receiver'],state:['state','StateVariable'],timer:['timer','scheduler']
                  };
                  return common;
                }
                function helpCategoryMatchesKeywords(item,category,view){
                  const map=helpCategoryKeywordMap(view), keywords=map[category]||[];
                  if(!keywords.length)return false;
                  const haystack=helpSearchText(item).toLowerCase();
                  return keywords.some(keyword=>haystack.includes(String(keyword).toLowerCase()));
                }
                function helpItemMatchesCategory(catalog,item,category,view='docs'){
                  if(category==='ALL')return true;
                  if(item.kind==='topic')return String(item.category||'')===category;
                  if(view!=='docs')return helpCategoryMatchesKeywords(item,category,view);
                  const related=[...(item.relatedTopicIds||[]),...(item.relatedTopics||[])];
                  return related.some(id=>String(helpTopicById(catalog,id)?.category||'')===category);
                }
                function helpSearchText(item){
                  return [
                    item.id,item.title,item.summary,item.searchText,item.term,item.definition,item.goal,item.whenToUse,item.professionalExplanation,item.technicalNotes,
                    (item.tags||[]).join(' '),(item.modules||[]).join(' '),(item.aliases||[]).join(' '),(item.steps||[]).join(' '),(item.commonErrors||[]).join(' '),
                    (item.professionalNotes||[]).join(' '),(item.likelyCauses||[]).join(' '),(item.checks||[]).join(' '),(item.fixHints||[]).join(' ')
                  ].join(' ').toLowerCase();
                }
                function helpFilterItems(catalog,overrides={}){
                  const f={...(appState.helpCenterFilters||{}),...(overrides||{})}, q=String(f.search||'').trim().toLowerCase(), kind=String(f.kind||'ALL'), category=String(f.category||'ALL'), view=helpNormalizeView(f.view||appState.helpCenterFilters?.view||'docs');
                  return helpAllItems(catalog).filter(item=>{
                    if(kind!=='ALL'&&String(item.kind)!==kind)return false;
                    if(!helpItemMatchesCategory(catalog,item,category,view))return false;
                    if(!q)return true;
                    return helpSearchText(item).includes(q);
                  });
                }
                function helpCleanPhrase(value){return String(value||'').trim().replace(/[。．.]+$/,'');}
                function helpPhraseList(items){return (items||[]).map(helpCleanPhrase).filter(Boolean).join(' / ');}
                """).toString();
    }
}
