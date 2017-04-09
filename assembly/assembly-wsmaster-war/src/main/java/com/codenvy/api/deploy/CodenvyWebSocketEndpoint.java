/*
 *  [2012] - [2017] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.deploy;

import org.eclipse.che.api.core.websocket.WebSocketMessageReceiver;
import org.eclipse.che.api.core.websocket.impl.BasicWebSocketEndpoint;
import org.eclipse.che.api.core.websocket.impl.GuiceInjectorEndpointConfigurator;
import org.eclipse.che.api.core.websocket.impl.MessagesReSender;
import org.eclipse.che.api.core.websocket.impl.WebSocketSessionRegistry;

import javax.inject.Inject;
import javax.websocket.server.ServerEndpoint;

/**
 * Implementation of {@link BasicWebSocketEndpoint} for Che packaging.
 * Add only mapping "/websocket/{endpoint-id}".
 */
@ServerEndpoint(value = "/websocket/{endpoint-id}", configurator = GuiceInjectorEndpointConfigurator.class)
public class CodenvyWebSocketEndpoint extends BasicWebSocketEndpoint {
    @Inject
    public CodenvyWebSocketEndpoint(WebSocketSessionRegistry registry,
                                    MessagesReSender reSender,
                                    WebSocketMessageReceiver receiver) {
        super(registry, reSender, receiver);
    }
}
