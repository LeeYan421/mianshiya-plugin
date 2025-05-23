package com.github.yuyuanweb.mianshiyaplugin.toolWindow;

import cn.hutool.core.util.StrUtil;
import com.github.yuyuanweb.mianshiyaplugin.actions.*;
import com.github.yuyuanweb.mianshiyaplugin.config.ApiConfig;
import com.github.yuyuanweb.mianshiyaplugin.config.GlobalState;
import com.github.yuyuanweb.mianshiyaplugin.constant.CommonConstant;
import com.github.yuyuanweb.mianshiyaplugin.constant.IconConstant;
import com.github.yuyuanweb.mianshiyaplugin.model.common.BaseResponse;
import com.github.yuyuanweb.mianshiyaplugin.model.enums.ErrorCode;
import com.github.yuyuanweb.mianshiyaplugin.model.response.User;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;

import static com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant.*;

/**
 * @author pine
 */
@Slf4j
public class MyToolWindowFactory implements ToolWindowFactory {

    private static final Logger logger = Logger.getInstance(MyToolWindowFactory.class);

    public MyToolWindowFactory() {}

    @Override
    public void createToolWindowContent(@NotNull Project project, ToolWindow toolWindow) {
        // 创建主面板
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty());

        // 获取 ContentFactory
        // ContentFactory contentFactory = ContentFactory.getInstance();
        // 使用旧写法，兼容旧版本
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        Content content = contentFactory.createContent(mainPanel, "首页", true);
        content.setCloseable(false);

        // 创建工具栏并添加到面板顶部，传入 content ，便于改 displayName
        ActionToolbar actionToolbar = createToolbar(content, mainPanel);
        mainPanel.add(actionToolbar.getComponent(), BorderLayout.NORTH);

        QuestionBankAction questionBankAction = new QuestionBankAction(mainPanel);

        DataContext dataContext = SimpleDataContext.getSimpleContext(CommonDataKeys.PROJECT, project);

        // 构建 AnActionEvent 对象
        AnActionEvent event = AnActionEvent.createFromAnAction(questionBankAction, null, "somePlace", dataContext);
        // 手动触发 action
        questionBankAction.actionPerformed(event);

        // 将内容添加到 Tool Window 中
        toolWindow.getContentManager().addContent(content);
    }

    private ActionToolbar createToolbar(Content content, JBPanel<?> mainPanel) {
        // 定义一个动作组
        ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        DefaultActionGroup action = (DefaultActionGroup) actionManager.getAction(ACTION_BAR);
        if (action != null) {
            // 创建工具栏
            ActionToolbar actionToolbar = actionManager.createActionToolbar(ACTION_BAR, action, true);
            // 设置目标组件
            actionToolbar.setTargetComponent(mainPanel);
            return actionToolbar;
        }

        QuestionBankAction questionBankAction = new QuestionBankAction(QUESTION_BANK_ZH, AllIcons.Scope.ChangedFilesAll);
        actionGroup.add(questionBankAction);
        actionManager.registerAction(QUESTION_BANK, questionBankAction);

        QuestionAction questionAction = new QuestionAction(QUESTION_ZH, AllIcons.Scope.ChangedFiles);
        actionGroup.add(questionAction);
        actionManager.registerAction(QUESTION, questionAction);

        OpenUrlAction webAction = new OpenUrlAction(WEB_ZH, CommonConstant.WEB_HOST, IconConstant.WEB);
        actionGroup.add(webAction);
        actionManager.registerAction(WEB, webAction);

        OpenUrlAction helpDocAction = new OpenUrlAction(HELP_ZH, CommonConstant.HELP_DOC, IconConstant.HELP);
        actionGroup.add(helpDocAction);
        actionManager.registerAction(HELP, helpDocAction);

        GlobalState globalState = GlobalState.getInstance();
        String cookie = globalState.getSavedCookie();
        if (StrUtil.isBlank(cookie)) {
            LoginAction loginAction = new LoginAction(LOGIN_ZH, IconConstant.LOGIN, actionGroup);
            actionGroup.add(loginAction);
            actionManager.registerAction(LOGIN, loginAction);
        } else {
            User loginUser = null;
            try {
                BaseResponse<User> response = ApiConfig.mianShiYaApi.getLoginUser().execute().body();
                if (response != null && response.getCode() == ErrorCode.SUCCESS.getCode()) {
                    loginUser = response.getData();
                    globalState.saveUser(loginUser);
                }
            } catch (IOException e) {
                logger.warn("获取登录用户失败");
            }
            // 未登录
            if (loginUser == null) {
                LoginAction loginAction = new LoginAction(LOGIN_ZH, IconConstant.LOGIN, actionGroup);
                actionGroup.add(loginAction);
                actionManager.registerAction(LOGIN, loginAction);
            } else {
                OpenUrlAction vipAction = new OpenUrlAction(VIP_ZH, CommonConstant.VIP, AllIcons.General.User);
                actionGroup.add(vipAction);
                actionManager.registerAction(VIP, vipAction);

                LogoutAction logoutAction = new LogoutAction(LOGOUT_ZH, IconConstant.LOGOUT, actionGroup);
                actionGroup.add(logoutAction);
                actionManager.registerAction(LOGOUT, logoutAction);

                String userName = loginUser.getUserName();
                if (StrUtil.isNotBlank(userName)) {
                    content.setDisplayName(userName);
                }
            }
        }

        // 创建工具栏
        actionManager.registerAction(ACTION_BAR, actionGroup);
        ActionToolbar actionToolbar = actionManager.createActionToolbar(ACTION_BAR, actionGroup, true);
        // 设置目标组件
        actionToolbar.setTargetComponent(mainPanel);
        return actionToolbar;
    }

}
