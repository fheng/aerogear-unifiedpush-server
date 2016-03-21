/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.unifiedpush.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.jms.Queue;

import org.jboss.aerogear.unifiedpush.message.serviceLease.AbstractServiceHolder;
import org.jboss.aerogear.unifiedpush.utils.AeroGearLogger;

public class MockServiceCacheForSingleNode extends AbstractServiceHolder<Integer> {

    private static final int INSTANCE_LIMIT = 5;
    private static final long INSTANTIATION_TIMEOUT = 200;
    private static final long DISPOSAL_DELAY = 1000;

    private AtomicInteger counter = new AtomicInteger(0);

    private AeroGearLogger log = AeroGearLogger.getInstance(MockServiceCacheForSingleNode.class);

    @Resource(mappedName = "java:/queue/APNsBadgeLeaseQueue")
    private Queue queue;

    public MockServiceCacheForSingleNode() {
        super(INSTANCE_LIMIT, INSTANTIATION_TIMEOUT, DISPOSAL_DELAY);
    }

    @Override
    public Queue getBadgeQueue() {
        return queue;
    }

    @Override
    public void initialize(String pushMessageInformationId, String variantID) {
        assertEquals("Counter has to be zero before initialize", 0, counter.get());
        super.initialize(pushMessageInformationId, variantID);
        counter.set(INSTANCE_LIMIT);
        log.fine("initialized: " + counter);
    }

    @Override
    public void destroy(String pushMessageInformationId, String variantID) {
        super.destroy(pushMessageInformationId, variantID);
        log.fine("destroyed: " + counter);
        assertEquals("Counter has to be zero after destroy", 0, counter.get());
    }

    @Override
    protected Object leaseBadge(String pushMessageInformationId) {
        Object badge = super.leaseBadge(pushMessageInformationId);
        if (badge != null) {
            counter.decrementAndGet();
            log.fine(counter.toString());
            assertTrue("Instance count can't be never lesser than zero", counter.get() >= 0);
        }
        return badge;
    }

    @Override
    protected void returnBadge(String pushMessageInformationId) {
        counter.incrementAndGet();
        log.fine(counter.toString());
        assertTrue("Instance count can't be never greater than limit", counter.get() <= INSTANCE_LIMIT);
        super.returnBadge(pushMessageInformationId);
    }
}