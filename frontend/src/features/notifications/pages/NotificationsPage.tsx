import { useState, useEffect } from 'react';
import { useNotifications } from '../context/useNotifications';
import { useNavigate } from 'react-router-dom';
import {
  Bell,
  CircleCheck,
  TriangleAlert,
  Sparkles,
  ArrowLeft,
  CheckCheck,
  RefreshCw,
  AlertCircle,
  Package,
  TrendingDown,
  Info,
} from 'lucide-react';
import type { NotificationTab, NotificationItem } from '../services/notificationService';
import './NotificationsPage.css';

const TAB_DEFINITIONS: Array<{ key: NotificationTab; label: string; icon: React.ReactNode }> = [
  { key: 'general', label: 'General', icon: <Bell size={16} /> },
  { key: 'errores', label: 'Errores', icon: <TriangleAlert size={16} /> },
  { key: 'ofertas', label: 'Ofertas', icon: <TrendingDown size={16} /> },
  { key: 'nuevo-producto', label: 'Nuevos Productos', icon: <Package size={16} /> },
];

const getTypeIcon = (type: string, className: string) => {
  switch (type) {
    case 'ERROR':
      return <AlertCircle size={18} className={className} />;
    case 'OFERTA':
      return <TrendingDown size={18} className={className} />;
    case 'NUEVO_PRODUCTO':
      return <Sparkles size={18} className={className} />;
    default:
      return <Info size={18} className={className} />;
  }
};

const getTypeTagClass = (type: string): string => {
  switch (type) {
    case 'ERROR': return 'tag-error';
    case 'OFERTA': return 'tag-offer';
    case 'NUEVO_PRODUCTO': return 'tag-new';
    default: return 'tag-general';
  }
};

const getIconClass = (type: string): string => {
  switch (type) {
    case 'ERROR': return 'type-error';
    case 'OFERTA': return 'type-offer';
    case 'NUEVO_PRODUCTO': return 'type-new';
    default: return 'type-general';
  }
};

const formatTime = (date: string) =>
  new Intl.DateTimeFormat('es-ES', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(date));

const getTabCount = (tab: NotificationTab, counts: { general: number; errores: number; ofertas: number; nuevoProducto: number }): number => {
  switch (tab) {
    case 'general': return counts.general;
    case 'errores': return counts.errores;
    case 'ofertas': return counts.ofertas;
    case 'nuevo-producto': return counts.nuevoProducto;
    default: return 0;
  }
};

const NotificationsPage = () => {
  const { notificationsByTab, counts, isLoading, loadTab, markAsRead, markAllAsRead } = useNotifications();
  const [activeTab, setActiveTab] = useState<NotificationTab>('general');
  const navigate = useNavigate();

  useEffect(() => {
    void loadTab(activeTab);
  }, [activeTab, loadTab]);

  const changeTab = (tab: NotificationTab) => {
    setActiveTab(tab);
  };

  const handleItemClick = async (notification: NotificationItem) => {
    if (!notification.read) {
      await markAsRead(notification.id);
    }
    if (notification.actionPath) {
      navigate(notification.actionPath);
    }
  };

  const activeList = notificationsByTab[activeTab] ?? [];

  return (
    <div className="notifications-page">
      <header className="notifications-header">
        <h1 className="notifications-title">
          <Bell size={24} aria-hidden="true" />
          Notificaciones
        </h1>
        <div className="notifications-actions">
          <button
            className="btn btn-secondary"
            onClick={() => navigate(-1)}
          >
            <ArrowLeft size={16} aria-hidden="true" />
            Volver
          </button>
          {activeList.length > 0 && (
            <button
              className="btn btn-secondary"
              onClick={() => void markAllAsRead(activeTab)}
            >
              <CheckCheck size={16} aria-hidden="true" />
              Marcar todo leído
            </button>
          )}
          <button
            className="btn btn-secondary"
            onClick={() => void loadTab(activeTab)}
            disabled={isLoading}
          >
            <RefreshCw size={16} className={isLoading ? 'spin-icon' : ''} aria-hidden="true" />
            Refrescar
          </button>
        </div>
      </header>

      <div className="notifications-tabs" role="tablist" aria-label="Categorías de notificaciones">
        {TAB_DEFINITIONS.map((tab) => {
          const count = getTabCount(tab.key, counts);
          return (
            <button
              key={tab.key}
              type="button"
              role="tab"
              className={`notification-tab-btn ${activeTab === tab.key ? 'active' : ''}`}
              aria-selected={activeTab === tab.key}
              onClick={() => changeTab(tab.key)}
            >
              {tab.icon}
              <span>{tab.label}</span>
              {count > 0 && <span className="tab-count-badge">{count > 99 ? '99+' : count}</span>}
            </button>
          );
        })}
      </div>

      {isLoading ? (
        <div className="notifications-loading">Cargando notificaciones...</div>
      ) : activeList.length === 0 ? (
        <div className="notifications-empty">
          <CircleCheck size={48} aria-hidden="true" />
          <p>No hay notificaciones en esta categoría</p>
        </div>
      ) : (
        <div className="notifications-list">
          {activeList.map((notification) => (
            <div
              key={notification.id}
              className={`notification-card ${notification.read ? 'read' : 'unread'}`}
              onClick={() => void handleItemClick(notification)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  void handleItemClick(notification);
                }
              }}
            >
              <div className={`notification-type-icon ${getIconClass(notification.type)}`}>
                {getTypeIcon(notification.type, '')}
              </div>
              <div className="notification-content">
                <div className="notification-title">{notification.title}</div>
                <div className="notification-message">{notification.message}</div>
                <div className="notification-meta">
                  <span className={`notification-type-tag ${getTypeTagClass(notification.type)}`}>
                    {notification.type.replace('_', ' ')}
                  </span>
                  {notification.supplierName && (
                    <span>{notification.supplierName}</span>
                  )}
                  <span>{formatTime(notification.createdAt)}</span>
                </div>
              </div>
              {!notification.read && <div className="notification-read-dot" />}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default NotificationsPage;
