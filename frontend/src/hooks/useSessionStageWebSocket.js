// hooks/useSessionWebSocket.js
import { useState, useEffect, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export const useSessionStageWebSocket = scheduleId => {
    const [stompClient, setStompClient] = useState(null);
    const [readyStatusResponse, setReadyStatusResponse] = useState({});
    const [sessionTransferResponse, setSessionTransferResponse] = useState(null);

    useEffect(() => {
        const client = new Client({
            webSocketFactory: () => new SockJS(`${process.env.REACT_APP_SERVER_URL}/ws`),
            connectHeaders: {
                Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
            },
            onConnect: () => {
                // 세션 스테이지 변경 구독
                client.subscribe(`/topic/session-stage/${scheduleId}`, message => {
                    const response = JSON.parse(message.body);

                    if (response.type === 'SESSION_TRANSFER') {
                        setSessionTransferResponse({
                            currentSessionStage: response.currentSessionStage, // 현재 세션단계
                            nextSessionStage: response.nextSessionStage, // 넘어가야하는 세션단계
                            sessionStartTime: new Date(response.sessionStartTime), // 다음 단계 시작시간
                            sessionDuration: response.sessionDuration, // 다음 단계 진행 시간
                        });
                    } else if (response.type === 'READY_STATUS') {
                        setReadyStatusResponse(response.readyMembers); // Map<Long, Boolean> readyMembers
                    }
                });
            },
            onDisconnect: () => {
            },
            onStompError: frame => {
                console.error('WebSocket Error:', frame);
            },
        });

        client.activate();
        setStompClient(client);

        return () => {
            if (client) {
                client.deactivate();
            }
        };
    }, [scheduleId]);

    const sendReady = useCallback((readyStatus) => {
        if (stompClient && stompClient.connected) {
            stompClient.publish({
                destination: '/app/ready',
                body: JSON.stringify({
                    scheduleId,
                    accessToken: localStorage.getItem('accessToken'),
                    status: readyStatus
                }),
            });
        }
    }, [stompClient, scheduleId]);

    return {
        sendReady,
        readyStatusResponse,
        sessionTransferResponse,
        isConnected: stompClient?.connected || false,
    };
};
