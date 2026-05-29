package com.zcpu.tzzmod.webadmin;

// Draft overlay 模块把 editor draft state 叠加到已计算好的 layout 上，生成仅用于预览的节点、
// 边、slot 和 anchor。它不调用 API、不写 store、不提交 graph document；所有正式写入仍由
// Logic Chain save payload 和后端 typed service 校验执行。pending/delete/protected draft 等
// 状态只能作为前端视觉或草稿边界存在，不能泄漏到 runtime 语义。
final class WebAdminLogicChainDraftOverlayScripts {
    private WebAdminLogicChainDraftOverlayScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                function logicChainLayoutWithDraft(layout,nodes,graph){
                """)
                .append("""
                  const editor=appState.logicChainEditor||{};
                """)
                .append("""
                  if(!layout||!editor.active||(!(editor.nodes||[]).length&&!editor.actionAppend&&!(editor.draftChannels||[]).length))return layout;
                """)
                .append("""
                  const metrics=logicChainDraftMetrics(layout),byId={},draftAliases={},hasDraftNode=(editor.nodes||[]).length>0,draftNode=logicChainActiveDraftNode(editor)||{};
                """)
                .append("""
                  layout.edges=layout.edges||[];
                """)
                .append("""
                  layout.flat.forEach(item=>{byId[item.node?.id]=item;});
                """)
                .append("""
                  editor.visibleNodeAnchors={};
                """)
                .append("""
                  editor.visibleChannelAnchors={};
                """)
                .append("""
                  layout.flat.forEach(item=>{const node=item.node||{},nodeId=String(node.id||''),channel=logicChainChannelIdForItem(item),col=Number(item.depth??item.lane??item.rawLane??0),slot=logicChainSlotFromY(metrics,item.y),anchor={nodeId,channel,col,slot,x:Number(item.x||0),y:Number(item.y||0),reference:node.metadata?.isReferenceCard===true||node.metadata?.nodeKind==='reference'};if(nodeId)editor.visibleNodeAnchors[nodeId]=anchor;if(channel){const current=editor.visibleChannelAnchors[channel];if(!current||current.reference&&!anchor.reference)editor.visibleChannelAnchors[channel]=anchor;}});
                """)
                .append("""
                  const draftType=String(draftNode.type||'signal_join').toLowerCase(),draftCol=logicChainColumnIndex(draftNode.column,draftType),previewColumn=String(editor.previewColumn||''),hasSlotPreview=previewColumn&&Number(editor.previewSlot)>=0,previewSlot=editor.drag?.active&&hasSlotPreview?Number(editor.previewSlot):Number(draftNode.slot||0),resolvedSlot=logicChainResolveDraftSlot(layout,draftCol,previewSlot,draftType,draftNode),canReserveDraftSlot=resolvedSlot!=null,draftY=logicChainSlotY(metrics,canReserveDraftSlot?resolvedSlot:0),reserveDraftSlot=canReserveDraftSlot&&(!!draftNode.placed||!!(editor.drag?.active&&hasSlotPreview));
                """)
                .append("""
                  editor.draftMetrics=metrics;
                """)
                .append("""
                  editor.legalSlots={};
                """)
                .append("""
                  logicChainDraftPlacementColumns(layout,draftType).forEach(col=>{editor.legalSlots[`C${col}`]=logicChainLegalSlotsForColumn(layout,col,draftType);});
                """)
                .append("""
                  if(hasDraftNode&&reserveDraftSlot)logicChainMakeRoomForDraft(layout,draftCol,draftY,metrics);
                """)
                .append("""
                  function draftChannelInfo(channel){return (editor.draftChannels||[]).find(c=>String(c.channel||'')===String(channel||''))||{};}
                """)
                .append("""
                  function channelReferenceItem(id,edgeType,side,preferredCol,anchorSlot=0,draftContextNode=draftNode){
                """)
                .append("""
                     if(!isChannelNodeId(id))return null;
                """)
                .append("""
                     const channel=id.substring('channel:'.length),key=`${id}:${edgeType}:${side}:${preferredCol}`;
                """)
                .append("""
                     if(draftAliases[key])return draftAliases[key];
                """)
                .append("""
                      const primary=byId[id]||null,info=draftChannelInfo(channel),col=Math.max(0,Number(preferredCol||3)),slot=logicChainNearestFreeSlotForColumn(layout,col,anchorSlot),aliasId=`reference:draft:${draftContextNode?.id||draftNode.id||'node'}:${edgeType}:${side}:${channel}`;
                """)
                .append("""
                     layout.referenceCardNearDraft=true;layout.referenceSlotNoOverlap=true;if(side==='downstream'){layout.outputReferenceRightSide=true;layout.visualUpstreamNonInputOutputReference=true;}if(side==='upstream')layout.inputReferenceLeftSide=true;
                """)
                .append("""
                     const item={node:{id:aliasId,type:'channel',refType:'channel',refId:channel,label:info.displayName||primary?.node?.label||channel,subtitle:`草稿频道引用 · ${side==='upstream'?'输入频道':'输出频道'}`,channel,enabled:true,status:'DRAFT',doctorStatus:'INFO',lastEvent:'',detailRoute:`#/signals/${encodeURIComponent(channel)}`,metadata:{draftEndpoint:true,nodeKind:'reference',isReferenceCard:true,primaryNodeId:id,canonicalNodeId:id,readOnly:true,visualOnly:true,nonTraversal:true,referenceReason:`draft_${edgeType}_${side}_lane`,aliasKind:'draft_channel_reference',draftReferenceCard:true,saveUsesCanonicalChannel:true,noDownstreamExpansion:true,referenceCardNecessaryOnly:true,referenceLocalNearDraft:true,referenceSlotNoOverlap:true,outputReferenceRightSide:side==='downstream',inputReferenceLeftSide:side==='upstream',visualUpstreamNonInputOutputReference:side==='downstream'}},children:[],childCount:0,collapsed:false,cycle:false,depth:col,x:logicChainColumnX(metrics,col),y:logicChainSlotY(metrics,slot),w:metrics.nodeWidth,h:metrics.nodeHeight,lane:col,rawLane:col,layoutVersion:'v2-join-lanes',draft:true};
                """)
                .append("""
                    layout.flat.push(item);
                """)
                .append("""
                    byId[aliasId]=item;
                """)
                .append("""
                    draftAliases[key]=item;
                """)
                .append("""
                    return item;
                """)
                .append("""
                  }
                """)
                .append("""
                  function logicChainFindReusableChannelEndpoint(id,side,draftItem,anchorSlot){if(!isChannelNodeId(id)||!draftItem)return null;const channel=id.substring('channel:'.length),draftLeft=Number(draftItem.x||0),draftRight=draftLeft+Number(draftItem.w||metrics.nodeWidth);const candidates=(layout.flat||[]).filter(item=>{const node=item.node||{},m=node.metadata||{},type=String(node.type||'').toLowerCase(),itemChannel=String(node.channel||node.refId||'');if(itemChannel!==channel)return false;return type==='channel'||type==='downstream_channel'||String(node.id||'').startsWith('channel:')||m.isReferenceCard===true||m.nodeKind==='reference'||m.draftEndpoint===true;}).map(item=>{const left=Number(item.x||0),right=left+Number(item.w||metrics.nodeWidth),slot=logicChainSlotFromY(metrics,item.y),isRef=!!(item.node?.metadata?.isReferenceCard||item.node?.metadata?.nodeKind==='reference');return {item,slotDistance:Math.abs(slot-anchorSlot),xDistance:side==='upstream'?Math.abs(draftLeft-right):Math.abs(left-draftRight),reference:isRef};}).filter(c=>side==='upstream'?Number(c.item.x||0)+Number(c.item.w||metrics.nodeWidth)<=draftLeft:Number(c.item.x||0)>=draftRight);candidates.sort((a,b)=>a.slotDistance-b.slotDistance||a.xDistance-b.xDistance||(a.reference?1:0)-(b.reference?1:0));return candidates[0]?.item||null;}
                """)
                .append("""
                  function logicChainResolveDraftVisualEndpoint(id,edgeType,side,preferredCol,draftContextNode=draftNode,draftContextItem=null){const draftItem=draftContextItem||byId[draftContextNode?.id]||byId[draftNode.id]||null,anchorSlot=draftItem?logicChainSlotFromY(metrics,draftItem.y):Number(draftContextNode?.slot||draftNode.slot||0),reusable=logicChainFindReusableChannelEndpoint(id,side,draftItem,anchorSlot);if(reusable){layout.sameSidePrimaryNoReference=true;if(reusable.node?.metadata?.isReferenceCard)layout.sameSideReferenceNoDuplicate=true;return reusable;}return channelReferenceItem(id,edgeType,side,preferredCol,anchorSlot,draftContextNode);}
                """)
                .append("""
                  function logicChainNearestFreeSlotForColumnIgnoring(layout,col,anchorSlot,ignoreId){const metrics=logicChainDraftMetrics(layout),anchor=Math.max(0,Number(anchorSlot||0)),free=slot=>{const rect=logicChainSlotRect(metrics,col,slot);return !logicChainColumnItems(layout,col).some(item=>String(item.node?.id||'')===String(ignoreId||'')?false:logicChainRectsOverlap(rect,{x:Number(item.x||0),y:Number(item.y||0),w:Number(item.w||metrics.nodeWidth),h:Number(item.h||metrics.nodeHeight)},0));};for(let radius=0;radius<80;radius++){const down=anchor+radius,up=anchor-radius;if(down>=0&&free(down))return down;if(radius>0&&up>=0&&free(up))return up;}return logicChainFirstFreeSlotForColumn(layout,col);}
                """)
                .append("""
                  function logicChainDraftChannelColumnValue(value,fallback){const text=String(value??'').toUpperCase();if(text.startsWith('C'))return Math.max(0,Number(text.substring(1))||0);const n=Number(value);return Number.isFinite(n)?Math.max(0,n):fallback;}
                """)
                .append("""
                  function logicChainDefaultDraftChannelAnchor(index=0){
                """)
                .append("""
                    const focus=String(appState.logicChainCanvas.routeInfo?.focusChannel||graph?.stats?.focusChannel||graph?.metadata?.rootChannel||graph?.root?.channel||'').trim(), rootChannel=String(graph?.metadata?.rootChannel||graph?.root?.channel||'').trim(), rootId=String(layout?.root?.node?.id||graph?.root?.id||''), selectedId=String(appState.logicChainCanvas.selectedNodeId||''), channels=(layout.flat||[]).filter(item=>logicChainChannelIdForItem(item));
                """)
                .append("""
                    const channelId=item=>String(logicChainChannelIdForItem(item)||''), isReference=item=>item?.node?.metadata?.isReferenceCard===true||item?.node?.metadata?.nodeKind==='reference';
                """)
                .append("""
                    const sorted=list=>(list||[]).slice().sort((a,b)=>Number(a.depth??a.lane??a.rawLane??99)-Number(b.depth??b.lane??b.rawLane??99)||(isReference(a)?1:0)-(isReference(b)?1:0)||Number(a.x||0)-Number(b.x||0)||Number(a.y||0)-Number(b.y||0));
                """)
                .append("""
                    const focusItem=sorted(channels.filter(item=>item.node?.metadata?.isFocusChannel===true||(focus&&channelId(item)===focus)))[0], rootItem=sorted(channels.filter(item=>String(item.node?.id||'')===rootId||(rootChannel&&channelId(item)===rootChannel)))[0], leftChannel=sorted(channels)[0], selected=channels.find(item=>String(item.node?.id||'')===selectedId), anchor=focusItem||rootItem||leftChannel||selected||layout.root;
                """)
                .append("""
                    const col=Math.max(0,Number(anchor?.depth??anchor?.lane??anchor?.rawLane??1)), baseSlot=logicChainSlotFromY(metrics,anchor?.y??metrics.margin);
                """)
                .append("""
                    layout.draftChannelDefaultUnderFocusChannel=true;
                """)
                .append("""
                    return {col,slot:baseSlot+1+Number(index||0)};
                """)
                .append("""
                  }
                """)
                .append("""
                  (editor.draftChannels||[]).filter(info=>info?.cardDraft).forEach((info,index)=>{
                """)
                .append("""
                    const channel=normalizeLogicChainDraftChannel(info.channel);
                """)
                .append("""
                    if(!channel)return;
                """)
                .append("""
                    const canonical=logicChainDraftChannelNodeId(channel),existing=byId[canonical]||null,explicitColumn=info.column!==undefined&&info.column!==null&&String(info.column)!=='',fallback=logicChainDefaultDraftChannelAnchor(index),col=explicitColumn?logicChainDraftChannelColumnValue(info.column,fallback.col):fallback.col,slot=logicChainNearestFreeSlotForColumnIgnoring(layout,col,Number(info.slot??fallback.slot),canonical),node=logicChainDraftChannelGraphNode({...info,channel});
                """)
                .append("""
                    node.id=canonical;
                """)
                .append("""
                    node.metadata={...(node.metadata||{}),draft:true,channelEndpointDraft:true,cardDraft:true,canonicalNodeId:canonical,primaryNodeId:canonical,singleDraftEndpointCard:true,logicChainChannelEndpointNoDuplicateCard:true,directDownstreamOfJoin:info.directDownstreamOfJoin===true,adjacentToJoinOutput:info.adjacentToJoinOutput===true,noForcedDraftOutputC3Gap:info.noForcedDraftOutputC3Gap===true};
                """)
                .append("""
                    if(existing){
                """)
                .append("""
                      const existingDraft=existing.node?.metadata?.draft===true||existing.node?.metadata?.channelEndpointDraft===true||existing.node?.metadata?.logicChainDraftOverlay===true;
                """)
                .append("""
                      if(existingDraft){
                """)
                .append("""
                        existing.depth=col;existing.lane=col;existing.rawLane=col;existing.x=logicChainColumnX(metrics,col);existing.y=logicChainSlotY(metrics,slot);existing.draft=true;
                """)
                .append("""
                        if(info.directDownstreamOfJoin){layout.draftChannelDirectDownstreamOfJoin=true;layout.noForcedDraftOutputC3Gap=true;layout.draftOutputAdjacentColumn=true;}
                """)
                .append("""
                      }
                """)
                .append("""
                      existing.node=existingDraft?{...(existing.node||{}),...node,id:canonical,metadata:{...(existing.node?.metadata||{}),...(node.metadata||{}),singleDraftEndpointCard:true,logicChainChannelEndpointNoDuplicateCard:true}}:{...(existing.node||{}),metadata:{...(existing.node?.metadata||{}),channelEndpointDraft:true,cardDraft:true,canonicalNodeId:canonical,primaryNodeId:canonical,singleDraftEndpointCard:true,logicChainChannelEndpointNoDuplicateCard:true}};
                """)
                .append("""
                      layout.draftChannelEndpointCard=true;layout.draftChannelEndpointSingleCard=true;return;
                """)
                .append("""
                    }
                """)
                .append("""
                    const item={node,children:[],childCount:0,collapsed:false,cycle:false,depth:col,x:logicChainColumnX(metrics,col),y:logicChainSlotY(metrics,slot),w:metrics.nodeWidth,h:metrics.nodeHeight,lane:col,rawLane:col,layoutVersion:'v2-join-lanes',draft:true};
                """)
                .append("""
                    layout.flat.push(item);byId[item.node.id]=item;layout.draftChannelEndpointCard=true;layout.draftChannelEndpointSingleCard=true;
                """)
                .append("""
                    if(info.directDownstreamOfJoin){layout.draftChannelDirectDownstreamOfJoin=true;layout.noForcedDraftOutputC3Gap=true;layout.draftOutputAdjacentColumn=true;}
                """)
                .append("""
                  });
                """)
                .append("""
                  (editor.nodes||[]).forEach(node=>{const type=String(node.type||'').toLowerCase(),col=logicChainColumnIndex(node.column,type),resolved=logicChainResolveDraftSlot(layout,col,node.slot,type,node),slot=resolved??0,drag=editor.drag?.active&&(!editor.activeDraftNodeId||String(editor.activeDraftNodeId||'')===String(node.id||'')),dragX=Number(editor.drag?.x),dragY=Number(editor.drag?.y),pendingY=Math.max(8,metrics.margin-Math.round(metrics.nodeHeight*.62)),placed=!!node.placed&&resolved!=null,graphNode=logicChainNewDraftGraphNode(node),item={node:{...graphNode,metadata:{...(graphNode.metadata||{}),placementColumn:node.column,placementSlot:slot,pendingPlacement:!placed,joinSlotHiddenWithoutInputContext:type==='signal_join'&&resolved==null,draftUsesOwnAnchorSlot:true,dataLogicChainDraftWorldDeviceMetadataVisible:type==='world_device'}},children:[],childCount:0,collapsed:false,cycle:false,depth:col,x:drag&&Number.isFinite(dragX)?dragX-metrics.nodeWidth/2:logicChainColumnX(metrics,col),y:drag&&Number.isFinite(dragY)?dragY-metrics.nodeHeight/2:(placed?logicChainSlotY(metrics,slot):pendingY),w:metrics.nodeWidth,h:metrics.nodeHeight,lane:col,rawLane:col,layoutVersion:'v2-join-lanes',draft:true};layout.flat.push(item);byId[item.node.id]=item;});
                """)
                .append("""
                  const draftRegionOwnerGroups={};
                """)
                .append("""
                  (editor.nodes||[]).forEach(node=>{const ownerItem=byId[node.id];if(!ownerItem)return;logicChainDraftNestedActions(node).forEach(entry=>{const action=entry.action||{},actionType=String(action.type||'').toLowerCase(),channel=normalizeLogicChainDraftChannel(action.value);if(actionType!=='signal'||!channel)return;const ownerType=String(entry.ownerType||'').toLowerCase(),bucket=String(entry.bucket||'').toLowerCase(),channelId=logicChainDraftChannelNodeId(channel),target=logicChainResolveDraftVisualEndpoint(channelId,'draft_action_outputs_channel','downstream',Number(ownerItem.depth??0)+2,node,ownerItem),targetCol=Number(target?.depth??target?.lane??Number(ownerItem.depth??0)+2),actionCol=Math.max(Number(ownerItem.depth??0)+1,targetCol-1),anchorSlot=target?logicChainSlotFromY(metrics,target.y):logicChainSlotFromY(metrics,ownerItem.y)+Number(entry.index||0),slot=logicChainNearestFreeSlotForColumn(layout,actionCol,anchorSlot),aliasId=`draft:action:${node.id}:${ownerType}:${bucket||'default'}:${Number(entry.index||0)}`,alias={node:{id:aliasId,type:'action',refType:'action',refId:node.id,label:labelActionType(action.type),subtitle:`${logicChainActionAppendBucketLabel(ownerType,bucket)} · ${logicChainDraftActionSummary(action)}`,channel,enabled:action.enabled!==false,status:'DRAFT',doctorStatus:'INFO',lastEvent:'',detailRoute:'',metadata:{draft:true,draftCreatedActionAlias:true,nodeKind:'primary',readOnly:false,ownerType,ownerId:node.id,bucket,regionBucket:bucket,actionIndex:Number(entry.index||0),actionType,downstreamChannel:channel,regionActionOwnedAlias:ownerType==='region_controller',regionActionOwnerNodeId:node.id,dataLogicChainDraftActionSemanticPlacementResolver:true}},children:[],childCount:0,collapsed:false,cycle:false,depth:actionCol,x:logicChainColumnX(metrics,actionCol),y:logicChainSlotY(metrics,slot),w:metrics.nodeWidth,h:metrics.nodeHeight,lane:actionCol,rawLane:actionCol,layoutVersion:'v2-join-lanes',draft:true};layout.flat.push(alias);byId[aliasId]=alias;layout.edges.push({from:ownerItem,to:alias,edge:{from:ownerItem.node.id,to:alias.node.id,type:'draft_action_owner',label:'草稿 Action',style:'dashed',pathGroupId:'execution',visualStyle:'draft-highlight',referenceEdge:false,metadata:{draft:true,visualOnly:true,nonTraversal:false,ownerType,bucket,actionIndex:Number(entry.index||0),regionActionOwnerEdge:ownerType==='region_controller'}},relation:'draft_action_owner',markerEnd:true});if(target)layout.edges.push({from:alias,to:target,edge:{from:alias.node.id,to:target.node.id,type:'emits',label:'草稿 Signal 输出',style:'solid',pathGroupId:'draft',visualStyle:'draft-highlight',referenceEdge:false,metadata:{draft:true,newEdge:true,saveUsesCanonicalChannel:true,ownerType,bucket,actionIndex:Number(entry.index||0),draftActionOutput:true}},relation:'emits',markerEnd:true});if(ownerType==='region_controller'){const group=draftRegionOwnerGroups[node.id]||(draftRegionOwnerGroups[node.id]={ownerItem,actions:[]});group.actions.push(alias);layout.regionControllerDraftActionAliases=true;}});});
                """)
                .append("""
                  Object.values(draftRegionOwnerGroups).forEach(group=>{if(!group.ownerItem||!group.actions.length)return;const minCol=Math.max(0,Math.min(...group.actions.map(item=>Number(item.depth??item.lane??1)))-1),avgSlot=Math.round(group.actions.reduce((sum,item)=>sum+logicChainSlotFromY(metrics,item.y),0)/group.actions.length),slot=logicChainNearestFreeSlotForColumnIgnoringId(layout,minCol,avgSlot,group.ownerItem.node?.id||'');group.ownerItem.depth=minCol;group.ownerItem.lane=minCol;group.ownerItem.rawLane=minCol;group.ownerItem.x=logicChainColumnX(metrics,minCol);group.ownerItem.y=logicChainSlotY(metrics,slot);group.ownerItem.node.metadata={...(group.ownerItem.node.metadata||{}),regionControllerDraftOwnerFollowsActionGroup:true,dataLogicChainRegionControllerDraftOwnerFollowsActionGroup:true};layout.regionControllerDraftOwnerFollowsActionGroup=true;});
                """)
                .append("""
                  if(editor.actionAppend){
                """)
                .append("""
                    const append=editor.actionAppend,ownerItem=byId[append.ownerNodeId]||byId[append.ownerGraphNodeId]||null,ownerY=ownerItem?Number(ownerItem.y||metrics.margin):metrics.margin,actionIndex=Math.max(0,Number(append.actionIndex||0)),action=append.action||{},signalChannel=String(action.type||'').toLowerCase()==='signal'?normalizeLogicChainDraftChannel(action.value):'',target=signalChannel?logicChainResolveDraftVisualEndpoint(logicChainDraftChannelNodeId(signalChannel),'action_append_outputs_channel','downstream',Number(ownerItem?.depth??ownerItem?.lane??1)+2,null,ownerItem):null,targetCol=Number(target?.depth??target?.lane??Number(ownerItem?.depth??ownerItem?.lane??1)+2),aliasId=`draft:action_append:${append.ownerType||'owner'}:${append.ownerId||''}:${append.bucket||'default'}:${actionIndex}`,col=target?Math.max(Number(ownerItem?.depth??ownerItem?.lane??0)+1,targetCol-1):logicChainActionAppendCanonicalLane(layout,ownerItem,append),ownerSlot=logicChainSlotFromY(metrics,ownerY),slot=logicChainNearestFreeSlotForColumn(layout,col,target?logicChainSlotFromY(metrics,target.y):Math.max(0,ownerSlot+actionIndex)),item={node:{id:aliasId,type:'action',refType:'action',refId:append.ownerId||'',label:'待追加 Action',subtitle:`追加到 ${logicChainActionAppendOwnerLabel(append)}`,channel:signalChannel,enabled:true,status:'DRAFT',doctorStatus:'INFO',lastEvent:'',detailRoute:'',metadata:{draft:true,actionAppendDraft:true,actionAppendOnly:true,nodeKind:'primary',readOnly:false,noOldActionMoveDeleteReorder:true,ownerType:append.ownerType||'',ownerId:append.ownerId||'',bucket:append.bucket||'',actionIndex,actionAppendSlot:slot,savedLayoutParity:true,listenerRightLane:true,downstreamChannel:signalChannel,dataLogicChainActionAppendTargetChannelAdjacent:true}},children:[],childCount:0,collapsed:false,cycle:false,depth:col,x:logicChainColumnX(metrics,col),y:logicChainSlotY(metrics,slot),w:metrics.nodeWidth,h:metrics.nodeHeight,lane:col,rawLane:col,layoutVersion:'v2-join-lanes',draft:true};layout.actionAppendSavedLayoutParity=true;layout.actionAppendListenerRightLane=true;layout.actionAppendTargetChannelAdjacent=!!target;layout.flat.push(item);byId[item.node.id]=item;if(ownerItem)layout.edges.push({from:ownerItem,to:item,edge:{from:ownerItem.node.id,to:item.node.id,type:'action_append_draft',label:'追加 Action 草稿',style:'dashed',pathGroupId:'execution',visualStyle:'draft-highlight',referenceEdge:false,metadata:{draft:true,actionAppendDraft:true,visualOnly:true,nonTraversal:false,savedLayoutParity:true,listenerRightLane:true}},relation:'action_append',markerEnd:true});if(target)layout.edges.push({from:item,to:target,edge:{from:item.node.id,to:target.node.id,type:'emits',label:'追加 Signal 输出',style:'solid',pathGroupId:'draft',visualStyle:'draft-highlight',referenceEdge:false,metadata:{draft:true,actionAppendDraft:true,newEdge:true,saveUsesCanonicalChannel:true,targetChannelAdjacent:true}},relation:'emits',markerEnd:true});
                """)
                .append("""
                  }
                """)
                .append("""
                  const draftEdges=[];
                """)
                .append("""
                  (editor.edges||[]).forEach(edge=>{
                """)
                .append("""
                    const type=String(edge.type||'');
                """)
                .append("""
                    const edgeDraftNode=(editor.nodes||[]).find(n=>String(n.id||'')===String(edge.from||'')||String(n.id||'')===String(edge.to||''))||draftNode,edgeDraftType=String(edgeDraftNode.type||draftType).toLowerCase(),edgeDraftCol=logicChainColumnIndex(edgeDraftNode.column,edgeDraftType),edgeDraftItem=byId[edgeDraftNode.id]||null;
                """)
                .append("""
                    let from=null,to=null;
                """)
                .append("""
                    if(type==='join_input'){from=logicChainResolveDraftVisualEndpoint(edge.from,type,'upstream',Math.max(0,edgeDraftCol-1),edgeDraftNode,edgeDraftItem);to=byId[edge.to];}
                """)
                .append("""
                    else if(type==='join_output'){from=byId[edge.from];layout.noForcedDraftOutputC3Gap=true;layout.draftOutputAdjacentColumn=true;to=logicChainResolveDraftVisualEndpoint(edge.to,type,'downstream',edgeDraftCol+1,edgeDraftNode,edgeDraftItem);}
                """)
                .append("""
                    else if(['timer_outputs_channel','vbd_outputs_channel','world_device_outputs_channel'].includes(type)){from=byId[edge.from];to=logicChainResolveDraftVisualEndpoint(edge.to,type,'downstream',Math.max(edgeDraftCol+1,1),edgeDraftNode,edgeDraftItem);}
                """)
                .append("""
                    else if(type==='world_device_consumes_channel'){from=logicChainResolveDraftVisualEndpoint(edge.from,type,'upstream',Math.max(edgeDraftCol-1,0),edgeDraftNode,edgeDraftItem);to=byId[edge.to];}
                """)
                .append("""
                    else{from=byId[edge.from];to=byId[edge.to];}
                """)
                .append("""
                    if(!from||!to)return;
                """)
                .append("""
                    draftEdges.push({from,to,edge:{from:edge.from,to:edge.to,type,label:'新增连线',style:'solid',pathGroupId:'draft',visualStyle:'draft-highlight',referenceEdge:false,metadata:{draft:true,newEdge:true,visualEndpointReference:true,saveUsesCanonicalChannel:true}},relation:type,markerEnd:true});
                """)
                .append("""
                  });
                """)
                .append("""
                  if(layout.regionControllerDraftOwnerFollowsActionGroup){const cols=new Set((layout.flat||[]).filter(item=>item.node?.metadata?.draftCreatedActionAlias||item.node?.metadata?.regionControllerDraftOwnerFollowsActionGroup).map(item=>Number(item.depth??item.lane??0)));logicChainCenterBalanceAdjustedColumns(layout,metrics,cols);layout.dataLogicChainSharedSemanticPlacementResolver=true;}
                """)
                .append("""
                  layout.edges=logicChainAnnotateEdgePorts([...(layout.edges||[]),...draftEdges]);
                """)
                .append("""
                  layout.width=Math.max(layout.width||960,...layout.flat.map(n=>n.x+n.w+metrics.margin));
                """)
                .append("""
                  layout.height=Math.max(layout.height||520,...layout.flat.map(n=>n.y+n.h+metrics.margin));
                """)
                .append("""
                  layout.draftActive=true;
                """)
                .append("""
                  layout.sameColumnMakeRoom=true;
                """)
                .append("""
                  layout.noDraftSlotOverlap=true;
                """)
                .append("""
                  layout.draftReferenceCards=true;
                """)
                .append("""
                  layout.referenceCardNecessaryOnly=true;
                """)
                .append("""
                  return layout;
                """)
                .append("""
                }
                """)
                .append("""
                function logicChainDraftSlotOverlay(layout){const editor=appState.logicChainEditor||{};if(!editor.active||!(editor.nodes||[]).length)return '';const node=logicChainActiveDraftNode(editor)||{}, type=String(node.type||'signal_join').toLowerCase(), metrics=logicChainDraftMetrics(layout), previewColumn=String(editor.previewColumn||''),previewSlot=Number(editor.previewSlot??-1),cols=logicChainDraftPlacementColumns(layout,type);return cols.map(col=>(editor.legalSlots?.[`C${col}`]||logicChainLegalSlotsForColumn(layout,col,type)).map(slot=>{const x=logicChainColumnX(metrics,col),y=logicChainSlotY(metrics,slot),active=!!node.placed&&String(node.column||'').toUpperCase()===`C${col}`&&Number(node.slot||0)===slot,preview=previewColumn===`C${col}`&&previewSlot===slot;return `<button type="button" class="logic-chain-draft-slot ${active?'active':''} ${preview?'preview':''}" style="left:${x}px;top:${y}px;width:${metrics.nodeWidth}px;height:${metrics.nodeHeight}px" data-logic-chain-draft-slot-button="true" data-column="C${col}" data-slot="${slot}" data-logic-chain-drag-slot-canvas="true" data-logic-chain-valid-slot-outline="true" data-logic-chain-drop-preview="${preview?'true':(active?'active':'available')}" data-logic-chain-snap-to-canonical-slot="true" data-logic-chain-slot-proximity="true" data-logic-chain-click-placement-fallback="true" data-logic-chain-slot-occupancy-column="true" data-logic-chain-slot-cannot-overlap-existing-node="true" data-logic-chain-same-column-make-room="true" data-logic-chain-nearest-slot-only="true" data-logic-chain-far-empty-slot-hidden="true" data-logic-chain-slot-context-anchor="true" data-logic-chain-all-draft-types-nearest-slot-policy="true" ${type==='signal_join'?`data-logic-chain-join-visual-downstream-slot="true" data-logic-chain-join-slot-input-channel-adjacent="${layout.joinSlotInputChannelAdjacent?'true':'false'}" data-logic-chain-join-slot-hidden-without-input-context="${layout.joinSlotHiddenWithoutInputContext?'true':'false'}" data-logic-chain-join-slot-shared-input-band="${layout.joinSlotSharedInputBand?'true':'false'}" data-logic-chain-join-slot-left-channel-column="${layout.joinSlotLeftChannelColumn?'true':'false'}" data-logic-chain-join-slot-upstream-channel-column="${layout.joinSlotUpstreamChannelColumn?'true':'false'}" data-logic-chain-join-slot-downstream-of-channel="${layout.joinSlotDownstreamOfChannel?'true':'false'}" data-logic-chain-join-slot-target-column-may-contain-listener="${layout.joinSlotTargetColumnMayContainListener?'true':'false'}" data-logic-chain-join-slot-no-forced-empty-processing-column="${layout.joinSlotNoForcedEmptyProcessingColumn?'true':'false'}" data-logic-chain-join-slot-dynamic-columns="${layout.joinSlotDynamicColumns?'true':'false'}" data-logic-chain-join-slot-empty-column-single-middle="${layout.joinSlotEmptyColumnSingleMiddle?'true':'false'}" data-logic-chain-join-slot-occupied-column-insert-anywhere="${layout.joinSlotOccupiedColumnInsertAnywhere?'true':'false'}" data-logic-chain-join-slot-bottom-append="${layout.joinSlotBottomAppend?'true':'false'}" data-logic-chain-join-slot-multi-gap="${layout.joinSlotMultiGap?'true':'false'}" data-logic-chain-join-slot-not-median-only="${layout.joinSlotNotMedianOnly?'true':'false'}" data-logic-chain-join-not-output-channel-column="false" data-logic-chain-join-not-action-listener-column="false"`:''} title="放置到 C${col} 就近合法槽位 ${slot}"></button>`;}).join('')).join('');}
                """).toString();
    }
}
