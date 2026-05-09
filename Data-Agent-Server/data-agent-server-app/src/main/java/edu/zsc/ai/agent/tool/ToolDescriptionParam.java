package edu.zsc.ai.agent.tool;

public final class ToolDescriptionParam {

    private ToolDescriptionParam() {
    }

    public static final String UI_STEP_DESCRIPTION =
            "Optional UI-only step summary in the user's language. Keep it short, for example: 查询可用数据库, 检查订单表结构, 执行汇总查询. "
                    + "It is only shown in the chat tool progress UI and must not affect execution.";
}
