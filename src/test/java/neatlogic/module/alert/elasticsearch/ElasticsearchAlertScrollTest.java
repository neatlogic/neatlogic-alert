package neatlogic.module.alert.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** 验证快照遍历跨批次、深分页与资源回收行为，无需连接真实搜索服务。 */
public class ElasticsearchAlertScrollTest {

    /** 超过普通搜索窗口时仍应消费全部快照 ID，且只发起一次初始查询。 */
    @Test
    public void consumesSnapshotBeyondSearchWindowAndClearsLatestScroll() throws Exception {
        FakeClient client = new FakeClient(12050);
        List<Long> ids = new ArrayList<>();
        consume(client, batch -> {
            ids.addAll(batch);
            // 模拟业务写入改变实时搜索结果；后续遍历仍只使用既有 Scroll。
            client.liveMatchCount = 0;
        });
        Assert.assertEquals(12050, ids.size());
        Assert.assertEquals(Long.valueOf(1), ids.get(0));
        Assert.assertEquals(Long.valueOf(12050), ids.get(ids.size() - 1));
        Assert.assertEquals(1, client.searchCalls);
        Assert.assertEquals(13, client.scrollCalls);
        Assert.assertEquals("scroll-13", client.clearedScrollId);
    }

    /** 空结果也可能已占用 Scroll 上下文，必须释放。 */
    @Test
    public void clearsScrollForEmptyResult() throws Exception {
        FakeClient client = new FakeClient(0);
        consume(client, batch -> Assert.fail("空快照不应回调"));
        Assert.assertEquals(0, client.scrollCalls);
        Assert.assertEquals("scroll-0", client.clearedScrollId);
    }

    /** 业务消费异常应保持原异常，清理失败只能作为附加信息。 */
    @Test
    public void consumerFailureIsNotHiddenByCleanupFailure() throws Exception {
        FakeClient client = new FakeClient(1);
        client.cleanupFailure = true;
        IllegalStateException failure = new IllegalStateException("业务失败");
        try {
            consume(client, batch -> { throw failure; });
            Assert.fail("必须抛出业务失败");
        } catch (IllegalStateException e) {
            Assert.assertSame(failure, e);
            Assert.assertEquals(1, e.getSuppressed().length);
            Assert.assertEquals("清理失败", e.getSuppressed()[0].getMessage());
        }
        Assert.assertEquals("scroll-0", client.clearedScrollId);
    }

    /** 翻页异常不能被误认为遍历结束，且仍需释放最近一次成功返回的上下文。 */
    @Test
    public void scrollFailurePropagatesAndReleasesContext() throws Exception {
        FakeClient client = new FakeClient(2000);
        client.scrollFailure = true;
        try {
            consume(client, batch -> { });
            Assert.fail("必须抛出翻页失败");
        } catch (IOException e) {
            Assert.assertEquals("翻页失败", e.getMessage());
        }
        Assert.assertEquals("scroll-0", client.clearedScrollId);
    }

    /** 超时返回的部分结果不得交付业务处理。 */
    @Test
    public void timedOutSearchFailsBeforeConsumerAndReleasesContext() throws Exception {
        FakeClient client = new FakeClient(1);
        client.timedOut = true;
        try {
            consume(client, batch -> Assert.fail("超时结果不应回调"));
            Assert.fail("必须报告超时");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("超时"));
        }
        Assert.assertEquals("scroll-0", client.clearedScrollId);
    }

    /** 通过反射测试私有资源管理算法，避免为测试增加生产装配入口。 */
    private void consume(FakeClient client, Consumer<List<Long>> consumer) throws Exception {
        Method method = ElasticsearchAlertIndex.class.getDeclaredMethod("consumeScroll",
                ElasticsearchClient.class, SearchRequest.class, Consumer.class);
        method.setAccessible(true);
        try {
            method.invoke(null, client, new SearchRequest.Builder().index("test_alert").size(1000).build(), consumer);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw (Error) e.getCause();
        }
    }

    /** 以固定 ID 集合模拟 Scroll，实时匹配数量变化不改变快照。 */
    private static class FakeClient extends ElasticsearchClient {
        private final int snapshotSize;
        private int liveMatchCount;
        private int offset;
        private int searchCalls;
        private int scrollCalls;
        private String clearedScrollId;
        private boolean cleanupFailure;
        private boolean scrollFailure;
        private boolean timedOut;

        private FakeClient(int count) {
            super(null);
            snapshotSize = count;
            liveMatchCount = count;
        }

        /** 返回快照首批 ID。 */
        @Override
        @SuppressWarnings("unchecked")
        public <T> SearchResponse<T> search(SearchRequest request, Class<T> documentClass) {
            searchCalls++;
            Assert.assertEquals(snapshotSize, liveMatchCount);
            return (SearchResponse<T>) new SearchResponse.Builder<Void>()
                    .took(1).timedOut(timedOut).shards(s -> s.total(1).successful(1).failed(0))
                    .scrollId("scroll-0").hits(h -> h.hits(nextHits())).build();
        }

        /** 不重跑实时查询，只消费快照剩余 ID。 */
        @Override
        @SuppressWarnings("unchecked")
        public <T> ScrollResponse<T> scroll(ScrollRequest request, Class<T> documentClass) throws IOException {
            Assert.assertEquals("scroll-" + scrollCalls, request.scrollId());
            scrollCalls++;
            if (scrollFailure) {
                throw new IOException("翻页失败");
            }
            return (ScrollResponse<T>) new ScrollResponse.Builder<Void>()
                    .took(1).timedOut(false).shards(s -> s.total(1).successful(1).failed(0))
                    .scrollId("scroll-" + scrollCalls).hits(h -> h.hits(nextHits())).build();
        }

        /** 记录被释放的上下文，可模拟清理失败。 */
        @Override
        public ClearScrollResponse clearScroll(ClearScrollRequest request) throws IOException {
            clearedScrollId = request.scrollId().get(0);
            if (cleanupFailure) {
                throw new IOException("清理失败");
            }
            return new ClearScrollResponse.Builder().succeeded(true).numFreed(1).build();
        }

        /** 每批最多生成一千个连续 ID。 */
        private List<Hit<Void>> nextHits() {
            List<Hit<Void>> hits = new ArrayList<>();
            int end = Math.min(offset + 1000, snapshotSize);
            while (offset < end) {
                offset++;
                hits.add(new Hit.Builder<Void>().index("test_alert").id(String.valueOf(offset)).build());
            }
            return hits;
        }
    }
}
