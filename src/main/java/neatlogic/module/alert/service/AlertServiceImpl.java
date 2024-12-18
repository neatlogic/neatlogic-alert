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

package neatlogic.module.alert.service;

import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.alert.dto.AlertRelVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.event.AlertEventManager;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.dto.elasticsearch.IndexResultVo;
import neatlogic.framework.exception.elasticsearch.ElasticSearchIndexNotFoundException;
import neatlogic.framework.store.elasticsearch.ElasticsearchIndexFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchIndex;
import neatlogic.framework.transaction.core.AfterTransactionJob;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.queue.AlertActionManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements IAlertService {
    @Resource
    private AlertMapper alertMapper;

    @Resource
    private AlertEventMapper alertEventMapper;

    @Override
    public void saveAlert(AlertVo alertVo) {
        AlertVo oldAlertVo = alertMapper.getAlertByUniqueKey(alertVo.getUniqueKey());
        if (oldAlertVo != null) {
            if (oldAlertVo.getId().equals(alertVo.getId())) {
                return;
            }
            AlertRelVo alertRelVo = new AlertRelVo();
            alertRelVo.setFromAlertId(oldAlertVo.getId());
            alertRelVo.setToAlertId(alertVo.getId());
            alertMapper.saveAlertRel(alertRelVo);
            alertVo.setFromAlertId(oldAlertVo.getId());
        }
        alertMapper.insertAlert(alertVo);
        AlertEventManager.doEvent(AlertEventType.ALERT_SAVE, alertVo);

        if (MapUtils.isNotEmpty(alertVo.getAttrObj())) {
            alertMapper.saveAlertAttr(alertVo);
        }
        IElasticsearchIndex<AlertVo> indexHandler = ElasticsearchIndexFactory.getIndex("ALERT");
        if (indexHandler == null) {
            throw new ElasticSearchIndexNotFoundException("ALERT");
        }
        indexHandler.createDocument(alertVo);
        AfterTransactionJob<AlertVo> job = new AfterTransactionJob<>("AFTER-SAVE-ALERT");
        job.execute(alertVo, AlertActionManager::handler);
    }

    @Override
    public List<AlertVo> searchAlert(AlertVo alertVo) {

        IElasticsearchIndex<AlertVo> index = ElasticsearchIndexFactory.getIndex("ALERT");
        IndexResultVo indexResultVo = index.searchDocument(alertVo, 1, 20);
        if (CollectionUtils.isNotEmpty(indexResultVo.getIdList())) {
            alertVo.setIdList(indexResultVo.getIdList().stream().map(Long::parseLong).collect(Collectors.toList()));
            alertVo.setCurrentPage(indexResultVo.getCurrentPage());
            alertVo.setPageCount(indexResultVo.getPageCount());
            alertVo.setRowNum(indexResultVo.getRowNum());
            return alertMapper.selectAlertById(alertVo);

        }
        return new ArrayList<>();
    }

    @Override
    public List<AlertEventHandlerVo> listAlertEventHandler(AlertEventHandlerVo alertEventHandlerVo) {
        // 获取平铺的结果
        List<AlertEventHandlerVo> handlerList = alertEventMapper.listEventHandler(alertEventHandlerVo);

        // 按 ID 映射
        Map<Long, AlertEventHandlerVo> handlerMap = handlerList.stream()
                .collect(Collectors.toMap(AlertEventHandlerVo::getId, h -> h));
        // 构造层级结构
        List<AlertEventHandlerVo> rootHandlers = new ArrayList<>();
        for (AlertEventHandlerVo handler : handlerList) {
            if (handler.getParentId() == null) {
                // 顶层节点
                rootHandlers.add(handler);
            } else {
                // 子节点
                AlertEventHandlerVo parent = handlerMap.get(handler.getParentId());
                if (parent != null) {
                    if (parent.getHandlerList() == null) {
                        parent.setHandlerList(new ArrayList<>());
                    }
                    parent.getHandlerList().add(handler);
                }
            }
        }
        return rootHandlers;
    }
}
