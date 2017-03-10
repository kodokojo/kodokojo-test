/**
 * Kodo Kojo - ${project.description}
 * Copyright © 2017 Kodo Kojo (infos@kodokojo.io)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.kodokojo.test;

public class WebSocketEventsListener { /*extends Endpoint{

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEventsListener.class);

    public interface CallBack {
        void open(Session session);
        void receive(Session session, String message);
        void close(Session session);
    }

    private final LinkedList<String> messages;

    private final CallBack callBack;

    public WebSocketEventsListener(CallBack callBack) {
        messages = new LinkedList<>();
        this.callBack = callBack;
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        LOGGER.debug("WebSocket Session opened");
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                messages.addLast(message);
                LOGGER.trace("Receive message : {}", message);
                if (callBack != null) {
                    callBack.receive(session, message);
                }
            }
        });
        if (callBack != null) {
            callBack.open(session);
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        if (callBack != null) {
            callBack.close(session);
        }
    }

    public LinkedList<String> getMessages() {
        return new LinkedList<>(messages);
    }

    */
}
