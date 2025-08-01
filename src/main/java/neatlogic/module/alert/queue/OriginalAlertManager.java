/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.alert.queue;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.alert.plugin.adapter.core.AlertIgnoreException;
import neatlogic.framework.alert.adaptor.core.AlertAdaptorManager;
import neatlogic.framework.alert.config.AlertConfig;
import neatlogic.framework.alert.dto.AlertTypeAdaptorVo;
import neatlogic.framework.alert.dto.AlertTypeVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.OriginalAlertVo;
import neatlogic.framework.alert.enums.AlertOriginStatus;
import neatlogic.framework.alert.event.AlertEventManager;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alerttype.AlertTypeIsNotActiveException;
import neatlogic.framework.alert.exception.alerttype.AlertTypeNotFoundException;
import neatlogic.framework.asynchronization.taskmanager.AsyncTaskManager;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.alert.dao.mapper.AlertTypeMapper;
import neatlogic.module.alert.service.IAlertService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class OriginalAlertManager {
    private static AlertTypeMapper alertTypeMapper;
    private static FileMapper fileMapper;
    private static IAlertService alertService;
    private static final Logger logger = LoggerFactory.getLogger(OriginalAlertManager.class);
    private static AsyncTaskManager<OriginalAlertVo> manager;

    @Autowired
    public OriginalAlertManager(AlertTypeMapper _alertTypeMapper, IAlertService _alertService, FileMapper _fileMapper) {
        alertTypeMapper = _alertTypeMapper;
        fileMapper = _fileMapper;
        alertService = _alertService;
        manager = AsyncTaskManager.getInstance("ORIGINAL-ALERT-HANDLER", AlertConfig.ORIGINAL_ALERT_THREAD_COUNT(),
                originalAlertVo -> {
                    Handler handler = new Handler(originalAlertVo);
                    handler.execute();
                });
    }


    public static void addAlert(OriginalAlertVo originalAlertVo) {
        manager.submitTask(originalAlertVo);
    }

    static class Handler {
        private final OriginalAlertVo originalAlertVo;

        public Handler(OriginalAlertVo _originalAlertVo) {
            originalAlertVo = _originalAlertVo;
        }

        public void execute() {
            Thread.currentThread().setName("ORIGINAL-ALERT-HANDLER-" + originalAlertVo.getId());
            try {
                AlertTypeVo alertTypeVo = alertTypeMapper.getAlertTypeByName(originalAlertVo.getType());
                if (alertTypeVo == null) {
                    throw new AlertTypeNotFoundException(originalAlertVo.getType());
                }
                if (!Objects.equals(1, alertTypeVo.getIsActive())) {
                    throw new AlertTypeIsNotActiveException(originalAlertVo.getType());
                }

                AlertVo alertVo;

                if (StringUtils.isNotBlank(originalAlertVo.getAdaptor()) && CollectionUtils.isNotEmpty(alertTypeVo.getAdaptorList())) {
                    List<AlertTypeAdaptorVo> adaptorList = alertTypeVo.getAdaptorList();
                    AlertTypeAdaptorVo adaptor = adaptorList.stream().filter(d -> d.getName().equals(originalAlertVo.getAdaptor())).findFirst().orElse(null);
                    if (adaptor == null) {
                        throw new ApiRuntimeException("告警类型{0}找不到转换插件{1}", alertTypeVo.getName(), originalAlertVo.getAdaptor());
                    }
                    if (adaptor.getFileId() == null) {
                        throw new ApiRuntimeException("告警类型{0}的转换插件{1}没有上传组件", alertTypeVo.getName(), originalAlertVo.getAdaptor());
                    }
                    FileVo fileVo = fileMapper.getFileById(adaptor.getFileId());
                    if (fileVo == null) {
                        throw new ApiRuntimeException("告警类型{0}的转换插件{1}的组件不存在", alertTypeVo.getName(), originalAlertVo.getAdaptor());
                    }
                    adaptor.setFilePath(fileVo.getPath());
                    JSONObject alertObj = AlertAdaptorManager.convert(alertTypeVo, adaptor, originalAlertVo.getContent());
                    alertVo = JSON.toJavaObject(alertObj, AlertVo.class);
                } else {
                    alertVo = JSON.parseObject(originalAlertVo.getContent(), AlertVo.class);
                }
                if (alertVo != null) {
                    //补充必要信息
                    alertVo.setType(alertTypeVo.getId());
                    alertVo.setTypeName(alertTypeVo.getLabel());
                    //如果告警信息没有来源，则使用系统来源
                    if (StringUtils.isBlank(alertVo.getSource())) {
                        alertVo.setSource(originalAlertVo.getSource());
                    }
                    //如果不提供告警时间，则使用当前时间
                    if (alertVo.getAlertTime() == null) {
                        alertVo.setAlertTime(new Date());
                    }

                    if (alertVo.getUpdateTime() == null) {
                        alertVo.setUpdateTime(alertVo.getAlertTime());
                    }

                    //如果提供了唯一键，则直接处理成md5
                    if (StringUtils.isNotBlank(alertVo.getUniqueKey())) {
                        if (!Md5Util.isMd5(alertVo.getUniqueKey())) {
                            alertVo.setUniqueKey(Md5Util.encryptMD5(alertVo.getUniqueKey()));
                        }
                    }

                    alertVo.setId(originalAlertVo.getId());
                    originalAlertVo.setAlertData(JSON.parseObject(JSON.toJSONString(alertVo)));
                    AlertEventManager.doEvent(AlertEventType.ALERT_INPUT, alertVo);
                }
                originalAlertVo.setStatus(AlertOriginStatus.SUCCEED.getValue());
            } catch (AlertIgnoreException ex) {
                originalAlertVo.setStatus(AlertOriginStatus.IGNORED.getValue());
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                originalAlertVo.setError(ex.getMessage() == null ? ExceptionUtils.getStackTrace(ex) : ex.getMessage());
                originalAlertVo.setStatus(AlertOriginStatus.FAILED.getValue());
            } finally {
                alertService.saveOriginAlert(originalAlertVo);
            }
        }

    }
}
