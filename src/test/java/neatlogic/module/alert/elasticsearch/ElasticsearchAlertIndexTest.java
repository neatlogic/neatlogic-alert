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

package neatlogic.module.alert.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.AlertViewVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.enums.AlertSearchMode;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.module.alert.dao.mapper.AlertViewMapper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

public class ElasticsearchAlertIndexTest {
    private final ElasticsearchAlertIndex alertIndex = new ElasticsearchAlertIndex();

    /**
     * 验证全部AND条件组成一个must查询。
     */
    @Test
    public void allAndConditionsBuildMustQuery() {
        Query ruleQuery = buildRuleQuery(rule(groups(
                group(fields("a", "b", "c"), relations("and", "and"))
        ), relations()));

        assertMustFields(ruleQuery, "attrObj.a", "attrObj.b", "attrObj.c");
    }

    /**
     * 验证全部OR条件组成一个至少命中一项的should查询。
     */
    @Test
    public void allOrConditionsBuildShouldQuery() {
        Query ruleQuery = buildRuleQuery(rule(groups(
                group(fields("a", "b", "c"), relations("or", "or"))
        ), relations()));

        assertShouldFields(ruleQuery, "attrObj.a", "attrObj.b", "attrObj.c");
    }

    /**
     * 验证A AND B OR C按AND优先组合。
     */
    @Test
    public void andBeforeOrBuildsFirstAndClause() {
        Query ruleQuery = buildRuleQuery(rule(groups(
                group(fields("a", "b", "c"), relations("and", "or"))
        ), relations()));

        Assert.assertEquals("1", ruleQuery.bool().minimumShouldMatch());
        Assert.assertEquals(2, ruleQuery.bool().should().size());
        assertMustFields(ruleQuery.bool().should().get(0), "attrObj.a", "attrObj.b");
        assertExistsField(ruleQuery.bool().should().get(1), "attrObj.c");
    }

    /**
     * 验证A OR B AND C按AND优先组合。
     */
    @Test
    public void andBeforeOrBuildsSecondAndClause() {
        Query ruleQuery = buildRuleQuery(rule(groups(
                group(fields("a", "b", "c"), relations("or", "and"))
        ), relations()));

        Assert.assertEquals("1", ruleQuery.bool().minimumShouldMatch());
        Assert.assertEquals(2, ruleQuery.bool().should().size());
        assertExistsField(ruleQuery.bool().should().get(0), "attrObj.a");
        assertMustFields(ruleQuery.bool().should().get(1), "attrObj.b", "attrObj.c");
    }

    /**
     * 验证多个连续AND分支通过OR连接。
     */
    @Test
    public void multipleAndClausesAreConnectedByOr() {
        Query ruleQuery = buildRuleQuery(rule(groups(
                group(fields("a", "b", "c", "d"), relations("and", "or", "and"))
        ), relations()));

        Assert.assertEquals(2, ruleQuery.bool().should().size());
        assertMustFields(ruleQuery.bool().should().get(0), "attrObj.a", "attrObj.b");
        assertMustFields(ruleQuery.bool().should().get(1), "attrObj.c", "attrObj.d");
    }

    /**
     * 验证条件组之间同样遵循AND优先规则。
     */
    @Test
    public void conditionGroupsUseSameOperatorPrecedence() {
        Query ruleQuery = buildRuleQuery(rule(groups(
                group(fields("a"), relations()),
                group(fields("b"), relations()),
                group(fields("c"), relations())
        ), relations("and", "or")));

        Assert.assertEquals(2, ruleQuery.bool().should().size());
        assertMustFields(ruleQuery.bool().should().get(0), "attrObj.a", "attrObj.b");
        assertExistsField(ruleQuery.bool().should().get(1), "attrObj.c");
    }

    /**
     * 验证高级检索OR规则不会绕过普通级别筛选条件。
     */
    @Test
    public void advancedRuleIsRequiredTogetherWithOrdinaryFilters() {
        AlertVo alertVo = buildAlertVo(rule(groups(
                group(fields("a", "b"), relations("or"))
        ), relations()));
        alertVo.setLevelList(Collections.singletonList(1));

        Query rootQuery = alertIndex.myBuildQuery(alertVo);

        Assert.assertEquals(2, rootQuery.bool().must().size());
        Assert.assertTrue(rootQuery.bool().must().get(0).isTerms());
        assertShouldFields(rootQuery.bool().must().get(1), "attrObj.a", "attrObj.b");
    }

    /**
     * 验证连接符数量异常时按历史兼容规则回退为全部AND。
     */
    @Test
    public void incompleteRelationsFallBackToAnd() {
        Query ruleQuery = buildRuleQuery(rule(groups(
                group(fields("a", "b", "c"), relations("or"))
        ), relations()));

        assertMustFields(ruleQuery, "attrObj.a", "attrObj.b", "attrObj.c");
    }

