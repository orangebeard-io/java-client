package io.orangebeard.client.v3;

import io.orangebeard.client.OrangebeardProperties;
import io.orangebeard.client.OrangebeardV3Client;
import io.orangebeard.client.entity.FinishV3TestRun;
import io.orangebeard.client.entity.StartV3TestRun;
import io.orangebeard.client.entity.alerting.FinishAlertRun;
import io.orangebeard.client.entity.alerting.ReportAlert;
import io.orangebeard.client.entity.alerting.StartAlertRun;
import io.orangebeard.client.entity.attachment.Attachment;
import io.orangebeard.client.entity.log.Log;
import io.orangebeard.client.entity.step.FinishStep;
import io.orangebeard.client.entity.step.StartStep;
import io.orangebeard.client.entity.suite.StartSuite;
import io.orangebeard.client.entity.suite.Suite;
import io.orangebeard.client.entity.test.FinishTest;
import io.orangebeard.client.entity.test.StartTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Non-blocking {@link V3Client}. Every call returns a temporary UUID immediately; the actual HTTP requests are
 * made on a small pool of daemon threads, in dependency order (a test is only started once its suite is started,
 * and so on). The temporary UUIDs are translated to the ones assigned by Orangebeard as the requests are made.
 * <p>
 * A failed request fails everything that depends on it (those requests are skipped), but never blocks the caller
 * and never prevents the run from being finished.
 */
