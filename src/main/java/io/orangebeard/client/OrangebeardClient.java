package io.orangebeard.client;

import io.orangebeard.client.entity.Attachment;
import io.orangebeard.client.entity.FinishTestItem;
import io.orangebeard.client.entity.FinishTestRun;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.StartTestItem;
import io.orangebeard.client.entity.StartTestRun;
import io.orangebeard.client.entity.UpdateTestRun;

import java.util.UUID;

public interface OrangebeardClient {

    UUID startTestRun(StartTestRun testRun);

    void updateTestRun(UUID testRunUUID, UpdateTestRun updateTestRun);

    UUID startTestItem(UUID suiteId, StartTestItem testItem);

    void finishTestItem(UUID itemId, FinishTestItem finishTestItem);

    void finishTestRun(UUID testRunUUID, FinishTestRun finishTestRun);

    void log(Log log);

    void sendAttachment(Attachment attachment);
}
