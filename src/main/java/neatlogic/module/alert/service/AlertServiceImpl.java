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

import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.exception.elasticsearch.ElasticSearchIndexNotFoundException;
import neatlogic.framework.store.elasticsearch.ElasticsearchIndexFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchIndex;
import neatlogic.framework.transaction.core.AfterTransactionJob;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.queue.AlertActionManager;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AlertServiceImpl implements IAlertService {
    @Resource
    private AlertMapper alertMapper;

    @Override
    public void saveAlert(AlertVo alertVo) {
        AlertVo oldAlertVo = alertMapper.getAlertByUniqueKey(alertVo.getUniqueKey());
        if (oldAlertVo != null) {
            alertVo.setId(oldAlertVo.getId());
            alertVo.setAlertCount(oldAlertVo.getAlertCount() + 1);
            alertMapper.updateAlert(alertVo);
        } else {
            alertMapper.insertAlert(alertVo);
        }
        IElasticsearchIndex<AlertVo> indexHandler = ElasticsearchIndexFactory.getIndex("ALERT");
        if (indexHandler == null) {
            throw new ElasticSearchIndexNotFoundException("ALERT");
        }
        indexHandler.createDocument(alertVo);
        AfterTransactionJob<AlertVo> job = new AfterTransactionJob<>("AFTER-SAVE-ALERT");
        job.execute(alertVo, AlertActionManager::handler);
    }
}
