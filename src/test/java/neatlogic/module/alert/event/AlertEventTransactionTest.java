package neatlogic.module.alert.event;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.breaker.AlertBreakerHandlerFactory;
import neatlogic.framework.alert.breaker.IAlertBreakerHandler;
import neatlogic.framework.alert.crossover.IAlertSuppressionCrossoverService;
import neatlogic.framework.alert.dao.mapper.AlertBreakerMapper;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.dto.breaker.AlertBreakerPolicyVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerResultVo;
import neatlogic.framework.alert.dto.breaker.AlertEventHandlerBreakerPolicyVo;
import neatlogic.framework.alert.enums.AlertEventStatus;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventHandlerFactory;
import neatlogic.framework.alert.utils.AlertEventHandlerContextBuilder;
import neatlogic.framework.asynchronization.thread.ModuleInitApplicationListener;
import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.integration.core.IIntegrationHandler;
import neatlogic.framework.integration.core.IntegrationHandlerFactory;
import neatlogic.framework.integration.dao.mapper.IntegrationMapper;
import neatlogic.framework.integration.dto.IntegrationResultVo;
import neatlogic.framework.integration.dto.IntegrationVo;
import neatlogic.framework.transaction.core.AfterTransactionJob;
import neatlogic.framework.transaction.util.TransactionUtil;
import neatlogic.framework.util.SpringContextUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** 从公开入口验证真实 Spring 事务生命周期，仅替换 JDBC、Mapper 与插件外部操作。 */
public class AlertEventTransactionTest {
    private AbstractDataSource dataSource;
    private Object originalManager;
    private Object originalContext;
    private final Map<Long, String> saved = new ConcurrentHashMap<>();
    private final Map<Long, Long> parents = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong(100);
    private final AtomicBoolean failCommit = new AtomicBoolean();
    private final AtomicInteger businessCommits = new AtomicInteger();
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<Throwable> asyncError = new AtomicReference<>();
    private final CountDownLatch updated = new CountDownLatch(1);
    private final ThreadLocal<Object> readResource = new ThreadLocal<>();
    private final List<Object> contextResources = new ArrayList<>();
    private final List<Runnable> restorers = new ArrayList<>();
    private AlertEventMapper mapper;
    private AlertEventHandlerVo callbackConfig;
    private Object callerResource;
    private boolean suppress;
    private Long suppressedTypeId;
    private final List<Long> checkedTypes = new ArrayList<>();
    private boolean breaked;
    private boolean failUpdate;

