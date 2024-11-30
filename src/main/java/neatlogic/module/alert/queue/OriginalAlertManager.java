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
import neatlogic.framework.alert.adaptor.core.AlertAdaptorManager;
import neatlogic.framework.alert.dto.AlertTypeVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.OriginalAlertVo;
import neatlogic.framework.alert.enums.AlertOriginStatus;
import neatlogic.framework.alert.exception.alerttype.AlertTypeNotFoundException;
import neatlogic.framework.asynchronization.queue.NeatLogicBlockingQueue;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.dao.mapper.AlertTypeMapper;
import neatlogic.module.alert.service.IAlertService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class OriginalAlertManager {
    private static AlertMapper alertMapper;
    private static AlertTypeMapper alertTypeMapper;
    private static FileMapper fileMapper;
    private static IAlertService alertService;
    private static final Logger logger = LoggerFactory.getLogger(OriginalAlertManager.class);

    private static final NeatLogicBlockingQueue<OriginalAlertVo> alertQueue = new NeatLogicBlockingQueue<>(new LinkedBlockingQueue<>());

    @Autowired
    public OriginalAlertManager(AlertTypeMapper _alertTypeMapper, AlertMapper _alertMapper, IAlertService _alertService, FileMapper _fileMapper) {
        alertTypeMapper = _alertTypeMapper;
        fileMapper = _fileMapper;
        alertMapper = _alertMapper;
        alertService = _alertService;
    }

    @PostConstruct
    public void init() {
        Thread t = new Thread(new NeatLogicThread("ALERT-MANAGER") {
            @Override
            protected void execute() {
                OriginalAlertVo originalAlertVo = null;
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        originalAlertVo = alertQueue.take();
                        new Handler(originalAlertVo).execute();
                        //CachedThreadPool.execute(new Builder(rebuildAuditVo));
                        originalAlertVo.setStatus(AlertOriginStatus.SUCCEED.getValue());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        if (originalAlertVo != null) {
                            originalAlertVo.setError(ExceptionUtils.getStackTrace(e));
                            originalAlertVo.setStatus(AlertOriginStatus.FAILED.getValue());

                        }
                    } finally {
                        alertMapper.insertAlertOrigin(originalAlertVo);
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public static void addAlert(OriginalAlertVo originalAlertVo) {
        alertQueue.offer(originalAlertVo);
    }

    static class Handler /*extends NeatLogicThread*/ {
        private final OriginalAlertVo originalAlertVo;

        public Handler(OriginalAlertVo _originalAlertVo) {
            //super(_rebuildAuditVo.getUserContext(), _rebuildAuditVo.getTenantContext());
            originalAlertVo = _originalAlertVo;
        }

        public void execute() throws Exception {
            AlertTypeVo alertTypeVo = alertTypeMapper.getAlertTypeByName(originalAlertVo.getType());
            if (alertTypeVo == null) {
                throw new AlertTypeNotFoundException(originalAlertVo.getType());
            }

            AlertVo alertVo = null;

            if (alertTypeVo.getFileId() != null) {
                FileVo fileVo = fileMapper.getFileById(alertTypeVo.getFileId());
                if (fileVo == null) {
                    throw new ApiRuntimeException("找不到转换插件，请重新上传");
                }
                alertTypeVo.setFilePath(fileVo.getPath());
                JSONObject alertObj = AlertAdaptorManager.convert(alertTypeVo, originalAlertVo.getContent());
                alertVo = JSON.toJavaObject(alertObj, AlertVo.class);
            } else {
                alertVo = JSON.parseObject(originalAlertVo.getContent(), AlertVo.class);
            }
            if (alertVo != null) {
                //补充必要信息
                alertVo.setType(alertTypeVo.getId());
                alertVo.setSource(originalAlertVo.getSource());
                if (alertVo.getAlertTime() == null) {
                    alertVo.setAlertTime(new Date());
                }
                if (StringUtils.isBlank(alertVo.getUniqueKey())) {
                    alertVo.generateUniqueKey();
                } else {
                    alertVo.setUniqueKey(Md5Util.encryptMD5(alertVo.getUniqueKey()));
                }
                alertVo.setId(originalAlertVo.getId());
                alertService.saveAlert(alertVo);
            }
        }
    }
}
