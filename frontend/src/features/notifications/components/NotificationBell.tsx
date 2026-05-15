import { useState, useRef, useEffect } from 'react';
import { useNotifications } from '../context/useNotifications';
import { Bell, CircleCheck, ExternalLink } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { NotificationTab } from '../services/notificationService';
import './NotificationBell.css';

const TAB_DEFINITIONS: Array<{ key: NotificationTab; label: string }> = [
    { key: 'general', label: 'General' },
    { key: 'errores', label: 'Errores' },
    { key: 'ofertas', label: 'Ofertas' },
    { key: 'nuevo-producto', label: 'Nuevo Producto' },
];

const NotificationBell: React.FC = () => {
    const { notificationsByTab, counts, unreadCount, isLoading, loadTab, markAsRead, markAllAsRead } = useNotifications();
    const [isOpen, setIsOpen] = useState(false);
    const [activeTab, setActiveTab] = useState<NotificationTab>('general');
    const dropdownRef = useRef<HTMLDivElement>(null);
    const navigate = useNavigate();

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const toggleDropdown = () => {
        const nextState = !isOpen;
        setIsOpen(nextState);
        if (nextState) {
            void loadTab(activeTab);
        }
    };

    const changeTab = (tab: NotificationTab) => {
        setActiveTab(tab);
        void loadTab(tab);
    };

    const formatTime = (date: string) =>
        new Intl.DateTimeFormat('es-ES', {
            day: '2-digit',
            month: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
        }).format(new Date(date));

    const getTabCount = (tab: NotificationTab): number => {
        switch (tab) {
            case 'general':
                return counts.general;
            case 'errores':
                return counts.errores;
            case 'ofertas':
                return counts.ofertas;
            case 'nuevo-producto':
                return counts.nuevoProducto;
            default:
                return 0;
        }
    };

    const getTypeClass = (type: string): string => {
        switch (type) {
            case 'ERROR':
                return 'type-error';
            case 'OFERTA':
                return 'type-offer';
            case 'NUEVO_PRODUCTO':
                return 'type-new';
            default:
                return 'type-general';
        }
    };

    const activeList = notificationsByTab[activeTab] ?? [];

    const handleItemClick = async (notificationId: number, actionPath: string | null) => {
        await markAsRead(notificationId);
        if (actionPath) {
            navigate(actionPath);
            setIsOpen(false);
        }
    };

    return (
        <div className="notification-bell-container" ref={dropdownRef}>
            <button
                className="bell-button"
                onClick={toggleDropdown}
                title="Notificaciones"
                aria-label={`Notificaciones${unreadCount > 0 ? `, ${unreadCount} sin leer` : ''}`}
            >
                <Bell width={22} height={22} aria-hidden="true" />
                {unreadCount > 0 && (
                    <span className="bell-badge" aria-hidden="true">
                        {unreadCount > 99 ? '99+' : unreadCount}
                    </span>
                )}
            </button>

            {isOpen && (
                <div className="notification-dropdown" role="dialog" aria-label="Panel de notificaciones">
                    <div className="dropdown-header">
                        <h3>Notificaciones</h3>
                        {activeList.length > 0 && (
                            <button
                                className="clear-btn"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    void markAllAsRead(activeTab);
                                }}
                            >
                                Marcar todo leído
                            </button>
                        )}
                    </div>

                    <div className="dropdown-tabs" role="tablist" aria-label="Categorías de notificaciones">
                        {TAB_DEFINITIONS.map((tab) => {
                            const count = getTabCount(tab.key);
                            return (
                                <button
                                    key={tab.key}
                                    type="button"
                                    role="tab"
                                    className={`tab-button ${activeTab === tab.key ? 'active' : ''}`}
                                    aria-selected={activeTab === tab.key}
                                    onClick={() => changeTab(tab.key)}
                                >
                                    <span>{tab.label}</span>
                                    {count > 0 && <span className="tab-count">{count > 99 ? '99+' : count}</span>}
                                </button>
                            );
                        })}
                    </div>

                    <div className="dropdown-body">
                        {isLoading ? (
                            <div className="no-notifications">Cargando notificaciones...</div>
                        ) : activeList.length === 0 ? (
                            <div className="no-notifications">
                                <CircleCheck size={16} aria-hidden="true" />
                                Sin notificaciones en esta pestaña
                            </div>
                        ) : (
                            <ul className="notification-list">
                                {activeList.map((notification) => (
                                    <li
                                        key={notification.id}
                                        className={`notification-item ${notification.read ? 'read' : 'unread'} ${getTypeClass(notification.type)}`}
                                        onClick={() => {
                                            void handleItemClick(notification.id, notification.actionPath);
                                        }}
                                    >
                                        <div className="notification-meta">
                                            <span className="notification-time">{formatTime(notification.createdAt)}</span>
                                            <span className="notification-status">{notification.type.replace('_', ' ')}</span>
                                        </div>
                                        <div className="notification-title">{notification.title}</div>
                                        <div className="notification-message">{notification.message}</div>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                    <div className="dropdown-footer">
                        <button
                            className="view-all-btn"
                            onClick={() => {
                                navigate('/notificaciones');
                                setIsOpen(false);
                            }}
                        >
                            <ExternalLink size={14} aria-hidden="true" />
                            Ver todas las notificaciones
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default NotificationBell;
