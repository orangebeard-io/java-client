package io.orangebeard.client.v3;

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
import io.orangebeard.client.entity.test.FinishTest;
import io.orangebeard.client.entity.test.StartTest;

import java.util.List;
import java.util.UUID;

public interface V3Client {
    UUID startTestRun(StartV3TestRun testRun);
    void startAnnouncedTestRun(UUID testRunUUID);
    void finishTestRun(UUID testRunUUID, FinishV3TestRun finishTestRun);
    List<UUID> startSuite(StartSuite startSuite);
    UUID startTest(StartTest startTest);
    void finishTest(UUID testUUID, FinishTest finishTest);
    UUID startStep(StartStep startStep);
    void finishStep(UUID stepUUID, FinishStep finishStep);
    UUID log(Log log);
    void sendLogBatch(List<Log> logs);
    UUID sendAttachment(Attachment attachment);
    UUID startAlertRun(StartAlertRun alertRun);
    void finishAlertRun(FinishAlertRun alertRun);
    UUID reportAlert(ReportAlert alert);
}
