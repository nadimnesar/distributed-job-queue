package com.nadimnesar.jobqueue.producer.config;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mockStatic;

public class TransactionSupportExtension implements BeforeEachCallback, AfterEachCallback {
    private MockedStatic<TransactionSynchronizationManager> mockedStatic;

    @Override
    public void beforeEach(@NonNull ExtensionContext context) {
        mockedStatic = mockStatic(TransactionSynchronizationManager.class);
        mockedStatic.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
    }

    @Override
    public void afterEach(@NonNull ExtensionContext context) {
        if (mockedStatic != null) {
            mockedStatic.close();
        }
    }
}
