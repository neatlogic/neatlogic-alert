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
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.enums.AlertSearchMode;
import org.junit.Assert;
import org.junit.Test;

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
