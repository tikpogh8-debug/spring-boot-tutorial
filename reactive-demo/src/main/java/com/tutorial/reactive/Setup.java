package com.tutorial.reactive;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.SignalType;

/**
 * ETL: Rakuten JSON -> item names -> Sport entities -> R2DBC.
 * The whole pipeline is pulled in batches: the subscriber asks for {@code batch-size} elements with
 * request(n), receives exactly that many onNext signals, then asks for the next batch.
 */
@Component
@ConditionalOnProperty(name = "etl.enabled", havingValue = "true", matchIfMissing = true)
public class Setup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Setup.class);

    private final RakutenClient client;
    private final SportRepository repository;
    private final RakutenProperties props;
    private final int batchSize;

    public Setup(RakutenClient client, SportRepository repository, RakutenProperties props,
            @Value("${etl.batch-size:20}") int batchSize) {
        this.client = client;
        this.repository = repository;
        this.props = props;
        this.batchSize = batchSize;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (props.applicationId() == null || props.applicationId().isBlank()) {
            log.warn("ETL skipped: set RAKUTEN_APP_ID to load sports from Rakuten");
            return;
        }
        Long saved = load(client.itemNames()).block();
        log.info("ETL finished, {} sports saved", saved);
    }

    /** Runs the ETL for the given names and emits the number of sports saved. */
    Mono<Long> load(Flux<String> names) {
        return Mono.create(sink -> pipeline(names).subscribe(new BatchSubscriber(batchSize, sink)));
    }

    /** Skips names that already exist, so the ETL can be re-run safely. */
    Flux<Sport> pipeline(Flux<String> names) {
        return names
                .map(Sport::named)
                .concatMap(sport -> repository.existsByName(sport.name())
                        .filter(exists -> !exists)
                        .flatMap(x -> repository.save(sport))
                        .onErrorResume(DuplicateKeyException.class, e -> Mono.empty()), 1);
    }

    /** Requests {@code batchSize} elements at a time and logs every request and onNext. */
    static final class BatchSubscriber extends BaseSubscriber<Sport> {

        private final int batchSize;
        private final MonoSink<Long> sink;
        private final List<Long> requests = new ArrayList<>();
        private long received;
        private long inBatch;

        BatchSubscriber(int batchSize, MonoSink<Long> sink) {
            this.batchSize = batchSize;
            this.sink = sink;
            sink.onCancel(this::cancel);
        }

        /** Sizes of all request(n) calls made so far (for tests). */
        List<Long> requests() {
            return requests;
        }

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            requestBatch();
        }

        @Override
        protected void hookOnNext(Sport sport) {
            received++;
            inBatch++;
            log.info("onNext #{} (batch item {}/{}): {}", received, inBatch, batchSize, sport.name());
            if (inBatch == batchSize) {
                inBatch = 0;
                requestBatch();
            }
        }

        @Override
        protected void hookOnComplete() {
            log.info("onComplete after {} elements", received);
            sink.success(received);
        }

        @Override
        protected void hookOnError(Throwable throwable) {
            sink.error(throwable);
        }

        @Override
        protected void hookFinally(SignalType type) {
            log.debug("Subscriber finished: {}", type);
        }

        private void requestBatch() {
            requests.add((long) batchSize);
            log.info("request({})", batchSize);
            request(batchSize);
        }
    }
}
