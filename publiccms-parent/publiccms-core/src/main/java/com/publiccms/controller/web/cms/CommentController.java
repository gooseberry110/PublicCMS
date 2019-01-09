package com.publiccms.controller.web.cms;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

import com.publiccms.common.api.Config;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.common.tools.ControllerUtils;
import com.publiccms.common.tools.JsonUtils;
import com.publiccms.common.tools.RequestUtils;
import com.publiccms.entities.cms.CmsComment;
import com.publiccms.entities.log.LogOperate;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.entities.sys.SysUser;
import com.publiccms.logic.component.config.ConfigComponent;
import com.publiccms.logic.component.config.LoginConfigComponent;
import com.publiccms.logic.component.site.SiteComponent;
import com.publiccms.logic.service.cms.CmsCommentService;
import com.publiccms.logic.service.log.LogLoginService;
import com.publiccms.logic.service.log.LogOperateService;

/**
 * 
 * ContentController 内容
 *
 */
@Controller
@RequestMapping("comment")
public class CommentController {
    @Autowired
    protected LogOperateService logOperateService;
    @Autowired
    protected SiteComponent siteComponent;
    @Autowired
    protected ConfigComponent configComponent;

    private String[] ignoreProperties = new String[] { "siteId", "userId", "createDate", "checkUserId", "checkDate", "status",
            "replyId", "replyUserId", "disabled" };

    /**
     * @param entity
     * @param _csrf
     * @param returnUrl
     * @param request
     * @param session
     * @param model
     * @return
     */
    @RequestMapping("save")
    public String save(CmsComment entity, String _csrf, String returnUrl, HttpServletRequest request, HttpSession session,
            ModelMap model) {
        SysSite site = siteComponent.getSite(request.getServerName());
        Map<String, String> config = configComponent.getConfigData(site.getId(), Config.CONFIG_CODE_SITE);
        String safeReturnUrl = config.get(LoginConfigComponent.CONFIG_RETURN_URL);
        if (ControllerUtils.isUnSafeUrl(returnUrl, site, safeReturnUrl, request)) {
            returnUrl = site.getDynamicPath();
        }
        SysUser user = ControllerUtils.getUserFromSession(session);
        if (ControllerUtils.verifyNotEquals("_csrf", ControllerUtils.getWebToken(request), _csrf, model)
                || ControllerUtils.verifyCustom("contribute", null == user, model)) {
            return UrlBasedViewResolver.REDIRECT_URL_PREFIX + returnUrl;
        }

        if (null != entity.getId()) {
            CmsComment oldEntity = service.getEntity(entity.getId());
            if (null != oldEntity && !oldEntity.isDisabled() && oldEntity.getUserId() == user.getId()) {
                entity.setUpdateDate(CommonUtils.getDate());
                entity = service.update(entity.getId(), entity, ignoreProperties);
                logOperateService
                        .save(new LogOperate(site.getId(), user.getId(), LogLoginService.CHANNEL_WEB, "update.cmsComment",
                                RequestUtils.getIpAddress(request), CommonUtils.getDate(), JsonUtils.getString(entity)));
            }
        } else {
            entity.setSiteId(site.getId());
            entity.setUserId(user.getId());
            entity.setStatus(CmsCommentService.STATUS_PEND);
            if (null != entity.getReplyId()) {
                CmsComment reply = service.getEntity(entity.getReplyId());
                if (null == reply) {
                    entity.setReplyId(null);
                } else {
                    entity.setReplyUserId(reply.getUserId());
                }
            }
            service.save(entity);
            logOperateService.save(new LogOperate(site.getId(), user.getId(), LogLoginService.CHANNEL_WEB, "save.cmsComment",
                    RequestUtils.getIpAddress(request), CommonUtils.getDate(), JsonUtils.getString(entity)));
        }
        return UrlBasedViewResolver.REDIRECT_URL_PREFIX + returnUrl;
    }

    @Autowired
    private CmsCommentService service;

}
