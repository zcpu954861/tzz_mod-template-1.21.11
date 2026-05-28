package com.zcpu.tzzmod.webadmin;

final class WebAdminFrontendCoreScripts {
    private WebAdminFrontendCoreScripts() {
    }

    static String appJs() {
        return new StringBuilder()
                .append("""
                class ApiError extends Error{
                  constructor(status, code, message){super(message || '请求失败');this.status=status;this.code=code || 'ERROR';}
                }
                const TZZ_WEBADMIN_ASSET_VERSION='8.18-snapshot-rollback-timeline-clickfix';
                if(typeof window!=='undefined')window.__TZZ_WEBADMIN_ASSET_VERSION=TZZ_WEBADMIN_ASSET_VERSION;
                const appState={me:null,status:null,capabilities:null,channelOptions:null,channelOptionsError:null,channelOptionsDirty:false,onlinePlayerOptions:null,onlinePlayerOptionsError:null,currentDeviceDetail:null,deviceConfigEdit:null,deviceMetadataEdit:null,deviceMetadataLockTimer:null,deviceBasicConfigEdit:null,deviceBasicConfigLockTimer:null,deviceExtendedConfigEdit:null,deviceExtendedConfigLockTimer:null,actionRelayActionsEdit:null,actionRelayActionsLockTimer:null,vbdNativeTriggerEdit:null,vbdNativeTriggerLockTimer:null,interactionItemMatcherEdit:null,interactionItemMatcherLockTimer:null,containerTemplateSession:null,containerTemplateSessionLockTimer:null,containerTemplateSessionStatusTimer:null,containerTemplateCancelConfirm:null,singleItemSubmitSession:null,singleItemSubmitSessionLockTimer:null,singleItemSubmitSessionStatusTimer:null,singleItemSubmitCancelConfirm:null,channelMetadataEdit:null,channelMetadataLockTimer:null,signalListenerBasicConfigEdit:null,signalListenerBasicConfigLockTimer:null,signalListenerActionsEdit:null,signalListenerActionsLockTimer:null,signalJoinEdit:null,signalJoinLockTimer:null,regionControllerEdit:null,regionControllerLockTimer:null,logicChainMetadataEdit:null,logicChainMetadataLockTimer:null,logicChainEditor:null,logicChainEditorLockTimer:null,logicChainActionAppendLockTimer:null,logicChainExistingEditLockTimer:null,conditionGroupEdit:null,conditionGroupLockTimer:null,conditionNodeEditor:null,conditionNodeEditorRerenderTimer:null,conditionCatalog:null,currentConditionGroup:null,conditionPreviewForms:{},conditionPreviewResult:null,conditionGroupLockNotice:null,currentLogicChainGraph:null,conditionDebuggerData:null,conditionDebuggerDetail:null,conditionDebuggerReplay:null,currentStateVariableDetail:null,stateVariableEdit:null,stateVariableLockTimer:null,selectionCreateVirtualBlock:null,virtualBlockDelete:null,signalListenerCreate:null,signalListenerDelete:null,selectionTerminalById:{},deviceEditLocks:{},openDeviceMoreMenuId:'',deviceMorePopover:null,deviceFilters:{search:'',type:'ALL',enabled:'ALL',doctor:'ALL',world:'ALL'},signalFilters:{search:'',consumer:'ALL',status:'ALL',sort:'RECENT'},signalJoinFilters:{search:'',enabled:'ALL',mode:'ALL',scope:'ALL'},logicChainFilters:{search:'',status:'ALL',saved:'ALL',expanded:{}},conditionFilters:{search:'',enabled:'ALL',suite:'ALL'},conditionDebuggerFilters:{search:'',targetType:'ALL',result:'ALL',conditionGroupId:'',channel:''},stateVariableFilters:{search:'',scope:'ALL',type:'ALL',target:''},logicChainCanvas:{zoom:1,panX:0,panY:0,selectedNodeId:'',focusNodeId:'',hoverNodeId:'',detailOpen:true,graphKey:'',collapsedChannels:{},viewMode:'BOTH',nodeTypeFilter:'ALL'},doctorFilters:{search:'',severity:'ALL',objectType:'ALL',jump:'ALL'},historyFilters:{search:'',channel:'ALL',sourceType:'ALL',result:'ALL',range:'ALL',sort:'NEWEST'},userFilters:{search:'',role:'ALL',enabled:'ALL',online:'ALL'},regionFilters:{search:'',world:'ALL',enabled:'ALL',doctor:'ALL',players:'ALL'},regionControllerFilters:{search:'',enabled:'ALL',target:'ALL',event:'ALL'},actionFilters:{search:'',type:'ALL',owner:'ALL',result:'ALL',doctor:'ALL',sort:'NAME'},templateFilters:{search:'',type:'ALL',status:'ALL',favorite:'ALL',sort:'NAME'},advancedDetailOpen:{}};
                appState.timerEdit=null;appState.timerLockTimer=null;appState.timerFilters={search:'',enabled:'ALL',mode:'ALL',scope:'ALL'};appState.timers=[];
                appState.templates=[];appState.templateCenter=null;appState.templateImport=null;appState.templateApply=null;appState.templateApplyLockTimer=null;appState.templateImportLockTimer=null;appState.templateCenterFilters={search:'',source:'ALL',category:'ALL',placeholder:'ALL'};
                appState.snapshotTimeline=null;appState.snapshotDetail=null;appState.snapshotManualDraft=null;appState.snapshotRollback=null;appState.snapshotFilters={search:'',kind:'ALL',module:'ALL',resource:'ALL',user:'ALL',from:'',to:''};
                appState.helpCatalog=null;appState.helpCenterFilters={search:'',category:'ALL',kind:'ALL',mode:'basic',topicId:'',view:'docs',composing:false,topicListScrollTop:0,documentScrollTop:0,rightPanelScrollTop:0};
                appState.helpInlineTermPopover=null;appState.helpInlineTermCloseTimer=null;appState.pendingHelpReturnContext=null;
                appState.modalClosePromise=null;appState.modalDismissPromise=null;appState.modalCloseHandler=null;appState.modalDirtyChecker=null;appState.modalSyncBeforeClose=null;appState.modalDiscardConfirmOpen=false;
                appState.realtime={source:null,status:'DISCONNECTED',reconnectTimer:null,reconnectAttempt:0,lastEventAt:'',lastSeenSeq:0,lastEventId:'',wasDisconnected:false,missed:false,offline:typeof navigator!=='undefined'&&!navigator.onLine,refreshTimers:{},dirtyRoutes:{},pendingRefresh:{},refreshSeq:{},pollTimer:null,pollHash:null};
                function esc(value){return String(value ?? '').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
                function isBlank(value){return value===undefined||value===null||String(value).trim()==='';}
                function iconKey(name){
                  const raw=String(name||'').trim().toLowerCase().replace(/_/g,'-');
                  const compact=raw.replace(/[^a-z0-9]/g,'');
                  const aliases={
                    signal:'signalbridge-main',signalbridge:'signalbridge-main',channel:'active-channel',broadcast:'signal-overview',
                    receiver:'receiver-main',listener:'consumer-listener',relay:'action-relay',virtual:'virtual-block-device',auto:'device',
                    cube:'device-overview',deviceoverview:'device-overview',doctoroverview:'doctor-overview',signaloverview:'signal-overview',regionoverview:'region-overview',actionoverview:'action-overview',template:'template-package',templates:'template-package',templatepackage:'template-package',prefab:'template-package',actiontemplate:'action-template',useroverview:'user-overview',
                    signaldevice:'signal-device',signalemitter:'signal-device',signallistener:'consumer-listener',signalreceiver:'signal-receiver',virtualblockdevice:'virtual-block-device',actionrelay:'action-relay',regioncontroller:'region-controller',
                    critical:'critical-issue',danger:'critical-issue',error:'critical-issue',warning:'warning-issue',info:'info-issue',ok:'check-pass',pass:'check-pass',success:'check-pass',healthy:'doctor-ok',
                    active:'active-region',consumers:'listener-receiver',consumer:'listener-receiver',response:'response-time',avgresponse:'response-time',today:'today-trigger',lightning:'action-overview',pulse:'pulse-duration',
                    users:'user-total',role:'current-role',status:'doctor-ok',enabled:'enabled',disabled:'receiver-disabled',pause:'receiver-disabled',play:'enabled',
                    orphan:'channel-orphan',search:'doctor-overview',eye:'current-user',login:'logout',plus:'receiver-total',upload:'action-binding',download:'channel-total',archive:'history',copy:'session',edit:'settings',pencil:'settings',close:'channel-error',chevronleft:'chevron-left',chevronright:'chevron-right',chevrondown:'chevron-down','import':'action-binding','export':'channel-total',
                    key:'current-role',chest:'channel-total',door:'logout',custom1:'device-overview',snapshot:'snapshot',snapshots:'snapshot',rollback:'snapshot',timeline:'snapshot',help:'help-center',helpcenter:'help-center',example:'example-center',examplecenter:'example-center',glossary:'help-center',troubleshooting:'doctor-overview',
                    conditioneditor:'condition-group',conditiongroups:'condition-group',conditiongroup:'condition-group',conditiondebugger:'condition-debugger',conditiongate:'runtime-gate',gate:'runtime-gate',
                    signaljoin:'signal-join',barrier:'signal-barrier',aggregator:'signal-aggregator',joinstatus:'join-status',
                    timerstart:'timer-start',timercancel:'timer-cancel',scheduler:'scheduler',timer:'timer',delay:'delay',countdown:'countdown',repeat:'repeat',
                    stateglobal:'state-variable-global',stateplayer:'state-variable-player',stateaction:'state-action',logicchain:'logic-chain',logicnode:'logic-node'
                  };
                  return FLAT_ICON_ASSETS[raw]?raw:(aliases[raw]||aliases[compact]||raw||'doctor-ok');
                }
                function iconClassName(name){return iconKey(name).replace(/[^a-z0-9_-]/g,'-');}
                function iconAssetKey(name){const key=iconKey(name);return FLAT_ICON_ASSETS[key]?key:'doctor-ok';}
                function iconSvgBody(key){return ICON_GEOMETRY[key]||ICON_GEOMETRY['doctor-ok'];}
                function icon(name){
                  const key=iconAssetKey(name);
                  const classes=`icon-img icon-geo icon-${iconClassName(name)} icon-asset-${key}`;
                  return `<svg class="${classes}" viewBox="0 0 24 24" aria-hidden="true" focusable="false">${iconSvgBody(key)}</svg>`;
                }
                function hydrateIcons(root=document){
                  if(Object.keys(ICON_GEOMETRY).length===0)document.documentElement.classList.add('flat-icons-missing');
                  const scope=root&&typeof root.querySelectorAll==='function'?root:document;
                  scope.querySelectorAll('[data-icon]').forEach(el=>{el.innerHTML=icon(el.dataset.icon);});
                }
                function renderIcons(root=document){hydrateIcons(root);}
                function statusClass(value){const v=String(value||'').toUpperCase();if(v==='ERROR'||v==='FAILED')return'error';if(v==='WARNING')return'warning';if(v==='INFO'||v==='UNKNOWN')return'info';return'ok';}
                function pill(value){return `<span class="pill ${statusClass(value)}">${esc(labelStatus(value))}</span>`}
                function textPill(text, kind='info'){return `<span class="pill ${esc(kind)}">${esc(text)}</span>`}
                function labelStatus(value){const v=String(value||'UNKNOWN').toUpperCase();return {OK:'正常',INFO:'信息',WARNING:'警告',ERROR:'错误',UNKNOWN:'未知',SUCCESS:'成功',FAILED:'失败',SKIPPED:'跳过'}[v]||value;}
                function labelBool(value){return value?'已启用':'已禁用';}
                function labelRuntimeBool(value){if(value===true)return '是';if(value===false)return '否';return '未知';}
                function labelType(value){const v=String(value||'UNKNOWN').toUpperCase();return {SIGNAL_EMITTER:'信号发射器',SIGNAL_RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',VIRTUAL_BLOCK_DEVICE:'虚拟方块设备',REGION_CONTROLLER:'区域控制器',UNKNOWN:'未知设备'}[v]||value||'未知设备';}
                function labelSourceType(value){return {DEVICE:'设备',LISTENER:'监听器',RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',REGION:'区域',SIGNAL_JOIN:'信号汇合',TIMER:'计时器',SCHEDULER_TIMER:'计时器',COMMAND:'命令',MANUAL:'手动',SYSTEM:'系统',UNKNOWN:'未知来源'}[String(value||'UNKNOWN').toUpperCase()]||value||'-';}
                function labelEndpointType(value){return {DEVICE:'触发设备',LISTENER:'监听器',RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',REGION:'区域',SIGNAL_JOIN:'信号汇合',TIMER:'计时器',SCHEDULER_TIMER:'计时器',COMMAND:'命令',SYSTEM:'系统',UNKNOWN:'未知节点'}[String(value||'UNKNOWN').toUpperCase()]||value||'未知节点';}
                function labelActionType(value){return {COMMAND:'命令动作',MESSAGE:'消息动作',SOUND:'音效动作',SIGNAL:'信号动作',UNKNOWN:'未知动作'}[String(value||'UNKNOWN').toUpperCase()]||value||'未知动作';}
                function labelOwnerType(value){return {LISTENER:'监听器',ACTION_RELAY:'动作继电器',REGION_ENTER:'区域进入动作',REGION_EXIT:'区域离开动作',REGION_STAY:'区域停留动作',REGION:'区域',DEVICE:'设备',SYSTEM:'系统',UNKNOWN:'未知归属'}[String(value||'UNKNOWN').toUpperCase()]||value||'未知归属';}
                function labelTargetFilter(value){return {ALL:'全部玩家',OP:'管理员',TAG:'标签过滤',TEAM:'队伍过滤',UNKNOWN:'未知'}[String(value||'UNKNOWN').toUpperCase()]||value||'未设置';}
                function labelSubType(value){const v=String(value||'').toLowerCase();return {signal_listener:'监听器',signal_emitter:'信号发射器',signal_receiver:'信号接收器',action_relay:'动作继电器',virtual_block_device:'虚拟方块设备',signal_join:'信号汇合',scheduler_timer:'计时器',timer:'计时器',all:'所有输入',any_n:'任意 N 个输入',count:'累计次数'}[v]||labelType(value);}
                function labelServerStatus(value){return {RUNNING:'运行中',STOPPED:'已停止',STARTING:'启动中',UNKNOWN:'未知'}[String(value||'').toUpperCase()]||value||'-';}
                function labelAccessMode(value){return {LOCAL_ONLY:'本机模式',LAN_DEV:'局域网开发模式',MULTIPLAYER_DEV:'多人开发模式'}[String(value||'').toUpperCase()]||value||'-';}
                function labelRole(value){return {OWNER:'所有者',EDITOR:'编辑者',TESTER:'测试者',VIEWER:'查看者'}[String(value||'').toUpperCase()]||value||'-';}
                function labelRoleFull(value){const id=String(value||'').toUpperCase();return `${labelRole(id)}${id&&id!=='-'?`（${id}）`:''}`;}
                function labelEnabledState(value){return value?'启用':'禁用';}
                function labelOnline(value){return value?'在线':'离线';}
                function labelChannel(value){return isBlank(value)?'未设置':value;}
                function labelChannelType(value){return {DEVICE:'设备频道',REGION:'区域频道',SYSTEM:'系统频道',GAME:'游戏流程频道'}[String(value||'').toUpperCase()]||'频道';}
                function labelConsumerFilter(value){return {ALL:'全部',HAS_CONSUMER:'有消费者',NO_CONSUMER:'无消费者',HAS_LISTENER:'有监听器',HAS_RECEIVER:'有接收器',HAS_RELAY:'有动作继电器'}[value]||value;}
                function labelSignalStatusFilter(value){return {ALL:'全部',RECENT:'最近有事件',NO_RECENT:'暂无事件',WARNING:'有警告'}[value]||value;}
                function labelSignalSort(value){return {RECENT:'最近触发时间',CHANNEL:'频道名',CONSUMERS:'消费者数量'}[value]||value;}
                function labelObjectType(value){return {DEVICE:'设备',CHANNEL:'频道',LISTENER:'监听器',RECEIVER:'接收器',ACTION_RELAY:'动作继电器',ACTION:'动作',REGION:'区域',TIMER:'计时器',SIGNAL_JOIN:'信号汇合',CONDITION_GROUP:'条件组',STATE_VARIABLE:'状态变量',SIGNAL_LISTENER_ACTION:'监听器单条 Action',ACTION_RELAY_ACTION:'继电器单条 Action',REGION_CONTROLLER_ACTION:'区域单条 Action',TIMER_ACTION:'Timer 单条 Action',SNAPSHOT:'配置时间轴',TEMPLATE:'模板',SYSTEM:'系统',UNKNOWN:'未知'}[String(value||'UNKNOWN').toUpperCase()]||value||'未知';}
                function labelHistoryRange(value){return {ALL:'全部',M10:'最近 10 分钟',H1:'最近 1 小时',H24:'最近 24 小时'}[value]||value;}
                function labelHistorySort(value){return {NEWEST:'最新优先',OLDEST:'最旧优先'}[value]||value;}
                function consumerCount(c){return Number(c?.listenerCount||0)+Number(c?.receiverCount||0)+Number(c?.actionRelayCount||0)+Number(c?.signalJoinCount||0);}
                async function loadSignalChannelOptions(force=false){
                  if(!force&&!appState.channelOptionsDirty&&Array.isArray(appState.channelOptions))return appState.channelOptions;
                  try{return storeSignalChannelOptions(await api('/api/signals/channels'));}
                  catch(err){appState.channelOptions=Array.isArray(appState.channelOptions)?appState.channelOptions:[];appState.channelOptionsError=err.message||'频道候选加载失败';}
                  return appState.channelOptions||[];
                }
                function storeSignalChannelOptions(channels){
                  appState.channelOptions=Array.isArray(channels)?channels:[];
                  appState.channelOptionsError=null;
                  appState.channelOptionsDirty=false;
                  return appState.channelOptions;
                }
                function markChannelOptionsDirty(event){
                  const type=String(event?.type||''), target=String(event?.payload?.targetType||'').toLowerCase(), source=String(event?.sourceType||event?.payload?.deviceType||'').toLowerCase();
                  const types=['signal_channel_changed','channel_metadata_changed','signal_listener_changed','signal_listener_config_changed','signal_join_changed','device_registered','device_removed','device_changed','device_config_changed','virtual_block_device_changed','selection_completed','selection_cancelled','selection_failed','action_changed','signal_listener_action_changed','config_changed','signal_history_appended','sync_required'];
                  const targets=['device_basic_config','device_extended_config','interaction_item_matcher','virtual_block_device_triggers','action_relay_actions','channel_metadata','signal_listener_basic_config','signal_join_config','virtual_block_device'];
                  const sources=['signal_receiver','action_relay','signal_emitter','signal_join','virtual_block_device'];
                  if(types.includes(type)||target.includes('channel')||targets.includes(target)||sources.includes(source))appState.channelOptionsDirty=true;
                }
                function normalizeChannelName(value){return String(value||'').trim();}
                function findChannelOption(channel,options){const name=normalizeChannelName(channel).toLowerCase();if(!name)return null;return (options||[]).find(c=>String(c.channel||'').trim().toLowerCase()===name)||null;}
                function channelOptionLabel(c){const parts=[`消费者：${consumerCount(c)}`];if(!isBlank(c?.lastTriggeredAt))parts.push(`最近触发：${formatDateTime(c.lastTriggeredAt)}`);if(!isBlank(c?.doctorStatus))parts.push(`诊断：${labelStatus(c.doctorStatus)}`);return parts.join(' · ');}
                function filteredChannelOptions(options,channel){const query=normalizeChannelName(channel).toLowerCase();const list=(options||[]).filter(c=>{if(!query)return true;const haystack=[c.channel,c.displayName,c.effectiveDisplayName,c.note].map(v=>String(v||'').toLowerCase()).join(' ');return haystack.includes(query);});return list.slice(0,50);}
                function channelComboQuery(draft,key=''){
                  if(!draft)return '';
                  if(key!==''&&key!==null&&key!==undefined){
                    const active=(draft.channelComboSearchActive||{})[key], query=(draft.channelComboQuery||{})[key];
                    return active?query||'':'';
                  }
                  return draft.channelComboSearchActive?draft.channelComboQuery||'':'';
                }
                function setChannelComboQuery(draft,query='',key=''){
                  if(!draft)return;
                  if(key!==''&&key!==null&&key!==undefined){
                    draft.channelComboSearchActive=draft.channelComboSearchActive||{};
                    draft.channelComboQuery=draft.channelComboQuery||{};
                    draft.channelComboSearchActive[key]=!isBlank(query);
                    draft.channelComboQuery[key]=query||'';
                    return;
                  }
                  draft.channelComboSearchActive=!isBlank(query);
                  draft.channelComboQuery=query||'';
                }
                function resetChannelComboQuery(draft,key=''){setChannelComboQuery(draft,'',key);}
                async function loadOnlinePlayerOptions(force=false){
                  if(!force&&Array.isArray(appState.onlinePlayerOptions))return appState.onlinePlayerOptions;
                  try{appState.onlinePlayerOptions=await api('/api/webadmin/online-players');appState.onlinePlayerOptionsError=null;}
                  catch(err){appState.onlinePlayerOptions=Array.isArray(appState.onlinePlayerOptions)?appState.onlinePlayerOptions:[];appState.onlinePlayerOptionsError=err.message||'在线玩家候选加载失败';}
                  return appState.onlinePlayerOptions||[];
                }
                function filteredOnlinePlayerOptions(options,playerName){const query=String(playerName||'').trim().toLowerCase();const list=(options||[]).filter(p=>!query||String(p.name||'').toLowerCase().includes(query)||String(p.uuid||'').toLowerCase().includes(query));return list.slice(0,50);}
                function channelComboOptionsHtml(deviceId,draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft)), current=normalizeChannelName(draft.channel).toLowerCase(), active=Math.max(0,Number(draft.channelComboIndex||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接保存为新频道</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" onclick='selectDeviceBasicConfigChannel(${jsString(deviceId)},${jsString(c.channel||'')})'><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function renderDeviceBasicConfigChannelCombo(deviceId,draft){
                  const open=draft.channelComboOpen?' open':'';
                  return `<div id="basic-channel-combo" class="channel-combo${open}"><div class="channel-combo-control"><input id="basic-channel" class="input" maxlength="128" value="${esc(draft.channel||'')}" placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${draft.channelComboOpen?'true':'false'}" aria-controls="basic-channel-menu" onfocus='openDeviceBasicConfigChannelMenu(${jsString(deviceId)})' oninput='updateDeviceBasicConfigDraftFromForm(${jsString(deviceId)},true)' onkeydown='handleDeviceBasicConfigChannelKey(event,${jsString(deviceId)})'><button class="channel-combo-toggle" type="button" onclick='toggleDeviceBasicConfigChannelMenu(${jsString(deviceId)})' aria-label="显示已有频道">${icon('chevron-down')}</button></div><div id="basic-channel-menu" class="channel-combo-menu" role="listbox">${channelComboOptionsHtml(deviceId,draft)}</div></div>`;
                }
                function channelHintHtml(channel,options,loadError){
                  if(loadError)return `<span class="muted">频道候选加载失败，仍可手动输入新的频道名。</span>`;
                  const name=normalizeChannelName(channel);
                  if(!name)return '<span class="muted">可选择已有频道，也可以输入新的频道名。新频道不会自动创建消费者。</span>';
                  const found=findChannelOption(name,options);
                  if(!found)return '<span class="muted">该频道当前未在系统中发现。保存后设备会使用此频道，但不会自动创建监听器、接收器或动作继电器。</span>';
                  const count=consumerCount(found), bits=[`已选择已有频道：${found.channel||name}`, `消费者：${count}`];
                  if(!isBlank(found.lastTriggeredAt))bits.push(`最近触发：${formatDateTime(found.lastTriggeredAt)}`);
                  if(['WARNING','ERROR'].includes(String(found.doctorStatus||'').toUpperCase()))bits.push(`诊断：${labelStatus(found.doctorStatus)}`);
                  if(count===0)bits.push('该频道当前暂无消费者。');
                  return bits.map(esc).join('<br>');
                }
                function updateDeviceBasicConfigDraftFromForm(deviceId,openMenu=false){
                  const draft=appState.deviceBasicConfigEdit;
                  if(!draft||draft.deviceId!==deviceId)return;
                  draft.enabled=(document.getElementById('basic-enabled')?.value||'false')==='true';
                  draft.channel=document.getElementById('basic-channel')?.value||'';
                  if(openMenu){draft.channelComboOpen=true;draft.channelComboIndex=0;setChannelComboQuery(draft,draft.channel);}
                  const hint=document.getElementById('basic-channel-hint');
                  if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);
                  syncDeviceBasicConfigChannelCombo(deviceId);
                }
                function syncDeviceBasicConfigChannelCombo(deviceId){
                  const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  const combo=document.getElementById('basic-channel-combo'), menu=document.getElementById('basic-channel-menu'), input=document.getElementById('basic-channel');
                  if(combo)combo.classList.toggle('open',!!draft.channelComboOpen);
                  if(input)input.setAttribute('aria-expanded',draft.channelComboOpen?'true':'false');
                  if(menu)menu.innerHTML=channelComboOptionsHtml(deviceId,draft);
                }
                function openDeviceBasicConfigChannelMenu(deviceId){const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;updateDeviceBasicConfigDraftFromForm(deviceId,false);closeAllCustomComboboxes();draft.channelComboOpen=true;resetChannelComboQuery(draft);syncDeviceBasicConfigChannelCombo(deviceId);}
                function closeDeviceBasicConfigChannelMenu(deviceId){const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;draft.channelComboOpen=false;syncDeviceBasicConfigChannelCombo(deviceId);}
                function toggleDeviceBasicConfigChannelMenu(deviceId){const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;updateDeviceBasicConfigDraftFromForm(deviceId,false);const wasOpen=!!draft.channelComboOpen;if(wasOpen){draft.channelComboOpen=false;syncDeviceBasicConfigChannelCombo(deviceId);return;}closeAllCustomComboboxes();draft.channelComboOpen=true;resetChannelComboQuery(draft);syncDeviceBasicConfigChannelCombo(deviceId);document.getElementById('basic-channel')?.focus();}
                function selectDeviceBasicConfigChannel(deviceId,channel){const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;draft.channel=channel||'';draft.channelComboOpen=false;draft.channelComboIndex=0;resetChannelComboQuery(draft);const input=document.getElementById('basic-channel');if(input)input.value=draft.channel;const hint=document.getElementById('basic-channel-hint');if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncDeviceBasicConfigChannelCombo(deviceId);}
                function handleDeviceBasicConfigChannelKey(event,deviceId){
                  const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft));
                  if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.channelComboOpen=false;syncDeviceBasicConfigChannelCombo(deviceId);return;}
                  if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex||0)+1:Number(draft.channelComboIndex||0)-1;draft.channelComboIndex=Math.min(max,Math.max(0,next));syncDeviceBasicConfigChannelCombo(deviceId);return;}
                  if(event.key==='Enter'&&draft.channelComboOpen&&options.length>0){event.preventDefault();selectDeviceBasicConfigChannel(deviceId,options[Math.min(options.length-1,Number(draft.channelComboIndex||0))].channel);return;}
                }
                function extendedFieldId(field){return String(field||'').replace(/[^a-zA-Z0-9_-]/g,'_');}
                function isExtendedChannelField(field){return ['interactChannel','successChannel','failChannel'].includes(String(field||''));}
                function isExtendedTickField(field){return ['interactionCooldownTicks','pulseTicks','cooldownTicks'].includes(String(field||''));}
                function extendedEditableFields(draft){return draft&&Array.isArray(draft.editableFields)?draft.editableFields:(draft?.supportedFields||[]);}
                function extendedFieldEditable(draft,field){return !!(draft&&draft.lockId&&extendedEditableFields(draft).includes(field));}
                function deviceExtendedRuntimeNote(cfg){
                  if(!cfg)return '';
                  const state=String(cfg.runtimeState||''), reason=cfg.unsupportedReason||'', block=(cfg.blockId&&!String(reason).includes('当前方块'))?`当前方块：${cfg.blockId}`:'', be=cfg.blockEntityType?`方块实体：${cfg.blockEntityType}`:'';
                  if(!state||state==='ready'||state==='unsupported')return reason?`<div class="readonly-note">${esc(reason)}</div>`:'';
                  return `<div class="readonly-note danger" data-device-runtime-state="${esc(state)}">${esc([reason,block,be].filter(Boolean).join(' · '))}</div>`;
                }
                function makeDeviceExtendedConfigDraft(deviceId,cfg,lock,channelOptions){
                  const draft={deviceId,values:{...(cfg.values||{})},originalValues:{...(cfg.values||{})},supportedFields:[...(cfg.supportedFields||[])],editableFields:[...(cfg.editableFields||cfg.supportedFields||[])],fieldLabels:{...(cfg.fieldLabels||{})},clearableFields:{...(cfg.clearableFields||{})},fieldDisabledReasons:{...(cfg.fieldDisabledReasons||{})},runtimeState:cfg.runtimeState||'',worldAvailable:cfg.worldAvailable,chunkLoaded:cfg.chunkLoaded,blockEntityLoaded:cfg.blockEntityLoaded,blockEntityType:cfg.blockEntityType||'',blockId:cfg.blockId||'',unsupportedReason:cfg.unsupportedReason||'',clear:{},channelOptions,channelOptionsError:appState.channelOptionsError,channelComboOpen:{},channelComboIndex:{},channelComboQuery:{},channelComboSearchActive:{},expectedFingerprint:cfg.expectedFingerprint||'',lockId:lock?.lockId||'',lock:lock||null,errors:[],saving:false,conflict:null};
                  markModalInitialSnapshot('device_extended_config',draft);
                  return draft;
                }
                function extendedChannelComboOptionsHtml(deviceId,field,draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const value=(draft.values||{})[field]||'', options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft,field)), current=normalizeChannelName(value).toLowerCase(), indexes=draft.channelComboIndex||{}, active=Math.max(0,Number(indexes[field]||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接保存为新频道</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" onclick='selectDeviceExtendedConfigChannel(${jsString(deviceId)},${jsString(field)},${jsString(c.channel||'')})'><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function renderDeviceExtendedConfigChannelCombo(deviceId,field,draft){
                  const id=extendedFieldId(field), open=(draft.channelComboOpen||{})[field]?' open':'', value=(draft.values||{})[field]||'';
                  const disabled=extendedFieldEditable(draft,field)?'':'disabled';
                """)
.append("""
                  return `<div id="extended-channel-combo-${id}" class="channel-combo extended-channel-combo${open}"><div class="channel-combo-control"><input id="extended-${id}" class="input" maxlength="128" value="${esc(value)}" ${disabled} placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${(draft.channelComboOpen||{})[field]?'true':'false'}" aria-controls="extended-${id}-menu" onfocus='openDeviceExtendedConfigChannelMenu(${jsString(deviceId)},${jsString(field)})' oninput='updateDeviceExtendedConfigDraftFromForm(${jsString(deviceId)},${jsString(field)})' onkeydown='handleDeviceExtendedConfigChannelKey(event,${jsString(deviceId)},${jsString(field)})'><button class="channel-combo-toggle" type="button" ${disabled} onclick='toggleDeviceExtendedConfigChannelMenu(${jsString(deviceId)},${jsString(field)})' aria-label="显示已有频道">${icon('chevron-down')}</button></div><div id="extended-${id}-menu" class="channel-combo-menu" role="listbox">${extendedChannelComboOptionsHtml(deviceId,field,draft)}</div></div>`;
                }
                function updateDeviceExtendedConfigDraftFromForm(deviceId,openField=''){
                  const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  draft.values=draft.values||{};draft.clear=draft.clear||{};draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};
                  (draft.supportedFields||[]).forEach(field=>{
                    const id=extendedFieldId(field);
                    if(isExtendedChannelField(field)){
                      draft.values[field]=document.getElementById(`extended-${id}`)?.value||'';
                      draft.clear[field]=!!document.getElementById(`extended-clear-${id}`)?.checked;
                    }else if(isExtendedTickField(field)){
                      draft.values[field]=document.getElementById(`extended-${id}`)?.value||'';
                    }
                  });
                  if(openField){draft.channelComboOpen[openField]=true;draft.channelComboIndex[openField]=0;setChannelComboQuery(draft,(draft.values||{})[openField]||'',openField);}
                  (draft.supportedFields||[]).filter(isExtendedChannelField).forEach(field=>{
                    const id=extendedFieldId(field), hint=document.getElementById(`extended-${id}-hint`);
                    if(hint)hint.innerHTML=channelHintHtml((draft.values||{})[field],draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);
                    syncDeviceExtendedConfigChannelCombo(deviceId,field);
                  });
                }
                function syncDeviceExtendedConfigChannelCombo(deviceId,field){
                  const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  const id=extendedFieldId(field), combo=document.getElementById(`extended-channel-combo-${id}`), menu=document.getElementById(`extended-${id}-menu`), input=document.getElementById(`extended-${id}`);
                  if(combo)combo.classList.toggle('open',!!(draft.channelComboOpen||{})[field]);
                  if(input)input.setAttribute('aria-expanded',(draft.channelComboOpen||{})[field]?'true':'false');
                  if(menu)menu.innerHTML=extendedChannelComboOptionsHtml(deviceId,field,draft);
                }
                function openDeviceExtendedConfigChannelMenu(deviceId,field){const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;updateDeviceExtendedConfigDraftFromForm(deviceId,'');closeAllCustomComboboxes();draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboOpen[field]=true;resetChannelComboQuery(draft,field);syncDeviceExtendedConfigChannelCombo(deviceId,field);}
                function closeDeviceExtendedConfigChannelMenu(deviceId,field){const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboOpen[field]=false;syncDeviceExtendedConfigChannelCombo(deviceId,field);}
                function toggleDeviceExtendedConfigChannelMenu(deviceId,field){const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;updateDeviceExtendedConfigDraftFromForm(deviceId,'');draft.channelComboOpen=draft.channelComboOpen||{};const wasOpen=!!draft.channelComboOpen[field];if(wasOpen){draft.channelComboOpen[field]=false;syncDeviceExtendedConfigChannelCombo(deviceId,field);return;}closeAllCustomComboboxes();draft.channelComboOpen[field]=true;resetChannelComboQuery(draft,field);syncDeviceExtendedConfigChannelCombo(deviceId,field);document.getElementById(`extended-${extendedFieldId(field)}`)?.focus();}
                function selectDeviceExtendedConfigChannel(deviceId,field,channel){const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;draft.values=draft.values||{};draft.clear=draft.clear||{};draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.values[field]=channel||'';draft.clear[field]=false;draft.channelComboOpen[field]=false;draft.channelComboIndex[field]=0;resetChannelComboQuery(draft,field);const id=extendedFieldId(field), input=document.getElementById(`extended-${id}`), clear=document.getElementById(`extended-clear-${id}`);if(input)input.value=draft.values[field];if(clear)clear.checked=false;const hint=document.getElementById(`extended-${id}-hint`);if(hint)hint.innerHTML=channelHintHtml(draft.values[field],draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncDeviceExtendedConfigChannelCombo(deviceId,field);}
                function handleDeviceExtendedConfigChannelKey(event,deviceId,field){
                  const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft,field));
                  draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};
                  if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.channelComboOpen[field]=false;syncDeviceExtendedConfigChannelCombo(deviceId,field);return;}
                  if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen[field]=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex[field]||0)+1:Number(draft.channelComboIndex[field]||0)-1;draft.channelComboIndex[field]=Math.min(max,Math.max(0,next));syncDeviceExtendedConfigChannelCombo(deviceId,field);return;}
                  if(event.key==='Enter'&&draft.channelComboOpen[field]&&options.length>0){event.preventDefault();selectDeviceExtendedConfigChannel(deviceId,field,options[Math.min(options.length-1,Number(draft.channelComboIndex[field]||0))].channel);return;}
                }
                function maybeReleaseDeviceExtendedConfigEditForRoute(hash){const draft=appState.deviceExtendedConfigEdit;if(!draft)return;if(String(hash||'').startsWith('#/devices/')){const info=detailRoute(String(hash).substring('#/devices/'.length),'#/devices');if(sameDeviceRef(info.id,draft.deviceId))return;}releaseDeviceExtendedConfigLock(draft,true);appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();}
                async function startDeviceExtendedConfigEdit(deviceId){if(!canEditDeviceExtendedConfig())return;try{const cfg=await api(`/api/webadmin/device-extended-config/${encodeURIComponent(deviceId)}`), fields=cfg.supportedFields||[], editable=cfg.editableFields||fields;if(cfg.supported===false||!fields.length){toast(cfg.unsupportedReason||'该设备类型暂无可编辑扩展配置。');return;}let lock={};if(editable.length){const result=await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_extended_config',targetId:deviceId})});if(!result.success){toast(result.message||'无法获取编辑锁');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}lock=result.data?.lock||{};}else{toast(cfg.unsupportedReason||'当前运行状态下只能查看类型专属配置快照。');}const channelOptions=await loadSignalChannelOptions();appState.deviceExtendedConfigEdit=makeDeviceExtendedConfigDraft(deviceId,cfg,lock,channelOptions);if(appState.deviceExtendedConfigEdit.lockId)scheduleDeviceExtendedConfigLockHeartbeat();await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});showDeviceExtendedConfigEditModal(deviceId);}catch(err){toast(err.message||'无法获取编辑锁');}}
                async function cancelDeviceExtendedConfigEdit(deviceId){const draft=appState.deviceExtendedConfigEdit;if(draft&&draft.deviceId===deviceId){await releaseDeviceExtendedConfigLock(draft,false);appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();}await dismissWebAdminModal();await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}
                async function reloadDeviceExtendedConfigAfterConflict(deviceId){const draft=appState.deviceExtendedConfigEdit;if(draft&&draft.deviceId===deviceId)await releaseDeviceExtendedConfigLock(draft,true);appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}
                function scheduleDeviceExtendedConfigLockHeartbeat(){stopDeviceExtendedConfigLockHeartbeat();appState.deviceExtendedConfigLockTimer=setTimeout(async()=>{await heartbeatDeviceExtendedConfigLock();if(appState.deviceExtendedConfigEdit)scheduleDeviceExtendedConfigLockHeartbeat();},30000);}
                function stopDeviceExtendedConfigLockHeartbeat(){if(appState.deviceExtendedConfigLockTimer){clearTimeout(appState.deviceExtendedConfigLockTimer);appState.deviceExtendedConfigLockTimer=null;}}
                async function heartbeatDeviceExtendedConfigLock(){const draft=appState.deviceExtendedConfigEdit;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_extended_config',targetId:draft.deviceId,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;appState.deviceExtendedConfigEdit=draft;return;}draft.errors=[{message:result.message||'编辑锁续期失败'}];appState.deviceExtendedConfigEdit=draft;stopDeviceExtendedConfigLockHeartbeat();await renderDeviceDetail(currentDeviceRouteArg(draft.deviceId),{silent:true});}catch(err){draft.errors=[{message:err.message||'编辑锁续期失败'}];appState.deviceExtendedConfigEdit=draft;stopDeviceExtendedConfigLockHeartbeat();}}
                async function releaseDeviceExtendedConfigLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_extended_config',targetId:draft.deviceId,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'编辑锁释放失败，将等待自动过期。');}}
                function deviceExtendedConfigPatchBody(draft){const body={expectedFingerprint:draft.expectedFingerprint||'',lockId:draft.lockId||''};const original=draft.originalValues||{};const addChannel=(field,value,clearName,valueName)=>{if((draft.clear||{})[field]){body[clearName]=true;return;}const text=String(value??'').trim(), before=String(original[field]??'').trim();if(text||before)body[valueName]=value||'';};extendedEditableFields(draft).forEach(field=>{const value=(draft.values||{})[field];if(field==='interactChannel')addChannel(field,value,'clearInteractChannel','interactChannel');else if(field==='successChannel')addChannel(field,value,'clearSuccessChannel','successChannel');else if(field==='failChannel')addChannel(field,value,'clearFailChannel','failChannel');else if(field==='interactionCooldownTicks')body.interactionCooldownTicks=Number(value);else if(field==='pulseTicks')body.pulseTicks=Number(value);else if(field==='cooldownTicks')body.cooldownTicks=Number(value);});return body;}
                async function saveDeviceExtendedConfig(deviceId){const draft=appState.deviceExtendedConfigEdit||{deviceId};if(!draft.lockId){toast(draft.unsupportedReason||'当前运行状态下类型专属配置只读。');showDeviceExtendedConfigEditModal(deviceId);return;}updateDeviceExtendedConfigDraftFromForm(deviceId);draft.saving=true;draft.errors=[];draft.conflict=null;appState.deviceExtendedConfigEdit=draft;renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});try{const result=await api(`/api/webadmin/device-extended-config/${encodeURIComponent(deviceId)}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(deviceExtendedConfigPatchBody(draft))});if(result.success){markChannelOptionsDirty({type:'device_config_changed',payload:{targetType:'device_extended_config'}});appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();await dismissWebAdminModal();toast(result.changed?(result.message||'设备扩展配置已保存。'):'没有变更。');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.conflict||null;appState.deviceExtendedConfigEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopDeviceExtendedConfigLockHeartbeat();toast(result.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});showDeviceExtendedConfigEditModal(deviceId);}catch(err){draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.deviceExtendedConfigEdit=draft;toast(err.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});showDeviceExtendedConfigEditModal(deviceId);}}
                function signalHash(channel){return `#/signals/${encodeURIComponent(channel||'')}`;}
                function signalJoinHash(id){return `#/signal-joins/${encodeURIComponent(id||'')}`;}
                function timerHash(id){return `#/timers/${encodeURIComponent(id||'')}`;}
                function logicChainHash(chainId){return `#/logic-chains/${encodeURIComponent(chainId||'')}`;}
                function logicChainResolveHash(rootType,rootRef){return `#/logic-chains/resolve?rootType=${encodeURIComponent(rootType||'channel')}&rootRef=${encodeURIComponent(rootRef||'')}`;}
                function isLogicChainResolveRoute(hash){const h=String(hash||'');return h==='#/logic-chains/resolve'||h.startsWith('#/logic-chains/resolve?');}
                function listenerHash(id){return `#/listeners/${encodeURIComponent(id||'')}`;}
                function deviceRouteRef(id,type=''){
                  const clean=String(id||''), normalizedType=String(type||'').toLowerCase();
                  if(['signal_emitter','signal_receiver','action_relay'].includes(normalizedType)&&!clean.startsWith(`${normalizedType}:`))return `${normalizedType}:${clean}`;
                  return clean;
                }
                function deviceHash(idOrDevice,type=''){const ref=idOrDevice&&typeof idOrDevice==='object'?deviceRouteRef(idOrDevice.id,idOrDevice.type):deviceRouteRef(idOrDevice,type);return `#/devices/${encodeURIComponent(ref)}`;}
                function deviceTypeRefPrefix(id){const value=String(id||'');for(const type of ['signal_emitter','signal_receiver','action_relay','virtual_block_device']){if(value.startsWith(`${type}:`))return type;}return '';}
                function stripDeviceTypeRef(id){let value=String(id||'');['signal_emitter','signal_receiver','action_relay','virtual_block_device'].forEach(type=>{const prefix=`${type}:`;if(value.startsWith(prefix))value=value.substring(prefix.length);});return value;}
                function deviceApiRef(id){return stripDeviceTypeRef(id);}
                function sameDeviceRef(a,b){return stripDeviceTypeRef(a)===stripDeviceTypeRef(b);}
                function deviceDetailRouteKeys(id,type=''){const clean=String(id||''), keys=new Set();if(!clean)return keys;keys.add(`deviceDetail:${clean}`);const ref=deviceRouteRef(clean,type);keys.add(`deviceDetail:${ref}`);return keys;}
                function addDeviceDetailRouteKeys(add,id,type=''){deviceDetailRouteKeys(id,type).forEach(key=>add(key));}
                function eventAffectedChannels(event){const out=[];const raw=event?.payload?.affectedChannels;if(Array.isArray(raw))out.push(...raw);const inputs=event?.payload?.inputChannels;if(Array.isArray(inputs))out.push(...inputs);else if(!isBlank(inputs))out.push(...String(inputs).split(','));if(!isBlank(event?.payload?.outputChannel))out.push(event.payload.outputChannel);if(!isBlank(event?.channel))out.push(event.channel);return [...new Set(out.map(v=>String(v||'').trim()).filter(v=>!isBlank(v)))];}
                function stateActionRealtimeChanged(event){const payload=event?.payload||{}, actionType=String(payload.actionType||event?.actionType||'').toLowerCase(), changed=String(payload['stateAction.changed']??payload.changed??'').toLowerCase(), success=String(payload.success??event?.success??'true').toLowerCase();return actionType==='state_variable'&&success!=='false'&&(changed==='true'||changed==='1');}
                function lockHeldByOther(lock){return !!(lock&&lock.locked&&!lock.heldByCurrentUser);}
                function lockMessage(lock,label='配置'){return `${label}正在由 ${lock?.holderUsername||'其他用户'} 编辑，锁到期：${formatDateTime(lock?.expiresAt)}`;}
                const DEVICE_EDIT_LOCK_TYPES=['device_metadata','device_basic_config','device_extended_config','action_relay_actions','virtual_block_device_triggers','virtual_block_device_container_template','interaction_item_matcher'];
                function isDeviceConfigLockType(targetType){return DEVICE_EDIT_LOCK_TYPES.includes(String(targetType||''));}
                function deviceConfigLockLabel(targetType){const type=String(targetType||'');if(type==='device_metadata')return '显示信息';if(type==='device_basic_config')return '基础配置';if(type==='device_extended_config')return '类型专属配置';if(type==='action_relay_actions')return 'Action 列表';if(type==='virtual_block_device_triggers')return '原生触发配置';if(type==='virtual_block_device_container_template')return '容器变化模板';if(type==='interaction_item_matcher')return '交互物品匹配';return '配置';}
                function editLockCacheKey(targetType,targetId){return `${String(targetType||'')}\n${stripDeviceTypeRef(targetId)}`;}
                function rememberDeviceEditLockEvent(event){
                  if(String(event?.type||'')!=='edit_lock_changed')return;
                  const payload=event.payload||{}, targetType=String(payload.targetType||''), targetId=String(payload.targetId||event.deviceId||'');
                  if(!isDeviceConfigLockType(targetType)||isBlank(targetId))return;
                  const key=editLockCacheKey(targetType,targetId);
                  if(payload.locked===false){delete appState.deviceEditLocks[key];return;}
                  appState.deviceEditLocks[key]={locked:true,heldByCurrentUser:false,targetType,targetId:stripDeviceTypeRef(targetId),holderUsername:payload.holderUsername||payload.actor||'',holderRole:payload.holderRole||'',expiresAt:payload.expiresAt||''};
                }
                function cachedDeviceConfigLocks(deviceId){
                  const raw=stripDeviceTypeRef(deviceId);
                  if(isBlank(raw))return [];
                  return DEVICE_EDIT_LOCK_TYPES.map(targetType=>({label:deviceConfigLockLabel(targetType),lock:appState.deviceEditLocks[editLockCacheKey(targetType,raw)]})).filter(item=>item.lock);
                }
                function actionRelayLockForDevice(detail){return detail?.actionRelayActions?.lockStatus||appState.deviceEditLocks[editLockCacheKey('action_relay_actions',detail?.id||'')]||null;}
                function actionRelayLockHeldByOther(lock){return lockHeldByOther(lock);}
                function actionRelayLockMessage(lock){return lockMessage(lock,'Action 列表');}
                function deviceConfigLocks(detail){
                  const locks=[];
                  if(detail?.metadataLock)locks.push({label:'显示信息',lock:detail.metadataLock});
                  if(detail?.basicConfig?.lockStatus)locks.push({label:'基础配置',lock:detail.basicConfig.lockStatus});
                  if(!isVirtualBlockDevice(detail)&&detail?.extendedConfig?.lockStatus)locks.push({label:'类型专属配置',lock:detail.extendedConfig.lockStatus});
                  if(isActionRelay(detail)){const actionLock=actionRelayLockForDevice(detail);if(actionLock)locks.push({label:'Action 列表',lock:actionLock});}
                  if(isVirtualBlockDevice(detail)&&detail?.nativeTriggers?.lockStatus)locks.push({label:'原生触发配置',lock:detail.nativeTriggers.lockStatus});
                  if(isVirtualBlockDevice(detail)&&detail?.interactionItemMatcher?.lockStatus)locks.push({label:'交互物品匹配',lock:detail.interactionItemMatcher.lockStatus});
                  const seen=new Set(locks.map(item=>String(item.lock?.targetType||'')));
                  cachedDeviceConfigLocks(detail?.id||'').forEach(item=>{if(isVirtualBlockDevice(detail)&&String(item.lock?.targetType||'')==='device_extended_config')return;if(!seen.has(String(item.lock?.targetType||''))){locks.push(item);seen.add(String(item.lock?.targetType||''));}});
                  return locks;
                }
                function deviceConfigLockBlocker(detail){return deviceConfigLocks(detail).find(item=>lockHeldByOther(item.lock))||null;}
                function deviceConfigLockMessage(detail){const blocker=deviceConfigLockBlocker(detail);return blocker?lockMessage(blocker.lock,blocker.label):'';}
                function canEditDeviceConfig(detail){return canEditDeviceMetadata()||canEditDeviceBasicConfig()||canEditDeviceExtendedConfig()||(isActionRelay(detail)&&canEditActionRelayActions())||(isVirtualBlockDevice(detail)&&(canEditVbdNativeTriggers()||canEditInteractionItemMatcher()));}
                function deviceConfigEditButton(detail,label='编辑设备配置',kind='primary'){
                  const canEdit=canEditDeviceConfig(detail);
                  if(!canEdit)return waButton(label,'settings','disabled title="需要 EDITOR 或 OWNER 权限才能编辑设备配置。"','ghost');
                  const message=deviceConfigLockMessage(detail);
                  if(message)return `${waButton(label,'settings',`disabled title="${esc(message)}" data-device-config-lock-disabled="true"`,'ghost is-locked')}<span class="wa-lock-badge" data-device-config-lock-badge="true">${esc(message)}</span>`;
                  return waButton(label,'settings',htmlHandler(`startDeviceConfigEdit(${jsString(detail.id)})`),kind);
                }
                function historyHash(channel){return isBlank(channel)?'#/history':`#/history?channel=${encodeURIComponent(channel)}`;}
                function currentRouteHash(){return location.hash||'#/dashboard';}
                function routeBase(hash){const h=String(hash||'');const index=h.indexOf('?');return index>=0?h.substring(0,index):h;}
                function conditionDebuggerDetailIdFromHash(hash){const h=String(hash||'');if(h.startsWith('#/condition-debugger/'))return detailRoute(h.substring('#/condition-debugger/'.length),'#/condition-debugger').id;if(h.startsWith('#/condition-debugger?'))return parseHashParams(h).id||'';return '';}
                function isConditionDebuggerDetailHash(hash){return !isBlank(conditionDebuggerDetailIdFromHash(hash));}
                function isDetailHash(hash){const h=String(hash||'');return h.startsWith('#/devices/')||h.startsWith('#/signals/')||h.startsWith('#/listeners/')||h.startsWith('#/signal-listeners/')||h.startsWith('#/signal-joins/')||h.startsWith('#/timers/')||h.startsWith('#/logic-chains/')||h.startsWith('#/templates/')||h.startsWith('#/condition-groups/')||h.startsWith('#/conditions/')||h.startsWith('#/state-variables/')||isConditionDebuggerDetailHash(h)||h.startsWith('#/regions/')||h.startsWith('#/region-controllers/')||h.startsWith('#/actions/');}
                function isValidReturnHash(hash){const h=String(hash||'');if(!h.startsWith('#/'))return false;if(h.startsWith('#/login'))return false;if(h.includes('://'))return false;return ['#/dashboard','#/devices','#/virtual-block-devices','#/block-devices','#/receivers','#/listeners','#/signal-listeners','#/signals','#/signalbridge','#/signal-joins','#/timers','#/logic-chains','#/condition-groups','#/conditions','#/condition-debugger','#/state-variables','#/doctor','#/diagnostics','#/signal-doctor','#/history','#/events','#/users','#/permissions','#/users-permissions','#/settings','#/system-settings','#/config','#/config-management','#/settings/config','#/regions','#/region-list','#/region-controllers','#/regionctl','#/actions','#/action-templates','#/templates','#/help','#/examples'].some(prefix=>h===prefix||h.startsWith(prefix+'/')||h.startsWith(prefix+'?'));}
                function withReturnContext(targetHash){const target=String(targetHash||'#/dashboard');if(!isDetailHash(target))return target;let source=currentRouteHash();if(source.startsWith('#/condition-debugger')&&!isConditionDebuggerDetailHash(source))source=conditionDebuggerListHash();if(!isValidReturnHash(source))return target;return `${target}${target.includes('?')?'&':'?'}returnTo=${encodeURIComponent(source)}`;}
                function navigateTo(targetHash){captureConditionDebuggerListState(targetHash);location.hash=withReturnContext(targetHash);}
                function appendHashParams(hash,params){
                  const target=String(hash||'#/dashboard'), base=routeBase(target), query=target.includes('?')?target.substring(target.indexOf('?')+1):'', next=new URLSearchParams(query);
                  Object.entries(params||{}).forEach(([key,value])=>{if(!isBlank(value))next.set(key,String(value));});
                  const qs=next.toString();
                  return `${base}${qs?'?'+qs:''}`;
                }
                function safeDecodeRoutePart(value){try{return decodeURIComponent(String(value||''));}catch(_){return String(value||'');}}
                function detailRoute(raw,fallback){const text=String(raw||''), index=text.indexOf('?'), encodedId=index>=0?text.substring(0,index):text, query=index>=0?text.substring(index+1):'', id=safeDecodeRoutePart(encodedId), params=new URLSearchParams(query);const returnTo=params.get('returnTo')||'';return {id, fallback, returnTo:isValidReturnHash(returnTo)?returnTo:''};}
                function goBackOrFallback(returnTo,fallback){location.hash=isValidReturnHash(returnTo)?returnTo:fallback;}
                function jsString(value){return JSON.stringify(String(value||''));}
                function htmlEvent(name,script){return `${esc(name)}="${esc(script)}"`;}
                function htmlHandler(script){return htmlEvent('onclick',script);}
                function navigationAttr(target,stop=true){return htmlHandler(`${stop?'event.stopPropagation();':''}navigateTo(${jsString(target)})`);}
                function navDataAttr(target,label='打开详情'){if(isBlank(target))return 'aria-disabled="true"';return `data-nav-route="${esc(target)}" role="link" tabindex="0" aria-label="${esc(label)}"`;}
                function activateNavRoute(el){const target=el&&el.dataset?el.dataset.navRoute:'';if(!target)return;navigateTo(target);}
                function backButton(route,fallbackLabel){const label=route.returnTo?'返回上一页':fallbackLabel;return `<button class="secondary" ${htmlHandler(`goBackOrFallback(${jsString(route.returnTo)},${jsString(route.fallback)})`)}>${esc(label)}</button>`}
                function channelButton(channel){if(isBlank(channel))return '<span class="muted">未设置</span>';return `<button class="link-button" ${navigationAttr(signalHash(channel))}>${esc(channel)}</button>`}
                function navigationButton(target,label){if(isBlank(target))return esc(label||'-');if(String(target).startsWith('#/'))return `<button class="link-button" ${navigationAttr(target)}>${esc(label||target)}</button>`;if(String(target).startsWith('device:'))return `<button class="link-button" ${navigationAttr(deviceHash(String(target).substring(7)))}>${esc(label)}</button>`;if(String(target).startsWith('channel:'))return `<button class="link-button" ${navigationAttr(signalHash(String(target).substring(8)))}>${esc(label)}</button>`;if(String(target).startsWith('signal_join:'))return `<button class="link-button" ${navigationAttr(signalJoinHash(String(target).substring(12)))}>${esc(label)}</button>`;if(String(target).startsWith('timer:'))return `<button class="link-button" ${navigationAttr(timerHash(String(target).substring(6)))}>${esc(label)}</button>`;if(String(target).startsWith('region:'))return `<button class="link-button" ${navigationAttr('#/regions/'+encodeURIComponent(String(target).substring(7)))}>${esc(label)}</button>`;if(String(target).startsWith('action:'))return `<button class="link-button" ${navigationAttr('#/actions/'+encodeURIComponent(String(target).substring(7)))}>${esc(label)}</button>`;return esc(label||target);}
                function labelInteractionSource(value){return {main_hand:'主手',off_hand:'副手',inventory_contains:'背包/热键栏',armor_head:'头盔槽',armor_chest:'胸甲槽',armor_legs:'护腿槽',armor_feet:'靴子槽',armor_any:'任意盔甲槽'}[String(value||'').toLowerCase()]||value;}
                function labelConsumeSource(value){return {matched_source:'匹配来源',main_hand:'主手',off_hand:'副手',inventory:'背包/热键栏'}[String(value||'').toLowerCase()]||value;}
                function labelConsumeOrder(value){return {hotbar_first:'优先热键栏',main_inventory_first:'优先主背包'}[String(value||'').toLowerCase()]||value;}
                function labelVanillaPolicy(value){return {allow:'允许原版交互',require_item_match:'需要物品匹配才允许原版交互'}[String(value||'').toLowerCase()]||value;}
                function posText(pos){return pos?`${pos.x} ${pos.y} ${pos.z}`:'-';}
                function deviceIcon(type){const v=String(type||'UNKNOWN').toUpperCase();return icon({SIGNAL_EMITTER:'signal',SIGNAL_RECEIVER:'receiver',ACTION_RELAY:'relay',VIRTUAL_BLOCK_DEVICE:'virtual',REGION_CONTROLLER:'region',UNKNOWN:'device'}[v]||'device');}
                function deviceMetadataIcon(detail){const key=String(detail?.metadata?.effectiveIconKey||detail?.metadata?.iconKey||'auto').toLowerCase();if(key&&key!=='auto')return icon({signal_emitter:'signal',signal_receiver:'receiver',action_relay:'relay',virtual_block_device:'virtual',region:'region',action:'action',warning:'warning',key:'settings',chest:'device',door:'device',signal:'signal',custom_1:'device',condition_group:'condition-group',condition_debugger:'condition-debugger',runtime_gate:'runtime-gate',signal_join:'signal-join',signal_barrier:'signal-barrier',signal_aggregator:'signal-aggregator',timer:'timer',scheduler:'scheduler',state_variable:'state-variable',logic_chain:'logic-chain'}[key]||key);return deviceIcon(detail?.type);}
                function parseTime(value){if(isBlank(value))return null;const raw=typeof value==='number'?value:String(value).trim();const d=new Date(raw);return Number.isNaN(d.getTime())?null:d;}
                function pad2(value){return String(value).padStart(2,'0');}
                function formatDateTime(value){const d=parseTime(value);if(!d)return '暂无';return `${d.getFullYear()}/${pad2(d.getMonth()+1)}/${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;}
                """)
.append("""
                function formatRelativeTime(value){const d=parseTime(value);if(!d)return '暂无';const seconds=Math.max(0,Math.floor((Date.now()-d.getTime())/1000));if(seconds<60)return `${seconds} 秒前`;const minutes=Math.floor(seconds/60);if(minutes<60)return `${minutes} 分钟前`;const hours=Math.floor(minutes/60);if(hours<24)return `${hours} 小时前`;return `${Math.floor(hours/24)} 天前`;}
                function fmtTime(value){return esc(formatDateTime(value));}
                function appView(){return document.getElementById('app-view');}
                function captureViewState(){
                  const view=appView(), active=document.activeElement;
                  return {
                    scrollTop:view?view.scrollTop:0,
                    scrollLeft:view?view.scrollLeft:0,
                    windowScrollTop:window.scrollY||document.documentElement.scrollTop||0,
                    windowScrollLeft:window.scrollX||document.documentElement.scrollLeft||0,
                    activeId:active&&active.id?active.id:'',
                    selectionStart:active&&typeof active.selectionStart==='number'?active.selectionStart:null,
                    selectionEnd:active&&typeof active.selectionEnd==='number'?active.selectionEnd:null,
                    details:[...((view&&view.querySelectorAll)?view.querySelectorAll('details'):[])].map((d,i)=>({key:detailPersistKey(d,i),open:d.open})),
                    nestedScrolls:[...((view&&view.querySelectorAll)?view.querySelectorAll('[data-condition-type-catalog] .condition-type-list,[data-condition-node-compact-list],[data-condition-preview-panel],.condition-preview-result,.snapshot-timeline-graph,.snapshot-detail-rail,.snapshot-diff-list'):[])].map((el,i)=>({key:scrollPersistKey(el,i),top:el.scrollTop||0,left:el.scrollLeft||0}))
                  };
                }
                function detailPersistKey(detail,index){return detail.dataset.persistKey||detail.querySelector('summary')?.textContent?.trim()||`details-${index}`;}
                function restoreViewState(state){
                  if(!state)return;
                  requestAnimationFrame(()=>{
                    const view=appView();if(!view)return;
                    [...view.querySelectorAll('details')].forEach((d,i)=>{const key=detailPersistKey(d,i), saved=(state.details||[]).find(item=>item.key===key);if(saved)d.open=!!saved.open;});
                    view.scrollTop=state.scrollTop||0;
                    view.scrollLeft=state.scrollLeft||0;
                    if(typeof window.scrollTo==='function')window.scrollTo({top:state.windowScrollTop||0,left:state.windowScrollLeft||0,behavior:'auto'});
                    [...view.querySelectorAll('[data-condition-type-catalog] .condition-type-list,[data-condition-node-compact-list],[data-condition-preview-panel],.condition-preview-result,.snapshot-timeline-graph,.snapshot-detail-rail,.snapshot-diff-list')].forEach((el,i)=>{const key=scrollPersistKey(el,i), saved=(state.nestedScrolls||[]).find(item=>item.key===key);if(saved){el.scrollTop=saved.top||0;el.scrollLeft=saved.left||0;}});
                    if(state.activeId){const active=document.getElementById(state.activeId);if(active){active.focus({preventScroll:true});if(typeof active.setSelectionRange==='function'&&state.selectionStart!==null)active.setSelectionRange(state.selectionStart,state.selectionEnd ?? state.selectionStart);}}
                  });
                }
                function scrollPersistKey(el,index){return el.dataset.scrollKey||el.dataset.conditionScrollKey||el.getAttribute('data-condition-node-compact-list')||el.getAttribute('data-condition-preview-panel')||el.getAttribute('data-snapshot-timeline-graph')||el.getAttribute('data-snapshot-detail-rail')||el.getAttribute('data-snapshot-diff-entry-event-delegation')||el.className||`scroll-${index}`;}
                function setView(html,options={}){
                  if(options.expectedHash&&currentRouteHash()!==options.expectedHash)return false;
                  if(options.expectedSeq&&options.expectedHash&&appState.realtime.refreshSeq[realtimeRouteKey(options.expectedHash)]!==options.expectedSeq)return false;
                  if(options.silent&&options.expectedHash&&appState.realtime.dirtyRoutes[realtimeRouteKey(options.expectedHash)])return false;
                  const text=String(html ?? '');
                  if(options.silent&&text.includes('loading-state'))return false;
                  if(options.silent&&text.includes('error-state')){toast('实时同步刷新失败，已保留当前页面。');return false;}
                  const view=appView();if(!view)return false;
                  const snapshot=options.silent?captureViewState():null;
                  view.innerHTML=text;
                  if(!options.silent)delete appState.realtime.dirtyRoutes[realtimeRouteKey(currentRouteHash())];
                  if(snapshot)restoreViewState(snapshot);
                  return true;
                }
                function loading(text='正在加载...'){return `<div class="loading-state">${esc(text)}</div>`}
                function empty(text){return `<div class="empty-state">${esc(text)}</div>`}
                function errorBlock(text){return `<div class="error-state">${esc(text)}</div>`}
                function toast(text){const box=document.getElementById('toast');if(!box)return;box.dataset.logicChainTopCenterToast='true';box.dataset.logicChainToastAutoDismiss='true';box.textContent=text;box.hidden=false;clearTimeout(box._timer);box._timer=setTimeout(()=>box.hidden=true,3200);}
                function row(k,v){return `<div class="k">${esc(k)}</div><div class="v">${v ?? ''}</div>`}
                async function api(path, options={}){
                  let res;
                  try{
                    res=await fetch(path,{credentials:'same-origin',headers:{'Content-Type':'application/json',...(options.headers||{})},...options});
                  }catch(err){throw new ApiError(0,'NETWORK_ERROR','无法连接 WebAdmin 服务');}
                  const json=await res.json().catch(()=>({ok:false,error:{code:'BAD_JSON',message:'响应解析失败'}}));
                  if(res.status===401 && document.body.dataset.page==='app'){
                    closeRealtime();
                    sessionStorage.setItem('webadmin.message','登录已失效，请重新登录');
                    location.href='/login';
                    throw new ApiError(401,'UNAUTHORIZED','登录已失效，请重新登录');
                  }
                  if(!res.ok||!json.ok){
                    throw new ApiError(res.status,json.error?.code,json.error?.message||'请求失败');
                  }
                  return json.data;
                }
                async function initLogin(){
                  const form=document.getElementById('login-form'); if(!form) return;
                  hydrateIcons();
                  const pending=sessionStorage.getItem('webadmin.message'); if(pending){document.getElementById('message').textContent=pending;sessionStorage.removeItem('webadmin.message');}
                  document.getElementById('toggle-password').onclick=()=>{const p=document.getElementById('password');p.type=p.type==='password'?'text':'password'};
                  form.onsubmit=async e=>{e.preventDefault();const msg=document.getElementById('message');msg.textContent='正在登录...';try{await api('/api/auth/login',{method:'POST',body:JSON.stringify({username:username.value,password:password.value,rememberMe:remember.checked})});location.href='/app#/dashboard'}catch(err){msg.textContent=err.message}};
                }
                """)
.append("""
                async function initApp(){
                  if(document.body.dataset.page!=='app') return;
                  bindChrome();
                  try{
                    appState.me=await api('/api/auth/me');
                    appState.status=await api('/api/status');
                    appState.capabilities=await api('/api/webadmin/write/capabilities');
                    renderTopbar();
                    startTopbarClock();
                    connectRealtime();
                  }catch(err){return;}
                  if(!location.hash){location.hash='#/dashboard';return;}
                  window.addEventListener('hashchange',()=>route());
                  route();
                }
                function bindChrome(){
                  hydrateIcons();
                  document.querySelectorAll('.nav-item[data-route]').forEach(btn=>btn.onclick=()=>{location.hash=btn.dataset.route});
                  document.querySelectorAll('.nav-item[data-pending]').forEach(btn=>btn.onclick=()=>toast(btn.dataset.pending));
                  document.getElementById('change-password')?.addEventListener('click',()=>showChangeOwnPasswordModal());
                  document.getElementById('logout').onclick=async()=>{try{closeRealtime();await api('/api/auth/logout',{method:'POST',body:'{}'});}finally{location.href='/login'}};
                }
                function renderTopbar(){
                  const s=appState.status, me=appState.me;
                  const serverState=document.getElementById('server-state');
                  if(serverState)serverState.innerHTML=`${icon('server-online')}<span>服务器在线</span>`;
                  document.getElementById('access-mode').textContent=`访问模式：${labelAccessMode(s?.webAdmin?.accessMode)}`;
                  document.getElementById('current-user').textContent=`${me?.displayName || me?.username || '-'}`;
                  document.getElementById('current-role').textContent=`${labelRole(me?.role)}`;
                  updateRealtimeStatus();
                }
                function showChangeOwnPasswordModal(){
                  const body=`<form id="change-password-form" class="edit-form wa-password-form" data-change-password-modal="true" onsubmit="event.preventDefault();submitChangeOwnPassword()">
                    <label>当前密码<input id="change-password-current" data-password-field="current" class="input" type="password" autocomplete="current-password" required></label>
                    <label>新密码<input id="change-password-new" data-password-field="new" class="input" type="password" autocomplete="new-password" minlength="10" maxlength="128" required></label>
                    <label>确认新密码<input id="change-password-confirm" data-password-field="confirm" class="input" type="password" autocomplete="new-password" minlength="10" maxlength="128" required></label>
                    <div id="change-password-error" class="validation-list wa-password-error" hidden></div>
                    <p class="readonly-note">修改成功后当前 session 会继续有效；下次登录请使用新密码。密码不会写入日志、URL 或浏览器存储。</p>
                  </form>`;
                  const footer=`<button class="wa-btn ghost" type="button" onclick="closeWebAdminModal()">${icon('close')}<span>取消</span></button><button id="change-password-save" class="wa-btn primary" type="button" data-change-password-save="true" onclick="document.getElementById('change-password-form')?.requestSubmit()">${icon('check-pass')}<span>保存修改</span></button>`;
                  openWebAdminModal('修改我的密码',body,footer,{className:'wa-password-modal',dirtyCheck:changeOwnPasswordModalDirty});
                  setTimeout(()=>document.getElementById('change-password-current')?.focus(),0);
                }
                function changeOwnPasswordModalDirty(){
                  return ['change-password-current','change-password-new','change-password-confirm'].some(id=>!isBlank(document.getElementById(id)?.value||''));
                }
                function setChangePasswordError(message){
                  const box=document.getElementById('change-password-error');
                  if(!box)return;
                  if(isBlank(message)){box.hidden=true;box.textContent='';return;}
                  box.hidden=false;box.textContent=message;
                }
                async function submitChangeOwnPassword(){
                  const form=document.getElementById('change-password-form');
                  if(!form)return;
                  const current=document.getElementById('change-password-current')?.value||'';
                  const next=document.getElementById('change-password-new')?.value||'';
                  const confirm=document.getElementById('change-password-confirm')?.value||'';
                  if(isBlank(current)){setChangePasswordError('请输入当前密码。');return;}
                  if(isBlank(next)){setChangePasswordError('请输入新密码。');return;}
                  if(next!==confirm){setChangePasswordError('两次输入的新密码不一致。');return;}
                  if(current===next){setChangePasswordError('新密码不能与当前密码相同。');return;}
                  if(next.length<10){setChangePasswordError('新密码至少需要 10 个字符。');return;}
                  if(next.length>128){setChangePasswordError('新密码不能超过 128 个字符。');return;}
                  const save=document.getElementById('change-password-save');
                  const oldText=save?save.innerHTML:'';
                  if(save){save.disabled=true;save.innerHTML=`${icon('clock')}<span>保存中...</span>`;}
                  setChangePasswordError('');
                  try{
                    const result=await api('/api/webadmin/users/me/password',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({oldPassword:current,newPassword:next,confirmPassword:confirm})});
                    if(!result.success){setChangePasswordError(result.message||'密码修改失败。');return;}
                    ['change-password-current','change-password-new','change-password-confirm'].forEach(id=>{const el=document.getElementById(id);if(el)el.value='';});
                    await dismissWebAdminModal();
                    toast(result.message||'密码已修改，下次登录请使用新密码。');
                    appState.capabilities=await api('/api/webadmin/write/capabilities').catch(()=>appState.capabilities);
                  }catch(err){
                    setChangePasswordError(err.message||'密码修改失败。');
                  }finally{
                    if(save){save.disabled=false;save.innerHTML=oldText;}
                  }
                }
                function startTopbarClock(){if(appState.topbarClockTimer)clearTimeout(appState.topbarClockTimer);const tick=()=>{updateTopbarClock();appState.topbarClockTimer=setTimeout(tick,1000);};tick();}
                function updateTopbarClock(){const el=document.getElementById('topbar-clock');if(!el)return;const d=new Date();el.textContent=`${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;}
                function route(options={}){
                  let hash=location.hash || '#/dashboard';
                  try{
                    const result=routeUnsafe(hash,options);
                    if(result&&typeof result.catch==='function')return result.catch(err=>handleRouteRenderError(hash,err,options));
                    return result;
                  }catch(err){
                    return handleRouteRenderError(hash,err,options);
                  }
                }
                function routeUnsafe(hash,options={}){
                  const base=routeBase(hash);
                  closeDeviceMoreMenu(false);
                  maybeReleaseDeviceMetadataEditForRoute(hash);
                  maybeReleaseDeviceBasicConfigEditForRoute(hash);
                  maybeReleaseDeviceExtendedConfigEditForRoute(hash);
                  maybeReleaseVbdNativeTriggerEditForRoute(hash);
                  maybeReleaseInteractionItemMatcherEditForRoute(hash);
                  maybeReleaseDeviceConfigEditForRoute(hash);
                  maybeCancelContainerTemplateSessionForRoute(hash);
                  maybeCancelSingleItemSubmitSessionForRoute(hash);
                  maybeReleaseChannelMetadataEditForRoute(hash);
                  maybeReleaseSignalListenerBasicConfigEditForRoute(hash);
                  maybeReleaseSignalListenerActionsEditForRoute(hash);
                  maybeReleaseSignalJoinEditForRoute(hash,options);
                  maybeReleaseTimerEditForRoute(hash,options);
                  maybeReleaseLogicChainMetadataEditForRoute(hash);
                  if(maybeReleaseLogicChainEditorForRoute(hash,options)===false)return;
                  maybeReleaseConditionGroupEditForRoute(hash);
                  if(maybeReleaseStateVariableEditForRoute(hash,options)===false)return;
                  document.querySelectorAll('.nav-item').forEach(btn=>btn.classList.toggle('active', isRouteActive(btn.dataset.route,hash)));
                  enterRealtimeRoute(hash);
                  if(base==='#/dashboard') return renderDashboard(options);
                  if(base==='#/devices') return renderDevices(options);
                  if(hash.startsWith('#/devices/')) return renderDeviceDetail(hash.substring('#/devices/'.length),options);
                  if(base==='#/virtual-block-devices'||base==='#/block-devices') return renderVirtualBlockDevices(options);
                  if(base==='#/listeners'||base==='#/signal-listeners') return renderListeners(options);
                  if(hash.startsWith('#/listeners/')) return renderSignalListenerDetail(hash.substring('#/listeners/'.length),options);
                  if(hash.startsWith('#/signal-listeners/')) return renderSignalListenerDetail(hash.substring('#/signal-listeners/'.length),options);
                  if(base==='#/receivers') return renderReceivers(options);
                  if(base==='#/signals'||base==='#/signalbridge') return renderSignals(options);
                  if(base==='#/signal-joins') return renderSignalJoinsPage(options);
                  if(hash.startsWith('#/signal-joins/')) return renderSignalJoinDetail(hash.substring('#/signal-joins/'.length),options);
                  if(base==='#/timers') return renderTimersPage(options);
                  if(hash.startsWith('#/timers/')) return renderTimerDetail(hash.substring('#/timers/'.length),options);
                  if(base==='#/conditions'||base==='#/condition-groups') return renderConditionGroupsPage(options);
                  if(hash.startsWith('#/conditions/')) return renderConditionGroupDetail(hash.substring('#/conditions/'.length),options);
                  if(hash.startsWith('#/condition-groups/')) return renderConditionGroupDetail(hash.substring('#/condition-groups/'.length),options);
                  if(hash.startsWith('#/condition-debugger')) return renderConditionDebuggerPage(hash.substring('#/condition-debugger'.length),options);
                  if(base==='#/state-variables') return renderStateVariablesPage(options);
                  if(hash.startsWith('#/state-variables/')) return renderStateVariableDetail(hash.substring('#/state-variables/'.length),options);
                  if(hash.startsWith('#/logic-chains?')&&!parseHashParams(hash).fromHelp) return renderLogicChainLegacyChannelRoute(hash,options);
                  if(base==='#/logic-chains') return renderLogicChainsPage(options);
                  if(isLogicChainResolveRoute(hash)) return renderLogicChainResolve(hash,options);
                  if(hash.startsWith('#/logic-chains/')) return renderLogicChainDetail(hash.substring('#/logic-chains/'.length),options);
                  if(hash.startsWith('#/signals/')) return renderSignalDetail(hash.substring('#/signals/'.length),options);
                  if(base==='#/doctor'||base==='#/diagnostics'||base==='#/signal-doctor') return renderDoctorPage(options);
                  if(hash.startsWith('#/history')) return renderHistoryPage(hash.substring('#/history'.length),options);
                  if(hash.startsWith('#/events')) return renderHistoryPage(hash.substring('#/events'.length),options);
                  if(base==='#/config'||base==='#/config-management'||base==='#/settings/config') return renderConfigPage(options);
                  if(base==='#/users'||base==='#/permissions'||base==='#/users-permissions') return renderUsersPage(options);
                  if(base==='#/settings'||base==='#/system-settings') return renderSettingsPage(options);
                  if(base==='#/regions'||base==='#/region-list') return renderRegionsPage(options);
                  if(base==='#/region-controllers'||base==='#/regionctl') return renderRegionControllersPage(options);
                  if(hash.startsWith('#/region-controllers/')) return renderRegionControllerDetail(hash.substring('#/region-controllers/'.length),options);
                  if(hash.startsWith('#/regions/')) return renderRegionDetail(hash.substring('#/regions/'.length),options);
                  if(base==='#/templates') return renderTemplatesPage(options);
                  if(hash.startsWith('#/templates/')) return renderTemplateDetailPage(hash.substring('#/templates/'.length),options);
                  if(base==='#/snapshots') return renderSnapshotTimelinePage(options);
                  if(hash.startsWith('#/snapshots/')) return renderSnapshotTimelinePage(options);
                  if(base==='#/help'||base==='#/examples') return renderHelpCenterPage(hash,options);
                  if(base==='#/actions') return renderActionsPage(options);
                  if(base==='#/action-templates') return renderActionTemplatesPage(options);
                  if(hash.startsWith('#/actions/')) return renderActionDetail(hash.substring('#/actions/'.length),options);
                  renderPlaceholder('页面暂未接入','该页面将在后续版本接入。');
                }
                function handleRouteRenderError(hash,err,options={}){
                  const message=err&&err.message?err.message:'页面渲染失败';
                  const key=`${hash}|${err&&err.name?err.name:'Error'}|${message}`;
                  if(appState.realtime.lastRenderError!==key){
                    appState.realtime.lastRenderError=key;
                    console.error('WebAdmin route render failed',hash,err);
                  }
                  if(options.silent){toast('实时同步刷新失败，已保留当前页面。');return false;}
                  setView(`<section class="wa-page">${waPageHead('页面渲染失败','当前路由渲染时发生错误，请检查 Console 或刷新重试。',waButton('重试','refresh','onclick="route()"','ghost'))}${errorBlock(message)}</section>`);
                  return false;
                }
                function isRouteActive(route,hash){
                  const r=String(route||''), h=String(hash||'#/dashboard');
                  if(!r)return false;
                  if(r==='#/signals')return h==='#/signals'||h==='#/signalbridge'||h.startsWith('#/signals/');
                  if(r==='#/signal-joins')return h==='#/signal-joins'||h.startsWith('#/signal-joins/');
                  if(r==='#/timers')return h==='#/timers'||h.startsWith('#/timers/');
                  if(r==='#/logic-chains')return h==='#/logic-chains'||h.startsWith('#/logic-chains?')||h.startsWith('#/logic-chains/');
                  if(r==='#/condition-debugger')return h==='#/condition-debugger'||h.startsWith('#/condition-debugger?')||h.startsWith('#/condition-debugger/');
                  if(r==='#/condition-groups')return h==='#/conditions'||h==='#/condition-groups'||h.startsWith('#/condition-groups/')||h.startsWith('#/conditions/');
                  if(r==='#/state-variables')return h==='#/state-variables'||h.startsWith('#/state-variables/');
                  if(r==='#/listeners')return h==='#/listeners'||h==='#/signal-listeners';
                  if(r==='#/history')return h.startsWith('#/history')||h.startsWith('#/events');
                  if(r==='#/devices')return h==='#/devices'||h.startsWith('#/devices/');
                  if(r==='#/virtual-block-devices')return h==='#/virtual-block-devices'||h==='#/block-devices';
                  if(r==='#/templates')return h==='#/templates'||h.startsWith('#/templates/');
                  if(r==='#/snapshots')return h==='#/snapshots'||h.startsWith('#/snapshots/');
                  if(r==='#/help')return h==='#/help'||h.startsWith('#/help?')||h.startsWith('#/examples');
                  if(r==='#/actions')return h==='#/actions'||h.startsWith('#/actions/')||h==='#/action-templates';
                  if(r==='#/regions')return h==='#/regions'||h==='#/region-list'||h.startsWith('#/regions/');
                  if(r==='#/region-controllers')return h==='#/region-controllers'||h==='#/regionctl';
                  if(r==='#/users')return h==='#/users'||h==='#/permissions'||h==='#/users-permissions';
                  if(r==='#/settings')return h==='#/settings'||h==='#/system-settings';
                  if(r==='#/config')return h==='#/config'||h==='#/config-management'||h==='#/settings/config';
                  if(r==='#/doctor')return h==='#/doctor'||h==='#/diagnostics'||h==='#/signal-doctor';
                  return h===r||h.startsWith(r+'/')||h.startsWith(r+'?');
                }
                async function settle(path){try{return{ok:true,data:await api(path)}}catch(err){return{ok:false,error:err}}}
                const REALTIME_EVENT_TYPES=['realtime_connected','heartbeat','sync_required','device_registered','device_removed','device_changed','device_config_changed','device_metadata_changed','receiver_changed','receiver_pulse_changed','virtual_block_device_changed','selection_started','selection_completed','selection_cancelled','selection_failed','container_template_session_started','container_template_session_opened','container_template_session_saved','container_template_session_cancelled','container_template_session_failed','container_template_session_expired','single_item_submit_template_session_started','single_item_submit_template_session_opened','single_item_submit_template_session_saved','single_item_submit_template_session_cancelled','single_item_submit_template_session_failed','single_item_submit_template_session_expired','signal_channel_changed','signal_emitted','signal_history_appended','history_appended','signal_listener_changed','signal_listener_enabled_changed','signal_listener_action_changed','signal_join_changed','timer_changed','timer_runtime_changed','state_variable_changed','action_changed','action_history_appended','action_execution_appended','region_changed','region_controller_changed','region_event_appended','logic_chain_metadata_changed','template_store_changed','template_applied','snapshot_created','snapshot_rollback_applied','snapshot_timeline_changed','condition_group_changed','condition_gate_history_appended','doctor_issues_changed','webadmin_user_changed','webadmin_audit_appended','webadmin_settings_changed','device_updated','doctor_changed','action_executed','receiver_pulse','region_event','config_changed','write_audit_appended','permission_denied','validation_failed','user_changed','system_settings_changed','signal_config_changed','channel_metadata_changed','signal_listener_config_changed','region_config_changed','action_config_changed','edit_lock_changed','webadmin_user_connected','webadmin_user_disconnected'];
                const REALTIME_KNOWN_ROUTE_KEYS=['dashboard','signals','signalJoins','timers','logicChains','templates','snapshots','help','conditionGroups','conditionDebugger','stateVariables','receivers','listeners','actions','actionTemplates','devices','virtualBlockDevices','history','doctor','regions','regionControllers','users','settings','config'];
                function setRealtimeStatus(status,lastEventAt){
                  appState.realtime.status=status;
                  if(lastEventAt)appState.realtime.lastEventAt=lastEventAt;
                  updateRealtimeStatus();
                }
                function updateRealtimeStatus(){
                  const state=document.getElementById('realtime-state'), last=document.getElementById('last-realtime-event');
                  if(!state)return;
                  const label={CONNECTED:'已连接',RECONNECTING:'正在重连',DISCONNECTED:'已断开',UNAVAILABLE:'不可用'}[appState.realtime.status]||'未连接';
                  state.textContent=`实时同步：${label}`;
                  if(last)last.textContent=`最后事件：${formatRelativeTime(appState.realtime.lastEventAt)}`;
                }
                function connectRealtime(force=false){
                  if(document.body.dataset.page!=='app')return;
                  if(appState.realtime.offline){setRealtimeStatus('DISCONNECTED');return;}
                  if(force&&appState.realtime.source){appState.realtime.source.close();appState.realtime.source=null;}
                  if(appState.realtime.source)return;
                  if(typeof EventSource==='undefined'){setRealtimeStatus('UNAVAILABLE');return;}
                  clearTimeout(appState.realtime.reconnectTimer);
                  setRealtimeStatus(appState.realtime.reconnectAttempt>0?'RECONNECTING':'RECONNECTING');
                  const lastSeq=Math.max(0,Number(appState.realtime.lastSeenSeq||0));
                  const url=lastSeq>0?`/api/realtime/events?lastEventId=${encodeURIComponent(String(lastSeq))}`:'/api/realtime/events';
                  const source=new EventSource(url);
                  appState.realtime.source=source;
                  source.onopen=()=>{
                    const shouldSync=appState.realtime.wasDisconnected||appState.realtime.missed;
                    appState.realtime.reconnectAttempt=0;
                    setRealtimeStatus('CONNECTED');
                    if(shouldSync){appState.realtime.wasDisconnected=false;markRealtimeDirty(currentRouteHash(),{type:'reconnect'});flushVisibleRealtimeRefresh('reconnect');}
                  };
                  source.onerror=()=>{if(appState.realtime.source===source){source.close();appState.realtime.source=null;appState.realtime.wasDisconnected=true;scheduleRealtimeReconnect();}};
                  source.addEventListener('message',event=>handleRealtimeEvent('message',event));
                  REALTIME_EVENT_TYPES.forEach(type=>{
                    source.addEventListener(type,event=>handleRealtimeEvent(type,event));
                  });
                }
                function closeRealtime(status='DISCONNECTED'){
                  clearTimeout(appState.realtime.reconnectTimer);
                  appState.realtime.reconnectTimer=null;
                  if(appState.realtime.source){appState.realtime.source.close();appState.realtime.source=null;}
                  appState.realtime.reconnectAttempt=0;
                  setRealtimeStatus('DISCONNECTED');
                }
                function scheduleRealtimeReconnect(){
                  if(document.body.dataset.page!=='app')return;
                  if(appState.realtime.offline)return;
                  if(appState.realtime.reconnectTimer)return;
                  appState.realtime.reconnectAttempt=Math.min(appState.realtime.reconnectAttempt+1,6);
                  const delay=Math.min(30000,1000*Math.pow(2,appState.realtime.reconnectAttempt-1));
                  setRealtimeStatus('RECONNECTING');
                  appState.realtime.reconnectTimer=setTimeout(()=>{appState.realtime.reconnectTimer=null;connectRealtime();},delay);
                }
                function handleRealtimeEvent(type,event){
                  let data={type};
                  try{data=JSON.parse(event.data||'{}');}catch(_){data={type};}
                  data.type=data.type||type;
                  const gap=recordRealtimeSeq(data,event);
                  setRealtimeStatus('CONNECTED',data.occurredAt);
                  if(data.type==='sync_required'||gap){
                    appState.realtime.missed=true;
                    markAllRealtimeDirty(data);
                    markRealtimeDirty(currentRouteHash(),data);
                    markChannelOptionsDirty(data);
                    flushVisibleRealtimeRefresh(data.type==='sync_required'?'sync_required':'seq_gap');
                    return;
                  }
                  if(data.type==='heartbeat'||data.type==='realtime_connected')return;
                  markChannelOptionsDirty(data);
                  rememberDeviceEditLockEvent(data);
                  markRealtimeRoutesForEvent(data);
                  handleSelectionRealtimeEvent(data);
                  handleActionRelayActionsRealtimeEvent(data);
                  handleInteractionItemMatcherRealtimeEvent(data);
                  handleContainerTemplateSessionRealtimeEvent(data);
                  handleSingleItemSubmitSessionRealtimeEvent(data);
                  const hash=currentRouteHash();
                  if(shouldHandleRealtimeEvent(hash,data)){
                    if(document.hidden||appState.realtime.offline){markRealtimeDirty(hash,data);return;}
                    scheduleRealtimeRefresh(hash,data);
                  }
                }
                function recordRealtimeSeq(data,event){
                  const seq=Number(data?.seq||event?.lastEventId||data?.id||0);
                  if(!Number.isFinite(seq)||seq<=0)return false;
                  const last=Number(appState.realtime.lastSeenSeq||0);
                  const control=['heartbeat','realtime_connected'].includes(String(data?.type||''));
                  const gap=last>0&&seq>last+1&&!control;
                  if(seq>last){
                    appState.realtime.lastSeenSeq=seq;
                    appState.realtime.lastEventId=String(seq);
                  }
                  return gap;
                }
                function shouldHandleRealtimeEvent(hash,event){
                  const key=realtimeRouteKey(hash);
                  if(realtimeRouteKeysForEvent(event).has(key))return true;
                  const type=String(event.type||'');
                  if(String(hash||'').startsWith('#/signals/')){const id=routeDetailId(hash,'#/signals/');return event.channel===id||eventAffectedChannels(event).includes(id)||(type==='edit_lock_changed'&&['channel_metadata','signal_listener_basic_config'].includes(String(event.payload?.targetType||'')));}
                  if(String(hash||'').startsWith('#/signal-joins/')){const id=routeDetailId(hash,'#/signal-joins/');return type==='signal_join_changed'||(type==='edit_lock_changed'&&String(event.payload?.targetType||'')==='signal_join_config'&&String(event.payload?.targetId||'')===id)||String(event.payload?.signalJoinId||event.payload?.targetId||'')===id;}
                  if(String(hash||'').startsWith('#/timers/')){const id=routeDetailId(hash,'#/timers/');return type==='timer_changed'||type==='timer_runtime_changed'||(type==='edit_lock_changed'&&String(event.payload?.targetType||'')==='timer_config'&&String(event.payload?.targetId||'')===id)||String(event.payload?.timerId||event.payload?.targetId||'')===id;}
                  if(String(hash||'').startsWith('#/templates/'))return ['template_store_changed','template_applied','edit_lock_changed','config_changed'].includes(type);
                  if(String(hash||'').startsWith('#/condition-groups/')||String(hash||'').startsWith('#/conditions/'))return ['condition_group_changed','edit_lock_changed','config_changed'].includes(type);
                  if(String(hash||'').startsWith('#/logic-chains/')){if(type==='condition_gate_history_appended')return logicChainRealtimeEventMatchesCurrentGraph(event);return ['logic_chain_metadata_changed','signal_emitted','signal_history_appended','history_appended','signal_listener_changed','signal_listener_enabled_changed','signal_listener_action_changed','signal_join_changed','timer_changed','timer_runtime_changed','state_variable_changed','action_changed','action_config_changed','region_controller_changed','region_event_appended','device_changed','device_config_changed','receiver_changed','virtual_block_device_changed','edit_lock_changed','doctor_issues_changed','doctor_changed','config_changed'].includes(type)||type.startsWith('signal_')||type.startsWith('action_')||type.startsWith('device_')||type.startsWith('region_');}
                  if(String(hash||'').startsWith('#/listeners/')){const id=routeDetailId(hash,'#/listeners/'), listenerChannel=routeListenerChannel(id);return listenerEventRef(event)===id||(event.channel&&!isBlank(id)&&String(event.type||'').startsWith('signal_listener_'))||(event.channel&&listenerChannel&&event.channel===listenerChannel&&['signal_emitted','signal_history_appended','history_appended','action_executed','action_execution_appended'].includes(type));}
                  if(String(hash||'').startsWith('#/signal-listeners/')){const id=routeDetailId(hash,'#/signal-listeners/'), listenerChannel=routeListenerChannel(id);return listenerEventRef(event)===id||(event.channel&&!isBlank(id)&&String(event.type||'').startsWith('signal_listener_'))||(event.channel&&listenerChannel&&event.channel===listenerChannel&&['signal_emitted','signal_history_appended','history_appended','action_executed','action_execution_appended'].includes(type));}
                  if(String(hash||'').startsWith('#/state-variables/')){const id=routeDetailId(hash,'#/state-variables/'), targetType=String(event.payload?.targetType||event.targetType||'');return type==='state_variable_changed'||(type==='edit_lock_changed'&&targetType==='state_variable'&&['',id,'new'].includes(String(event.payload?.targetId||event.targetId||'')))||((type==='write_audit_appended'||type==='webadmin_audit_appended')&&['state_variable','state_variable_definition'].includes(targetType));}
                  if(String(hash||'').startsWith('#/devices/'))return !!event.deviceId&&sameDeviceRef(event.deviceId,routeDetailId(hash,'#/devices/'));
                  if(String(hash||'').startsWith('#/regions/'))return !!event.regionId&&event.regionId===routeDetailId(hash,'#/regions/');
                  if(String(hash||'').startsWith('#/actions/'))return !!event.actionId&&event.actionId===routeDetailId(hash,'#/actions/');
                  return false;
                }
                function logicChainRealtimeEventMatchesCurrentGraph(event){const graph=appState.currentLogicChainGraph||{}, payload=event?.payload||{}, targetId=String(payload.targetId||event?.targetId||''), targetType=String(payload.targetType||event?.targetType||''), groupId=String(payload.conditionGroupId||event?.conditionGroupId||''), channel=String(event?.channel||payload.channel||'');return (graph.nodes||[]).some(node=>{const m=node?.metadata||{};return (!!targetId&&String(m.targetId||'')===targetId)||!!targetType&&String(m.targetType||'')===targetType||!!groupId&&String(m.conditionGroupId||'')===groupId||!!channel&&String(node.channel||m.channel||'')===channel;});}
                function handleActionRelayActionsRealtimeEvent(event){
                  const draft=appState.actionRelayActionsEdit;
                  if(!draft||draft.saving)return;
                  const target=String(event?.payload?.targetType||'');
                  const type=String(event?.type||''), source=String(event?.sourceType||event?.payload?.deviceType||'').toLowerCase();
                  const eventDevice=String(event?.deviceId||event?.payload?.deviceId||'');
                  if(type==='edit_lock_changed')return;
                  const actionRelayChange=target==='action_relay_actions'||(type==='action_config_changed'&&source==='action_relay');
                  if(!actionRelayChange||!eventDevice||!sameDeviceRef(eventDevice,draft.deviceId))return;
                  syncActionRelayActionsDraftFromForm(draft.deviceId);
                  const current=appState.actionRelayActionsEdit;
                  if(!current||current.deviceId!==draft.deviceId)return;
                  current.conflict=current.conflict||{remote:true,message:'Action 列表已被其他 WebAdmin 客户端修改。'};
                  current.errors=current.errors&&current.errors.length?current.errors:[{message:'Action 列表已被其他 WebAdmin 客户端修改，请重新加载后再保存。'}];
                  appState.actionRelayActionsEdit=current;
                  rerenderActionRelayActionsEditor(current.deviceId);
                }
                function handleInteractionItemMatcherRealtimeEvent(event){
                  const draft=appState.interactionItemMatcherEdit;
                  if(!draft||draft.saving)return;
                  const target=String(event?.payload?.targetType||''), type=String(event?.type||'');
                  const eventDevice=String(event?.deviceId||event?.payload?.deviceId||'');
                  if(type==='edit_lock_changed')return;
                  if(target!=='interaction_item_matcher'||!eventDevice||!sameDeviceRef(eventDevice,draft.deviceId))return;
                  syncInteractionItemMatcherDraftFromForm(draft.deviceId);
                  const current=appState.interactionItemMatcherEdit;
                  if(!current||!sameDeviceRef(current.deviceId,draft.deviceId))return;
                  current.conflict=current.conflict||{remote:true,message:'交互物品匹配已被其他 WebAdmin 客户端修改。'};
                """).toString();
    }
}