    /**
     * 验证无效条件被跳过后，剩余条件安全回退为AND。
     */
    @Test
    public void skippedInvalidConditionFallsBackToAnd() {
        JSONArray conditionList = conditions(fields("a"));
        conditionList.add(invalidCondition());
        conditionList.add(condition("c"));
        Query ruleQuery = buildRuleQuery(rule(groups(
                groupWithConditionList(conditionList, relations("or", "or"))
        ), relations()));

        assertMustFields(ruleQuery, "attrObj.a", "attrObj.c");
    }

    /**
     * 验证单条件直接作为规则查询，空规则不追加根查询条件。
     */
    @Test
    public void singleAndEmptyRulesRemainCompatible() {
        Query singleRuleQuery = buildRuleQuery(rule(groups(
                group(fields("a"), relations())
        ), relations()));
        assertExistsField(singleRuleQuery, "attrObj.a");

        Query emptyRootQuery = alertIndex.myBuildQuery(buildAlertVo(rule(groups(), relations())));
        Assert.assertTrue(emptyRootQuery.bool().must().isEmpty());
    }

    /**
     * 验证不支持的连接符继续抛出异常。
     */
    @Test(expected = IllegalArgumentException.class)
    public void unsupportedRelationStillFails() {
        buildRuleQuery(rule(groups(
                group(fields("a", "b"), relations("xor"))
        ), relations()));
    }

    /** 同一规则随访问用户动态解析，并保留已保存的动态标识。 */
    @Test
    public void loginUserResolvesForEachViewerWithoutChangingRule() {
        JSONObject rule = userRule("like", "common#loginuser");
        String original = rule.toJSONString();
        UserContext context = UserContext.init((UserContext) null);
        try {
            context.setUserUuid("viewer-a");
            assertUserMatch(buildRuleQuery(rule).bool().should().get(0), "viewer-a");
            context.setUserUuid("viewer-b");
            assertUserMatch(buildRuleQuery(rule).bool().should().get(0), "viewer-b");
            Assert.assertEquals(original, rule.toJSONString());
        } finally {
            context.release();
        }
    }

    /** 动态用户与指定用户混选时，包含和不包含均使用原有组合语义。 */
    @Test
    public void loginUserCanMixWithExplicitUsers() {
        UserContext context = UserContext.init((UserContext) null);
        try {
            context.setUserUuid("viewer-a");
            for (String expression : new String[]{"like", "notlike"}) {
                JSONObject rule = userRule(expression, "common#loginuser", "user#explicit", "legacy-uuid");
                Query query = buildRuleQuery(rule);
                List<Query> clauses;
                if ("like".equals(expression)) {
                    clauses = query.bool().should();
                } else {
                    clauses = query.bool().mustNot();
                }
                Assert.assertEquals(3, clauses.size());
                assertUserMatch(clauses.get(0), "viewer-a");
                assertUserMatch(clauses.get(1), "explicit");
                assertUserMatch(clauses.get(2), "legacy-uuid");
                Assert.assertEquals("common#loginuser", rule.getJSONArray("conditionGroupList").getJSONObject(0)
                        .getJSONArray("conditionList").getJSONObject(0).getJSONArray("valueList").getString(0));
            }
        } finally {
            context.release();
        }
    }

