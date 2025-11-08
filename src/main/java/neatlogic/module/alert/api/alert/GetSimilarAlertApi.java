/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.alert.api.alert;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Like;
import co.elastic.clients.elasticsearch._types.query_dsl.MoreLikeThisQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.store.elasticsearch.ElasticsearchClientFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetSimilarAlertApi extends PrivateApiComponentBase {


    @Override
    public String getToken() {
        return "/alert/similar/get";
    }

    @Override
    public String getName() {
        return "获取相似告警";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "title", desc = "告警标题", isRequired = true, type = ApiParamType.STRING),
    })
    @Output({
            @Param(explode = AlertVo.class)
    })
    @Description(desc = "获取告警详情")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        ElasticsearchClient client = ElasticsearchClientFactory.getClient();
        Query query = new Query.Builder()
                .moreLikeThis(new MoreLikeThisQuery.Builder()
                        .fields("title")  // 相似度计算的字段
                        .like(Like.of(d -> d.text(jsonObj.getString("title"))))                       // 输入文本
                        .minTermFreq(1)
                        .minDocFreq(2)
                        .maxQueryTerms(15)
                        .minWordLength(4)
                        .build())
                .build();

        SearchResponse<Map> response = client.search(s -> s
                        .index("tlcb35_alert")
                        .query(query)
                        .size(5),
                Map.class
        );
        List<String> returnList = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            returnList.add(hit.source().toString());
        }



        return returnList;
    }

    public static void main(String[] args) throws Exception {
        // ========= 1. 从 classpath 读取 vocab.txt =========
        Map<String, Integer> vocab = new HashMap<>();
        try (InputStream is = GetSimilarAlertApi.class.getClassLoader().getResourceAsStream("onnx/vocab.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            int idx = 0;
            while ((line = br.readLine()) != null) {
                vocab.put(line.trim(), idx++);
            }
        }

        // ========= 2. 简单分词（空格切分） =========
        String text = "CPU usage exceeded 90% on server node1";
        String[] tokens = text.toLowerCase().split("\\s+");
        int maxLen = 32;
        long[] inputIds = new long[maxLen];
        long[] attentionMask = new long[maxLen];
        for (int i = 0; i < Math.min(tokens.length, maxLen); i++) {
            inputIds[i] = vocab.getOrDefault(tokens[i], vocab.getOrDefault("[UNK]", 100));
            attentionMask[i] = 1;
        }
        long[] tokenTypeIds = new long[maxLen]; // 默认全 0

        // ========= 3. 从 classpath 读取 ONNX 模型，写到临时文件 =========
        InputStream modelStream = GetSimilarAlertApi.class.getClassLoader().getResourceAsStream("onnx/all-MiniLM-L6-v2.onnx");
        if (modelStream == null) throw new FileNotFoundException("模型文件 all-MiniLM-L6-v2.onnx 不存在");
        File tempModel = File.createTempFile("minilm", ".onnx");
        try (OutputStream os = new FileOutputStream(tempModel)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = modelStream.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
        }

        // ========= 4. 加载 ONNX 模型 =========
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        OrtSession session = env.createSession(tempModel.getAbsolutePath(), new OrtSession.SessionOptions());

        // ========= 5. 构造输入 =========
        Map<String, OnnxTensor> feed = new HashMap<>();
        feed.put("input_ids", OnnxTensor.createTensor(env, new long[][]{inputIds}));
        feed.put("attention_mask", OnnxTensor.createTensor(env, new long[][]{attentionMask}));
        // 如果模型需要 token_type_ids，就传进去
        if (session.getInputNames().contains("token_type_ids")) {
            feed.put("token_type_ids", OnnxTensor.createTensor(env, new long[][]{tokenTypeIds}));
        }

        // ========= 6. 执行推理 =========
        try (OrtSession.Result result = session.run(feed)) {
            float[][][] lastHidden = (float[][][]) result.get(0).getValue(); // (1, seq_len, hidden_dim)

            // 平均池化
            float[] sum = new float[lastHidden[0][0].length];
            int count = 0;
            for (int i = 0; i < lastHidden[0].length; i++) {
                if (attentionMask[i] == 1) {
                    for (int j = 0; j < sum.length; j++) sum[j] += lastHidden[0][i][j];
                    count++;
                }
            }
            for (int j = 0; j < sum.length; j++) sum[j] /= Math.max(1, count);

            // ========= 7. 输出 =========
            System.out.println("向量维度 = " + sum.length);
            System.out.println("前 8 个值 = " + Arrays.toString(Arrays.copyOf(sum, 8)));
        }
    }
}
