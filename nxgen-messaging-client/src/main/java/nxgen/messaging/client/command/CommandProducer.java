package nxgen.messaging.client.command;

import nxgen.messaging.client.event.Event;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class CommandProducer
{
    CompletionStage<List<Event>> produceCommand(Command command, CommandSpecification specification)
    {
        return null;
    }
}