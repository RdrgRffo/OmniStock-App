import { useCallback, useEffect, useMemo, useRef, useState, ReactNode } from 'react';
import {
    fetchNotificationCounts,
    fetchNotifications,
    markAllNotificationsAsRead,
    markNotificationAsRead,
    NotificationCounts,
    NotificationItem,
    NotificationTab,
} from '../services/notificationService';
import { NotificationContext } from './NotificationContextStore';
import { Client, Message } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

type NotificationsByTab = Record<NotificationTab, NotificationItem[]>;

export interface NotificationContextType {
    notificationsByTab: NotificationsByTab;
    counts: NotificationCounts;
    unreadCount: number;
    isLoading: boolean;
    loadTab: (tab: NotificationTab) => Promise<void>;
    refreshCounts: () => Promise<void>;
    markAsRead: (id: number) => Promise<void>;
    markAllAsRead: (tab: NotificationTab) => Promise<void>;
}

const POLL_INTERVAL_MS = 30000;

const EMPTY_COUNTS: NotificationCounts = {
    general: 0,
    errores: 0,
    ofertas: 0,
    nuevoProducto: 0,
};

const EMPTY_NOTIFICATIONS: NotificationsByTab = {
    general: [],
    errores: [],
    ofertas: [],
    'nuevo-producto': [],
};

export const NotificationProvider = ({ children }: { children: ReactNode }) => {
    const [notificationsByTab, setNotificationsByTab] = useState<NotificationsByTab>(EMPTY_NOTIFICATIONS);
    const [counts, setCounts] = useState<NotificationCounts>(EMPTY_COUNTS);
    const [isLoading, setIsLoading] = useState(false);
    const stompRef = useRef<Client | null>(null);
    const intervalRef = useRef<number | null>(null);

    const refreshCounts = useCallback(async () => {
        try {
            const nextCounts = await fetchNotificationCounts();
            setCounts(nextCounts);
        } catch {
            // Si la red no está disponible, mantener el estado anterior y evitar que la app se bloquee.
        }
    }, []);

    const loadTab = useCallback(async (tab: NotificationTab) => {
        setIsLoading(true);
        try {
            const items = await fetchNotifications(tab, 50);
            setNotificationsByTab((prev) => ({
                ...prev,
                [tab]: items,
            }));
        } catch {
            // Mantener el contenido de la pestaña anterior en fallos transitorios de API/red.
        } finally {
            setIsLoading(false);
        }
    }, []);

    const markAsRead = useCallback(async (id: number) => {
        try {
            await markNotificationAsRead(id);
        } catch {
            return;
        }
        setNotificationsByTab((prev) => ({
            general: prev.general.map((n) => (n.id === id ? { ...n, read: true } : n)),
            errores: prev.errores.map((n) => (n.id === id ? { ...n, read: true } : n)),
            ofertas: prev.ofertas.map((n) => (n.id === id ? { ...n, read: true } : n)),
            'nuevo-producto': prev['nuevo-producto'].map((n) => (n.id === id ? { ...n, read: true } : n)),
        }));
        await refreshCounts();
    }, [refreshCounts]);

    const markAllAsRead = useCallback(async (tab: NotificationTab) => {
        try {
            await markAllNotificationsAsRead(tab);
        } catch {
            return;
        }

        setNotificationsByTab((prev) => {
            if (tab === 'general') {
                return {
                    general: prev.general.map((n) => ({ ...n, read: true })),
                    errores: prev.errores.map((n) => ({ ...n, read: true })),
                    ofertas: prev.ofertas.map((n) => ({ ...n, read: true })),
                    'nuevo-producto': prev['nuevo-producto'].map((n) => ({ ...n, read: true })),
                };
            }

            return {
                ...prev,
                [tab]: prev[tab].map((n) => ({ ...n, read: true })),
            };
        });

        await refreshCounts();
    }, [refreshCounts]);

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (!token) {
            setCounts(EMPTY_COUNTS);
            setNotificationsByTab(EMPTY_NOTIFICATIONS);
            return;
        }

        const bootstrap = async () => {
            await Promise.all([refreshCounts(), loadTab('general')]);
        };

        void bootstrap();

        // Cliente STOMP sobre SockJS para recibir notificaciones push
        const stompClient = new Client({
            webSocketFactory: () => new SockJS('/ws-notifications'),
            connectHeaders: { Authorization: `Bearer ${token}` },
            debug: () => {},
            reconnectDelay: 5000,
            onConnect: () => {
                stompClient.subscribe('/topic/notifications', (msg: Message) => {
                    try {
                        const item = JSON.parse(msg.body) as NotificationItem;
                        setNotificationsByTab((prev) => {
                            const tabKey: NotificationTab = item.type === 'ERROR' ? 'errores' : item.type === 'OFERTA' ? 'ofertas' : item.type === 'NUEVO_PRODUCTO' ? 'nuevo-producto' : 'general';
                            return {
                                ...prev,
                                general: [item, ...prev.general],
                                [tabKey]: [item, ...prev[tabKey]],
                            } as NotificationsByTab;
                        });
                        void refreshCounts();
                    } catch {
                        // ignorar mensajes malformados
                    }
                });
            },
        });

        stompRef.current = stompClient;
        stompClient.activate();

        const interval = window.setInterval(() => {
            void Promise.all([refreshCounts(), loadTab('general')]);
        }, POLL_INTERVAL_MS);
        intervalRef.current = interval;

        return () => {
            window.clearInterval(interval);
            intervalRef.current = null;
            try {
                stompClient.deactivate();
                stompRef.current = null;
            } catch {
                // ignorar
            }
        };
    }, [loadTab, refreshCounts]);

    const unreadCount = useMemo(() => counts.general, [counts.general]);

    const contextValue = useMemo(() => ({
        notificationsByTab,
        counts,
        unreadCount,
        isLoading,
        loadTab,
        refreshCounts,
        markAsRead,
        markAllAsRead,
    }), [notificationsByTab, counts, unreadCount, isLoading, loadTab, refreshCounts, markAsRead, markAllAsRead]);

    return (
        <NotificationContext.Provider value={contextValue}>
            {children}
        </NotificationContext.Provider>
    );
};
