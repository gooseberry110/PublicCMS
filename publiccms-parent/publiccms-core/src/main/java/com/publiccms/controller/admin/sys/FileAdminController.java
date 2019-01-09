package com.publiccms.controller.admin.sys;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import com.publiccms.common.constants.CommonConstants;
import com.publiccms.common.tools.CmsFileUtils;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.common.tools.ControllerUtils;
import com.publiccms.common.tools.RequestUtils;
import com.publiccms.entities.log.LogUpload;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.logic.component.site.SiteComponent;
import com.publiccms.logic.service.log.LogLoginService;
import com.publiccms.logic.service.log.LogUploadService;

/**
 *
 * FileAdminController
 *
 */
@Controller
@RequestMapping("file")
public class FileAdminController {
    protected final Log log = LogFactory.getLog(getClass());
    @Autowired
    protected LogUploadService logUploadService;
    @Autowired
    protected SiteComponent siteComponent;

    /**
     * @param file
     * @param field
     * @param originalField
     * @param _csrf
     * @param request
     * @param session
     * @param model
     * @return view name
     */
    @RequestMapping(value = "doUpload", method = RequestMethod.POST)
    public String upload(MultipartFile file, String field, String originalField, String _csrf, HttpServletRequest request,
            HttpSession session, ModelMap model) {
        if (ControllerUtils.verifyNotEquals("_csrf", ControllerUtils.getAdminToken(request), _csrf, model)) {
            return CommonConstants.TEMPLATE_ERROR;
        }
        SysSite site = siteComponent.getSite(request.getServerName());
        if (null != file && !file.isEmpty()) {
            String originalName = file.getOriginalFilename();
            String suffix = CmsFileUtils.getSuffix(originalName);
            String fileName = CmsFileUtils.getUploadFileName(suffix);
            try {
                CmsFileUtils.upload(file, siteComponent.getWebFilePath(site, fileName));
                model.put("field", field);
                model.put(field, fileName);
                String fileType = CmsFileUtils.getFileType(suffix);
                model.put("fileType", fileType);
                model.put("fileSize", file.getSize());
                if (CommonUtils.notEmpty(originalField)) {
                    model.put("originalField", originalField);
                    model.put(originalField, originalName);
                }
                logUploadService.save(new LogUpload(site.getId(), ControllerUtils.getAdminFromSession(session).getId(),
                        LogLoginService.CHANNEL_WEB_MANAGER, originalName, fileType, file.getSize(),
                        RequestUtils.getIpAddress(request), CommonUtils.getDate(), fileName));
            } catch (IllegalStateException | IOException e) {
                log.error(e.getMessage(), e);
                return "common/uploadResult";
            }
        }
        return "common/uploadResult";
    }
}
