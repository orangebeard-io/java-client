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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrangebeardAsyncV3Client implements V3Client {
    private final ConcurrentHashMap<UUID, CompletableFuture<Object>> tasks;
    private final ConcurrentHashMap<UUID, UUID> uuidMap;
    private final OrangebeardV3Client client;

    public OrangebeardAsyncV3Client(String endpoint, UUID accessToken, String projectName, boolean connectionWithOrangebeardIsValid) {
        client = new OrangebeardV3Client(endpoint, accessToken, projectName, connectionWithOrangebeardIsValid);
        tasks = new ConcurrentHashMap<>();
        uuidMap = new ConcurrentHashMap<>();
    }

    public OrangebeardAsyncV3Client(OrangebeardProperties configuration) {
        client = new OrangebeardV3Client(configuration.getEndpoint(), configuration.getAccessToken(), configuration.getProjectName(), configuration.requiredValuesArePresent());
        tasks = new ConcurrentHashMap<>();
        uuidMap = new ConcurrentHashMap<>();
    }

    public OrangebeardAsyncV3Client() {
        this(new OrangebeardProperties());
    }

    private CompletableFuture<Object> parentTask(UUID taskUUID) {
        return tasks.get(taskUUID);
    }

    @Override
    public UUID startTestRun(StartV3TestRun testRun) {
        UUID temporaryUUID = UUID.randomUUID();
        CompletableFuture<Object> startTestRunTask = new CompletableFuture<>();
        tasks.put(temporaryUUID, startTestRunTask);

        CompletableFuture.runAsync(() -> {
            UUID actualUUID = client.startTestRun(testRun);
            uuidMap.put(temporaryUUID, actualUUID);
            startTestRunTask.complete(actualUUID);
        });

        return temporaryUUID;
    }

    @Override
    public void startAnnouncedTestRun(UUID testRunUUID) {
        CompletableFuture<Object> startTestRunTask = new CompletableFuture<>();
        tasks.put(testRunUUID, startTestRunTask);

        CompletableFuture.runAsync(() -> {
            client.startAnnouncedTestRun(testRunUUID);
            startTestRunTask.complete(testRunUUID);
        });

        uuidMap.put(testRunUUID, testRunUUID);
    }

    @Override
    public void finishTestRun(UUID testRunUUID, FinishV3TestRun finishTestRun) {
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(tasks.values().toArray(new CompletableFuture[0]));
        allTasks.join(); //await completion of all tasks
        client.finishTestRun(uuidMap.get(testRunUUID), finishTestRun);
    }

    @Override
    public List<UUID> startSuite(StartSuite startSuite) {
        List<UUID> tempUUIDs = startSuite.getSuiteNames().stream().map(
                suite -> UUID.randomUUID()).collect(Collectors.toCollection(LinkedList::new));

        CompletableFuture<Object> parent = startSuite.getParentSuiteUUID() != null ?
                parentTask(startSuite.getParentSuiteUUID()) :
                parentTask(startSuite.getTestRunUUID());

        CompletableFuture<Object> startSuiteTask = new CompletableFuture<>();
        tempUUIDs.forEach(tempUUID -> tasks.put(tempUUID, startSuiteTask));

        parent.thenCompose(parentUUID -> {
            StartSuite realStartSuite;
            UUID testRunUUID;
            UUID parentSuiteUUID = null;
            if (startSuite.getParentSuiteUUID() == null) {
                testRunUUID = (UUID) parentUUID;
            } else {
                testRunUUID = uuidMap.get(startSuite.getTestRunUUID());
                parentSuiteUUID = parentUUID instanceof List ? //If the parent is a suite, a list of uuid's is returned instead of just a uuid.
                        (UUID) ((List<?>) parentUUID).get(((List<?>) parentUUID).size() - 1) :
                        (UUID) parentUUID;
            }

            realStartSuite = new StartSuite(
                    testRunUUID,
                    parentSuiteUUID,
                    startSuite.getDescription(),
                    startSuite.getAttributes(),
                    startSuite.getSuiteNames());

            List<Suite> suites = client.startSuite(realStartSuite);
            List<UUID> actualUUIDs = suites.stream().map(
                    Suite::getSuiteUUID).toList();

            startSuiteTask.complete(actualUUIDs);
            return CompletableFuture.supplyAsync(() -> actualUUIDs);
        });
        return tempUUIDs;
    }

    @Override
    public UUID startTest(StartTest startTest) {
        UUID temporaryUUID = UUID.randomUUID();
        CompletableFuture<Object> parent = parentTask(startTest.getSuiteUUID());
        CompletableFuture<Object> startTestTask = new CompletableFuture<>();
        tasks.put(temporaryUUID, startTestTask);

        parent.thenCompose(parentUUID -> {
            UUID parentSuiteUUID = (UUID) ((List<?>) parentUUID).get(((List<?>) parentUUID).size() - 1);

            StartTest realStartTest = new StartTest(
                    uuidMap.get(startTest.getTestRunUUID()),
                    parentSuiteUUID,
                    startTest.getTestName(),
                    startTest.getTestType(),
                    startTest.getDescription(),
                    startTest.getAttributes(),
                    startTest.getStartTime());

            UUID actualUUID = client.startTest(realStartTest);
            uuidMap.put(temporaryUUID, actualUUID);
            startTestTask.complete(actualUUID);
            return CompletableFuture.supplyAsync(() -> actualUUID);
        });
        return temporaryUUID;
    }

    @Override
    public void finishTest(UUID testUUID, FinishTest finishTest) {
        CompletableFuture<Object> parent = parentTask(testUUID);
        CompletableFuture<Object> finishTestTask = new CompletableFuture<>();
        tasks.put(UUID.randomUUID(), finishTestTask);

        parent.thenCompose(parentUUID -> {
            FinishTest realFinishTest = new FinishTest(
                    uuidMap.get(finishTest.getTestRunUUID()),
                    finishTest.getStatus(),
                    finishTest.getEndTime());
            client.finishTest((UUID) parentUUID, realFinishTest);
            finishTestTask.complete(null);
            return null;
        });
    }

    @Override
    public UUID startStep(StartStep startStep) {
        UUID temporaryUUID = UUID.randomUUID();
        CompletableFuture<Object> parent = startStep.getParentStepUUID() != null ?
                parentTask(startStep.getParentStepUUID()) :
                parentTask(startStep.getTestUUID());

        CompletableFuture<Object> startStepTask = new CompletableFuture<>();
        tasks.put(temporaryUUID, startStepTask);

        parent.thenCompose(parentUUID -> {
            UUID testUUID;
            UUID parentStepUUID = null;
            if (startStep.getParentStepUUID() == null) {
                testUUID = (UUID) parentUUID;
            } else {
                testUUID = uuidMap.get(startStep.getTestUUID());
                parentStepUUID = (UUID) parentUUID;
            }
            StartStep realStartStep = new StartStep(
                    uuidMap.get(startStep.getTestRunUUID()),
                    testUUID,
                    parentStepUUID,
                    startStep.getStepName(),
                    startStep.getDescription(),
                    startStep.getStartTime()
            );
            UUID actualUUID = client.startStep(realStartStep);
            startStepTask.complete(actualUUID);
            uuidMap.put(temporaryUUID, actualUUID);
            return CompletableFuture.supplyAsync(() -> actualUUID);
        });

        return temporaryUUID;
    }

    @Override
    public void finishStep(UUID stepUUID, FinishStep finishStep) {
        CompletableFuture<Object> parent = parentTask(stepUUID);
        CompletableFuture<Object> finishStepTask = new CompletableFuture<>();
        tasks.put(UUID.randomUUID(), finishStepTask);

        parent.thenCompose(parentUUID -> {
            FinishStep realFinishStep = new FinishStep(
                    uuidMap.get(finishStep.getTestRunUUID()),
                    finishStep.getStatus(),
                    finishStep.getEndTime());
            client.finishStep((UUID) parentUUID, realFinishStep);
            finishStepTask.complete(null);
            return null;
        });
    }

    @Override
    public UUID log(Log log) {
        UUID temporaryUUID = UUID.randomUUID();
        CompletableFuture<Object> parent = log.getStepUUID() != null ?
                parentTask(log.getStepUUID()) :
                parentTask(log.getTestUUID());

        CompletableFuture<Object> logTask = new CompletableFuture<>();
        tasks.put(temporaryUUID, logTask);

        parent.thenCompose(parentUUID -> {
            UUID testUUID;
            UUID stepUUID = null;
            if (log.getStepUUID() == null) {
                testUUID = (UUID) parentUUID;
            } else {
                testUUID = uuidMap.get(log.getTestUUID());
                stepUUID = (UUID) parentUUID;
            }
            Log realLog = new Log(
                    uuidMap.get(log.getTestRunUUID()),
                    testUUID,
                    stepUUID,
                    log.getMessage(),
                    log.getLogLevel(),
                    log.getLogTime(),
                    log.getLogFormat()
            );
            UUID actualUUID = client.log(realLog);
            logTask.complete(actualUUID);
            uuidMap.put(temporaryUUID, actualUUID);
            return CompletableFuture.supplyAsync(() -> actualUUID);
        });
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
        UUID temporaryUUID = UUID.randomUUID();
        Attachment.AttachmentMetaData meta = attachment.getMetaData();
        CompletableFuture<Object> parent = parentTask(meta.getLogUUID());

        CompletableFuture<Object> attachmentTask = new CompletableFuture<>();
        tasks.put(temporaryUUID, attachmentTask);

        parent.thenCompose(logUUID -> {
            Attachment realAttachment = new Attachment(
                    attachment.getFile(),
                    new Attachment.AttachmentMetaData(
                            uuidMap.get(meta.getTestRunUUID()),
                            uuidMap.get(meta.getTestUUID()),
                            meta.getStepUUID() == null ? null : uuidMap.get(meta.getStepUUID()),
                            (UUID) logUUID,
                            meta.getAttachmentTime()
                    )
            );
            UUID actualUUID = client.sendAttachment(realAttachment);
            attachmentTask.complete(actualUUID);
            uuidMap.put(temporaryUUID, actualUUID);
            return CompletableFuture.supplyAsync(() -> actualUUID);
        });
        return temporaryUUID;
    }

    @Override
    public UUID startAlertRun(StartAlertRun alertRun) {
        UUID temporaryUUID = UUID.randomUUID();
        CompletableFuture<Object> startAlertRuntask = new CompletableFuture<>();
        tasks.put(temporaryUUID, startAlertRuntask);

        CompletableFuture.runAsync(() -> {
            UUID actualUUID = client.startAlertRun(alertRun);
            uuidMap.put(temporaryUUID, actualUUID);
            startAlertRuntask.complete(actualUUID);
        });
        return temporaryUUID;
    }

    @Override
    public void finishAlertRun(FinishAlertRun alertRun) {
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(tasks.values().toArray(new CompletableFuture[0]));
        allTasks.join(); //await completion of all tasks

        UUID realAlertRunUUID = uuidMap.get(alertRun.getAlertRunUUID());
        alertRun.setAlertRunUUID(realAlertRunUUID);
        client.finishAlertRun(alertRun);
    }

    @Override
    public UUID reportAlert(ReportAlert alert) {
        UUID temporaryUUID = UUID.randomUUID();
        CompletableFuture<Object> parent = parentTask(alert.getAlertRunUUID());
        CompletableFuture<Object> reportAlertTask = new CompletableFuture<>();
        tasks.put(temporaryUUID, reportAlertTask);

        parent.thenCompose(parentUUID -> {
            alert.setAlertRunUUID((UUID) parentUUID);
            UUID actualUUID = client.reportAlert(alert);
            uuidMap.put(temporaryUUID, actualUUID);
            reportAlertTask.complete(actualUUID);
            return CompletableFuture.supplyAsync(() -> actualUUID);
        });
        return temporaryUUID;
    }
}
