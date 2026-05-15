package com.zcpu.tzzmod.webadmin.dto;

import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebAdminConditionGroupPreviewRequest {
    public ConditionGroupDefinition groupDefinition;
    public PreviewContext context = new PreviewContext();

    public static final class PreviewContext {
        public String playerId = "";
        public String playerName = "";
        public Boolean playerOnline = null;
        public Boolean playerOp = null;
        public List<String> playerTags = new ArrayList<>();
        public String playerTeam = "";
        public String playerGameMode = "";
        public Boolean playerAlive = null;
        public String world = "";
        public String sourceType = "";
        public String sourceId = "";
        public String channel = "";
        public String deviceId = "";
        public String listenerId = "";
        public String regionId = "";
        public String actionId = "";
        public long gameTime = 0L;
        public Map<String, String> eventMetadata = new LinkedHashMap<>();
        public Map<String, String> variables = new LinkedHashMap<>();
        public List<StateVariableInput> stateVariables = new ArrayList<>();
    }

    public static final class StateVariableInput {
        public StateVariableScope scope = StateVariableScope.GLOBAL;
        public String targetId = "";
        public String key = "";
        public StateVariableType type = StateVariableType.STRING;
        public String value = "";
        public String displayName = "";
        public String note = "";
    }
}
