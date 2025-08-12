package com.alibaba.cloud.ai.mcp.samples.client.custom;

import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;

public class ReturnDirectSyncMcpProvider extends SyncMcpToolCallbackProvider {

    @Override
    public ToolCallback[] getToolCallbacks() {
        return super.getToolCallbacks();
    }
}
