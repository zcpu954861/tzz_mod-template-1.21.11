package com.zcpu.tzzmod.webadmin;

final class WebAdminFrontendHelpScripts {
    private WebAdminFrontendHelpScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                function helpInlineTermDefinitions(){
                  return [
                    {termId:'signalbridge',display:'SignalBridge',aliases:['SignalBridge','事件总线','频道'],shortDefinition:'负责接收和分发 signal 的事件总线。',targetRoute:'#/signals',targetHelpTopic:'signalbridge.channel-basics',category:'Signal'},
                    {termId:'signal-listener',display:'SignalListener',aliases:['SignalListener','信号监听器','监听器'],shortDefinition:'监听指定频道并触发后续 Action 的配置。',targetRoute:'#/listeners',targetHelpTopic:'signalbridge.listener-flow',category:'Signal'},
                    {termId:'timer',display:'Timer',aliases:['Timer','调度器','计时器'],shortDefinition:'负责延迟、倒计时和重复触发的时间轴能力。',targetRoute:'#/timers',targetHelpTopic:'timer.delay',category:'Scheduler'},
                    {termId:'condition-group',display:'ConditionGroup',aliases:['ConditionGroup','条件组'],shortDefinition:'只读判断规则组，用作 runtime gate 或 action gate。',targetRoute:'#/condition-groups',targetHelpTopic:'condition.group-basics',category:'Condition'},
                    {termId:'state-variable',display:'StateVariable',aliases:['StateVariable','状态变量'],shortDefinition:'保存全局或玩家维度的 typed 状态。',targetRoute:'#/state-variables',targetHelpTopic:'state-variable.basics',category:'State'},
                    {termId:'logic-chain',display:'Logic Chain',aliases:['Logic Chain','LogicChain','逻辑链'],shortDefinition:'把频道、监听器、Join、Timer 和 Action 的关系可视化为组件图。',targetRoute:'#/logic-chains',targetHelpTopic:'logic-chain.viewer',category:'Graph'},
                    {termId:'templates',display:'Templates',aliases:['Templates','Template Center','模板中心','模板','Prefab'],shortDefinition:'复用配置组合的只读预览和受控 apply 入口。',targetRoute:'#/templates',targetHelpTopic:'templates.prefab',category:'Template'},
                    {termId:'snapshot',display:'Snapshot',aliases:['Snapshot','配置时间轴','保存点'],shortDefinition:'WebAdmin allowlist 配置保存点，不是世界备份。',targetRoute:'#/snapshots',targetHelpTopic:'snapshot.rollback',category:'Snapshot'},
                    {termId:'rollback',display:'Rollback',aliases:['Rollback','配置回滚','回滚','pre_rollback','本次操作变化'],shortDefinition:'先 dry-run 再确认的配置恢复流程，apply 前会创建保护点。',targetRoute:'#/snapshots',targetHelpTopic:'snapshot.rollback',category:'Snapshot'},
                    {termId:'debugger',display:'Debugger',aliases:['Debugger','调试器','Replay'],shortDefinition:'查看 gate 评估、历史和 replay 的排查入口。',targetRoute:'#/condition-debugger',targetHelpTopic:'debugger.doctor-replay',category:'Debug'},
                    {termId:'doctor',display:'Doctor',aliases:['Doctor','诊断'],shortDefinition:'聚合 Signal、设备、监听器和动作问题的只读诊断页。',targetRoute:'#/doctor',targetHelpTopic:'debugger.doctor-replay',category:'Debug'},
                    {termId:'signal-join',display:'Signal Join',aliases:['Signal Join','Join','信号汇合'],shortDefinition:'等待多个输入频道满足规则后输出到下游频道。',targetRoute:'#/signal-joins',targetHelpTopic:'signal-join.basics',category:'Signal'},
                    {termId:'action',display:'Action',aliases:['Action','动作'],shortDefinition:'由监听器、Timer 或 ActionRelay 调用的受控执行单元。',targetRoute:'#/actions',targetHelpTopic:'action.config-basics',category:'Action'},
                    {termId:'action-relay',display:'ActionRelay',aliases:['ActionRelay','动作继电器'],shortDefinition:'设备侧承载 Action 列表的世界对象引用。',targetRoute:'#/devices',targetHelpTopic:'device-trigger.references',category:'Device'},
                    {termId:'vbd',display:'VBD',aliases:['VBD','VirtualBlockDevice','虚拟方块'],shortDefinition:'用虚拟设备把世界交互接入 Signal / Action 工作流。',targetRoute:'#/virtual-block-devices',targetHelpTopic:'device-trigger.references',category:'Device'},
                    {termId:'region',display:'Region',aliases:['Region','区域'],shortDefinition:'区域与区域控制器提供 enter / exit / stay 类触发来源。',targetRoute:'#/regions',targetHelpTopic:'region.controller',category:'Region'},
                    {termId:'signal-receiver',display:'SignalReceiver',aliases:['SignalReceiver','接收器'],shortDefinition:'作为世界侧 Signal 接收或展示引用的设备类型。',targetRoute:'#/receivers',targetHelpTopic:'device-trigger.references',category:'Device'}
                  ];
                }
                function helpInlineTermById(termId){return helpInlineTermDefinitions().find(t=>String(t.termId)===String(termId))||null;}
                function helpInlineTermButton(term,matched){
                  return `<button class="help-inline-term" type="button" data-help-inline-term="true" data-help-example-center-inline-term="true" data-help-example-center-inline-term-data-id="true" data-term-id="${esc(term.termId)}" aria-haspopup="dialog" aria-label="查看术语 ${esc(term.display)}">${esc(matched)}</button>`;
                }
                function helpInlineText(value,mode='basic'){
                  const text=String(value||'');
                  if(!text)return '';
                  const lower=text.toLowerCase(), defs=helpInlineTermDefinitions(), candidates=[];
                  defs.forEach((term,termOrder)=>(term.aliases||[]).forEach(alias=>{
                    const raw=String(alias||''), index=lower.indexOf(raw.toLowerCase());
                    if(index>=0)candidates.push({index,end:index+raw.length,term,alias:raw,termOrder});
                  }));
                  const limit=mode==='professional'?4:2, chosen=[], usedTerms=new Set();
                  candidates.sort((a,b)=>a.index-b.index||(b.end-b.index)-(a.end-a.index)||a.termOrder-b.termOrder).forEach(c=>{
                    if(chosen.length>=limit||usedTerms.has(c.term.termId))return;
                    if(chosen.some(x=>c.index<x.end&&c.end>x.index))return;
                    chosen.push(c);usedTerms.add(c.term.termId);
                  });
                  if(!chosen.length)return esc(text);
                  chosen.sort((a,b)=>a.index-b.index);
                  let cursor=0, html='';
                  chosen.forEach(c=>{html+=esc(text.substring(cursor,c.index));html+=helpInlineTermButton(c.term,text.substring(c.index,c.end));cursor=c.end;});
                  return html+esc(text.substring(cursor));
                }
                function helpInlineTermPopoverHtml(term){
                  const helpAction=term.targetHelpTopic?waButton('查看文档','help-center',`data-help-term-open-help="true" data-term-id="${esc(term.termId)}" data-help-example-center-inline-term-related-help-action="true"`,'ghost'):'';
                  return `<div class="help-term-popover" data-help-term-popover="true" data-term-id="${esc(term.termId)}" data-help-example-center-inline-term-popover="true" data-help-example-center-single-active-popover="true" data-help-example-center-popover-close-timer-term-id="true" data-help-example-center-popover-fast-switch-stable="true" data-help-example-center-popover-scroll-close="true" data-help-example-center-popover-bottom-safe="true" role="dialog" aria-label="${esc(term.display)} 术语解释"><header><strong>${esc(term.display)}</strong><span>${esc(term.category||'模块')}</span></header><p data-help-example-center-inline-term-definition="true">${esc(term.shortDefinition||'暂无说明。')}</p><div class="help-term-popover-actions">${waButton('打开页面','chevron-right',`data-help-term-open-page="true" data-term-id="${esc(term.termId)}" data-help-example-center-inline-term-open-page-action="true"`,'primary')}${helpAction}</div></div>`;
                }
                function helpClearInlineTermCloseTimer(){
                  if(appState.helpInlineTermCloseTimer){clearTimeout(appState.helpInlineTermCloseTimer);appState.helpInlineTermCloseTimer=null;}
                }
                function helpActiveTermId(){return appState.helpInlineTermPopover?.termId||'';}
                function helpTermHoverOrFocus(termId){
                  const id=String(termId||helpActiveTermId()||'');
                  const active=document.activeElement;
                  const pop=document.querySelector('[data-help-term-popover]');
                  if(pop&&(pop.matches(':hover')||pop.contains(active)))return true;
                  return Array.from(document.querySelectorAll(`[data-help-inline-term][data-term-id="${cssEscape(id)}"]`)).some(el=>el.matches(':hover')||el===active||el.contains(active));
                }
                function helpScheduleInlineTermPopoverClose(termId,delay=140){
                  const id=String(termId||helpActiveTermId()||'');
                  helpClearInlineTermCloseTimer();
                  appState.helpInlineTermCloseTimer=setTimeout(()=>{if(helpActiveTermId()!==id)return;if(!helpTermHoverOrFocus(id))hideHelpInlineTermPopover(id);},delay);
                }
                function hideHelpInlineTermPopover(termId=''){
                  if(termId&&helpActiveTermId()!==String(termId))return;
                  helpClearInlineTermCloseTimer();
                  const pop=document.querySelector('[data-help-term-popover]');
                  if(pop)pop.remove();
                  appState.helpInlineTermPopover=null;
                }
                function positionHelpInlineTermPopover(el,pop){
                  if(!el||!pop)return;
                  const rect=el.getBoundingClientRect(), margin=10, gap=8;
                  const width=pop.offsetWidth||320, height=pop.offsetHeight||160;
                  let top=rect.bottom+gap;
                  if(top+height>window.innerHeight-margin)top=rect.top-height-gap;
                  top=Math.min(window.innerHeight-height-margin,Math.max(margin,top));
                  const left=Math.min(window.innerWidth-width-margin,Math.max(margin,rect.left));
                  pop.style.top=`${top}px`;pop.style.left=`${left}px`;
                }
                function showHelpInlineTermPopover(el){
                  const term=helpInlineTermById(el?.dataset?.termId||'');
                  if(!term)return;
                  helpClearInlineTermCloseTimer();
                  const existing=document.querySelector('[data-help-term-popover]');
                  if(existing&&helpActiveTermId()===term.termId){positionHelpInlineTermPopover(el,existing);return;}
                  hideHelpInlineTermPopover();
                  const wrapper=document.createElement('div');
                  wrapper.innerHTML=helpInlineTermPopoverHtml(term);
                  const pop=wrapper.firstElementChild;
                  document.body.appendChild(pop);
                  pop.addEventListener('mouseenter',()=>helpClearInlineTermCloseTimer());
                  pop.addEventListener('mouseleave',()=>helpScheduleInlineTermPopoverClose(term.termId,140));
                  pop.addEventListener('focusin',()=>helpClearInlineTermCloseTimer());
                  pop.addEventListener('focusout',()=>helpScheduleInlineTermPopoverClose(term.termId,140));
                  positionHelpInlineTermPopover(el,pop);
                  appState.helpInlineTermPopover={termId:term.termId};
                }
                function openHelpInlineTermPage(termId){
                  const term=helpInlineTermById(termId);
                  if(!term||!isValidReturnHash(term.targetRoute)){toast('该术语暂未配置可打开页面。');return;}
                  hideHelpInlineTermPopover();
                  location.hash=helpTargetRouteWithReturn(term.targetRoute);
                }
                function openHelpInlineTermHelp(termId){
                  const term=helpInlineTermById(termId);
                  if(!term?.targetHelpTopic)return;
                  hideHelpInlineTermPopover();
                  navigateTo(helpViewHash('docs',term.targetHelpTopic));
                }
                function openHelpInlineTermDefault(termId,anchor){
                  const term=helpInlineTermById(termId);
                  if(!term)return;
                  if(term.targetHelpTopic){
                    const params=parseHashParams(currentRouteHash());
                    if(routeBase(currentRouteHash())==='#/help'&&String(params.topic||'')===String(term.targetHelpTopic)){showHelpInlineTermPopover(anchor);return;}
                    hideHelpInlineTermPopover();
                    navigateTo(helpViewHash('docs',term.targetHelpTopic));
                    return;
                  }
                  showHelpInlineTermPopover(anchor);
                }
                function helpSectionList(sections){
                  const list=sections||[];
                  if(list.length===0)return empty('这个分类的专业文档还在补充中。');
                  const mode=appState.helpCenterFilters?.mode||'basic';
                  return `<div class="help-section-list">${list.map(section=>`<section class="help-section"><h3>${helpInlineText(section.title||'说明',mode)}</h3><ul>${(section.bullets||[]).map(item=>`<li>${helpInlineText(item,mode)}</li>`).join('')}</ul></section>`).join('')}</div>`;
                }
                function helpRouteLinks(links){
                  if(!links||links.length===0)return '<span class="muted">暂无页面链接。</span>';
                  return `<div class="help-link-row">${links.map(link=>{const route=String(link.route||''), attrs=[navDataAttr(route,link.label||route||'打开'),'data-help-example-center-route-link="true"'];if(route.startsWith('#/doctor')||route.includes('debugger')||route.includes('diagnostic'))attrs.push('data-help-example-center-doctor-link="true"');if(route.startsWith('#/templates'))attrs.push('data-help-example-center-template-link="true"');return waButton(link.label||route||'打开','chevron-right',attrs.join(' '),'ghost');}).join('')}</div>`;
                }
                function helpTopicCards(catalog,items,selectedId=''){
                  const topics=items.filter(item=>item.kind==='topic');
                  if(topics.length===0)return empty('这个分类的专业文档还在补充中。');
                  return topics.map(topic=>{const active=String(topic.id)===String(selectedId);return `<article class="help-topic-card ${active?'active':''}" data-help-example-center-topic-card="true" data-help-example-center-topic-active="${active?'true':'false'}" data-help-topic-id="${esc(topic.id)}" data-page-help-topic="${esc(topic.id)}" ${navDataAttr(helpViewHash('docs',topic.id),'打开帮助主题')}><div><span class="pill info">${esc(helpCategoryTitle(catalog,topic.category))}</span>${active?'<span class="pill ok">当前</span>':''}<strong>${esc(topic.title)}</strong><p>${esc(topic.summary)}</p></div><small>${esc(helpPhraseList((topic.tags||[]).slice(0,4)))}</small></article>`;}).join('');
                }
                function helpExampleCard(catalog,example,mode='basic'){
                  const templateLink=example.relatedTemplateId?waButton('去模板中心','template-package',navDataAttr('#/templates','去模板中心')+' data-help-example-center-template-link="true" data-help-example-center-template-cta-aligned="true"','ghost'):'<span class="muted" data-help-example-center-no-template-aligned="true">无模板关联</span>';
                  const pro=mode==='professional'&&Array.isArray(example.professionalNotes)&&example.professionalNotes.length?`<div class="help-mini-list"><strong>专业说明</strong><ul>${example.professionalNotes.map(item=>`<li>${esc(item)}</li>`).join('')}</ul></div>`:'';
                  return `<article class="help-example-card" data-help-example-center-example-card="true"><header><span class="pill ok">示例</span><h3>${helpInlineText(example.title||example.id,mode)}</h3><p>${helpInlineText(example.goal||example.whenToUse||'',mode)}</p></header><div class="help-mini-list"><strong>基础步骤</strong><ol>${(example.steps||[]).map(step=>`<li>${helpInlineText(step,mode)}</li>`).join('')}</ol></div>${pro}<div class="help-mini-list"><strong>常见错误</strong><ul>${(example.commonErrors||[]).map(item=>`<li>${helpInlineText(helpCleanPhrase(item),mode)}</li>`).join('')}</ul></div><footer class="help-example-footer" data-help-example-center-template-relation-footer="true"><div class="help-example-routes">${helpRouteLinks(example.relatedRoutes||[])}</div><div class="help-template-relation">${templateLink}</div></footer></article>`;
                }
                function helpTroubleCard(item,mode='basic'){
                  const pro=mode==='professional'&&item.professionalExplanation?`<small class="muted">${esc(item.professionalExplanation)}</small>`:'';
                  return `<article class="event-row help-trouble-card" data-help-example-center-clean-reason-list="true"><strong>${helpInlineText(item.title||item.symptom||item.id,mode)}</strong><span class="meta">可能原因：${helpInlineText(helpPhraseList(item.likelyCauses)||'暂无',mode)}</span><span>检查位置：${helpInlineText(helpPhraseList(item.checks)||'暂无',mode)}</span><span>推荐操作：${helpInlineText(helpPhraseList(item.fixHints)||'暂无',mode)}</span>${pro}${helpRouteLinks(item.relatedRoutes||[])}</article>`;
                }
                function helpGlossaryTerm(item,mode='basic'){
                  const notes=mode==='professional'&&item.technicalNotes?` · ${esc(item.technicalNotes)}`:'';
                  return `<div class="help-glossary-term"><strong>${helpInlineText(item.term||item.title||item.id,mode)}</strong><span>${helpInlineText(item.definition||'',mode)}</span><small>${esc((item.aliases||[]).join(' / '))}${notes}</small></div>`;
                }
                function helpTopicDetail(catalog,selectedId,mode,returnTo=''){
                  const topic=helpTopicById(catalog,selectedId)||helpTopicById(catalog,(catalog?.featuredTopicIds||[])[0])||(catalog?.topics||[])[0];
                  if(!topic)return `<article class="help-detail-panel" data-help-example-center-topic-detail="true">${empty('帮助目录暂无主题。')}</article>`;
                  const missing=selectedId&&!helpTopicById(catalog,selectedId)?`<div class="empty-state">未找到请求的帮助主题：${esc(selectedId)}。已显示推荐入门主题。</div>`:'';
                  const examples=(topic.examples||[]).map(id=>helpExampleById(catalog,id)).filter(Boolean);
                  const trouble=(topic.troubleshootingLinks||[]).map(id=>helpTroubleById(catalog,id)).filter(Boolean);
                  const terms=(topic.glossaryTerms||[]).map(id=>helpGlossaryById(catalog,id)).filter(Boolean);
                  const sections=mode==='professional'?topic.professionalSections:topic.basicSections;
                  const back=isValidReturnHash(returnTo)?waButton('返回原页面','chevron-left',navDataAttr(returnTo,'返回原页面')+' data-page-help-return-action="true"','ghost'):'';
                  return `<article class="help-detail-panel" data-help-example-center-topic-detail="true" data-page-help-topic="${esc(topic.id)}">${missing}<header><h2>${helpInlineText(mode==='professional'?topic.professionalTitle||topic.title:topic.basicTitle||topic.title,mode)}</h2><p>${helpInlineText(mode==='professional'?topic.professionalSummary||topic.summary:topic.basicSummary||topic.summary,mode)}</p>${back}</header>${helpSectionList(sections)}<section><h3>相关页面</h3>${helpRouteLinks(topic.pageLinks||[])}</section><section><h3>相关示例</h3><div class="help-example-inline">${examples.length?examples.map(example=>helpExampleCard(catalog,example,mode)).join(''):empty('暂无直接关联示例。')}</div></section><section><h3>排错入口</h3><div class="list-stack">${trouble.length?trouble.map(item=>helpTroubleCard(item,mode)).join(''):empty('暂无直接关联排错。')}</div></section><section><h3>术语</h3><div class="help-glossary-grid">${terms.length?terms.map(term=>helpGlossaryTerm(term,mode)).join(''):empty('暂无直接关联术语。')}</div></section></article>`;
                }
                function helpViewTabs(view){
                  const tabs=[['docs','文档区','help-center'],['examples','示例中心','example-center'],['troubleshooting','排错中心','doctor-overview'],['glossary','术语表','help-center']];
                  return `<nav class="help-view-tabs" data-help-example-center-view-tabs="true" aria-label="帮助中心主视图">${tabs.map(([id,label,iconName])=>`<button class="wa-btn ${view===id?'primary':'ghost'}" type="button" data-help-view="${esc(id)}" data-help-example-center-view-tab="${esc(id)}">${icon(iconName)}<span>${esc(label)}</span></button>`).join('')}</nav>`;
                }
                function helpRightNavCategories(catalog,view){
                  if(view==='docs')return (catalog?.categories||[]).map(c=>({id:String(c.id),title:c.title,summary:c.summary||''}));
                  if(view==='examples')return [
                    {id:'join',title:'Join',summary:'多输入汇合示例。'},{id:'timer',title:'Timer',summary:'延迟、倒计时和启动动作。'},{id:'listener',title:'Listener',summary:'监听频道后执行动作。'},{id:'state',title:'StateVariable',summary:'状态变量写入示例。'},{id:'condition',title:'Condition',summary:'条件组控制 action。'},{id:'template',title:'Template',summary:'模板、prefab 和 apply。'},{id:'logicChain',title:'Logic Chain',summary:'逻辑链草稿和可视化。'},{id:'signal',title:'Signal',summary:'频道、消费者和历史。'}
                  ];
                  if(view==='troubleshooting')return [
                    {id:'condition',title:'条件',summary:'条件组不可选、gate 调试。'},{id:'join',title:'Join',summary:'汇合无输出或输入未满足。'},{id:'timer',title:'Timer',summary:'计时器未启动或无输出。'},{id:'listener',title:'Listener',summary:'监听器未执行 action。'},{id:'template',title:'模板',summary:'apply 冲突或导入未生效。'},{id:'editor',title:'编辑器',summary:'保存失败、锁和节点可见性。'},{id:'signal',title:'Signal',summary:'频道无消费者或拼写问题。'},{id:'state',title:'状态变量',summary:'状态变量动作失败。'}
                  ];
                  if(view==='glossary')return [
                    {id:'signal',title:'Signal',summary:'频道、监听器、接收器和 Join。'},{id:'action',title:'Action',summary:'动作、动作配置和动作引擎。'},{id:'condition',title:'Condition',summary:'条件组、条件引擎和 gate。'},{id:'logicChain',title:'Logic Chain',summary:'逻辑链、组件和焦点频道。'},{id:'template',title:'Template',summary:'模板、prefab、dry-run 和 placeholder。'},{id:'runtime',title:'Runtime',summary:'运行态、调度和 replay。'},{id:'editLock',title:'编辑锁',summary:'锁、fingerprint 和冲突。'},{id:'device',title:'设备',summary:'VBD、Region、Relay 和 Receiver。'}
                  ];
                  return [];
                }
                function helpRightPanel(catalog,view,category,topic=null){
                  const categories=helpRightNavCategories(catalog,view);
                  const related=(topic?.relatedTopics||[]).map(id=>helpTopicById(catalog,id)).filter(Boolean);
                  const title=category==='ALL'?'全部':(categories.find(c=>String(c.id)===String(category))?.title||helpCategoryTitle(catalog,category));
                  return `<aside class="help-right-panel" data-help-example-center-right-category-nav="true" data-help-example-center-right-nav-per-view="true" data-help-example-center-right-nav-view="${esc(view)}"><section><h2>${esc(view==='docs'?'分类导航':view==='examples'?'示例分类':view==='troubleshooting'?'排错分类':'术语分类')}</h2><button class="help-category-button ${category==='ALL'?'active':''}" type="button" data-help-category="ALL" data-help-example-center-category-clickable="true" data-help-example-center-category-active="${category==='ALL'?'true':'false'}">全部</button>${categories.map(c=>`<button class="help-category-button ${String(c.id)===String(category)?'active':''}" type="button" data-help-category="${esc(c.id)}" data-help-example-center-category-clickable="true" data-help-example-center-category-active="${String(c.id)===String(category)?'true':'false'}"><strong>${esc(c.title)}</strong><span>${esc(c.summary||'')}</span></button>`).join('')}</section>${view==='docs'?`<section><h2>相关主题</h2><div class="help-related-list">${related.length?related.map(t=>`<button class="help-related-topic" type="button" data-help-topic-id="${esc(t.id)}">${esc(t.title)}</button>`).join(''):'<span class="muted">暂无直接关联主题。</span>'}</div></section>`:''}<section><h2>当前视图</h2><p class="muted">${esc(helpViewLabel(view))} · ${esc(title)}</p></section></aside>`;
                }
                function helpDocsView(catalog,items,selected,mode,returnTo,category){
                  const topic=helpTopicById(catalog,selected)||helpTopicById(catalog,(catalog?.featuredTopicIds||[])[0])||(catalog?.topics||[])[0]||null;
                  const selectedId=topic?.id||selected||'';
                  return `<section class="help-center-layout help-docs-layout" data-help-example-center-docs-view="true" data-help-example-center-fixed-viewport="true" data-help-example-center-no-whole-page-long-scroll="true"><aside class="help-topic-list-panel" data-help-example-center-topic-list-scroll-shell="true"><h2>主题列表</h2><div class="help-topic-list" data-help-example-center-topic-list="true" data-help-example-center-topic-list-internal-scroll="true" data-help-example-center-topic-list-preserve-scroll="true">${helpTopicCards(catalog,items,selectedId)}</div></aside><main class="help-document-scroll" data-help-example-center-document-scroll="true">${helpTopicDetail(catalog,selectedId,mode,returnTo)}</main>${helpRightPanel(catalog,'docs',category,topic)}</section>`;
                }
                function helpExamplesView(catalog,examples,mode,category){
                  return `<section class="help-center-layout help-single-view-layout" data-help-example-center-examples-view="true" data-help-example-center-fixed-viewport="true" data-help-example-center-no-whole-page-long-scroll="true"><main class="help-document-scroll" data-help-example-center-document-scroll="true"><section class="help-example-section" data-help-example-center-example-list="true"><header><h2>示例中心</h2><p>示例是文档示例，不会自动创建配置。</p></header><div class="help-example-grid">${examples.length?examples.map(example=>helpExampleCard(catalog,example,mode)).join(''):empty('当前筛选下暂无示例。')}</div></section></main>${helpRightPanel(catalog,'examples',category)}</section>`;
                }
                function helpTroubleshootingView(catalog,troubles,mode,category){
                  return `<section class="help-center-layout help-single-view-layout" data-help-example-center-troubleshooting-view="true" data-help-example-center-fixed-viewport="true" data-help-example-center-no-whole-page-long-scroll="true"><main class="help-document-scroll" data-help-example-center-document-scroll="true"><section class="help-troubleshooting-section" data-help-example-center-troubleshooting-list="true" data-help-example-center-clean-reason-list="true"><header><h2>排错中心</h2><p>从现象出发，定位检查位置和推荐操作。</p></header><div class="list-stack">${troubles.length?troubles.map(item=>helpTroubleCard(item,mode)).join(''):empty('当前筛选下暂无排错条目。')}</div></section></main>${helpRightPanel(catalog,'troubleshooting',category)}</section>`;
                }
                function helpGlossaryView(catalog,terms,mode,category){
                  return `<section class="help-center-layout help-single-view-layout" data-help-example-center-glossary-view="true" data-help-example-center-fixed-viewport="true" data-help-example-center-no-whole-page-long-scroll="true"><main class="help-document-scroll" data-help-example-center-document-scroll="true"><section class="help-glossary-section" data-help-example-center-glossary="true"><header><h2>术语表</h2><p>中文主文案，技术术语作为副文本。</p></header><div class="help-glossary-grid">${terms.length?terms.map(term=>helpGlossaryTerm(term,mode)).join(''):empty('当前筛选下暂无术语。')}</div></section></main>${helpRightPanel(catalog,'glossary',category)}</section>`;
                }
                """)
.append("""
                async function renderHelpCenterPage(hash=currentRouteHash(),options={}){
                  waEnsureState();
                  const routeHash=String(hash||currentRouteHash()), params=parseHashParams(routeHash), f=appState.helpCenterFilters;
                  const previousList=document.querySelector('.help-topic-list');
                  const previousDoc=document.querySelector('.help-document-scroll'), previousRight=document.querySelector('.help-right-panel');
                  if(previousList&&!options.resetTopicListScroll)f.topicListScrollTop=previousList.scrollTop||0;
                  if(previousDoc)f.documentScrollTop=previousDoc.scrollTop||0;
                  if(previousRight)f.rightPanelScrollTop=previousRight.scrollTop||0;
                  if(routeHash.startsWith('#/examples'))f.view='examples';else f.view=helpNormalizeView(params.view||f.view||'docs');
                  if(routeHash==='#/help')f.view='docs';
                  if(params.topic){f.topicId=params.topic;f.view='docs';}else if(routeHash==='#/help'||routeHash==='#/examples')f.topicId='';
                  if(params.mode==='professional'||params.mode==='basic')f.mode=params.mode;
                  if(!options.silent)setView(loading('正在加载帮助中心...'));
                  let catalog;try{catalog=await loadHelpCatalog();}catch(err){if(options.silent){toast('帮助中心实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message||'帮助中心加载失败'));return;}
                  const view=helpNormalizeView(f.view), effectiveKind=helpKindForView(view);
                  const items=helpFilterItems(catalog,{kind:effectiveKind}), topicItems=helpFilterItems(catalog,{kind:'topic'}), mode=f.mode==='professional'?'professional':'basic', selected=f.topicId||params.topic||(catalog.featuredTopicIds||[])[0]||'';
                  const examples=items.filter(item=>item.kind==='example'), troubles=items.filter(item=>item.kind==='troubleshooting'), terms=items.filter(item=>item.kind==='glossary');
                  const body=view==='examples'?helpExamplesView(catalog,examples,mode,f.category):(view==='troubleshooting'?helpTroubleshootingView(catalog,troubles,mode,f.category):(view==='glossary'?helpGlossaryView(catalog,terms,mode,f.category):helpDocsView(catalog,topicItems,selected,mode,params.returnTo||'',f.category)));
                  const rendered=setView(`<section class="wa-page help-center-page" data-help-example-center-route="true" data-help-example-center-view="${esc(view)}" data-help-example-center-default-view="${view==='docs'?'true':'false'}" data-help-example-center-readonly="true" data-help-example-center-no-write-api="true" data-help-example-center-copy-only="true" data-help-example-center-no-browser-dialogs="true" data-help-example-center-responsive-stack="true" data-help-example-center-event-delegation="true" data-help-example-center-button-type-button="true" data-help-example-center-no-unsafe-inline-onclick="true" data-help-example-center-no-toolbar="true" data-help-example-center-no-topic-category-pill="true" data-help-example-center-inline-term-click-opens-topic="true" data-help-example-center-inline-term-click-not-feature-page="true" data-help-example-center-open-page-return-context-only="true" data-help-example-center-no-unexpected-end-of-input="true" data-help-example-center-no-punctuation-before-slash="true" data-help-example-center-return-context-session="true" data-help-example-center-return-context-safe-id="true" data-help-example-center-return-restore-view-topic-mode="true" data-help-example-center-return-restore-scroll="true">
                    ${waPageHead('帮助中心 / 示例中心','基础入门、专业参考、示例、排错和术语都在这里；内容只读，不写用户笔记或收藏。',waButton('刷新','refresh','data-help-refresh="true"','ghost'))}
                    ${helpViewTabs(view)}
                    ${body}
                  </section>`,options);
                  if(rendered){bindHelpCenterFilters(options.resetTopicListScroll);applyPendingHelpReturnContext();}
                }
                function bindHelpCenterFilters(resetTopicListScroll=false){
                  const list=document.querySelector('.help-topic-list');
                  if(list&&!resetTopicListScroll)list.scrollTop=Number(appState.helpCenterFilters.topicListScrollTop||0);
                  const doc=document.querySelector('.help-document-scroll'), right=document.querySelector('.help-right-panel');
                  if(doc&&!resetTopicListScroll)doc.scrollTop=Number(appState.helpCenterFilters.documentScrollTop||0);
                  if(right&&!resetTopicListScroll)right.scrollTop=Number(appState.helpCenterFilters.rightPanelScrollTop||0);
                  const search=document.getElementById('help-search');
                  if(search){
                    search.addEventListener('compositionstart',()=>{appState.helpCenterFilters.composing=true;});
                    search.addEventListener('compositionend',event=>{appState.helpCenterFilters.composing=false;updateHelpFiltersFromInputs(event,true);});
                    search.addEventListener('input',event=>{if(appState.helpCenterFilters.composing)return;updateHelpFiltersFromInputs(event,true);});
                  }
                  document.getElementById('help-category')?.addEventListener('change',event=>updateHelpFiltersFromInputs(event,true));
                }
                function updateHelpFiltersFromInputs(event,resetScroll=false){
                  const f=appState.helpCenterFilters;
                  f.search=document.getElementById('help-search')?.value||'';
                  f.category=document.getElementById('help-category')?.value||'ALL';
                  if(resetScroll)f.topicListScrollTop=0;
                  renderHelpCenterPage(currentRouteHash(),{silent:true,resetTopicListScroll:resetScroll});
                  const focusId=event?.target?.id;
                  if(focusId)restoreFocusEnd(focusId);
                }
                function handleHelpCenterDelegatedClick(event){
                  const target=event?.target;if(!target?.closest)return false;
                  const returnHelp=target.closest('[data-help-return-action]');
                  if(returnHelp){event.preventDefault();event.stopPropagation();restoreHelpReturnContext(returnHelp.dataset.helpReturnId||'');return true;}
                  const popPage=target.closest('[data-help-term-open-page]');
                  if(popPage){event.preventDefault();event.stopPropagation();openHelpInlineTermPage(popPage.dataset.termId||'');return true;}
                  const popHelp=target.closest('[data-help-term-open-help]');
                  if(popHelp){event.preventDefault();event.stopPropagation();openHelpInlineTermHelp(popHelp.dataset.termId||'');return true;}
                  const root=target.closest('.help-center-page');if(!root)return false;
                  const inlineTerm=target.closest('[data-help-inline-term]');
                  if(inlineTerm){event.preventDefault();event.stopPropagation();openHelpInlineTermDefault(inlineTerm.dataset.termId||'',inlineTerm);return true;}
                  const refresh=target.closest('[data-help-refresh]');
                  if(refresh){event.preventDefault();event.stopPropagation();loadHelpCatalog(true).then(()=>renderHelpCenterPage(currentRouteHash(),{silent:true}));return true;}
                  const mode=target.closest('[data-help-mode]');
                  if(mode){event.preventDefault();event.stopPropagation();setHelpMode(mode.dataset.helpMode||'basic');return true;}
                  const view=target.closest('[data-help-view]');
                  if(view){event.preventDefault();event.stopPropagation();setHelpView(view.dataset.helpView||'docs');return true;}
                  const category=target.closest('[data-help-category]');
                  if(category){event.preventDefault();event.stopPropagation();setHelpCategory(category.dataset.helpCategory||'ALL');return true;}
                  const topic=target.closest('[data-help-topic-id]');
                  if(topic){event.preventDefault();event.stopPropagation();navigateTo(helpViewHash('docs',topic.dataset.helpTopicId||''));return true;}
                  return false;
                }
                function setHelpMode(mode){appState.helpCenterFilters.mode=mode==='professional'?'professional':'basic';renderHelpCenterPage(currentRouteHash(),{silent:true});}
                function setHelpView(view){const normalized=helpNormalizeView(view);appState.helpCenterFilters.view=normalized;appState.helpCenterFilters.category='ALL';if(normalized!=='docs')appState.helpCenterFilters.topicId='';navigateTo(helpViewHash(normalized));}
                function setHelpCategory(category){appState.helpCenterFilters.category=category||'ALL';appState.helpCenterFilters.topicListScrollTop=0;renderHelpCenterPage(currentRouteHash(),{silent:true,resetTopicListScroll:true});}
                function detailHeader(opts){
                  const badges=(opts.badges||[]).filter(Boolean).join('');
                  const help=opts.helpTopic?pageHelpLink(opts.helpTopic,currentRouteHash()):'';
                  const actions=[helpReturnButton(),help,...(opts.actions||[])].filter(Boolean).join('');
                  const copy=opts.copyValue?waIconButton('复制 ID','copy',htmlHandler(`copyTextToClipboard(${jsString(opts.copyValue)})`)):'';
                  return `<header class="wa-detail-head">
                    <div class="wa-detail-title-wrap">
                      ${opts.back?`<div class="back-row compact">${opts.back}</div>`:''}
                      <div class="wa-detail-title-line">
                        <span class="wa-detail-icon icon-bubble-${iconClassName(opts.iconName||'dashboard')}">${icon(opts.iconName||'dashboard')}</span>
                        <div class="wa-detail-title-copy">
                          <div class="wa-detail-kicker">${esc(opts.kicker||'WebAdmin 7.5')}</div>
                          <h1>${esc(opts.title||'详情')}</h1>
                          <p>${esc(opts.subtitle||'')}</p>
                          <div class="wa-detail-badges">${badges}${copy}</div>
                        </div>
                      </div>
                    </div>
                    <div class="wa-detail-actions">${actions}</div>
                  </header>`;
                }
                function detailTabs(labels,active='基本信息'){
                  return `<nav class="wa-detail-tabs wa-tabs-scroll" data-detail-tabs="true" data-responsive-tabs="true" role="tablist" aria-label="详情页分栏">${labels.map(label=>`<button class="wa-detail-tab ${label===active?'active':''}" type="button" role="tab" aria-selected="${label===active?'true':'false'}" ${htmlHandler(`scrollDetailSection(${jsString(label)})`)}>${esc(label)}</button>`).join('')}</nav>`;
                }
                function scrollDetailSection(label){
                  const view=appView();
                  if(!view||!view.querySelectorAll)return;
                  const key=String(label||'').replace(/列表|信息|统计|检查/g,'').trim();
                  const cards=Array.from(view.querySelectorAll('.wa-detail-card'));
                  const target=cards.find(card=>String(card.textContent||'').includes(label)||key&&String(card.textContent||'').includes(key));
                  if(target&&target.scrollIntoView)target.scrollIntoView({behavior:'smooth',block:'start'});
                }
                function detailCard(title,body,actions='',extraClass=''){
                  return `<article class="wa-detail-card ${esc(extraClass)}"><header class="wa-detail-card-head"><h2>${esc(title)}</h2>${actions?`<div class="wa-detail-card-actions">${actions}</div>`:''}</header><div class="wa-detail-card-body">${body}</div></article>`;
                }
                function detailInfoGrid(rows){
                  const html=(rows||[]).filter(row=>row&&row.length>=2).map(([key,value])=>`<div class="k">${esc(key)}</div><div class="v">${detailValue(value)}</div>`).join('');
                  return `<div class="identity-grid wa-detail-info">${html||'<div class="muted">暂无详情字段。</div>'}</div>`;
                }
                function detailStatGrid(items){
                  return `<div class="wa-detail-stat-grid">${(items||[]).map(item=>`<div class="wa-detail-stat ${esc(item.kind||'')}"><span class="wa-detail-stat-icon">${icon(item.icon||'status')}</span><span class="wa-detail-stat-label">${esc(item.label)}</span><strong>${esc(item.value??'--')}</strong>${item.sub?`<small>${esc(item.sub)}</small>`:''}</div>`).join('')}</div>`;
                }
                function detailConsumerGrid(items){
                  return `<div class="wa-detail-consumers">${(items||[]).map(item=>`<div class="wa-detail-consumer"><span class="wa-detail-consumer-icon">${icon(item.icon||'signal')}</span><span><strong>${esc(item.label)}</strong><small>${esc(item.value??0)}</small></span>${item.target?`<button class="wa-btn ghost" ${navigationAttr(item.target,false)}>查看</button>`:`<button class="wa-btn ghost" disabled>查看</button>`}</div>`).join('')}</div>`;
                }
                function detailSideStack(cards){return `<div class="wa-detail-side-stack" data-detail-right-rail="true">${(cards||[]).filter(Boolean).join('')}</div>`;}
                function detailFixedLayout(leftCards=[],rightCards=[],bottomCards=[]){
                  const left=(leftCards||[]).filter(Boolean).join('');
                  const right=(rightCards||[]).filter(Boolean).join('');
                  const bottom=(bottomCards||[]).filter(Boolean).join('');
                  return `<section class="wa-detail-second-row detail-fixed-layout" data-detail-layout="fixed-two-column" data-detail-row="main" data-detail-two-column="true"><div class="detail-column detail-column-left" data-detail-column="left">${left}</div><div class="detail-column detail-column-right" data-detail-column="right">${right}</div></section>${bottom?`<section class="detail-full-width-stack" data-detail-bottom-full-width="true">${bottom}</section>`:''}`;
                }
                function detailTableWrap(html){return `<div class="wa-table-scroll wa-detail-table-scroll" data-table-row-stretch="false">${html}</div>`;}
                function compactEventList(items,emptyText='暂无最近事件。'){
                  if(!items||items.length===0)return empty(emptyText);
                  return `<div class="wa-compact-list">${items.slice(0,5).map(e=>`<div class="wa-compact-row"><strong>${fmtTime(e.time||e.triggeredAt||e.lastTriggeredAt||e.executedAt)}</strong><span>${esc(e.channel||e.type||e.result||'事件')}</span><small>${esc(e.source||e.sourceType||e.playerName||e.detail||'')}</small></div>`).join('')}</div>`;
                }
                function detailToolGrid(items){
                  return `<div class="wa-quick-grid">${(items||[]).map(item=>waButton(item.label,item.icon||'',item.attrs||'disabled',item.kind||'ghost')).join('')}</div>${items&&items.note?`<p class="wa-disabled-note">${esc(items.note)}</p>`:''}`;
                }
                function advancedDetailKey(kind,id){return `${kind}:${String(id||'')}`;}
                function isAdvancedDetailOpen(kind,id){waEnsureState();return !!appState.advancedDetailOpen[advancedDetailKey(kind,id)];}
                async function toggleAdvancedDetail(kind,id){
                  waEnsureState();
                  const key=advancedDetailKey(kind,id);
                  appState.advancedDetailOpen[key]=!appState.advancedDetailOpen[key];
                  await route({silent:true});
                }
                function advancedValue(value){
                  if(value===null||value===undefined||value==='')return '暂无';
                  if(typeof value==='object'){try{return JSON.stringify(value);}catch(_){return String(value);}}
                  return String(value);
                }
                function advancedRowsFromObject(obj,prefix='',limit=48){
                  const rows=[];
                  const visit=(value,path,depth)=>{
                    if(rows.length>=limit||value===null||value===undefined)return;
                    if(typeof value==='object'&&!Array.isArray(value)&&depth<2){
                      Object.keys(value).sort().forEach(key=>visit(value[key],path?`${path}.${key}`:key,depth+1));
                      return;
                    }
                    rows.push([path,advancedValue(value)]);
                  };
                  visit(obj,prefix,0);
                  return rows;
                }
                function advancedTable(rows){
                  const clean=(rows||[]).filter(r=>r&&r.length>=2);
                  if(clean.length===0)return '<div class="muted">暂无额外字段。</div>';
                  return `<div class="wa-advanced-table">${clean.map(([key,value])=>`<div class="k">${esc(key)}</div><div class="v"><code>${esc(advancedValue(value))}</code></div>`).join('')}</div>`;
                }
                function advancedDetailCard(kind,id,summaryRows=[],groups=[]){
                  const open=isAdvancedDetailOpen(kind,id), button=open?'收起详情':'显示全部详情';
                  const preview=summaryRows.slice(0,6);
                  const body=open?`<div class="wa-advanced-groups">${groups.map(group=>`<section class="wa-advanced-group"><h3>${esc(group.title)}</h3>${advancedTable(group.rows)}</section>`).join('')||advancedTable(summaryRows)}</div>`:advancedTable(preview);
                  return `<article class="wa-detail-card wa-advanced-card detail-bottom-full ${open?'is-open':'is-closed'}" data-collapsible-detail="true" data-detail-bottom-card="advanced" data-detail-full-width="true" data-advanced-open="${open?'true':'false'}"><header class="wa-detail-card-head"><h2>完整详情</h2><button class="wa-btn ghost" type="button" ${htmlHandler(`toggleAdvancedDetail(${jsString(kind)},${jsString(id)})`)}>${esc(button)}</button></header><div class="wa-advanced-body">${body}</div></article>`;
                }
                function waSelect(id,options,value,labeler=(v)=>v,attrs=''){
                  return `<select class="select" id="${esc(id)}" ${String(attrs||'')}>${options.map(o=>`<option value="${esc(o)}" ${String(o)===String(value)?'selected':''}>${esc(labeler(o))}</option>`).join('')}</select>`;
                }
                function waPageItems(key,items,pageSize=10){
                  waEnsureState();
                  const total=items.length, pages=Math.max(1,Math.ceil(total/pageSize)), current=Math.min(pages,Math.max(1,Number(appState.uiPages[key]||1)));
                  appState.uiPages[key]=current;
                  return {items:items.slice((current-1)*pageSize,current*pageSize),total,pages,current,pageSize};
                }
                function handlePaginationAction(event){
                  const target=event.target;
                  const button=target&&target.closest?target.closest('[data-action="wa-pagination-page"]'):null;
                  if(!button)return false;
                  event.preventDefault();
                  event.stopPropagation();
                  if(button.disabled)return true;
                  const key=button.dataset.pageKey;
                  const page=Number(button.dataset.page);
                  if(!key||!Number.isFinite(page))return true;
                  setWaPage(key,page);
                  return true;
                }
                function setWaPage(key,page){
                  waEnsureState();
                  appState.uiPages[key]=Math.max(1,Number(page)||1);
                  if(key==='signalbridge')renderSignalList('');
                  if(key==='receivers')renderReceiverList('');
                  if(key==='listeners')renderListenerList('');
                  if(key.startsWith('listenerDetail:'))renderSignalListenerDetail(key.substring('listenerDetail:'.length),{silent:true});
                  if(key==='devices')renderDeviceList('');
                  if(key==='virtualBlockDevices')renderVirtualBlockList('');
                  if(key==='actions')renderActionList('');
                  if(key==='actionTemplates')renderActionTemplateList('');
                  if(key==='templates')renderTemplateList('');
                  if(key==='history')renderHistoryListPage('');
                  if(key==='doctor')renderDoctorList('');
                  if(key==='config')renderConfigList('');
                  if(key==='users')renderUserList('');
                  if(key==='regions')renderRegionList('');
                  if(key==='regionControllers')renderRegionControllerList('');
                  if(key==='logicChains')renderLogicChainList('');
                  if(key==='conditionGroups')renderConditionGroupList('');
                  if(key==='conditionDebugger')renderConditionDebuggerList('');
                  if(key==='timers')renderTimerList('',{silent:true});
                }
                function waPagination(key,page){
                  if(page.total<=page.pageSize)return `<div class="wa-pagination"><span class="wa-page-meta">共 ${esc(page.total)} 条 · 每页 ${esc(page.pageSize)} 条</span></div>`;
                  const nums=[1,2,3].filter(n=>n<=page.pages);
                  if(page.pages>4)nums.push('…');
                  if(page.pages>3)nums.push(page.pages);
                  const prev=Math.max(1,page.current-1), next=Math.min(page.pages,page.current+1);
                  const pageButton=(label,targetPage,active=false,disabled=false,ariaLabel='')=>`<button type="button" class="wa-page-btn ${active?'active':''}" data-action="wa-pagination-page" data-page-key="${esc(key)}" data-page="${esc(targetPage)}" ${active?'aria-current="page"':''} ${disabled?'disabled':''} aria-label="${esc(ariaLabel||`第 ${targetPage} 页`)}">${esc(label)}</button>`;
                  return `<div class="wa-pagination" data-shared-pagination-helper="true"><div class="wa-page-buttons">${pageButton('‹',prev,false,page.current<=1,'上一页')}${nums.map(n=>n==='…'?`<span class="wa-page-meta">…</span>`:pageButton(n,n,n===page.current,false,`第 ${n} 页`)).join('')}${pageButton('›',next,false,page.current>=page.pages,'下一页')}</div><span class="wa-page-meta">共 ${esc(page.total)} 条</span><span class="wa-page-meta">每页 ${esc(page.pageSize)} 条</span></div>`;
                }
                function modalSnapshot(kind,draft){
                  const k=String(kind||'');
                  if(!draft)return '';
                  if(k==='device_metadata')return JSON.stringify({displayName:String(draft.displayName||''),note:String(draft.note||''),iconKey:String(draft.iconKey||'auto')});
                  if(k==='device_basic_config')return JSON.stringify({enabled:!!draft.enabled,channel:normalizeChannelName(draft.channel)});
                  if(k==='device_extended_config'){const values={}, clear={};(draft.supportedFields||Object.keys(draft.values||{})).forEach(field=>{const value=(draft.values||{})[field];values[field]=isExtendedTickField(field)?Number(value||0):(isExtendedChannelField(field)?normalizeChannelName(value):String(value??''));if((draft.clear||{})[field])clear[field]=true;});return JSON.stringify({values,clear});}
                  if(k==='action_relay_actions')return JSON.stringify({actions:actionRelayActionsEditableJson(draft.actions||[]),conditionGroupId:String(draft.conditionGroupId||'')});
                  if(k==='vbd_native_triggers')return vbdNativeTriggerEditableJson(draft);
                  if(k==='interaction_item_matcher')return interactionItemMatcherEditableJson(draft);
                  if(k==='channel_metadata')return JSON.stringify({displayName:String(draft.displayName||''),note:String(draft.note||''),iconKey:String(draft.iconKey||'auto')});
                  if(k==='logic_chain_metadata')return JSON.stringify({chainId:String(draft.chainId||''),displayName:String(draft.displayName||''),note:String(draft.note||''),iconKey:String(draft.iconKey||'auto'),tags:String(draft.tags||''),group:String(draft.group||''),rootType:String(draft.rootType||'channel'),rootRef:String(draft.rootType||'channel').toLowerCase()==='channel'?normalizeLogicChainDraftChannel(draft.rootRef):String(draft.rootRef||'').trim(),includeDisabled:draft.includeDisabled!==false,maxDepth:Number(draft.maxDepth||3),layoutPreference:String(draft.layoutPreference||'auto')});
                  if(k==='signal_listener_basic_config')return JSON.stringify({enabled:!!draft.enabled,channel:normalizeChannelName(draft.channel),cooldownTicks:Number(draft.cooldownTicks||0),conditionGroupId:String(draft.conditionGroupId||'')});
                  if(k==='signal_join_config')return JSON.stringify({id:String(draft.id||''),displayName:String(draft.displayName||''),note:String(draft.note||''),enabled:draft.enabled!==false,inputChannels:(draft.inputChannels||[]).map(i=>({channel:normalizeChannelName(i.channel),displayName:String(i.displayName||''),note:String(i.note||'')})),outputChannel:normalizeChannelName(draft.outputChannel),mode:String(draft.modeValue||'ALL'),threshold:Number(draft.threshold||0),scopeMode:String(draft.scopeMode||'GLOBAL'),resetPolicy:String(draft.resetPolicy||'RESET_AFTER_EMIT'),timeoutTicks:Number(draft.timeoutTicks||0),cooldownTicks:Number(draft.cooldownTicks||0)});
                  if(k==='timer_config')return JSON.stringify({id:String(draft.id||''),displayName:String(draft.displayName||''),note:String(draft.note||''),enabled:draft.enabled!==false,mode:String(draft.modeValue||'DELAY'),scopeMode:String(draft.scopeMode||'GLOBAL'),durationTicks:Number(draft.durationTicks||0),intervalTicks:Number(draft.intervalTicks||0),maxRuns:Number(draft.maxRuns||0),startPolicy:String(draft.startPolicy||'RESTART'),outputChannel:normalizeChannelName(draft.outputChannel),onStartActions:(draft.onStartActions||[]).map(actionDraftPayload),onTickActions:(draft.onTickActions||[]).map(actionDraftPayload),onCompleteActions:(draft.onCompleteActions||[]).map(actionDraftPayload),onCancelActions:(draft.onCancelActions||[]).map(actionDraftPayload)});
                  if(k==='signal_listener_action')return JSON.stringify(actionDraftPayload(draft));
                  if(k==='signal_listener_create')return JSON.stringify({name:String(draft.name||''),displayName:String(draft.displayName||draft.name||''),note:String(draft.note||''),channel:normalizeChannelName(draft.channel),enabled:draft.enabled!==false,cooldownTicks:Number(draft.cooldownTicks||0),conditionGroupId:String(draft.conditionGroupId||'')});
                  if(k==='state_variable_definition')return JSON.stringify({mode:String(draft.mode||''),scope:String(draft.scope||'GLOBAL'),targetId:String(draft.targetId||''),key:String(draft.key||''),type:String(draft.type||'STRING'),value:String(draft.value??''),displayName:String(draft.displayName||''),note:String(draft.note||'')});
                  if(k==='region_controller_config')return JSON.stringify({enabled:draft.enabled!==false,name:String(draft.name||''),regionId:String(draft.regionId||''),targetFilterType:String(draft.targetFilterType||'ALL'),targetFilterValue:String(draft.targetFilterValue||''),stayIntervalTicks:Number(draft.stayIntervalTicks||0),enterConditionGroupId:String(draft.enterConditionGroupId||''),exitConditionGroupId:String(draft.exitConditionGroupId||''),stayConditionGroupId:String(draft.stayConditionGroupId||'')});
                  if(k==='region_controller_action')return JSON.stringify({trigger:String(draft.trigger||''),...actionDraftPayload(draft)});
                  if(k==='condition_group')return JSON.stringify({id:String(draft.id||''),displayName:String(draft.displayName||''),note:String(draft.note||''),iconKey:String(draft.iconKey||''),enabled:draft.enabled!==false,tags:String(draft.tagsText||''),root:draft.groupDefinition?.root||{}});
                  if(k==='selection_create_virtual_block')return JSON.stringify({targetPlayerName:String(draft.targetPlayerName||'').trim(),channel:normalizeChannelName(draft.channel),displayName:String(draft.displayName||'').trim(),note:String(draft.note||''),iconKey:String(draft.iconKey||'auto'),enabled:draft.enabled!==false});
                  if(k==='template_import')return JSON.stringify({packageJson:String(draft.packageJson||''),importedTemplateId:String(draft.importedTemplateId||''),importedDisplayName:String(draft.importedDisplayName||'')});
                  if(k==='template_apply')return JSON.stringify({source:String(draft.source||''),templateId:String(draft.templateId||''),prefix:normalizeTemplatePrefix(draft.prefix),displayNamePrefix:String(draft.displayNamePrefix||''),rootChannel:normalizeChannelName(draft.rootChannel),placeholderMappings:draft.placeholderMappings||{}});
                  return JSON.stringify(draft);
                }
                function markModalInitialSnapshot(kind,draft){if(draft&&!draft.initialSnapshot)draft.initialSnapshot=modalSnapshot(kind,draft);}
                function modalDraftDirty(kind,draft){if(!draft)return false;const current=modalSnapshot(kind,draft);if(!draft.initialSnapshot){draft.initialSnapshot=current;return false;}return current!==draft.initialSnapshot;}
                function syncModalDraftBeforeClose(kind,id){
                  const k=String(kind||'');
                  if(k==='device_metadata'){const d=appState.deviceMetadataEdit;if(d){d.displayName=document.getElementById('metadata-display-name')?.value||'';d.note=document.getElementById('metadata-note')?.value||'';d.iconKey=document.getElementById('metadata-icon')?.value||'auto';}}
                  if(k==='device_basic_config')updateDeviceBasicConfigDraftFromForm(id,false);
                  if(k==='device_extended_config')updateDeviceExtendedConfigDraftFromForm(id);
                  if(k==='action_relay_actions')syncActionRelayActionsDraftFromForm(id);
                  if(k==='vbd_native_triggers')syncVbdNativeTriggerDraftFromForm(id);
                  if(k==='interaction_item_matcher')syncInteractionItemMatcherDraftFromForm(id);
                  if(k==='device_config')applyDeviceConfigDraftsFromForm(id);
                  if(k==='channel_metadata'){const d=appState.channelMetadataEdit;if(d){d.displayName=document.getElementById('channel-metadata-display-name')?.value||'';d.note=document.getElementById('channel-metadata-note')?.value||'';d.iconKey=document.getElementById('channel-metadata-icon')?.value||'auto';}}
                  if(k==='signal_listener_basic_config')updateSignalListenerBasicConfigDraftFromForm(id,false);
                  if(k==='signal_listener_action')syncSignalListenerActionDraft();
                  if(k==='region_controller_action')syncRegionControllerActionDraft();
                  if(k==='signal_listener_create')updateSignalListenerCreateDraftFromForm(false);
                  if(k==='state_variable_definition')syncStateVariableEditDraftFromForm();
                  if(k==='selection_create_virtual_block'){const d=appState.selectionCreateVirtualBlock;if(d&&d.step==='config')appState.selectionCreateVirtualBlock=selectionDraftFromForm();}
                  if(k==='template_import')syncTemplateImportDraft();
                  if(k==='template_apply')syncTemplateApplyDraft();
                }
                function isDeviceConfigModalDirty(deviceId){
                  const meta=appState.deviceMetadataEdit,basic=appState.deviceBasicConfigEdit,ext=appState.deviceExtendedConfigEdit,actions=appState.actionRelayActionsEdit,native=appState.vbdNativeTriggerEdit,matcher=appState.interactionItemMatcherEdit;
                  return !!((meta&&meta.deviceId===deviceId&&modalDraftDirty('device_metadata',meta))||(basic&&basic.deviceId===deviceId&&modalDraftDirty('device_basic_config',basic))||(ext&&ext.deviceId===deviceId&&modalDraftDirty('device_extended_config',ext))||(actions&&actions.deviceId===deviceId&&modalDraftDirty('action_relay_actions',actions))||(native&&sameDeviceRef(native.deviceId,deviceId)&&modalDraftDirty('vbd_native_triggers',native))||(matcher&&sameDeviceRef(matcher.deviceId,deviceId)&&modalDraftDirty('interaction_item_matcher',matcher)));
                }
                function cancelDiscardModalClose(){
                  appState.modalDiscardConfirmOpen=false;
                  const layer=document.getElementById('wa-discard-confirm');
                  if(layer)layer.remove();
                }
                function confirmDiscardModalClose(){
                  cancelDiscardModalClose();
                  closeWebAdminModal(true,true);
                }
                function openDiscardChangesConfirm(){
                  const root=document.getElementById('wa-modal-root');
                  if(!root||appState.modalDiscardConfirmOpen)return;
                  appState.modalDiscardConfirmOpen=true;
                  const layer=document.createElement('div');
                  layer.id='wa-discard-confirm';
                  layer.className='wa-discard-confirm-layer';
                  layer.setAttribute('data-discard-confirm-modal','true');
                  layer.onclick=event=>{if(event.target===layer)cancelDiscardModalClose();};
                  layer.innerHTML=`<section class="wa-discard-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="wa-discard-confirm-title" onclick="event.stopPropagation()"><header><h3 id="wa-discard-confirm-title">未保存的修改</h3></header><p>当前编辑内容尚未保存。关闭后这些修改会丢失。</p><footer><button class="wa-btn ghost" type="button" onclick="cancelDiscardModalClose()">继续编辑</button><button class="wa-btn danger" type="button" onclick="confirmDiscardModalClose()">放弃修改并关闭</button></footer></section>`;
                  root.appendChild(layer);
                }
                function openWebAdminModal(title,body,footer='',options={}){
                  const existing=document.getElementById('wa-modal-root');
                  const existingClosing=existing&&String(existing.className||'').includes('closing');
                  const wrap=existing&&!existingClosing?existing:document.createElement('div');
                  if(existing&&existingClosing)existing.remove();
                  appState.modalDismissPromise=null;
                  wrap.className='wa-modal-backdrop';
                  wrap.id='wa-modal-root';
                  appState.modalCloseHandler=options.onClose||null;
                  appState.modalDirtyChecker=options.dirtyCheck||null;
                  appState.modalSyncBeforeClose=options.syncBeforeClose||null;
                  appState.modalDiscardConfirmOpen=false;
                  const closeAttr=options.closeAttr||'onclick="closeWebAdminModal()"';
                  wrap.innerHTML=`<section class="wa-modal wa-modal-viewport ${esc(options.className||'')}" role="dialog" aria-modal="true" aria-labelledby="wa-modal-title"><header class="wa-modal-head"><h2 id="wa-modal-title" class="wa-modal-title">${esc(title)}</h2>${waIconButton('关闭','close',closeAttr)}</header><div class="wa-modal-body">${body}</div><footer class="wa-modal-foot">${footer||waButton('关闭','',closeAttr,'ghost')}</footer></section>`;
                  wrap.onclick=event=>{if(event.target===wrap)closeWebAdminModal();};
                  if(!existing||existingClosing)document.body.appendChild(wrap);
                }
                function closeWebAdminModal(runHandler=true,force=false){
                  if(appState.modalClosePromise)return appState.modalClosePromise;
                  if(!force&&appState.modalDiscardConfirmOpen){cancelDiscardModalClose();return Promise.resolve(false);}
                  if(!force&&runHandler&&typeof appState.modalSyncBeforeClose==='function')appState.modalSyncBeforeClose();
                  if(!force&&runHandler&&typeof appState.modalDirtyChecker==='function'&&appState.modalDirtyChecker()){openDiscardChangesConfirm();return Promise.resolve(false);}
                """).toString();
    }
}
