package com.zcpu.tzzmod.webadmin;

final class WebAdminFrontendModalScripts {
    private WebAdminFrontendModalScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                  const handler=appState.modalCloseHandler, dirtyChecker=appState.modalDirtyChecker, syncBeforeClose=appState.modalSyncBeforeClose, rootBefore=document.getElementById('wa-modal-root');
                  appState.modalDirtyChecker=null;appState.modalSyncBeforeClose=null;
                  if(runHandler&&handler){appState.modalCloseHandler=null;appState.modalClosePromise=Promise.resolve(handler()).then(result=>{if(result===false){appState.modalCloseHandler=handler;appState.modalDirtyChecker=dirtyChecker;appState.modalSyncBeforeClose=syncBeforeClose;return result;}if(document.getElementById('wa-modal-root')===rootBefore)return dismissWebAdminModal().then(()=>result);return result;}).finally(()=>{appState.modalClosePromise=null;appState.modalDiscardConfirmOpen=false;});return appState.modalClosePromise;}
                  appState.modalCloseHandler=null;
                  return dismissWebAdminModal();
                }
                function dismissWebAdminModal(){
                  const root=document.getElementById('wa-modal-root');
                  if(!root)return Promise.resolve();
                  appState.modalDirtyChecker=null;appState.modalSyncBeforeClose=null;appState.modalCloseHandler=null;appState.modalDiscardConfirmOpen=false;
                  const discard=document.getElementById('wa-discard-confirm');if(discard)discard.remove();
                  if(String(root.className||'').includes('closing'))return appState.modalDismissPromise||Promise.resolve();
                  root.classList.add('closing');
                  if(root.setAttribute)root.setAttribute('data-modal-closing','true');
                  if(root.dataset)root.dataset.modalClosing='true';
                  let promise;
                  promise=new Promise(resolve=>{
                    let done=false;
                    const finish=()=>{if(done)return;done=true;clearTimeout(timer);root.removeEventListener('animationend',onEnd);root.removeEventListener('animationcancel',finish);if(document.getElementById('wa-modal-root')===root)root.remove();if(appState.modalDismissPromise===promise)appState.modalDismissPromise=null;resolve();};
                    const onEnd=(event)=>{if(event.target===root||String(event.target?.className||'').includes('wa-modal'))finish();};
                    const timer=setTimeout(finish,260);
                    root.addEventListener('animationend',onEnd);
                    root.addEventListener('animationcancel',finish);
                  });
                  appState.modalDismissPromise=promise;
                  return promise;
                }
                function editModalFooter(saving=false){
                  return `<button class="wa-btn ghost" type="button" onclick="closeWebAdminModal()">${icon('close')}<span>取消</span></button><button class="wa-btn primary" type="button" ${saving?'disabled':''} onclick="document.querySelector('#wa-modal-root form')?.requestSubmit()">${icon('check-pass')}<span>${saving?'保存中...':'保存'}</span></button>`;
                }
                const DIRTY_MODAL_ESCAPE_GUARD_MARKER="event.key==='Escape'){if(appState.modalDiscardConfirmOpen)";
                document.addEventListener('keydown',event=>{if(event.key==='Escape'){if(closeAllCustomComboboxes()){event.preventDefault();event.stopPropagation();return;}if(appState.logicChainEditor?.connectionMode&&!document.querySelector('#wa-modal-root .wa-modal')){event.preventDefault();event.stopPropagation();stopLogicChainConnectionMode('escape');return;}if(document.getElementById('condition-node-discard-confirm')){event.preventDefault();event.stopPropagation();cancelConditionNodeDiscardConfirm();return;}if(appState.conditionNodeEditor?.open){event.preventDefault();event.stopPropagation();closeConditionNodeEditor(false);return;}if(appState.containerTemplateCancelConfirm){event.preventDefault();cancelContainerTemplateCancelConfirm();return;}if(appState.modalDiscardConfirmOpen){event.preventDefault();cancelDiscardModalClose();return;}if(appState.openDeviceMoreMenuId){event.preventDefault();closeDeviceMoreMenu(false);return;}closeWebAdminModal();}});
                function unavailableFeature(title='功能暂未开放',message='当前版本没有完整后端支持，因此该操作保持不可用。'){
                  openWebAdminModal(title,`<p>${esc(message)}</p><div class="wa-disabled-note">本轮前端重构只接入只读展示和布局基础，不新增 API 或真实写能力。</div>`);
                }
                """)
.append("""
                function dashboardCard(title,desc,iconName,rows,target){
                  const attrs=target?navDataAttr(target,`打开${title}`):'aria-disabled="true"';
                  return `<article class="wa-dashboard-card${target?'':' disabled'}" ${attrs}><div class="wa-dash-title"><span class="wa-icon-bubble icon-bubble-${iconClassName(iconName)}">${icon(iconName)}</span><div><h2>${esc(title)}</h2><p>${esc(desc)}</p></div></div><div class="wa-metric-list">${rows.map(r=>`<div class="wa-metric-row"><span class="mini-icon mini-icon-${iconClassName(r.icon||'dashboard')}">${icon(r.icon||'dashboard')}</span><span class="label">${esc(r.label)}</span><span class="value">${esc(r.value)}</span></div>`).join('')}</div><span class="wa-card-link">查看详情 →</span></article>`;
                }
                function asArray(result){return result&&result.ok&&Array.isArray(result.data)?result.data:[];}
                async function renderDashboard(options={}){
                  if(!options.silent)setView(loading('正在加载总览...'));
                  const [status,devices,channels,history,doctor,regions,actions,users,joins,timers,conditionGroups,conditionHistory,stateVariables]=await Promise.all([
                    settle('/api/status'),settle('/api/devices'),settle('/api/signals/channels'),settle('/api/signals/history?limit=10'),settle('/api/doctor'),settle('/api/regions'),settle('/api/actions'),settle('/api/webadmin/users'),settle('/api/webadmin/signal-joins'),settle('/api/webadmin/timers'),settle('/api/webadmin/condition-groups'),settle('/api/webadmin/condition-gates/history?limit=20'),settle('/api/webadmin/state-variables?limit=200')
                  ]);
                  const deviceList=asArray(devices), channelList=asArray(channels), regionList=asArray(regions), actionList=asArray(actions), hist=asArray(history);
                  const doc=doctor.ok?doctor.data:{summary:{errorCount:0,warningCount:0,infoCount:0},issues:[]};
                  const userList=asArray(users);
                  const joinList=joins.ok?(joins.data?.joins||[]):[], timerList=timers.ok?(timers.data?.timers||[]):[], conditionGroupList=conditionGroups.ok?(conditionGroups.data?.groups||[]):[], conditionRecordList=conditionHistory.ok?(conditionHistory.data?.records||[]):[], stateVariableList=stateVariables.ok?(stateVariables.data?.variables||[]):[], stateSummary=stateVariables.ok?(stateVariables.data?.summary||{}):{};
                  const receivers=deviceList.filter(d=>String(d.type||'').toUpperCase()==='SIGNAL_RECEIVER');
                  const relays=deviceList.filter(d=>String(d.type||'').toUpperCase()==='ACTION_RELAY');
                  const virtuals=deviceList.filter(d=>String(d.type||'').toUpperCase()==='VIRTUAL_BLOCK_DEVICE');
                  const signalEvents=hist.length;
                  setView(`<section class="wa-page">
                    ${waPageHead('总览','关键系统概览与快速访问',waButton('刷新','refresh','onclick="renderDashboard()"','ghost'))}
                    <section class="wa-dashboard-grid">
                      ${dashboardCard('设备概览','信号设备、虚拟方块和动作继电器状态','device-overview',[
                        {icon:'signal-device',label:'信号设备',value:deviceList.length},
                        {icon:'signal-receiver',label:'信号接收器',value:receivers.length},
                        {icon:'virtual-block-device',label:'虚拟方块设备',value:virtuals.length},
                        {icon:'action-relay',label:'动作继电器',value:relays.length}
                      ],'#/devices')}
                      ${dashboardCard('Doctor 概览','系统健康检查与潜在问题诊断','doctor-overview',[
                        {icon:'critical-issue',label:'严重问题',value:doc.summary?.errorCount||0},
                        {icon:'warning-issue',label:'警告',value:doc.summary?.warningCount||0},
                        {icon:'info-issue',label:'信息提示',value:doc.summary?.infoCount||0},
                        {icon:'check-pass',label:'检查通过',value:Math.max(0,deviceList.length-(doc.summary?.affectedDeviceCount||0))}
                      ],'#/doctor')}
                      ${dashboardCard('Signal 概览','信号通道与监听器运行状态','signal-overview',[
                        {icon:'active-channel',label:'活跃信号通道',value:channelList.length},
                        {icon:'listener-receiver',label:'信号监听/接收',value:channelList.reduce((n,c)=>n+Number(c.listenerCount||0)+Number(c.receiverCount||0),0)},
                        {icon:'recent-event',label:'最近事件',value:signalEvents},
                        {icon:'response-time',label:'平均响应',value:'--'}
                      ],'#/signals')}
                      ${dashboardCard('信号汇合','Join / Barrier / Aggregator 汇聚状态','signal-join',[
                        {icon:'signal-join',label:'汇合配置',value:joinList.length},
                        {icon:'enabled',label:'启用中',value:joinList.filter(j=>j.enabled!==false).length},
                        {icon:'join-status',label:'待满足作用域',value:joinList.reduce((n,j)=>n+Number(j.status?.pendingScopeCount||0),0)},
                        {icon:'active-channel',label:'输出频道',value:uniqueNonBlank(joinList.map(j=>j.outputChannel)).length}
                      ],'#/signal-joins')}
                      ${dashboardCard('条件与调试','条件组、runtime gate 和只读 replay','condition-group',[
                        {icon:'condition-group',label:'条件组',value:conditionGroupList.length},
                        {icon:'runtime-gate',label:'启用条件组',value:conditionGroupList.filter(g=>g.enabled!==false).length},
                        {icon:'condition-debugger',label:'Gate 历史',value:conditionRecordList.length},
                        {icon:'replay',label:'可重放记录',value:conditionRecordList.filter(r=>r.replayable!==false).length}
                      ],'#/condition-groups')}
                      ${dashboardCard('状态变量','全局 / 玩家状态变量只读概览','state-variable',[
                        {icon:'state-variable',label:'变量总数',value:stateSummary.totalCount??stateVariableList.length},
                        {icon:'state-variable-global',label:'全局变量',value:stateSummary.globalCount??stateVariableList.filter(v=>String(v.scope||'').toUpperCase()==='GLOBAL').length},
                        {icon:'state-variable-player',label:'玩家变量',value:stateSummary.playerCount??stateVariableList.filter(v=>String(v.scope||'').toUpperCase()==='PLAYER').length},
                        {icon:'state-action',label:'受控写入入口',value:'只读'}
                      ],'#/state-variables')}
                      ${dashboardCard('调度器 / 计时器','延迟、倒计时和重复执行配置','timer',[
                        {icon:'timer',label:'Timer 配置',value:timerList.length},
                        {icon:'enabled',label:'启用中',value:timerList.filter(t=>t.enabled!==false).length},
                        {icon:'timer-start',label:'运行中实例',value:timerList.reduce((n,t)=>n+Number(t.activeInstanceCount||t.status?.activeInstanceCount||0),0)},
                        {icon:'repeat',label:'重复计时器',value:timerList.filter(t=>String(t.mode||'').toUpperCase()==='REPEAT').length}
                      ],'#/timers')}
                      ${dashboardCard('区域概览','区域控制器和区域运行状态','region-overview',[
                        {icon:'region-controller',label:'区域控制器',value:regionList.length},
                        {icon:'active-region',label:'活跃区域',value:regionList.filter(r=>r.enabled!==false).length},
                        {icon:'action-binding',label:'动作绑定',value:regionList.reduce((n,r)=>n+Number(r.enterActionCount||0)+Number(r.exitActionCount||0)+Number(r.stayActionCount||0),0)},
                        {icon:'today-trigger',label:'今日触发',value:'--'}
                      ],'#/regions')}
                      ${dashboardCard('动作概览','动作执行与配置使用情况','action-overview',[
                        {icon:'action-total',label:'动作总数',value:actionList.length},
                        {icon:'enabled',label:'启用中',value:actionList.filter(a=>a.enabled!==false).length},
                        {icon:'today-trigger',label:'今日触发',value:actionList.reduce((n,a)=>n+Number(a.executionCountToday||0),0)},
                        {icon:'success-rate',label:'执行成功率',value:'--'}
                      ],'#/actions')}
                      ${dashboardCard('用户概览','用户与权限管理','user-overview',[
                        {icon:'user-total',label:'用户总数',value:userList.length||'权限限制'},
                        {icon:'current-user',label:'当前用户',value:appState.me?.displayName||appState.me?.username||'-'},
                        {icon:'current-role',label:'当前角色',value:labelRole(appState.me?.role)},
                        {icon:'session',label:'Session',value:status.ok?status.data.webAdmin?.sessionCount||0:'--'}
                      ],'#/users')}
                    </section>
                  </section>`,options);
                }
                function signalDotClass(c){const status=String(c.doctorStatus||'').toUpperCase();if(status==='ERROR')return 'error';if(status==='WARNING')return 'warning';if(consumerCount(c)===0)return 'muted';return 'ok';}
                function signalStatusLabel(c){const status=String(c.doctorStatus||'UNKNOWN').toUpperCase();if(status==='ERROR')return '错误';if(status==='WARNING')return '警告';if(consumerCount(c)===0)return '无消费者';return '正常';}
                async function renderSignals(options={}){
                  if(!options.silent)setView(loading('正在加载 SignalBridge...'));
                  let channels;try{channels=await api('/api/signals/channels')}catch(err){if(options.silent){toast('SignalBridge 实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  storeSignalChannelOptions(channels);
                  appState.signals=channels||[];
                  renderSignalList('',options);
                }
                function renderSignalList(focusId,options={}){
                  waEnsureState();
                  const channels=appState.signals||[], filtered=filterSignalChannels(channels);
                  const page=waPageItems('signalbridge',filtered,10);
                  const hasConsumers=channels.filter(c=>consumerCount(c)>0).length;
                  const abnormal=channels.filter(c=>['WARNING','ERROR'].includes(String(c.doctorStatus||'').toUpperCase())).length;
                  if(setView(`<section class="wa-page">
                    ${waPageHead('SignalBridge','管理信号频道（Channel），查看消费者连接关系与逻辑链入口。',waButton('刷新','refresh','onclick="renderSignals()"','ghost'))}
                    <section class="wa-card-grid wa-metrics-4">
                      ${waMetric('频道总数',channels.length,'所有已创建的信号频道','channel-total')}
                      ${waMetric('有消费者频道',hasConsumers,'至少有消费者的频道','channel-with-consumers','ok')}
                      ${waMetric('无消费者频道',channels.length-hasConsumers,'暂无任何消费者连接','channel-orphan','warning')}
                      ${waMetric('异常频道',abnormal,'存在问题的频道','channel-error',abnormal?'error':'')}
                    </section>
                    <section class="wa-table-card">
                      <div class="wa-filter-bar">
                        <label class="filter-field search-control"><span>搜索</span><input class="input" id="signal-search" placeholder="搜索频道名或频道 ID..." value="${esc(appState.signalFilters.search)}"></label>
                        <label class="filter-field"><span>状态</span>${waSelect('signal-status',['ALL','RECENT','NO_RECENT','WARNING'],appState.signalFilters.status,signalOptionLabel)}</label>
                        <label class="filter-field"><span>排序</span>${waSelect('signal-sort',['RECENT','CHANNEL','CONSUMERS'],appState.signalFilters.sort,signalOptionLabel)}</label>
                        ${waButton('刷新','refresh','onclick="renderSignals()"','ghost')}
                      </div>
                      ${page.items.length===0?empty(channels.length===0?'当前暂无 SignalBridge 频道数据。':'没有匹配当前筛选条件的频道。'):signalBridgeTable(page.items)}
                      ${waPagination('signalbridge',page)}
                    </section>
                  </section>`,options))bindSignalFilters(focusId);
                }
                function signalBridgeTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>显示名</th><th>Raw Channel</th><th>消费者摘要</th><th>最后触发</th><th>Doctor 状态</th><th>操作</th></tr></thead><tbody>${items.map(c=>{const target=signalHash(c.channel), chainTarget=logicChainResolveHash('channel',c.channel), title=c.displayName||c.channel||'未命名频道';return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看频道 ${title}`)}><td><div class="wa-row-title"><span class="wa-status-dot ${signalDotClass(c)}"></span><span class="wa-truncate"><strong>${esc(title)}</strong></span></div></td><td class="truncate" title="${esc(c.channel||'')}">${esc(c.channel||'-')}</td><td>${signalConsumerSummary(c)}</td><td>${fmtTime(c.lastTriggeredAt)}</td><td>${pill(c.doctorStatus)} <span class="muted">${esc(signalStatusLabel(c))}</span></td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看频道 ${title}`)}>详情</button><button class="wa-btn ghost" ${navDataAttr(chainTarget,`查看逻辑链 ${title}`)}>逻辑链</button>${waIconButton('更多','more','disabled')}</div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function signalConsumerSummary(c){
                  const regionCount=Number(c.regionControllerCount||c.regionCount||c.regionControllerConsumerCount||0);
                  const pairs=[['consumer-listener','listener',Number(c.listenerCount||0)],['consumer-receiver','receiver',Number(c.receiverCount||0)],['consumer-relay','relay',Number(c.actionRelayCount||0)],['consumer-region','region',regionCount]];
                  return `<span class="wa-consumers">${pairs.map(([name,kind,count])=>`<span class="wa-consumer wa-consumer-${kind}">${icon(name)}<span>${esc(count)}</span></span>`).join('')}</span>`;
                }
                function bindSignalFilters(focusId){
                  const update=(event)=>{appState.signalFilters.search=document.getElementById('signal-search')?.value||'';appState.signalFilters.consumer=document.getElementById('signal-consumer')?.value||'ALL';appState.signalFilters.status=document.getElementById('signal-status')?.value||'ALL';appState.signalFilters.sort=document.getElementById('signal-sort')?.value||'RECENT';appState.uiPages.signalbridge=1;renderSignalList(event?.target?.id||'');};
                  ['signal-search','signal-status','signal-sort'].forEach(id=>document.getElementById(id)?.addEventListener(id==='signal-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                const RECEIVER_DEFAULT_PULSE_TICKS=5;
                function firstPulseValue(...values){for(const value of values){if(!isBlank(value))return value;}return null;}
                function normalizePulseTicks(value){const n=Number(value);return Number.isFinite(n)&&n>0?n:null;}
                function receiverPulseValueFrom(data){
                  if(!data)return null;
                  return firstPulseValue(data.pulseTicks,data.receiverPulseTicks,data.config?.pulseTicks,data.configSummary?.pulseTicks,data.extendedConfig?.values?.pulseTicks,data.extendedConfig?.pulseTicks,data.debug?.pulseTicks,data.debugSummary?.pulseTicks,data.blockEntity?.pulseTicks);
                }
                function receiverPulseState(d){
                  const direct=normalizePulseTicks(receiverPulseValueFrom(d));
                  if(direct!==null)return{state:'ready',value:direct,source:'list'};
                  const id=String(d?.id||''), cache=id?(appState.receiverDetailCache||{})[id]:null;
                  if(cache&&cache.status==='ok'){
                    const cached=normalizePulseTicks(receiverPulseValueFrom(cache.detail));
                    if(cached!==null)return{state:'ready',value:cached,source:'detail'};
                    if(isReceiver(d))return{state:'ready',value:RECEIVER_DEFAULT_PULSE_TICKS,source:'default'};
                  }
                  if(cache&&cache.status==='error')return{state:'error',message:cache.message||'详情加载失败'};
                  if(isReceiver(d)&&id)return{state:'loading'};
                  return{state:'missing'};
                }
                function receiverPulseTicks(d){const state=receiverPulseState(d);return state.state==='ready'?state.value:null;}
                function receiverPulseText(ticks){const n=normalizePulseTicks(ticks);return n===null?'--':`${n} tick`;}
                function receiverPulseCell(d){
                  const state=receiverPulseState(d);
                  if(state.state==='ready')return `<span class="wa-pulse-value">${esc(receiverPulseText(state.value))}</span>`;
                  if(state.state==='loading')return '<span class="wa-skeleton-text">加载中</span>';
                  if(state.state==='error')return `<span class="muted" title="${esc(state.message||'详情加载失败')}">--</span>`;
                  return '<span class="muted">--</span>';
                }
                async function refreshVisibleReceiverDetails(items,options={}){
                  waEnsureState();
                  if(document.hidden)return;
                  const now=Date.now(), force=!!options.force;
                  const targets=(items||[]).filter(d=>isReceiver(d)&&!isBlank(d.id)).slice(0,10);
                  const jobs=[];
                  targets.forEach(d=>{
                    const id=String(d.id), cache=appState.receiverDetailCache[id];
                    if(cache&&cache.status==='loading')return;
                    if(cache&&cache.status==='ok'&&!force)return;
                    if(cache&&cache.status==='error'&&!force&&now-Number(cache.at||0)<5000)return;
                    if(cache&&cache.status==='ok')appState.receiverDetailCache[id]={...cache,refreshing:true};
                    else appState.receiverDetailCache[id]={status:'loading',at:now};
                    jobs.push((async()=>{
                      try{
                        const detail=await api(`/api/devices/${encodeURIComponent(id)}`);
                        appState.receiverDetailCache[id]={status:'ok',detail,at:Date.now(),refreshing:false};
                        return true;
                      }catch(err){
                        const latest=appState.receiverDetailCache[id];
                        if(latest&&latest.status==='ok'){
                          appState.receiverDetailCache[id]={...latest,refreshing:false,errorAt:Date.now(),message:err.message||'详情加载失败'};
                          return false;
                        }
                        appState.receiverDetailCache[id]={status:'error',message:err.message||'详情加载失败',at:Date.now()};
                        return true;
                      }
                    })());
                  });
                  if(jobs.length===0)return;
                  const results=await Promise.all(jobs);
                  if(results.some(Boolean)&&currentRouteHash()==='#/receivers')renderReceiverList('',{silent:true,skipDetailRefresh:true});
                }
                function receiverOutputActive(d){return !!(d.outputActive||d.outputting||String(d.outputStatus||'').toUpperCase()==='OUTPUTTING');}
                function isReceiver(d){return String(d.type||'').toUpperCase()==='SIGNAL_RECEIVER'||String(d.subType||'').toLowerCase()==='signal_receiver';}
                async function renderReceivers(options={}){
                  if(!options.silent)setView(loading('正在加载接收器...'));
                  let devices;try{devices=await api('/api/devices')}catch(err){if(options.silent){toast('接收器实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.receivers=(devices||[]).filter(isReceiver);
                  renderReceiverList('',options);
                }
                function renderReceiverList(focusId,options={}){
                  waEnsureState();
                  const receivers=appState.receivers||[], filtered=filterReceivers(receivers);
                  const page=waPageItems('receivers',filtered,10);
                  const enabled=receivers.filter(d=>d.enabled).length, outputting=receivers.filter(receiverOutputActive).length, today=receivers.reduce((n,d)=>n+Number(d.triggerCountToday||d.todayTriggerCount||0),0);
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('接收器','信号接收器用于监听指定频道，当接收到 signal 时输出红石脉冲。',`${waButton('添加接收器','plus','disabled','primary')}${waButton('批量导入','upload','disabled','ghost')}${waButton('导出配置','download','disabled','ghost')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('接收器总数',receivers.length,'较昨日 --','receiver-total')}
                      ${waMetric('启用中',enabled,'已启用接收器','receiver-enabled','ok')}
                      ${waMetric('禁用中',receivers.length-enabled,'已禁用接收器','receiver-disabled','warning')}
                      ${waMetric('当前输出中',outputting,'正在输出红石脉冲','receiver-outputting',outputting?'error':'')}
                      ${waMetric('今日触发次数',today||'--','API 未提供时显示占位','receiver-trigger-today')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="receiver-search" placeholder="搜索接收器名称 / ID / 频道..." value="${esc(appState.receiverFilters.search)}"></label>
                          <label class="filter-field"><span>状态</span>${waSelect('receiver-enabled',['ALL','ENABLED','DISABLED'],appState.receiverFilters.enabled,optionLabel)}</label>
                          <label class="filter-field"><span>输出状态</span>${waSelect('receiver-output',['ALL','OUTPUTTING','IDLE'],appState.receiverFilters.output,v=>({ALL:'全部输出状态',OUTPUTTING:'输出中',IDLE:'空闲'}[v]||v))}</label>
                          ${waButton('刷新','refresh','onclick="renderReceivers()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(receivers.length===0?'当前暂无信号接收器。':'没有匹配当前筛选条件的接收器。'):receiverTable(page.items)}
                        ${waPagination('receivers',page)}
                      </div>
                      ${receiverRightRail(receivers)}
                    </section>
                  </section>`,options);
                  if(rendered){bindReceiverFilters(focusId);if(!options.skipDetailRefresh)refreshVisibleReceiverDetails(page.items,{force:!!options.silent});}
                }
                function receiverTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>接收器名称 / ID</th><th>监听频道</th><th>位置</th><th>脉冲时长</th><th>输出状态</th><th>启用状态</th><th>最近触发</th><th>操作</th></tr></thead><tbody>${items.map(d=>{const target=deviceHash(d,'signal_receiver'), title=d.displayName||d.id;return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看接收器 ${title}`)}><td><span class="device-name"><span class="device-icon">${icon('receiver-row')}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(d.id))}</span></span></span></td><td class="truncate">${channelCell(d.channel)}</td><td class="truncate">${esc(posText(d.pos))}</td><td>${receiverPulseCell(d)}</td><td>${receiverOutputActive(d)?textPill('输出中','error'):textPill('空闲','info')}</td><td>${pill(d.enabled?'OK':'WARNING')} ${esc(d.enabled?'启用':'禁用')}</td><td>${fmtTime(d.lastTriggeredAt)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看接收器 ${title}`)}>查看</button>${waIconButton('更多','more','disabled')}</div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function filterReceivers(items){
                  const f=appState.receiverFilters||{};
                  return items.filter(d=>{const hay=[d.id,d.displayName,d.channel,posText(d.pos),d.world].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.enabled==='ENABLED'&&!d.enabled)return false;if(f.enabled==='DISABLED'&&d.enabled)return false;if(f.output==='OUTPUTTING'&&!receiverOutputActive(d))return false;if(f.output==='IDLE'&&receiverOutputActive(d))return false;return true;});
                }
                function bindReceiverFilters(focusId){
                  const update=(event)=>{appState.receiverFilters.search=document.getElementById('receiver-search').value;appState.receiverFilters.enabled=document.getElementById('receiver-enabled').value;appState.receiverFilters.output=document.getElementById('receiver-output').value;appState.uiPages.receivers=1;renderReceiverList(event.target.id);};
                  ['receiver-search','receiver-enabled','receiver-output'].forEach(id=>document.getElementById(id)?.addEventListener(id==='receiver-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                function receiverRightRail(receivers){
                  const enabled=receivers.filter(d=>d.enabled).length, outputting=receivers.filter(receiverOutputActive).length;
                  const pulseBuckets=receiverPulseBuckets(receivers);
                  return `<aside class="wa-right-rail">
                    <article class="wa-panel"><h2>全部接收器状态分布</h2>${progressList([{label:'输出中',value:outputting,total:receivers.length,kind:'error'},{label:'空闲中',value:Math.max(0,enabled-outputting),total:receivers.length,kind:'ok'},{label:'禁用中',value:receivers.length-enabled,total:receivers.length,kind:'warning'}])}</article>
                    <article class="wa-panel"><h2>脉冲时长分布</h2>${progressList(pulseBuckets)}</article>
                    <article class="wa-panel"><h2>筛选</h2><div class="wa-rail-filter"><label><span>频道</span><input class="input" id="receiver-rail-search" placeholder="搜索频道名称..." oninput="document.getElementById('receiver-search').value=this.value;appState.receiverFilters.search=this.value;appState.uiPages.receivers=1;renderReceiverList('receiver-rail-search')"></label><label><span>启用状态</span>${waSelect('receiver-rail-enabled',['ALL','ENABLED','DISABLED'],appState.receiverFilters.enabled,optionLabel)}</label><div class="wa-button-row"><button class="wa-btn primary" onclick="appState.receiverFilters.enabled=document.getElementById('receiver-rail-enabled').value;appState.uiPages.receivers=1;renderReceiverList()">应用筛选</button><button class="wa-btn ghost" onclick="appState.receiverFilters={search:'',enabled:'ALL',output:'ALL',channel:'ALL'};appState.uiPages.receivers=1;renderReceiverList()">重置筛选</button></div></div></article>
                    <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('批量启用','receiver-enabled','disabled','ghost')}${waButton('批量禁用','receiver-disabled','disabled','ghost')}${waButton('测试脉冲','pulse-duration','disabled','ghost')}${waButton('清空记录','channel-error','disabled','danger')}</div><p class="wa-disabled-note">批量操作和清空记录需要后端写入、权限、CSRF、审计与 edit lock 完整支持，当前保持禁用。</p></article>
                  </aside>`;
                }
                function receiverPulseBuckets(items){
                  const buckets=[{label:'1-10 tick',value:0,total:items.length,kind:'ok'},{label:'11-20 tick',value:0,total:items.length,kind:''},{label:'21+ tick',value:0,total:items.length,kind:'warning'},{label:'加载中',value:0,total:items.length,kind:'info'},{label:'--',value:0,total:items.length,kind:'info'}];
                  items.forEach(d=>{const state=receiverPulseState(d);if(state.state==='ready'){const t=Number(state.value);if(t<=10)buckets[0].value++;else if(t<=20)buckets[1].value++;else buckets[2].value++;}else if(state.state==='loading')buckets[3].value++;else buckets[4].value++;});
                  return buckets.filter(b=>b.value>0||items.length===0);
                }

                function stateVariableHash(id){return `#/state-variables/${encodeURIComponent(id||'')}`;}
                function stateVariableOptionLabel(value){return {ALL:'全部',GLOBAL:'全局',PLAYER:'玩家',BOOLEAN:'布尔',INTEGER:'整数',STRING:'文本'}[String(value||'')]||value;}
                function stateVariableScopeTone(scope){return String(scope||'').toUpperCase()==='PLAYER'?'warning':'info';}
                function stateVariableValueText(item){return String(item?.valueText ?? item?.value ?? '');}
                const STATE_VARIABLE_CREATE_EXPECTED_FINGERPRINT='state_variable_create_v1';
                function stateVariableDefaultDraft(mode='create',detail=null){
                  const create=mode!=='edit', d=detail||{};
                  return {mode:create?'create':'edit',id:create?'':String(d.id||''),scope:String(d.scope||'GLOBAL').toUpperCase(),targetId:String(d.targetId||''),key:String(d.key||''),type:String(d.type||'STRING').toUpperCase(),value:String(d.valueText??d.value??''),displayName:String(d.displayName||''),note:String(d.note||''),expectedFingerprint:create?STATE_VARIABLE_CREATE_EXPECTED_FINGERPRINT:String(d.fingerprint||d.expectedFingerprint||''),lockTargetId:create?'new':String(d.id||''),lockId:'',lock:null,saving:false,errors:[],conflict:null};
                }
                function syncStateVariableEditDraftFromForm(){
                  const d=appState.stateVariableEdit;if(!d)return;
                  d.scope=document.getElementById('state-variable-edit-scope')?.value||d.scope||'GLOBAL';
                  d.targetId=document.getElementById('state-variable-edit-target')?.value??d.targetId??'';
                  d.key=document.getElementById('state-variable-edit-key')?.value??d.key??'';
                  d.type=document.getElementById('state-variable-edit-type')?.value||d.type||'STRING';
                  d.value=document.getElementById('state-variable-edit-value')?.value??d.value??'';
                  d.displayName=document.getElementById('state-variable-edit-name')?.value??d.displayName??'';
                  d.note=document.getElementById('state-variable-edit-note')?.value??d.note??'';
                  if(String(d.scope).toUpperCase()==='GLOBAL')d.targetId='';
                }
                function stateVariableEditForm(d){
                  const edit=d.mode==='edit', identityDisabled=edit?'disabled':'', targetDisabled=edit||String(d.scope||'GLOBAL').toUpperCase()==='GLOBAL', lockLine=d.lockId?`<div class="readonly-note">正在编辑状态变量定义 · 锁到期：${esc(formatDateTime(d.lock?.expiresAt))}</div>`:'<div class="readonly-note">需要获取 state_variable 编辑锁后才能保存。</div>', errs=(d.errors||[]).length?`<ul class="validation-list">${d.errors.map(e=>`<li>${esc(e.field?`${e.field}：`:'')}${esc(e.message||'保存失败')}</li>`).join('')}</ul>`:'', conflict=d.conflict?`<div class="readonly-note danger">状态变量定义已被其它写入修改，请刷新详情后再编辑。</div>`:'';
                  return `<form class="edit-form" data-logic-chain-state-variable-definition-edit="true" data-state-variable-definition-edit="true" data-state-variable-dirty-route-guard="true" data-state-variable-lock-lost-disables-save="true" data-state-variable-save-lock-guard="true" data-logic-chain-global-editor-completion-9-1="true" data-logic-chain-no-freeform-graph-save="true" onsubmit="event.preventDefault();saveStateVariableEdit()">${lockLine}<label>作用域<select id="state-variable-edit-scope" class="select" ${identityDisabled} onchange="syncStateVariableEditDraftFromForm();withPreservedModalScroll(()=>showStateVariableEditModal())">${['GLOBAL','PLAYER'].map(v=>`<option value="${v}" ${String(d.scope||'GLOBAL').toUpperCase()===v?'selected':''}>${esc(stateVariableOptionLabel(v))}</option>`).join('')}</select></label><label>目标 ID<input id="state-variable-edit-target" class="input" maxlength="128" value="${esc(d.targetId||'')}" ${targetDisabled?'disabled':''} oninput="syncStateVariableEditDraftFromForm()" placeholder="PLAYER scope 时填写玩家/目标 ID"></label><label>变量键<input id="state-variable-edit-key" class="input" maxlength="128" value="${esc(d.key||'')}" ${identityDisabled} oninput="syncStateVariableEditDraftFromForm()" placeholder="例如 game.ready"></label><label>类型<select id="state-variable-edit-type" class="select" onchange="syncStateVariableEditDraftFromForm()">${['BOOLEAN','INTEGER','STRING'].map(v=>`<option value="${v}" ${String(d.type||'STRING').toUpperCase()===v?'selected':''}>${esc(stateVariableOptionLabel(v))}</option>`).join('')}</select></label><label>默认 / 当前值<textarea id="state-variable-edit-value" class="input wa-action-textarea" maxlength="512" oninput="syncStateVariableEditDraftFromForm()">${esc(d.value??'')}</textarea></label><label>显示名称<input id="state-variable-edit-name" class="input" maxlength="80" value="${esc(d.displayName||'')}" oninput="syncStateVariableEditDraftFromForm()"></label><label>备注<textarea id="state-variable-edit-note" class="input wa-action-textarea" maxlength="512" oninput="syncStateVariableEditDraftFromForm()">${esc(d.note||'')}</textarea></label><p class="readonly-note">保存走 WebAdmin 写链路：权限、CSRF / same-origin、edit lock、expectedFingerprint、audit、realtime 和自动快照。scope / targetId / key 组成稳定 ID，已有变量暂不支持重命名。</p>${errs}${conflict}</form>`;
                }
                function showStateVariableEditModal(){
                  const d=appState.stateVariableEdit;if(!d)return;
                  markModalInitialSnapshot('state_variable_definition',d);
                  openWebAdminModal(d.mode==='edit'?'编辑状态变量定义':'新建状态变量定义',stateVariableEditForm(d),editModalFooter(d.saving||!d.lockId),{className:'wa-config-modal',onClose:()=>cancelStateVariableEdit(false),syncBeforeClose:()=>syncModalDraftBeforeClose('state_variable_definition'),dirtyCheck:()=>modalDraftDirty('state_variable_definition',appState.stateVariableEdit)});
                }
                async function startStateVariableCreate(){
                  if(!canEditStateVariableDefinitions()){toast('需要 EDITOR 或 OWNER 权限才能编辑状态变量定义。');return;}
                  const d=stateVariableDefaultDraft('create');
                  try{const result=await acquireWebAdminEditLock('state_variable','new');if(!result.success){toast(result.message||'无法获取状态变量编辑锁。');return;}d.lock=result.data?.lock||{};d.lockId=d.lock.lockId||'';}catch(err){toast(err.message||'无法获取状态变量编辑锁。');return;}
                  appState.stateVariableEdit=d;markModalInitialSnapshot('state_variable_definition',d);scheduleStateVariableLockHeartbeat();showStateVariableEditModal();
                }
                async function startStateVariableEdit(id=''){
                  if(!canEditStateVariableDefinitions()){toast('需要 EDITOR 或 OWNER 权限才能编辑状态变量定义。');return;}
                  try{const detail=id?await api(`/api/webadmin/state-variables/${encodeURIComponent(id)}`):(appState.currentStateVariableDetail||{}), d=stateVariableDefaultDraft('edit',detail), result=await acquireWebAdminEditLock('state_variable',d.id);if(!result.success){toast(result.message||'无法获取状态变量编辑锁。');await renderStateVariableDetail(encodeURIComponent(d.id),{silent:true});return;}d.lock=result.data?.lock||{};d.lockId=d.lock.lockId||'';appState.stateVariableEdit=d;markModalInitialSnapshot('state_variable_definition',d);scheduleStateVariableLockHeartbeat();showStateVariableEditModal();}catch(err){toast(err.message||'无法打开状态变量编辑器。');}
                }
                async function saveStateVariableEdit(){
                  const d=appState.stateVariableEdit;if(!d)return;syncStateVariableEditDraftFromForm();if(!d.lockId){d.errors=[{message:'状态变量编辑锁未就绪，无法保存；输入已保留。'}];d.saving=false;toast('状态变量编辑锁未就绪，无法保存；输入已保留。');showStateVariableEditModal();return;}d.saving=true;d.errors=[];d.conflict=null;showStateVariableEditModal();
                  const body={scope:d.scope,targetId:d.targetId,key:d.key,type:d.type,value:d.value,displayName:d.displayName,note:d.note,expectedFingerprint:d.expectedFingerprint,lockId:d.lockId};
                  try{const path=d.mode==='edit'?`/api/webadmin/state-variables/${encodeURIComponent(d.id)}`:'/api/webadmin/state-variables', method=d.mode==='edit'?'PATCH':'POST', result=await api(path,{method,headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(body)});if(result.success){const routeTarget=result.data?.routeTarget||stateVariableHash(result.data?.variable?.id||d.id);appState.stateVariableEdit=null;stopStateVariableLockHeartbeat();await dismissWebAdminModal();toast(result.changed?(result.message||'状态变量定义已保存。'):'没有变更。');if(routeTarget)location.hash=routeTarget;else await renderStateVariablesPage({silent:true});return;}d.saving=false;d.errors=writeResultErrors(result,'状态变量保存失败。');d.conflict=result.conflict||null;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code)){d.lockId='';d.lock=null;stopStateVariableLockHeartbeat();}showStateVariableEditModal();toast(result.message||'状态变量保存失败。');}catch(err){d.saving=false;d.errors=[{message:err.message||'状态变量保存失败。'}];showStateVariableEditModal();toast(err.message||'状态变量保存失败。');}
                }
                async function cancelStateVariableEdit(silent=false){const d=appState.stateVariableEdit;if(d?.lockId)await releaseStateVariableLock(d,silent);appState.stateVariableEdit=null;stopStateVariableLockHeartbeat();await dismissWebAdminModal();}
                function scheduleStateVariableLockHeartbeat(){stopStateVariableLockHeartbeat();appState.stateVariableLockTimer=setTimeout(async()=>{await heartbeatStateVariableLock();if(appState.stateVariableEdit?.lockId)scheduleStateVariableLockHeartbeat();},30000);}
                function stopStateVariableLockHeartbeat(){if(appState.stateVariableLockTimer){clearTimeout(appState.stateVariableLockTimer);appState.stateVariableLockTimer=null;}}
                async function heartbeatStateVariableLock(){const d=appState.stateVariableEdit;if(!d?.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'state_variable',targetId:d.lockTargetId||d.id||'new',lockId:d.lockId})});if(result.success){d.lock=result.data?.lock||d.lock;return;}d.errors=[{message:result.message||'状态变量编辑锁续期失败'}];d.lockId='';stopStateVariableLockHeartbeat();showStateVariableEditModal();}catch(err){d.errors=[{message:err.message||'状态变量编辑锁续期失败'}];d.lockId='';stopStateVariableLockHeartbeat();showStateVariableEditModal();}}
                async function releaseStateVariableLock(d,silent=false){if(!d?.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'state_variable',targetId:d.lockTargetId||d.id||'new',lockId:d.lockId})});}catch(err){if(!silent)toast(err.message||'状态变量编辑锁释放失败，将等待自动过期。');}}
                function maybeReleaseStateVariableEditForRoute(hash,options={}){const d=appState.stateVariableEdit;if(!d||options.silent)return true;const h=String(hash||'');if(d.mode==='create'&&h==='#/state-variables')return true;if(d.mode==='edit'&&h.startsWith('#/state-variables/')){const info=detailRoute(h.substring('#/state-variables/'.length),'#/state-variables');if(info.id===d.id)return true;}syncStateVariableEditDraftFromForm();if(modalDraftDirty('state_variable_definition',d)){const keep=d.mode==='create'?'#/state-variables':stateVariableHash(d.id);if(location.hash!==keep)setTimeout(()=>{if(appState.stateVariableEdit&&location.hash!==keep)location.hash=keep;},0);toast('状态变量定义有未保存修改，请先保存或取消编辑。');showStateVariableEditModal();return false;}releaseStateVariableLock(d,true);appState.stateVariableEdit=null;stopStateVariableLockHeartbeat();return true;}
                async function renderStateVariablesPage(options={}){
                  if(!options.silent)setView(loading('正在加载状态变量...'));
                  const f=appState.stateVariableFilters||{search:'',scope:'ALL',type:'ALL',target:''};
                  const params=new URLSearchParams();
                  if(f.search)params.set('q',f.search);
                  if(f.scope&&f.scope!=='ALL')params.set('scope',f.scope);
                  if(f.type&&f.type!=='ALL')params.set('type',f.type);
                  if(f.target)params.set('targetId',f.target);
                  params.set('limit','500');
                  let data;try{data=await api(`/api/webadmin/state-variables?${params.toString()}`)}catch(err){if(options.silent){toast('状态变量实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.stateVariablesData=data||{variables:[],summary:{}};
                  renderStateVariableList('',options);
                }
                function renderStateVariableList(focusId,options={}){
                  waEnsureState();
                  const data=appState.stateVariablesData||{variables:[],summary:{}}, variables=data.variables||[], summary=data.summary||{};
                  const page=waPageItems('stateVariables',variables,12);
                """).toString();
    }
}
