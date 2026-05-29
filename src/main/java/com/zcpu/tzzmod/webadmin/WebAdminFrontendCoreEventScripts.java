package com.zcpu.tzzmod.webadmin;

// WebAdmin delegated event/realtime 模块维护全局 route table、listener 顺序和 silent refresh 边界。
// 新交互应加入命名 handler + route entry，避免 inline lambda、BeforeVxx wrapper 或巨型 if/closest 回潮。
// 这里的状态更新必须保持非扰动：不重置输入、滚动、modal、dirty draft 或已打开的二级配置页。
final class WebAdminFrontendCoreEventScripts {
    private WebAdminFrontendCoreEventScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                  current.errors=current.errors&&current.errors.length?current.errors:[{message:'交互物品匹配已被其他 WebAdmin 客户端修改，请重新加载后再保存。'}];
                  appState.interactionItemMatcherEdit=current;
                  if(appState.deviceConfigEdit&&sameDeviceRef(appState.deviceConfigEdit.deviceId,current.deviceId))showDeviceConfigEditModal(current.deviceId);else rerenderInteractionItemMatcherEditor(current.deviceId);
                }
                function handleContainerTemplateSessionRealtimeEvent(event){
                  const type=String(event?.type||'');
                  if(!type.startsWith('container_template_session_'))return;
                  const draft=appState.containerTemplateSession;
                  if(!draft)return;
                  const eventDevice=String(event?.deviceId||event?.payload?.deviceId||'');
                  if(eventDevice&&!sameDeviceRef(eventDevice,draft.deviceId))return;
                  const payload=event.payload||{}, sessionId=containerTemplateSessionId(payload)||containerTemplateSessionId(event);
                  if(draft.sessionId&&sessionId&&draft.sessionId!==sessionId)return;
                  draft.sessionId=sessionId||draft.sessionId||'';
                  draft.status=type.replace('container_template_session_','')||draft.status||'started';
                  if(type==='container_template_session_opened')draft.status='opened';
                  if(type==='container_template_session_saved')draft.status='saved';
                  if(type==='container_template_session_cancelled')draft.status='cancelled';
                  if(type==='container_template_session_failed')draft.status='failed';
                  if(type==='container_template_session_expired')draft.status='expired';
                  draft.active=!['saved','completed','cancelled','failed','expired'].includes(draft.status);
                  if(!draft.active){draft.lockId='';draft.lock=null;stopContainerTemplateSessionHeartbeat();stopContainerTemplateSessionStatusPoll();cancelContainerTemplateCancelConfirm();}
                  draft.message=event.summary||payload.message||draft.message||'';
                  if(['saved','completed'].includes(draft.status)){refreshContainerTemplateSessionOverview(draft.deviceId);return;}
                  showContainerTemplateSessionModal(draft.deviceId);
                }
                function handleSingleItemSubmitSessionRealtimeEvent(event){
                  const type=String(event?.type||'');
                  if(!type.startsWith('single_item_submit_template_session_'))return;
                  const draft=appState.singleItemSubmitSession;
                  if(!draft)return;
                  const eventDevice=String(event?.deviceId||event?.payload?.deviceId||'');
                  if(eventDevice&&!sameDeviceRef(eventDevice,draft.deviceId))return;
                  const payload=event.payload||{}, sessionId=singleItemSubmitSessionId(payload)||singleItemSubmitSessionId(event);
                  if(draft.sessionId&&sessionId&&draft.sessionId!==sessionId)return;
                  draft.sessionId=sessionId||draft.sessionId||'';
                  draft.status=type.replace('single_item_submit_template_session_','')||draft.status||'started';
                  if(type==='single_item_submit_template_session_opened')draft.status='opened';
                  if(type==='single_item_submit_template_session_saved')draft.status='saved';
                  if(type==='single_item_submit_template_session_cancelled')draft.status='cancelled';
                  if(type==='single_item_submit_template_session_failed')draft.status='failed';
                  if(type==='single_item_submit_template_session_expired')draft.status='expired';
                  draft.active=!['saved','completed','cancelled','failed','expired'].includes(draft.status);
                  if(!draft.active){draft.lockId='';draft.lock=null;stopSingleItemSubmitSessionHeartbeat();stopSingleItemSubmitSessionStatusPoll();cancelSingleItemSubmitCancelConfirm();}
                  draft.message=event.summary||payload.message||draft.message||'';
                  if(['saved','completed'].includes(draft.status)){refreshSingleItemSubmitSessionOverview(draft.deviceId,true);return;}
                  showSingleItemSubmitSessionModal(draft.deviceId);
                }
                function listenerEventRef(event){return String(event?.listenerId||event?.payload?.listenerId||event?.payload?.listenerRef||event?.payload?.targetId||event?.payload?.id||'');}
                function routeListenerChannel(id){
                  const target=String(id||'');
                  const current=appState.currentSignalListenerDetail||{};
                  if(target&&listenerMatches(current.listener||{},target))return current.channel||current.listener?.channel||'';
                  const cached=(appState.listeners||[]).find(l=>listenerMatches(l,target));
                  return cached?.channel||cached?.sourceChannel||'';
                }
                function markRealtimeRoutesForEvent(event){
                  const keys=realtimeRouteKeysForEvent(event);
                  keys.forEach(key=>markRealtimeRouteKeyDirty(key,event));
                }
                function realtimeRouteKeysForEvent(event){
                  const type=String(event?.type||''), keys=new Set();
                  const add=(...items)=>items.filter(Boolean).forEach(item=>keys.add(item));
                  const starts=(...prefixes)=>prefixes.some(prefix=>type.startsWith(prefix));
                  const isAny=(...items)=>items.includes(type);
                  if(isAny('config_changed')){
                    const target=String(event?.payload?.targetType||'');
                    if(target==='template_package'||target==='template_store'||target==='template_apply'){add('dashboard','templates','logicChains','signals','signalJoins','timers','listeners','config');}
                    else if(target==='action_relay_actions'){add('dashboard','signals','devices','actions','actionTemplates','doctor');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,event?.sourceType||event?.payload?.deviceType||'action_relay');if(event?.channel)add(`signalDetail:${event.channel}`);eventAffectedChannels(event).forEach(channel=>add(`signalDetail:${channel}`));}
                    else if(target==='interaction_item_matcher'||target==='virtual_block_device_triggers'||target==='virtual_block_device_container_template'||target==='virtual_block_device_single_item_submit'){add('dashboard','signals','devices','virtualBlockDevices','doctor');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,event?.sourceType||event?.payload?.deviceType||'virtual_block_device');if(event?.channel)add(`signalDetail:${event.channel}`);eventAffectedChannels(event).forEach(channel=>add(`signalDetail:${channel}`));}
                    else if(target.includes('device')){const source=String(event?.sourceType||event?.payload?.deviceType||'').toLowerCase();add('dashboard','devices','doctor');if(target==='device_basic_config'){add('signals');if(event?.channel)add(`signalDetail:${event.channel}`);if(event?.payload?.previousChannel)add(`signalDetail:${event.payload.previousChannel}`);}if(source==='signal_receiver')add('receivers');else if(source==='virtual_block_device')add('virtualBlockDevices');else if(source==='action_relay')add('actions','actionTemplates');else if(!source)add('receivers','virtualBlockDevices','actions','actionTemplates');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,source);}
                    else if(target.includes('listener')){add('dashboard','signals','listeners','doctor');if(listenerEventRef(event))add(`listenerDetail:${listenerEventRef(event)}`);}
                    else if(target==='signal_join_config'){add('dashboard','signals','signalJoins','logicChains','history','doctor','config');if(event?.payload?.signalJoinId||event?.payload?.targetId)add(`signalJoinDetail:${event.payload.signalJoinId||event.payload.targetId}`);if(event?.channel)add(`signalDetail:${event.channel}`);eventAffectedChannels(event).forEach(channel=>add(`signalDetail:${channel}`));}
                    else if(target==='timer_config'||target==='timer_runtime'){add('dashboard','signals','timers','logicChains','history','doctor','config');if(event?.payload?.timerId||event?.payload?.targetId)add(`timerDetail:${event.payload.timerId||event.payload.targetId}`);if(event?.channel)add(`signalDetail:${event.channel}`);}
                    else if(target==='logic_chain_metadata'){add('dashboard','logicChains','config');if(event?.payload?.targetId)add(`logicChainDetail:${event.payload.targetId}`);}
                    else if(target.includes('channel')){add('dashboard','signals','doctor');if(event?.channel)add(`signalDetail:${event.channel}`);}
                    else if(target==='condition_group'){add('dashboard','conditionGroups','config');if(event?.payload?.targetId)add(`conditionGroupDetail:${event.payload.targetId}`);}
                    else if(target==='state_variable'||target==='state_variable_definition'){add('dashboard','stateVariables','logicChains','conditionGroups','history','config');if(event?.payload?.targetId)add(`stateVariableDetail:${event.payload.targetId}`);}
                    else if(target==='region_controller_config'){add('dashboard','regions','regionControllers','history','config');if(event?.payload?.controllerId)add(`regionControllerDetail:${event.payload.controllerId}`);if(event?.regionId)add(`regionDetail:${event.regionId}`);}
                    else add('dashboard','config');
                  }
                  if(starts('device_')||starts('receiver_')||starts('virtual_block_device_')||isAny('device_updated','receiver_pulse','device_config_changed')){
                    const source=String(event?.sourceType||event?.payload?.deviceType||'').toLowerCase();
                    const target=String(event?.payload?.targetType||'');
                    add('dashboard','devices','doctor');
                    if(target==='device_basic_config'){add('signals');if(event?.channel)add(`signalDetail:${event.channel}`);if(event?.payload?.previousChannel)add(`signalDetail:${event.payload.previousChannel}`);}
                    if(source==='signal_receiver'||starts('receiver_')||isAny('receiver_pulse'))add('receivers');
                    if(source==='virtual_block_device'||starts('virtual_block_device_'))add('virtualBlockDevices');
                    if(source==='action_relay')add('actions','actionTemplates');
                    if(isAny('device_config_changed')&&!source)add('config');
                    if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,source);
                  }
                  if(starts('selection_')){
                    add('dashboard','devices','virtualBlockDevices','history');
                    const deviceId=event?.deviceId||event?.payload?.deviceId;
                    if(deviceId)addDeviceDetailRouteKeys(add,deviceId,event?.sourceType||event?.payload?.deviceType||'virtual_block_device');
                  }
                  if(starts('container_template_session_')){
                    add('dashboard','devices','virtualBlockDevices','history');
                    const deviceId=event?.deviceId||event?.payload?.deviceId;
                    if(deviceId)addDeviceDetailRouteKeys(add,deviceId,event?.sourceType||event?.payload?.deviceType||'virtual_block_device');
                  }
                  if(starts('single_item_submit_template_session_')){
                    add('dashboard','devices','virtualBlockDevices','history','doctor');
                    const deviceId=event?.deviceId||event?.payload?.deviceId;
                    if(deviceId)addDeviceDetailRouteKeys(add,deviceId,event?.sourceType||event?.payload?.deviceType||'virtual_block_device');
                  }
                  if(starts('signal_')||isAny('signal_emitted','history_appended','signal_history_appended','channel_metadata_changed','signal_listener_config_changed','signal_config_changed')){
                    add('dashboard','signals','logicChains','listeners','history','doctor');
                    if(isAny('signal_listener_changed','signal_listener_enabled_changed','signal_listener_action_changed','signal_listener_config_changed','signal_config_changed'))add('config');
                    if(isAny('signal_join_changed')){add('signalJoins','config');if(event?.payload?.signalJoinId||event?.payload?.targetId)add(`signalJoinDetail:${event.payload.signalJoinId||event.payload.targetId}`);eventAffectedChannels(event).forEach(channel=>add(`signalDetail:${channel}`));}
                    if(isAny('timer_changed','timer_runtime_changed')){add('timers','config');if(event?.payload?.timerId||event?.payload?.targetId)add(`timerDetail:${event.payload.timerId||event.payload.targetId}`);}
                    if(event?.channel)add(`signalDetail:${event.channel}`);
                    if(listenerEventRef(event))add(`listenerDetail:${listenerEventRef(event)}`);
                  }
                  if(starts('action_')||isAny('action_executed','action_config_changed')){
                    const source=String(event?.sourceType||event?.payload?.deviceType||'').toLowerCase();
                    add('dashboard','logicChains','actions','actionTemplates','history');
                    if(stateActionRealtimeChanged(event))add('stateVariables');
                    if(source==='action_relay'){add('signals');if(event?.channel)add(`signalDetail:${event.channel}`);eventAffectedChannels(event).forEach(channel=>add(`signalDetail:${channel}`));}
                    if(isAny('action_changed','action_config_changed')){add('devices','doctor');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,source);}
                    if(event?.actionId)add(`actionDetail:${event.actionId}`);
                  }
                  if(starts('region_')||isAny('region_event','region_config_changed')){
                    add('dashboard','logicChains','regions','regionControllers','history');
                    if(isAny('region_changed','region_config_changed','region_controller_changed'))add('config');
                    if(event?.regionId)add(`regionDetail:${event.regionId}`);
                    if(event?.payload?.controllerId)add(`regionControllerDetail:${event.payload.controllerId}`);
                  }
                  if(isAny('logic_chain_metadata_changed')){
                    add('dashboard','logicChains','config');
                    if(event?.payload?.chainId||event?.payload?.targetId)add(`logicChainDetail:${event.payload.chainId||event.payload.targetId}`);
                  }
                  if(isAny('template_store_changed','template_applied')){
                    add('dashboard','templates','logicChains','signals','signalJoins','timers','listeners','actions','config');
                    const source=event?.payload?.source||'built_in', templateId=event?.payload?.templateId||event?.templateId||'';
                    if(templateId)add(`templateDetail:${source}:${templateId}`);
                  }
                  if(isAny('snapshot_created','snapshot_rollback_applied','snapshot_timeline_changed')){
                    add('dashboard','snapshots','config','templates','logicChains','signals','signalJoins','timers','listeners','conditionGroups','stateVariables','regions','regionControllers');
                  }
                  if(isAny('condition_group_changed')){
                    add('dashboard','conditionGroups','config');
                    if(event?.payload?.conditionGroupId)add(`conditionGroupDetail:${event.payload.conditionGroupId}`);
                  }
                  if(isAny('state_variable_changed')){
                    add('dashboard','stateVariables','logicChains','conditionGroups','history','config');
                    if(event?.payload?.targetId||event?.payload?.stateVariableId)add(`stateVariableDetail:${event.payload.targetId||event.payload.stateVariableId}`);
                  }
                  if(isAny('condition_gate_history_appended')){
                    add('dashboard','conditionDebugger','history','doctor');
                    const gateTargetType=String(event?.payload?.targetType||'');
                    const gateTargetId=event?.payload?.targetId||'';
                    const gateDeviceId=event?.deviceId||event?.payload?.deviceId||(['ACTION_RELAY','VBD_REDSTONE','VBD_BLOCKSTATE','VBD_INTERACTION','ITEM_SUBMIT','CONTAINER_OPEN','CONTAINER_CLOSE','CONTAINER_CHANGE'].includes(gateTargetType)?gateTargetId:'');
                    if(gateDeviceId)addDeviceDetailRouteKeys(add,gateDeviceId,event?.sourceType||gateTargetType||'');
                    if(event?.payload?.conditionGroupId)add(`conditionGroupDetail:${event.payload.conditionGroupId}`);
                    if(gateTargetType==='SIGNAL_LISTENER'&&(event?.payload?.listenerId||gateTargetId))add(`listenerDetail:${event?.payload?.listenerId||gateTargetId}`);
                    if(gateTargetType.startsWith('REGION_')&&gateTargetId)add(`regionControllerDetail:${gateTargetId}`);
                  }
                  if(starts('doctor_')||isAny('doctor_changed'))add('dashboard','doctor','settings');
                  if(starts('webadmin_user_')||isAny('webadmin_user_connected','webadmin_user_disconnected','user_changed'))add('dashboard','users');
                  if(starts('webadmin_audit_')||isAny('write_audit_appended')){
                    const target=String(event?.payload?.targetType||'');
                    if(target==='action_relay_actions'){add('dashboard','history','signals','devices','actions','actionTemplates');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,event?.sourceType||event?.payload?.deviceType||'action_relay');if(event?.channel)add(`signalDetail:${event.channel}`);eventAffectedChannels(event).forEach(channel=>add(`signalDetail:${channel}`));}
                    else if(target==='timer_config'||target==='timer_runtime'){add('dashboard','history','timers','signals','logicChains');if(event?.payload?.timerId||event?.payload?.targetId)add(`timerDetail:${event.payload.timerId||event.payload.targetId}`);if(event?.channel)add(`signalDetail:${event.channel}`);}
                    else if(target==='interaction_item_matcher'||target==='virtual_block_device_triggers'||target==='virtual_block_device_container_template'){add('dashboard','history','devices','virtualBlockDevices','signals');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,event?.sourceType||event?.payload?.deviceType||'virtual_block_device');if(event?.channel)add(`signalDetail:${event.channel}`);eventAffectedChannels(event).forEach(channel=>add(`signalDetail:${channel}`));}
                    else if(target.includes('device')){const source=String(event?.sourceType||event?.payload?.deviceType||'').toLowerCase();add('dashboard','history','devices');if(target==='device_basic_config'){add('signals');if(event?.channel)add(`signalDetail:${event.channel}`);if(event?.payload?.previousChannel)add(`signalDetail:${event.payload.previousChannel}`);}if(source==='signal_receiver')add('receivers');else if(source==='virtual_block_device')add('virtualBlockDevices');else if(source==='action_relay')add('actions','actionTemplates');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,source);}
                    else if(target==='logic_chain_metadata'){add('dashboard','history','logicChains','config');if(event?.payload?.targetId)add(`logicChainDetail:${event.payload.targetId}`);}
                    else if(target==='state_variable'||target==='state_variable_definition'){add('dashboard','history','stateVariables','logicChains','conditionGroups','config');if(event?.payload?.targetId)add(`stateVariableDetail:${event.payload.targetId}`);}
                    else add('dashboard','history','settings','config','users');
                  }
                  if(starts('webadmin_settings_')||isAny('system_settings_changed'))add('settings','config','dashboard');
                  if(type==='edit_lock_changed'){
                    const target=String(event?.payload?.targetType||'');
                    if(target==='logic_chain_metadata'){add('logicChains');if(event?.payload?.targetId)add(`logicChainDetail:${event.payload.targetId}`);}
                    if(target==='condition_group'){add('conditionGroups');if(event?.payload?.targetId)add(`conditionGroupDetail:${event.payload.targetId}`);}
                    if(target==='signal_join_config'){add('signalJoins');if(event?.payload?.targetId)add(`signalJoinDetail:${event.payload.targetId}`);}
                    if(target==='timer_config'){add('timers');if(event?.payload?.targetId)add(`timerDetail:${event.payload.targetId}`);}
                    if(target==='template_store'||target==='template_apply'){add('templates');if(event?.payload?.targetId)add(`templateDetail:${event.payload.targetId}`);}
                    if(target.includes('device')||target==='action_relay_actions'||target==='interaction_item_matcher'||target==='virtual_block_device_triggers'||target==='virtual_block_device_container_template'){add('devices');if(event?.payload?.targetId)addDeviceDetailRouteKeys(add,event.payload.targetId,target==='action_relay_actions'?'action_relay':(target==='interaction_item_matcher'||target==='virtual_block_device_triggers'||target==='virtual_block_device_container_template'?'virtual_block_device':''));}
                    if(target.includes('channel'))add('signals');
                    if(target.includes('listener')){add('listeners','signals');if(listenerEventRef(event))add(`listenerDetail:${listenerEventRef(event)}`);}
                    if(target==='state_variable'){add('stateVariables');if(event?.payload?.targetId)add(`stateVariableDetail:${event.payload.targetId}`);}
                    if(target==='region_controller_config'){add('regionControllers');if(event?.payload?.targetId)add(`regionControllerDetail:${event.payload.targetId}`);}
                  }
                  return keys;
                }
                function routeDetailId(hash,prefix){const raw=String(hash||'').substring(prefix.length), q=raw.indexOf('?'), value=q>=0?raw.substring(0,q):raw;try{return decodeURIComponent(value)}catch(_){return value;}}
                function scheduleRealtimeRefresh(hash,event){
                  const key=realtimeRouteKey(hash);
                  if(document.hidden||appState.realtime.offline){markRealtimeDirty(hash,event);return;}
                  if(appState.realtime.pendingRefresh[key]){markRealtimeDirty(hash,event);return;}
                  if(appState.realtime.refreshTimers[key])return;
                  appState.realtime.refreshTimers[key]=setTimeout(()=>runRealtimeRefresh(hash,key),250);
                }
                async function runRealtimeRefresh(hash,key){
                  delete appState.realtime.refreshTimers[key];
                  if(currentRouteHash()!==hash)return;
                  if(appState.realtime.pendingRefresh[key]){markRealtimeDirty(hash,{type:'pending'});return;}
                  appState.realtime.pendingRefresh[key]=true;
                  delete appState.realtime.dirtyRoutes[key];
                  const seq=(appState.realtime.refreshSeq[key]||0)+1;
                  appState.realtime.refreshSeq[key]=seq;
                  try{
                    await route({silent:true,expectedHash:hash,expectedSeq:seq});
                    appState.realtime.missed=false;
                    appState.realtime.wasDisconnected=false;
                  }catch(err){
                    toast('实时同步刷新失败，已保留当前页面。');
                  }finally{
                    delete appState.realtime.pendingRefresh[key];
                    if(appState.realtime.dirtyRoutes[key]&&currentRouteHash()===hash&&!document.hidden)scheduleRealtimeRefresh(hash,{type:'dirty'});
                  }
                }
                function markRealtimeDirty(hash,event){
                  appState.realtime.dirtyRoutes[realtimeRouteKey(hash)]={hash,event,at:Date.now()};
                }
                function markRealtimeRouteKeyDirty(key,event){
                  if(!key)return;
                  appState.realtime.dirtyRoutes[key]={hash:null,event,at:Date.now()};
                }
                function markAllRealtimeDirty(event){
                  REALTIME_KNOWN_ROUTE_KEYS.forEach(key=>markRealtimeRouteKeyDirty(key,event));
                }
                function flushVisibleRealtimeRefresh(reason='visibility'){
                  const hash=currentRouteHash(), key=realtimeRouteKey(hash);
                  if(appState.realtime.offline)return;
                  if(appState.realtime.dirtyRoutes[key]||appState.realtime.missed||appState.realtime.wasDisconnected)scheduleRealtimeRefresh(hash,appState.realtime.dirtyRoutes[key]?.event||{type:reason});
                }
                function realtimeRouteKey(hash){
                  const h=String(hash||'#/dashboard');
                  if(h.startsWith('#/devices/'))return `deviceDetail:${routeDetailId(h,'#/devices/')}`;
                  if(h.startsWith('#/signals/'))return `signalDetail:${routeDetailId(h,'#/signals/')}`;
                  if(h.startsWith('#/signal-joins/'))return `signalJoinDetail:${routeDetailId(h,'#/signal-joins/')}`;
                  if(h.startsWith('#/timers/'))return `timerDetail:${routeDetailId(h,'#/timers/')}`;
                  if(h.startsWith('#/logic-chains?'))return 'logicChains';
                  if(h.startsWith('#/logic-chains/'))return isLogicChainResolveRoute(h)?'logicChains':`logicChainDetail:${routeDetailId(h,'#/logic-chains/')}`;
                  if(h.startsWith('#/templates/'))return `templateDetail:${routeDetailId(h,'#/templates/')}`;
                  if(h.startsWith('#/snapshots/'))return 'snapshots';
                  if(h.startsWith('#/condition-groups/'))return `conditionGroupDetail:${routeDetailId(h,'#/condition-groups/')}`;
                  if(h.startsWith('#/conditions/'))return `conditionGroupDetail:${routeDetailId(h,'#/conditions/')}`;
                  if(h.startsWith('#/state-variables/'))return `stateVariableDetail:${routeDetailId(h,'#/state-variables/')}`;
                  if(h.startsWith('#/listeners/'))return `listenerDetail:${routeDetailId(h,'#/listeners/')}`;
                  if(h.startsWith('#/signal-listeners/'))return `listenerDetail:${routeDetailId(h,'#/signal-listeners/')}`;
                  if(h.startsWith('#/regions/'))return `regionDetail:${routeDetailId(h,'#/regions/')}`;
                  if(h.startsWith('#/region-controllers/'))return `regionControllerDetail:${routeDetailId(h,'#/region-controllers/')}`;
                  if(h.startsWith('#/actions/'))return `actionDetail:${routeDetailId(h,'#/actions/')}`;
                  if(h.startsWith('#/history')||h.startsWith('#/events'))return 'history';
                  if(h.startsWith('#/condition-debugger'))return 'conditionDebugger';
                  if(h==='#/dashboard')return 'dashboard';
                  if(h==='#/devices')return 'devices';
                  if(h==='#/virtual-block-devices'||h==='#/block-devices')return 'virtualBlockDevices';
                  if(h==='#/listeners'||h==='#/signal-listeners')return 'listeners';
                  if(h==='#/receivers')return 'receivers';
                  if(h==='#/signals'||h==='#/signalbridge')return 'signals';
                  if(h==='#/signal-joins')return 'signalJoins';
                  if(h==='#/timers')return 'timers';
                  if(h==='#/logic-chains')return 'logicChains';
                  if(h==='#/conditions'||h==='#/condition-groups')return 'conditionGroups';
                  if(h==='#/state-variables')return 'stateVariables';
                  if(h==='#/doctor'||h==='#/diagnostics'||h==='#/signal-doctor')return 'doctor';
                  if(h==='#/regions'||h==='#/region-list')return 'regions';
                  if(h==='#/region-controllers'||h==='#/regionctl')return 'regionControllers';
                  if(h==='#/actions')return 'actions';
                  if(h==='#/templates')return 'templates';
                  if(h==='#/snapshots')return 'snapshots';
                  if(h==='#/action-templates')return 'actionTemplates';
                  if(h==='#/users'||h==='#/permissions'||h==='#/users-permissions')return 'users';
                  if(h==='#/settings'||h==='#/system-settings')return 'settings';
                  if(h==='#/config'||h==='#/config-management'||h==='#/settings/config')return 'config';
                  return h;
                }
                function routePollInterval(hash){
                  return 0;
                }
                function enterRealtimeRoute(hash){
                  startRouteSilentPolling(hash);
                }
                function startRouteSilentPolling(hash){
                  clearTimeout(appState.realtime.pollTimer);
                  appState.realtime.pollTimer=null;
                  const delay=routePollInterval(hash);
                  if(!delay||document.body.dataset.page!=='app')return;
                  const key=realtimeRouteKey(hash);
                  appState.realtime.pollHash=hash;
                  appState.realtime.pollTimer=setTimeout(()=>{
                    appState.realtime.pollTimer=null;
                    const current=currentRouteHash();
                    if(realtimeRouteKey(current)!==key)return;
                    if(document.hidden){markRealtimeDirty(current,{type:'poll'});startRouteSilentPolling(current);return;}
                    scheduleRealtimeRefresh(current,{type:'poll'});
                    startRouteSilentPolling(current);
                  },delay);
                }
                document.addEventListener('visibilitychange',()=>{if(!document.hidden&&document.body.dataset.page==='app'){updateRealtimeStatus();connectRealtime();flushVisibleRealtimeRefresh('visibility');}});
                window.addEventListener('online',()=>{appState.realtime.offline=false;appState.realtime.wasDisconnected=true;connectRealtime(true);markRealtimeDirty(currentRouteHash(),{type:'online'});flushVisibleRealtimeRefresh('online');});
                window.addEventListener('offline',()=>{appState.realtime.offline=true;appState.realtime.wasDisconnected=true;if(appState.realtime.source){appState.realtime.source.close();appState.realtime.source=null;}markRealtimeDirty(currentRouteHash(),{type:'offline'});setRealtimeStatus('DISCONNECTED');});
                window.addEventListener('pagehide',()=>closeRealtime('DISCONNECTED'));
                function dispatchDelegatedEvent(event,routes,options={}){const target=event?.target;if(!target)return false;for(const route of routes||[]){const selector=route.selector||'',match=selector?(target.closest?.(selector)||null):target;if(!match)continue;if(route.handler(event,match))return true;}return false;}
                function dispatchDelegatedSideEffects(event,routes){const target=event?.target;if(!target)return false;let changed=false;for(const route of routes||[]){const selector=route.selector||'',match=selector?(target.closest?.(selector)||null):target;if(!match)continue;if(route.handler(event,match))changed=true;}return changed;}
                function closeAllCustomComboboxes(){
                  let closed=false;
                  const basic=appState.deviceBasicConfigEdit;
                  if(basic&&basic.channelComboOpen){basic.channelComboOpen=false;syncDeviceBasicConfigChannelCombo(basic.deviceId);closed=true;}
                  const extended=appState.deviceExtendedConfigEdit;
                  if(extended){Object.keys(extended.channelComboOpen||{}).forEach(field=>{if(extended.channelComboOpen[field])closed=true;extended.channelComboOpen[field]=false;});(extended.supportedFields||[]).filter(isExtendedChannelField).forEach(field=>syncDeviceExtendedConfigChannelCombo(extended.deviceId,field));}
                  const createListener=appState.signalListenerCreate;
                  if(createListener&&createListener.channelComboOpen){createListener.channelComboOpen=false;syncSignalListenerCreateChannelCombo();closed=true;}
                  const listener=appState.signalListenerBasicConfigEdit;
                  if(listener&&listener.channelComboOpen){listener.channelComboOpen=false;syncSignalListenerBasicConfigChannelCombo(listener.listenerRef);closed=true;}
                  const listenerAction=appState.signalListenerActionsEdit?.draft;
                  if(listenerAction&&listenerAction.channelComboOpen){listenerAction.channelComboOpen=false;syncSignalListenerActionChannelCombo();closed=true;}
                  const nativeDraft=appState.vbdNativeTriggerEdit;
                  if(nativeDraft){Object.keys(nativeDraft.channelComboOpen||{}).forEach(key=>{if(nativeDraft.channelComboOpen[key])closed=true;nativeDraft.channelComboOpen[key]=false;syncVbdNativeTriggerChannelCombo(nativeDraft.deviceId,key);});}
                  const actionRelay=appState.actionRelayActionsEdit;
                  if(actionRelay){Object.keys(actionRelay.channelComboOpen||{}).forEach(index=>{if(actionRelay.channelComboOpen[index])closed=true;actionRelay.channelComboOpen[index]=false;syncActionRelayChannelCombo(actionRelay.deviceId,index);});}
                  const logicChainChannel=appState.logicChainEditor?.newChannelDraft;
                  if(logicChainChannel&&logicChainChannel.channelComboOpen){logicChainChannel.channelComboOpen=false;syncLogicChainDraftChannelCombo();closed=true;}
                  const logicChainAppend=appState.logicChainEditor?.actionAppend;
                  if(logicChainAppend&&logicChainAppend.channelComboOpen){logicChainAppend.channelComboOpen=false;syncLogicChainActionAppendChannelCombo();closed=true;}
                  const regionDraft=appState.regionControllerEdit?.draft;
                  if(regionDraft&&regionDraft.regionComboOpen){regionDraft.regionComboOpen=false;syncRegionControllerRegionCombo(appState.regionControllerEdit.mode);closed=true;}
                  if(regionDraft&&regionDraft.channelComboOpen){regionDraft.channelComboOpen=false;syncRegionControllerActionChannelCombo();closed=true;}
                  const selection=appState.selectionCreateVirtualBlock;
                  if(selection&&selection.step==='config'){if(selection.playerComboOpen||selection.channelComboOpen)closed=true;selection.playerComboOpen=false;selection.channelComboOpen=false;syncSelectionCombos();}
                  if(closeSignalJoinChannelOptions())closed=true;
                  if(closeTimerChannelOptions())closed=true;
                  if(closeTimerActionOptions())closed=true;
                  return closed;
                }
                const CUSTOM_COMBOBOX_GUARD_MARKERS='data-custom-combobox-arrow-toggle-close="true" data-custom-combobox-outside-click-close="true" data-custom-combobox-escape-close="true" data-custom-combobox-select-option-close="true" data-custom-combobox-single-open="true" data-custom-combobox-toggle-no-dirty="true"';
                function globalEventTargetOutside(target,selector){return !(target&&target.closest&&target.closest(selector));}
                function handleGlobalVbdCaptureRetryRoute(event,button){return handleLogicChainVbdCaptureRetryDelegatedClick(event,button);}
                function handleGlobalLogicChainClickRoute(event){return handleLogicChainEditorDelegatedClick(event);}
                function handleGlobalLogicChainPointerDownRoute(event){return handleLogicChainEditorDelegatedPointerDown(event);}
                function handleGlobalLogicChainMouseOverRoute(event){return handleLogicChainEditorDelegatedMouseOver(event);}
                function handleGlobalLogicChainMouseOutRoute(event){return handleLogicChainEditorDelegatedMouseOut(event);}
                const globalPointerUpCaptureRoutes=[{selector:'#wa-modal-root [data-logic-chain-vbd-capture-retry-click-capture="true"]',handler:handleGlobalVbdCaptureRetryRoute}];
                const globalClickCaptureRoutes=[{selector:'#wa-modal-root [data-logic-chain-vbd-capture-retry-click-capture="true"]',handler:handleGlobalVbdCaptureRetryRoute},{handler:handleGlobalLogicChainClickRoute}];
                const globalPointerDownCaptureRoutes=[{handler:handleGlobalLogicChainPointerDownRoute}];
                const globalMouseOverCaptureRoutes=[{handler:handleGlobalLogicChainMouseOverRoute}];
                const globalMouseOutCaptureRoutes=[{handler:handleGlobalLogicChainMouseOutRoute}];
                function handleGlobalPointerUpCapture(event){dispatchDelegatedEvent(event,globalPointerUpCaptureRoutes);}
                function handleGlobalClickCapture(event){dispatchDelegatedEvent(event,globalClickCaptureRoutes);}
                function handleGlobalPointerDownCapture(event){dispatchDelegatedEvent(event,globalPointerDownCaptureRoutes);}
                function handleGlobalMouseOverCapture(event){dispatchDelegatedEvent(event,globalMouseOverCaptureRoutes);}
                function handleGlobalMouseOutCapture(event){dispatchDelegatedEvent(event,globalMouseOutCaptureRoutes);}
                function handleGlobalTimerManualSubmitClick(event,button){event.preventDefault();event.stopPropagation();submitTimerManualForm(button.closest('#wa-modal-root')?.querySelector('[data-timer-manual-form]'));return true;}
                function handleGlobalTimerActionSubmitClick(event){event.preventDefault();event.stopPropagation();syncTimerDraft();saveTimerEdit();return true;}
                function handleGlobalConditionTypeSuiteClick(event,button){event.preventDefault();event.stopPropagation();changeConditionTypeSuiteFromElement(button);return true;}
                function handleGlobalConditionTypeOptionClick(event,button){event.preventDefault();event.stopPropagation();changeConditionNodeTypeFromElement(button);return true;}
                function handleGlobalConditionCardClick(event,card){const target=event.target;if(target.closest('[data-condition-node-quick-action],button,a,input,select,textarea'))return false;event.preventDefault();event.stopPropagation();openConditionNodeEditor(card.dataset.conditionEditPath||'');return true;}
                const globalClickCommandRoutes=[{handler:handleContainerTemplateAction},{handler:handleSingleItemSubmitAction},{handler:handlePaginationAction},{handler:handleGlobalLogicChainClickRoute},{selector:'[data-logic-chain-vbd-trigger-card]',handler:handleLogicChainVbdTriggerCardClick},{handler:handleSnapshotTimelineNodeClick},{handler:handleSnapshotDiffDelegatedClick},{selector:'[data-timer-manual-submit]',handler:handleGlobalTimerManualSubmitClick},{selector:'[data-timer-action-submit]',handler:handleGlobalTimerActionSubmitClick},{selector:'[data-condition-type-suite]',handler:handleGlobalConditionTypeSuiteClick},{selector:'[data-condition-type-option]',handler:handleGlobalConditionTypeOptionClick},{selector:'[data-condition-node-card-click-opens-editor]',handler:handleGlobalConditionCardClick}];
                function handleGlobalCustomComboboxOutsideClick(event){
                  const target=event.target;
                  if(globalEventTargetOutside(target,'.channel-combo'))closeAllCustomComboboxes();
                  const basic=appState.deviceBasicConfigEdit;
                  if(basic&&globalEventTargetOutside(target,'#basic-channel-combo')){basic.channelComboOpen=false;syncDeviceBasicConfigChannelCombo(basic.deviceId);}
                  const extended=appState.deviceExtendedConfigEdit;
                  if(extended&&globalEventTargetOutside(target,'.extended-channel-combo')){Object.keys(extended.channelComboOpen||{}).forEach(field=>extended.channelComboOpen[field]=false);(extended.supportedFields||[]).filter(isExtendedChannelField).forEach(field=>syncDeviceExtendedConfigChannelCombo(extended.deviceId,field));}
                  const listener=appState.signalListenerBasicConfigEdit;
                  if(listener&&globalEventTargetOutside(target,'.listener-channel-combo')){listener.channelComboOpen=false;syncSignalListenerBasicConfigChannelCombo(listener.listenerRef);}
                  const listenerAction=appState.signalListenerActionsEdit?.draft;
                  if(listenerAction&&globalEventTargetOutside(target,'.signal-listener-action-channel-combo')){listenerAction.channelComboOpen=false;syncSignalListenerActionChannelCombo();}
                  const createListener=appState.signalListenerCreate;
                  if(createListener&&globalEventTargetOutside(target,'.listener-create-channel-combo')){createListener.channelComboOpen=false;syncSignalListenerCreateChannelCombo();}
                  const nativeDraft=appState.vbdNativeTriggerEdit;
                  if(nativeDraft&&globalEventTargetOutside(target,'.vbd-native-channel-combo')){Object.keys(nativeDraft.channelComboOpen||{}).forEach(key=>nativeDraft.channelComboOpen[key]=false);Object.keys(nativeDraft.channelComboOpen||{}).forEach(key=>syncVbdNativeTriggerChannelCombo(nativeDraft.deviceId,key));}
                  const actionRelay=appState.actionRelayActionsEdit;
                  if(actionRelay&&globalEventTargetOutside(target,'.action-relay-channel-combo')){Object.keys(actionRelay.channelComboOpen||{}).forEach(index=>actionRelay.channelComboOpen[index]=false);(actionRelay.actions||[]).forEach((_,index)=>syncActionRelayChannelCombo(actionRelay.deviceId,index));}
                  const logicChainChannel=appState.logicChainEditor?.newChannelDraft;
                  if(logicChainChannel&&globalEventTargetOutside(target,'.logic-chain-channel-combo')){logicChainChannel.channelComboOpen=false;syncLogicChainDraftChannelCombo();}
                  const logicChainAppend=appState.logicChainEditor?.actionAppend;
                  if(logicChainAppend&&globalEventTargetOutside(target,'.logic-chain-action-append-channel-combo')){logicChainAppend.channelComboOpen=false;syncLogicChainActionAppendChannelCombo();}
                  const regionDraft=appState.regionControllerEdit?.draft;
                  if(regionDraft&&globalEventTargetOutside(target,'.region-controller-region-combo')){regionDraft.regionComboOpen=false;syncRegionControllerRegionCombo(appState.regionControllerEdit.mode);}
                  if(regionDraft&&globalEventTargetOutside(target,'.region-controller-action-channel-combo')){regionDraft.channelComboOpen=false;syncRegionControllerActionChannelCombo();}
                  if(globalEventTargetOutside(target,'.signal-join-channel-combo'))closeSignalJoinChannelOptions();
                  if(globalEventTargetOutside(target,'.timer-channel-combo'))closeTimerChannelOptions();
                  return false;
                }
                function handleGlobalDeviceMoreOutsideClick(event){if(appState.openDeviceMoreMenuId&&globalEventTargetOutside(event.target,'.wa-device-more-popover,.wa-menu-wrap,[data-device-more-trigger]'))closeDeviceMoreMenu(false);return false;}
                function handleGlobalSelectionComboOutsideClick(event){const selection=appState.selectionCreateVirtualBlock;if(selection&&selection.step==='config'){if(globalEventTargetOutside(event.target,'.selection-player-combo'))selection.playerComboOpen=false;if(globalEventTargetOutside(event.target,'.selection-channel-combo'))selection.channelComboOpen=false;syncSelectionCombos();}return false;}
                const globalClickSideEffectRoutes=[{handler:handleGlobalCustomComboboxOutsideClick},{handler:handleGlobalDeviceMoreOutsideClick},{handler:handleGlobalSelectionComboOutsideClick}];
                function handleGlobalHelpPopoverOutsideClick(event){if(document.querySelector('[data-help-term-popover]')&&globalEventTargetOutside(event.target,'[data-help-term-popover],[data-help-inline-term]'))hideHelpInlineTermPopover();return false;}
                function handleGlobalNavRouteClick(event,nav){const target=event.target, interactive=target.closest('button,a,input,select,textarea,[data-no-nav]');if(!interactive||interactive===nav){event.preventDefault();event.stopPropagation();activateNavRoute(nav);return true;}return false;}
                const globalClickLateRoutes=[{handler:handleHelpCenterDelegatedClick},{handler:handleGlobalHelpPopoverOutsideClick},{selector:'[data-nav-route]',handler:handleGlobalNavRouteClick}];
                function handleGlobalDocumentClick(event){if(dispatchDelegatedEvent(event,globalClickCommandRoutes))return;dispatchDelegatedSideEffects(event,globalClickSideEffectRoutes);dispatchDelegatedEvent(event,globalClickLateRoutes);}
                function handleGlobalHelpPopoverEscape(event){if(event.key==='Escape'&&document.querySelector('[data-help-term-popover]')){event.preventDefault();event.stopPropagation();hideHelpInlineTermPopover();return true;}return false;}
                function handleGlobalDeviceMoreEscape(event){if(event.key==='Escape'&&appState.openDeviceMoreMenuId){event.preventDefault();closeDeviceMoreMenu(false);return true;}return false;}
                function handleGlobalLogicChainEscape(event){return event.key==='Escape'&&clearLogicChainHighlightByEscape(event);}
                function handleGlobalConditionCardKeydown(event,card){if((event.key!=='Enter'&&event.key!==' ')||event.target!==card)return false;event.preventDefault();openConditionNodeEditor(card.dataset.conditionEditPath||'');return true;}
                function handleGlobalNavRouteKeydown(event,nav){if((event.key!=='Enter'&&event.key!==' ')||event.target!==nav||nav.tagName==='BUTTON'||nav.tagName==='A')return false;event.preventDefault();activateNavRoute(nav);return true;}
                const globalPrimaryKeydownRoutes=[{handler:handleGlobalHelpPopoverEscape},{handler:handleGlobalDeviceMoreEscape},{handler:handleGlobalLogicChainEscape},{handler:handleLogicChainEditorDelegatedKeydown},{selector:'[data-condition-node-card-click-opens-editor]',handler:handleGlobalConditionCardKeydown},{selector:'[data-nav-route]',handler:handleGlobalNavRouteKeydown}];
                function handleGlobalPrimaryKeydown(event){dispatchDelegatedEvent(event,globalPrimaryKeydownRoutes);}
                function handleHelpInlineTermOpen(event,term){showHelpInlineTermPopover(term);return true;}
                function handleHelpInlineTermPopoverHover(){helpClearInlineTermCloseTimer();return true;}
                const helpPopoverOpenRoutes=[{selector:'[data-help-inline-term]',handler:handleHelpInlineTermOpen},{selector:'[data-help-term-popover]',handler:handleHelpInlineTermPopoverHover}];
                function handleHelpPopoverOpenEvent(event){dispatchDelegatedSideEffects(event,helpPopoverOpenRoutes);}
                function handleHelpPopoverMouseOut(event){const term=event.target?.closest?.('[data-help-inline-term]');if(!term)return;const next=event.relatedTarget;if(next?.closest?.(`[data-help-term-popover],[data-help-inline-term][data-term-id="${cssEscape(term.dataset.termId||'')}"]`))return;helpScheduleInlineTermPopoverClose(term.dataset.termId||'',140);}
                function handleHelpPopoverFocusOut(event){const term=event.target?.closest?.('[data-help-inline-term]');if(term)helpScheduleInlineTermPopoverClose(term.dataset.termId||'',140);}
                function handleHelpPopoverScroll(event){if(document.querySelector('[data-help-term-popover]')&&event.target?.closest?.('.help-document-scroll,.help-topic-list,.help-right-panel'))hideHelpInlineTermPopover();}
                document.addEventListener('pointerup',handleGlobalPointerUpCapture,true);
                document.addEventListener('click',handleGlobalClickCapture,true);
                document.addEventListener('pointerdown',handleGlobalPointerDownCapture,true);
                document.addEventListener('mouseover',handleGlobalMouseOverCapture,true);
                document.addEventListener('mouseout',handleGlobalMouseOutCapture,true);
                document.addEventListener('click',handleGlobalDocumentClick);
                document.addEventListener('keydown',handleGlobalPrimaryKeydown);
                document.addEventListener('mouseover',handleHelpPopoverOpenEvent);
                document.addEventListener('focusin',handleHelpPopoverOpenEvent);
                document.addEventListener('mouseout',handleHelpPopoverMouseOut);
                document.addEventListener('focusout',handleHelpPopoverFocusOut);
                document.addEventListener('scroll',handleHelpPopoverScroll,true);
                window.addEventListener('beforeunload',event=>{if(logicChainEditorHasUnsavedWork(appState.logicChainEditor)){event.preventDefault();event.returnValue='';}});
                async function renderDashboard(options={}){
                  if(!options.silent)setView(loading('正在加载总览...'));
                  const [status,devices,channels,history,doctor,regions,actions]=await Promise.all([
                    settle('/api/status'),settle('/api/devices'),settle('/api/signals/channels'),settle('/api/signals/history?limit=10'),settle('/api/doctor'),settle('/api/regions'),settle('/api/actions')
                  ]);
                  const deviceList=devices.ok?devices.data:[], channelList=channels.ok?channels.data:[], regionList=regions.ok?regions.data:[], actionList=actions.ok?actions.data:[], hist=history.ok?history.data:[], doc=doctor.ok?doctor.data:{summary:{errorCount:0,warningCount:0,infoCount:0},issues:[]};
                  setView(`
                    <div class="page-head"><div><h1>总览</h1><p>查看服务器、设备、信号与诊断状态</p></div><button class="secondary" onclick="renderDashboard()">刷新</button></div>
                    <section class="card-grid">
                      ${metric('服务器状态',status.ok?labelServerStatus(status.data.server?.status):'加载失败','','dashboard')}
                      ${metric('设备总数',deviceList.length,'','device')}
                      ${metric('信号频道数',channelList.length,'','signal')}
                      ${metric('区域 / 动作',`${regionList.length} / ${actionList.length}`,'','region')}
                      ${metric('诊断错误',doc.summary?.errorCount||0,'error','doctor')}
                      ${metric('诊断警告',doc.summary?.warningCount||0,'warning','warning')}
                      ${metric('当前用户',appState.me?.displayName||appState.me?.username||'-','','user')}
                      ${metric('访问模式',labelAccessMode(appState.status?.webAdmin?.accessMode),'','settings')}
                    </section>
                    <section class="content-grid">
                      <article class="panel-card"><h2>最近信号触发</h2>${history.ok?historyList(hist):errorBlock(history.error.message)}<p class="muted"><button class="link-button" onclick="location.hash='#/history'">查看全部历史</button></p></article>
                      <article class="panel-card"><h2>诊断摘要</h2>${doctor.ok?doctorList(doc.issues||[],5):errorBlock(doctor.error.message)}<p class="muted"><button class="link-button" onclick="location.hash='#/doctor'">查看 Doctor 诊断</button></p></article>
                      <article class="panel-card"><h2>设备概览</h2>${devices.ok?deviceOverview(deviceList):errorBlock(devices.error.message)}</article>
                      <article class="panel-card"><h2>WebAdmin 状态</h2><p class="muted">Dashboard、设备管理、Signal 频道、Doctor 诊断、History 历史、用户管理、系统设置、区域管理和动作系统只读页面已接入。编辑、配置写入、WebSocket 和完整写操作将在后续阶段接入。</p><p><button class="link-button" onclick="location.hash='#/signals'">进入 Signal 管理</button> / <button class="link-button" onclick="location.hash='#/regions'">查看区域</button> / <button class="link-button" onclick="location.hash='#/actions'">查看动作</button> / <button class="link-button" onclick="location.hash='#/doctor'">查看 Doctor</button> / <button class="link-button" onclick="location.hash='#/history'">查看 History</button></p></article>
                    </section>`,options);
                }
                """).toString();
    }
}
