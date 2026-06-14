package io.github.mmaciekk111.uitestlens.examples;

import io.github.mmaciekk111.uitestlens.core.logging.ConsumerLogSink;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomLoggerSinkExampleTest {

    @Test
    void forwardEventsToExistingLogger() {
        List<String> forwarded = new ArrayList<>();

        UiTestLensLogger logger = UiTestLensLogger.builder()
                .sink(new ConsumerLogSink(entry -> forwarded.add(entry.level() + ": " + entry.message())))
                .build();

        logger.info("Forwarded event");

        assertEquals(List.of("INFO: Forwarded event"), forwarded);
    }
}