    /** 按视图名加载的规则同样动态解析，且不改变组间逻辑或持久化配置。 */
    @Test
    public void savedViewResolvesLoginUserAcrossConditionGroups() throws Exception {
        JSONObject rule = userRule("like", "common#loginuser");
        rule.getJSONArray("conditionGroupList").add(group(fields("a"), relations()));
        rule.put("conditionGroupRelList", relations("or"));
        String original = rule.toJSONString();
        AlertViewVo view = new AlertViewVo();
        JSONObject config = new JSONObject();
        config.put("rule", rule);
        view.setConfig(config);
        Field mapper = ElasticsearchAlertIndex.class.getDeclaredField("alertViewMapper");
        mapper.setAccessible(true);
        mapper.set(alertIndex, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{AlertViewMapper.class},
                (proxy, method, args) -> {
                    Assert.assertEquals("getAlertViewByName", method.getName());
                    Assert.assertEquals("shared-view", args[0]);
                    return view;
                }));
        UserContext context = UserContext.init((UserContext) null);
        try {
            for (String viewer : new String[]{"viewer-a", "viewer-b"}) {
                context.setUserUuid(viewer);
                AlertVo request = buildAlertVo(null);
                request.setViewName("shared-view");
                Query query = alertIndex.myBuildQuery(request).bool().must().get(0);
                Assert.assertEquals("1", query.bool().minimumShouldMatch());
                assertUserMatch(query.bool().should().get(0).bool().should().get(0), viewer);
                assertExistsField(query.bool().should().get(1), "attrObj.a");
            }
            Assert.assertEquals(original, rule.toJSONString());
        } finally {
            context.release();
        }
    }

    /** 人员条件的空值判断继续使用存在性查询。 */
    @Test
    public void userNullChecksRemainCompatible() {
        Query present = buildRuleQuery(userRule("is-not-null"));
        assertExistsField(present, "userList");
        Query absent = buildRuleQuery(userRule("is-null"));
        assertExistsField(absent.bool().mustNot().get(0), "userList");
    }

    /** 构建视图处理人条件。 */
    private JSONObject userRule(String expression, String... users) {
        JSONObject condition = new JSONObject();
        condition.put("id", "const_userList");
        condition.put("expression", expression);
        condition.put("valueList", fields(users));
        JSONArray conditions = new JSONArray();
        conditions.add(condition);
        return rule(groups(groupWithConditionList(conditions, relations())), relations());
    }

    /** 验证最终匹配的是人员索引中的实际 UUID。 */
    private void assertUserMatch(Query query, String userUuid) {
        Assert.assertTrue(query.isMatch());
        Assert.assertEquals("userList", query.match().field());
        Assert.assertEquals(userUuid, query.match().query().stringValue());
    }

    /**
     * 构建并返回根查询中的高级检索规则查询。
     */
    private Query buildRuleQuery(JSONObject rule) {
        Query rootQuery = alertIndex.myBuildQuery(buildAlertVo(rule));
        List<Query> mustQueryList = rootQuery.bool().must();
        Assert.assertEquals(1, mustQueryList.size());
        return mustQueryList.get(0);
    }

    /**
     * 构建仅包含高级检索规则的告警查询参数。
     */
    private AlertVo buildAlertVo(JSONObject rule) {
        AlertVo alertVo = new AlertVo();
        alertVo.setSearchMode(AlertSearchMode.FLAT.getValue());
        alertVo.setRule(rule);
        return alertVo;
    }

    /**
     * 构建高级检索规则。
     */
    private JSONObject rule(JSONArray groupList, JSONArray groupRelList) {
        JSONObject rule = new JSONObject();
        rule.put("conditionGroupList", groupList);
        rule.put("conditionGroupRelList", groupRelList);
        return rule;
    }

    /**
     * 构建条件组。
     */
    private JSONObject group(JSONArray fieldList, JSONArray relList) {
        JSONObject group = new JSONObject();
        group.put("conditionList", conditions(fieldList));
        group.put("conditionRelList", relList);
        return group;
    }

    /**
     * 构建已经组装好条件列表的条件组。
     */
    private JSONObject groupWithConditionList(JSONArray conditionList, JSONArray relList) {
        JSONObject group = new JSONObject();
        group.put("conditionList", conditionList);
        group.put("conditionRelList", relList);
        return group;
    }

    /**
     * 根据字段列表构建条件列表。
     */
    private JSONArray conditions(JSONArray fieldList) {
        JSONArray conditionList = new JSONArray();
        for (int i = 0; i < fieldList.size(); i++) {
            conditionList.add(condition(fieldList.getString(i)));
        }
        return conditionList;
    }

    /**
     * 构建存在性判断条件。
     */
    private JSONObject condition(String field) {
        JSONObject condition = new JSONObject();
        condition.put("id", "attr_" + field);
        condition.put("expression", "is-not-null");
        condition.put("valueList", new JSONArray());
        return condition;
    }

    /**
     * 构建会被查询构造器跳过的无效条件。
     */
    private JSONObject invalidCondition() {
        JSONObject condition = new JSONObject();
        condition.put("id", "");
        condition.put("expression", "");
        condition.put("valueList", new JSONArray());
        return condition;
    }

    /**
     * 构建字符串数组。
     */
    private JSONArray fields(String... fieldArray) {
        JSONArray fieldList = new JSONArray();
        Collections.addAll(fieldList, fieldArray);
        return fieldList;
    }

    /**
     * 构建连接符数组。
     */
    private JSONArray relations(String... relArray) {
        JSONArray relList = new JSONArray();
        Collections.addAll(relList, relArray);
        return relList;
    }

    /**
     * 构建条件组数组。
     */
    private JSONArray groups(JSONObject... groupArray) {
        JSONArray groupList = new JSONArray();
        Collections.addAll(groupList, groupArray);
        return groupList;
    }

    /**
     * 验证AND查询中的字段顺序。
     */
    private void assertMustFields(Query query, String... expectedFieldArray) {
        Assert.assertTrue(query.isBool());
        Assert.assertEquals(expectedFieldArray.length, query.bool().must().size());
        for (int i = 0; i < expectedFieldArray.length; i++) {
            assertExistsField(query.bool().must().get(i), expectedFieldArray[i]);
        }
    }

    /**
     * 验证OR查询中的字段顺序和最小命中数。
     */
    private void assertShouldFields(Query query, String... expectedFieldArray) {
        Assert.assertTrue(query.isBool());
        Assert.assertEquals("1", query.bool().minimumShouldMatch());
        Assert.assertEquals(expectedFieldArray.length, query.bool().should().size());
        for (int i = 0; i < expectedFieldArray.length; i++) {
            assertExistsField(query.bool().should().get(i), expectedFieldArray[i]);
        }
    }

    /**
     * 验证存在性查询字段。
     */
    private void assertExistsField(Query query, String expectedField) {
        Assert.assertTrue(query.isExists());
        Assert.assertEquals(expectedField, query.exists().field());
    }
}
