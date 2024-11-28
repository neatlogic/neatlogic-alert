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

import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.asynchronization.queue.NeatLogicBlockingQueue;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.dao.mapper.AlertTypeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class AlertActionManager {
    private static AlertMapper alertMapper;
    private static AlertTypeMapper alertTypeMapper;
    private static FileMapper fileMapper;
    private static final Logger logger = LoggerFactory.getLogger(AlertActionManager.class);

    private static final NeatLogicBlockingQueue<AlertVo> alertQueue = new NeatLogicBlockingQueue<>(new LinkedBlockingQueue<>());

    @Autowired
    public AlertActionManager(AlertTypeMapper _alertTypeMapper, AlertMapper _alertMapper, FileMapper _fileMapper) {
        alertTypeMapper = _alertTypeMapper;
        fileMapper = _fileMapper;
        alertMapper = _alertMapper;
    }

    @PostConstruct
    public void init() {
        Thread t = new Thread(new NeatLogicThread("ALERT-ACTION-MANAGER") {
            @Override
            protected void execute() {
                AlertVo alertVo = null;
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        alertVo = alertQueue.take();
                        new Handler(alertVo).execute();
                        //CachedThreadPool.execute(new Builder(rebuildAuditVo));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public static void handler(AlertVo alertVo) {
        alertQueue.offer(alertVo);
    }

    static class Handler /*extends NeatLogicThread*/ {
        private final AlertVo alertVo;

        public Handler(AlertVo _alertVo) {
            //super(_rebuildAuditVo.getUserContext(), _rebuildAuditVo.getTenantContext());
            alertVo = _alertVo;
        }

        public void execute() throws Exception {
        }
    }
}
