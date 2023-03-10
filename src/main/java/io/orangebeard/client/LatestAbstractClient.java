package io.orangebeard.client;

import io.orangebeard.client.entity.Attachment;
import io.orangebeard.client.entity.FinishTestItem;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.StartTestItem;
import io.orangebeard.client.exceptions.ClientVersionException;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.UUID;

@Slf4j
public abstract class LatestAbstractClient extends AbstractClient implements LatestOrangebeardClient {

    protected LatestAbstractClient(UUID uuid) {
        super(uuid);
    }

    @Override
    public UUID startTestItem(UUID suiteId, StartTestItem testItem) {
        throw new ClientVersionException("StartTestItem is used only in V1 & V2 clients.");
    }

    @Override
    public void finishTestItem(UUID itemId, FinishTestItem finishTestItem) {
        throw new ClientVersionException("StartTestItem is used only in V1 & V2 clients.");
    }

    @Override
    public void log(Set<Log> logs) {
        log.warn("Log API for V3 Client not defined yet");
    }

    @Override
    public void sendAttachment(Attachment attachment) {
        log.warn("Attachment for V3 Client is not defined yet");
    }
}