public class OrangebeardAsyncV3Client implements V3Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrangebeardAsyncV3Client.class);
    private static final int DEFAULT_THREADS = 4;
    private static final Duration DEFAULT_FINISH_TIMEOUT = Duration.ofMinutes(5);
    private static final AtomicInteger POOL_COUNTER = new AtomicInteger();

    /** Temporary UUID -> eventual real UUID. */
    private final ConcurrentHashMap<UUID, CompletableFuture<UUID>> tasks = new ConcurrentHashMap<>();
    /** Test temporary UUID -> temporary UUIDs of the steps and logs reported under it. */
    private final ConcurrentHashMap<UUID, Set<UUID>> testChildren = new ConcurrentHashMap<>();
    /** Requests that have not completed yet. */
    private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();
    private final AtomicInteger failures = new AtomicInteger();
    private final OrangebeardV3Client client;
    private final Executor executor;
    private final Duration finishTimeout;

    public OrangebeardAsyncV3Client(String endpoint, UUID accessToken, String projectName, boolean connectionWithOrangebeardIsValid) {
        this(new OrangebeardV3Client(endpoint, accessToken, projectName, connectionWithOrangebeardIsValid), newExecutor(DEFAULT_THREADS), DEFAULT_FINISH_TIMEOUT);
    }

    public OrangebeardAsyncV3Client(OrangebeardProperties configuration) {
        this(new OrangebeardV3Client(configuration.getEndpoint(), configuration.getAccessToken(), configuration.getProjectName(), configuration.requiredValuesArePresent()),
                newExecutor(DEFAULT_THREADS), DEFAULT_FINISH_TIMEOUT);

        UUID testRunUUID = configuration.getTestRunUUID();
        if (testRunUUID != null) {
            tasks.put(testRunUUID, CompletableFuture.completedFuture(testRunUUID));
        }
    }

    public OrangebeardAsyncV3Client() {
        this(new OrangebeardProperties());
    }

    OrangebeardAsyncV3Client(OrangebeardV3Client client, Executor executor, Duration finishTimeout) {
        this.client = client;
        this.executor = executor;
        this.finishTimeout = finishTimeout;
    }

    private static Executor newExecutor(int threads) {
        int poolId = POOL_COUNTER.incrementAndGet();
        AtomicInteger threadCounter = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "orangebeard-client-" + poolId + "-" + threadCounter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), factory);
        pool.allowCoreThreadTimeOut(true);
        return pool;
    }

    private static UUID temporaryUUID() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new UUID((random.nextLong() & ~0xF000L) | 0x4000L, (random.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L);
    }

    /** The future of an earlier call; fails when that call is not known (anymore). */
    private CompletableFuture<UUID> dependency(UUID temporaryUUID) {
        CompletableFuture<UUID> task = temporaryUUID == null ? null : tasks.get(temporaryUUID);
        return task != null ? task :
                CompletableFuture.failedFuture(new IllegalStateException("Unknown Orangebeard item: " + temporaryUUID));
    }

    private CompletableFuture<UUID> optionalDependency(UUID temporaryUUID) {
        return temporaryUUID == null ? CompletableFuture.completedFuture(null) : dependency(temporaryUUID);
    }

    /**
     * Runs {@code action} on the executor once all dependencies completed, passing their real UUIDs in order.
     * The action is skipped when a dependency failed.
     */
    private CompletableFuture<UUID> afterAll(String description, Function<UUID[], UUID> action, List<CompletableFuture<UUID>> dependencies) {
        CompletableFuture<UUID> result = CompletableFuture.allOf(dependencies.toArray(new CompletableFuture[0]))
                .thenApplyAsync(ignored -> {
                    UUID[] resolved = dependencies.stream().map(CompletableFuture::join).toArray(UUID[]::new);
                    try {
                        return action.apply(resolved);
                    } catch (RuntimeException e) {
                        reportFailure(description, e);
                        throw e;
                    }
                }, executor);
        track(result);
        return result;
    }

    /** Keeps a dead server from flooding the log: the first failures are warnings, after that every 100th. */
    private void reportFailure(String description, RuntimeException e) {
        int count = failures.incrementAndGet();
        if (count <= 5 || count % 100 == 0) {
            LOGGER.warn("Orangebeard: {} failed: {} ({} failures so far)", description, e.getMessage(), count);
        } else {
            LOGGER.debug("Orangebeard: {} failed: {}", description, e.getMessage());
        }
    }

    @SafeVarargs
    private CompletableFuture<UUID> afterAll(String description, Function<UUID[], UUID> action, CompletableFuture<UUID>... dependencies) {
        return afterAll(description, action, List.of(dependencies));
    }

    private CompletableFuture<UUID> async(String description, java.util.function.Supplier<UUID> action) {
        return afterAll(description, ignored -> action.get(), List.of());
    }

    private void track(CompletableFuture<?> future) {
        pending.add(future);
        future.whenComplete((value, error) -> pending.remove(future));
    }

    private void register(UUID temporaryUUID, CompletableFuture<UUID> task) {
        tasks.put(temporaryUUID, task);
    }

    private void registerUnderTest(UUID test, UUID temporaryUUID, CompletableFuture<UUID> task) {
        register(temporaryUUID, task);
        if (test != null) {
            testChildren.computeIfAbsent(test, key -> ConcurrentHashMap.newKeySet()).add(temporaryUUID);
        }
    }

    private void evictChildrenOf(UUID test) {
        Set<UUID> children = testChildren.remove(test);
        if (children != null) {
            children.forEach(tasks::remove);
        }
    }

    /** Waits (bounded) for all outstanding requests, whether they succeed or not. */
    private void awaitPending() {
        CompletableFuture<?>[] outstanding = pending.stream()
                .map(future -> future.handle((value, error) -> null))
                .toArray(CompletableFuture[]::new);
        try {
            CompletableFuture.allOf(outstanding).get(finishTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            LOGGER.warn("Orangebeard: {} requests were still outstanding after {}; finishing anyway.", outstanding.length, finishTimeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (java.util.concurrent.ExecutionException e) {
            LOGGER.debug("Unexpected failure while waiting for outstanding requests", e);
        }
    }

    private void clear() {
        tasks.clear();
        testChildren.clear();
    }

    /** Real UUID of a completed call, or null when it is unknown or failed. */
    private UUID realUUID(UUID temporaryUUID) {
        CompletableFuture<UUID> task = temporaryUUID == null ? null : tasks.get(temporaryUUID);
        if (task == null || !task.isDone() || task.isCompletedExceptionally()) {
            return null;
        }
        return task.join();
    }

    @Override
    public UUID startTestRun(StartV3TestRun testRun) {
        UUID temporaryUUID = temporaryUUID();
        register(temporaryUUID, async("start test run", () -> client.startTestRun(testRun)));
        return temporaryUUID;
    }

    public UUID startTestRunAndAwaitRealUUID(StartV3TestRun testRun) {
        return client.startTestRun(testRun);
    }

    @Override
    public void startAnnouncedTestRun(UUID testRunUUID) {
        register(testRunUUID, async("start announced test run", () -> {
            client.startAnnouncedTestRun(testRunUUID);
            return testRunUUID;
        }));
    }

    @Override
    public void finishTestRun(UUID testRunUUID, FinishV3TestRun finishTestRun) {
        awaitPending();
        UUID realUUID = realUUID(testRunUUID);
        clear();
        if (realUUID == null) {
            LOGGER.warn("Orangebeard: test run {} was not started; not finishing it.", testRunUUID);
            return;
        }
        client.finishTestRun(realUUID, finishTestRun);
    }

    public void finishTestRunWithRealUUID(UUID testRunUUID, FinishV3TestRun finishTestRun) {
        client.finishTestRun(testRunUUID, finishTestRun);
    }

    @Override
    public List<UUID> startSuite(StartSuite startSuite) {
        int suiteCount = startSuite.getSuiteNames().size();
        List<UUID> temporaryUUIDs = new ArrayList<>(suiteCount);

        CompletableFuture<UUID> run = dependency(startSuite.getTestRunUUID());
        CompletableFuture<UUID> parent = optionalDependency(startSuite.getParentSuiteUUID());

        List<Suite> started = new ArrayList<>();
        CompletableFuture<UUID> lastSuite = afterAll("start suite", resolved -> {
            List<Suite> suites = client.startSuite(new StartSuite(
                    resolved[0],
                    resolved[1],
                    startSuite.getDescription(),
                    startSuite.getAttributes(),
                    startSuite.getSuiteNames()));
            synchronized (started) {
                started.addAll(suites);
            }
            return suites.isEmpty() ? null : suites.get(suites.size() - 1).getSuiteUUID();
        }, run, parent);

        for (int i = 0; i < suiteCount; i++) {
            UUID temporaryUUID = temporaryUUID();
            int index = i;
            // Each suite in the path gets its own UUID; the server returns them in path order.
            CompletableFuture<UUID> suiteTask = index == suiteCount - 1 ? lastSuite : lastSuite.thenApply(leaf -> {
                synchronized (started) {
                    return started.size() == suiteCount ? started.get(index).getSuiteUUID() : leaf;
                }
            });
            register(temporaryUUID, suiteTask);
            temporaryUUIDs.add(temporaryUUID);
        }
        return temporaryUUIDs;
    }

    @Override
    public UUID startTest(StartTest startTest) {
        UUID temporaryUUID = temporaryUUID();
        register(temporaryUUID, afterAll("start test", resolved -> client.startTest(new StartTest(
                resolved[0],
                resolved[1],
                startTest.getTestName(),
                startTest.getTestType(),
                startTest.getDescription(),
                startTest.getAttributes(),
                startTest.getStartTime())),
                dependency(startTest.getTestRunUUID()), dependency(startTest.getSuiteUUID())));
        return temporaryUUID;
    }

    @Override
    public void finishTest(UUID testUUID, FinishTest finishTest) {
        afterAll("finish test", resolved -> {
            client.finishTest(resolved[1], new FinishTest(resolved[0], finishTest.getStatus(), finishTest.getEndTime()));
            return null;
        }, dependency(finishTest.getTestRunUUID()), dependency(testUUID))
                .whenComplete((value, error) -> evictChildrenOf(testUUID));
    }

    @Override
    public UUID startStep(StartStep startStep) {
        UUID temporaryUUID = temporaryUUID();
        registerUnderTest(startStep.getTestUUID(), temporaryUUID, afterAll("start step", resolved -> client.startStep(new StartStep(
                resolved[0],
                resolved[1],
                resolved[2],
                startStep.getStepName(),
                startStep.getDescription(),
                startStep.getStartTime())),
                dependency(startStep.getTestRunUUID()), dependency(startStep.getTestUUID()), optionalDependency(startStep.getParentStepUUID())));
        return temporaryUUID;
    }

    @Override
    public void finishStep(UUID stepUUID, FinishStep finishStep) {
        afterAll("finish step", resolved -> {
            client.finishStep(resolved[1], new FinishStep(resolved[0], finishStep.getStatus(), finishStep.getEndTime()));
            return null;
        }, dependency(finishStep.getTestRunUUID()), dependency(stepUUID));
    }

    @Override
    public UUID log(Log log) {
        UUID temporaryUUID = temporaryUUID();
        registerUnderTest(log.getTestUUID(), temporaryUUID, afterAll("log", resolved -> client.log(new Log(
                resolved[0],
                resolved[1],
                resolved[2],
                log.getMessage(),
                log.getLogLevel(),
                log.getLogTime(),
                log.getLogFormat())),
                dependency(log.getTestRunUUID()), dependency(log.getTestUUID()), optionalDependency(log.getStepUUID())));
        return temporaryUUID;
    }

    /**
     * @param logs list of log entries to send
     * @deprecated Use single async log calls to ensure synchronization. This method now acts as a forwarder
     */
    @Override
    @Deprecated(since = "2.0.0")
    public void sendLogBatch(List<Log> logs) {
        logs.forEach(this::log);
    }

    @Override
    public UUID sendAttachment(Attachment attachment) {
        UUID temporaryUUID = temporaryUUID();
        Attachment.AttachmentMetaData meta = attachment.getMetaData();
        registerUnderTest(meta.getTestUUID(), temporaryUUID, afterAll("send attachment", resolved -> client.sendAttachment(new Attachment(
                attachment.getFile(),
                new Attachment.AttachmentMetaData(resolved[0], resolved[1], resolved[2], resolved[3], meta.getAttachmentTime()))),
                dependency(meta.getTestRunUUID()), dependency(meta.getTestUUID()), optionalDependency(meta.getStepUUID()), dependency(meta.getLogUUID())));
        return temporaryUUID;
    }

    @Override
    public UUID startAlertRun(StartAlertRun alertRun) {
        UUID temporaryUUID = temporaryUUID();
        register(temporaryUUID, async("start alert run", () -> client.startAlertRun(alertRun)));
        return temporaryUUID;
    }

    //For CLI lifecycle use
    public UUID startAlertRunAndAwaitRealUUID(StartAlertRun alertRun) {
        return client.startAlertRun(alertRun);
    }

    @Override
    public void finishAlertRun(FinishAlertRun alertRun) {
        awaitPending();
        UUID realUUID = realUUID(alertRun.getAlertRunUUID());
        clear();
        if (realUUID == null) {
            LOGGER.warn("Orangebeard: alert run {} was not started; not finishing it.", alertRun.getAlertRunUUID());
            return;
        }
        alertRun.setAlertRunUUID(realUUID);
        client.finishAlertRun(alertRun);
    }

    //For CLI lifecycle use
    public void finishAlertRunWithRealUUID(FinishAlertRun alertRun) {
        client.finishAlertRun(alertRun);
    }

    @Override
    public UUID reportAlert(ReportAlert alert) {
        UUID temporaryUUID = temporaryUUID();
        register(temporaryUUID, afterAll("report alert", resolved -> {
            alert.setAlertRunUUID(resolved[0]);
            return client.reportAlert(alert);
        }, dependency(alert.getAlertRunUUID())));
        return temporaryUUID;
    }
}
