package io.orangebeard.client;

import io.orangebeard.client.entity.FinishTestRun;
import io.orangebeard.client.entity.StartTestRun;
import io.orangebeard.client.entity.UpdateTestRun;
import io.orangebeard.client.entity.suite.StartSuite;
import io.orangebeard.client.entity.suite.Suite;
import io.orangebeard.client.entity.test.FinishTest;
import io.orangebeard.client.entity.test.StartTest;

import java.util.List;
import java.util.UUID;

public interface LatestOrangebeardClient {

    UUID startTestRun(StartTestRun testRun);

    void startAnnouncedTestRun(UUID testRunUUID);

    void updateTestRun(UUID testRunUUID, UpdateTestRun updateTestRun);

    void finishTestRun(UUID testRunUUID, FinishTestRun finishTestRun);

    UUID startTest(UUID suiteId, StartTest startTest);

    void finishTest(UUID itemId, FinishTest finishTest);

    List<Suite> startSuite(StartSuite startSuite);
}
