package com.uhsl.flowmessage.flowmessagev2.utils;

import com.imgtec.flow.client.core.ResourceCreatedResponse;

/**
 * Created by Marcus on 22/02/2016.
 */
public interface AsyncCall<Result> {
    /**
     * An asynchronous call
     * @return The result
     */
    Result call();
}
