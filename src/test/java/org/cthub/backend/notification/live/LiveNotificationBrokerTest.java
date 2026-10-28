package org.cthub.backend.notification.live;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class LiveNotificationBrokerTest {

    private final LiveNotificationBroker broker = new LiveNotificationBroker();

    @AfterEach
    void tearDown() {
        broker.shutdown();
    }

    @Test
    void connectRegistersStreamPerUser() {
        broker.connect(1L, "s1");
        broker.connect(1L, "s1");

        assertThat(broker.isConnected(1L)).isTrue();
        assertThat(broker.isConnected(2L)).isFalse();
        assertThat(broker.connectionCount()).isEqualTo(2);
    }

    @Test
    void closeSessionOnlyEndsThatSessionsStreams() {
        broker.connect(1L, "laptop");
        broker.connect(1L, "phone");

        broker.closeSession("laptop");

        assertThat(broker.isConnected(1L)).isTrue();
        assertThat(broker.connectionCount()).isEqualTo(1);

        broker.closeSession("phone");
        assertThat(broker.isConnected(1L)).isFalse();
    }

    @Test
    void oldestStreamsAreEvictedBeyondPerUserLimit() {
        for (int i = 0; i < LiveNotificationBroker.MAX_CONNECTIONS_PER_USER + 3; i++) {
            broker.connect(1L, "s" + i);
        }

        assertThat(broker.connectionCount()).isEqualTo(LiveNotificationBroker.MAX_CONNECTIONS_PER_USER);
        // The first three sessions were evicted, so closing them changes nothing.
        broker.closeSession("s0");
        broker.closeSession("s2");
        assertThat(broker.connectionCount()).isEqualTo(LiveNotificationBroker.MAX_CONNECTIONS_PER_USER);
    }

    @Test
    void dispatchRunsOffThreadForConnectedUsers() throws InterruptedException {
        broker.connect(1L, "s1");
        CountDownLatch ran = new CountDownLatch(1);
        Thread caller = Thread.currentThread();
        AtomicBoolean offThread = new AtomicBoolean();

        broker.dispatch(1L, sender -> {
            offThread.set(Thread.currentThread() != caller);
            sender.send("unread-count", 3);
            ran.countDown();
        });

        assertThat(ran.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(offThread).isTrue();
    }

    @Test
    void dispatchSkipsUsersWithoutStream() throws InterruptedException {
        CountDownLatch ran = new CountDownLatch(1);

        broker.dispatch(2L, sender -> ran.countDown());

        assertThat(ran.await(200, TimeUnit.MILLISECONDS)).isFalse();
    }

    @Test
    void closedStreamIsForgotten() {
        SseEmitter emitter = broker.connect(1L, "s1");
        assertThat(emitter).isNotNull();

        broker.closeSession("s1");

        assertThat(broker.isConnected(1L)).isFalse();
        assertThat(broker.connectionCount()).isZero();
    }
}
