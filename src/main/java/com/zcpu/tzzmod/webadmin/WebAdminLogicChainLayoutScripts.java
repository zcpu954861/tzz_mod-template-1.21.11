package com.zcpu.tzzmod.webadmin;

final class WebAdminLogicChainLayoutScripts {
    private WebAdminLogicChainLayoutScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                function logicChainLayoutGraphV2(graph,nodes){const NODE_W=268,NODE_H=118,ROW_GAP=34,MARGIN=44,edgeIndexes=logicChainEdgeIndexes(graph),modeIds=logicChainVisibleNodeIdsForMode(graph,nodes,edgeIndexes),visibleNodes=Object.values(nodes||{}).filter(n=>modeIds.has(n.id)&&logicChainNodeVisibleByFilter(n));if(!visibleNodes.length)return null;const visibleIds=new Set(visibleNodes.map(n=>n.id)),graphEdges=(graph?.edges||[]).filter(e=>visibleIds.has(e.from)&&visibleIds.has(e.to)),laneById={};function setLane(id,lane){if(!id)return;laneById[id]=Math.max(laneById[id]??0,Math.max(0,Number(lane)||0));}visibleNodes.forEach(node=>setLane(node.id,logicChainBaseLane(node,edgeIndexes)));for(let pass=0;pass<8;pass++){graphEdges.forEach(edge=>{const type=String(edge.type||''),fromLane=laneById[edge.from]??0,toLane=laneById[edge.to]??0;if(type==='join_input'){setLane(edge.from,Math.min(laneById[edge.from]??1,1));}else if(type==='join_output'){setLane(edge.to,Math.max(3,(laneById[edge.from]??2)+1));}else if(['consumes','executes','gate_guards','emits_downstream','state_writes','action_starts_timer','action_cancels_timer','emits','timer_outputs_channel','vbd_outputs_channel','world_device_outputs_channel'].includes(type)){setLane(edge.to,Math.max(toLane,fromLane+1));}});}logicChainPreventDownstreamFoldback(laneById,graphEdges,visibleNodes.length);visibleNodes.forEach(node=>{const t=String(node.type||'').toLowerCase();if(t==='signal_join')laneById[node.id]=2;else if((edgeIndexes.byTo[node.id]||[]).some(e=>e.type==='join_output'))laneById[node.id]=Math.max(3,laneById[node.id]??3);else if((edgeIndexes.byFrom[node.id]||[]).some(e=>e.type==='join_input'))laneById[node.id]=Math.min(1,laneById[node.id]??1);});logicChainPreventDownstreamFoldback(laneById,graphEdges,visibleNodes.length);const aliasByEdge={},displayNodes=[...visibleNodes];graphEdges.filter(e=>e.type==='join_input').forEach(edge=>{const fromLane=laneById[edge.from]??1;if(fromLane<2)return;const source=nodes?.[edge.from];if(!source)return;const aliasId=`reference:join-input:${edge.to}:${edge.from}`;aliasByEdge[`${edge.from}>${edge.to}>${edge.type}`]=aliasId;displayNodes.push({...source,id:aliasId,type:source.type||'channel',subtitle:`引用输入 · ${source.label||source.id}`,metadata:{...(source.metadata||{}),nodeKind:'reference',isReferenceCard:true,primaryNodeId:source.id,canonicalNodeId:source.id,referenceReason:'join_input_left_lane',joinInputTargetId:edge.to,joinInputPortIndex:Number(edge.metadata?.portIndex??999),visualLane:'reference',nonTraversal:true,visualOnly:true}});laneById[aliasId]=1;});const lanes={};displayNodes.forEach(node=>{const lane=laneById[node.id]??0;(lanes[lane]||(lanes[lane]=[])).push(node);});logicChainCrossingReducedLaneSort(lanes,graphEdges,edgeIndexes,laneById);const flat=[],laneKeys=Object.keys(lanes).map(Number).sort((a,b)=>a-b),laneX=logicChainCompactLanePositions(laneKeys),singleChain=logicChainIsSingleChain(graphEdges,visibleIds),colGap=singleChain?92:132,maxRows=Math.max(...laneKeys.map(k=>lanes[k].length));laneKeys.forEach(lane=>{const list=lanes[lane],laneHeight=list.length*NODE_H+Math.max(0,list.length-1)*ROW_GAP,top=MARGIN+Math.max(0,(maxRows*NODE_H+Math.max(0,maxRows-1)*ROW_GAP-laneHeight)/2),xIndex=laneX[lane]??lane;list.forEach((node,index)=>flat.push({node,children:[],childCount:node.metadata?.isReferenceCard?0:(edgeIndexes.byFrom[node.id]||[]).length+(edgeIndexes.byTo[node.id]||[]).length,collapsed:false,cycle:false,depth:xIndex,x:MARGIN+xIndex*(NODE_W+colGap),y:top+index*(NODE_H+ROW_GAP),w:NODE_W,h:NODE_H,lane:xIndex,rawLane:lane,layoutVersion:'v2-join-lanes'}));});const byId={};flat.forEach(item=>{byId[item.node.id]=item;});const edges=logicChainAnnotateEdgePorts(graphEdges.map(edge=>({from:byId[aliasByEdge[`${edge.from}>${edge.to}>${edge.type}`]||edge.from],to:byId[edge.to],edge,relation:String(edge.metadata?.joinInputRole||edge.type||'graph')})).filter(edge=>edge.from&&edge.to));const width=Math.max(960,...flat.map(n=>n.x+n.w+MARGIN)),height=Math.max(520,...flat.map(n=>n.y+n.h+MARGIN));return {layoutVersion:'v2-join-lanes',root:flat.find(i=>i.node.id===graph?.root?.id)||flat[0],flat,edges,width,height,nodeWidth:NODE_W,nodeHeight:NODE_H,emptyLaneCompaction:true,noDownstreamFoldback:true,singleChainCompact:singleChain,joinSemanticLanePreserved:logicChainJoinProcessingDraftActive()};}
                """)
                .append("""
                function logicChainPreventDownstreamFoldback(laneById,graphEdges,nodeCount){const forwardTypes=new Set(['consumes','executes','gate_guards','emits_downstream','state_writes','action_starts_timer','action_cancels_timer','join_output','emits','timer_outputs_channel','vbd_outputs_channel','world_device_outputs_channel']);const maxPass=Math.max(8,Math.min(96,Number(nodeCount||0)+Number((graphEdges||[]).length||0)));for(let pass=0;pass<maxPass;pass++){let changed=false;(graphEdges||[]).forEach(edge=>{if(!forwardTypes.has(String(edge.type||''))||edge.referenceEdge===true||edge.metadata?.nonTraversal)return;const from=laneById[edge.from]??0,to=laneById[edge.to]??0;if(to<=from){laneById[edge.to]=from+1;changed=true;}});if(!changed)break;}}
                """)
                .append("""
                function logicChainJoinProcessingDraftActive(){const editor=appState.logicChainEditor||{},node=logicChainActiveDraftNode(editor)||{};return !!editor.active&&String(node.type||'').toLowerCase()==='signal_join';}
                """)
                .append("""
                function logicChainCompactLanePositions(laneKeys){const map={},keys=(laneKeys||[]).sort((a,b)=>a-b);keys.forEach((lane,index)=>{map[lane]=index;});return map;}
                """)
                .append("""
                function logicChainIsSingleChain(graphEdges,visibleIds){const indeg={},outdeg={};Array.from(visibleIds||[]).forEach(id=>{indeg[id]=0;outdeg[id]=0;});(graphEdges||[]).forEach(edge=>{if(edge.metadata?.nonTraversal||edge.referenceEdge===true)return;if(indeg[edge.to]!=null)indeg[edge.to]++;if(outdeg[edge.from]!=null)outdeg[edge.from]++;});let branchy=0,total=0;Object.keys(indeg).forEach(id=>{total++;if(indeg[id]>1||outdeg[id]>1)branchy++;});return total>2&&branchy<=1;}
                """)
                .append("""
                function logicChainDraftMetrics(layout){const nodeWidth=layout?.nodeWidth||268,nodeHeight=layout?.nodeHeight||118,margin=44,rowGap=34;const lanes=(layout?.flat||[]).map(item=>({lane:Number(item.depth??item.lane??item.rawLane??-1),x:Number(item.x||0)})).filter(item=>item.lane>=0).sort((a,b)=>a.lane-b.lane||a.x-b.x);let colGap=132;for(let i=1;i<lanes.length;i++){if(lanes[i].lane!==lanes[i-1].lane){const gap=lanes[i].x-lanes[i-1].x-nodeWidth;if(Number.isFinite(gap)&&gap>60){colGap=gap;break;}}}return {nodeWidth,nodeHeight,margin,rowGap,colGap,stepY:nodeHeight+rowGap};}
                """)
                .append("""
                function logicChainColumnIndex(col,type){const c=String(col||'').toUpperCase();if(c.startsWith('C'))return Math.max(0,Number(c.substring(1))||0);return String(type||'').toLowerCase()==='signal_join'?2:0;}
                """)
                .append("""
                function logicChainColumnX(metrics,col){return metrics.margin+Number(col||0)*(metrics.nodeWidth+metrics.colGap);}
                """)
                .append("""
                function logicChainSlotY(metrics,slot){return metrics.margin+Number(slot||0)*metrics.stepY;}
                """)
                .append("""
                function logicChainSlotFromY(metrics,y){return Math.max(0,Math.round((Number(y||0)-metrics.margin)/metrics.stepY));}
                """)
                .append("""
                function isChannelNodeId(id){return String(id||'').startsWith('channel:');}
                """)
                .append("""
                function logicChainAllowedDraftColumns(type){const t=String(type||'').toLowerCase();if(t==='timer')return [0];if(t==='signal_join')return [];return [2];}
                """)
                .append("""
                function logicChainColumnItems(layout,col){return (layout?.flat||[]).filter(item=>Number(item.depth??item.lane??item.rawLane??-1)===Number(col)).sort((a,b)=>Number(a.y||0)-Number(b.y||0));}
                """)
                .append("""
                function logicChainSlotRect(metrics,col,slot){return {x:logicChainColumnX(metrics,col),y:logicChainSlotY(metrics,slot),w:metrics.nodeWidth,h:metrics.nodeHeight};}
                """)
                .append("""
                function logicChainRectsOverlap(a,b,gap=0){return a.x<a.w+b.x+gap&&b.x<b.w+a.x+gap&&a.y<a.h+b.y+gap&&b.y<b.h+a.y+gap;}
                """)
                .append("""
                function logicChainSlotOverlapsColumn(layout,col,slot,metrics=logicChainDraftMetrics(layout)){const rect=logicChainSlotRect(metrics,col,slot);return logicChainColumnItems(layout,col).some(item=>logicChainRectsOverlap(rect,{x:Number(item.x||0),y:Number(item.y||0),w:Number(item.w||metrics.nodeWidth),h:Number(item.h||metrics.nodeHeight)},0));}
                """)
                .append("""
                function logicChainChannelIdForItem(item){const node=item?.node||{},type=String(node.type||'').toLowerCase(),id=String(node.id||''),channel=String(node.channel||node.refId||'').trim();if(channel&&(type==='channel'||type==='downstream_channel'||id.startsWith('channel:')||node.metadata?.nodeKind==='reference'||node.metadata?.isReferenceCard))return channel;if(id.startsWith('channel:'))return id.substring('channel:'.length);return '';}
                """)
                .append("""
                function logicChainJoinSlotColumnFromInput(item){const sourceCol=Number(item?.depth??item?.lane??item?.rawLane??-1),target=sourceCol+1;return Number.isFinite(target)&&target>0?target:null;}
                """)
                .append("""
                function logicChainJoinInputAnchorItems(layout){const editor=appState.logicChainEditor||{}, edges=editor.edges||[], node=logicChainActiveDraftNode(editor)||{}, metrics=logicChainDraftMetrics(layout), channels=new Set(edges.filter(edge=>edge.type==='join_input').map(edge=>logicChainChannelIdFromRef(edge.from)).filter(Boolean));const anchors=(layout?.flat||[]).filter(item=>logicChainChannelIdForItem(item)&&logicChainJoinSlotColumnFromInput(item)!=null);if(channels.size){const seen=new Set(anchors.map(item=>logicChainChannelIdForItem(item)));channels.forEach(channel=>{if(seen.has(channel))return;const draftCol=Math.max(2,logicChainColumnIndex(node.column,'signal_join')),sourceCol=Math.max(1,draftCol-1),slot=Math.max(0,Number(node.slot||0));anchors.push({node:{id:`reference:draft-input-anchor:${channel}`,type:'channel',refType:'channel',refId:channel,channel,metadata:{draftEndpoint:true,nodeKind:'reference',isReferenceCard:true,virtualJoinInputAnchor:true}},depth:sourceCol,lane:sourceCol,rawLane:sourceCol,x:logicChainColumnX(metrics,sourceCol),y:logicChainSlotY(metrics,slot),w:metrics.nodeWidth,h:metrics.nodeHeight,draft:true,virtualJoinInputAnchor:true});});}return anchors;}
                """)
                .append("""
                function logicChainJoinMedianAnchorSlot(metrics,anchors){const slots=(anchors||[]).map(item=>logicChainSlotFromY(metrics,item.y)).filter(Number.isFinite).sort((a,b)=>a-b);if(!slots.length)return 0;const mid=slots.length%2?slots[(slots.length-1)/2]:(slots[slots.length/2-1]+slots[slots.length/2])/2;return Math.max(0,Math.round(mid));}
                """)
                .append("""
                function logicChainJoinSlotsForEmptyColumn(layout,col,anchors,metrics=logicChainDraftMetrics(layout)){return [logicChainNearestFreeSlotForColumn(layout,col,logicChainJoinMedianAnchorSlot(metrics,anchors))];}
                """)
                .append("""
                function logicChainJoinBottomAppendSlot(layout,col,metrics=logicChainDraftMetrics(layout)){const slots=logicChainColumnItems(layout,col).filter(item=>!item.draft).map(item=>logicChainSlotFromY(metrics,item.y)).filter(Number.isFinite);return slots.length?Math.max(...slots)+1:0;}
                """)
                .append("""
                function logicChainJoinSlotsForOccupiedColumn(layout,col,metrics=logicChainDraftMetrics(layout)){const occupied=logicChainColumnItems(layout,col).filter(item=>!item.draft).map(item=>logicChainSlotFromY(metrics,item.y)).filter(Number.isFinite).sort((a,b)=>a-b),bottom=logicChainJoinBottomAppendSlot(layout,col,metrics),result=[];for(let i=0;i<occupied.length-1;i++){const start=occupied[i]+1,end=occupied[i+1]-1;if(start>end)continue;const mid=Math.round((start+end)/2);for(let radius=0;radius<=end-start;radius++){const down=mid+radius,up=mid-radius;if(down<=end&&!logicChainSlotOverlapsColumn(layout,col,down,metrics)){result.push(down);break;}if(up>=start&&!logicChainSlotOverlapsColumn(layout,col,up,metrics)){result.push(up);break;}}}result.push(bottom);return Array.from(new Set(result)).sort((a,b)=>a-b);}
                """)
                .append("""
                function logicChainJoinMultiGapLegalSlots(layout,col,anchors){const metrics=logicChainDraftMetrics(layout), targetItems=logicChainColumnItems(layout,col).filter(item=>!item.draft);layout.joinSlotLeftChannelColumn=anchors.length>0;layout.joinSlotUpstreamChannelColumn=anchors.length>0;layout.joinSlotDownstreamOfChannel=anchors.length>0;layout.joinSlotDynamicColumns=true;layout.joinSlotNoForcedEmptyProcessingColumn=true;if(!anchors.length)return [];if(!targetItems.length){layout.joinSlotEmptyColumnSingleMiddle=true;return logicChainJoinSlotsForEmptyColumn(layout,col,anchors,metrics);}layout.joinSlotOccupiedColumnInsertAnywhere=true;layout.joinSlotBottomAppend=true;layout.joinSlotNotMedianOnly=true;layout.joinSlotTargetMayBeOccupied=true;layout.joinSlotTargetColumnMayContainListener=targetItems.some(item=>['consumer','action','state_action','timer_action','condition_gate','action_gate'].includes(String(item.node?.type||'').toLowerCase()));const slots=logicChainJoinSlotsForOccupiedColumn(layout,col,metrics);layout.joinSlotMultiGap=slots.length>1;return slots;}
                """)
                .append("""
                function logicChainJoinTargetColumns(layout){return Array.from(new Set(logicChainJoinInputAnchorItems(layout).map(item=>logicChainJoinSlotColumnFromInput(item)).filter(col=>Number.isFinite(col)&&col>0))).sort((a,b)=>a-b);}
                """)
                .append("""
                function logicChainDraftPlacementColumns(layout,type){const t=String(type||'').toLowerCase();if(t==='signal_join'||t==='signal_listener')return logicChainJoinTargetColumns(layout);const base=logicChainAllowedDraftColumns(type),producer=['timer','virtual_block_device','world_device','region_controller'].includes(t);if(!producer)return base;const e=appState.logicChainEditor||{},adjacent=Object.values(e.visibleChannelAnchors||{}).map(a=>Math.max(0,Number(a.col||0)-1)).filter(Number.isFinite);return Array.from(new Set([...base,...adjacent])).sort((a,b)=>a-b);}
                """)
                .append("""
                function logicChainJoinInputAdjacentLegalSlots(layout,col,type){const t=String(type||'').toLowerCase();if(t!=='signal_join'&&t!=='signal_listener')return null;const allAnchors=logicChainJoinInputAnchorItems(layout), anchors=allAnchors.filter(item=>logicChainJoinSlotColumnFromInput(item)===Number(col));layout.joinSlotInputChannelAdjacent=true;layout.joinSlotHiddenWithoutInputContext=allAnchors.length===0;layout.joinSlotSharedInputBand=allAnchors.length>1;return logicChainJoinMultiGapLegalSlots(layout,col,anchors);}
                """)
                .append("""
                function logicChainDraftAnchorSlot(layout,col,type,draftNodeOverride=null){const metrics=logicChainDraftMetrics(layout), editor=appState.logicChainEditor||{}, node=draftNodeOverride||logicChainActiveDraftNode(editor)||{}, selectedId=appState.logicChainCanvas.selectedNodeId||layout?.root?.node?.id||'', selected=(layout?.flat||[]).find(item=>item.node?.id===selectedId)||layout?.root||null;if(node.placed&&String(node.column||'').toUpperCase()===`C${col}`)return Math.max(0,Number(node.slot||0));if(selected)return logicChainSlotFromY(metrics,selected.y);const slots=(layout?.flat||[]).filter(item=>!item.draft).map(item=>logicChainSlotFromY(metrics,item.y)).filter(Number.isFinite).sort((a,b)=>a-b);return slots.length?slots[Math.floor(slots.length/2)]:0;}
                """)
                .append("""
                function logicChainLegalSlotsForColumn(layout,col,type,draftNodeOverride=null){const joinSlots=logicChainJoinInputAdjacentLegalSlots(layout,col,type);if(joinSlots)return joinSlots;const anchor=logicChainDraftAnchorSlot(layout,col,type,draftNodeOverride), slot=logicChainNearestFreeSlotForColumn(layout,col,anchor);return [slot];}
                """)
                .append("""
                function logicChainResolveDraftSlot(layout,col,slot,type,draftNodeOverride=null){const legal=logicChainLegalSlotsForColumn(layout,col,type,draftNodeOverride);if(!legal.length)return null;const wanted=Math.max(0,Number(slot||0));let best=legal[0],dist=Infinity;legal.forEach(item=>{const d=Math.abs(item-wanted);if(d<dist){dist=d;best=item;}});return best;}
                """)
                .append("""
                function logicChainFirstFreeSlotForColumn(layout,col){const metrics=logicChainDraftMetrics(layout);let slot=0;while(logicChainSlotOverlapsColumn(layout,col,slot,metrics)&&slot<80)slot++;return slot;}
                """)
                .append("""
                function logicChainNearestFreeSlotForColumn(layout,col,anchorSlot=0){const metrics=logicChainDraftMetrics(layout),anchor=Math.max(0,Number(anchorSlot||0));for(let radius=0;radius<80;radius++){const down=anchor+radius,up=anchor-radius;if(down>=0&&!logicChainSlotOverlapsColumn(layout,col,down,metrics))return down;if(radius>0&&up>=0&&!logicChainSlotOverlapsColumn(layout,col,up,metrics))return up;}return logicChainFirstFreeSlotForColumn(layout,col);}
                """)
                .append("""
                function logicChainNearestFreeSlotForColumnIgnoringId(layout,col,anchorSlot=0,ignoreId=''){const metrics=logicChainDraftMetrics(layout),anchor=Math.max(0,Number(anchorSlot||0)),id=String(ignoreId||''),free=slot=>{const rect=logicChainSlotRect(metrics,col,slot);return !logicChainColumnItems(layout,col).some(item=>String(item.node?.id||'')===id?false:logicChainRectsOverlap(rect,{x:Number(item.x||0),y:Number(item.y||0),w:Number(item.w||metrics.nodeWidth),h:Number(item.h||metrics.nodeHeight)},0));};for(let radius=0;radius<80;radius++){const down=anchor+radius,up=anchor-radius;if(down>=0&&free(down))return down;if(radius>0&&up>=0&&free(up))return up;}return logicChainFirstFreeSlotForColumn(layout,col);}
                """)
                .append("""
                function logicChainMakeRoomForDraft(layout,col,draftY,metrics){let cursorY=Number(draftY||0), shifted=false;logicChainColumnItems(layout,col).filter(item=>!item.draft).forEach(item=>{const y=Number(item.y||0),h=Number(item.h||metrics.nodeHeight);if(y+h<=draftY)return;const minY=cursorY+metrics.stepY;if(y<minY){item.y=minY;item.slotShiftedForDraft=true;item.sameColumnMakeRoomForDraft=true;shifted=true;}cursorY=Number(item.y||0);});return shifted;}
                """)
                .append("""
                function logicChainActionAppendCanonicalLane(layout,ownerItem,append){const ownerLane=Number(ownerItem?.depth??ownerItem?.lane??ownerItem?.rawLane??3);const lane=Number.isFinite(ownerLane)?ownerLane+1:4;return Math.max(0,lane);}
                """).toString();
    }
}
