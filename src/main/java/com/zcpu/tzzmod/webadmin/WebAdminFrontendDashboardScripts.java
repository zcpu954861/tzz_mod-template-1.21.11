package com.zcpu.tzzmod.webadmin;

final class WebAdminFrontendDashboardScripts {
    private WebAdminFrontendDashboardScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                function metric(label,value,kind='',iconName=''){return `<article class="metric-card ${kind}"><div class="metric-head"><div class="label">${esc(label)}</div>${iconName?`<span class="metric-icon">${icon(iconName)}</span>`:''}</div><div class="value">${esc(value)}</div></article>`}
                function historyList(items){if(!items||items.length===0)return empty('暂无 Signal 历史记录。');return `<div class="list-stack">${items.map(h=>`<div class="event-row"><strong>${esc(labelChannel(h.channel))}</strong><span class="meta">${fmtTime(h.time)} · ${esc(labelSourceType(h.sourceType))} / ${esc(h.sourceName||'-')} · ${labelStatus(h.result)}</span><span>${esc(h.description||'')}</span></div>`).join('')}</div>`}
                function doctorList(items,limit){if(!items||items.length===0)return empty('当前没有诊断问题。');return `<div class="list-stack">${items.slice(0,limit).map(i=>`<div class="issue-row"><strong>${pill(i.severity)} ${esc(i.title||'诊断问题')}</strong><span class="meta">${esc(issueContext(i))}</span><span>${esc(i.suggestion||i.message||'')}</span></div>`).join('')}</div>`}
                function issueContext(i){if(!i)return '';if(!isBlank(i.relatedObjectName))return i.relatedObjectType==='DEVICE'?`设备：${i.relatedObjectName}`:i.relatedObjectName;if(!isBlank(i.channel))return `频道：${i.channel}`;if(!isBlank(i.relatedObjectId))return `${labelObjectType(i.relatedObjectType)}：${i.relatedObjectId}`;return '';}
                function issueNavigation(i){if(!i)return '<span class="muted">暂无跳转目标</span>';const target=i.navigationTarget||(!isBlank(i.relatedObjectId)&&String(i.relatedObjectType).toUpperCase()==='DEVICE'?`device:${i.relatedObjectId}`:(!isBlank(i.channel)?`channel:${i.channel}`:''));const buttons=[];if(target)buttons.push(navigationButton(target,'查看对象'));if(!isBlank(i.channel))buttons.push(`<button class="link-button" ${navigationAttr(historyHash(i.channel))}>查看历史</button>`);return buttons.length?buttons.join(' / '):'<span class="muted">暂无跳转目标</span>';}
                function issueTitle(i){return esc(i?.title||'诊断问题');}
                function issueMessage(i){return esc(i?.message||i?.impact||'暂无说明');}
                function issueSuggestion(i){return esc(i?.suggestion||'暂无建议');}
                function historyAction(h){const buttons=[];const type=String(h?.sourceType||'').toUpperCase();if(!isBlank(h?.channel))buttons.push(channelButton(h.channel));if(type==='DEVICE'&&!isBlank(h?.sourceId))buttons.push(navigationButton(`device:${h.sourceId}`,'查看设备'));if(type==='REGION'&&!isBlank(h?.sourceId))buttons.push(regionButton(h.sourceId,'查看区域'));if(type==='ACTION'&&!isBlank(h?.sourceId))buttons.push(actionButton(h.sourceId,'查看动作'));return buttons.length?buttons.join(' / '):'<span class="muted">暂无关联对象</span>';}
                function deviceOverview(items){if(!items||items.length===0)return empty('当前暂无设备数据。');const enabled=items.filter(d=>d.enabled).length;const warn=items.filter(d=>['WARNING','ERROR'].includes(String(d.doctorStatus||'').toUpperCase())).length;return `<div class="summary-grid">${metric('启用设备',enabled)}${metric('禁用设备',items.length-enabled)}${metric('诊断警告/错误',warn)}${metric('虚拟方块设备',items.filter(d=>d.type==='VIRTUAL_BLOCK_DEVICE').length)}</div>`}
                async function renderDevices(options={}){
                  if(!options.silent)setView(loading('正在加载设备列表...'));
                  let devices;try{devices=await api('/api/devices')}catch(err){if(options.silent){toast('设备列表实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.devices=devices||[];
                  renderDeviceList('',options);
                }
                function renderDeviceList(focusId,options={}){
                  const devices=appState.devices||[], worlds=[...new Set(devices.map(d=>d.world).filter(Boolean))].sort();
                  const filtered=filterDevices(devices);
                  if(setView(`
                    <div class="page-head"><div><h1>设备管理</h1><p>查看信号设备、虚拟方块设备、动作继电器等状态</p></div></div>
                    <section class="card-grid">${metric('设备总数',devices.length,'','device')}${metric('启用设备',devices.filter(d=>d.enabled).length,'','ok')}${metric('禁用设备',devices.filter(d=>!d.enabled).length,'warning','warning')}${metric('诊断警告/错误',devices.filter(d=>['WARNING','ERROR'].includes(String(d.doctorStatus||'').toUpperCase())).length,'','doctor')}</section>
                    <div class="toolbar">
                      <input class="input" id="device-search" placeholder="搜索设备名称 / id / channel / 坐标" value="${esc(appState.deviceFilters.search)}">
                      ${filterSelect('设备类型','device-type',['ALL','SIGNAL_EMITTER','SIGNAL_RECEIVER','ACTION_RELAY','VIRTUAL_BLOCK_DEVICE','UNKNOWN'],appState.deviceFilters.type)}
                      ${filterSelect('启用状态','device-enabled',['ALL','ENABLED','DISABLED'],appState.deviceFilters.enabled)}
                      ${filterSelect('诊断状态','device-doctor',['ALL','OK','INFO','WARNING','ERROR','UNKNOWN'],appState.deviceFilters.doctor)}
                      ${filterSelect('世界/维度','device-world',['ALL',...worlds],appState.deviceFilters.world)}
                    </div>
                    ${filtered.length===0?(devices.length===0?empty('当前暂无设备数据。请在游戏内创建或绑定设备后刷新页面。'):empty('没有匹配当前筛选条件的设备。')):deviceTable(filtered)}
                  `,options))bindDeviceFilters(focusId);
                }
                function filterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span>${select(id,options,value)}</label>`}
                function select(id,options,value){return `<select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(optionLabel(o))}</option>`).join('')}</select>`}
                function optionLabel(v){return {ALL:'全部',ENABLED:'已启用',DISABLED:'已禁用',SIGNAL_EMITTER:'信号发射器',SIGNAL_RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',VIRTUAL_BLOCK_DEVICE:'虚拟方块设备',UNKNOWN:'未知',OK:'正常',INFO:'信息',WARNING:'警告',ERROR:'错误'}[v]||v;}
                function bindDeviceFilters(focusId){
                  const update=(event)=>{appState.deviceFilters.search=document.getElementById('device-search').value;appState.deviceFilters.type=document.getElementById('device-type').value;appState.deviceFilters.enabled=document.getElementById('device-enabled').value;appState.deviceFilters.doctor=document.getElementById('device-doctor').value;appState.deviceFilters.world=document.getElementById('device-world').value;renderDeviceList(event.target.id);};
                  ['device-search','device-type','device-enabled','device-doctor','device-world'].forEach(id=>document.getElementById(id).addEventListener(id==='device-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterDevices(items){const f=appState.deviceFilters;return items.filter(d=>{const hay=[d.id,d.displayName,d.channel,d.world,posText(d.pos),d.type].join(' ').toLowerCase();if(f.search&& !hay.includes(f.search.toLowerCase()))return false;if(f.type!=='ALL'&&d.type!==f.type)return false;if(f.enabled==='ENABLED'&&!d.enabled)return false;if(f.enabled==='DISABLED'&&d.enabled)return false;if(f.doctor!=='ALL'&&String(d.doctorStatus||'UNKNOWN').toUpperCase()!==f.doctor)return false;if(f.world!=='ALL'&&d.world!==f.world)return false;return true;});}
                function deviceTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>设备</th><th>类型</th><th>世界/维度</th><th>坐标</th><th>主频道</th><th>状态</th><th>最近触发</th><th>诊断</th><th>操作</th></tr></thead><tbody>${items.map(d=>{const target=deviceHash(d);return `<tr ${navigationAttr(target,false)}><td><span class="device-name"><span class="device-icon">${deviceMetadataIcon(d)}</span><span><strong>${esc(d.displayName)}</strong>${deviceSubtitle(d)}</span></span></td><td>${esc(labelType(d.type))}</td><td>${esc(d.world||'-')}</td><td>${esc(posText(d.pos))}</td><td>${channelCell(d.channel)}</td><td>${pill(d.enabled?'OK':'WARNING')} ${esc(labelBool(d.enabled))}</td><td>${fmtTime(d.lastTriggeredAt)}</td><td>${pill(d.doctorStatus)}</td><td><button class="text-button" ${navigationAttr(target)}>查看详情</button></td></tr>`;}).join('')}</tbody></table></div>`}
                function deviceSubtitle(d){const id=shortId(d.id);if(!isBlank(id)&&!String(id).toLowerCase().startsWith('minecraf'))return `<span class="device-subtitle">ID：${esc(id)}</span>`;if(!isBlank(d.world))return `<span class="device-subtitle">维度：${esc(d.world)}</span>`;return '';}
                function channelCell(channel){return channelButton(channel);}
                async function renderDeviceDetail(id,options={}){
                  if(!options.silent)setView(loading('正在加载设备详情...'));
                  const routeInfo=detailRoute(id,'#/devices'), lookupId=deviceApiRef(routeInfo.id), encoded=encodeURIComponent(lookupId);
                  let detail;try{detail=await api(`/api/devices/${encoded}`);const expectedType=deviceTypeRefPrefix(routeInfo.id);if(expectedType&&String(detail.type||'').toLowerCase()!==expectedType){const mismatch=new Error('该位置当前设备类型已变化，目标类型不存在。');mismatch.status=404;throw mismatch;}}catch(err){if(options.silent){toast('设备详情实时刷新失败，已保留当前页面。');return;}setView(`<section class="wa-page"><div class="back-row">${backButton(routeInfo,'返回设备管理')}</div>${waPageHead('详情暂不可用','当前只读接口尚未提供该设备详情或设备已被删除。',waButton('返回列表','device',navigationAttr('#/devices'),'ghost'))}${err.status===404?empty('设备不存在或已被删除。'):errorBlock(err.message)}</section>`);return;}
                  if(!routeInfo.returnTo&&isVirtualBlockDevice(detail))routeInfo.fallback='#/virtual-block-devices';
                  const canonicalEncoded=encodeURIComponent(detail.id||lookupId);
                  const vbdDetail=isVirtualBlockDevice(detail);
                  const [debug,history,doctor,lockStatus,basicConfig,extendedConfig,actionRelayActions,interactionItemMatcher,nativeTriggers]=await Promise.all([settle(`/api/devices/${canonicalEncoded}/debug`),detail.channel?settle(`/api/signals/history?channel=${encodeURIComponent(detail.channel)}&limit=10`):Promise.resolve({ok:true,data:[]}),settle('/api/doctor'),settle(`/api/webadmin/edit-locks/status?targetType=device_metadata&targetId=${canonicalEncoded}`),settle(`/api/webadmin/device-basic-config/${canonicalEncoded}`),vbdDetail?Promise.resolve({ok:true,data:null}):settle(`/api/webadmin/device-extended-config/${canonicalEncoded}`),isActionRelay(detail)?settle(`/api/webadmin/action-relay-actions/${canonicalEncoded}`):Promise.resolve({ok:true,data:null}),vbdDetail?settle(`/api/webadmin/interaction-item-matcher/${canonicalEncoded}`):Promise.resolve({ok:true,data:null}),vbdDetail?settle(`/api/webadmin/virtual-block-devices/${canonicalEncoded}/native-triggers`):Promise.resolve({ok:true,data:null})]);
                  detail.metadataLock=lockStatus.ok?lockStatus.data:null;
                  detail.basicConfig=basicConfig.ok?basicConfig.data:null;
                  detail.basicConfigError=basicConfig.ok?null:basicConfig.error;
                  detail.extendedConfig=extendedConfig.ok?extendedConfig.data:null;
                  detail.extendedConfigError=extendedConfig.ok?null:extendedConfig.error;
                  detail.actionRelayActions=actionRelayActions.ok?actionRelayActions.data:null;
                  detail.actionRelayActionsError=actionRelayActions.ok?null:actionRelayActions.error;
                  detail.interactionItemMatcher=interactionItemMatcher.ok?interactionItemMatcher.data:null;
                  detail.interactionItemMatcherError=interactionItemMatcher.ok?null:interactionItemMatcher.error;
                  detail.nativeTriggers=nativeTriggers.ok?nativeTriggers.data:null;
                  detail.nativeTriggersError=nativeTriggers.ok?null:nativeTriggers.error;
                  appState.currentDeviceDetail=detail;
                  const relatedDoctor=[...(detail.doctorIssues||[])];
                  if(doctor.ok){relatedDoctor.push(...(doctor.data.issues||[]).filter(i=>i.relatedObjectId===detail.id||(!isBlank(detail.channel)&&i.channel===detail.channel)));}
                  const configAction=deviceConfigEditButton(detail,'编辑设备配置','primary');
                  const actionLock=detail.actionRelayActions?.lockStatus||null, actionLockedByOther=actionRelayLockHeldByOther(actionLock);
                  const actionListAction=isActionRelay(detail)?waButton(actionLockedByOther?'只读查看 Action 列表':((detail.actionRelayActions&&detail.actionRelayActions.actionsEditable===false)?'查看 Action 状态':(canEditActionRelayActions()?'编辑 Action 列表':'查看 Action 列表')),'action-relay',actionLockedByOther?htmlHandler(`openActionRelayActionsReadonlyModal(${jsString(detail.id)})`):htmlHandler(`openActionRelayActionsModal(${jsString(detail.id)})`),(canEditActionRelayActions()&&!actionLockedByOther&&!(detail.actionRelayActions&&detail.actionRelayActions.actionsEditable===false))?'primary':'ghost'):'';
                  const deleteAction=isVirtualBlockDevice(detail)?(canDeleteVirtualBlockDevice()?waButton('删除 / 解绑','channel-error',htmlHandler(`openVirtualBlockDeviceDeleteModal(${jsString(detail.id)})`),'danger'):waButton('删除 / 解绑','channel-error','disabled','danger')):'';
                  const quickNote=isVirtualBlockDevice(detail)?'删除 / 解绑只移除 virtual_block_device 配置，不破坏世界方块；导出和其它写操作仍保持禁用。':(isPhysicalSignalDevice(detail)?'这是已放置的真实方块设备。WebAdmin 只编辑安全配置，不创建、不删除、不 setblock；删除请在游戏内破坏方块。':'仅设备显示信息、基础配置和安全扩展配置可写；删除、导出和其它写操作没有完整后端支持，保持禁用。');
                  const statusValue=detail.enabled?'启用':'停用';
                  const doctorStatus=detail.doctorStatus||detail.debugSummary?.status||'UNKNOWN';
                  const recentEvents=history.ok?(history.data||[]):[];
                  const advancedRows=[
                    ['device.id',detail.id],
                    ['device.type',detail.type],
                    ['device.world',detail.world],
                    ['device.pos',posText(detail.pos)],
                    ['device.channel',detail.channel],
                    ['device.enabled',statusValue],
                    ['device.lastTriggeredAt',formatDateTime(detail.lastTriggeredAt)],
                    ['doctor.status',doctorStatus]
                  ];
                  setView(`<section class="wa-page wa-detail-shell" data-detail-kind="device">
                    ${detailHeader({back:backButton(routeInfo,'返回设备管理'),kicker:'设备详情',iconName:deviceTypeIcon(detail.type),title:detail.displayName||detail.id,subtitle:`${detail.world||'暂无世界'} · ${posText(detail.pos)} · ${labelChannel(detail.channel)}`,copyValue:detail.id,badges:[`<span class="pill">${esc(labelType(detail.type))}</span>`,pill(detail.enabled?'OK':'WARNING'),pill(doctorStatus)],actions:[detail.channel?waButton('查看逻辑链','action-binding',navigationAttr(logicChainResolveHash('device',detail.id)),'ghost'):'',actionListAction,configAction,waButton('导出设备配置','download','disabled','ghost'),waButton('更多','more','disabled','ghost')].filter(Boolean)})}
                    ${detailTabs(['基本信息','配置','最近事件','关联对象','Doctor'])}
                    <section class="wa-detail-first-row">
                      ${detailCard('基本信息',detailInfoGrid([
                        ['设备名称',detail.displayName||detail.id],
                        ['设备 ID',detail.id],
                        ['类型',labelType(detail.type)],
                        ['状态',safeHtml(pill(detail.enabled?'OK':'WARNING')+' '+esc(labelBool(detail.enabled)))],
                        ['世界/维度',detail.world||'暂无'],
                        ['坐标',posText(detail.pos)],
                        ['主频道',safeHtml(channelCell(detail.channel))],
                        ['最近触发',safeHtml(fmtTime(detail.lastTriggeredAt))]
                      ]),configAction)}
                      ${detailCard('状态与统计',`${detailStatGrid([
                        {label:'启用状态',value:statusValue,sub:'基础配置',icon:'enabled',kind:detail.enabled?'ok':'warning'},
                        {label:'最近触发',value:formatDateTime(detail.lastTriggeredAt),sub:'运行状态',icon:'recent-event'},
                        {label:'Doctor',value:labelStatus(doctorStatus),sub:'诊断摘要',icon:'doctor-overview',kind:String(doctorStatus).toUpperCase()==='OK'?'ok':'warning'},
                        {label:'历史事件',value:recentEvents.length,sub:'当前频道缓存',icon:'history'}
                      ])}${detailConsumerGrid([
                        {label:'关联频道',value:labelChannel(detail.channel),icon:'active-channel',target:detail.channel?signalHash(detail.channel):''},
                        {label:'Doctor 诊断',value:uniqueIssues(relatedDoctor).length,icon:'doctor-overview',target:'#/doctor'},
                        {label:'历史记录',value:recentEvents.length,icon:'history',target:detail.channel?historyHash(detail.channel):''},
                        {label:'设备列表',value:'返回',icon:'device',target:'#/devices'}
                      ])}`)}
                    </section>
                    ${detailFixedLayout([
                      detailCard('配置摘要',deviceConfigOverview(detail),'','detail-card-stretchable'),
                      isActionRelay(detail)?detailCard('Action 列表',actionRelayActionListReadonlyCard(detail),actionListAction):'',
                      detailCard('最近事件',`${history.ok?compactEventList(recentEvents,'当前设备暂无关联频道历史。'):errorBlock(history.error.message)}<p class="muted">${isBlank(detail.channel)?'当前设备暂无关联频道历史。':`<button class="link-button" ${navigationAttr(historyHash(detail.channel),false)}>查看相关历史</button>`}</p>`)
                    ],[
                      isVirtualBlockDevice(detail)?detailCard('原生触发配置',`<div data-vbd-native-trigger-side-card="true" data-detail-side-card="vbd-native-triggers">${vbdNativeTriggerOverviewCard(detail)}</div>`,'','detail-card-stretchable'):'',
                      detailCard('关联对象 / Doctor',`${deviceChannelSideCard(detail)}${deviceDoctorSideCard(detail,uniqueIssues(relatedDoctor))}`,'','detail-card-stretchable'),
                      detailCard('快捷操作',`<div class="wa-quick-grid">${actionListAction}${configAction}${waButton('查看逻辑链','action-binding',detail.channel?navigationAttr(logicChainResolveHash('device',detail.id)):'disabled','ghost')}${waButton('打开频道','active-channel',detail.channel?navigationAttr(signalHash(detail.channel)):'disabled','ghost')}${waButton('查看历史','history',detail.channel?navigationAttr(historyHash(detail.channel)):'disabled','ghost')}${deleteAction}</div><p class="wa-disabled-note">${esc(quickNote)}</p>`)
                    ],[
                      advancedDetailCard('devices',detail.id,advancedRows,[
                      {title:'配置摘要完整字段',rows:advancedRowsFromObject(detail.configSummary||{},'configSummary')},
                      {title:'基础配置',rows:advancedRowsFromObject(detail.basicConfig||{},'basicConfig')},
                      {title:'扩展配置',rows:advancedRowsFromObject(detail.extendedConfig||{},'extendedConfig')},
                      {title:'7.9 原生触发只读摘要',rows:advancedRowsFromObject(detail.nativeTriggers||{},'nativeTriggers')},
                      {title:'运行与调试',rows:advancedRowsFromObject({metadata:detail.metadata,debug:debug.ok?debug.data:null,debugSummary:detail.debugSummary,doctorIssues:uniqueIssues(relatedDoctor),lastResult:detail.lastResult},'runtime')}
                    ])
                    ])}
                  </section>`,options);
                }
                function canEditDeviceMetadata(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canEditDeviceBasicConfig(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canEditDeviceExtendedConfig(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canEditChannelMetadata(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canEditSignalListenerBasicConfig(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canStartObjectSelection(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function roleCanWriteLifecycle(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function operationAllowed(operation){const found=(appState.capabilities?.operations||[]).find(entry=>String(entry.operation||'')===operation);return found?!!found.allowed:roleCanWriteLifecycle();}
                function canDeleteVirtualBlockDevice(){const flag=appState.capabilities?.virtualBlockDeviceLifecycleEnabled;return flag!==false&&operationAllowed('DELETE_VIRTUAL_BLOCK_DEVICE');}
                function canWriteSignalListenerLifecycle(){const flag=appState.capabilities?.signalListenerLifecycleWriteEnabled;return flag!==false&&(operationAllowed('CREATE_SIGNAL_LISTENER')||operationAllowed('DELETE_SIGNAL_LISTENER'));}
                function canCreateSignalListener(){const flag=appState.capabilities?.signalListenerLifecycleWriteEnabled;return flag!==false&&operationAllowed('CREATE_SIGNAL_LISTENER');}
                function canDeleteSignalListener(){const flag=appState.capabilities?.signalListenerLifecycleWriteEnabled;return flag!==false&&operationAllowed('DELETE_SIGNAL_LISTENER');}
                function canEditSignalListenerActions(){const flag=appState.capabilities?.signalListenerActionListWriteEnabled;return flag!==false&&operationAllowed('EDIT_SIGNAL_LISTENER_ACTIONS');}
                function canEditActionRelayActions(){const flag=appState.capabilities?.actionRelayActionListWriteEnabled;return flag!==false&&operationAllowed('EDIT_ACTION_RELAY_ACTIONS');}
                function canEditVbdNativeTriggers(){const flag=appState.capabilities?.vbdNativeTriggerWriteEnabled;return flag!==false&&operationAllowed('EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS');}
                function canEditInteractionItemMatcher(){const flag=appState.capabilities?.interactionItemMatcherWriteEnabled;return flag!==false&&operationAllowed('EDIT_ITEM_MATCHER');}
                function canEditSingleItemSubmit(){const flag=appState.capabilities?.singleItemSubmitTemplateWriteEnabled;return flag!==false&&operationAllowed('START_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION');}
                function canEditRegionController(){const flag=appState.capabilities?.regionControllerWriteEnabled;return flag!==false&&operationAllowed('EDIT_REGION');}
                function canEditLogicChainMetadata(){return operationAllowed('EDIT_LOGIC_CHAIN_METADATA');}
                function canEditLogicChainEditor(){return operationAllowed('EDIT_LOGIC_CHAIN');}
                function canEditSignalJoin(){const flag=appState.capabilities?.signalJoinWriteEnabled;return flag!==false&&operationAllowed('EDIT_SIGNAL_JOIN');}
                function canEditTimer(){const flag=appState.capabilities?.timerWriteEnabled;return flag!==false&&operationAllowed('EDIT_TIMER');}
                function canEditStateVariableDefinitions(){return operationAllowed('EDIT_STATE_VARIABLE');}
                function canImportTemplate(){const flag=appState.capabilities?.templatePrefabWriteEnabled;return flag!==false&&operationAllowed('IMPORT_TEMPLATE');}
                function canApplyTemplate(){const flag=appState.capabilities?.templatePrefabWriteEnabled;return flag!==false&&operationAllowed('APPLY_TEMPLATE');}
                function isActionRelay(d){return String(d?.type||d?.deviceType||'').toUpperCase()==='ACTION_RELAY';}
                function isPhysicalSignalDevice(d){return ['SIGNAL_EMITTER','SIGNAL_RECEIVER','ACTION_RELAY'].includes(String(d?.type||d?.deviceType||'').toUpperCase());}
                function csrfToken(){return appState.capabilities?.csrf?.token || '';}
                function metadataIconOptions(){return ['auto','signal_emitter','signal_receiver','action_relay','virtual_block_device','region','action','warning','key','chest','door','signal','custom_1','condition_group','condition_debugger','runtime_gate','signal_join','signal_barrier','signal_aggregator','timer','scheduler','state_variable','logic_chain'];}
                function labelMetadataIcon(value){return {auto:'自动图标',signal_emitter:'信号发射器',signal_receiver:'信号接收器',action_relay:'动作继电器',virtual_block_device:'虚拟方块设备',region:'区域',action:'动作',warning:'警告',key:'钥匙',chest:'箱子',door:'门',signal:'Signal',custom_1:'自定义 1',condition_group:'条件组',condition_debugger:'条件调试',runtime_gate:'Runtime Gate',signal_join:'信号汇合',signal_barrier:'信号 Barrier',signal_aggregator:'信号 Aggregator',timer:'计时器',scheduler:'调度器',state_variable:'状态变量',logic_chain:'逻辑链'}[String(value||'auto')]||value;}
                """)
.append("""
                function selectionModalDefaultDraft(){
                  return {step:'config',status:'draft',purpose:'create_virtual_block_device',targetPlayerName:'',channel:'',displayName:'',note:'',iconKey:'auto',enabled:true,selectionId:'',deviceId:'',routeTarget:'',returnTo:'#/virtual-block-devices',message:'',errors:[],saving:false,playerOptions:[],playerOptionsError:null,playerComboOpen:false,playerComboIndex:0,channelOptions:[],channelOptionsError:null,channelComboOpen:false,channelComboIndex:0,channelComboQuery:'',channelComboSearchActive:false,terminalStatus:'',terminalEventId:''};
                }
                function selectionSourceReturnTo(){
                  const hash=currentRouteHash();
                  if(hash==='#/virtual-block-devices'||hash==='#/block-devices'||hash==='#/devices')return hash;
                  return '#/virtual-block-devices';
                }
                function selectionDeviceDetailRoute(deviceId,returnTo){
                  if(isBlank(deviceId))return '';
                  const safeReturn=isValidReturnHash(returnTo)?returnTo:'#/virtual-block-devices';
                  return `${deviceHash(deviceId)}?returnTo=${encodeURIComponent(safeReturn)}`;
                }
                async function openCreateVirtualBlockDeviceModal(){
                  waEnsureState();
                  if(!canStartObjectSelection()){toast('需要 EDITOR 或 OWNER 权限才能新建虚拟方块设备。');return;}
                  appState.selectionCreateVirtualBlock={...selectionModalDefaultDraft(),returnTo:selectionSourceReturnTo()};
                  markModalInitialSnapshot('selection_create_virtual_block',appState.selectionCreateVirtualBlock);
                  showCreateVirtualBlockDeviceModal();
                  const [players,channels]=await Promise.all([loadOnlinePlayerOptions(),loadSignalChannelOptions()]);
                  const draft=appState.selectionCreateVirtualBlock;
                  if(draft&&draft.step==='config'){
                    appState.selectionCreateVirtualBlock={...draft,playerOptions:players,playerOptionsError:appState.onlinePlayerOptionsError,channelOptions:channels,channelOptionsError:appState.channelOptionsError};
                    showCreateVirtualBlockDeviceModal();
                  }
                }
                function selectionDraftFromForm(){
                  const draft={...(appState.selectionCreateVirtualBlock||selectionModalDefaultDraft())};
                  const targetInput=document.getElementById('selection-target-player'), channelInput=document.getElementById('selection-channel');
                  draft.targetPlayerName=targetInput?targetInput.value.trim():(draft.targetPlayerName||'');
                  draft.channel=channelInput?channelInput.value.trim():(draft.channel||'');
                  draft.displayName=document.getElementById('selection-display-name')?.value?.trim()||draft.displayName||'';
                  draft.note=document.getElementById('selection-note')?.value||draft.note||'';
                  draft.iconKey=document.getElementById('selection-icon-key')?.value||draft.iconKey||'auto';
                  const enabled=document.getElementById('selection-enabled');
                  draft.enabled=enabled?!!enabled.checked:draft.enabled!==false;
                  return draft;
                }
                function selectionPlayerOptionsHtml(draft){
                  if(draft.playerOptionsError||appState.onlinePlayerOptionsError)return '<div class="channel-combo-empty">在线玩家候选加载失败，仍可手动输入玩家名。</div>';
                  const options=filteredOnlinePlayerOptions(draft.playerOptions||appState.onlinePlayerOptions||[],draft.targetPlayerName), current=String(draft.targetPlayerName||'').trim().toLowerCase(), active=Math.max(0,Number(draft.playerComboIndex||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的在线玩家，可继续手动输入。</div>';
                  return options.map((p,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(p.name||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" ${htmlHandler(`selectSelectionTargetPlayer(${jsString(p.name||'')})`)}><strong>${esc(p.name||'未命名玩家')}</strong><span>UUID：${esc(p.uuid||'-')}</span></button>`).join('');
                }
                function renderSelectionPlayerCombo(draft){
                  const open=draft.playerComboOpen?' open':'';
                  return `<div id="selection-target-player-combo" class="channel-combo selection-player-combo${open}" data-selection-player-combo="true"><div class="channel-combo-control"><input id="selection-target-player" class="input" maxlength="64" value="${esc(draft.targetPlayerName||'')}" placeholder="选择在线玩家或输入玩家名" autocomplete="off" role="combobox" aria-expanded="${draft.playerComboOpen?'true':'false'}" aria-controls="selection-target-player-menu" onfocus='openSelectionTargetPlayerMenu()' oninput='updateSelectionCombosFromForm("player")' onkeydown='handleSelectionTargetPlayerKey(event)'><button class="channel-combo-toggle" type="button" onclick='toggleSelectionTargetPlayerMenu()' aria-label="显示在线玩家">${icon('chevron-down')}</button></div><div id="selection-target-player-menu" class="channel-combo-menu" role="listbox">${selectionPlayerOptionsHtml(draft)}</div></div>`;
                }
                function selectionChannelOptionsHtml(draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft)), current=normalizeChannelName(draft.channel).toLowerCase(), active=Math.max(0,Number(draft.channelComboIndex||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接使用新频道。</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" ${htmlHandler(`selectSelectionChannel(${jsString(c.channel||'')})`)}><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function renderSelectionChannelCombo(draft){
                  const open=draft.channelComboOpen?' open':'';
                  return `<div id="selection-channel-combo" class="channel-combo selection-channel-combo${open}" data-selection-channel-combo="true"><div class="channel-combo-control"><input id="selection-channel" class="input" maxlength="128" value="${esc(draft.channel||'')}" placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${draft.channelComboOpen?'true':'false'}" aria-controls="selection-channel-menu" onfocus='openSelectionChannelMenu()' oninput='updateSelectionCombosFromForm("channel")' onkeydown='handleSelectionChannelKey(event)'><button class="channel-combo-toggle" type="button" onclick='toggleSelectionChannelMenu()' aria-label="显示已有频道">${icon('chevron-down')}</button></div><div id="selection-channel-menu" class="channel-combo-menu" role="listbox">${selectionChannelOptionsHtml(draft)}</div></div>`;
                }
                function updateSelectionCombosFromForm(openTarget=''){
                  const draft=appState.selectionCreateVirtualBlock;if(!draft||draft.step!=='config')return;
                  draft.targetPlayerName=document.getElementById('selection-target-player')?.value||'';
                  draft.channel=document.getElementById('selection-channel')?.value||'';
                  if(openTarget==='player'){draft.playerComboOpen=true;draft.playerComboIndex=0;}
                  if(openTarget==='channel'){draft.channelComboOpen=true;draft.channelComboIndex=0;setChannelComboQuery(draft,draft.channel);}
                  const hint=document.getElementById('selection-channel-hint');
                  if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);
                  syncSelectionCombos();
                }
                function syncSelectionCombos(){
                  const draft=appState.selectionCreateVirtualBlock;if(!draft)return;
                  const playerCombo=document.getElementById('selection-target-player-combo'), playerMenu=document.getElementById('selection-target-player-menu'), playerInput=document.getElementById('selection-target-player');
                  if(playerCombo)playerCombo.classList.toggle('open',!!draft.playerComboOpen);
                  if(playerInput)playerInput.setAttribute('aria-expanded',draft.playerComboOpen?'true':'false');
                  if(playerMenu)playerMenu.innerHTML=selectionPlayerOptionsHtml(draft);
                  const channelCombo=document.getElementById('selection-channel-combo'), channelMenu=document.getElementById('selection-channel-menu'), channelInput=document.getElementById('selection-channel');
                  if(channelCombo)channelCombo.classList.toggle('open',!!draft.channelComboOpen);
                  if(channelInput)channelInput.setAttribute('aria-expanded',draft.channelComboOpen?'true':'false');
                  if(channelMenu)channelMenu.innerHTML=selectionChannelOptionsHtml(draft);
                }
                function openSelectionTargetPlayerMenu(){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;updateSelectionCombosFromForm('');closeAllCustomComboboxes();draft.playerComboOpen=true;draft.channelComboOpen=false;syncSelectionCombos();}
                function toggleSelectionTargetPlayerMenu(){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;updateSelectionCombosFromForm('');const wasOpen=!!draft.playerComboOpen;if(wasOpen){draft.playerComboOpen=false;syncSelectionCombos();return;}closeAllCustomComboboxes();draft.playerComboOpen=true;draft.channelComboOpen=false;syncSelectionCombos();document.getElementById('selection-target-player')?.focus();}
                function selectSelectionTargetPlayer(playerName){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;draft.targetPlayerName=playerName||'';draft.playerComboOpen=false;draft.playerComboIndex=0;const input=document.getElementById('selection-target-player');if(input)input.value=draft.targetPlayerName;syncSelectionCombos();}
                function handleSelectionTargetPlayerKey(event){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;const options=filteredOnlinePlayerOptions(draft.playerOptions||appState.onlinePlayerOptions||[],document.getElementById('selection-target-player')?.value||draft.targetPlayerName);if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.playerComboOpen=false;syncSelectionCombos();return;}if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.playerComboOpen=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.playerComboIndex||0)+1:Number(draft.playerComboIndex||0)-1;draft.playerComboIndex=Math.min(max,Math.max(0,next));syncSelectionCombos();return;}if(event.key==='Enter'&&draft.playerComboOpen&&options.length>0){event.preventDefault();selectSelectionTargetPlayer(options[Math.min(options.length-1,Number(draft.playerComboIndex||0))].name);return;}}
                function openSelectionChannelMenu(){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;updateSelectionCombosFromForm('');closeAllCustomComboboxes();draft.channelComboOpen=true;draft.playerComboOpen=false;resetChannelComboQuery(draft);syncSelectionCombos();}
                function toggleSelectionChannelMenu(){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;updateSelectionCombosFromForm('');const wasOpen=!!draft.channelComboOpen;if(wasOpen){draft.channelComboOpen=false;syncSelectionCombos();return;}closeAllCustomComboboxes();draft.channelComboOpen=true;draft.playerComboOpen=false;resetChannelComboQuery(draft);syncSelectionCombos();document.getElementById('selection-channel')?.focus();}
                function selectSelectionChannel(channel){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;draft.channel=channel||'';draft.channelComboOpen=false;draft.channelComboIndex=0;resetChannelComboQuery(draft);const input=document.getElementById('selection-channel'), hint=document.getElementById('selection-channel-hint');if(input)input.value=draft.channel;if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncSelectionCombos();}
                function handleSelectionChannelKey(event){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft));if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.channelComboOpen=false;syncSelectionCombos();return;}if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex||0)+1:Number(draft.channelComboIndex||0)-1;draft.channelComboIndex=Math.min(max,Math.max(0,next));syncSelectionCombos();return;}if(event.key==='Enter'&&draft.channelComboOpen&&options.length>0){event.preventDefault();selectSelectionChannel(options[Math.min(options.length-1,Number(draft.channelComboIndex||0))].channel);return;}}
                function selectionErrorsHtml(draft){
                  const errors=(draft?.errors||[]).filter(Boolean);
                  if(!errors.length)return '';
                  return `<ul class="validation-list">${errors.map(err=>`<li>${esc(err.field?`${err.field}：`: '')}${esc(err.message||err||'选择请求失败')}</li>`).join('')}</ul>`;
                }
                function selectionStatusTone(status){
                  const value=String(status||'').toLowerCase();
                  if(value==='completed')return 'ok';
                  if(value==='failed')return 'error';
                  if(value==='cancelled')return 'warning';
                  return 'info';
                }
                function selectionModalBody(draft){
                  const d=draft||selectionModalDefaultDraft(), errors=selectionErrorsHtml(d);
                  if(d.step==='waiting'){
                    return `<div class="wa-selection-wizard" data-selection-wizard="virtual_block_device"><div class="wa-selection-status info"><strong>等待玩家在游戏内右键方块</strong><span>目标玩家：${esc(d.targetPlayerName||'-')} · Session：${esc(d.selectionId||'-')}</span><span>客户端会显示“选择虚拟方块设备目标 / 右键方块确认 / ESC 取消”。选择模式不会影响移动和视角。</span></div>${errors}</div>`;
                  }
                  if(d.step==='completed'){
                    return `<div class="wa-selection-wizard" data-selection-wizard="virtual_block_device"><div class="wa-selection-status ok"><strong>选择完成</strong><span>${esc(d.message||'虚拟方块设备已创建。')}</span>${d.deviceId?`<span>设备 ID：${esc(d.deviceId)}</span>`:''}<span>正在进入设备详情...</span></div></div>`;
                  }
                  if(d.step==='cancelled'||d.step==='failed'){
                    return `<div class="wa-selection-wizard" data-selection-wizard="virtual_block_device"><div class="wa-selection-status ${selectionStatusTone(d.step)}"><strong>${d.step==='failed'?'选择失败':'选择已取消'}</strong><span>${esc(d.message||'本次选择未创建设备。')}</span></div>${errors}</div>`;
                  }
                  return `<form class="edit-form wa-selection-form" data-selection-wizard="virtual_block_device" ${htmlEvent('onsubmit','event.preventDefault();startCreateVirtualBlockDeviceSelection()')}>
                    <div class="wa-selection-grid">
                      <label>目标在线玩家${renderSelectionPlayerCombo(d)}<span class="muted">可选择当前在线玩家，也可手动输入玩家名。</span></label>
                      <label>Channel${renderSelectionChannelCombo(d)}<span id="selection-channel-hint" class="muted">${channelHintHtml(d.channel,d.channelOptions||appState.channelOptions||[],d.channelOptionsError||appState.channelOptionsError)}</span></label>
                      <label>显示名<input id="selection-display-name" class="input" maxlength="64" value="${esc(d.displayName||'')}" placeholder="可选，仅用于 WebAdmin 展示"></label>
                      <label>图标<select id="selection-icon-key" class="select">${metadataIconOptions().map(k=>`<option value="${esc(k)}" ${k===(d.iconKey||'auto')?'selected':''}>${esc(labelMetadataIcon(k))}</option>`).join('')}</select></label>
                    </div>
                    <label>备注<textarea id="selection-note" maxlength="512" placeholder="可选，仅用于 WebAdmin 展示">${esc(d.note||'')}</textarea></label>
                    <label class="check-row"><input id="selection-enabled" type="checkbox" ${d.enabled!==false?'checked':''}><span>创建后启用设备</span></label>
                    <p class="readonly-note">开始后目标玩家客户端进入选择模式。右键任意方块都会作为目标选择，并阻断原方块交互、手持物品使用和 GUI 打开。</p>
                    ${errors}
                  </form>`;
                }
                function selectionModalFooter(draft){
                  const d=draft||selectionModalDefaultDraft();
                  if(d.step==='waiting')return `${waButton('取消选择','close',d.saving?'disabled':htmlHandler('cancelCreateVirtualBlockDeviceSelection()'),'danger')}${waButton('等待玩家选择','virtual-block-device','disabled','primary')}`;
                  if(d.step==='completed')return `${waButton('正在进入详情','eye','disabled','primary')}`;
                  if(d.step==='cancelled'||d.step==='failed')return `${waButton('返回配置','settings',htmlHandler('resetCreateVirtualBlockDeviceModalToConfig()'),'ghost')}${waButton('关闭','close',htmlHandler('closeWebAdminModal()'),'primary')}`;
                  return `${waButton('取消','close',htmlHandler('closeWebAdminModal()'),'ghost')}${waButton(d.saving?'开始中...':'开始选择','virtual-block-device',d.saving?'disabled':htmlHandler('startCreateVirtualBlockDeviceSelection()'),'primary')}`;
                }
                function showCreateVirtualBlockDeviceModal(){
                  const draft=appState.selectionCreateVirtualBlock||selectionModalDefaultDraft();
                  if(draft.step==='config')markModalInitialSnapshot('selection_create_virtual_block',draft);
                  openWebAdminModal('新建虚拟方块设备',selectionModalBody(draft),selectionModalFooter(draft),{className:'wa-selection-modal',onClose:()=>closeCreateVirtualBlockDeviceModal(true),syncBeforeClose:()=>syncModalDraftBeforeClose('selection_create_virtual_block'),dirtyCheck:()=>{const d=appState.selectionCreateVirtualBlock;return !!d&&d.step==='config'&&modalDraftDirty('selection_create_virtual_block',d);}});
                }
                function writeSelectionData(result){return result?.data?.selection||result?.data||result?.selection||{};}
                function writeResultErrors(result,fallback){return result?.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result?.message||fallback||'选择请求失败'}];}
                function selectionTerminalStatus(type){const t=String(type||'');if(t==='selection_completed')return 'completed';if(t==='selection_cancelled')return 'cancelled';if(t==='selection_failed')return 'failed';return '';}
                function shouldIgnoreSelectionTerminal(selectionId,status,eventId=''){
                  if(isBlank(selectionId)||isBlank(status))return false;
                  const existing=appState.selectionTerminalById[selectionId];
                  if(!existing)return false;
                  if(existing.eventId&&eventId&&existing.eventId===eventId)return true;
                  return ['completed','cancelled','failed'].includes(existing.status);
                }
                function markSelectionTerminal(selectionId,status,eventId=''){
                  if(isBlank(selectionId)||isBlank(status))return;
                  if(!appState.selectionTerminalById[selectionId])appState.selectionTerminalById[selectionId]={status,eventId};
                }
                async function startCreateVirtualBlockDeviceSelection(){
                  if(!canStartObjectSelection())return;
                  const draft=selectionDraftFromForm();
                  draft.saving=true;draft.errors=[];draft.message='';
                  appState.selectionCreateVirtualBlock=draft;showCreateVirtualBlockDeviceModal();
                  try{
                    const result=await api('/api/webadmin/selection/start',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({purpose:'create_virtual_block_device',targetPlayerName:draft.targetPlayerName,channel:draft.channel,displayName:draft.displayName,note:draft.note,iconKey:draft.iconKey,enabled:draft.enabled})});
                    const selection=writeSelectionData(result);
                    if(result.success){
                      appState.selectionCreateVirtualBlock={...draft,...selection,step:'waiting',status:'started',saving:false,selectionId:selection.selectionId||result.targetId||draft.selectionId,message:result.message||'已通知目标玩家进入选择模式。',errors:[]};
                      showCreateVirtualBlockDeviceModal();
                      toast('已通知目标玩家进入选择模式。');
                      return;
                    }
                    draft.saving=false;draft.errors=writeResultErrors(result,'无法开始选择。');draft.message=result.message||'无法开始选择。';appState.selectionCreateVirtualBlock=draft;showCreateVirtualBlockDeviceModal();toast(draft.message);
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'无法开始选择。'}];draft.message=err.message||'无法开始选择。';appState.selectionCreateVirtualBlock=draft;showCreateVirtualBlockDeviceModal();toast(draft.message);}
                }
                async function cancelCreateVirtualBlockDeviceSelection(){
                  const draft=appState.selectionCreateVirtualBlock||selectionModalDefaultDraft();
                  if(draft.terminalStatus||shouldIgnoreSelectionTerminal(draft.selectionId,draft.status)){return;}
                  if(!draft.selectionId){appState.selectionCreateVirtualBlock={...draft,step:'cancelled',status:'cancelled',message:'已取消选择。'};showCreateVirtualBlockDeviceModal();return;}
                  draft.saving=true;draft.errors=[];appState.selectionCreateVirtualBlock=draft;showCreateVirtualBlockDeviceModal();
                  try{
                    const result=await api('/api/webadmin/selection/cancel',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({selectionId:draft.selectionId,confirmed:true,reason:'WebAdmin 已取消选择。'})});
                    const selection=writeSelectionData(result);
                    const returnedStatus=String(selection.status||'').toLowerCase(), finalStatus=result.success&&['completed','cancelled','failed'].includes(returnedStatus)?returnedStatus:(result.success?'cancelled':'failed');
                    markSelectionTerminal(draft.selectionId,finalStatus,'cancel-api');
                    if(finalStatus==='completed'){
                      const deviceId=String(selection.deviceId||draft.deviceId||''), routeTarget=selectionDeviceDetailRoute(deviceId,draft.returnTo||'#/virtual-block-devices');
                      appState.selectionCreateVirtualBlock={...draft,...selection,step:'completed',status:'completed',terminalStatus:'completed',saving:false,deviceId,routeTarget,message:selection.message||result.message||'虚拟方块设备已创建。',errors:[]};
                      showCreateVirtualBlockDeviceModal();
                      if(routeTarget){dismissWebAdminModal();appState.selectionCreateVirtualBlock=null;location.hash=routeTarget;}
                      return;
                    }
                    appState.selectionCreateVirtualBlock={...draft,...selection,step:finalStatus,status:finalStatus,terminalStatus:finalStatus,saving:false,message:result.message||selection.message||'选择已取消。',errors:result.success?[]:writeResultErrors(result,'取消选择失败。')};
                    showCreateVirtualBlockDeviceModal();
                    toast(result.message||'选择已取消。');
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'取消选择失败。'}];draft.message=err.message||'取消选择失败。';appState.selectionCreateVirtualBlock=draft;showCreateVirtualBlockDeviceModal();toast(draft.message);}
                }
                async function closeCreateVirtualBlockDeviceModal(cancelActive){
                  const draft=appState.selectionCreateVirtualBlock;
                  if(cancelActive&&draft&&draft.step==='waiting'&&draft.selectionId&&!draft.terminalStatus&&!shouldIgnoreSelectionTerminal(draft.selectionId,draft.status)){
                    try{await api('/api/webadmin/selection/cancel',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({selectionId:draft.selectionId,confirmed:true,reason:'WebAdmin modal closed.'})});}catch(_){}
                  }
                  appState.selectionCreateVirtualBlock=null;
                  await dismissWebAdminModal();
                }
                function resetCreateVirtualBlockDeviceModalToConfig(){
                  const current=appState.selectionCreateVirtualBlock||selectionModalDefaultDraft();
                  appState.selectionCreateVirtualBlock={...selectionModalDefaultDraft(),targetPlayerName:current.targetPlayerName||'',channel:current.channel||'',displayName:current.displayName||'',note:current.note||'',iconKey:current.iconKey||'auto',enabled:current.enabled!==false,returnTo:current.returnTo||selectionSourceReturnTo(),playerOptions:current.playerOptions||appState.onlinePlayerOptions||[],playerOptionsError:current.playerOptionsError||appState.onlinePlayerOptionsError,channelOptions:current.channelOptions||appState.channelOptions||[],channelOptionsError:current.channelOptionsError||appState.channelOptionsError};
                  showCreateVirtualBlockDeviceModal();
                }
                function handleSelectionRealtimeEvent(event){
                  const draft=appState.selectionCreateVirtualBlock;
                  if(!draft||!draft.selectionId)return;
                  const payload=event?.payload||{}, selectionId=String(event?.selectionId||payload.selectionId||'');
                  if(selectionId&&selectionId!==draft.selectionId)return;
                  const type=String(event?.type||'');
                  const terminal=selectionTerminalStatus(type), eventId=String(event?.id||event?.seq||'');
                  if(terminal&&shouldIgnoreSelectionTerminal(draft.selectionId,terminal,eventId))return;
                  if(type==='selection_completed'){
                    const deviceId=String(event?.deviceId||payload.deviceId||''), routeTarget=selectionDeviceDetailRoute(deviceId,draft.returnTo||'#/virtual-block-devices');
                    markChannelOptionsDirty(event);
                    markSelectionTerminal(draft.selectionId,'completed',eventId);
                    appState.selectionCreateVirtualBlock={...draft,...payload,step:'completed',status:'completed',terminalStatus:'completed',deviceId,routeTarget,message:event?.summary||payload.message||'虚拟方块设备已创建。',errors:[],saving:false};
                    showCreateVirtualBlockDeviceModal();
                    toast('虚拟方块设备已创建。');
                    if(routeTarget){dismissWebAdminModal();appState.selectionCreateVirtualBlock=null;location.hash=routeTarget;}
                  }else if(type==='selection_cancelled'){
                    markSelectionTerminal(draft.selectionId,'cancelled',eventId);
                    appState.selectionCreateVirtualBlock={...draft,...payload,step:'cancelled',status:'cancelled',terminalStatus:'cancelled',message:event?.summary||payload.message||'选择已取消。',errors:[],saving:false};
                    showCreateVirtualBlockDeviceModal();
                  }else if(type==='selection_failed'){
                    markSelectionTerminal(draft.selectionId,'failed',eventId);
                    appState.selectionCreateVirtualBlock={...draft,...payload,step:'failed',status:'failed',terminalStatus:'failed',message:event?.summary||payload.message||'选择失败。',errors:[{message:event?.summary||payload.message||'选择失败。'}],saving:false};
                    showCreateVirtualBlockDeviceModal();
                  }
                }
                """)
.append("""
                function lifecycleErrorsHtml(draft,fallback='操作失败'){
                  const errors=(draft?.errors||[]).filter(Boolean);
                  if(!errors.length)return '';
                  return `<ul class="validation-list">${errors.map(err=>`<li>${esc(err.field?`${err.field}：`: '')}${esc(err.message||err||fallback)}</li>`).join('')}</ul>`;
                }
                function dangerousModalFooter(saving=false,label='确认删除'){
                  return `<button class="wa-btn ghost" type="button" onclick="closeWebAdminModal()">${icon('close')}<span>取消</span></button><button class="wa-btn danger" type="button" ${saving?'disabled':''} onclick="document.querySelector('#wa-modal-root form')?.requestSubmit()">${icon('critical-issue')}<span>${saving?'处理中...':esc(label)}</span></button>`;
                }
                function lifecycleRouteWithReturn(targetHash,returnTo){
                  const safeReturn=isValidReturnHash(returnTo)?returnTo:'#/listeners';
                  return `${targetHash}${targetHash.includes('?')?'&':'?'}returnTo=${encodeURIComponent(safeReturn)}`;
                }
                const SIGNAL_LISTENER_CREATE_EXPECTED_FINGERPRINT='signal_listener_create_v1';
                function listenerCreateDefaultDraft(lock=null){
                  const draft={name:'',displayName:'',note:'',channel:'',enabled:true,cooldownTicks:0,conditionGroupId:'',conditionGateOptions:{},channelOptions:appState.channelOptions||[],channelOptionsError:appState.channelOptionsError||null,channelComboOpen:false,channelComboIndex:0,channelComboQuery:'',channelComboSearchActive:false,expectedFingerprint:SIGNAL_LISTENER_CREATE_EXPECTED_FINGERPRINT,lockId:lock?.lockId||'',lock:lock||null,saving:false,errors:[]};
                  markModalInitialSnapshot('signal_listener_create',draft);
                  return draft;
                }
                function listenerCreateChannelOptionsHtml(draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft)), current=normalizeChannelName(draft.channel).toLowerCase(), active=Math.max(0,Number(draft.channelComboIndex||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接使用新频道。</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" ${htmlHandler(`selectSignalListenerCreateChannel(${jsString(c.channel||'')})`)}><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function renderSignalListenerCreateChannelCombo(draft){
                  const open=draft.channelComboOpen?' open':'';
                  return `<div id="listener-create-channel-combo" class="channel-combo listener-create-channel-combo${open}" data-listener-create-channel-combo="true"><div class="channel-combo-control"><input id="listener-create-channel" class="input" maxlength="128" value="${esc(draft.channel||'')}" placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${draft.channelComboOpen?'true':'false'}" aria-controls="listener-create-channel-menu" onfocus='openSignalListenerCreateChannelMenu()' oninput='updateSignalListenerCreateDraftFromForm(true)' onkeydown='handleSignalListenerCreateChannelKey(event)'><button class="channel-combo-toggle" type="button" onclick='toggleSignalListenerCreateChannelMenu()' aria-label="显示已有频道">${icon('chevron-down')}</button></div><div id="listener-create-channel-menu" class="channel-combo-menu" role="listbox">${listenerCreateChannelOptionsHtml(draft)}</div></div>`;
                }
                function updateSignalListenerCreateDraftFromForm(openMenu=false){
                  const draft=appState.signalListenerCreate;if(!draft)return;
                  const nameInput=document.getElementById('listener-create-name'), displayNameInput=document.getElementById('listener-create-display-name'), noteInput=document.getElementById('listener-create-note'), channelInput=document.getElementById('listener-create-channel');
                  draft.name=nameInput?nameInput.value.trim():(draft.name||'');
                  draft.displayName=displayNameInput?displayNameInput.value.trim():(draft.displayName||'');
                  draft.note=noteInput?noteInput.value:(draft.note||'');
                  draft.channel=channelInput?channelInput.value:(draft.channel||'');
                  draft.enabled=!!document.getElementById('listener-create-enabled')?.checked;
                  draft.cooldownTicks=document.getElementById('listener-create-cooldown')?.value||draft.cooldownTicks||0;
                  draft.conditionGroupId=document.getElementById('runtime-condition-conditionGroupId')?.value??draft.conditionGroupId??'';
                  if(openMenu){draft.channelComboOpen=true;draft.channelComboIndex=0;setChannelComboQuery(draft,draft.channel);}
                  const hint=document.getElementById('listener-create-channel-hint');
                  if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);
                  syncSignalListenerCreateChannelCombo();
                }
                function syncSignalListenerCreateChannelCombo(){
                  const draft=appState.signalListenerCreate;if(!draft)return;
                  const combo=document.getElementById('listener-create-channel-combo'), menu=document.getElementById('listener-create-channel-menu'), input=document.getElementById('listener-create-channel');
                  if(combo)combo.classList.toggle('open',!!draft.channelComboOpen);
                  if(input)input.setAttribute('aria-expanded',draft.channelComboOpen?'true':'false');
                  if(menu)menu.innerHTML=listenerCreateChannelOptionsHtml(draft);
                }
                function openSignalListenerCreateChannelMenu(){const draft=appState.signalListenerCreate;if(!draft)return;updateSignalListenerCreateDraftFromForm(false);closeAllCustomComboboxes();draft.channelComboOpen=true;resetChannelComboQuery(draft);syncSignalListenerCreateChannelCombo();}
                function toggleSignalListenerCreateChannelMenu(){const draft=appState.signalListenerCreate;if(!draft)return;updateSignalListenerCreateDraftFromForm(false);const wasOpen=!!draft.channelComboOpen;if(wasOpen){draft.channelComboOpen=false;syncSignalListenerCreateChannelCombo();return;}closeAllCustomComboboxes();draft.channelComboOpen=true;resetChannelComboQuery(draft);syncSignalListenerCreateChannelCombo();document.getElementById('listener-create-channel')?.focus();}
                function selectSignalListenerCreateChannel(channel){const draft=appState.signalListenerCreate;if(!draft)return;draft.channel=channel||'';draft.channelComboOpen=false;draft.channelComboIndex=0;resetChannelComboQuery(draft);const input=document.getElementById('listener-create-channel'), hint=document.getElementById('listener-create-channel-hint');if(input)input.value=draft.channel;if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncSignalListenerCreateChannelCombo();}
                function handleSignalListenerCreateChannelKey(event){
                  const draft=appState.signalListenerCreate;if(!draft)return;
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft));
                  if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.channelComboOpen=false;syncSignalListenerCreateChannelCombo();return;}
                  if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex||0)+1:Number(draft.channelComboIndex||0)-1;draft.channelComboIndex=Math.min(max,Math.max(0,next));syncSignalListenerCreateChannelCombo();return;}
                  if(event.key==='Enter'&&draft.channelComboOpen&&options.length>0){event.preventDefault();selectSignalListenerCreateChannel(options[Math.min(options.length-1,Number(draft.channelComboIndex||0))].channel);return;}
                }
                function signalListenerCreateModalBody(draft){
                  const errors=lifecycleErrorsHtml(draft,'创建监听器失败');
                  const gatePicker=runtimeConditionGatePicker(draft,'conditionGroupId','SIGNAL_LISTENER',draft.name||'new',false,'updateSignalListenerCreateDraftFromForm(false)');
                  return `<form class="edit-form" data-listener-create-modal="true" data-logic-chain-condition-group-reference-edit="true" ${htmlEvent('onsubmit','event.preventDefault();saveSignalListenerCreateModal()')}><label>监听器技术 ID<input id="listener-create-name" class="input" maxlength="64" value="${esc(draft.name||'')}" placeholder="例如：spawn_entry_listener" oninput="updateSignalListenerCreateDraftFromForm(false)"></label><label>显示名称<input id="listener-create-display-name" class="input" maxlength="80" value="${esc(draft.displayName||'')}" placeholder="可选；为空时使用技术 ID" oninput="updateSignalListenerCreateDraftFromForm(false)"></label><label>备注<textarea id="listener-create-note" class="input wa-action-textarea" maxlength="512" oninput="updateSignalListenerCreateDraftFromForm(false)">${esc(draft.note||'')}</textarea></label><label>监听频道${renderSignalListenerCreateChannelCombo(draft)}<span id="listener-create-channel-hint" class="muted">${channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError)}</span></label><label class="switch-row"><span>启用监听器</span><input id="listener-create-enabled" type="checkbox" ${draft.enabled!==false?'checked':''} onchange="updateSignalListenerCreateDraftFromForm(false)"></label><label>冷却时间（ticks）<input id="listener-create-cooldown" class="input" type="number" min="0" max="72000" step="1" value="${esc(draft.cooldownTicks ?? 0)}" oninput="updateSignalListenerCreateDraftFromForm(false)"></label>${gatePicker}<p class="readonly-note" data-logic-chain-gate-reference-not-branch="true">条件组是监听器外层 gate：allow 时继续旧逻辑，block 时跳过；不是 if/else 分支。</p><p class="readonly-note">新建虚拟监听器默认动作列表为空；创建后可在详情页管理动作。本阶段不会创建 matcher、itemSubmit 或 ConditionEngine。</p>${errors}</form>`;
                }
                function showSignalListenerCreateModal(){
                  const draft=appState.signalListenerCreate||listenerCreateDefaultDraft();
                  markModalInitialSnapshot('signal_listener_create',draft);
                  openWebAdminModal('新建虚拟监听器',signalListenerCreateModalBody(draft),editModalFooter(draft.saving),{className:'wa-config-modal',onClose:async()=>{const closing=appState.signalListenerCreate;appState.signalListenerCreate=null;await releaseSignalListenerCreateLock(closing,true);return dismissWebAdminModal();},syncBeforeClose:()=>syncModalDraftBeforeClose('signal_listener_create'),dirtyCheck:()=>modalDraftDirty('signal_listener_create',appState.signalListenerCreate)});
                }
                async function openSignalListenerCreateModal(){
                  waEnsureState();
                  if(!canCreateSignalListener()){toast('需要 EDITOR 或 OWNER 权限才能新建虚拟监听器。');return;}
                  const lockResult=await acquireWebAdminEditLock('signal_listener_basic_config','new');
                  if(!lockResult.success){toast(lockResult.message||'无法获取新建监听器编辑锁。');return;}
                  appState.signalListenerCreate=listenerCreateDefaultDraft(lockResult.data?.lock||{});
                  showSignalListenerCreateModal();
                  const [channels,conditionGateOptions]=await Promise.all([loadSignalChannelOptions(),loadRuntimeConditionGateOptions(['SIGNAL_LISTENER'],'new')]);
                  const draft=appState.signalListenerCreate;
                  if(draft){appState.signalListenerCreate={...draft,channelOptions:channels,channelOptionsError:appState.channelOptionsError,conditionGateOptions};showSignalListenerCreateModal();}
                }
                async function saveSignalListenerCreateModal(){
                  const draft=appState.signalListenerCreate||listenerCreateDefaultDraft();
                  updateSignalListenerCreateDraftFromForm(false);
                  draft.saving=true;draft.errors=[];appState.signalListenerCreate=draft;showSignalListenerCreateModal();
                  try{
                    if(!draft.lockId){const lockResult=await acquireWebAdminEditLock('signal_listener_basic_config','new');if(!lockResult.success){draft.saving=false;draft.errors=writeResultErrors(lockResult,'无法获取新建监听器编辑锁。');appState.signalListenerCreate=draft;showSignalListenerCreateModal();toast(lockResult.message||'无法获取新建监听器编辑锁。');return;}draft.lock=lockResult.data?.lock||{};draft.lockId=draft.lock.lockId||'';}
                    const result=await api('/api/webadmin/signal-listeners',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({name:draft.name,displayName:draft.displayName||draft.name,note:draft.note||'',channel:String(draft.channel||'').trim(),enabled:draft.enabled!==false,cooldownTicks:Number(draft.cooldownTicks||0),conditionGroupId:draft.conditionGroupId||'',expectedFingerprint:draft.expectedFingerprint||SIGNAL_LISTENER_CREATE_EXPECTED_FINGERPRINT,lockId:draft.lockId||''})});
                    if(result.success){
                      markChannelOptionsDirty({type:'signal_listener_changed'});
                      const routeTarget=result.data?.routeTarget||lifecycleRouteWithReturn(listenerHash(result.data?.listenerId||result.targetId||''),'#/listeners');
                      appState.signalListenerCreate=null;
                      await dismissWebAdminModal();
                      toast(result.message||'虚拟监听器已创建。');
                      if(routeTarget)location.hash=routeTarget;else await renderListeners({silent:true});
                      return;
                    }
                    draft.saving=false;draft.errors=writeResultErrors(result,'创建虚拟监听器失败。');if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code)){draft.lockId='';draft.lock=null;}appState.signalListenerCreate=draft;showSignalListenerCreateModal();toast(result.message||'创建虚拟监听器失败。');
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'创建虚拟监听器失败。'}];appState.signalListenerCreate=draft;showSignalListenerCreateModal();toast(err.message||'创建虚拟监听器失败。');}
                }
                async function releaseSignalListenerCreateLock(draft,silent=true){
                  if(!draft||!draft.lockId)return;
                  try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_listener_basic_config',targetId:'new',lockId:draft.lockId})});}
                  catch(err){if(!silent)toast(err.message||'编辑锁释放失败，将等待自动过期。');}
                }
                """).toString();
    }
}
