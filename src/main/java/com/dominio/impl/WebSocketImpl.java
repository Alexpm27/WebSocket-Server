package com.dominio.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint(value = "/testWebSocket/{token}")
public class WebSocketImpl {
	private static final Logger LOG = LoggerFactory.getLogger(WebSocketImpl.class);
	private static ConcurrentHashMap<String, WebSocketImpl> concurrentHashMap = new ConcurrentHashMap<>(12);

	/**
	 * Objeto de conversación
	 **/
	private Session session;
	@OnOpen
	public void onOpen(Session session, @PathParam("token") String token) {
		this.session = session;
		concurrentHashMap.put(token, this);
		System.out.println("Open a websocket. token={}" + token);
		LOG.info("Open a websocket. token={}" + token);
	}

	@OnClose
	public void onClose(Session session, @PathParam("token") String token) {
		concurrentHashMap.remove(token);
		System.out.println("Close a websocket, concurrentHashMap remove sessionId= {}" + token);
		LOG.info("Close a websocket, concurrentHashMap remove sessionId= {}" + token);
	}

	@OnMessage
	public void onMessage(String message, @PathParam("token") String token) throws Exception {
		System.out.println("receive a message from client id={},msg={}" + token + " " + message);
		sendMessage(token, message);
		sendAllMessage(message);
		LOG.info("receive a message from client id={},msg={}" + token + " " + message);
	}

	@OnError
	public void onError(Session session, Throwable error) {
		System.out.println("Error while websocket. " + error);
		LOG.error("Error while websocket. " + error);
	}

	/**
	 * Enviar mensaje al cliente especificado
	 * 
	 * @param token
	 * @param message
	 */
	public void sendMessage(String token, String message) throws Exception {
		WebSocketImpl webSocketProcess = concurrentHashMap.get(token);
		if (!ObjectUtils.isEmpty(webSocketProcess)) {
			if (webSocketProcess.session.isOpen()) {
				webSocketProcess.session.getBasicRemote().sendText(message);
			} else {
				System.out.println("websocket session={} is closed " + token);
				LOG.error("websocket session={} is closed " + token);
			}
		} else {
			System.out.println("websocket session={} is not exit " + token);
			LOG.error("websocket session={} is not exit " + token);
		}
	}

	/**
	 * Enviar mensajes a todos los clientes
	 * 
	 */
	public void sendAllMessage(String msg) throws Exception {
		System.out.println("online client count={}" + concurrentHashMap.size());
		LOG.info("online client count={}" + concurrentHashMap.size());
		Set<Map.Entry<String, WebSocketImpl>> entries = concurrentHashMap.entrySet();
		for (Map.Entry<String, WebSocketImpl> entry : entries) {
			String cid = entry.getKey();
			WebSocketImpl webSocketProcess = entry.getValue();
			boolean sessionOpen = webSocketProcess.session.isOpen();
			if (sessionOpen) {
//				  webSocketProcess.session.getAsyncRemote().sendText(msg);
				webSocketProcess.session.getBasicRemote().sendText(msg);
			} else {
				System.out.println("cid={} is closed,ignore send text" + cid);
				LOG.info("cid={} is closed,ignore send text" + cid);
			}
		}
	}
	
	public static String readFileAsString(String file) throws Exception {
		byte[] utf8 = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.ISO_8859_1).getBytes("UTF-8");;
		String content = new String(utf8);
		return content;
	}

}