    /** 一秒默认超时用于复现慢调用；审计状态只在真实提交回调后变为可见。 */
    @Before
    public void setUp() throws Exception {
        originalManager = field(TransactionUtil.class, "dataSourceTransactionManager").get(null);
        originalContext = field(SpringContextUtil.class, "ctx").get(null);
        dataSource = new AbstractDataSource() {
            @Override public Connection getConnection() {
                return proxy(Connection.class, (method, args) -> {
                    switch (method) {
                        case "getAutoCommit": return true;
                        case "getTransactionIsolation": return Connection.TRANSACTION_READ_COMMITTED;
                        case "isReadOnly": case "isClosed": return false;
                        case "commit":
                            if (failCommit.getAndSet(false)) { throw new java.sql.SQLException("模拟提交失败"); }
                            return null;
                        default: return null;
                    }
                });
            }
            @Override public Connection getConnection(String user, String password) { return getConnection(); }
        };
        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource);
        manager.setDefaultTimeout(1);
        new TransactionUtil(manager);
        mapper = proxy(AlertEventMapper.class, (method, args) -> {
            if (method.equals("getAlertEventPluginConfigByName")) {
                assertSame("插件配置读取应保留调用方数据可见性", readResource.get(), resource());
                return null;
            }
            if (method.equals("getAlertEventHandlerByUuid")) { return callbackConfig; }
            assertTrue("审计必须使用独立物理事务", TransactionSynchronizationManager.isActualTransactionActive());
            assertNotSame(callerResource, resource());
            checkTimeout();
            AlertEventHandlerAuditVo audit = (AlertEventHandlerAuditVo) args[0];
            if (method.equals("insertAlertEventAudit")) {
                audit.setId(ids.incrementAndGet());
                Long id = audit.getId();
                Long parent = audit.getParentId();
                afterCommit(() -> {
                    saved.put(id, AlertEventStatus.RUNNING.getValue());
                    if (parent != null) { parents.put(id, parent); }
                });
            } else if (method.equals("updateAlertEventHandlerAudit")) {
                assertTrue("更新前初始审计必须已提交", saved.containsKey(audit.getId()));
                if (failUpdate) { throw new IllegalStateException("模拟审计更新失败"); }
                Long id = audit.getId();
                String status = audit.getStatus();
                afterCommit(() -> { saved.put(id, status); updated.countDown(); });
            } else { throw new UnsupportedOperationException(method); }
            return null;
        });
        AlertBreakerMapper breakerMapper = proxy(AlertBreakerMapper.class, (method, args) -> {
            assertFalse("熔断编排不得继承父事务", TransactionSynchronizationManager.isActualTransactionActive());
            if (method.equals("getBreakerPolicyListByEventHandlerId")) {
                if (!breaked) { return Collections.emptyList(); }
                AlertEventHandlerBreakerPolicyVo relation = new AlertEventHandlerBreakerPolicyVo();
                relation.setPolicyId(1L);
                return Collections.singletonList(relation);
            }
            if (method.equals("getAlertBreakerPolicyById")) {
                AlertBreakerPolicyVo policy = new AlertBreakerPolicyVo();
                policy.setId(1L); policy.setIsActive(1); policy.setHandler("tx-test");
                return policy;
            }
            throw new UnsupportedOperationException(method);
        });
        new SpringContextUtil().setApplicationContext(proxy(ApplicationContext.class, (method, args) -> {
            assertEquals(AlertBreakerMapper.class, args[0]); return breakerMapper;
        }));
        replaceMapEntry(CrossoverServiceFactory.class, "apiMap", IAlertSuppressionCrossoverService.class,
                proxy(IAlertSuppressionCrossoverService.class, (method, args) -> {
                    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                    checkedTypes.add((Long) args[1]);
                    return suppress || (suppressedTypeId != null && suppressedTypeId.equals(args[1]));
                }));
        replaceMapEntry(AlertBreakerHandlerFactory.class, "handlerMap", "tx-test",
                proxy(IAlertBreakerHandler.class, (method, args) -> {
                    if (method.equals("execute")) {
                        AlertBreakerResultVo result = new AlertBreakerResultVo(); result.setBreaked(true); return result;
                    }
                    return null;
                }));
    }

    /** 等待线程池任务退出，再恢复所有静态装配和注册项。 */
    @After
    public void tearDown() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(6);
        while (!CachedThreadPool.getStatus().getThreadTaskList().isEmpty() && System.nanoTime() < deadline) { Thread.sleep(10); }
        boolean drained = CachedThreadPool.getStatus().getThreadTaskList().isEmpty();
        field(TransactionUtil.class, "dataSourceTransactionManager").set(null, originalManager);
        field(SpringContextUtil.class, "ctx").set(null, originalContext);
        for (Runnable restorer : restorers) { restorer.run(); }
        if (TenantContext.get() != null) { TenantContext.get().release(); }
        if (UserContext.get() != null) { UserContext.get().release(); }
        assertTrue("异步任务应退出后再恢复测试装配", drained);
        assertFalse(TransactionSynchronizationManager.hasResource(dataSource));
        assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
        if (asyncError.get() != null) { throw new AssertionError(asyncError.get()); }
    }

    /** 默认插件在独立事务中提交，提交后回调设定的失败状态不可被成功状态覆盖。 */
    @Test public void defaultTransactionAndAfterCommitOrderArePreserved() throws Exception {
        TransactionStatus caller = caller();
        try {
            DatabasePlugin handler = wire(new DatabasePlugin((alert, audit, status) -> {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                assertNotSame(callerResource, resource());
                new AfterTransactionJob<Long>("test-index").execute(1L, id -> {
                    assertEquals(AlertEventStatus.RUNNING.getValue(), saved.get(audit.getId()));
                    businessCommits.incrementAndGet();
                    audit.setStatus(AlertEventStatus.FAILED.getValue());
                }, true);
            }));
            handler.trigger(config(0), alert());
            assertEquals(1, businessCommits.get());
            assertEquals(AlertEventStatus.FAILED.getValue(), onlyStatus());
            assertSame(callerResource, resource());
            assertSame(callerResource, contextResources.get(0));
        } finally { TransactionUtil.rollbackTx(caller); }
        assertEquals(AlertEventStatus.FAILED.getValue(), onlyStatus());
    }

    /** 默认插件异常时业务不提交，但失败审计提交且原异常继续向调用方抛出。 */
    @Test public void pluginRollbackDoesNotRollbackAudit() throws Exception {
        DatabasePlugin handler = wire(new DatabasePlugin((alert, audit, status) -> {
            afterCommit(businessCommits::incrementAndGet);
            throw new IllegalStateException("模拟插件失败");
        }));
        try { handler.trigger(config(0), alert()); fail("应向同步调用方抛出异常"); }
        catch (IllegalStateException expected) { assertEquals("模拟插件失败", expected.getMessage()); }
        assertEquals(0, businessCommits.get());
        assertEquals(AlertEventStatus.FAILED.getValue(), onlyStatus());
    }

    /** 同步慢集成不持有事务；父事务过期后最终审计仍提交，且父事务资源恢复。 */
    @Test public void syncSlowIntegrationSurvivesCallerTimeout() throws Exception {
        TransactionStatus caller = caller();
        try {
            wire(slowIntegration()).trigger(config(0), alert());
            assertEquals(AlertEventStatus.SUCCEED.getValue(), onlyStatus());
            assertSame(callerResource, resource());
            try { checkTimeout(); fail("调用方应已超时"); }
            catch (TransactionTimedOutException expected) { /* 调用方超时与审计提交相互独立。 */ }
        } finally { TransactionUtil.rollbackTx(caller); }
        assertEquals(AlertEventStatus.SUCCEED.getValue(), onlyStatus());
    }

    /** 真实线程池必须在审计提交后启动，调用方随后回滚不影响慢插件最终审计。 */
    @Test public void asyncSlowIntegrationStartsAfterAuditCommit() throws Exception {
        prepareAsyncEnvironment();
        TransactionStatus caller = caller();
        try { wire(slowIntegration()).trigger(config(1), alert()); }
        finally { TransactionUtil.rollbackTx(caller); }
        assertTrue(updated.await(5, TimeUnit.SECONDS));
        assertEquals(AlertEventStatus.SUCCEED.getValue(), onlyStatus());
    }

    /** 异步插件异常不会向调度方抛出，失败审计必须独立落地。 */
    @Test public void asyncPluginExceptionIsRecordedAsFailed() throws Exception {
        prepareAsyncEnvironment();
        DatabasePlugin handler = wire(new DatabasePlugin((alert, audit, status) -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            throw new IllegalStateException("模拟异步插件失败");
        }));
        handler.trigger(config(1), alert());
        assertTrue(updated.await(5, TimeUnit.SECONDS));
        assertEquals(AlertEventStatus.FAILED.getValue(), onlyStatus());
    }

    /** 父插件有事务、子集成无事务、孙插件有事务，父子审计及结果传递仍保持一致。 */
    @Test public void nestedPluginsUseOwnTransactionPolicyAndKeepResults() throws Exception {
        DatabasePlugin child = wire(new DatabasePlugin((alert, audit, status) -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals("接口结果", alert.getPrevEventResult());
        }));
        AlertIntegrationEventHandler integration = wire(new AlertIntegrationEventHandler() {
            @Override protected AlertVo myTrigger(AlertEventHandlerVo config, AlertEventPluginVo plugin, AlertVo alert,
                    AlertEventHandlerAuditVo audit, AlertEventStatusVo status) {
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                alert.setPrevEventResult("接口结果");
                child.trigger(config(0), alert, audit.getId());
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                return alert;
            }
        });
        DatabasePlugin parent = wire(new DatabasePlugin((alert, audit, status) -> {
            Object parentResource = resource();
            integration.trigger(config(0), alert, audit.getId());
            assertSame(parentResource, resource());
        }));
        parent.trigger(config(0), alert());
        assertEquals(3, saved.size()); assertEquals(2, parents.size());
        for (String status : saved.values()) { assertEquals(AlertEventStatus.SUCCEED.getValue(), status); }
        for (Long id : parents.values()) { assertTrue(saved.containsKey(id)); }
    }

    /** 邮件分类在外层判定成功时子插件不会启动，外层通过后子插件独立检查其配置的分类。 */
    @Test public void nestedMailUsesOuterAndInnerSuppressionChecks() throws Exception {
        AlertConditionEventHandler condition = wire(new AlertConditionEventHandler());
        AlertSendMailEventHandler mail = wire(new AlertSendMailEventHandler());
        replaceMapEntry(AlertEventHandlerFactory.class, "eventMap", mail.getName(), mail);
        AlertEventHandlerVo outer = config(0);
        outer.setHandler(condition.getName());
        outer.setTypeId(10L);
        outer.setConfig(JSONObject.parseObject("{'conditionList':[{'handler':{'uuid':'mail','handler':'EMAIL'}}]}"));
        callbackConfig = config(0);
        callbackConfig.setHandler(mail.getName());
        callbackConfig.setTypeId(11L);
        suppressedTypeId = 10L;
        condition.trigger(outer, alert());
        assertEquals(Collections.singletonList(10L), checkedTypes);
        assertEquals(AlertEventStatus.SUPPRESS.getValue(), onlyStatus());

        checkedTypes.clear();
        suppressedTypeId = 11L;
        InputFromContext previous = InputFromContext.get();
        try {
            condition.trigger(outer, alert());
            assertEquals(Arrays.asList(10L, 11L), checkedTypes);
            assertEquals(AlertEventStatus.SUCCEED.getValue(), saved.get(102L));
            assertEquals(AlertEventStatus.SUPPRESS.getValue(), saved.get(103L));
            assertEquals(Long.valueOf(102L), parents.get(103L));
        } finally {
            if (previous != null) {
                InputFromContext.init(previous);
            } else if (InputFromContext.get() != null) {
                InputFromContext.get().release();
            }
        }
    }

    /** 禁用、屏蔽、熔断均跳过插件，并分别保存原有状态。 */
    @Test public void preCheckStatusesArePreserved() throws Exception {
        DatabasePlugin handler = wire(new DatabasePlugin((alert, audit, status) -> calls.incrementAndGet()));
        AlertEventHandlerVo disabled = config(0); disabled.setIsActive(0);
        handler.trigger(disabled, alert()); assertEquals(AlertEventStatus.DISABLED.getValue(), latestStatus());
        suppress = true;
        handler.trigger(config(0), alert()); assertEquals(AlertEventStatus.SUPPRESS.getValue(), latestStatus());
        suppress = false; breaked = true;
        handler.trigger(config(0), alert()); assertEquals(AlertEventStatus.BREAKED.getValue(), latestStatus());
        assertEquals(0, calls.get());
    }

    /** 初始审计提交失败时不得执行插件。 */
    @Test public void initialAuditCommitFailurePreventsExecution() throws Exception {
        failCommit.set(true);
        try { wire(new DatabasePlugin((alert, audit, status) -> calls.incrementAndGet())).trigger(config(0), alert()); fail(); }
        catch (TransactionSystemException expected) { assertNotNull(expected.getCause()); }
        assertEquals(0, calls.get()); assertTrue(saved.isEmpty());
    }

    /** 插件提交失败沿用标记失败规则，最终审计仍能正常保存。 */
    @Test public void pluginCommitFailureIsAudited() throws Exception {
        wire(new DatabasePlugin((alert, audit, status) -> failCommit.set(true))).trigger(config(0), alert());
        assertEquals(AlertEventStatus.FAILED.getValue(), onlyStatus());
    }

    /** 最终审计写入失败不覆盖同步插件原异常，且所有事务资源仍须清理。 */
    @Test public void auditFailureDoesNotMaskPluginFailure() throws Exception {
        failUpdate = true;
        try {
            wire(new DatabasePlugin((alert, audit, status) -> { throw new IllegalArgumentException("原始异常"); }))
                    .trigger(config(0), alert());
            fail();
        } catch (IllegalArgumentException expected) { assertEquals("原始异常", expected.getMessage()); }
        assertEquals(AlertEventStatus.RUNNING.getValue(), onlyStatus());
    }

    /** 插件通过状态对象指定的终态必须保留。 */
    @Test public void customStatusIsPreserved() throws Exception {
        wire(new DatabasePlugin((alert, audit, status) -> status.setStatus(AlertEventStatus.FAILED.getValue())))
                .trigger(config(0), alert());
        assertEquals(AlertEventStatus.FAILED.getValue(), onlyStatus());
    }

    /** 真实集成成功分支保持响应传递和独立数据库回调事务。 */
    @Test public void realIntegrationSuccessCallbackUsesOwnTransaction() throws Exception {
        verifyRealIntegrationCallback(200, AlertEventStatus.SUCCEED.getValue());
    }

    /** 真实集成失败分支保持失败终态，仍执行配置的失败回调。 */
    @Test public void realIntegrationFailureCallbackUsesOwnTransaction() throws Exception {
        verifyRealIntegrationCallback(500, AlertEventStatus.FAILED.getValue());
    }

    /** 仅替换集成传输接口，参数构造、成功失败分支和回调派发保持真实。 */
    private void verifyRealIntegrationCallback(int responseCode, String expectedStatus) throws Exception {
        DatabasePlugin callback = wire(new DatabasePlugin((alert, audit, status) -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals("响应正文", alert.getPrevEventResult());
            calls.incrementAndGet();
        }));
        callbackConfig = config(0);
        callbackConfig.setHandler("TX_CALLBACK");
        replaceMapEntry(AlertEventHandlerFactory.class, "eventMap", "TX_CALLBACK", callback);
        replaceMapEntry(IntegrationHandlerFactory.class, "componentMap", "tx-transport",
                proxy(IIntegrationHandler.class, (method, args) -> {
                    assertEquals("sendRequest", method);
                    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                    assertEquals(AlertEventStatus.RUNNING.getValue(), onlyStatus());
                    IntegrationResultVo result = new IntegrationResultVo();
                    result.setStatusCode(responseCode); result.setTransformedResult("响应正文");
                    return result;
                }));
        IntegrationVo integration = new IntegrationVo();
        integration.setUuid("tx-integration"); integration.setIsActive(1); integration.setHandler("tx-transport");
        integration.setConfig("{}");
        AlertIntegrationEventHandler handler = wire(new AlertIntegrationEventHandler());
        field(AlertIntegrationEventHandler.class, "integrationMapper").set(handler,
                proxy(IntegrationMapper.class, (method, args) -> integration));
        AlertEventHandlerVo config = config(0);
        JSONObject pluginConfig = new JSONObject(); pluginConfig.put("integrationUuid", "tx-integration");
        JSONObject callbackEntry = new JSONObject(); callbackEntry.put("handler", "TX_CALLBACK");
        callbackEntry.put("uuid", "tx-callback");
        JSONArray callbacks = new JSONArray(); callbacks.add(callbackEntry);
        if (responseCode == 200) { pluginConfig.put("successCallbackList", callbacks); }
        else { pluginConfig.put("failedCallbackList", callbacks); }
        config.setConfig(pluginConfig);
        AlertVo result = handler.trigger(config, alert());
        assertEquals("响应正文", result.getPrevEventResult());
        assertEquals(1, calls.get());
        assertEquals(expectedStatus, saved.get(101L));
        assertEquals(AlertEventStatus.SUCCEED.getValue(), saved.get(102L));
        assertEquals(Long.valueOf(101L), parents.get(102L));
    }

    /** 保留真实集成插件策略，只用可控慢操作替换外部接口。 */
    private AlertIntegrationEventHandler slowIntegration() {
        return new AlertIntegrationEventHandler() {
            @Override protected AlertVo myTrigger(AlertEventHandlerVo config, AlertEventPluginVo plugin, AlertVo alert,
                    AlertEventHandlerAuditVo audit, AlertEventStatusVo status) {
                try {
                    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                    assertEquals(AlertEventStatus.RUNNING.getValue(), saved.get(audit.getId()));
                    Thread.sleep(1100);
                    checkTimeout();
                    calls.incrementAndGet();
                    return alert;
                } catch (Throwable failure) {
                    asyncError.compareAndSet(null, failure);
                    throw new AssertionError(failure);
                }
            }
        };
    }

    /** 使用普通数据库插件的真实默认策略，替换具体业务操作。 */
    private static class DatabasePlugin extends AlertConditionEventHandler {
        private final Body body;
        private DatabasePlugin(Body body) { this.body = body; }
        @Override protected AlertVo myTrigger(AlertEventHandlerVo config, AlertEventPluginVo plugin, AlertVo alert,
                AlertEventHandlerAuditVo audit, AlertEventStatusVo status) {
            body.run(alert, audit, status); return alert;
        }
    }
    /** 为测试定义具体插件副作用，不替换基类执行流程。 */
    private interface Body { void run(AlertVo alert, AlertEventHandlerAuditVo audit, AlertEventStatusVo status); }
    /** 代理方法允许保留 JDBC 的受检异常。 */
    private interface MethodCall { Object call(String method, Object[] args) throws Throwable; }

    /** 仅替换依赖，公开 trigger 和事务执行逻辑均保持真实。 */
    private <T extends AlertEventHandlerBase> T wire(T handler) throws Exception {
        field(AlertEventHandlerBase.class, "alertEventMapper").set(handler, mapper);
        field(AlertEventHandlerBase.class, "alertEventHandlerContextBuilder").set(handler, new AlertEventHandlerContextBuilder() {
            @Override public AlertVo build(AlertVo alert) {
                readResource.set(resource()); contextResources.add(resource()); return alert;
            }
        });
        return handler;
    }
    /** 模拟模块加载完成，以真实线程池运行异步插件。 */
    private void prepareAsyncEnvironment() {
        TenantContext.init("event-tx-test"); UserContext.init((UserContext) null);
        if (ModuleInitApplicationListener.getModuleinitphaser().getPhase() == 0) {
            ModuleInitApplicationListener.getModuleinitphaser().register();
            ModuleInitApplicationListener.getModuleinitphaser().arriveAndDeregister();
        }
    }
    /** 开启可用于断言恢复情况的调用方事务。 */
    private TransactionStatus caller() { TransactionStatus status = TransactionUtil.openNewTx(); callerResource = resource(); return status; }
    /** 创建最小可执行配置。 */
    private AlertEventHandlerVo config(int async) {
        AlertEventHandlerVo config = new AlertEventHandlerVo(); config.setId(10L); config.setHandler("INTEGRATION");
        config.setName("事务测试"); config.setIsActive(1); config.setIsAsync(async); return config;
    }
    /** 创建固定 ID 的告警，避免依赖运行环境的 ID 生成配置。 */
    private AlertVo alert() { AlertVo alert = new AlertVo(); alert.setId(1L); return alert; }
    /** 返回线程绑定的实际 JDBC 资源。 */
    private Object resource() { return TransactionSynchronizationManager.getResource(dataSource); }
    /** 采用 Spring 真实的事务剩余时间检查。 */
    private void checkTimeout() throws Exception {
        DataSourceUtils.applyTransactionTimeout(proxy(Statement.class, (method, args) -> null), dataSource);
    }
    /** 模拟数据库提交可见性，而非依赖可变审计对象的内存状态。 */
    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
    }
    /** 读取唯一事件终态。 */
    private String onlyStatus() { assertEquals(1, saved.size()); return saved.values().iterator().next(); }
    /** 读取最近一次事件终态。 */
    private String latestStatus() { return saved.get(ids.get()); }
    /** 测试通过反射注入和恢复依赖，不向生产类添加测试入口。 */
    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    /** 静态工厂仅替换单个测试注册项，测试后恢复原值。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void replaceMapEntry(Class<?> owner, String name, Object key, Object value) throws Exception {
        Map map = (Map) field(owner, name).get(null);
        boolean existed = map.containsKey(key); Object original = map.put(key, value);
        restorers.add(() -> { if (existed) { map.put(key, original); } else { map.remove(key); } });
    }
    /** 创建轻量依赖替身，并保留 Object 方法的稳定行为。 */
    private <T> T proxy(Class<T> type, MethodCall action) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if (method.getName().equals("equals")) { return proxy == args[0]; }
                if (method.getName().equals("hashCode")) { return System.identityHashCode(proxy); }
                return type.getSimpleName() + "替身";
            }
            return action.call(method.getName(), args);
        }));
    }
}
